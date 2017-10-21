package acr.browser.lightning.network

import acr.browser.lightning.rx.DisposableBroadcastReceiver
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    fun connectivity(): Observable<Boolean> = Observable.create {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == NETWORK_BROADCAST_ACTION) {
                    it.onNext(connectivityManager.activeNetworkInfo?.isConnected == true)
                }
            }
        }

        it.setDisposable(DisposableBroadcastReceiver(application, receiver))

        application.registerReceiver(receiver, IntentFilter().apply {
            addAction(NETWORK_BROADCAST_ACTION)
        })
    }

    companion object {
        private const val NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    }

}
