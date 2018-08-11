package acr.browser.lightning.rx

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.reactivex.Observable
import io.reactivex.Observer

/**
 * An [Observable] of [Intent] emitted from a [BroadcastReceiver] registered with the [Application].
 *
 * Created by anthonycr on 3/30/18.
 */
class BroadcastReceiverObservable(
        private val action: String,
        private val application: Application
) : Observable<Intent>() {

    override fun subscribeActual(observer: Observer<in Intent>) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == action) {
                    observer.onNext(intent)
                }
            }
        }

        application.registerReceiver(receiver, IntentFilter().apply {
            addAction(action)
        })

        observer.onSubscribe(BroadcastReceiverDisposable(application, receiver))
    }

}