package acr.browser.lightning.preference

import acr.browser.lightning.constant.SCHEME_HOMEPAGE
import acr.browser.lightning.utils.FileUtils
import android.app.Application
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's preferences.
 */
@Singleton
class UserPreferences @Inject constructor(application: Application) {

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