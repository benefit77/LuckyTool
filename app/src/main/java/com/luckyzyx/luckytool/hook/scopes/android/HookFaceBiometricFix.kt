package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.GlobalKeyValue
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 将 ColorOS 人脸传感器上报的 4095 强度修正为 Class 3。
 *
 * 主要解决 Bitwarden 等应用使用 Keystore + BiometricPrompt.CryptoObject 解锁时，
 * 人脸认证成功后仍被判定为弱认证而无法解密的问题。
 */
@Obfuscate
object HookFaceBiometricFix : YukiBaseHooker() {

    private val appPreAuthPackage = ThreadLocal<String>()
    private val appKeystoreAuthIds = ThreadLocal<Boolean>()

    override fun onHook() {
        val enabled = preferences(ModulePrefs).getBoolean(
            GlobalKeyValue.keyFaceBiometricFix,
            false,
        )
        if (!enabled) return

        log("hooking biometrics in system_server ...")
        runCatching { hookPreAuthInfo() }
            .onFailure { log("FAIL PreAuthInfo hook: $it") }
        runCatching { hookIsAtLeastStrength() }
            .onFailure { log("FAIL Utils.isAtLeastStrength hook: $it") }
        runCatching { hookAuthenticatorIds() }
            .onFailure { log("FAIL getAuthenticatorIds hook: $it") }
        runCatching { hookAuthSession() }
            .onFailure { log("FAIL AuthSession hook: $it") }
        runCatching { hookBiometricService() }
            .onFailure { log("FAIL BiometricService hook: $it") }
    }

    private fun hookPreAuthInfo() {
        "com.android.server.biometrics.PreAuthInfo"
            .toClass()
            .resolve()
            .apply {
                firstMethod {
                    name = "getStatusForBiometricAuthenticator"
                    parameterCount = 10
                }.hook {
                    before {
                        val packageName = arg(4).get<String>()
                        if (packageName != null && packageName != SYSTEM_UI_PACKAGE) {
                            appPreAuthPackage.set(packageName)
                        }
                    }

                    after {
                        appPreAuthPackage.remove()
                    }
                }
            }
        log("OK: PreAuthInfo app context hook")
    }

    private fun hookIsAtLeastStrength() {
        "com.android.server.biometrics.Utils"
            .toClass()
            .resolve()
            .apply {
                firstMethod {
                    name = "isAtLeastStrength"
                    parameterCount = 2
                    returnType = Boolean::class
                }.hook {
                    after {
                        val packageName = appPreAuthPackage.get()
                        val keystoreAuthCheck = appKeystoreAuthIds.get() == true
                        val sensorStrength = arg(0).get<Int>() ?: 0
                        val requestedStrength = arg(1).get<Int>() ?: 0
                        if ((keystoreAuthCheck ||
                                (packageName != null && packageName != CODEBOOK_PACKAGE)) &&
                            sensorStrength == OEM_STRENGTH_MASK &&
                            (requestedStrength == STRONG_AUTHENTICATORS ||
                                requestedStrength == WEAK_AUTHENTICATORS)
                        ) {
                            result = true
                        }
                    }
                }
            }
        log("OK: app-only 4095 strength check")
    }

    private fun hookAuthenticatorIds() {
        "com.android.server.biometrics.BiometricService\$BiometricServiceWrapper"
            .toClass()
            .resolve()
            .apply {
                firstMethod {
                    name = "getAuthenticatorIds"
                    parameterCount = 1
                }.hook {
                    before {
                        appKeystoreAuthIds.set(true)
                    }

                    after {
                        appKeystoreAuthIds.remove()
                    }
                }
            }
        log("OK: BiometricService getAuthenticatorIds keystore context hook")
    }

    private fun hookAuthSession() {
        "com.android.server.biometrics.AuthSession"
            .toClass()
            .resolve()
            .apply {
                firstMethod {
                    name = "onAuthenticationSucceeded"
                    parameterCount = 3
                }.hook {
                    before {
                        val packageName = getSessionPackageName(instance)
                        val sensorId = arg(0).get<Int>() ?: 0
                        log("AuthSession pkg=$packageName sensorId=$sensorId")
                        if (isNormalAppPackage(packageName) &&
                            isOemFaceSession(instance, sensorId)
                        ) {
                            arg(1).set(true)
                            log(
                                "AuthSession strong=true for $packageName " +
                                    "sensorId=$sensorId",
                            )
                        }
                    }
                }
            }
        log("OK: AuthSession post-auth strong hook")
    }

