package acr.browser.lightning.react;

import android.support.annotation.NonNull;

public interface Action<T> {
    void onSubscribe(@NonNull Subscriber<T> subscriber);
}
