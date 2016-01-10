package acr.browser.lightning.app;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.preference.PreferenceManager;

public class BrowserApp extends Application {

    private static BrowserApp sInstance;
    private static AppComponent appComponent;

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
