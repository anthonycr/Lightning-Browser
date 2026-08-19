package acr.browser.lightning.settings

import acr.browser.lightning.R
import acr.browser.lightning.compose.StatusBar
import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.device.BuildType
import acr.browser.lightning.settings.framework.SettingsClickableState
import acr.browser.lightning.settings.framework.compose.SettingsClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.StateFlow

enum class SettingsNavigation {
    ROOT,
    ADBLOCK,
    GENERAL,
    BOOKMARK,
    DISPLAY,
    PRIVACY,
    ADVANCED,
    ABOUT,
    FAQ,
    DEBUG
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    useBlackStatusBarStateFlow: StateFlow<Boolean?>,
    buildInfo: BuildInfo,
    onNavigate: (SettingsNavigation) -> Unit
) {
    Scaffold(
        topBar = {
            StatusBar(
                paintSurfaceColor = false,
                useBlackStatusBarStateFlow = useBlackStatusBarStateFlow,
            )
            TopAppBar(
                title = {
                    Text(stringResource(R.string.settings))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsClickable(SettingsClickableState(title = stringResource(R.string.settings_adblock))) {
                onNavigate(SettingsNavigation.ADBLOCK)
            }
            SettingsClickable(SettingsClickableState(title = stringResource(R.string.settings_general))) {
                onNavigate(SettingsNavigation.GENERAL)
            }
            SettingsClickable(SettingsClickableState(title = stringResource(R.string.bookmark_settings))) {
                onNavigate(SettingsNavigation.BOOKMARK)
            }
            SettingsClickable(SettingsClickableState(title = stringResource(R.string.settings_display))) {
                onNavigate(SettingsNavigation.DISPLAY)
            }
            SettingsClickable(SettingsClickableState(title = stringResource(R.string.settings_privacy))) {
                onNavigate(SettingsNavigation.PRIVACY)
            }
            SettingsClickable(SettingsClickableState(title = stringResource(R.string.settings_advanced))) {
                onNavigate(SettingsNavigation.ADVANCED)
            }
            SettingsClickable(
                SettingsClickableState(
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_explain)
                )
            ) {
                onNavigate(SettingsNavigation.ABOUT)
            }
            SettingsClickable(
                SettingsClickableState(
                    title = stringResource(R.string.faq),
                    summary = stringResource(R.string.faq_description)
                )
            ) {
                onNavigate(SettingsNavigation.FAQ)
            }
            if (buildInfo.buildType == BuildType.DEBUG) {
                SettingsClickable(SettingsClickableState(title = stringResource(R.string.debug_title))) {
                    onNavigate(SettingsNavigation.DEBUG)
                }
            }
        }
    }
}

