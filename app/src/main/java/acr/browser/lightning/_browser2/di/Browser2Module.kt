package acr.browser.lightning._browser2.di

import acr.browser.lightning._browser2.search.IntentExtractor
import acr.browser.lightning.adblock.AdBlocker
import acr.browser.lightning.adblock.BloomFilterAdBlocker
import acr.browser.lightning.adblock.NoOpAdBlocker
import acr.browser.lightning.preference.UserPreferences
import android.content.Intent
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

    @Provides
    @InitialUrl
    fun providesInitialUrl(
        @InitialIntent initialIntent: Intent,
        intentExtractor: IntentExtractor
    ): String? = intentExtractor.extractUrlFromIntent(initialIntent)

}
