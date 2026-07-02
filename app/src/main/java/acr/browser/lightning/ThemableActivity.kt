package acr.browser.lightning

import acr.browser.lightning.compose.StateProvider
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject
import javax.inject.Named

// Should just be a ComponentActivity except for a few injected instances that need the subtype.
abstract class ThemableActivity : AppCompatActivity() {
    @Named("theme")
    @Inject lateinit var appThemePreferenceStoreStateProvider: StateProvider<AppTheme>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }
}
