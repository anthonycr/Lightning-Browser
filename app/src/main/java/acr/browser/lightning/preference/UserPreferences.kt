package acr.browser.lightning.preference

import acr.browser.lightning.AppTheme
import acr.browser.lightning.browser.di.UserPrefs
import acr.browser.lightning.device.ScreenSize
import acr.browser.lightning.preference.delegates.enumPreference
import acr.browser.lightning.preference.delegates.intPreference
import acr.browser.lightning.preference.delegates.stringPreference
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

    /**
     * The folder into which files will be downloaded.
     */
    var downloadDirectory by preferences.stringPreference(
        DOWNLOAD_DIRECTORY,
        FileUtils.DEFAULT_DOWNLOAD_PATH
    )

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
     * The index of the theme used by the application.
     */
    var useTheme by preferences.enumPreference(THEME, AppTheme.LIGHT)

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
private const val SHOW_TABS_IN_DRAWER = "showTabsInDrawer"
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
