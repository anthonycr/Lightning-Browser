package acr.browser.lightning.browser.di

import acr.browser.lightning.R
import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.data.CookieAdministrator
import acr.browser.lightning.browser.data.DefaultCookieAdministrator
import acr.browser.lightning.browser.history.DefaultHistoryRecord
import acr.browser.lightning.browser.history.HistoryRecord
import acr.browser.lightning.browser.history.NoOpHistoryRecord
import acr.browser.lightning.browser.image.IconFreeze
import acr.browser.lightning.browser.notification.DefaultTabCountNotifier
import acr.browser.lightning.browser.notification.IncognitoTabCountNotifier
import acr.browser.lightning.browser.notification.TabCountNotifier
import acr.browser.lightning.browser.search.IntentExtractor
import acr.browser.lightning.browser.tab.DefaultUserAgent
import acr.browser.lightning.browser.tab.bundle.BundleStore
import acr.browser.lightning.browser.tab.bundle.DefaultBundleStore
import acr.browser.lightning.browser.tab.bundle.IncognitoBundleStore
import acr.browser.lightning.browser.ui.BookmarkConfiguration
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.ui.UiConfiguration
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.NoOpAdBlocker
import acr.browser.lightning.extensions.drawable
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.IntentUtils
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebSettings
import androidx.core.graphics.drawable.toBitmap
import dagger.Module
import dagger.Provides
import javax.inject.Provider

/**
 * Created by anthonycr on 9/17/20.
 */
@Module
class Browser2Module {

    @Provides
    fun providesAdBlocker(
        userPreferences: UserPreferences,
        bloomFilterAdBlocker: Provider<BloomFilterAdBlocker>,
        noOpAdBlocker: NoOpAdBlocker
    ): AdBlocker = if (userPreferences.adBlockEnabled) {
        bloomFilterAdBlocker.get()
    } else {
        noOpAdBlocker
    }

    // TODO: dont force cast
    @Provides
    @InitialUrl
    fun providesInitialUrl(
        @InitialIntent initialIntent: Intent,
        intentExtractor: IntentExtractor
    ): String? =
        (intentExtractor.extractUrlFromIntent(initialIntent) as? BrowserContract.Action.LoadUrl)?.url

    // TODO: auto inject intent utils
    @Provides
    fun providesIntentUtils(activity: Activity): IntentUtils = IntentUtils(activity)

    @Provides
    fun providesUiConfiguration(
        userPreferences: UserPreferences
    ): UiConfiguration = UiConfiguration(
        tabConfiguration = if (userPreferences.showTabsInDrawer) {
            TabConfiguration.DRAWER
        } else {
            TabConfiguration.DESKTOP
        },
        bookmarkConfiguration = if (userPreferences.bookmarksAndTabsSwapped) {
            BookmarkConfiguration.LEFT
        } else {
            BookmarkConfiguration.RIGHT
        }
    )

    @DefaultUserAgent
    @Provides
    fun providesDefaultUserAgent(application: Application): String =
        WebSettings.getDefaultUserAgent(application)


    @Provides
    fun providesHistoryRecord(
        @IncognitoMode incognitoMode: Boolean,
        defaultHistoryRecord: DefaultHistoryRecord
    ): HistoryRecord = if (incognitoMode) {
        NoOpHistoryRecord
    } else {
        defaultHistoryRecord
    }

    @Provides
    fun providesCookieAdministrator(
        @IncognitoMode incognitoMode: Boolean,
        defaultCookieAdministrator: DefaultCookieAdministrator,
        incognitoCookieAdministrator: DefaultCookieAdministrator
    ): CookieAdministrator = if (incognitoMode) {
        incognitoCookieAdministrator
    } else {
        defaultCookieAdministrator
    }

    @Provides
    fun providesTabCountNotifier(
        @IncognitoMode incognitoMode: Boolean,
        incognitoTabCountNotifier: IncognitoTabCountNotifier
    ): TabCountNotifier = if (incognitoMode) {
        incognitoTabCountNotifier
    } else {
        DefaultTabCountNotifier
    }

    @Provides
    fun providesBundleStore(
        @IncognitoMode incognitoMode: Boolean,
        defaultBundleStore: DefaultBundleStore
    ): BundleStore = if (incognitoMode) {
        IncognitoBundleStore
    } else {
        defaultBundleStore
    }

    @IconFreeze
    @Provides
    fun providesFrozenIcon(activity: Activity): Bitmap =
        activity.drawable(R.drawable.ic_frozen).toBitmap()

}
