package acr.browser.lightning.di

import acr.browser.lightning.browser.BrowserContract
import acr.browser.lightning.browser.history.DefaultHistoryRecord
import acr.browser.lightning.browser.history.HistoryRecord
import acr.browser.lightning.browser.history.NoOpHistoryRecord
import acr.browser.lightning.browser.notification.DefaultTabCountNotifier
import acr.browser.lightning.browser.notification.IncognitoTabCountNotifier
import acr.browser.lightning.browser.notification.TabCountNotifier
import acr.browser.lightning.browser.search.IntentExtractor
import acr.browser.lightning.browser.tab.bundle.BundleStore
import acr.browser.lightning.browser.tab.bundle.DefaultBundleStore
import acr.browser.lightning.browser.tab.bundle.IncognitoBundleStore
import android.content.Intent
import dagger.Module
import dagger.Provides

/**
 * Constructs dependencies for the browser scope.
 */
@Module
class BrowserModule {

    @Provides
    @InitialAction
    fun providesInitialUrl(
        @InitialIntent initialIntent: Intent?,
        intentExtractor: IntentExtractor
    ): BrowserContract.Action? = intentExtractor.extractUrlFromIntent(initialIntent)

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
