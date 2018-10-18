package acr.browser.lightning.rx

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Functional tests for [BroadcastReceiverObservable].
 *
 * Created by anthonycr on 4/23/18.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class, sdk = [SDK_VERSION])
class BroadcastReceiverObservableTest {

    @Test
    fun `subscriber receives intents for action`() {
        val action = "test"
        val application = RuntimeEnvironment.application
        val broadcastReceiver = BroadcastReceiverObservable(action, application)

        val broadcastObservable = broadcastReceiver.test()

        val intentList = listOf(
            Intent(action).apply { putExtra("extra", 1) },
            Intent(action).apply { putExtra("extra", 2) },
            Intent(action).apply { putExtra("extra", 3) }
        )

        intentList.forEach(application::sendBroadcast)

        broadcastObservable.assertValues(*intentList.toTypedArray())
    }

    @Test
    fun `subscriber does not receive intents for different action`() {
        val action = "test"
        val application = RuntimeEnvironment.application
        val broadcastReceiver = BroadcastReceiverObservable(action, application)

        val broadcastObservable = broadcastReceiver.test()

        val otherAction = "test2"
        val intentList = listOf(
            Intent(otherAction).apply { putExtra("extra", 1) },
            Intent(otherAction).apply { putExtra("extra", 2) },
            Intent(otherAction).apply { putExtra("extra", 3) }
        )

        intentList.forEach(application::sendBroadcast)

        broadcastObservable.assertEmpty()
    }

    @Test
    fun `subscriber does not receive intents after dispose`() {
        val action = "test"
        val application = RuntimeEnvironment.application
        val broadcastReceiver = BroadcastReceiverObservable(action, application)

        val broadcastObservable = broadcastReceiver.test()

        val intent = Intent(action).apply { putExtra("extra", 1) }
        val postDisposeIntent = Intent(action).apply { putExtra("extra", 2) }

        application.sendBroadcast(intent)
        broadcastObservable.dispose()
        application.sendBroadcast(postDisposeIntent)

        broadcastObservable.assertValue(intent)
    }
}
