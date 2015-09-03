package acr.browser.lightning.app;

import android.app.Application;
import android.content.Context;

public class BrowserApp extends Application {

    private static Context context;
    private static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        buildDepencyGraph();
    }

    public static Context getAppContext() {
        return context;
    }

    public static AppComponent getAppComponent() {
        return appComponent;
    }

    public void buildDepencyGraph() {
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
    }

}
