package acr.browser.lightning.preference

import acr.browser.lightning.constant.DEFAULT_ENCODING
import acr.browser.lightning.constant.NO_PROXY
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.di.UserPrefs
import acr.browser.lightning.preference.delegates.booleanPreference
import acr.browser.lightning.preference.delegates.intPreference
import acr.browser.lightning.preference.delegates.stringPreference
import acr.browser.lightning.search.engine.GoogleSearch
import acr.browser.lightning.utils.FileUtils
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

    var webRtcEnabled by preferences.booleanPreference(WEB_RTC, false)

    var adBlockEnabled by preferences.booleanPreference(BLOCK_ADS, false)

    var blockImagesEnabled by preferences.booleanPreference(BLOCK_IMAGES, false)

    var clearCacheExit by preferences.booleanPreference(CLEAR_CACHE_EXIT, false)

    var cookiesEnabled by preferences.booleanPreference(COOKIES, true)

    var downloadDirectory by preferences.stringPreference(DOWNLOAD_DIRECTORY, FileUtils.DEFAULT_DOWNLOAD_PATH)

    var fullScreenEnabled by preferences.booleanPreference(FULL_SCREEN, true)

    var hideStatusBarEnabled by preferences.booleanPreference(HIDE_STATUS_BAR, false)

    var homepage by preferences.stringPreference(HOMEPAGE, SCHEME_HOMEPAGE)

    var incognitoCookiesEnabled by preferences.booleanPreference(INCOGNITO_COOKIES, false)

    var javaScriptEnabled by preferences.booleanPreference(JAVASCRIPT, true)

    var locationEnabled by preferences.booleanPreference(LOCATION, false)

    var overviewModeEnabled by preferences.booleanPreference(OVERVIEW_MODE, true)

    var popupsEnabled by preferences.booleanPreference(POPUPS, true)

    var restoreLostTabsEnabled by preferences.booleanPreference(RESTORE_LOST_TABS, true)

    var savePasswordsEnabled by preferences.booleanPreference(SAVE_PASSWORDS, true)

    var searchChoice by preferences.intPreference(SEARCH, 1)

    var searchUrl by preferences.stringPreference(SEARCH_URL, GoogleSearch().queryUrl)

    var textReflowEnabled by preferences.booleanPreference(TEXT_REFLOW, false)

    var textSize by preferences.intPreference(TEXT_SIZE, 3)

    var useWideViewportEnabled by preferences.booleanPreference(USE_WIDE_VIEWPORT, true)

    var userAgentChoice by preferences.intPreference(USER_AGENT, 1)

    var userAgentString by preferences.stringPreference(USER_AGENT_STRING, "")

    var clearHistoryExitEnabled by preferences.booleanPreference(CLEAR_HISTORY_EXIT, false)

    var clearCookiesExitEnabled by preferences.booleanPreference(CLEAR_COOKIES_EXIT, false)

    var savedUrl by preferences.stringPreference(SAVE_URL, "")

    var renderingMode by preferences.intPreference(RENDERING_MODE, 0)

    var blockThirdPartyCookiesEnabled by preferences.booleanPreference(BLOCK_THIRD_PARTY, false)

    var colorModeEnabled by preferences.booleanPreference(ENABLE_COLOR_MODE, true)

    var urlBoxContentChoice by preferences.intPreference(URL_BOX_CONTENTS, 0)

    var invertColors by preferences.booleanPreference(INVERT_COLORS, false)

    var readingTextSize by preferences.intPreference(READING_TEXT_SIZE, 2)

    var useTheme by preferences.intPreference(THEME, 0)

    var textEncoding by preferences.stringPreference(TEXT_ENCODING, DEFAULT_ENCODING)

    var clearWebStorageExitEnabled by preferences.booleanPreference(CLEAR_WEB_STORAGE_EXIT, false)

    var showTabsInDrawer by preferences.booleanPreference(SHOW_TABS_IN_DRAWER, !screenSize.isTablet())

    var doNotTrackEnabled by preferences.booleanPreference(DO_NOT_TRACK, false)

    var removeIdentifyingHeadersEnabled by preferences.booleanPreference(IDENTIFYING_HEADERS, false)

    var bookmarksAndTabsSwapped by preferences.booleanPreference(SWAP_BOOKMARKS_AND_TABS, false)

    var useBlackStatusBar by preferences.booleanPreference(BLACK_STATUS_BAR, false)

    var proxyChoice by preferences.intPreference(PROXY_CHOICE, NO_PROXY)

    var proxyHost by preferences.stringPreference(USE_PROXY_HOST, "localhost")

    var proxyPort by preferences.intPreference(USE_PROXY_PORT, 8118)

    var searchSuggestionChoice by preferences.intPreference(SEARCH_SUGGESTIONS, 1)
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
private const val IDENTIFYING_HEADERS = "removeIdentifyingHeaders"
private const val SWAP_BOOKMARKS_AND_TABS = "swapBookmarksAndTabs"
private const val BLACK_STATUS_BAR = "blackStatusBar"
private const val PROXY_CHOICE = "proxyChoice"
private const val USE_PROXY_HOST = "useProxyHost"
private const val USE_PROXY_PORT = "useProxyPort"
private const val SEARCH_SUGGESTIONS = "searchSuggestionsChoice"
