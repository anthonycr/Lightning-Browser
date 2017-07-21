package acr.browser.lightning.network

import acr.browser.lightning.BrowserApp
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject


/**
 * An observable that emits changes in network connectivity to listeners.
 */
class NetworkObservable @Inject constructor() : BroadcastReceiver() {

    companion object {
        private const val NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    }

    /**
     * A network event listener.
     */
    interface EventListener {

        /**
         * Called when the network connectivity changes.
         *
         * @param connected true if the network is connected,
         * false otherwise.
         */
        fun onNetworkConnectionChange(connected: Boolean)

    }

    private val listeners: MutableSet<EventListener> = CopyOnWriteArraySet()
    private var isConnected = false

    @Inject internal lateinit var application: Application

    init {
        BrowserApp.getAppComponent().inject(this)
    }

    /**
     * Registers the receiver to begin receiving broadcasts.
     */
    private fun register() {
        if (isConnected) {
            return
        }

        val filter = IntentFilter()
        filter.addAction(NETWORK_BROADCAST_ACTION)
        application.registerReceiver(this, filter)
        isConnected = true
    }

    /**
     * Unregisters the receiver from receiving broadcasts.
     */
    private fun unregister() {
        if (!isConnected) {
            return
        }

        application.unregisterReceiver(this)
        isConnected = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NETWORK_BROADCAST_ACTION) {
            notifyOfNetworkChange(isConnected(context))
        }
    }

    /**
     * Determines if the device is currently connected to a network or not.
     *
     * @param context the context needed to determine the connection status
     */
    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    /**
     * Start listening for changes in network connectivity.
     *
     * @param listener the listener to register to receive events.
     */
    fun beginListening(listener: EventListener) {
        if (!isConnected) {
            register()
        }
        listeners.add(listener)
    }

    /**
     * Stop listening for changes in network connectivity.
     *
     * @param listener the listener to unregister from receiving events.
     */
    fun stopListening(listener: EventListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            unregister()
        }
    }

    /**
     * Notify the receivers of a network change.
     *
     * @param connected true if the network is connected,
     * false otherwise.
     */
    private fun notifyOfNetworkChange(connected: Boolean) = listeners.forEach {
        it.onNetworkConnectionChange(connected)
    }

}
