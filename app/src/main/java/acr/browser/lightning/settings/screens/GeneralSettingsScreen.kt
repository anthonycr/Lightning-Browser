package acr.browser.lightning.settings.screens

import acr.browser.lightning.R
import acr.browser.lightning.constant.SCHEME_BLANK
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.preference.UserAgentChoice
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.search.SearchEngineChoice
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.Suggestions
import acr.browser.lightning.search.engine.BaseSearchEngine
import acr.browser.lightning.search.engine.CustomSearch
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import acr.browser.lightning.settings.framework.ToggleState
import acr.browser.lightning.utils.FileUtils
import android.os.Environment
import android.webkit.URLUtil
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GeneralSettingsScreen(
    resourceProvider: ResourceProvider,
    userPreferencesDataStore: UserPreferencesDataStore,
    searchEngineProvider: SearchEngineProvider,
    onUp: () -> Unit
) {
    val presenter: SettingsFrameworkPresenter = viewModel(
        key = "general",
        factory = SettingsFrameworkPresenter.Factory(
            settingsFrameworkState = {
                SettingsFrameworkState(
                    title = resourceProvider.stringResource(R.string.settings_general),
                    content = listOf(
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.block),
                            isChecked = { userPreferencesDataStore.blockImagesEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.blockImagesEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.save_data),
                            isChecked = { userPreferencesDataStore.saveDataEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.saveDataEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.java),
                            isChecked = { userPreferencesDataStore.javaScriptEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.javaScriptEnabled.set(it)
                                null
                            }
                        ),
                        ToggleState(
                            title = resourceProvider.stringResource(R.string.color_mode),
                            isChecked = { userPreferencesDataStore.colorModeEnabled.get() },
                            onToggle = {
                                userPreferencesDataStore.colorModeEnabled.set(it)
                                null
                            }
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.title_user_agent),
                            summary = {
                                userPreferencesDataStore.userAgentChoice.get()
                                    .asSummary(resourceProvider)
                            },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.title_user_agent),
                                values = UserAgentChoice.entries.map { it.asSummary(resourceProvider) },
                                selected = {
                                    UserAgentChoice.entries.indexOf(
                                        userPreferencesDataStore.userAgentChoice.get()
                                    )
                                },
                                onSelected = {
                                    when (it) {
                                        0, 1, 3 -> ClickableOnClick.Action {
                                            userPreferencesDataStore.userAgentChoice.set(
                                                UserAgentChoice.entries[it]
                                            )
                                        }

                                        else -> ClickableOnClick.Input(
                                            title = resourceProvider.stringResource(R.string.title_user_agent),
                                            hint = resourceProvider.stringResource(R.string.title_user_agent),
                                            currentValue = { userPreferencesDataStore.userAgentString.get() },
                                            onValueUpdated = { value ->
                                                ClickableOnClick.Action {
                                                    userPreferencesDataStore.userAgentChoice.set(
                                                        UserAgentChoice.CUSTOM
                                                    )
                                                    userPreferencesDataStore.userAgentString.set(
                                                        value
                                                    )
                                                }
                                            }
                                        )
                                    }
                                },
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.download),
                            summary = { userPreferencesDataStore.downloadDirectory.get() },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.download),
                                values = listOf(
                                    resourceProvider.stringResource(R.string.folder_default),
                                    resourceProvider.stringResource(R.string.folder_custom)
                                ),
                                selected = {
                                    if (userPreferencesDataStore.downloadDirectory.get()
                                            .contains(Environment.DIRECTORY_DOWNLOADS)
                                    ) {
                                        0
                                    } else {
                                        1
                                    }
                                },
                                onSelected = { index ->
                                    when (index) {
                                        0 -> ClickableOnClick.Action {
                                            userPreferencesDataStore.downloadDirectory.set(FileUtils.DEFAULT_DOWNLOAD_PATH)
                                        }

                                        else -> ClickableOnClick.Input(
                                            title = resourceProvider.stringResource(R.string.download),
                                            hint = "",
                                            currentValue = { userPreferencesDataStore.downloadDirectory.get() },
                                            onValueUpdated = {
                                                ClickableOnClick.Action {
                                                    userPreferencesDataStore.downloadDirectory.set(
                                                        it
                                                    )
                                                }
                                            }
                                        )
                                    }
                                },
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.home),
                            summary = {
                                resourceProvider.homePageUrlToDisplayTitle(userPreferencesDataStore.homepage.get())
                            },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.home),
                                values = listOf(
                                    resourceProvider.stringResource(R.string.action_homepage),
                                    resourceProvider.stringResource(R.string.action_blank),
                                    resourceProvider.stringResource(R.string.action_bookmarks),
                                    resourceProvider.stringResource(R.string.action_webpage),
                                ),
                                selected = {
                                    when (userPreferencesDataStore.homepage.get()) {
                                        SCHEME_HOMEPAGE -> 0
                                        SCHEME_BLANK -> 1
                                        SCHEME_BOOKMARKS -> 2
                                        else -> 3
                                    }
                                },
                                onSelected = { index ->
                                    when (index) {
                                        0 -> ClickableOnClick.Action {
                                            userPreferencesDataStore.homepage.set(SCHEME_HOMEPAGE)
                                        }

                                        1 -> ClickableOnClick.Action {
                                            userPreferencesDataStore.homepage.set(SCHEME_BLANK)
                                        }

                                        2 -> ClickableOnClick.Action {
                                            userPreferencesDataStore.homepage.set(SCHEME_BOOKMARKS)
                                        }

                                        else -> ClickableOnClick.Input(
                                            title = resourceProvider.stringResource(R.string.title_custom_homepage),
                                            hint = resourceProvider.stringResource(R.string.hint_url),
                                            currentValue = {
                                                if (!URLUtil.isAboutUrl(userPreferencesDataStore.homepage.get())) {
                                                    userPreferencesDataStore.homepage.get()
                                                } else {
                                                    "https://duckduckgo.com"
                                                }
                                            },
                                            onValueUpdated = {
                                                ClickableOnClick.Action {
                                                    userPreferencesDataStore.homepage.set(it)
                                                }
                                            }
                                        )
                                    }
                                },
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.title_search_engine),
                            summary = {
                                searchEngineProvider.provideSearchEngine()
                                    .getSearchEngineSummary(resourceProvider)
                            },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.title_search_engine),
                                values = searchEngineProvider.provideAllSearchEngines().map {
                                    resourceProvider.stringResource(it.titleRes)
                                },
                                selected = {
                                    SearchEngineChoice.entries.indexOf(userPreferencesDataStore.searchChoice.get())
                                },
                                onSelected = { index ->
                                    when (index) {
                                        0 -> ClickableOnClick.Input(
                                            title = resourceProvider.stringResource(R.string.search_engine_custom),
                                            hint = resourceProvider.stringResource(R.string.hint_url),
                                            currentValue = { userPreferencesDataStore.searchUrl.get() },
                                            onValueUpdated = {
                                                ClickableOnClick.Action {
                                                    userPreferencesDataStore.searchChoice.set(
                                                        SearchEngineChoice.CUSTOM
                                                    )
                                                    userPreferencesDataStore.searchUrl.set(it)
                                                }
                                            }
                                        )

                                        else -> ClickableOnClick.Action {
                                            userPreferencesDataStore.searchChoice.set(
                                                SearchEngineChoice.entries[index]
                                            )
                                        }
                                    }
                                },
                            )
                        ),
                        ClickableState(
                            title = resourceProvider.stringResource(R.string.search_suggestions),
                            summary = {
                                userPreferencesDataStore.searchSuggestionChoice.get()
                                    .searchSuggestionChoiceToTitle(resourceProvider)
                            },
                            onClick = ClickableOnClick.ItemSelector(
                                title = resourceProvider.stringResource(R.string.search_suggestions),
                                values = Suggestions.entries.map {
                                    it.searchSuggestionChoiceToTitle(
                                        resourceProvider
                                    )
                                },
                                selected = {
                                    Suggestions.entries.indexOf(userPreferencesDataStore.searchSuggestionChoice.get())
                                },
                                onSelected = {
                                    ClickableOnClick.Action {
                                        userPreferencesDataStore.searchSuggestionChoice.set(
                                            Suggestions.entries[it]
                                        )
                                    }
                                },
                            )
                        )
                    )
                )
            }

        )
    )
    SettingsFrameworkScreen(presenter, onUp)
}

