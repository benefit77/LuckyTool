package com.luckyzyx.luckytool.ui.fragment.scopes.apps

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.betterandroid.ui.extension.view.toast
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.UserService
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.GlobalKeyValue
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class OplusSecuritypPermission : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("com.oplus.securitypermission")

    override val isEnableRestartMenu: Boolean = true

    override val isEnableOpenMenu: Boolean = false

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.oplusSecuritypPermission

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "com.oplus.securitypermission"
            setPrefsIconRes(key) { resource, show ->
                icon = resource
                isIconSpaceReserved = show
            }
            title = AppUtils(context).getAppLabel(key)
            summary = arraySummaryLine(
                getString(R.string.app_start_dialog_use_old_version),
                getString(R.string.enable_always_allow_app_start_dialog),
            )
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_malicious_app_intercept)
                summary = getString(R.string.need_restart_system)
                key = "disable_malicious_app_intercept"
                setDefaultValue(false)
                isVisible = osCode >= 38
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_app_jump_confirm_dialog)
                summary = arraySummaryLine(
                    getString(R.string.disable_app_jump_confirm_dialog_summary),
                    getString(R.string.need_restart_system)
                )
                key = GlobalKeyValue.keyDisableAppJumpConfirmDialog
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.app_start_dialog_use_old_version)
                key = "app_start_dialog_use_old_version"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    findPreference<SwitchPreference>("enable_always_allow_app_start_dialog")
                        ?.isEnabled = !(newValue as Boolean)
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_always_allow_app_start_dialog)
                summary = getString(R.string.need_restart_system)
                key = "enable_always_allow_app_start_dialog"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "enable_always_allow_app_start_dialog", false)) {
                add(Preference(this@loadPreferences).apply {
                    title = getString(R.string.remove_always_allow_app_start_list)
                    key = "remove_always_allow_app_start_list"
                    isPersistent = false
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        UserService.get(context) {
                            val users = it?.users
                            if (users.isNullOrEmpty()) {
                                toast("userId is null")
                                return@get
                            }

                            val items = arrayListOf(Pair("All", -1))
                            users.forEach { info ->
                                val pair = Pair("${info.name} [${info.id}]", info.id)
                                items.add(pair)
                            }

                            var curUserId = ArrayList(users.map { info -> info.id })
                            MaterialAlertDialogBuilder(context, dialogCentered).apply {
                                setTitle(title)
                                setSingleChoiceItems(
                                    items.map { i -> i.first }.toTypedArray(), 0,
                                ) { _, which ->
                                    curUserId = when (which) {
                                        0 -> ArrayList(users.map { info -> info.id })
                                        else -> arrayListOf(items[which].second)
                                    }
                                }
                                setNeutralButton(android.R.string.cancel, null)
                                setPositiveButton(android.R.string.ok) { _, _ ->
                                    sendPrefsValue("android", key, curUserId)
                                }
                            }.show()
                        }
                        true
                    }
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.auto_unlock_app_ecm_permission_restrict)
                key = "auto_unlock_app_ecm_permission_restrict"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
        }
    }
}
