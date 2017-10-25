package acr.browser.lightning.utils

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Utils used to access information about the device.
 */
object DeviceUtils {

    /**
     * Gets the width of the device's screen.
     *
     * @param context the context used to access the [WindowManager].
     */
    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return Point().apply {
            windowManager.defaultDisplay.getSize(this)
        }.x
    }

    /**
     * Gets the width of the screen space currently available to the app.
     *
     * @param context the context used to access the [WindowManager].
     */
    @JvmStatic
    fun getAvailableScreenWidth(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            return DisplayMetrics().apply {
                windowManager.defaultDisplay.getRealMetrics(this)
            }.widthPixels
        } else {
            getScreenWidth(context)
        }
    }

}
