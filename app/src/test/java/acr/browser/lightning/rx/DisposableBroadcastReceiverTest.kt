package acr.browser.lightning.rx

import acr.browser.lightning.BuildConfig
import acr.browser.lightning.SDK_VERSION
import acr.browser.lightning.TestApplication
import android.content.BroadcastReceiver
import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [DisposableBroadcastReceiver].
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApplication::class, sdk = [SDK_VERSION])
class DisposableBroadcastReceiverTest {

    private val context = mock<Context>()
    private val broadcastReceiver = mock<BroadcastReceiver>()

    @Test
    fun `isDisposed defaults to false`() {
        val disposableBroadcastReceiver = DisposableBroadcastReceiver(context, broadcastReceiver)

        assertThat(disposableBroadcastReceiver.isDisposed).isFalse()
    }

    @Test
    fun `isDisposed returns true after dispose`() {
        val disposableBroadcastReceiver = DisposableBroadcastReceiver(context, broadcastReceiver)

        disposableBroadcastReceiver.dispose()

        assertThat(disposableBroadcastReceiver.isDisposed).isTrue()
    }

    @Test
    fun `dispose unregisters receiver once`() {
        val disposableBroadcastReceiver = DisposableBroadcastReceiver(context, broadcastReceiver)

        disposableBroadcastReceiver.dispose()
        disposableBroadcastReceiver.dispose()
        disposableBroadcastReceiver.dispose()

        verify(context).unregisterReceiver(broadcastReceiver)
        verifyNoMoreInteractions(context)
    }

}