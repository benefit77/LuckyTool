package com.luckyzyx.luckytool.hook.scopes.market

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveMarketUpdateDownloadPageAppRecommend(val dexKitBridge: DexKitBridge) :
    YukiBaseHooker() {
    override fun onHook() {
        val cardDto = "com.heytap.cdo.card.domain.dto.CardDto"
        val imageLoader = "com.nearme.imageloader.ImageLoader"

        //Source CardDataProcessor
        val appUpdateItemHolder = dexKitBridge.findClass {
            matcher {
                addFieldForName("mDataUtil")
                addMethod {
                    name("processData")
//                    paramTypes(ListClass, IntType, null)
                    returnType(List::class.java)
                }
            }
        }.apply {
            checkDataList("RemoveMarketUpdateDownloadPageAppRecommend")
            single().name.toClass().resolve().apply {
                firstMethod { name = "processData" }.hook {
                    after {
                        result<ArrayList<Any>>()?.clear()
                    }
                }
            }
        }

        //Source APPUpdateItemHolder list_item_product_upgrade
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(ViewGroup::class.java)
                    addForType(TextView::class.java)
                    addForType(imageLoader)
                }
                methods {
                    add {
                        paramTypes(Context::class.java, Int::class.java)
                        returnType(Void.TYPE)
                    }
                    add {
                        paramTypes(
                            Context::class.java,
                            String::class.java,
                            Int::class.java,
                            Int::class.java
                        )
                        returnType(Void.TYPE)
                    }
                    add {
                        paramTypes(View::class.java, Boolean::class.java)
                        returnType(Void.TYPE)
                    }
                    add {
                        paramCount(0)
                        returnType(View::class.java)
                    }
                    add {
                        paramTypes(LayoutInflater::class.java);returnType(View::class.java)
                    }
                }
            }
        }
        if (appUpdateItemHolder.isNullOrEmpty()) return@onHook
        appUpdateItemHolder.apply {
            checkDataList("RemoveMarketUpdatePageAppRecommend APPUpdateItemHolder")
            single().name.toClass().resolve().apply {
                optional()
                firstMethodOrNull {
                    parameters(
                        cardDto, String::class, VagueType,
                        Map::class, Boolean::class, Long::class
                    )
                    returnType(Void.TYPE)
                    superclass()
                }?.hook {
                    intercept()
                }
            }
        }

        //Source AppUpdateFragmentV2
        "com.heytap.cdo.client.ui.upgrademgrv2.AppUpdateFragmentV2".toClassOrNull()?.let {
            dexKitBridge.findClass {
                matcher {
                    className(it.name)
                }
            }.apply {
                checkDataList("RemoveMarketUpdatePageAppRecommend AppUpdateFragmentV2")
                findMethod {
                    matcher {
                        paramTypes(List::class.java)
                        addInvoke {
                            paramTypes(Context::class.java, Float::class.java)
                            returnType(Int::class.java)
                        }
                        usingNumbers(114.0F)
                    }
                }.apply {
                    checkDataList("RemoveMarketUpdatePageAppRecommend addDataAndNotifyChanged")
                    it.resolve().firstMethod {
                        name = single().name
                        parameters(List::class)
                    }.hook {
                        before {
                            args().first().cast<java.util.ArrayList<Any>>()?.clear()
                        }
                    }
                }

                findMethod {
                    matcher {
                        paramTypes(Boolean::class.java)
                        usingNumbers(0, 300L)
                        addUsingField { type(ValueAnimator::class.java) }
                        addInvoke { paramCount(0) }
                        usingStrings("mRecommendUpdateContainer", "mNormalUpdateContainer")
                    }
                }.apply {
                    checkDataList("RemoveMarketUpdatePageAppRecommend AutoScrollWhenUpdateAll")
                    it.resolve().firstMethod {
                        name = single().name
                        parameters(Boolean::class)
                    }.hook {
                        intercept()
                    }
                }
            }
        }
    }
}
