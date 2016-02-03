package acr.browser.lightning.react;

import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Schedulers {
    private static final Executor sWorker = Executors.newCachedThreadPool();
    private static final Executor sMain = new ThreadExecutor(Looper.getMainLooper());

    /**
     * The worker thread.
     *
     * @return a non-null executor.
     */
    @NonNull
    public static Executor worker() {
        return sWorker;
    }

    /**
     * The main thread.
     *
     * @return a non-null executor that does work on the main thread.
     */
    @NonNull
    public static Executor main() {
        return sMain;
    }
}
