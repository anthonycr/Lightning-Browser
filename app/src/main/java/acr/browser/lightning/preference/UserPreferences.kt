package acr.browser.lightning.preference

import acr.browser.lightning.AppTheme
import acr.browser.lightning.browser.proxy.ProxyChoice
import acr.browser.lightning.browser.search.SearchBoxDisplayChoice
import acr.browser.lightning.browser.search.SearchBoxModel
import acr.browser.lightning.constant.DEFAULT_ENCODING
import acr.browser.lightning.constant.SCHEME_BOOKMARKS
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.browser.di.UserPrefs
import acr.browser.lightning.search.SearchEngineProvider
import acr.browser.lightning.search.engine.GoogleSearch
import acr.browser.lightning.utils.FileUtils
import acr.browser.lightning.browser.view.RenderingMode
import acr.browser.lightning.preference.delegates.booleanPreference
import acr.browser.lightning.preference.delegates.enumPreference
import acr.browser.lightning.preference.delegates.intPreference
import acr.browser.lightning.preference.delegates.nullableStringPreference
import acr.browser.lightning.preference.delegates.stringPreference
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's preferences.
 */
@Singleton
class UserPreferences @Inject constructor(
    @UserPrefs preferences: SharedPreferences,
    screenSize: ScreenSize
) {

    /**
     * True if Web RTC is enabled in the browser, false otherwise.
     */
    var webRtcEnabled by preferences.booleanPreference(WEB_RTC, false)

    /**
     * True if the browser should block ads, false otherwise.
     */
    var adBlockEnabled by preferences.booleanPreference(BLOCK_ADS, false)

    /**
     * True if the browser should block images from being loaded, false otherwise.
     */
    var blockImagesEnabled by preferences.booleanPreference(BLOCK_IMAGES, false)

    /**
     * True if the browser should clear the browser cache when the app is exited, false otherwise.
     */
    var clearCacheExit by preferences.booleanPreference(CLEAR_CACHE_EXIT, false)

    /**
     * True if the browser should allow websites to store and access cookies, false otherwise.
     */
    var cookiesEnabled by preferences.booleanPreference(COOKIES, true)

    /**
     * The folder into which files will be downloaded.
     */
    var downloadDirectory by preferences.stringPreference(
        DOWNLOAD_DIRECTORY,
        FileUtils.DEFAULT_DOWNLOAD_PATH
    )

    /**
     * True if the browser should hide the navigation bar when scrolling, false if it should be
     * immobile.
     */
    var fullScreenEnabled by preferences.booleanPreference(FULL_SCREEN, true)

    /**
     * True if the system status bar should be hidden throughout the app, false if it should be
     * visible.
     */
    var hideStatusBarEnabled by preferences.booleanPreference(HIDE_STATUS_BAR, false)

    /**
     * The URL of the selected homepage.
     */
    var homepage by preferences.stringPreference(HOMEPAGE, SCHEME_BOOKMARKS)

    /**
     * True if cookies should be enabled in incognito mode, false otherwise.
     *
     * WARNING: Cookies will be shared between regular and incognito modes if this is enabled.
     */
    var incognitoCookiesEnabled by preferences.booleanPreference(INCOGNITO_COOKIES, false)

    /**
     * True if the browser should allow execution of javascript, false otherwise.
     */
    var javaScriptEnabled by preferences.booleanPreference(JAVASCRIPT, true)

    /**
     * True if the device location should be accessible by websites, false otherwise.
     *
     * NOTE: If this is enabled, permission will still need to be granted on a per-site basis.
     */
    var locationEnabled by preferences.booleanPreference(LOCATION, false)

    /**
     * True if the browser should load pages zoomed out instead of zoomed in so that the text is
     * legible, false otherwise.
     */
    var overviewModeEnabled by preferences.booleanPreference(OVERVIEW_MODE, true)

    /**
     * True if the browser should allow websites to open new windows, false otherwise.
     */
    var popupsEnabled by preferences.booleanPreference(POPUPS, true)

    /**
     * True if the app should remember which browser tabs were open and restore them if the browser
     * is automatically closed by the system.
     */
    var restoreLostTabsEnabled by preferences.booleanPreference(RESTORE_LOST_TABS, true)

    /**
     * True if the browser should save form input, false otherwise.
     */
    var savePasswordsEnabled by preferences.booleanPreference(SAVE_PASSWORDS, true)

    /**
     * The index of the chosen search engine.
     *
     * @see SearchEngineProvider
     */
    var searchChoice by preferences.intPreference(SEARCH, 1)

    /**
     * The custom URL which should be used for making searches.
     */
    var searchUrl by preferences.stringPreference(SEARCH_URL, GoogleSearch().queryUrl)

    /**
     * True if the browser should attempt to reflow the text on a web page after zooming in or out
     * of the page.
     */
    var textReflowEnabled by preferences.booleanPreference(TEXT_REFLOW, false)

    /**
     * The index of the text size that should be used in the browser.
     */
    var textSize by preferences.intPreference(TEXT_SIZE, 3)

    /**
     * True if the browser should fit web pages to the view port, false otherwise.
     */
    var useWideViewPortEnabled by preferences.booleanPreference(USE_WIDE_VIEWPORT, true)

    /**
     * The index of the user agent choice that should be used by the browser.
     *
     * @see UserPreferences.userAgent
     */
    var userAgentChoice by preferences.intPreference(USER_AGENT, 1)

    /**
     * The custom user agent that should be used by the browser.
     */
    var userAgentString by preferences.stringPreference(USER_AGENT_STRING, "")

    /**
     * True if the browser should clear the navigation history on app exit, false otherwise.
     */
    var clearHistoryExitEnabled by preferences.booleanPreference(CLEAR_HISTORY_EXIT, false)

    /**
     * True if the browser should clear the browser cookies on app exit, false otherwise.
     */
    var clearCookiesExitEnabled by preferences.booleanPreference(CLEAR_COOKIES_EXIT, false)

    /**
     * The index of the rendering mode that should be used by the browser.
     */
    var renderingMode by preferences.enumPreference(RENDERING_MODE, RenderingMode.NORMAL)

    /**
     * True if third party cookies should be disallowed by the browser, false if they should be
     * allowed.
     */
    var blockThirdPartyCookiesEnabled by preferences.booleanPreference(BLOCK_THIRD_PARTY, false)

    /**
     * True if the browser should extract the theme color from a website and color the UI with it,
     * false otherwise.
     */
    var colorModeEnabled by preferences.booleanPreference(ENABLE_COLOR_MODE, true)

    /**
     * The index of the URL/search box display choice/
     *
     * @see SearchBoxModel
     */
    var urlBoxContentChoice by preferences.enumPreference(
        URL_BOX_CONTENTS,
        SearchBoxDisplayChoice.DOMAIN
    )

    /**
     * True if the browser should invert the display colors of the web page content, false
     * otherwise.
     */
    var invertColors by preferences.booleanPreference(INVERT_COLORS, false)

    /**
     * The index of the reading mode text size.
     */
    var readingTextSize by preferences.intPreference(READING_TEXT_SIZE, 2)

    /**
     * The index of the theme used by the application.
     */
    var useTheme by preferences.enumPreference(THEME, AppTheme.LIGHT)

    /**
     * The text encoding used by the browser.
     */
    var textEncoding by preferences.stringPreference(TEXT_ENCODING, DEFAULT_ENCODING)

    /**
     * True if the web page storage should be cleared when the app exits, false otherwise.
     */
    var clearWebStorageExitEnabled by preferences.booleanPreference(CLEAR_WEB_STORAGE_EXIT, false)

    /**
     * True if the app should use the navigation drawer UI, false if it should use the traditional
     * desktop browser tabs UI.
     */
    var showTabsInDrawer by preferences.booleanPreference(
        SHOW_TABS_IN_DRAWER,
        !screenSize.isTablet()
    )

    /**
     * True if the browser should send a do not track (DNT) header with every GET request, false
     * otherwise.
     */
    var doNotTrackEnabled by preferences.booleanPreference(DO_NOT_TRACK, false)

    /**
     * True if the browser should save form data, false otherwise.
     */
    var saveDataEnabled by preferences.booleanPreference(SAVE_DATA, false)

    /**
     * True if the browser should attempt to remove identifying headers in GET requests, false if
     * the default headers should be left along.
     */
    var removeIdentifyingHeadersEnabled by preferences.booleanPreference(IDENTIFYING_HEADERS, false)

    /**
     * True if the bookmarks tab should be on the opposite side of the screen, false otherwise. If
     * the navigation drawer UI is used, the tab drawer will be displayed on the opposite side as
     * well.
     */
    var bookmarksAndTabsSwapped by preferences.booleanPreference(SWAP_BOOKMARKS_AND_TABS, false)

    /**
     * True if the status bar of the app should always be high contrast, false if it should follow
     * the theme of the app.
     */
    var useBlackStatusBar by preferences.booleanPreference(BLACK_STATUS_BAR, false)

    /**
     * The index of the proxy choice.
     */
    var proxyChoice by preferences.enumPreference(PROXY_CHOICE, ProxyChoice.NONE)

    /**
     * The proxy host used when [proxyChoice] is [ProxyChoice.MANUAL].
     */
    var proxyHost by preferences.stringPreference(USE_PROXY_HOST, "localhost")

    /**
     * The proxy port used when [proxyChoice] is [ProxyChoice.MANUAL].
     */
    var proxyPort by preferences.intPreference(USE_PROXY_PORT, 8118)

    /**
     * The index of the search suggestion choice.
     *
     * @see SearchEngineProvider
     */
    var searchSuggestionChoice by preferences.intPreference(SEARCH_SUGGESTIONS, 1)

    /**
     * The index of the ad blocking hosts file source.
     */
    var hostsSource by preferences.intPreference(HOSTS_SOURCE, 0)

    /**
     * The local file from which ad blocking hosts should be read, depending on the [hostsSource].
     */
    var hostsLocalFile by preferences.nullableStringPreference(HOSTS_LOCAL_FILE)

    /**
     * The remote URL from which ad blocking hosts should be read, depending on the [hostsSource].
     */
    var hostsRemoteFile by preferences.nullableStringPreference(HOSTS_REMOTE_FILE)
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
private const val SAVE_PASSWORDS = "passwords"
private const val SEARCH = "search"
private const val SEARCH_URL = "searchurl"
private const val TEXT_REFLOW = "textreflow"
private const val TEXT_SIZE = "textsize"
private const val USE_WIDE_VIEWPORT = "wideviewport"
private const val USER_AGENT = "agentchoose"
private const val USER_AGENT_STRING = "userAgentString"
private const val CLEAR_HISTORY_EXIT = "clearHistoryExit"
private const val CLEAR_COOKIES_EXIT = "clearCookiesExit"
private const val SAVE_URL = "saveUrl"
private const val RENDERING_MODE = "renderMode"
private const val BLOCK_THIRD_PARTY = "thirdParty"
private const val ENABLE_COLOR_MODE = "colorMode"
private const val URL_BOX_CONTENTS = "urlContent"
private const val INVERT_COLORS = "invertColors"
private const val READING_TEXT_SIZE = "readingTextSize"
private const val THEME = "Theme"
private const val TEXT_ENCODING = "textEncoding"
private const val CLEAR_WEB_STORAGE_EXIT = "clearWebStorageExit"
private const val SHOW_TABS_IN_DRAWER = "showTabsInDrawer"
private const val DO_NOT_TRACK = "doNotTrack"
private const val SAVE_DATA = "saveData"
private const val IDENTIFYING_HEADERS = "removeIdentifyingHeaders"
private const val SWAP_BOOKMARKS_AND_TABS = "swapBookmarksAndTabs"
private const val BLACK_STATUS_BAR = "blackStatusBar"
private const val PROXY_CHOICE = "proxyChoice"
private const val USE_PROXY_HOST = "useProxyHost"
private const val USE_PROXY_PORT = "useProxyPort"
private const val SEARCH_SUGGESTIONS = "searchSuggestionsChoice"
private const val HOSTS_SOURCE = "hostsSource"
private const val HOSTS_LOCAL_FILE = "hostsLocalFile"
private const val HOSTS_REMOTE_FILE = "hostsRemoteFile"
