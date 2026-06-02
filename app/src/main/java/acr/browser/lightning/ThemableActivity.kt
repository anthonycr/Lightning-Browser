package acr.browser.lightning

import acr.browser.lightning.compose.StateProvider
import androidx.activity.ComponentActivity
import javax.inject.Inject
import javax.inject.Named

abstract class ThemableActivity : ComponentActivity() {
    @Named("theme")
    @Inject lateinit var appThemePreferenceStoreStateProvider: StateProvider<AppTheme>
}