    private fun hookBiometricService() {
        "com.android.server.biometrics.BiometricService"
            .toClass()
            .resolve()
            .apply {
                firstMethod {
                    name = "isStrongBiometric"
                    parameterCount = 1
                }.hook {
                    after {
                        val sensorId = arg(0).get<Int>() ?: 0
                        val session = instance
                            .asResolver()
                            .firstFieldOrNull { name = "mAuthSession"; superclass() }
                            ?.get<Any>()
                            ?: return@after
                        val packageName = getSessionPackageName(session)
                        log(
                            "BiometricService strong check pkg=$packageName " +
                                "sensorId=$sensorId",
                        )
                        if (isNormalAppPackage(packageName) &&
                            isOemFaceSession(session, sensorId)
                        ) {
                            result = true
                            log(
                                "BiometricService strong=true for $packageName " +
                                    "sensorId=$sensorId",
                            )
                        }
                    }
                }
            }
        log("OK: BiometricService isStrongBiometric hook")
    }

    private fun getSessionPackageName(session: Any): String? {
        val resolver = session.asResolver()
        for (field in listOf(
            "mOpPackageName",
            "mPackageName",
            "mCallingPackage",
            "opPackageName",
        )) {
            val value = resolver
                .firstFieldOrNull {
                    name = field
                    type = String::class
                    superclass()
                }
                ?.get<String>()
            if (value != null) return value
        }
        return null
    }

    private fun isNormalAppPackage(packageName: String?): Boolean =
        packageName != null &&
            packageName != ANDROID_PACKAGE &&
            packageName != SYSTEM_UI_PACKAGE &&
            packageName != CODEBOOK_PACKAGE

    private fun isOemFaceSession(authSession: Any, sensorId: Int): Boolean {
        val preAuthInfo = authSession
            .asResolver()
            .firstFieldOrNull { name = "mPreAuthInfo"; superclass() }
            ?.get<Any>()
            ?: return true
        val sensors = preAuthInfo
            .asResolver()
            .firstFieldOrNull { name = "eligibleSensors"; superclass() }
            ?.get<Any>()
            as? Iterable<*>
            ?: return true

        var inspected = false
        var fieldError = false
        for (sensor in sensors) {
            if (sensor == null) continue
            inspected = true
            val id = sensor.asResolver()
                .firstFieldOrNull {
                    name = "id"
                    type = Int::class
                    superclass()
                }
                ?.get<Int>()
            val modality = sensor.asResolver()
                .firstFieldOrNull {
                    name = "modality"
                    type = Int::class
                    superclass()
                }
                ?.get<Int>()
            val strength = sensor.asResolver()
                .firstFieldOrNull {
                    name = "oemStrength"
                    type = Int::class
                    superclass()
                }
                ?.get<Int>()
            if (id == null || modality == null || strength == null) {
                fieldError = true
                continue
            }
            if (id == sensorId &&
                modality == FACE_MODALITY &&
                strength == OEM_STRENGTH_MASK
            ) {
                return true
            }
        }
        if (fieldError) {
            log("isOemFaceSession: sensor field reflection failed, fallback for app")
        }
        if (!inspected) {
            log("isOemFaceSession: no eligible sensors inspected, fallback for app")
        }
        return fieldError || !inspected
    }

    private fun log(message: String) {
        YLog.debug("[$TAG] $message")
    }

    private const val TAG = "FaceBiometricFix"
    private const val ANDROID_PACKAGE = "android"
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val CODEBOOK_PACKAGE = "com.coloros.codebook"
    private const val OEM_STRENGTH_MASK = 4095
    private const val STRONG_AUTHENTICATORS = 15
    private const val WEAK_AUTHENTICATORS = 255
    private const val FACE_MODALITY = 8
}
