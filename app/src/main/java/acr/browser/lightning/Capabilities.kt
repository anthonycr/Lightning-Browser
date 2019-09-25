package acr.browser.lightning

import android.os.Build

/**
 * Capabilities that are specific to certain API levels.
 */
enum class Capabilities {
    FULL_INCOGNITO
}

/**
 * Returns true if the capability is supported, false otherwise.
 */
val Capabilities.isSupported: Boolean
    get() = when (this) {
        Capabilities.FULL_INCOGNITO -> Build.VERSION.SDK_INT >= 28
    }
