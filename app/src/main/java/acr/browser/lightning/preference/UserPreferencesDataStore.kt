package acr.browser.lightning.preference

import acr.browser.lightning.AppTheme
import acr.browser.lightning.browser.search.SearchBoxDisplayChoice
import acr.browser.lightning.browser.search.SearchBoxModel
import acr.browser.lightning.browser.ui.TabConfiguration
import acr.browser.lightning.browser.view.RenderingMode
import acr.browser.lightning.constant.DEFAULT_ENCODING
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.preference.datastore.EnumPreferenceStore
import acr.browser.lightning.preference.datastore.NonNullPreferenceStore
import acr.browser.lightning.preference.datastore.NullablePreferenceStore
import acr.browser.lightning.preference.datastore.migrateBoolean
import acr.browser.lightning.preference.datastore.migrateEnum
import acr.browser.lightning.preference.datastore.migrateInt
import acr.browser.lightning.preference.datastore.migrateNullableString
import acr.browser.lightning.preference.datastore.migrateString
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.engine.GoogleSearch
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val application: Application,
    screenSize: ScreenSize,
) {
    private val dataStore = PreferenceDataStoreFactory.create(
        migrations = listOf(
            SharedPreferencesMigration(
                produceSharedPreferences = {
                    application.getSharedPreferences(FILE_NAME, 0)
                },
            ) { sharedPreferences: SharedPreferencesView, preferences: Preferences ->
                (preferences.toMutablePreferences() to sharedPreferences).apply {
                    migrateBoolean(webRtcEnabled)
                    migrateBoolean(adBlockEnabled)
                    migrateBoolean(blockImagesEnabled)
                    migrateBoolean(clearCacheExit)
                    migrateBoolean(cookiesEnabled)
                    migrateString(downloadDirectory)
                    migrateBoolean(fullScreenEnabled)
                    migrateBoolean(hideStatusBarEnabled)
                    migrateString(homepage)
                    migrateBoolean(incognitoCookiesEnabled)
                    migrateBoolean(javaScriptEnabled)
                    migrateBoolean(locationEnabled)
                    migrateBoolean(overviewModeEnabled)
                    migrateBoolean(popupsEnabled)
                    migrateBoolean(restoreLostTabsEnabled)
                    migrateInt(searchChoice)
                    migrateString(searchUrl)
                    migrateBoolean(textReflowEnabled)
                    migrateInt(textSize)
                    migrateBoolean(useWideViewPortEnabled)
                    migrateInt(userAgentChoice)
                    migrateString(userAgentString)
                    migrateBoolean(clearHistoryExitEnabled)
                    migrateBoolean(clearCookiesExitEnabled)
                    migrateEnum(renderingMode)
                    migrateBoolean(blockThirdPartyCookiesEnabled)
                    migrateBoolean(colorModeEnabled)
                    migrateEnum(urlBoxContentChoice)
                    migrateEnum(useTheme)
                    migrateString(textEncoding)
                    migrateBoolean(clearWebStorageExitEnabled)
                    migrateEnum(tabConfiguration)
                    migrateBoolean(doNotTrackEnabled)
                    migrateBoolean(saveDataEnabled)
                    migrateBoolean(removeIdentifyingHeadersEnabled)
                    migrateBoolean(bookmarksAndTabsSwapped)
                    migrateBoolean(useBlackStatusBar)
                    migrateInt(searchSuggestionChoice)
                    migrateInt(hostsSource)
                    migrateNullableString(hostsLocalFile)
                    migrateNullableString(hostsRemoteFile)
                }.first
            }
        ),
        produceFile = {
            application.preferencesDataStoreFile(FILE_NAME)
        }
    )

    /**
     * True if Web RTC is enabled in the browser, false otherwise.
     */
    val webRtcEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(WEB_RTC),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should block ads, false otherwise.
     */
    val adBlockEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(BLOCK_ADS),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should block images from being loaded, false otherwise.
     */
    val blockImagesEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(BLOCK_IMAGES),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should clear the browser cache when the app is exited, false otherwise.
     */
    val clearCacheExit: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(CLEAR_CACHE_EXIT),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should allow websites to store and access cookies, false otherwise.
     */
    val cookiesEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(COOKIES),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * The folder into which files will be downloaded.
     */
    val downloadDirectory: NonNullPreferenceStore<String> = NonNullPreferenceStore(
        key = stringPreferencesKey(DOWNLOAD_DIRECTORY),
        dataStore = dataStore,
        defaultValue = FileUtils.DEFAULT_DOWNLOAD_PATH
    )

    /**
     * True if the browser should hide the navigation bar when scrolling, false if it should be
     * immobile.
     */
    val fullScreenEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(FULL_SCREEN),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * True if the system status bar should be hidden throughout the app, false if it should be
     * visible.
     */
    val hideStatusBarEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(HIDE_STATUS_BAR),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * The URL of the selected homepage.
     */
    val homepage: NonNullPreferenceStore<String> = NonNullPreferenceStore(
        key = stringPreferencesKey(HOMEPAGE),
        dataStore = dataStore,
        defaultValue = SCHEME_BOOKMARKS
    )

    /**
     * True if cookies should be enabled in incognito mode, false otherwise.
     *
     * WARNING: Cookies will be shared between regular and incognito modes if this is enabled.
     */
    val incognitoCookiesEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(INCOGNITO_COOKIES),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should allow execution of javascript, false otherwise.
     */
    val javaScriptEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(JAVASCRIPT),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * True if the device location should be accessible by websites, false otherwise.
     *
     * NOTE: If this is enabled, permission will still need to be granted on a per-site basis.
     */
    val locationEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(LOCATION),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should load pages zoomed out instead of zoomed in so that the text is
     * legible, false otherwise.
     */
    val overviewModeEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(OVERVIEW_MODE),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * True if the browser should allow websites to open new windows, false otherwise.
     */
    val popupsEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(POPUPS),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * True if the app should remember which browser tabs were open and restore them if the browser
     * is automatically closed by the system.
     */
    val restoreLostTabsEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(RESTORE_LOST_TABS),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * The index of the chosen search engine.
     *
     * @see SearchEngineProvider
     */
    val searchChoice: NonNullPreferenceStore<Int> = NonNullPreferenceStore(
        key = intPreferencesKey(SEARCH),
        dataStore = dataStore,
        defaultValue = 1
    )

    /**
     * The custom URL which should be used for making searches.
     */
    val searchUrl: NonNullPreferenceStore<String> = NonNullPreferenceStore(
        key = stringPreferencesKey(SEARCH_URL),
        dataStore = dataStore,
        defaultValue = GoogleSearch().queryUrl
    )

    /**
     * True if the browser should attempt to reflow the text on a web page after zooming in or out
     * of the page.
     */
    val textReflowEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(TEXT_REFLOW),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * The index of the text size that should be used in the browser.
     */
    val textSize: NonNullPreferenceStore<Int> = NonNullPreferenceStore(
        key = intPreferencesKey(TEXT_SIZE),
        dataStore = dataStore,
        defaultValue = 3
    )

    /**
     * True if the browser should fit web pages to the view port, false otherwise.
     */
    val useWideViewPortEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(USE_WIDE_VIEWPORT),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * The index of the user agent choice that should be used by the browser.
     *
     * @see userAgent
     */
    val userAgentChoice: NonNullPreferenceStore<Int> = NonNullPreferenceStore(
        key = intPreferencesKey(USER_AGENT),
        dataStore = dataStore,
        defaultValue = 1
    )

    /**
     * The custom user agent that should be used by the browser.
     */
    val userAgentString: NonNullPreferenceStore<String> = NonNullPreferenceStore(
        key = stringPreferencesKey(USER_AGENT_STRING),
        dataStore = dataStore,
        defaultValue = ""
    )

    /**
     * True if the browser should clear the navigation history on app exit, false otherwise.
     */
    val clearHistoryExitEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(CLEAR_HISTORY_EXIT),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should clear the browser cookies on app exit, false otherwise.
     */
    val clearCookiesExitEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(CLEAR_COOKIES_EXIT),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * The index of the rendering mode that should be used by the browser.
     */
    val renderingMode: EnumPreferenceStore<RenderingMode> = EnumPreferenceStore(
        key = intPreferencesKey(RENDERING_MODE),
        dataStore = dataStore,
        defaultValue = RenderingMode.NORMAL
    )

    /**
     * True if third party cookies should be disallowed by the browser, false if they should be
     * allowed.
     */
    val blockThirdPartyCookiesEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(BLOCK_THIRD_PARTY),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should extract the theme color from a website and color the UI with it,
     * false otherwise.
     */
    val colorModeEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(ENABLE_COLOR_MODE),
        dataStore = dataStore,
        defaultValue = true
    )

    /**
     * The index of the URL/search box display choice/
     *
     * @see SearchBoxModel
     */
    val urlBoxContentChoice: EnumPreferenceStore<SearchBoxDisplayChoice> = EnumPreferenceStore(
        key = intPreferencesKey(URL_BOX_CONTENTS),
        dataStore = dataStore,
        defaultValue = SearchBoxDisplayChoice.DOMAIN
    )

    /**
     * The index of the theme used by the application.
     */
    val useTheme: EnumPreferenceStore<AppTheme> = EnumPreferenceStore(
        key = intPreferencesKey(THEME),
        dataStore = dataStore,
        defaultValue = AppTheme.LIGHT
    )

    /**
     * The text encoding used by the browser.
     */
    val textEncoding: NonNullPreferenceStore<String> = NonNullPreferenceStore(
        key = stringPreferencesKey(TEXT_ENCODING),
        dataStore = dataStore,
        defaultValue = DEFAULT_ENCODING
    )

    /**
     * True if the web page storage should be cleared when the app exits, false otherwise.
     */
    val clearWebStorageExitEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(CLEAR_WEB_STORAGE_EXIT),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * The display configuration for the tabs. One of bottom, drawer, or desktop style.
     */
    val tabConfiguration: EnumPreferenceStore<TabConfiguration> = EnumPreferenceStore(
        key = intPreferencesKey(TAB_CONFIGURATION),
        dataStore = dataStore,
        defaultValue = if (!screenSize.isTablet()) {
            TabConfiguration.DRAWER_BOTTOM
        } else {
            TabConfiguration.DESKTOP
        }
    )

    /**
     * True if the browser should send a do not track (DNT) header with every GET request, false
     * otherwise.
     */
    val doNotTrackEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(DO_NOT_TRACK),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should save form data, false otherwise.
     */
    val saveDataEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(SAVE_DATA),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the browser should attempt to remove identifying headers in GET requests, false if
     * the default headers should be left along.
     */
    val removeIdentifyingHeadersEnabled: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(IDENTIFYING_HEADERS),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the bookmarks tab should be on the opposite side of the screen, false otherwise. If
     * the navigation drawer UI is used, the tab drawer will be displayed on the opposite side as
     * well.
     */
    val bookmarksAndTabsSwapped: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(SWAP_BOOKMARKS_AND_TABS),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * True if the status bar of the app should always be high contrast, false if it should follow
     * the theme of the app.
     */
    val useBlackStatusBar: NonNullPreferenceStore<Boolean> = NonNullPreferenceStore(
        key = booleanPreferencesKey(BLACK_STATUS_BAR),
        dataStore = dataStore,
        defaultValue = false
    )

    /**
     * The index of the search suggestion choice.
     *
     * @see SearchEngineProvider
     */
    val searchSuggestionChoice: NonNullPreferenceStore<Int> = NonNullPreferenceStore(
        key = intPreferencesKey(SEARCH_SUGGESTIONS),
        dataStore = dataStore,
        defaultValue = 1
    )

    /**
     * The index of the ad blocking hosts file source.
     */
    val hostsSource: NonNullPreferenceStore<Int> = NonNullPreferenceStore(
        key = intPreferencesKey(HOSTS_SOURCE),
        dataStore = dataStore,
        defaultValue = 0
    )

    /**
     * The local file from which ad blocking hosts should be read, depending on the [hostsSource].
     */
    val hostsLocalFile: NullablePreferenceStore<String> = NullablePreferenceStore(
        key = stringPreferencesKey(HOSTS_LOCAL_FILE),
        dataStore = dataStore
    )

    /**
     * The remote URL from which ad blocking hosts should be read, depending on the [hostsSource].
     */
    val hostsRemoteFile: NullablePreferenceStore<String> = NullablePreferenceStore(
        key = stringPreferencesKey(HOSTS_REMOTE_FILE),
        dataStore = dataStore
    )

    companion object {
        private const val FILE_NAME = "settings"
    }
}

