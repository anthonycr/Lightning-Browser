package acr.browser.lightning.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;

public class AdBlock {

    private static final String TAG = "AdBlock";
    private static final String BLOCKED_DOMAINS_LIST_FILE_NAME = "hosts.txt";
    private static final String LOCAL_IP_V4 = "127.0.0.1";
    private static final String LOCAL_IP_V4_ALT = "0.0.0.0";
    private static final String LOCAL_IP_V6 = "::1";
    private static final String LOCALHOST = "localhost";
    private static final String COMMENT = "#";
    private static final String TAB = "\t";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
    private final Set<String> mBlockedDomainsList = new HashSet<>();
    private boolean mBlockAds;
    private static final Locale mLocale = Locale.getDefault();
    private static AdBlock mInstance;

    public static AdBlock getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AdBlock(context);
        }
        return mInstance;
    }

    private AdBlock(Context context) {
        if (mBlockedDomainsList.isEmpty() && Constants.FULL_VERSION) {
            loadHostsFile(context);
        }
        mBlockAds = BrowserApp.getAppComponent().getPreferenceManager().getAdBlockEnabled();
    }

    public void updatePreference() {
        mBlockAds = BrowserApp.getAppComponent().getPreferenceManager().getAdBlockEnabled();
    }

    private void loadBlockedDomainsList(final Context context) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                AssetManager asset = context.getAssets();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(
                            asset.open(BLOCKED_DOMAINS_LIST_FILE_NAME)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        mBlockedDomainsList.add(line.trim().toLowerCase(mLocale));
                    }
                } catch (IOException e) {
                    Log.wtf(TAG, "Reading blocked domains list from file '"
                            + BLOCKED_DOMAINS_LIST_FILE_NAME + "' failed.", e);
                } finally {
                    Utils.close(reader);
                }
            }
        });
        thread.start();
    }

    /**
     * a method that determines if the given URL is an ad or not. It performs
     * a search of the URL's domain on the blocked domain hash set.
     * @param url the URL to check for being an ad
     * @return true if it is an ad, false if it is not an ad
     */
    public boolean isAd(String url) {
        if (!mBlockAds || url == null) {
            return false;
        }

        String domain;
        try {
            domain = getDomainName(url);
        } catch (URISyntaxException e) {
            Log.d(TAG, "URL '" + url + "' is invalid", e);
            return false;
        }

        boolean isOnBlacklist = mBlockedDomainsList.contains(domain.toLowerCase(mLocale));
        if (isOnBlacklist) {
            Log.d(TAG, "URL '" + url + "' is an ad");
        }
        return isOnBlacklist;
    }

    /**
     * Returns the probable domain name for a given URL
     * @param url the url to parse
     * @return returns the domain
     * @throws URISyntaxException throws an exception if the string cannot form a URI
     */
    private static String getDomainName(String url) throws URISyntaxException {
        int index = url.indexOf('/', 8);
        if (index != -1) {
            url = url.substring(0, index);
        }

        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            return url;
        }

        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    /**
     * This method reads through a hosts file and extracts the domains that should
     * be redirected to localhost (a.k.a. IP address 127.0.0.1). It can handle files that
     * simply have a list of hostnames to block, or it can handle a full blown hosts file.
     * It will strip out comments, references to the base IP address and just extract the
     * domains to be used
     * @param context the context needed to read the file
     */
    private void loadHostsFile(final Context context) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                AssetManager asset = context.getAssets();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(
                            asset.open(BLOCKED_DOMAINS_LIST_FILE_NAME)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isEmpty() && !line.startsWith(COMMENT)) {
                            line = line.replace(LOCAL_IP_V4, EMPTY)
                                    .replace(LOCAL_IP_V4_ALT, EMPTY)
                                    .replace(LOCAL_IP_V6, EMPTY)
                                    .replace(TAB, EMPTY);
                            int comment = line.indexOf(COMMENT);
                            if (comment >= 0) {
                                line = line.substring(0, comment);
                            }
                            line = line.trim();
                            if (!line.isEmpty() && !line.equals(LOCALHOST)) {
                                while (line.contains(SPACE)) {
                                    int space = line.indexOf(SPACE);
                                    String host = line.substring(0, space);
                                    mBlockedDomainsList.add(host.trim());
                                    line = line.substring(space, line.length()).trim();
                                }
                                mBlockedDomainsList.add(line.trim());
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.wtf(TAG, "Reading blocked domains list from file '"
                            + BLOCKED_DOMAINS_LIST_FILE_NAME + "' failed.", e);
                } finally {
                    Utils.close(reader);
                }
            }
        });
        thread.start();
    }
}
