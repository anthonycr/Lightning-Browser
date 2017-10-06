package acr.browser.lightning.constant

import android.support.annotation.IntDef


/**
 * Proxy choice integer definition.
 *
 * These should match the order of @array/proxy_choices_array
 */
@IntDef(NO_PROXY.toLong(), PROXY_ORBOT.toLong(), PROXY_I2P.toLong(), PROXY_MANUAL.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class Proxy
