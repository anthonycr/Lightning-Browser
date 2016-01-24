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
    private final BrowserApp mApp;
    private final Bus mBus;

    public AppModule(BrowserApp app) {
        this.mApp = app;
        this.mBus = new Bus();
    }

    @Provides
    public Context provideContext() {
        return mApp.getApplicationContext();
    }

    @Provides
    public Bus provideBus() {
        return mBus;
    }

    @Provides
    @Singleton
    public I2PAndroidHelper provideI2PAndroidHelper() {
        return new I2PAndroidHelper(mApp.getApplicationContext());
    }

}
