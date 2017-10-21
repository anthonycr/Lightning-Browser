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
 * An observable that emits changes in network connectivity to listeners.
 */
class NetworkConnectivityModel @Inject constructor(private val application: Application) {

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun connectivity(): Observable<Boolean> = Observable.create {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == NETWORK_BROADCAST_ACTION) {
                    it.onNext(connectivityManager.activeNetworkInfo?.isConnected == true)
                }
            }
        }

        application.registerReceiver(receiver, IntentFilter().apply {
            addAction(NETWORK_BROADCAST_ACTION)
        })

        it.setDisposable(DisposableBroadcastReceiver(application, receiver))
    }

    companion object {
        private const val NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    }

}
