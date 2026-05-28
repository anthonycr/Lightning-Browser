package acr.browser.lightning.settings.screens

import acr.browser.lightning.Capabilities
import acr.browser.lightning.R
import acr.browser.lightning.browser.search.SearchBoxDisplayChoice
import acr.browser.lightning.browser.view.RenderingMode
import acr.browser.lightning.constant.TEXT_ENCODINGS
import acr.browser.lightning.isSupported
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.SettingsBottomSheetChooserState
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.inject.Inject

class AdvancedSettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val userPreferencesDataStore: UserPreferencesDataStore,
) {
    val key = "advanced"

    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
        title = resourceProvider.stringResource(R.string.settings_advanced),
        content = listOf(
            ToggleState(
                title = resourceProvider.stringResource(R.string.window),
                summary = { resourceProvider.stringResource(R.string.recommended) },
                isChecked = { userPreferencesDataStore.popupsEnabled.get() },
                onToggle = {
                    userPreferencesDataStore.popupsEnabled.set(it)
                    null
                }
            ),
            ToggleState(
                title = resourceProvider.stringResource(R.string.cookies),
                summary = { resourceProvider.stringResource(R.string.recommended) },
                isChecked = { userPreferencesDataStore.cookiesEnabled.get() },
                onToggle = {
                    userPreferencesDataStore.cookiesEnabled.set(it)
                    null
                }
            ),
            ToggleState(
                enabled = { !Capabilities.FULL_INCOGNITO.isSupported },
                title = resourceProvider.stringResource(R.string.incognito_cookies),
                summary = if (Capabilities.FULL_INCOGNITO.isSupported) {
                    { resourceProvider.stringResource(R.string.incognito_cookies_pie) }
                } else {
                    null
                },
                isChecked = {
                    if (Capabilities.FULL_INCOGNITO.isSupported) {
                        userPreferencesDataStore.cookiesEnabled.get()
                    } else {
                        userPreferencesDataStore.incognitoCookiesEnabled.get()
                    }
                },
                onToggle = {
                    userPreferencesDataStore.incognitoCookiesEnabled.set(it)
                    null
                }
            ),
            ToggleState(
                title = resourceProvider.stringResource(R.string.restore),
                isChecked = { userPreferencesDataStore.restoreLostTabsEnabled.get() },
                onToggle = {
                    userPreferencesDataStore.restoreLostTabsEnabled.set(it)
                    null
                }
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.text_encoding),
                summary = { userPreferencesDataStore.textEncoding.get() },
                onClick = ClickableOnClick.ItemSelector(
                    produceState = {
                        SettingsBottomSheetChooserState(
                            title = resourceProvider.stringResource(R.string.text_encoding),
                            values = TEXT_ENCODINGS.toList(),
                            selected = TEXT_ENCODINGS.indexOf(userPreferencesDataStore.textEncoding.get())
                        )
                    },
                    onSelected = {
                        ClickableOnClick.Action {
                            userPreferencesDataStore.textEncoding.set(TEXT_ENCODINGS[it])
                        }
                    }
                )
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.rendering_mode),
                summary = {
                    userPreferencesDataStore.renderingMode.get().toDisplayString(resourceProvider)
                },
                onClick = ClickableOnClick.ItemSelector(
                    produceState = {
                        SettingsBottomSheetChooserState(
                            title = resourceProvider.stringResource(R.string.rendering_mode),
                            values = RenderingMode.entries.map {
                                it.toDisplayString(resourceProvider)
                            },
                            selected = RenderingMode.entries.indexOf(
                                userPreferencesDataStore.renderingMode.get()
                            )
                        )
                    },
                    onSelected = {
                        ClickableOnClick.Action {
                            userPreferencesDataStore.renderingMode.set(RenderingMode.entries[it])
                        }
                    }
                )
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.url_contents),
                summary = {
                    userPreferencesDataStore.urlBoxContentChoice.get()
                        .toDisplayString(resourceProvider)
                },
                onClick = ClickableOnClick.ItemSelector(
                    produceState = {
                        SettingsBottomSheetChooserState(
                            title = resourceProvider.stringResource(R.string.url_contents),
                            values = SearchBoxDisplayChoice.entries.map {
                                it.toDisplayString(resourceProvider)
                            },
                            selected = SearchBoxDisplayChoice.entries.indexOf(
                                userPreferencesDataStore.urlBoxContentChoice.get()
                            )
                        )
                    },
                    onSelected = {
                        ClickableOnClick.Action {
                            userPreferencesDataStore.urlBoxContentChoice.set(
                                SearchBoxDisplayChoice.entries[it]
                            )
                        }
                    }
                )
            )
        )
    )
}

@Composable
fun AdvancedSettingsScreen(
    advancedSettingsScreen: AdvancedSettingsScreen,
    onUp: () -> Unit
) {
    SettingsFrameworkScreen(
        viewModel(
            key = advancedSettingsScreen.key,
            factory = SettingsFrameworkPresenter.Factory(
                settingsFrameworkState = {
                    advancedSettingsScreen.createSettingsFrameworkState()
                }
            )
        ),
        onUp
    )
}

private fun SearchBoxDisplayChoice.toDisplayString(resourceProvider: ResourceProvider): String {
    val stringArray = resourceProvider.stringArrayResource(R.array.url_content_array)
    return when (this) {
        SearchBoxDisplayChoice.DOMAIN -> stringArray[0]
        SearchBoxDisplayChoice.URL -> stringArray[1]
        SearchBoxDisplayChoice.TITLE -> stringArray[2]
    }
}

private fun RenderingMode.toDisplayString(resourceProvider: ResourceProvider): String =
    resourceProvider.stringResource(
        when (this) {
            RenderingMode.NORMAL -> R.string.name_normal
            RenderingMode.INVERTED -> R.string.name_inverted
            RenderingMode.GRAYSCALE -> R.string.name_grayscale
            RenderingMode.INVERTED_GRAYSCALE -> R.string.name_inverted_grayscale
            RenderingMode.INCREASE_CONTRAST -> R.string.name_increase_contrast
        }
    )
