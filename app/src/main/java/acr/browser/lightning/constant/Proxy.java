package acr.browser.lightning.constant;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Proxy choice integer definition.
 * <p>
 * These should match the order of @array/proxy_choices_array
 */
@IntDef({Constants.NO_PROXY, Constants.PROXY_ORBOT, Constants.PROXY_I2P, Constants.PROXY_MANUAL})
@Retention(RetentionPolicy.SOURCE)
public @interface Proxy {}
