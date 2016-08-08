/*
 *  Copyright 2011 Peter Karich 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package acr.browser.lightning.reading;

import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Peter Karich
 */
class SHelper {

    private static final String UTF8 = "UTF-8";
    private static final Pattern SPACE = Pattern.compile(" ");

    public static String replaceSpaces(String url) {
        if (!url.isEmpty()) {
            url = url.trim();
            if (url.contains(" ")) {
                Matcher spaces = SPACE.matcher(url);
                url = spaces.replaceAll("%20");
            }
        }
        return url;
    }

    public static int count(String str, String substring) {
        int c = 0;
        int index1 = str.indexOf(substring);
        if (index1 >= 0) {
            c++;
            c += count(str.substring(index1 + substring.length()), substring);
        }
        return c;
    }

    /**
     * remove more than two spaces or newlines
     */
    public static String innerTrim(String str) {
        if (str.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder(str.length());
        boolean previousSpace = false;
        for (int i = 0, length = str.length(); i < length; i++) {
            char c = str.charAt(i);
            if (c == ' ' || (int) c == 9 || c == '\n') {
                previousSpace = true;
                continue;
            }

            if (previousSpace)
                sb.append(' ');

            previousSpace = false;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    /**
     * Starts reading the encoding from the first valid character until an
     * invalid encoding character occurs.
     */
    public static String encodingCleanup(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        boolean startedWithCorrectString = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c) || Character.isLetter(c) || c == '-' || c == '_') {
                startedWithCorrectString = true;
                sb.append(c);
                continue;
            }

            if (startedWithCorrectString)
                break;
        }
        return sb.toString().trim();
    }

    /**
     * @return the longest substring as str1.substring(result[0], result[1]);
     */
    public static String getLongestSubstring(String str1, String str2) {
        int res[] = longestSubstring(str1, str2);
        if (res == null || res[0] >= res[1])
            return "";

        return str1.substring(res[0], res[1]);
    }

    private static int[] longestSubstring(String str1, String str2) {
        if (str1 == null || str1.isEmpty() || str2 == null || str2.isEmpty())
            return null;

        // dynamic programming => save already identical length into array
        // to understand this algo simply print identical length in every entry of the array
        // i+1, j+1 then reuses information from i,j
        // java initializes them already with 0
        int[][] num = new int[str1.length()][str2.length()];
        int maxlen = 0;
        int lastSubstrBegin = 0;
        int endIndex = 0;
        for (int i = 0; i < str1.length(); i++) {
            for (int j = 0; j < str2.length(); j++) {
                if (str1.charAt(i) == str2.charAt(j)) {
                    if ((i == 0) || (j == 0))
                        num[i][j] = 1;
                    else
                        num[i][j] = 1 + num[i - 1][j - 1];

                    if (num[i][j] > maxlen) {
                        maxlen = num[i][j];
                        // generate substring from str1 => i
                        lastSubstrBegin = i - num[i][j] + 1;
                        endIndex = i + 1;
                    }
                }
            }
        }
        return new int[]{lastSubstrBegin, endIndex};
    }

    public static String getDefaultFavicon(String url) {
        return useDomainOfFirstArg4Second(url, "/favicon.ico");
    }

    /**
     * @param urlForDomain extract the domain from this url
     * @param path         this url does not have a domain
     * @return
     */
    public static String useDomainOfFirstArg4Second(String urlForDomain, String path) {
        try {
            // See: http://stackoverflow.com/questions/1389184/building-an-absolute-url-from-a-relative-url-in-java
            URL baseUrl = new URL(urlForDomain);
            URL relativeurl = new URL(baseUrl, path);
            return relativeurl.toString();
        } catch (MalformedURLException ex) {
            return path;
        }
    }

    public static String extractHost(String url) {
        return extractDomain(url, false);
    }

    public static String extractDomain(String url, boolean aggressive) {
        if (url.startsWith("http://"))
            url = url.substring("http://".length());
        else if (url.startsWith("https://"))
            url = url.substring("https://".length());

        if (aggressive) {
            if (url.startsWith("www."))
                url = url.substring("www.".length());

            // strip mobile from start
            if (url.startsWith("m."))
                url = url.substring("m.".length());
        }

        int slashIndex = url.indexOf('/');
        if (slashIndex > 0)
            url = url.substring(0, slashIndex);

        return url;
    }

    public static boolean isVideoLink(String url) {
        url = extractDomain(url, true);
        return url.startsWith("youtube.com") || url.startsWith("video.yahoo.com")
            || url.startsWith("vimeo.com") || url.startsWith("blip.tv");
    }

    public static boolean isVideo(String url) {
        return url.endsWith(".mpeg") || url.endsWith(".mpg") || url.endsWith(".avi") || url.endsWith(".mov")
            || url.endsWith(".mpg4") || url.endsWith(".mp4") || url.endsWith(".flv") || url.endsWith(".wmv");
    }

    public static boolean isAudio(String url) {
        return url.endsWith(".mp3") || url.endsWith(".ogg") || url.endsWith(".m3u") || url.endsWith(".wav");
    }

    public static boolean isDoc(String url) {
        return url.endsWith(".pdf") || url.endsWith(".ppt") || url.endsWith(".doc")
            || url.endsWith(".swf") || url.endsWith(".rtf") || url.endsWith(".xls");
    }

