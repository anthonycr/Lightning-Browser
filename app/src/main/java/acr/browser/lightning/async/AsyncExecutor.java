package acr.browser.lightning.async;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created 9/27/2015 Anthony Restaino
 */
public class AsyncExecutor implements Executor {

    private static final String TAG = AsyncExecutor.class.getSimpleName();
    private static final AsyncExecutor INSTANCE = new AsyncExecutor();
    private final Queue<Runnable> mQueue = new ArrayDeque<>(1);
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    private AsyncExecutor() {}

    public static AsyncExecutor getInstance() {
        return INSTANCE;
    }

    public synchronized void notifyThreadFinish() {
        if (mQueue.isEmpty()) {
            return;
        }
        Runnable runnable = mQueue.remove();
        execute(runnable);
    }

    @Override
    protected void finalize() throws Throwable {
        mExecutor.shutdownNow();
        super.finalize();
    }

    @Override
    public void execute(@NonNull Runnable command) {
        try {
            mExecutor.execute(command);
        } catch (RejectedExecutionException ignored) {
            mQueue.add(command);
            Log.d(TAG, "Thread was enqueued");
        }
    }
}
