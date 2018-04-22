package acr.browser.lightning.network

import acr.browser.lightning.rx.ObservableBroadcastReceiver
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import io.reactivex.Observable
import javax.inject.Inject


/**
 * A model that supplies network connectivity status updates.
 */
class NetworkConnectivityModel @Inject constructor(private val application: Application) {

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * An infinite observable that emits a boolean value whenever the network condition changes.
     * Emitted value is true when the network is in the connected state, and it is false otherwise.
     */
    fun connectivity(): Observable<Boolean> = ObservableBroadcastReceiver(
            NETWORK_BROADCAST_ACTION,
            application
    ).map { connectivityManager.activeNetworkInfo?.isConnected == true }

    companion object {
        private const val NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    }

}