    public static boolean isPackage(String url) {
        return url.endsWith(".gz") || url.endsWith(".tgz") || url.endsWith(".zip")
            || url.endsWith(".rar") || url.endsWith(".deb") || url.endsWith(".rpm") || url.endsWith(".7z");
    }

    public static boolean isApp(String url) {
        return url.endsWith(".exe") || url.endsWith(".bin") || url.endsWith(".bat") || url.endsWith(".dmg");
    }

    public static boolean isImage(String url) {
        return url.endsWith(".png") || url.endsWith(".jpeg") || url.endsWith(".gif")
            || url.endsWith(".jpg") || url.endsWith(".bmp") || url.endsWith(".ico") || url.endsWith(".eps");
    }

    /**
     * @see "http://blogs.sun.com/CoreJavaTechTips/entry/cookie_handling_in_java_se"
     */
    public static void enableCookieMgmt() {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }

    /**
     * @see "http://stackoverflow.com/questions/2529682/setting-user-agent-of-a-java-urlconnection"
     */
    public static void enableUserAgentOverwrite() {
        System.setProperty("http.agent", "");
    }

    public static String getUrlFromUglyGoogleRedirect(String url) {
        if (url.startsWith("https://www.google.com/url?")) {
            url = url.substring("https://www.google.com/url?".length());
            String arr[] = urlDecode(url).split("&");
            for (String str : arr) {
                if (str.startsWith("q="))
                    return str.substring("q=".length());
            }
        }

        return null;
    }

    public static String getUrlFromUglyFacebookRedirect(String url) {
        if (url.startsWith("https://www.facebook.com/l.php?u=")) {
            url = url.substring("https://www.facebook.com/l.php?u=".length());
            return urlDecode(url);
        }

        return null;
    }

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, UTF8);
        } catch (UnsupportedEncodingException ex) {
            return str;
        }
    }

    private static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, UTF8);
        } catch (UnsupportedEncodingException ex) {
            return str;
        }
    }

    /**
     * Popular sites uses the #! to indicate the importance of the following
     * chars. Ugly but true. Such as: facebook, twitter, gizmodo, ...
     */
    public static String removeHashbang(String url) {
        return url.replaceFirst("#!", "");
    }

    public static String printNode(Element root) {
        return printNode(root, 0);
    }

    private static String printNode(Element root, int indentation) {
        StringBuilder sb = new StringBuilder(indentation);
        for (int i = 0; i < indentation; i++) {
            sb.append(' ');
        }
        sb.append(root.tagName());
        sb.append(':');
        sb.append(root.ownText());
        sb.append('\n');
        for (Element el : root.children()) {
            sb.append(printNode(el, indentation + 1));
            sb.append('\n');
        }
        return sb.toString();
    }

    public static String estimateDate(String url) {
        int index = url.indexOf("://");
        if (index > 0)
            url = url.substring(index + 3);

        int year = -1;
        int yearCounter = -1;
        int month = -1;
        int monthCounter = -1;
        int day = -1;
        String strs[] = url.split("/");
        for (int counter = 0; counter < strs.length; counter++) {
            String str = strs[counter];
            if (str.length() == 4) {
                try {
                    year = Integer.parseInt(str);
                } catch (Exception ex) {
                    continue;
                }
                if (year < 1970 || year > 3000) {
                    year = -1;
                    continue;
                }
                yearCounter = counter;
            } else if (str.length() == 2) {
                if (monthCounter < 0 && counter == yearCounter + 1) {
                    try {
                        month = Integer.parseInt(str);
                    } catch (Exception ex) {
                        continue;
                    }
                    if (month < 1 || month > 12) {
                        month = -1;
                        continue;
                    }
                    monthCounter = counter;
                } else if (counter == monthCounter + 1) {
                    try {
                        day = Integer.parseInt(str);
                    } catch (Exception ignored) {
                        // ignored
                    }
                    if (day < 1 || day > 31) {
                        day = -1;
                        continue;
                    }
                    break;
                }
            }
        }

        if (year < 0)
            return null;

        StringBuilder str = new StringBuilder(year);
        if (month < 1)
            return str.toString();

        str.append('/');
        if (month < 10)
            str.append('0');
        str.append(month);
        if (day < 1)
            return str.toString();

        str.append('/');
        if (day < 10)
            str.append('0');
        str.append(day);
        return str.toString();
    }

    public static String completeDate(String dateStr) {
        if (dateStr == null)
            return null;

        int index = dateStr.indexOf('/');
        if (index > 0) {
            index = dateStr.indexOf('/', index + 1);
            if (index > 0)
                return dateStr;
            else
                return dateStr + "/01";
        }
        return dateStr + "/01/01";
    }

    // with the help of http://stackoverflow.com/questions/1828775/httpclient-and-ssl
    public static void enableAnySSL() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
            SSLContext.setDefault(ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String arg1) throws CertificateException {
            Date today = new Date();
            for (X509Certificate certificate : certs) {
                certificate.checkValidity(today);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String arg1) throws CertificateException {
            Date today = new Date();
            for (X509Certificate certificate : certs) {
                certificate.checkValidity(today);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static int countLetters(String str) {
        int len = str.length();
        int chars = 0;
        for (int i = 0; i < len; i++) {
            if (Character.isLetter(str.charAt(i)))
                chars++;
        }
        return chars;
    }
}