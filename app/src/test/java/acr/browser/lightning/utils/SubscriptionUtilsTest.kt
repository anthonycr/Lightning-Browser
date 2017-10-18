package acr.browser.lightning.utils

import com.anthonycr.bonsai.Subscription

import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [SubscriptionUtils].
 */
class SubscriptionUtilsTest {

    @Test
    fun `safeUnsubscribe succeeds with null subscription`() = SubscriptionUtils.safeUnsubscribe(null)

    @Test
    fun `safeUnsubscribe unsubscribes successfully with valid description`() {
        val subscription = object : Subscription {

            internal var isUnsubscribed = false

            override fun unsubscribe() {
                isUnsubscribed = true
            }

            override fun isUnsubscribed(): Boolean = isUnsubscribed
        }

        Assert.assertFalse(subscription.isUnsubscribed())

        SubscriptionUtils.safeUnsubscribe(subscription)

        Assert.assertTrue(subscription.isUnsubscribed())
    }
}