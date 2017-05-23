package acr.browser.lightning.react;

import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Schedulers {
    private static final Executor sWorker = Executors.newFixedThreadPool(4);
    private static final Executor sIOWorker = Executors.newSingleThreadExecutor();
    private static final Executor sMain = new ThreadExecutor(Looper.getMainLooper());

    /**
     * The worker thread executor, will
     * execute work on any one of multiple
     * threads.
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

    /**
     * The io thread.
     *
     * @return a non-null executor that does
     * work on a single thread off the main thread.
     */
    @NonNull
    public static Executor io() {
        return sIOWorker;
    }
}
