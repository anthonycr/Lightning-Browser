package acr.browser.lightning.app;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.squareup.leakcanary.LeakCanary;

public class BrowserApp extends Application {

    private static Context sContext;
    private static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        buildDepencyGraph();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = base;
        MultiDex.install(this);
    }

    public static Context getAppContext() {
        return sContext;
    }

    public static AppComponent getAppComponent() {
        return appComponent;
    }

    private void buildDepencyGraph() {
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
    }
}
