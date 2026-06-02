/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.compose.BrowserTheme
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.settings.SettingsNavigation
import acr.browser.lightning.settings.SettingsScreen
import acr.browser.lightning.settings.screens.AboutSettingsScreen
import acr.browser.lightning.settings.screens.AdBlockSettingsScreen
import acr.browser.lightning.settings.screens.AdvancedSettingsScreen
import acr.browser.lightning.settings.screens.BookmarkSettingsScreen
import acr.browser.lightning.settings.screens.DebugSettingsScreen
import acr.browser.lightning.settings.screens.DisplaySettingsScreen
import acr.browser.lightning.settings.screens.GeneralSettingsScreen
import acr.browser.lightning.settings.screens.PrivacySettingsScreen
import android.content.Intent
import android.os.Bundle
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

class SettingsActivity : ThemableActivity() {


    @Inject internal lateinit var buildInfo: BuildInfo

    @Inject internal lateinit var aboutSettingsScreen: AboutSettingsScreen
    @Inject internal lateinit var adBlockSettingsScreen: AdBlockSettingsScreen
    @Inject internal lateinit var advancedSettingsScreen: AdvancedSettingsScreen
    @Inject internal lateinit var bookmarkSettingsScreen: BookmarkSettingsScreen
    @Inject internal lateinit var debugSettingsScreen: DebugSettingsScreen
    @Inject internal lateinit var displaySettingsScreen: DisplaySettingsScreen
    @Inject internal lateinit var generalSettingsScreen: GeneralSettingsScreen
    @Inject internal lateinit var privacySettingsScreen: PrivacySettingsScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)

        setContent {
            BrowserTheme {
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
                            adBlockSettingsScreen
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.GENERAL -> GeneralSettingsScreen(
                            generalSettingsScreen
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.BOOKMARK -> BookmarkSettingsScreen(
                            bookmarkSettingsScreen
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.DISPLAY -> DisplaySettingsScreen(
                            displaySettingsScreen
                        ) { navigationState = SettingsNavigation.ROOT }

                        SettingsNavigation.PRIVACY -> PrivacySettingsScreen(
                            privacySettingsScreen
                        ) {
                            navigationState = SettingsNavigation.ROOT
                        }

                        SettingsNavigation.ADVANCED -> AdvancedSettingsScreen(
                            advancedSettingsScreen
                        ) {
                            navigationState = SettingsNavigation.ROOT
                        }

                        SettingsNavigation.ABOUT -> AboutSettingsScreen(aboutSettingsScreen) {
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
                            debugSettingsScreen
                        ) {
                            navigationState = SettingsNavigation.ROOT
                        }
                    }
                }
            }
        }
    }
}
