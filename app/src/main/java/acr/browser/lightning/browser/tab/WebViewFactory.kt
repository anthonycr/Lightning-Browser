package acr.browser.lightning.browser.tab

import acr.browser.lightning.browser.di.IncognitoMode
import acr.browser.lightning.browser.view.CompositeTouchListener
import acr.browser.lightning.browser.view.RenderingMode
import acr.browser.lightning.log.Logger
import acr.browser.lightning.preference.UserPreferencesDataStore
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import javax.inject.Inject

/**
 * Constructs [WebView] instances configured for the browser based on user's preferences and create
 * the headers we will send with requests.
 */
class WebViewFactory @Inject constructor(
    private val activity: Activity,
    private val logger: Logger,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @IncognitoMode private val incognitoMode: Boolean,
) {

    /**
     * Create the request headers that notify websites of various privacy and data preferences.
     */
    suspend fun createRequestHeaders(): Map<String, String> {
        val requestHeaders = mutableMapOf<String, String>()
        if (userPreferencesDataStore.doNotTrackEnabled.get()) {
            requestHeaders[HEADER_DNT] = "1"
        } else {
            requestHeaders.remove(HEADER_DNT)
        }

        if (userPreferencesDataStore.saveDataEnabled.get()) {
            requestHeaders[HEADER_SAVEDATA] = "on"
        } else {
            requestHeaders.remove(HEADER_SAVEDATA)
        }

        if (userPreferencesDataStore.removeIdentifyingHeadersEnabled.get()) {
            requestHeaders[HEADER_REQUESTED_WITH] = ""
            requestHeaders[HEADER_WAP_PROFILE] = ""
        } else {
            requestHeaders.remove(HEADER_REQUESTED_WITH)
            requestHeaders.remove(HEADER_WAP_PROFILE)
        }

        return requestHeaders
    }

    /**
     * Construct a [WebView] based on the user's preferences.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(tabSettings: TabSettings): Lazy<WebView> = lazy {
        WebView(activity).apply {
            tag = CompositeTouchListener().also(::setOnTouchListener)
            isFocusableInTouchMode = true
            isFocusable = true
            setBackgroundColor(Color.WHITE)

            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES

            isScrollbarFadingEnabled = true
            isSaveEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setNetworkAvailable(true)

            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                    settings,
                    tabSettings.algorithmicDarkeningEnabled
                )
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                // Remove when minSdk >= 33
                @Suppress("DEPRECATION")
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }
            settings.mediaPlaybackRequiresUserGesture = true

            settings.mixedContentMode = if (!incognitoMode) {
                WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            } else {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            settings.domStorageEnabled = true
            // Remove when minSdk >= 35
            @Suppress("DEPRECATION")
            settings.databaseEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.allowContentAccess = true
            settings.allowFileAccess = true

            val modifiesHeaders = tabSettings.doNotTrackEnabled
                || tabSettings.saveDataEnabled
                || tabSettings.removeIdentifyingHeadersEnabled

            settings.defaultTextEncodingName = tabSettings.textEncoding
            setColorMode(Paint(), tabSettings.renderingMode)

            if (!incognitoMode) {
                settings.setGeolocationEnabled(tabSettings.locationEnabled)
            } else {
                settings.setGeolocationEnabled(false)
            }

            settings.userAgentString = tabSettings.userAgent

            if (tabSettings.javaScriptEnabled) {
                settings.javaScriptEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
            } else {
                settings.javaScriptEnabled = false
                settings.javaScriptCanOpenWindowsAutomatically = false
            }

            if (tabSettings.textReflowEnabled) {
                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                try {
                    settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                } catch (e: Exception) {
                    // This shouldn't be necessary, but there are a number
                    // of KitKat devices that crash trying to set this
                    logger.log(TAG, "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING")
                }
            } else {
                settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            }

            settings.blockNetworkImage = tabSettings.blockImagesEnabled
            // Modifying headers causes SEGFAULTS, so disallow multi window if headers are enabled.
            settings.setSupportMultipleWindows(tabSettings.popupsEnabled && !modifiesHeaders)

            settings.useWideViewPort = tabSettings.useWideViewPortEnabled
            settings.loadWithOverviewMode = tabSettings.overviewModeEnabled
            settings.textZoom = when (tabSettings.textSize) {
                0 -> 200
                1 -> 150
                2 -> 125
                3 -> 100
                4 -> 75
                5 -> 50
                else -> throw IllegalArgumentException("Unsupported text size")
            }

            CookieManager.getInstance().setAcceptThirdPartyCookies(
                this,
                !tabSettings.blockThirdPartyCookiesEnabled
            )
        }
    }

    private fun WebView.setColorMode(paint: Paint, mode: RenderingMode) {
        when (mode) {
            RenderingMode.NORMAL -> {
                paint.colorFilter = null
                // setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Some devices get segfaults
                // in the WebView with Hardware Acceleration enabled,
                // the only fix is to disable hardware rendering
                setLayerType(View.LAYER_TYPE_NONE, null)
            }

            RenderingMode.INVERTED -> {
                val filterInvert = ColorMatrixColorFilter(
                    negativeColorArray
                )
                paint.colorFilter = filterInvert
                setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }

            RenderingMode.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val filterGray = ColorMatrixColorFilter(cm)
                paint.colorFilter = filterGray
                setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }

            RenderingMode.INVERTED_GRAYSCALE -> {
                val matrix = ColorMatrix()
                matrix.set(negativeColorArray)
                val matrixGray = ColorMatrix()
                matrixGray.setSaturation(0f)
                val concat = ColorMatrix()
                concat.setConcat(matrix, matrixGray)
                val filterInvertGray = ColorMatrixColorFilter(concat)
                paint.colorFilter = filterInvertGray
                setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }

            RenderingMode.INCREASE_CONTRAST -> {
                val increaseHighContrast = ColorMatrixColorFilter(increaseContrastColorArray)
                paint.colorFilter = increaseHighContrast
                setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }
        }
    }

    companion object {
        private const val TAG = "WebViewFactory"

        const val HEADER_REQUESTED_WITH = "X-Requested-With"
        const val HEADER_WAP_PROFILE = "X-Wap-Profile"
        private const val HEADER_DNT = "DNT"
        private const val HEADER_SAVEDATA = "Save-Data"

        private val negativeColorArray = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, // red
            0f, -1.0f, 0f, 0f, 255f, // green
            0f, 0f, -1.0f, 0f, 255f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
        private val increaseContrastColorArray = floatArrayOf(
            2.0f, 0f, 0f, 0f, -160f, // red
            0f, 2.0f, 0f, 0f, -160f, // green
            0f, 0f, 2.0f, 0f, -160f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
    }

}
