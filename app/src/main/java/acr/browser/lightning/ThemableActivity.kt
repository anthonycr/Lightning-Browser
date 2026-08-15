package acr.browser.lightning

import acr.browser.lightning.compose.asColorScheme
import acr.browser.lightning.compose.isDark
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.R
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

// Should just be a ComponentActivity except for a few injected instances that need the subtype.
abstract class ThemableActivity : AppCompatActivity() {
    @Named("theme")
    @Inject lateinit var appThemeStateFlow: StateFlow<@JvmSuppressWildcards AppTheme?>

    @Named("black_status")
    @Inject lateinit var useBlackStatusBarStateFlow: StateFlow<@JvmSuppressWildcards Boolean?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val systemDarkTheme =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

            appThemeStateFlow.filterNotNull()
                .combine(useBlackStatusBarStateFlow.filterNotNull()) { a, b -> a to b }
                .collectLatest { (appTheme, useBlackStatusBar) ->
                    when (appTheme) {
                        AppTheme.LIGHT -> setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
                        AppTheme.DARK -> setTheme(R.style.Theme_AppCompat_NoActionBar)
                        AppTheme.BLACK -> setTheme(R.style.Theme_AppCompat_NoActionBar)
                        AppTheme.SYSTEM ->
                            if (systemDarkTheme) {
                                setTheme(R.style.Theme_AppCompat_NoActionBar)
                            } else {
                                setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
                            }
                    }

                    val colorScheme = appTheme.asColorScheme(systemDarkTheme)
                    val isDarkColorScheme = appTheme.isDark(systemDarkTheme)
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            Color.Transparent.toArgb(),
                            Color.Transparent.toArgb(),
                        ) { isDarkColorScheme || useBlackStatusBar },
                        navigationBarStyle = SystemBarStyle.auto(
                            colorScheme.scrim.toArgb(),
                            colorScheme.scrim.toArgb(),
                        ) { isDarkColorScheme },
                    )
                }
        }
    }
}
