package acr.browser.lightning.constant

import androidx.annotation.IntDef


/**
 * Proxy choice integer definition.
 *
 * These should match the order of @array/proxy_choices_array
 */
@IntDef(NO_PROXY, PROXY_ORBOT, PROXY_I2P, PROXY_MANUAL)
@Retention(AnnotationRetention.SOURCE)
annotation class Proxy
