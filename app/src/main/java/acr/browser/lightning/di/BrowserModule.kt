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
import acr.browser.lightning.shortcuts.DefaultShortcutGenerator
import acr.browser.lightning.shortcuts.LegacyShortcutGenerator
import acr.browser.lightning.shortcuts.ShortcutGenerator
import android.content.Intent
import android.os.Build
import dagger.Module
import dagger.Provides
import javax.inject.Provider

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
        incognitoTabCountNotifier: Provider<IncognitoTabCountNotifier>
    ): TabCountNotifier = if (incognitoMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        incognitoTabCountNotifier.get()
    } else {
        DefaultTabCountNotifier
    }

    @Provides
    fun providesShortcutGenerator(
        legacyShortcutGenerator: Provider<LegacyShortcutGenerator>,
        defaultShortcutGenerator: Provider<DefaultShortcutGenerator>,
    ): ShortcutGenerator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        defaultShortcutGenerator.get()
    } else {
        legacyShortcutGenerator.get()
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
