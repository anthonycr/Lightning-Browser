package acr.browser.lightning.settings.screens

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.source.HostsSourcePreference
import acr.browser.lightning.adblock.source.HostsSourceType
import acr.browser.lightning.adblock.source.selectedHostsSource
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsBottomSheetChooserState
import acr.browser.lightning.settings.SettingsBottomSheetInputState
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.adblock.HostsFileUpdater
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class AdBlockSettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val hostsFileUpdater: HostsFileUpdater,
    private val bloomFilterAdBlocker: BloomFilterAdBlocker,
) {
    val key = "ad_block"

    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
        resourceProvider.stringResource(R.string.settings_adblock), content = listOf(
            ToggleState(
                title = resourceProvider.stringResource(R.string.block_ads),
                isChecked = { userPreferencesDataStore.adBlockEnabled.get() },
                onToggle = {
                    userPreferencesDataStore.adBlockEnabled.set(it)
                    null
                }), if (BuildConfig.FULL_VERSION) {
                ClickableState(
                    title = resourceProvider.stringResource(R.string.block_ad_source),
                    summary = {
                        userPreferencesDataStore.selectedHostsSource().toSummary(resourceProvider)
                    },
                    onClick = ClickableOnClick.ItemSelector(
                        produceState = {
                            SettingsBottomSheetChooserState(
                                title = resourceProvider.stringResource(R.string.block_ad_source),
                                values = HostsSourcePreference.entries.map {
                                    it.displayText(resourceProvider)
                                },
                                selected = HostsSourcePreference.entries.indexOf(
                                    userPreferencesDataStore.hostsSource.get()
                                ),
                            )
                        },
                        onSelected = { index ->
                            when (val updatedPreference = HostsSourcePreference.entries[index]) {
                                HostsSourcePreference.DEFAULT -> ClickableOnClick.Action {
                                    userPreferencesDataStore.hostsSource.set(updatedPreference)
                                }

                                HostsSourcePreference.LOCAL -> ClickableOnClick.FileChooser(
                                    mimeType = TEXT_MIME_TYPE,
                                    onSelected = { uri ->
                                        val file = uri?.let { hostsFileUpdater.readTextFromUri(it) }
                                        if (file == null) {
                                            ClickableOnClick.Snackbar {
                                                userPreferencesDataStore.hostsSource.set(
                                                    HostsSourcePreference.DEFAULT
                                                )
                                                SettingsSnackBarState(
                                                    resourceProvider.stringResource(R.string.action_message_canceled)
                                                )
                                            }
                                        } else {
                                            ClickableOnClick.Action {
                                                userPreferencesDataStore.hostsSource.set(
                                                    updatedPreference
                                                )
                                                userPreferencesDataStore.hostsLocalFile.set(file.path)
                                                bloomFilterAdBlocker.populateAdBlockerFromDataSource(
                                                    forceRefresh = true
                                                )
                                            }
                                        }
                                    }
                                )

                                HostsSourcePreference.REMOTE -> ClickableOnClick.Input(
                                    produceState = {
                                        SettingsBottomSheetInputState(
                                            title = resourceProvider.stringResource(R.string.block_source_remote),
                                            currentValue = userPreferencesDataStore.hostsRemoteFile.get()
                                                .orEmpty(),
                                            hint = resourceProvider.stringResource(R.string.hint_url),
                                        )
                                    },
                                    onValueUpdated = { value ->
                                        val url = value.toHttpUrlOrNull()
                                        if (url == null) {
                                            ClickableOnClick.Snackbar {
                                                userPreferencesDataStore.hostsSource.set(
                                                    HostsSourcePreference.DEFAULT
                                                )
                                                SettingsSnackBarState(
                                                    resourceProvider.stringResource(R.string.problem_download)
                                                )
                                            }
                                        } else {
                                            ClickableOnClick.Action {
                                                userPreferencesDataStore.hostsSource.set(
                                                    updatedPreference
                                                )
                                                userPreferencesDataStore.hostsRemoteFile.set(value)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    )
                )
            } else {
                ClickableState(
                    enabled = { false },
                    title = resourceProvider.stringResource(R.string.block_ad_source),
                    summary = { resourceProvider.stringResource(R.string.block_ads_upsell_source) },
                    onClick = ClickableOnClick.None
                )
            },
            ClickableState(
                enabled = { userPreferencesDataStore.selectedHostsSource() is HostsSourceType.Remote },
                title = resourceProvider.stringResource(R.string.block_ad_refresh_now),
                summary = { resourceProvider.stringResource(R.string.block_ad_remote_refresh_frequency_description) },
                onClick = ClickableOnClick.Action {
                    bloomFilterAdBlocker.populateAdBlockerFromDataSource(forceRefresh = true)
                }
            )
        )
    )
}

@Composable
fun AdBlockSettingsScreen(
    adBlockSettingsScreen: AdBlockSettingsScreen,
    onUp: () -> Unit
) {
    SettingsFrameworkScreen(
        viewModel(
            key = adBlockSettingsScreen.key,
            factory = SettingsFrameworkPresenter.Factory(
                settingsFrameworkState = { adBlockSettingsScreen.createSettingsFrameworkState() }
            )
        ),
        onUp
    )
}

private fun HostsSourceType.toSummary(resourceProvider: ResourceProvider): String = when (this) {
    HostsSourceType.Default -> resourceProvider.stringResource(R.string.block_source_default)
    is HostsSourceType.Local -> resourceProvider.stringResource(
        R.string.block_source_local_description, file.path
    )

    is HostsSourceType.Remote -> resourceProvider.stringResource(
        R.string.block_source_remote_description, httpUrl
    )
}

private fun HostsSourcePreference.displayText(resourceProvider: ResourceProvider): String =
    when (this) {
        HostsSourcePreference.DEFAULT -> resourceProvider.stringResource(R.string.block_source_default)
        HostsSourcePreference.LOCAL -> resourceProvider.stringResource(R.string.block_source_local)
        HostsSourcePreference.REMOTE -> resourceProvider.stringResource(R.string.block_source_remote)
    }

private const val TEXT_MIME_TYPE = "text/*"
