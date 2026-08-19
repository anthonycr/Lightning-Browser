/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.activity

import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.compose.BrowserTheme
import acr.browser.lightning.compose.slideInFrom
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.di.injector
import acr.browser.lightning.settings.SettingsNavigation
import acr.browser.lightning.settings.SettingsScreen
import acr.browser.lightning.settings.SettingsScreenStateProvider
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.inject.Inject

class SettingsActivity : ThemableActivity() {

    @Inject internal lateinit var buildInfo: BuildInfo
    @Inject internal lateinit var settingsScreenStateProvider: SettingsScreenStateProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.settingsComponentBuilder()
            .activity(this)
            .build()
            .inject(this)

        super.onCreate(savedInstanceState)

        setContent {
            BrowserTheme(appThemeStateFlow) {
                var navigationState by remember { mutableStateOf(SettingsNavigation.ROOT) }
                AnimatedContent(navigationState, transitionSpec = {
                    if (targetState == SettingsNavigation.ROOT) {
                        slideInFrom { -it / 2 }
                    } else {
                        slideInFrom { it / 2 }
                    }
                }) { state ->
                    when (state) {
                        SettingsNavigation.ROOT -> SettingsScreen(
                            useBlackStatusBarStateFlow,
                            buildInfo
                        ) {
                            navigationState = it
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

                        else -> {
                            val frameworkState = settingsScreenStateProvider.provideState(state)
                            SettingsFrameworkScreen(
                                useBlackStatusBarStateFlow,
                                viewModel(
                                    key = state.name,
                                    factory = SettingsFrameworkPresenter.Factory(
                                        settingsFrameworkState = { frameworkState }
                                    )
                                )
                            ) {
                                navigationState = SettingsNavigation.ROOT
                            }
                        }
                        // TODO: Add licenses screen
                    }
                }
            }
        }
    }
}
