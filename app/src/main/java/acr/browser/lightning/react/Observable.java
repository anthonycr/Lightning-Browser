package acr.browser.lightning.react;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.Executor;

import acr.browser.lightning.utils.Preconditions;

/**
 * An RxJava implementation. This class allows work
 * to be done on a certain thread and then allows
 * items to be emitted on a different thread. It is
 * a replacement for {@link android.os.AsyncTask}.
 *
 * @param <T> the type that the Observable will emit.
 */
public class Observable<T> {

    private static final String TAG = Observable.class.getSimpleName();

    @NonNull private final Action<T> mAction;
    @Nullable private Executor mSubscriber;
    @Nullable private Executor mObserver;
    @NonNull private final Executor mDefault;

    private Observable(@NonNull Action<T> action) {
        mAction = action;
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        mDefault = new ThreadExecutor(looper);
    }

    /**
     * Static creator method that creates an Observable from the
     * {@link Action} that is passed in as the parameter. Action
     * must not be null.
     *
     * @param action the Action to perform
     * @param <T>    the type that will be emitted to the subscriber
     * @return a valid non-null Observable.
     */
    @NonNull
    public static <T> Observable<T> create(@NonNull Action<T> action) {
        Preconditions.checkNonNull(action);
        return new Observable<>(action);
    }

    /**
     * Tells the Observable what Executor that the subscription
     * work should run on.
     *
     * @param subscribeExecutor the Executor to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    public Observable<T> subscribeOn(@NonNull Executor subscribeExecutor) {
        mSubscriber = subscribeExecutor;
        return this;
    }

    /**
     * Tells the Observable what Executor the subscriber should observe
     * the work on.
     *
     * @param observerExecutor the Executor to run to callback on.
     * @return returns this so that calls can be conveniently chained.
     */
    public Observable<T> observeOn(@NonNull Executor observerExecutor) {
        mObserver = observerExecutor;
        return this;
    }

    /**
     * Subscribes immediately to the Observable and ignores
     * all onComplete and onNext calls.
     */
    public void subscribe() {
        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(new Subscriber<T>() {
                    @Override
                    public void onComplete() {}

                    @Override
                    public void onNext(T item) {}
                });
            }
        });
    }

    /**
     * Immediately subscribes to the Observable and starts
     * sending events from the Observable to the {@link Subscription}.
     *
     * @param subscription the class that wishes to receive onNext and
     *                     onComplete callbacks from the Observable.
     */
    public void subscribe(@NonNull final Subscription<T> subscription) {
        Preconditions.checkNonNull(subscription);
        executeOnSubscriberThread(new Runnable() {

            private boolean mOnCompleteExecuted = false;

            @Override
            public void run() {
                mAction.onSubscribe(new Subscriber<T>() {
                    @Override
                    public void onComplete() {
                        if (!mOnCompleteExecuted) {
                            mOnCompleteExecuted = true;
                            executeOnObserverThread(new OnCompleteRunnable<>(subscription));
                        } else {
                            Log.e(TAG, "onComplete called more than once");
                            throw new RuntimeException("onComplete called more than once");
                        }
                    }

                    @Override
                    public void onNext(final T item) {
                        if (!mOnCompleteExecuted) {
                            executeOnObserverThread(new OnNextRunnable<>(subscription, item));
                        } else {
                            Log.e(TAG, "onComplete has been already called, onNext should not be called");
                            throw new RuntimeException("onNext should not be called after onComplete has been called");
                        }
                    }
                });

            }
        });
    }

    private void executeOnObserverThread(@NonNull Runnable runnable) {
        if (mObserver != null) {
            mObserver.execute(runnable);
        } else {
            mDefault.execute(runnable);
        }
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (mSubscriber != null) {
            mSubscriber.execute(runnable);
        } else {
            mDefault.execute(runnable);
        }
    }

    private static class OnCompleteRunnable<T> implements Runnable {
        private final Subscription<T> subscription;

        public OnCompleteRunnable(Subscription<T> subscription) {this.subscription = subscription;}

        @Override
        public void run() {
            subscription.onComplete();
        }
    }

    private static class OnNextRunnable<T> implements Runnable {
        private final Subscription<T> subscription;
        private final T item;

        public OnNextRunnable(Subscription<T> subscription, T item) {
            this.subscription = subscription;
            this.item = item;
        }

        @Override
        public void run() {
            subscription.onNext(item);
        }
    }
}

