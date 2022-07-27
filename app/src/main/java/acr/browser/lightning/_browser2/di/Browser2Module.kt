package acr.browser.lightning._browser2.di

import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning._browser2.data.CookieAdministrator
import acr.browser.lightning._browser2.data.DefaultCookieAdministrator
import acr.browser.lightning._browser2.history.DefaultHistoryRecord
import acr.browser.lightning._browser2.history.HistoryRecord
import acr.browser.lightning._browser2.history.NoOpHistoryRecord
import acr.browser.lightning._browser2.notification.DefaultTabCountNotifier
import acr.browser.lightning._browser2.notification.IncognitoTabCountNotifier
import acr.browser.lightning._browser2.notification.TabCountNotifier
import acr.browser.lightning._browser2.search.IntentExtractor
import acr.browser.lightning._browser2.tab.DefaultUserAgent
import acr.browser.lightning._browser2.tab.WebViewFactory
import acr.browser.lightning._browser2.tab.bundle.BundleStore
import acr.browser.lightning._browser2.tab.bundle.DefaultBundleStore
import acr.browser.lightning._browser2.tab.bundle.IncognitoBundleStore
import acr.browser.lightning._browser2.ui.BookmarkConfiguration
import acr.browser.lightning._browser2.ui.TabConfiguration
import acr.browser.lightning._browser2.ui.UiConfiguration
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.NoOpAdBlocker
import acr.browser.lightning.browser.BrowserView
import acr.browser.lightning.preference.UserPreferences
import acr.browser.lightning.utils.IntentUtils
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.webkit.WebSettings
import dagger.Binds
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

}
