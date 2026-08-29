package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

/**
 * 禁用应用跳转确认弹框。
 *
 * 该弹框由 ColorOS 的 OplusAppStartConfirmManager 触发，
 * 拦截 checkMaliciousIntercept 后系统会直接继续原跳转。
 */
@Obfuscate
object DisableAppJumpConfirmDialog : YukiBaseHooker() {
    override fun onHook() {
        // Source OplusAppStartConfirmManager
        "com.android.server.wm.OplusAppStartConfirmManager".toClass().resolve().apply {
            firstMethod { name = "checkMaliciousIntercept" }.hook {
                intercept()
            }
        }
    }
}
