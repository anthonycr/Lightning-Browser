package acr.browser.lightning._browser2.proxy

import acr.browser.lightning._browser2.BrowserActivity
import acr.browser.lightning.utils.ProxyUtils
import android.app.Activity
import android.app.Application
import android.os.Bundle
import javax.inject.Inject

/**
 * An adapter between [ProxyUtils] and [Proxy].
 */
class ProxyAdapter @Inject constructor(
    application: Application,
    private val proxyUtils: ProxyUtils
) : Proxy {

    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity !is BrowserActivity) return
                currentActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {
                if (activity !is BrowserActivity) return
                proxyUtils.onStart(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity !is BrowserActivity) return
                proxyUtils.checkForProxy(activity)
                proxyUtils.updateProxySettings(activity)
            }

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) {
                if (activity !is BrowserActivity) return
                proxyUtils.onStop()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                if (activity !is BrowserActivity) return
                currentActivity = null
            }

        })
    }

    override fun isProxyReady(): Boolean = currentActivity?.let(proxyUtils::isProxyReady) ?: false
}
