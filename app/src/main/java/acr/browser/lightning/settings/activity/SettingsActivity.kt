/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.compose.AppTheme
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.preference.DeveloperPreferenceStore
import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.settings.SettingsNavigation
import acr.browser.lightning.settings.SettingsScreen
import acr.browser.lightning.settings.adblock.HostsFileUpdater
import acr.browser.lightning.settings.screens.AboutSettingsScreen
import acr.browser.lightning.settings.screens.AdBlockSettingsScreen
import acr.browser.lightning.settings.screens.AdvancedSettingsScreen
import acr.browser.lightning.settings.screens.BookmarkSettingsScreen
import acr.browser.lightning.settings.screens.DebugSettingsScreen
import acr.browser.lightning.settings.screens.DisplaySettingsScreen
import acr.browser.lightning.settings.screens.GeneralSettingsScreen
import acr.browser.lightning.settings.screens.PrivacySettingsScreen
import acr.browser.lightning.utils.WebUtils
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import javax.inject.Inject

class SettingsActivity : ComponentActivity() {

    @Inject internal lateinit var userPreferencesDataStore: UserPreferencesDataStore
    @Inject internal lateinit var developerPreferenceStore: DeveloperPreferenceStore
    @Inject internal lateinit var resourceProvider: ResourceProvider
    @Inject internal lateinit var bloomFilterAdBlocker: BloomFilterAdBlocker
    @Inject internal lateinit var hostsFileUpdater: HostsFileUpdater
    @Inject internal lateinit var searchEngineProvider: SearchEngineProvider
    @Inject internal lateinit var webUtils: WebUtils
    @Inject internal lateinit var bookmarkExporter: BookmarkExporter
    @Inject internal lateinit var bookmarkRepository: BookmarkRepository
    @Inject internal lateinit var buildInfo: BuildInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                var navigationState by remember { mutableStateOf(SettingsNavigation.ROOT) }
                AnimatedContent(navigationState, transitionSpec = {
                    if (targetState == SettingsNavigation.ROOT) {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + slideInHorizontally(
                            animationSpec = tween(220, delayMillis = 90),
                            initialOffsetX = { -it / 2 }))
                            .togetherWith(
                                (fadeOut(animationSpec = tween(220, delayMillis = 90)))
                            )
                    } else {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + slideInHorizontally(
                            animationSpec = tween(220, delayMillis = 90),
                            initialOffsetX = { it / 2 }))
                            .togetherWith(
                                (fadeOut(animationSpec = tween(220, delayMillis = 90)))
                            )
                    }
                }) { state ->
                    when (state) {
                        SettingsNavigation.ROOT -> SettingsScreen(buildInfo) {
                            navigationState = it
                        }

                        SettingsNavigation.ADBLOCK -> AdBlockSettingsScreen(
                            resourceProvider,
                            userPreferencesDataStore,
                            bloomFilterAdBlocker,
                            hostsFileUpdater,
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.GENERAL -> GeneralSettingsScreen(
                            resourceProvider,
                            userPreferencesDataStore,
                            searchEngineProvider,
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.BOOKMARK -> BookmarkSettingsScreen(
                            resourceProvider,
                            bookmarkExporter,
                            bookmarkRepository
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.DISPLAY -> DisplaySettingsScreen(
                            userPreferencesDataStore,
                            resourceProvider,
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.PRIVACY -> PrivacySettingsScreen(
                            resourceProvider,
                            userPreferencesDataStore,
                            webUtils
                        ) {
                            navigationState = SettingsNavigation.ROOT
                        }

                        SettingsNavigation.ADVANCED -> AdvancedSettingsScreen(
                            userPreferencesDataStore,
                            resourceProvider
                        ) {
                            navigationState = SettingsNavigation.ROOT
                        }

                        SettingsNavigation.ABOUT -> AboutSettingsScreen(resourceProvider) {
                            navigationState = SettingsNavigation.ROOT
                        }

                        SettingsNavigation.FAQ -> {
                            val current = LocalContext.current
                            LaunchedEffect("faq") {
                                current.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "http://acrdevelopment.org/lightning/faq".toUri()
                                    )
                                )
                            }
                        }

                        SettingsNavigation.DEBUG -> DebugSettingsScreen(
                            resourceProvider,
                            developerPreferenceStore
                        ) {
                            navigationState = SettingsNavigation.ROOT
                        }
                    }
                }
            }
        }
    }
}
