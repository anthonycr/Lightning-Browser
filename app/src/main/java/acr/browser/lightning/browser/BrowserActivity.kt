package acr.browser.lightning.browser

import acr.browser.lightning.ThemableActivity
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.browser.keys.KeyEventAdapter
import acr.browser.lightning.browser.search.IntentExtractor
import acr.browser.lightning.browser.tab.TabPager
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.compose.StateProvider
import acr.browser.lightning.search.SuggestionsModel
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Named

/**
 * The base browser activity that governs the browsing experience for both default and incognito
 * browsers.
 */
abstract class BrowserActivity : ThemableActivity(), BrowserContract.View {

    @Suppress("ConvertLambdaToReference")
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { presenter.onFileChooserResult(it) }

    @Inject
    internal lateinit var keyEventAdapter: KeyEventAdapter

    @Inject
    internal lateinit var presenter: BrowserPresenter

    @Inject
    internal lateinit var tabPager: TabPager

    @Inject
    internal lateinit var intentExtractor: IntentExtractor

    @Named("tab")
    @Inject
    internal lateinit var tabConfigurationProvider: StateProvider<TabConfiguration>

    @Inject
    internal lateinit var suggestionsModel: SuggestionsModel

    /**
     * True if the activity is operating in incognito mode, false otherwise.
     */
    abstract fun isIncognito(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {

        val browserFrame = FrameLayout(this)
        val customFrame = FrameLayout(this)
        injector.browser2ComponentBuilder()
            .activity(this)
            .browserFrame(browserFrame)
            .customFrame(customFrame)
            .initialIntent(intent.takeIf { savedInstanceState == null })
            .build()
            .inject(this)

        super.onCreate(savedInstanceState)

        setContent {
            val currentState = presenter.state.collectAsMutableState(
                produceState = { BrowserComposeState(it) },
                updateFrom = { updateFrom(it) }
            )

            BrowserScreen(
                tabConfigurationProvider,
                currentState,
                presenter,
                browserFrame,
                customFrame,
                suggestionsModel
            )
            if (currentState.showCustomView) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                setFullscreen(enabled = true, immersive = true)
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                setFullscreen(enabled = false, immersive = false)
            }
        }

        presenter.onViewAttached(this)

        tabPager.longPressListener = presenter::onPageLongPress

        onBackPressedDispatcher.addCallback {
            presenter.onNavigateBack()
        }
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun <T, R> StateFlow<T>.collectAsMutableState(
        produceState: (T) -> R,
        updateFrom: R.(T) -> Unit
    ): R {
        val state = remember { produceState(value) }
        LaunchedEffect(null) {
            collectLatest {
                state.updateFrom(it)
            }
        }
        return state
    }

    override fun onNewIntent(intent: Intent) {
        intentExtractor.extractUrlFromIntent(intent)?.let(presenter::onNewAction)
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDetached()
    }

    override fun onPause() {
        super.onPause()
        presenter.onViewHidden()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventAdapter.adaptKeyEvent(event)?.let(presenter::onKeyComboClick)?.let { true }
            ?: super.onKeyUp(keyCode, event)
    }

    /**
     * @see BrowserContract.View.showToolbar
     */
    override fun showToolbar() {
        // TODO: Show toolbar
    }

    /**
     * @see BrowserContract.View.showFileChooser
     */
    override fun showFileChooser(intent: Intent) {
        launcher.launch(intent)
    }

    private fun setFullscreen(enabled: Boolean, immersive: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            if (enabled) {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (immersive) {
                    hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            } else {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // TODO: Animate color change
//    private fun animateColorChange(color: Int) {
//        if (!userPreferencesDataStore.colorModeEnabled.getUnsafe() || userPreferencesDataStore.useTheme.getUnsafe() != AppTheme.LIGHT || isIncognito()) {
//            return
//        }
//        val adapter = tabsAdapter as? DesktopTabRecyclerViewAdapter
//        val colorAnimator = ColorAnimator(defaultColor)
//        binding.toolbar.startAnimation(
//            colorAnimator.animateTo(
//                color
//            ) { mainColor, secondaryColor ->
//                if (userPreferencesDataStore.tabConfiguration.getUnsafe() != TabConfiguration.DESKTOP) {
//                    backgroundDrawable.color = mainColor
//                    window.setBackgroundDrawable(backgroundDrawable)
//                } else {
//                    adapter?.updateForegroundTabColor(mainColor)
//                }
//                binding.toolbar.setBackgroundColor(mainColor)
//                binding.searchContainer.background?.tint(secondaryColor)
//            })
//    }
}
