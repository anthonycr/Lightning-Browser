package acr.browser.lightning.utils;

import com.anthonycr.bonsai.Subscription;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SubscriptionUtils}.
 */
public class SubscriptionUtilsTest {

    @Test
    public void safeUnsubscribe_NullSubscription_Succeeds() {
        SubscriptionUtils.safeUnsubscribe(null);
    }

    @Test
    public void safeUnsubscribe_NonNullSubscription_SuccessfullyUnsubscribes() {
        Subscription subscription = new Subscription() {

            boolean isUnsubscribed = false;

            @Override
            public void unsubscribe() {
                isUnsubscribed = true;
            }

            @Override
            public boolean isUnsubscribed() {
                return isUnsubscribed;
            }
        };

        Assert.assertFalse(subscription.isUnsubscribed());

        SubscriptionUtils.safeUnsubscribe(subscription);

        Assert.assertTrue(subscription.isUnsubscribed());
    }
}