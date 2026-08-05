package acr.browser.lightning

import acr.browser.lightning.theme.ThemeProvider
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

// Should just be a ComponentActivity except for a few injected instances that need the subtype.
abstract class ThemableActivity : AppCompatActivity() {
    @Named("theme")
    @Inject lateinit var appThemeStateFlow: StateFlow<@JvmSuppressWildcards AppTheme?>

    @Named("black_status")
    @Inject lateinit var useBlackStatusBarStateFlow: StateFlow<@JvmSuppressWildcards Boolean?>

    @Inject lateinit var themeProvider: ThemeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            themeProvider.appThemeValues().collectLatest { appTheme ->
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
}