private const val WEB_RTC = "webRtc"
private const val BLOCK_ADS = "AdBlock"
private const val BLOCK_IMAGES = "blockimages"
private const val CLEAR_CACHE_EXIT = "cache"
private const val COOKIES = "cookies"
private const val DOWNLOAD_DIRECTORY = "downloadLocation"
private const val FULL_SCREEN = "fullscreen"
private const val HIDE_STATUS_BAR = "hidestatus"
private const val HOMEPAGE = "home"
private const val INCOGNITO_COOKIES = "incognitocookies"
private const val JAVASCRIPT = "java"
private const val LOCATION = "location"
private const val OVERVIEW_MODE = "overviewmode"
private const val POPUPS = "newwindows"
private const val RESTORE_LOST_TABS = "restoreclosed"
private const val SEARCH = "search"
private const val SEARCH_URL = "searchurl"
private const val TEXT_REFLOW = "textreflow"
private const val TEXT_SIZE = "textsize"
private const val USE_WIDE_VIEWPORT = "wideviewport"
private const val USER_AGENT = "agentchoose"
private const val USER_AGENT_STRING = "userAgentString"
private const val CLEAR_HISTORY_EXIT = "clearHistoryExit"
private const val CLEAR_COOKIES_EXIT = "clearCookiesExit"
private const val RENDERING_MODE = "renderMode"
private const val BLOCK_THIRD_PARTY = "thirdParty"
private const val ENABLE_COLOR_MODE = "colorMode"
private const val URL_BOX_CONTENTS = "urlContent"
private const val THEME = "Theme"
private const val TEXT_ENCODING = "textEncoding"
private const val CLEAR_WEB_STORAGE_EXIT = "clearWebStorageExit"
private const val TAB_CONFIGURATION = "tabConfiguration"
private const val DO_NOT_TRACK = "doNotTrack"
private const val SAVE_DATA = "saveData"
private const val IDENTIFYING_HEADERS = "removeIdentifyingHeaders"
private const val SWAP_BOOKMARKS_AND_TABS = "swapBookmarksAndTabs"
private const val BLACK_STATUS_BAR = "blackStatusBar"
private const val SEARCH_SUGGESTIONS = "searchSuggestionsChoice"
private const val HOSTS_SOURCE = "hostsSource"
private const val HOSTS_LOCAL_FILE = "hostsLocalFile"
private const val HOSTS_REMOTE_FILE = "hostsRemoteFile"
