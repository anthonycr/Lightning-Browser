package acr.browser.lightning.browser.proxy

import acr.browser.lightning.browser.BrowserActivity
import acr.browser.lightning.utils.ProxyUtils
import android.app.Activity
import android.app.Application
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Properly updates the current proxy when the activity is refreshed..
 */
@Singleton
class ProxyAdapter @Inject constructor(
    private val proxyUtils: ProxyUtils
) : Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is BrowserActivity) return
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        if (activity !is BrowserActivity) return
        proxyUtils.checkForProxy(activity)
        proxyUtils.updateProxySettings(activity)
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (activity !is BrowserActivity) return
        currentActivity = null
    }
}
