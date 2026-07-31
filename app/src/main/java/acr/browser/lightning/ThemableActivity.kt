package acr.browser.lightning

import acr.browser.lightning.concurrency.StateProvider
import acr.browser.lightning.theme.ThemeProvider
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

// Should just be a ComponentActivity except for a few injected instances that need the subtype.
abstract class ThemableActivity : AppCompatActivity() {
    @Named("theme")
    @Inject lateinit var appThemePreferenceStoreStateProvider: StateProvider<AppTheme>

    @Named("black_status")
    @Inject lateinit var blackStatusBarPreferenceStoreStateProvider: StateProvider<Boolean>

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
                AppTheme.SYSTEM ->
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                        setTheme(R.style.Theme_AppCompat_NoActionBar)
                    } else {
                        setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
                    }
            }
        }
    }
}
