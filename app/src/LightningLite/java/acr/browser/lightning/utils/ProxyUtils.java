package acr.browser.lightning.utils;

import android.app.Activity;
import android.content.Context;

/**
 * 6/4/2015 Anthony Restaino
 */
public class ProxyUtils {

    private static ProxyUtils mInstance;

    private ProxyUtils(Context context) {

    }

    public static ProxyUtils getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ProxyUtils(context);
        }
        return mInstance;
    }

    /*
     * If Orbot/Tor or I2P is installed, prompt the user if they want to enable
	 * proxying for this session
	 */
    public void checkForProxy(final Activity activity) {

    }

    /*
     * Initialize WebKit Proxying
	 */
    private void initializeProxy(Activity activity) {

    }

    public boolean isProxyReady(Context context) {
        return true;
    }

    public void updateProxySettings(Activity activity) {

    }

    public void onStop() {

    }

    public void onStart(final Activity activity) {

    }

    public static int setProxyChoice(int choice, Activity activity) {
        return choice;
    }
}
