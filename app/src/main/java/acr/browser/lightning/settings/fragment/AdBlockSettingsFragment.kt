package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.source.HostsSourcePreference
import acr.browser.lightning.adblock.source.HostsSourceType
import acr.browser.lightning.adblock.source.selectedHostsSource
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.concurrency.AppCoroutineScope
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.toast
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.preference.datastore.getUnsafe
import acr.browser.lightning.preference.datastore.setUnsafe
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Settings for the ad block mechanic.
 */
class AdBlockSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferencesDataStore: UserPreferencesDataStore
    @Inject internal lateinit var bloomFilterAdBlocker: BloomFilterAdBlocker
    @Inject internal lateinit var appCoroutineScope: AppCoroutineScope
    @Inject internal lateinit var coroutineDispatchers: CoroutineDispatchers

    private var recentSummaryUpdater: SummaryUpdater? = null
    private var forceRefreshHostsPreference: Preference? = null

    override fun providePreferencesXmlResource(): Int = R.xml.preference_ad_block

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        injector.inject(this)

        togglePreference(
            preference = "cb_block_ads",
            isChecked = userPreferencesDataStore.adBlockEnabled.getUnsafe(),
            onCheckChange = { userPreferencesDataStore.adBlockEnabled.setUnsafe(it) }
        )

        clickableDynamicPreference(
            preference = "preference_hosts_source",
            isEnabled = BuildConfig.FULL_VERSION,
            summary = if (BuildConfig.FULL_VERSION) {
                userPreferencesDataStore.selectedHostsSource().toSummary()
            } else {
                getString(R.string.block_ads_upsell_source)
            },
            onClick = ::showHostsSourceChooser
        )

        forceRefreshHostsPreference = clickableDynamicPreference(
            preference = "preference_hosts_refresh_force",
            isEnabled = isRefreshHostsEnabled(),
            onClick = {
                bloomFilterAdBlocker.populateAdBlockerFromDataSource(forceRefresh = true)
            }
        )
    }

    private fun updateRefreshHostsEnabledStatus() {
        forceRefreshHostsPreference?.isEnabled = isRefreshHostsEnabled()
    }

    private fun isRefreshHostsEnabled() =
        userPreferencesDataStore.selectedHostsSource() is HostsSourceType.Remote

    private fun HostsSourceType.toSummary(): String = when (this) {
        HostsSourceType.Default -> getString(R.string.block_source_default)
        is HostsSourceType.Local -> getString(R.string.block_source_local_description, file.path)
        is HostsSourceType.Remote -> getString(R.string.block_source_remote_description, httpUrl)
    }

    private fun showHostsSourceChooser(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showListChoices(
            requireActivity(),
            R.string.block_ad_source,
            DialogItem(
                title = R.string.block_source_default,
                isConditionMet = userPreferencesDataStore.selectedHostsSource() == HostsSourceType.Default,
                onClick = {
                    userPreferencesDataStore.hostsSource.setUnsafe(HostsSourcePreference.DEFAULT)
                    summaryUpdater.updateSummary(
                        userPreferencesDataStore.selectedHostsSource().toSummary()
                    )
                    updateForNewHostsSource()
                }
            ),
            DialogItem(
                title = R.string.block_source_local,
                isConditionMet = userPreferencesDataStore.selectedHostsSource() is HostsSourceType.Local,
                onClick = {
                    showFileChooser(summaryUpdater)
                }
            ),
            DialogItem(
                title = R.string.block_source_remote,
                isConditionMet = userPreferencesDataStore.selectedHostsSource() is HostsSourceType.Remote,
                onClick = {
                    showUrlChooser(summaryUpdater)
                }
            )
        )
    }

    private fun showFileChooser(summaryUpdater: SummaryUpdater) {
        this.recentSummaryUpdater = summaryUpdater
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = TEXT_MIME_TYPE
        }

        startActivityForResult(intent, FILE_REQUEST_CODE)
    }

    private fun showUrlChooser(summaryUpdater: SummaryUpdater) {
        BrowserDialog.showEditText(
            requireActivity(),
            title = R.string.block_source_remote,
            hint = R.string.hint_url,
            currentText = userPreferencesDataStore.hostsRemoteFile.getUnsafe(),
            action = R.string.action_ok,
            textInputListener = {
                val url = it.toHttpUrlOrNull()
                    ?: return@showEditText run { activity?.toast(R.string.problem_download) }
                userPreferencesDataStore.hostsSource.setUnsafe(HostsSourcePreference.REMOTE)
                userPreferencesDataStore.hostsRemoteFile.setUnsafe(it)
                summaryUpdater.updateSummary(it)
                updateForNewHostsSource()
            }
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.also { uri ->
                    appCoroutineScope.launch {
                        val file = readTextFromUri(uri)
                        if (file == null) {
                            activity?.toast(R.string.action_message_canceled)
                        } else {
                            userPreferencesDataStore.hostsSource.setUnsafe(HostsSourcePreference.LOCAL)
                            userPreferencesDataStore.hostsLocalFile.setUnsafe(file.path)
                            recentSummaryUpdater?.updateSummary(
                                userPreferencesDataStore.selectedHostsSource().toSummary()
                            )
                            updateForNewHostsSource()
                        }
                    }
                }
            } else {
                activity?.toast(R.string.action_message_canceled)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateForNewHostsSource() {
        bloomFilterAdBlocker.populateAdBlockerFromDataSource(forceRefresh = true)
        updateRefreshHostsEnabledStatus()
    }

    private suspend fun readTextFromUri(uri: Uri): File? = withContext(coroutineDispatchers.io) {
        val externalFilesDir = activity?.getExternalFilesDir("")
            ?: return@withContext null
        val inputStream = activity?.contentResolver?.openInputStream(uri)
            ?: return@withContext null

        try {
            val outputFile = File(externalFilesDir, AD_HOSTS_FILE)

            val input = inputStream.source()
            val output = outputFile.sink().buffer()
            output.writeAll(input)
            return@withContext outputFile
        } catch (exception: IOException) {
            return@withContext null
        }
    }

    companion object {
        private const val FILE_REQUEST_CODE = 100
        private const val AD_HOSTS_FILE = "local_hosts.txt"
        private const val TEXT_MIME_TYPE = "text/*"
    }
}
