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
     * @param <T>    the type that will be emitted to the onSubscribe
     * @return a valid non-null Observable.
     */
    @NonNull
    public static <T> Observable<T> create(@NonNull Action<T> action) {
        Preconditions.checkNonNull(action);
        return new Observable<>(action);
    }

    /**
     * Tells the Observable what Executor that the onSubscribe
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
     * Tells the Observable what Executor the onSubscribe should observe
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
                mAction.onSubscribe(new Subscriber<T>() {
                    @Override
                    public void unsubscribe() {}

                    @Override
                    public void onComplete() {}

                    @Override
                    public void onStart() {}

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
     * sending events from the Observable to the {@link OnSubscribe}.
     *
     * @param onSubscribe the class that wishes to receive onNext and
     *                    onComplete callbacks from the Observable.
     */
    public Subscription subscribe(@NonNull OnSubscribe<T> onSubscribe) {

        Preconditions.checkNonNull(onSubscribe);

        final Subscriber<T> subscriber = new SubscriberImpl<>(onSubscribe, this);

        subscriber.onStart();

        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(subscriber);
            }
        });

        return subscriber;
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

    private static class SubscriberImpl<T> implements Subscriber<T> {

        @Nullable private volatile OnSubscribe<T> mOnSubscribe;
        @NonNull private final Observable<T> mObservable;
        private boolean mOnCompleteExecuted = false;
        private boolean mOnError = false;

        public SubscriberImpl(@NonNull OnSubscribe<T> onSubscribe, @NonNull Observable<T> observable) {
            mOnSubscribe = onSubscribe;
            mObservable = observable;
        }

        @Override
        public void unsubscribe() {
            mOnSubscribe = null;
        }

        @Override
        public void onComplete() {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null && !mOnError) {
                mOnCompleteExecuted = true;
                mObservable.executeOnObserverThread(new OnCompleteRunnable<>(onSubscribe));
            } else if (!mOnError) {
                Log.e(TAG, "onComplete called more than once");
                throw new RuntimeException("onComplete called more than once");
            }
        }

        @Override
        public void onStart() {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mObservable.executeOnObserverThread(new OnStartRunnable<>(onSubscribe));
            }
        }

        @Override
        public void onError(@NonNull final Throwable throwable) {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (onSubscribe != null) {
                mOnError = true;
                mObservable.executeOnObserverThread(new OnErrorRunnable<>(onSubscribe, throwable));
            }
        }

        @Override
        public void onNext(final T item) {
            OnSubscribe<T> onSubscribe = mOnSubscribe;
            if (!mOnCompleteExecuted && onSubscribe != null) {
                mObservable.executeOnObserverThread(new OnNextRunnable<>(onSubscribe, item));
            } else {
                Log.e(TAG, "onComplete has been already called, onNext should not be called");
                throw new RuntimeException("onNext should not be called after onComplete has been called");
            }
        }
    }

    private static class OnCompleteRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;

        public OnCompleteRunnable(@NonNull OnSubscribe<T> onSubscribe) {this.onSubscribe = onSubscribe;}

        @Override
        public void run() {
            onSubscribe.onComplete();
        }
    }

    private static class OnNextRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;
        private final T item;

        public OnNextRunnable(@NonNull OnSubscribe<T> onSubscribe, T item) {
            this.onSubscribe = onSubscribe;
            this.item = item;
        }

        @Override
        public void run() {
            onSubscribe.onNext(item);
        }
    }

    private static class OnErrorRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;
        private final Throwable throwable;

        public OnErrorRunnable(@NonNull OnSubscribe<T> onSubscribe, @NonNull Throwable throwable) {
            this.onSubscribe = onSubscribe;
            this.throwable = throwable;
        }

        @Override
        public void run() {
            onSubscribe.onError(throwable);
        }
    }

    private static class OnStartRunnable<T> implements Runnable {
        private final OnSubscribe<T> onSubscribe;

        public OnStartRunnable(@NonNull OnSubscribe<T> onSubscribe) {this.onSubscribe = onSubscribe;}

        @Override
        public void run() {
            onSubscribe.onStart();
        }
    }
}

