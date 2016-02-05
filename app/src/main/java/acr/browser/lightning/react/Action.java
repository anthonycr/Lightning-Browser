package acr.browser.lightning.react;

import android.support.annotation.NonNull;

public interface Action<T> {
    /**
     * Should be overridden to send the onSubscribe
     * events such as {@link OnSubscribe#onNext(Object)}
     * or {@link OnSubscribe#onComplete()}.
     *
     * @param onSubscribe the onSubscribe that is sent in
     *                   when the user of the Observable
     *                   subscribes.
     */
    void onSubscribe(@NonNull OnSubscribe<T> onSubscribe);
}
