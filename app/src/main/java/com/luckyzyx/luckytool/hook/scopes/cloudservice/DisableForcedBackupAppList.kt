package com.luckyzyx.luckytool.hook.scopes.cloudservice

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableForcedBackupAppList : YukiBaseHooker() {
    override fun onHook() {
        val backupRestoreOptUiStyle =
            "com.heytap.cloud.backuprestore.bswitch.BackupRestoreOptUiStyle"

        val uiStyleEnum = backupRestoreOptUiStyle.toClassOrNull() ?: run {
            YLog.debug("DisableForcedBackupAppList clazz is null!")
            return
        }
        if (!uiStyleEnum.isEnum) {
            YLog.debug("DisableForcedBackupAppList enum is error!")
            return
        }
        val switchStyle = uiStyleEnum.enumConstants?.find { it.toString() == "STYLE_SWITCH" }
            ?: return

        //Source BackupRestoreOpt
        "com.heytap.cloud.backuprestore.bswitch.BackupRestoreOpt".toClassOrNull()
            ?.resolve()?.apply {
            optional()
            firstMethodOrNull { name = "getForceSelect"; superclass() }?.hook {
                intercept(false)
            }
        }

        //Source BackupRestoreOptUiData
        "com.heytap.cloud.backuprestore.bswitch.bean.BackupRestoreOptUiData".toClassOrNull()
            ?.resolve()
            ?.apply {
                optional()
                firstMethod { name = "getOptStyle"; superclass() }.hook {
                    before {
                        val optId = firstField {
                            name = "optId"
                            superclass()
                        }.of(instance).get<String>()
                        if (optId == "backup_switch_key_third_app") {
                            val style = switchStyle.asResolver().firstMethod {
                                name = "getStyle"
                                superclass()
                            }
                                .invoke()
                            result = style
                        }
                    }
                }
            }
    }
}
