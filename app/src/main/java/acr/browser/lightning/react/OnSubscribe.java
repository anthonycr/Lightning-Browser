package acr.browser.lightning.react;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class OnSubscribe<T> implements Subscription {

    @Nullable private Subscriber<T> mSubscriber;

    public OnSubscribe(@Nullable Subscriber<T> subscriber) {
        mSubscriber = subscriber;
        start();
    }

    public abstract void start();

    @Nullable
    public Subscriber<T> getSubscriber() {
        return mSubscriber;
    }

    public void setSubscriber(@Nullable Subscriber<T> subscriber) {
        mSubscriber = subscriber;
    }

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
    public abstract void onError(@NonNull Throwable throwable);

    /**
     * Called when the Observer emits an
     * item. It can be called multiple times.
     * It cannot be called after onComplete
     * has been called.
     *
     * @param item the item that has been emitted,
     *             can be null.
     */
    public abstract void onNext(@Nullable T item);

    /**
     * This method is called when the observer is
     * finished sending the subscriber events. It
     * is guaranteed that no other methods will be
     * called on the Subscriber after this method
     * has been called.
     */
    public abstract void onComplete();
}
