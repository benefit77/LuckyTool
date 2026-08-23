package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableAccessibilityWarningDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source FraudBehaviorDetectManager
        "com.android.server.am.FraudBehaviorDetectManager".toClass().resolve().apply {
            //ColorOS 16.x 的真正弹窗调度入口，直接拦截即可阻止无障碍风险提示
            firstMethodOrNull {
                name = "handleChangedApps"
                parameterCount = 3
            }?.hook {
                intercept()
            }
            //兜底：配置重新加载后强制关闭全局关闭风险功能
            firstMethodOrNull {
                name = "jsonToConfig"
                parameters("java.io.InputStream")
            }?.hook {
                after {
                    val mConfig = firstField { name = "mConfig" }.of(instance).get() ?: return@after
                    mConfig.asResolver().firstField { name = "enabled" }.set(false)
                }
            }
        }
    }
}
