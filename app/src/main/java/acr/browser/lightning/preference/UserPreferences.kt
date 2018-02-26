package acr.browser.lightning.preference

import acr.browser.lightning.constant.DEFAULT_ENCODING
import acr.browser.lightning.constant.NO_PROXY
import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.search.engine.GoogleSearch
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's preferences.
 */
@Singleton
class UserPreferences @Inject constructor(application: Application, screenSize: ScreenSize) {

    private val preferences = application.getSharedPreferences("settings", 0)

    var webRtcEnabled by BooleanPreference(WEB_RTC, false, preferences).delegate()

    var adBlockEnabled by BooleanPreference(BLOCK_ADS, false, preferences).delegate()

    var blockImagesEnabled by BooleanPreference(BLOCK_IMAGES, false, preferences).delegate()

    var flashSupport by IntPreference(ADOBE_FLASH_SUPPORT, 0, preferences).delegate()

    var clearCacheExit by BooleanPreference(CLEAR_CACHE_EXIT, false, preferences).delegate()

    var cookiesEnabled by BooleanPreference(COOKIES, true, preferences).delegate()

    var downloadDirectory by StringPreference(DOWNLOAD_DIRECTORY, FileUtils.DEFAULT_DOWNLOAD_PATH, preferences).delegate()

    var fullScreenEnabled by BooleanPreference(FULL_SCREEN, true, preferences).delegate()

    var hideStatusBarEnabled by BooleanPreference(HIDE_STATUS_BAR, false, preferences).delegate()

    var homepage by StringPreference(HOMEPAGE, SCHEME_HOMEPAGE, preferences).delegate()

    var incognitoCookiesEnabled by BooleanPreference(INCOGNITO_COOKIES, false, preferences).delegate()

    var javaScriptEnabled by BooleanPreference(JAVASCRIPT, true, preferences).delegate()

    var locationEnabled by BooleanPreference(LOCATION, false, preferences).delegate()

    var overviewModeEnabled by BooleanPreference(OVERVIEW_MODE, true, preferences).delegate()

    var popupsEnabled by BooleanPreference(POPUPS, true, preferences).delegate()

    var restoreLostTabsEnabled by BooleanPreference(RESTORE_LOST_TABS, true, preferences).delegate()

    var savePasswordsEnabled by BooleanPreference(SAVE_PASSWORDS, true, preferences).delegate()

    var searchChoice by IntPreference(SEARCH, 1, preferences).delegate()

    var searchUrl by StringPreference(SEARCH_URL, GoogleSearch().queryUrl, preferences).delegate()

    var textReflowEnabled by BooleanPreference(TEXT_REFLOW, false, preferences).delegate()

    var textSize by IntPreference(TEXT_SIZE, 3, preferences).delegate()

    var useWideViewportEnabled by BooleanPreference(USE_WIDE_VIEWPORT, true, preferences).delegate()

    var userAgentChoice by IntPreference(USER_AGENT, 1, preferences).delegate()

    var userAgentString by StringPreference(USER_AGENT_STRING, "", preferences).delegate()

    var clearHistoryExitEnabled by BooleanPreference(CLEAR_HISTORY_EXIT, false, preferences).delegate()

    var clearCookiesExitEnabled by BooleanPreference(CLEAR_COOKIES_EXIT, false, preferences).delegate()

    var savedUrl by StringPreference(SAVE_URL, "", preferences).delegate()

    var renderingMode by IntPreference(RENDERING_MODE, 0, preferences).delegate()

    var blockThirdPartyCookiesEnabled by BooleanPreference(BLOCK_THIRD_PARTY, false, preferences).delegate()

    var colorModeEnabled by BooleanPreference(ENABLE_COLOR_MODE, true, preferences).delegate()

    var urlBoxContentChoice by IntPreference(URL_BOX_CONTENTS, 0, preferences).delegate()

    var invertColors by BooleanPreference(INVERT_COLORS, false, preferences).delegate()

    var readingTextSize by IntPreference(READING_TEXT_SIZE, 2, preferences).delegate()

    var useTheme by IntPreference(THEME, 0, preferences).delegate()

    var textEncoding by StringPreference(TEXT_ENCODING, DEFAULT_ENCODING, preferences).delegate()

    var clearWebStorageExitEnabled by BooleanPreference(CLEAR_WEBSTORAGE_EXIT, false, preferences).delegate()

    var showTabsInDrawer by BooleanPreference(SHOW_TABS_IN_DRAWER, !screenSize.isTablet(), preferences).delegate()

    var doNotTrackEnabled by BooleanPreference(DO_NOT_TRACK, false, preferences).delegate()

    var removeIdentifyingHeadersEnabled by BooleanPreference(IDENTIFYING_HEADERS, false, preferences).delegate()

    var bookmarksAndTabsSwapped by BooleanPreference(SWAP_BOOKMARKS_AND_TABS, false, preferences).delegate()

    var useBlackStatusBar by BooleanPreference(BLACK_STATUS_BAR, false, preferences).delegate()

    var proxyChoice by IntPreference(PROXY_CHOICE, NO_PROXY, preferences).delegate()

    var proxyHost by StringPreference(USE_PROXY_HOST, "localhost", preferences).delegate()

    var proxyPort by IntPreference(USE_PROXY_PORT, 8118, preferences).delegate()

    var searchSuggestionChoice by IntPreference(SEARCH_SUGGESTIONS, 1, preferences).delegate()
}

private const val WEB_RTC = "webRtc"
private const val BLOCK_ADS = "AdBlock"
private const val BLOCK_IMAGES = "blockimages"
private const val ADOBE_FLASH_SUPPORT = "enableflash"
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
private const val CLEAR_WEBSTORAGE_EXIT = "clearWebStorageExit"
private const val SHOW_TABS_IN_DRAWER = "showTabsInDrawer"
private const val DO_NOT_TRACK = "doNotTrack"
private const val IDENTIFYING_HEADERS = "removeIdentifyingHeaders"
private const val SWAP_BOOKMARKS_AND_TABS = "swapBookmarksAndTabs"
private const val BLACK_STATUS_BAR = "blackStatusBar"
private const val PROXY_CHOICE = "proxyChoice"
private const val USE_PROXY_HOST = "useProxyHost"
private const val USE_PROXY_PORT = "useProxyPort"
private const val SEARCH_SUGGESTIONS = "searchSuggestionsChoice"