package acr.browser.lightning.react;

import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Schedulers {
    private static final Executor sWorker = Executors.newCachedThreadPool();
    private static final Executor sMain = new ThreadExecutor(Looper.getMainLooper());

    public static Executor worker() {
        return sWorker;
    }

    public static Executor main() {
        return sMain;
    }
}
