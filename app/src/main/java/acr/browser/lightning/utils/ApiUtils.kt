package acr.browser.lightning.utils

import android.os.Build

/**
 * Utils to determine the capabilities of the Android version used on the device.
 */
object ApiUtils {

    /**
     * Returns true if the Android version supports custom headers in the WebView.
     */
    @JvmStatic
    fun doesSupportWebViewHeaders(): Boolean = true

    /**
     * Returns true if the Android version supports WebRTC in the WebView.
     */
    @JvmStatic
    fun doesSupportWebRtc(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    /**
     * Returns true if the Android version supports blocking third party cookies in the WebView.
     */
    @JvmStatic
    fun doesSupportThirdPartyCookieBlocking(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

}
