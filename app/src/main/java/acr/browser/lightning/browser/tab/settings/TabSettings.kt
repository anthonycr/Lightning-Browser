package acr.browser.lightning.browser.tab.settings

import acr.browser.lightning.preference.UserPreferencesDataStore
import acr.browser.lightning.useragent.UserAgentProvider

/**
 * Represents the settings used by a tab.
 *
 * @param algorithmicDarkeningEnabled see [UserPreferencesDataStore.algorithmicDarkeningEnabled].
 * @param doNotTrackEnabled see [UserPreferencesDataStore.doNotTrackEnabled].
 * @param saveDataEnabled see [UserPreferencesDataStore.saveDataEnabled].
 * @param removeIdentifyingHeadersEnabled see [UserPreferencesDataStore.removeIdentifyingHeadersEnabled].
 * @param textEncoding see [UserPreferencesDataStore.textEncoding].
 * @param renderingMode see [UserPreferencesDataStore.renderingMode].
 * @param locationEnabled see [UserPreferencesDataStore.locationEnabled].
 * @param userAgent see [UserAgentProvider.getUserAgent].
 * @param javaScriptEnabled see [UserPreferencesDataStore.javaScriptEnabled].
 * @param textReflowEnabled see [UserPreferencesDataStore.textReflowEnabled].
 * @param blockImagesEnabled see [UserPreferencesDataStore.blockImagesEnabled].
 * @param popupsEnabled see [UserPreferencesDataStore.popupsEnabled].
 * @param useWideViewPortEnabled see [UserPreferencesDataStore.useWideViewPortEnabled].
 * @param overviewModeEnabled see [UserPreferencesDataStore.overviewModeEnabled].
 * @param textSize see [UserPreferencesDataStore.textSize].
 * @param blockThirdPartyCookiesEnabled see [UserPreferencesDataStore.blockThirdPartyCookiesEnabled].
 */
data class TabSettings(
    val algorithmicDarkeningEnabled: Boolean,
    val doNotTrackEnabled: Boolean,
    val saveDataEnabled: Boolean,
    val removeIdentifyingHeadersEnabled: Boolean,
    val textEncoding: String,
    val renderingMode: RenderingMode,
    val locationEnabled: Boolean,
    val userAgent: String,
    val javaScriptEnabled: Boolean,
    val textReflowEnabled: Boolean,
    val blockImagesEnabled: Boolean,
    val popupsEnabled: Boolean,
    val useWideViewPortEnabled: Boolean,
    val overviewModeEnabled: Boolean,
    val textSize: Int,
    val blockThirdPartyCookiesEnabled: Boolean,
) {
    companion object {
        /**
         * Construct a [TabSettings] instance from the [UserPreferencesDataStore] and [UserAgentProvider].
         */
        suspend fun create(
            userPreferencesDataStore: UserPreferencesDataStore,
            userAgentProvider: UserAgentProvider,
        ): TabSettings = TabSettings(
            algorithmicDarkeningEnabled = userPreferencesDataStore.algorithmicDarkeningEnabled.get(),
            doNotTrackEnabled = userPreferencesDataStore.doNotTrackEnabled.get(),
            saveDataEnabled = userPreferencesDataStore.saveDataEnabled.get(),
            removeIdentifyingHeadersEnabled = userPreferencesDataStore.removeIdentifyingHeadersEnabled.get(),
            textEncoding = userPreferencesDataStore.textEncoding.get(),
            renderingMode = userPreferencesDataStore.renderingMode.get(),
            locationEnabled = userPreferencesDataStore.locationEnabled.get(),
            userAgent = userAgentProvider.getUserAgent(),
            javaScriptEnabled = userPreferencesDataStore.javaScriptEnabled.get(),
            textReflowEnabled = userPreferencesDataStore.textReflowEnabled.get(),
            blockImagesEnabled = userPreferencesDataStore.blockImagesEnabled.get(),
            popupsEnabled = userPreferencesDataStore.popupsEnabled.get(),
            useWideViewPortEnabled = userPreferencesDataStore.useWideViewPortEnabled.get(),
            overviewModeEnabled = userPreferencesDataStore.overviewModeEnabled.get(),
            textSize = userPreferencesDataStore.textSize.get(),
            blockThirdPartyCookiesEnabled = userPreferencesDataStore.blockThirdPartyCookiesEnabled.get(),
        )
    }
}
