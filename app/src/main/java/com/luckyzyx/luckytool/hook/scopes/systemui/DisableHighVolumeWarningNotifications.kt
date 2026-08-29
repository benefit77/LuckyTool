package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableHighVolumeWarningNotifications : YukiBaseHooker() {
    override fun onHook() {
        val volumeReceiver = VariousClass(
            "com.oplusos.systemui.notification.receiver.VolumeReceiver", //C12 C13
            "com.oplus.systemui.statusbar.receiver.VolumeReceiver" //C14 C15
        ).toClass()

        //Source OplusPowerUI
        VariousClass(
            "com.oplusos.systemui.notification.power.OplusPowerUI", //C12 C13
            "com.oplus.systemui.statusbar.notification.power.OplusPowerUI" //C14 C15
        ).toClass().resolve().apply {
            optional()
            firstMethod { name = "start"; superclass() }.hook {
                after {
                    val context = (firstFieldOrNull { type = Context::class; superclass() }
                        ?: firstField { type = Context::class; superclass() }).of(instance)
                        .get<Context>() ?: return@after
                    val mVolumeReceiver = (firstFieldOrNull {
                        type = volumeReceiver
                        superclass()
                    }
                        ?: firstField {
                            name { it.contains("VolumeReceiver", true) }
                            superclass()
                        }).of(instance).get() ?: return@after
                    mVolumeReceiver.asResolver().firstMethod { name = "unregister";superclass() }
                        .invoke(context)
                }
            }
        }
    }
}
