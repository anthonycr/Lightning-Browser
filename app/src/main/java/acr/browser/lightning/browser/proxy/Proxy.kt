package acr.browser.lightning.browser.proxy

/**
 * A proxy for the proxy that determines if the proxy is ready (proxy-ception).
 */
interface Proxy {

    /**
     * True if the proxy is ready for use or if no proxy is being used, false otherwise.
     */
    fun isProxyReady(): Boolean

}
