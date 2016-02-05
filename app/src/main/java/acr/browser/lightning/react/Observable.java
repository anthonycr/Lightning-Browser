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
    @Nullable private Executor mSubscriberThread;
    @Nullable private Executor mObserverThread;
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
     * Tells the Observable what Executor that the subscriber
     * work should run on.
     *
     * @param subscribeExecutor the Executor to run the work on.
     * @return returns this so that calls can be conveniently chained.
     */
    public Observable<T> subscribeOn(@NonNull Executor subscribeExecutor) {
        mSubscriberThread = subscribeExecutor;
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
        mObserverThread = observerExecutor;
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
                mAction.onSubscribe(new OnSubscribe<T>(null) {
                    @Override
                    public void unsubscribe() {}

                    @Override
                    public void onComplete() {}

                    @Override
                    public void start() {}

                    @Override
                    public void onError(@NonNull Throwable throwable) {}

                    @Override
                    public void onNext(T item) {}
                });
            }
        });
    }

    /**
     * Immediately subscribes to the Observable and starts
     * sending events from the Observable to the {@link Subscriber}.
     *
     * @param subscriber the class that wishes to receive onNext and
     *                   onComplete callbacks from the Observable.
     */
    public Subscription subscribe(@NonNull Subscriber<T> subscriber) {

        Preconditions.checkNonNull(subscriber);

        final OnSubscribe<T> onSubscribe = new OnSubscribe<T>(subscriber) {

            @Override
            public void unsubscribe() {
                setSubscriber(null);
            }

            private boolean mOnCompleteExecuted = false;

            @Override
            public void onComplete() {
                Subscriber<T> subscription = getSubscriber();
                if (!mOnCompleteExecuted && subscription != null) {
                    mOnCompleteExecuted = true;
                    executeOnObserverThread(new OnCompleteRunnable<>(subscription));
                } else {
                    Log.e(TAG, "onComplete called more than once");
                    throw new RuntimeException("onComplete called more than once");
                }
            }

            @Override
            public void start() {
                Subscriber<T> subscription = getSubscriber();
                executeOnObserverThread(new OnStartRunnable<>(subscription));
            }

            @Override
            public void onError(@NonNull final Throwable throwable) {
                Subscriber<T> subscription = getSubscriber();
                if (!mOnCompleteExecuted && subscription != null) {
                    mOnCompleteExecuted = true;
                    executeOnObserverThread(new OnErrorRunnable<>(subscription, throwable));
                } else {
                    Log.e(TAG, "onComplete already called");
                    throw new RuntimeException("onComplete already called");
                }
            }

            @Override
            public void onNext(final T item) {
                Subscriber<T> subscription = getSubscriber();
                if (!mOnCompleteExecuted && subscription != null) {
                    executeOnObserverThread(new OnNextRunnable<>(subscription, item));
                } else {
                    Log.e(TAG, "onComplete has been already called, onNext should not be called");
                    throw new RuntimeException("onNext should not be called after onComplete has been called");
                }
            }
        };
        executeOnSubscriberThread(new Runnable() {

            @Override
            public void run() {
                mAction.onSubscribe(onSubscribe);
            }
        });
        return onSubscribe;
    }

    private void executeOnObserverThread(@NonNull Runnable runnable) {
        if (mObserverThread != null) {
            mObserverThread.execute(runnable);
        } else {
            mDefault.execute(runnable);
        }
    }

    private void executeOnSubscriberThread(@NonNull Runnable runnable) {
        if (mSubscriberThread != null) {
            mSubscriberThread.execute(runnable);
        } else {
            mDefault.execute(runnable);
        }
    }

    private static class OnCompleteRunnable<T> implements Runnable {
        private final Subscriber<T> subscriber;

        public OnCompleteRunnable(Subscriber<T> subscriber) {this.subscriber = subscriber;}

        @Override
        public void run() {
            subscriber.onComplete();
        }
    }

    private static class OnNextRunnable<T> implements Runnable {
        private final Subscriber<T> subscriber;
        private final T item;

        public OnNextRunnable(Subscriber<T> subscriber, T item) {
            this.subscriber = subscriber;
            this.item = item;
        }

        @Override
        public void run() {
            subscriber.onNext(item);
        }
    }

    private static class OnErrorRunnable<T> implements Runnable {
        private final Subscriber<T> subscriber;
        private final Throwable throwable;

        public OnErrorRunnable(Subscriber<T> subscriber, Throwable throwable) {
            this.subscriber = subscriber;
            this.throwable = throwable;
        }

        @Override
        public void run() {
            subscriber.onError(throwable);
        }
    }

    private static class OnStartRunnable<T> implements Runnable {
        private final Subscriber<T> subscriber;

        public OnStartRunnable(Subscriber<T> subscriber) {this.subscriber = subscriber;}

        @Override
        public void run() {
            subscriber.onStart();
        }
    }
}

