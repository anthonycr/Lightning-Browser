package acr.browser.lightning.view;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

/**
 * Simple holder for useful handlers that
 * will live for the duration of the app.
 */
public final class Handlers {

    private Handlers() {}

    static {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
    }

    @NonNull
    public static final Handler MAIN = new Handler(Looper.getMainLooper());

}
