package acr.browser.lightning.app;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class BrowserApp extends Application {

    private static AppComponent mAppComponent;
    private static final Executor mIOThread = Executors.newSingleThreadExecutor();

    @Inject Bus mBus;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        mAppComponent.inject(this);
        LeakCanary.install(this);
    }

    @NonNull
    public static BrowserApp get(@NonNull Context context) {
        return (BrowserApp) context.getApplicationContext();
    }

    public static AppComponent getAppComponent() {
        return mAppComponent;
    }

    @NonNull
    public static Executor getIOThread() {
        return mIOThread;
    }

    public static Bus getBus(@NonNull Context context) {
        return get(context).mBus;
    }

}
