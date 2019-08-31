package acr.browser.lightning.utils

import android.content.Context

/**
 * Reflectively install multidex to avoid requiring that the Multidex class is on the classpath,
 * since it is only needed on debug.
 */
fun installMultiDex(context: Context) {
    val clazz = Class.forName("androidx.multidex.MultiDex")
    val method = clazz.getMethod("install", Context::class.java)
    method.invoke(null, context)
}