private fun Suggestions.searchSuggestionChoiceToTitle(resourceProvider: ResourceProvider): String =
    when (this) {
        Suggestions.NONE -> resourceProvider.stringResource(R.string.search_suggestions_off)
        Suggestions.GOOGLE -> resourceProvider.stringResource(R.string.powered_by_google)
        Suggestions.DUCK -> resourceProvider.stringResource(R.string.powered_by_duck)
        Suggestions.BAIDU -> resourceProvider.stringResource(R.string.powered_by_baidu)
        Suggestions.NAVER -> resourceProvider.stringResource(R.string.powered_by_naver)
    }

private fun BaseSearchEngine.getSearchEngineSummary(resourceProvider: ResourceProvider): String {
    return if (this is CustomSearch) {
        this.queryUrl
    } else {
        resourceProvider.stringResource(this.titleRes)
    }
}

private fun ResourceProvider.homePageUrlToDisplayTitle(url: String): String = when (url) {
    SCHEME_HOMEPAGE -> stringResource(R.string.action_homepage)
    SCHEME_BLANK -> stringResource(R.string.action_blank)
    SCHEME_BOOKMARKS -> stringResource(R.string.action_bookmarks)
    else -> url
}

private fun UserAgentChoice.asSummary(resourceProvider: ResourceProvider) = when (this) {
    UserAgentChoice.DEFAULT -> resourceProvider.stringResource(R.string.agent_default)
    UserAgentChoice.DESKTOP -> resourceProvider.stringResource(R.string.agent_desktop)
    UserAgentChoice.MOBILE -> resourceProvider.stringResource(R.string.agent_mobile)
    UserAgentChoice.CUSTOM -> resourceProvider.stringResource(R.string.agent_custom)
}
