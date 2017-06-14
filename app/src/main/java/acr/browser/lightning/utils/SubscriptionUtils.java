package acr.browser.lightning.utils;

import android.support.annotation.Nullable;

import com.anthonycr.bonsai.Subscription;

/**
 * Utilities used when working with bonsai code.
 * <p>
 * Created by anthonycr on 6/6/17.
 */
public final class SubscriptionUtils {

    private SubscriptionUtils() {}

    /**
     * Unsubscribes from a subscription if the subscription is not null.
     *
     * @param subscription the subscription from which to unsubscribe.
     */
    public static void safeUnsubscribe(@Nullable Subscription subscription) {
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }
}
