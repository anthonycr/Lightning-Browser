/*
 * Copyright 2014 A.C.R. Development
 */
@file:JvmName("Constants")

package acr.browser.lightning.constant

// Hardcoded user agents
const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36"
const val MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.4; en-us; Nexus 4 Build/JOP24G) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"

// Search query URLs
const val YAHOO_SEARCH = "https://search.yahoo.com/search?p="
const val GOOGLE_SEARCH = "https://www.google.com/search?client=lightning&ie=UTF-8&oe=UTF-8&q="
const val BING_SEARCH = "https://www.bing.com/search?q="
const val DUCK_SEARCH = "https://duckduckgo.com/?t=lightning&q="
const val DUCK_LITE_SEARCH = "https://duckduckgo.com/lite/?t=lightning&q="
const val STARTPAGE_MOBILE_SEARCH = "https://startpage.com/do/m/mobilesearch?language=english&query="
const val STARTPAGE_SEARCH = "https://startpage.com/do/search?language=english&query="
const val ASK_SEARCH = "http://www.ask.com/web?qsrc=0&o=0&l=dir&qo=LightningBrowser&q="
const val BAIDU_SEARCH = "https://www.baidu.com/s?wd="
const val YANDEX_SEARCH = "https://yandex.ru/yandsearch?lr=21411&text="

// URL Schemes
const val HTTP = "http://"
const val HTTPS = "https://"
const val FILE = "file://"
const val ABOUT = "about:"
const val FOLDER = "folder://"

// Custom local page schemes
const val SCHEME_HOMEPAGE = "${ABOUT}home"
const val SCHEME_BLANK = "${ABOUT}blank"
const val SCHEME_BOOKMARKS = "${ABOUT}bookmarks"

const val LOAD_READING_URL = "ReadingUrl"

const val NO_PROXY = 0
const val PROXY_ORBOT = 1
const val PROXY_I2P = 2
const val PROXY_MANUAL = 3

const val UTF8 = "UTF-8"

// Default text encoding we will use
const val DEFAULT_ENCODING = UTF8

// Allowable text encodings for the WebView
@JvmField
val TEXT_ENCODINGS = arrayOf("ISO-8859-1", UTF8, "GBK", "Big5", "ISO-2022-JP", "SHIFT_JS", "EUC-JP", "EUC-KR")

const val INTENT_ORIGIN = "URL_INTENT_ORIGIN"
