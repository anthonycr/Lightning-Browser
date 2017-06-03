package acr.browser.lightning.utils;

import android.app.Application;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.BuildConfig;
import acr.browser.lightning.preference.PreferenceManager;

@Singleton
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

    @NonNull private final Set<String> mBlockedDomainsList = new HashSet<>();
    @NonNull private final PreferenceManager mPreferenceManager;
    @NonNull private final Application mApplication;
    private boolean mBlockAds;

    @Inject
    AdBlock(@NonNull Application application, @NonNull PreferenceManager preferenceManager) {
        mApplication = application;
        mPreferenceManager = preferenceManager;
        if (mBlockedDomainsList.isEmpty() && BuildConfig.FULL_VERSION) {
            loadHostsFile().subscribeOn(Schedulers.io()).subscribe();
        }
        mBlockAds = mPreferenceManager.getAdBlockEnabled();
    }

    public void updatePreference() {
        mBlockAds = mPreferenceManager.getAdBlockEnabled();
    }

    /**
     * a method that determines if the given URL is an ad or not. It performs
     * a search of the URL's domain on the blocked domain hash set.
     *
     * @param url the URL to check for being an ad
     * @return true if it is an ad, false if it is not an ad
     */
    public boolean isAd(@Nullable String url) {
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

        boolean isOnBlacklist = mBlockedDomainsList.contains(domain);
        if (isOnBlacklist) {
            Log.d(TAG, "URL '" + url + "' is an ad");
        }
        return isOnBlacklist;
    }

    /**
     * Returns the probable domain name for a given URL
     *
     * @param url the url to parse
     * @return returns the domain
     * @throws URISyntaxException throws an exception if the string cannot form a URI
     */
    @NonNull
    private static String getDomainName(@NonNull String url) throws URISyntaxException {
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
     * This Completable reads through a hosts file and extracts the domains that should
     * be redirected to localhost (a.k.a. IP address 127.0.0.1). It can handle files that
     * simply have a list of host names to block, or it can handle a full blown hosts file.
     * It will strip out comments, references to the base IP address and just extract the
     * domains to be used.
     *
     * @return a Completable that will load the hosts file into memory.
     */
    @NonNull
    private Completable loadHostsFile() {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                AssetManager asset = mApplication.getAssets();
                BufferedReader reader = null;
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    reader = new BufferedReader(new InputStreamReader(
                        asset.open(BLOCKED_DOMAINS_LIST_FILE_NAME)));
                    StringBuilder lineBuilder = new StringBuilder();
                    String line;
                    long time = System.currentTimeMillis();

                    while ((line = reader.readLine()) != null) {
                        lineBuilder.append(line);

                        if (!StringBuilderUtils.isEmpty(lineBuilder) &&
                            !StringBuilderUtils.startsWith(lineBuilder, COMMENT)) {
                            StringBuilderUtils.replace(lineBuilder, LOCAL_IP_V4, EMPTY);
                            StringBuilderUtils.replace(lineBuilder, LOCAL_IP_V4_ALT, EMPTY);
                            StringBuilderUtils.replace(lineBuilder, LOCAL_IP_V6, EMPTY);
                            StringBuilderUtils.replace(lineBuilder, TAB, EMPTY);

                            int comment = lineBuilder.indexOf(COMMENT);
                            if (comment >= 0) {
                                lineBuilder.replace(comment, lineBuilder.length(), EMPTY);
                            }

                            StringBuilderUtils.trim(lineBuilder);

                            if (!StringBuilderUtils.isEmpty(lineBuilder) &&
                                !StringBuilderUtils.equals(lineBuilder, LOCALHOST)) {
                                while (StringBuilderUtils.contains(lineBuilder, SPACE)) {
                                    int space = lineBuilder.indexOf(SPACE);
                                    StringBuilder partial = StringBuilderUtils.substring(lineBuilder, 0, space);
                                    StringBuilderUtils.trim(partial);

                                    String partialLine = partial.toString();
                                    mBlockedDomainsList.add(partialLine);
                                    StringBuilderUtils.replace(lineBuilder, partialLine, EMPTY);
                                    StringBuilderUtils.trim(lineBuilder);
                                }
                                if (lineBuilder.length() > 0) {
                                    mBlockedDomainsList.add(lineBuilder.toString());
                                }
                            }
                        }
                        lineBuilder.setLength(0);
                    }
                    Log.d(TAG, "Loaded ad list in: " + (System.currentTimeMillis() - time) + " ms");
                } catch (IOException e) {
                    Log.wtf(TAG, "Reading blocked domains list from file '"
                        + BLOCKED_DOMAINS_LIST_FILE_NAME + "' failed.", e);
                } finally {
                    Utils.close(reader);
                }
            }
        });
    }

}
