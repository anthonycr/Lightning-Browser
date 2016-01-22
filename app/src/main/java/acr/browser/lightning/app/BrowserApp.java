package acr.browser.lightning.app;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.preference.PreferenceManager;

public class BrowserApp extends Application {

    private static AppComponent appComponent;
    private static final Executor mIOThread = Executors.newSingleThreadExecutor();

    @Inject static Context context;
    @Inject static BookmarkPage bookmarkPage;
    @Inject static HistoryDatabase historyDatabase;
    @Inject static Bus bus;
    @Inject static PreferenceManager preferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        appComponent.inject(this);
        LeakCanary.install(this);
    }

    public static AppComponent getAppComponent() {
        return appComponent;
    }

    public static Context getContext() {
        return context;
    }

    public static HistoryDatabase getHistoryDatabase() {
        return historyDatabase;
    }

    public static Executor getIOThread() {
        return mIOThread;
    }

    public static PreferenceManager getPreferenceManager() {
        return preferenceManager;
    }

    public static Bus getBus() {
        return bus;
    }

    public static BookmarkPage getBookmarkPage() {
        return bookmarkPage;
    }

}
