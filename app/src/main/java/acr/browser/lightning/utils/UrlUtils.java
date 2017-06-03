/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package acr.browser.lightning.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;
import android.webkit.URLUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.DownloadsPage;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.constant.StartPage;

/**
 * Utility methods for Url manipulation
 */
public class UrlUtils {
    private static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
        "(?i)" + // switch on case insensitive matching
            '(' +    // begin group for schema
            "(?:http|https|file)://" +
            "|(?:inline|data|about|javascript):" +
            "|(?:.*:.*@)" +
            ')' +
            "(.*)");
    // Google search
    public final static String QUERY_PLACE_HOLDER = "%s";

    private UrlUtils() { /* cannot be instantiated */ }

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search if canBeSearch is true.
     * <p/>
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @param canBeSearch If true, will return a search url if it isn't a valid
     *                    URL. If false, invalid URLs will return null
     * @return Original or modified URL
     */
    @NonNull
    public static String smartUrlFilter(@NonNull String url, boolean canBeSearch, String searchUrl) {
        String inUrl = url.trim();
        boolean hasSpace = inUrl.indexOf(' ') != -1;
        Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl);
        if (matcher.matches()) {
            // force scheme to lowercase
            String scheme = matcher.group(1);
            String lcScheme = scheme.toLowerCase();
            if (!lcScheme.equals(scheme)) {
                inUrl = lcScheme + matcher.group(2);
            }
            if (hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
                inUrl = inUrl.replace(" ", "%20");
            }
            return inUrl;
        }
        if (!hasSpace) {
            if (Patterns.WEB_URL.matcher(inUrl).matches()) {
                return URLUtil.guessUrl(inUrl);
            }
        }
        if (canBeSearch) {
            return URLUtil.composeSearchUrl(inUrl,
                searchUrl, QUERY_PLACE_HOLDER);
        }
        return "";
    }

    /**
     * Returns whether the given url is the bookmarks/history page or a normal website
     */
    public static boolean isSpecialUrl(@Nullable String url) {
        return url != null && url.startsWith(Constants.FILE) &&
            (url.endsWith(BookmarkPage.FILENAME) ||
                url.endsWith(DownloadsPage.FILENAME) ||
                url.endsWith(HistoryPage.FILENAME) ||
                url.endsWith(StartPage.FILENAME));
    }

    /**
     * Determines if the url is a url for the bookmark page.
     *
     * @param url the url to check, may be null.
     * @return true if the url is a bookmark url, false otherwise.
     */
    public static boolean isBookmarkUrl(@Nullable String url) {
        return url != null && url.startsWith(Constants.FILE) && url.endsWith(BookmarkPage.FILENAME);
    }

    /**
     * Determines if the url is a url for the bookmark page.
     *
     * @param url the url to check, may be null.
     * @return true if the url is a bookmark url, false otherwise.
     */
    public static boolean isDownloadsUrl(@Nullable String url) {
        return url != null && url.startsWith(Constants.FILE) && url.endsWith(DownloadsPage.FILENAME);
    }

    /**
     * Determines if the url is a url for the history page.
     *
     * @param url the url to check, may be null.
     * @return true if the url is a history url, false otherwise.
     */
    public static boolean isHistoryUrl(@Nullable String url) {
        return url != null && url.startsWith(Constants.FILE) && url.endsWith(HistoryPage.FILENAME);
    }

    /**
     * Determines if the url is a url for the start page.
     *
     * @param url the url to check, may be null.
     * @return true if the url is a start page url, false otherwise.
     */
    public static boolean isStartPageUrl(@Nullable String url) {
        return url != null && url.startsWith(Constants.FILE) && url.endsWith(StartPage.FILENAME);
    }
}