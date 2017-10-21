package acr.browser.lightning.rx

import android.content.BroadcastReceiver
import android.content.Context
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [Disposable] that safely unregisters a [BroadcastReceiver] when disposed.
 *
 * Created by anthonycr on 10/21/17.
 */
class DisposableBroadcastReceiver(
        private val context: Context,
        private val broadcastReceiver: BroadcastReceiver
) : Disposable {

    private val disposed = AtomicBoolean(false)

    override fun isDisposed(): Boolean = disposed.get()

    override fun dispose() {
        if (!disposed.getAndSet(true)) {
            context.unregisterReceiver(broadcastReceiver)
        }
    }
}