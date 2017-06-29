package acr.browser.lightning.search.engine

import android.support.annotation.StringRes

/**
 * A class representative of a search engine.
 *
 * Contains three key pieces of information:
 *  * The icon shown for the search engine, should point to a local assets URL.
 *  * The query URL for the search engine, the query will be appended to the end.
 *  * The title string resource for the search engine.
 */
open class BaseSearchEngine internal constructor(val iconUrl: String,
                                                 val queryUrl: String,
                                                 @StringRes val titleRes: Int)
