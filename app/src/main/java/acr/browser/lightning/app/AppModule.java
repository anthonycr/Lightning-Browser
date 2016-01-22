package acr.browser.lightning.app;

import android.content.Context;

import com.squareup.otto.Bus;

import net.i2p.android.ui.I2PAndroidHelper;

import javax.inject.Singleton;

import acr.browser.lightning.database.BookmarkManager;
import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    private final BrowserApp app;
    private final Bus bus;

    public AppModule(BrowserApp app) {
        this.app = app;
        this.bus = new Bus();
    }

    @Provides
    public Context provideContext() {
        return app.getApplicationContext();
    }

    @Provides
    @Singleton
    public BookmarkManager provideBookmarkManager() {
        return new BookmarkManager(app.getApplicationContext());
    }

    @Provides
    public Bus provideBus() {
        return bus;
    }

    @Provides
    @Singleton
    public I2PAndroidHelper provideI2PAndroidHelper() {
        return new I2PAndroidHelper(app.getApplicationContext());
    }

}
