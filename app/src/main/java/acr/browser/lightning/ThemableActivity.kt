package acr.browser.lightning

import acr.browser.lightning.browser.theme.ThemeProvider
import acr.browser.lightning.compose.StateProvider
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

// Should just be a ComponentActivity except for a few injected instances that need the subtype.
abstract class ThemableActivity : AppCompatActivity() {
    @Named("theme")
    @Inject lateinit var appThemePreferenceStoreStateProvider: StateProvider<AppTheme>

    @Inject lateinit var themeProvider: ThemeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val appTheme = themeProvider.appTheme()
            when (appTheme) {
                AppTheme.LIGHT -> setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
                AppTheme.DARK -> setTheme(R.style.Theme_AppCompat_NoActionBar)
                AppTheme.BLACK -> setTheme(R.style.Theme_AppCompat_NoActionBar)
            }
        }
    }
}
