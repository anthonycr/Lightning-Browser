package acr.browser.lightning.react;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    @NonNull private Action<T> mAction;
    @Nullable private Executor mSubscriber;
    @Nullable private Executor mObserver;
    @NonNull private final Executor mDefault;

    public Observable(@NonNull Action<T> action) {
        mAction = action;
        Looper looper = Looper.myLooper();
        Preconditions.checkNonNull(looper);
        mDefault = new ThreadExecutor(looper);
    }

    public static <T> Observable<T> create(@NonNull Action<T> action) {
        Preconditions.checkNonNull(action);
        return new Observable<>(action);
    }

    public Observable<T> subscribeOn(@NonNull Executor subscribeExecutor) {
        mSubscriber = subscribeExecutor;
        return this;
    }

    public Observable<T> observeOn(@NonNull Executor observerExecutor) {
        mObserver = observerExecutor;
        return this;
    }

    public void subscribe() {
        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(new Subscriber<T>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onNext(T item) {

                    }
                });
            }
        });
    }

    public void subscribe(@NonNull final Subscription<T> subscription) {
        Preconditions.checkNonNull(subscription);
        executeOnSubscriberThread(new Runnable() {
            @Override
            public void run() {
                mAction.onSubscribe(new Subscriber<T>() {
                    @Override
                    public void onComplete() {
                        executeOnObserverThread(new OnCompleteRunnable(subscription));
                    }

                    @Override
                    public void onNext(final T item) {
                        executeOnObserverThread(new OnNextRunnable(subscription, item));
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

    private static class OnCompleteRunnable implements Runnable {
        private final Subscription subscription;

        public OnCompleteRunnable(Subscription subscription) {this.subscription = subscription;}

        @Override
        public void run() {
            subscription.onComplete();
        }
    }

    private class OnNextRunnable implements Runnable {
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

