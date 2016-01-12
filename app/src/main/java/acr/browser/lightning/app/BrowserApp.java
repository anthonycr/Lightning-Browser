package acr.browser.lightning.app;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.preference.PreferenceManager;

public class BrowserApp extends Application {

    private static BrowserApp sInstance;
    private static AppComponent appComponent;
    private static final Executor mIOThread = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        LeakCanary.install(this);
        buildDepencyGraph();
    }

    public static BrowserApp getContext() {
        return sInstance;
    }

    public static AppComponent getAppComponent() {
        return appComponent;
    }

    private void buildDepencyGraph() {
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
    }

    public static HistoryDatabase getHistoryDatabase() {
        return appComponent.getHistoryDatabase();
    }

    public static Executor getIOThread() {
        return mIOThread;
    }

    public static PreferenceManager getPreferenceManager() {
        return appComponent.getPreferenceManager();
    }

    public static Bus getBus() {
        return appComponent.getBus();
    }

    public static BookmarkPage getBookmarkPage() {
        return appComponent.getBookmarkPage();
    }

}
