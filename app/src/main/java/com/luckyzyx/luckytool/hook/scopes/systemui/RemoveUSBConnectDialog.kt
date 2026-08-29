package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveUSBConnectDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source UsbService
        VariousClass(
            "com.coloros.systemui.notification.usb.UsbService", //A11
            "com.oplusos.systemui.notification.usb.UsbService",
            "com.oplus.systemui.usb.UsbService" //C14 C15
        ).toClass().resolve().apply {
            optional()
            (firstMethodOrNull { name = "onUsbConnected"; superclass() }
                ?: firstMethod {
                    name { it.contains("onUsbConnected") }
                    superclass()
                }).hook {
                before {
                    val instance = instanceOrNull ?: firstArg().get()
                    val context = lastArg().get<Context>() ?: return@before
                    firstMethod { name = "onUsbSelect"; superclass() }.of(instance).invoke(1)
                    firstMethod { name = "updateAdbNotification"; superclass() }.of(instance)
                        .invoke(context)
                    firstMethod { name = "updateUsbNotification"; superclass() }.let {
                        val contextIndex = it.self.parameterTypes.indexOf(classOf<Context>())
                        if (contextIndex == 0) it.of(instance).invoke(context, 1)
                        else it.of(instance).invoke(1, context)
                    }
                    firstMethod { name = "changeUsbConfig"; superclass() }.let {
                        val contextIndex = it.self.parameterTypes.indexOf(classOf<Context>())
                        if (contextIndex == 0) it.of(instance).invoke(context, 1)
                        else it.of(instance).invoke(1, context)
                    }
                    result = null
                }
            }
            firstMethod { name = "updateUsbNotification"; superclass() }.hook {
                before {
                    firstField {
                        name { it.contains("NeedShowUsbDialog", true) }
                        superclass()
                    }.of(instance).set(false)
                }
            }
        }
    }
}
