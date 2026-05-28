package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.browser.tab.WebViewFactory
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsSnackBarState
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import acr.browser.lightning.utils.WebUtils
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PrivacySettingsScreen(
    resourceProvider: ResourceProvider,
    userPreferencesDataStore: UserPreferencesDataStore,
    webUtils: WebUtils,
    onUp: () -> Unit
) {
    val presenter: SettingsFrameworkPresenter = viewModel(
        key = "privacy",
        factory = SettingsFrameworkPresenter.Factory(
            settingsFrameworkState = {
                SettingsFrameworkState(
                    title = resourceProvider.stringResource(R.string.settings_privacy),
                    content = listOf(
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.location),
                            isChecked = { userPreferencesDataStore.locationEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.locationEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.third_party),
                            isChecked = { userPreferencesDataStore.blockThirdPartyCookiesEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.blockThirdPartyCookiesEnabled.get()
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.do_not_track),
                            isChecked = { userPreferencesDataStore.doNotTrackEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.doNotTrackEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.remove_identifying_headers),
                            summary = { "${WebViewFactory.HEADER_REQUESTED_WITH}, ${WebViewFactory.HEADER_WAP_PROFILE}" },
                            isChecked = { userPreferencesDataStore.removeIdentifyingHeadersEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.removeIdentifyingHeadersEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.webrtc_support),
                            isChecked = { userPreferencesDataStore.webRtcEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.webRtcEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.cache),
                            isChecked = { userPreferencesDataStore.clearCacheExit.get() },
                            onToggle = {
                                userPreferencesDataStore.clearCacheExit.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.clear_history_exit),
                            isChecked = { userPreferencesDataStore.clearHistoryExitEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.clearHistoryExitEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.clear_cookies_exit),
                            isChecked = { userPreferencesDataStore.clearCookiesExitEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.clearCookiesExitEnabled.get()
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.clear_web_storage_exit),
                            isChecked = { userPreferencesDataStore.clearWebStorageExitEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.clearWebStorageExitEnabled.set(it)
                                null
                            }
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.clear_cache),
                            onClick = ClickableOnClick.Snackbar {
                                webUtils.clearCache()
                                SettingsSnackBarState(resourceProvider.stringResource(R.string.message_cache_cleared))
                            }
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.title_clear_history),
                            onClick = ClickableOnClick.Confirmation(
                                title = resourceProvider.stringResource(R.string.title_clear_history),
                                message = resourceProvider.stringResource(R.string.dialog_history),
                                negativeAction = resourceProvider.stringResource(R.string.no),
                                positiveAction = resourceProvider.stringResource(R.string.yes),
                                onConfirmed = {
                                    if (it) {
                                        ClickableOnClick.Snackbar {
                                            webUtils.clearHistory()
                                            SettingsSnackBarState(resourceProvider.stringResource(R.string.message_clear_history))
                                        }
                                    } else {
                                        ClickableOnClick.Action {}
                                    }
                                }
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.clear_cookies),
                            onClick = ClickableOnClick.Confirmation(
                                title = resourceProvider.stringResource(R.string.title_clear_cookies),
                                message = resourceProvider.stringResource(R.string.dialog_cookies),
                                negativeAction = resourceProvider.stringResource(R.string.no),
                                positiveAction = resourceProvider.stringResource(R.string.yes),
                                onConfirmed = {
                                    if (it) {
                                        ClickableOnClick.Snackbar {
                                            webUtils.clearCookies()
                                            SettingsSnackBarState(resourceProvider.stringResource(R.string.message_cookies_cleared))
                                        }
                                    } else {
                                        ClickableOnClick.Action {}
                                    }
                                }
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.clear_web_storage),
                            onClick = ClickableOnClick.Snackbar {
                                webUtils.clearWebStorage()
                                SettingsSnackBarState(resourceProvider.stringResource(R.string.message_web_storage_cleared))
                            }
                        )
                    )
                )
            }
        )
    )
    SettingsFrameworkScreen(presenter, onUp)
}
