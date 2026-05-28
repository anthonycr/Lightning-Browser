package acr.browser.lightning.settings.screens

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.R
import acr.browser.lightning.resources.ResourceProvider
import acr.browser.lightning.settings.framework.ClickableOnClick
import acr.browser.lightning.settings.framework.ClickableState
import acr.browser.lightning.settings.framework.SettingsFrameworkPresenter
import acr.browser.lightning.settings.framework.SettingsFrameworkScreen
import acr.browser.lightning.settings.framework.SettingsFrameworkState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import javax.inject.Inject

class AboutSettingsScreen @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {
    val key = "about"

    fun createSettingsFrameworkState(): SettingsFrameworkState = SettingsFrameworkState(
        title = resourceProvider.stringResource(R.string.settings_about),
        content = listOf(
            ClickableState(
                title = resourceProvider.stringResource(R.string.action_follow_me),
                summary = { "https://bsky.app/profile/anthonycr.bsky.social" },
                onClick = ClickableOnClick.WebLink("https://bsky.app/profile/anthonycr.bsky.social")
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.version),
                summary = { BuildConfig.VERSION_NAME },
                onClick = ClickableOnClick.Action {}
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.app_name),
                summary = { resourceProvider.stringResource(R.string.mpl_license) },
                onClick = ClickableOnClick.WebLink("http://www.mozilla.org/MPL/2.0/")
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.android_open_source_project),
                summary = { resourceProvider.stringResource(R.string.apache) },
                onClick = ClickableOnClick.WebLink("http://www.apache.org/licenses/LICENSE-2.0")
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.hphosts_ad_server_list),
                summary = { resourceProvider.stringResource(R.string.freeware) },
                onClick = ClickableOnClick.WebLink("http://hosts-file.net/")
            ),
            ClickableState(
                title = resourceProvider.stringResource(R.string.jsoup),
                summary = { resourceProvider.stringResource(R.string.mit_license) },
                onClick = ClickableOnClick.WebLink("http://jsoup.org/license")
            ),
        )
    )
}

@Composable
fun AboutSettingsScreen(
    aboutSettingsScreen: AboutSettingsScreen,
    onUp: () -> Unit
) {
    SettingsFrameworkScreen(
        viewModel(
            key = aboutSettingsScreen.key,
            factory = SettingsFrameworkPresenter.Factory(
                settingsFrameworkState = {
                    aboutSettingsScreen.createSettingsFrameworkState()
                }
            )
        ),
        onUp
    )
}
