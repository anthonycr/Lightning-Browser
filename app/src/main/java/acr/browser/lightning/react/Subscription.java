package acr.browser.lightning.react;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class Subscription<T> {

    /**
     * Called when the observable
     * runs into an error that will
     * cause it to abort and not finish.
     * Receiving this callback means that
     * the observable is dead and no
     * {@link #onComplete()} or {@link #onNext(Object)}
     * callbacks will be called.
     *
     * @param throwable an optional throwable that could
     *                  be sent.
     */
    public void onError(@NonNull Throwable throwable) {}

    /**
     * Called before the observer begins
     * to process and emit items or complete.
     */
    public void onStart() {}

    /**
     * Called when the Observer emits an
     * item. It can be called multiple times.
     * It cannot be called after onComplete
     * has been called.
     *
     * @param item the item that has been emitted,
     *             can be null.
     */
    public void onNext(@Nullable T item) {}

    /**
     * This method is called when the observer is
     * finished sending the subscriber events. It
     * is guaranteed that no other methods will be
     * called on the Subscription after this method
     * has been called.
     */
    public void onComplete() {}
}
