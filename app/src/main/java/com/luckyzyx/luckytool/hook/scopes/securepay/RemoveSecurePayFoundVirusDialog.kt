package com.luckyzyx.luckytool.hook.scopes.securepay

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.CheckBox
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSecurePayFoundVirusDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source RiskDialogWrapper
        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    fields {
                    addForType(classOf<Boolean>())
                        addForType(classOf<CheckBox>())
                    }
                    methods {
                        add { paramCount(0);returnType(Void.TYPE) }
                        add { paramCount(4..8);returnType(Void.TYPE) }
                        add { paramCount(0);returnType(classOf<Boolean>()) }
                        add { paramTypes(classOf<View>());returnType(Void.TYPE) }
                        add {
                            paramTypes(
                                classOf<Context>(), classOf<String>(),
                                classOf<Int>(), classOf<DialogInterface>(), classOf<Int>()
                            )
                            returnType(Void.TYPE)
                        }
                    }
                }
            }.let {
                if (it.isEmpty()) return@create
                it.checkDataList("RemoveSecurePayFoundVirusDialog")
                it.single().name.toClass().resolve().apply {
                    optional()
                    firstMethodOrNull {
                        parameters(VagueType, String::class)
                        returnType = Void.TYPE
                        superclass()
                    }?.hook {
                        intercept()
                    }
                    method {
                        emptyParameters()
                        returnType = Void.TYPE
                        superclass()
                    }.hookAll {
                        intercept()
                    }
                }
            }
        }
    }
}
