package acr.browser.lightning.utils

import android.os.Build

/**
 * `true` if the build version is >= 26, `false` otherwise.
 */
fun isAtLeast26() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O