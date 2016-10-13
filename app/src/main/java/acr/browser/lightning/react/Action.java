package acr.browser.lightning.react;

import android.support.annotation.NonNull;

public interface Action<T> {
    /**
     * Should be overridden to send the subscriber
     * events such as {@link Subscriber#onNext(Object)}
     * or {@link Subscriber#onComplete()}.
     *
     * @param subscriber the subscriber that is sent in
     *                   when the user of the Observable
     *                   subscribes.
     */
    void onSubscribe(@NonNull Subscriber<T> subscriber);
}
