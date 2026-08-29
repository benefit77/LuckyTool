package com.luckyzyx.luckytool.hook.scopes.securepay

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.CheckBox
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
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
                        addForType(Boolean::class.java)
                        addForType(CheckBox::class.java)
                    }
                    methods {
                        add { paramCount(0);returnType(Void.TYPE) }
                        add { paramCount(4..8);returnType(Void.TYPE) }
                        add { paramCount(0);returnType(Boolean::class.java) }
                        add { paramTypes(View::class.java);returnType(Void.TYPE) }
                        add {
                            paramTypes(
                                Context::class.java, String::class.java,
                                Int::class.java, DialogInterface::class.java, Int::class.java
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
