package acr.browser.lightning.react;

import android.support.annotation.Nullable;

public interface Subscription<T> {

    /**
     * Called when the Observer emits an
     * item. It can be called multiple times.
     * It cannot be called after onComplete
     * has been called.
     *
     * @param item the item that has been emitted,
     *             can be null.
     */
    void onNext(@Nullable T item);

    /**
     * This method is called when the observer is
     * finished sending the subscriber events. It
     * is guaranteed that no other methods will be
     * called on the Subscription after this method
     * has been called.
     */
    void onComplete();
}
