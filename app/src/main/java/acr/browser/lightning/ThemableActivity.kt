package acr.browser.lightning

import acr.browser.lightning.compose.StateProvider
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import javax.inject.Inject
import javax.inject.Named

abstract class ThemableActivity : ComponentActivity() {
    @Named("theme")
    @Inject lateinit var appThemePreferenceStoreStateProvider: StateProvider<AppTheme>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }
}
