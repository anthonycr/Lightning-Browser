package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.adblock.source.HostsSourceType
import acr.browser.lightning.adblock.source.selectedHostsSource
import acr.browser.lightning.adblock.source.toPreferenceIndex
import acr.browser.lightning.di.injector
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.preference.UserPreferences
import android.os.Bundle
import javax.inject.Inject

/**
 * Settings for the ad block mechanic.
 */
class AdBlockSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferences: UserPreferences

    override fun providePreferencesXmlResource(): Int = R.xml.preference_ad_block

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        checkBoxPreference(
            preference = "cb_block_ads",
            isChecked = userPreferences.adBlockEnabled,
            onCheckChange = { userPreferences.adBlockEnabled = it }
        )

        clickableDynamicPreference(
            preference = "preference_hosts_source",
            isEnabled = BuildConfig.FULL_VERSION,
            summary = if (BuildConfig.FULL_VERSION) {
                userPreferences.selectedHostsSource().toSummary()
            } else {
                getString(R.string.block_ads_upsell_source)
            },
            onClick = ::showHostsSourceChooser
        )

        clickableDynamicPreference(
            preference = "preference_hosts_refresh_frequency",
            isEnabled = userPreferences.selectedHostsSource() is HostsSourceType.Remote,
            // TODO implement changing summary and enable/disable
            onClick = {}
        )

        clickableDynamicPreference(
            preference = "preference_hosts_refresh_force",
            isEnabled = userPreferences.selectedHostsSource() != HostsSourceType.Default,
            // TODO implement
            onClick = {}
        )
    }

    private fun HostsSourceType.toSummary(): String = when (this) {
        HostsSourceType.Default -> getString(R.string.block_source_default)
        is HostsSourceType.Local -> getString(R.string.block_source_local_description, file.path)
        is HostsSourceType.Remote -> getString(R.string.block_source_remote_description, httpUrl)
    }

    private fun showHostsSourceChooser(summaryUpdater: SummaryUpdater) {
        BrowserDialog.show(
            activity,
            R.string.block_ad_source,
            DialogItem(
                title = R.string.block_source_default,
                onClick = {
                    userPreferences.hostsSource = HostsSourceType.Default.toPreferenceIndex()
                    summaryUpdater.updateSummary(userPreferences.selectedHostsSource().toSummary())
                }
            ),
            DialogItem(
                title = R.string.block_source_local,
                // TODO implement
                isConditionMet = false,
                onClick = {}
            ),
            DialogItem(
                title = R.string.block_source_remote,
                // TODO implement
                isConditionMet = false,
                onClick = {}
            )
        )
    }

    private fun showFileChooser(summaryUpdater: SummaryUpdater) {
        // TODO implement
    }

    private fun showUrlChooser(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showEditText(
            activity,
            title = R.string.block_source_remote,
            hint = R.string.hint_url,
            currentText = userPreferences.hostsRemoteFile,
            action = R.string.action_ok,
            textInputListener = {
                // TODO implement
            }
        )
    }
}
