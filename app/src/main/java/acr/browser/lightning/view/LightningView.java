/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;

import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

import javax.inject.Inject;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.DownloadsPage;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.download.LightningDownloadListener;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.utils.Utils;

/**
 * {@link LightningView} acts as a tab for the browser,
 * handling WebView creation and handling logic, as well
 * as properly initialing it. All interactions with the
 * WebView should be made through this class.
 */
public class LightningView {

    private static final String TAG = "LightningView";

    public static final String HEADER_REQUESTED_WITH = "X-Requested-With";
    public static final String HEADER_WAP_PROFILE = "X-Wap-Profile";
    private static final String HEADER_DNT = "DNT";
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private static final int SCROLL_UP_THRESHOLD = Utils.dpToPx(10);

    @Nullable private static String sHomepage;
    @Nullable private static String sDefaultUserAgent;
    private static float sMaxFling;
    private static final float[] sNegativeColorArray = {
        -1.0f, 0, 0, 0, 255, // red
        0, -1.0f, 0, 0, 255, // green
        0, 0, -1.0f, 0, 255, // blue
        0, 0, 0, 1.0f, 0 // alpha
    };
    private static final float[] sIncreaseContrastColorArray = {
        2.0f, 0, 0, 0, -160.f, // red
        0, 2.0f, 0, 0, -160.f, // green
        0, 0, 2.0f, 0, -160.f, // blue
        0, 0, 0, 1.0f, 0 // alpha
    };

    @NonNull private final LightningViewTitle mTitle;
    @Nullable private WebView mWebView;
    @NonNull private final UIController mUIController;
    @NonNull private final GestureDetector mGestureDetector;
    @NonNull private final Activity mActivity;
    @NonNull private final Paint mPaint = new Paint();
    private boolean mIsNewTab;
    private final boolean mIsIncognitoTab;
    private boolean mIsForegroundTab;
    private boolean mInvertPage = false;
    private boolean mToggleDesktop = false;
    @NonNull private final WebViewHandler mWebViewHandler = new WebViewHandler(this);
    @NonNull private final Map<String, String> mRequestHeaders = new ArrayMap<>();

    @Inject PreferenceManager mPreferences;
    @Inject LightningDialogBuilder mDialogBuilder;
    @Inject ProxyUtils mProxyUtils;

    private final LightningWebClient mLightningWebClient;

    public LightningView(@NonNull Activity activity, @Nullable String url, boolean isIncognito) {
        BrowserApp.getAppComponent().inject(this);
        mActivity = activity;
        mUIController = (UIController) activity;
        mWebView = new WebView(activity);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            mWebView.setId(View.generateViewId());
        }
        mIsIncognitoTab = isIncognito;
        mTitle = new LightningViewTitle(activity);

        sMaxFling = ViewConfiguration.get(activity).getScaledMaximumFlingVelocity();

        mWebView.setDrawingCacheBackgroundColor(Color.WHITE);
        mWebView.setFocusableInTouchMode(true);
        mWebView.setFocusable(true);
        mWebView.setDrawingCacheEnabled(false);
        mWebView.setWillNotCacheDrawing(true);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            //noinspection deprecation
            mWebView.setAnimationCacheEnabled(false);
            //noinspection deprecation
            mWebView.setAlwaysDrawnWithCacheEnabled(false);
        }
        mWebView.setBackgroundColor(Color.WHITE);

        mWebView.setScrollbarFadingEnabled(true);
        mWebView.setSaveEnabled(true);
        mWebView.setNetworkAvailable(true);
        mWebView.setWebChromeClient(new LightningChromeClient(activity, this));
        mLightningWebClient = new LightningWebClient(activity, this);
        mWebView.setWebViewClient(mLightningWebClient);
        mWebView.setDownloadListener(new LightningDownloadListener(activity));
        mGestureDetector = new GestureDetector(activity, new CustomGestureListener());
        mWebView.setOnTouchListener(new TouchListener());
        sDefaultUserAgent = mWebView.getSettings().getUserAgentString();
        initializeSettings();
        initializePreferences(activity);

        if (url != null) {
            if (!url.trim().isEmpty()) {
                mWebView.loadUrl(url, mRequestHeaders);
            } else {
                // don't load anything, the user is looking for a blank tab
            }
        } else {
            loadHomepage();
        }
    }

    /**
     * Sets whether this tab was the
     * result of a new intent sent
     * to the browser.
     *
     * @param isNewTab true if it's from
     *                 a new intent,
     *                 false otherwise.
     */
    public void setIsNewTab(boolean isNewTab) {
        mIsNewTab = isNewTab;
    }

    /**
     * Returns whether this tab was created
     * as a result of a new intent.
     *
     * @return true if it was a new intent,
     * false otherwise.
     */
    public boolean isNewTab() {
        return mIsNewTab;
    }

    /**
     * This method loads the homepage for the browser. Either
     * it loads the URL stored as the homepage, or loads the
     * startpage or bookmark page if either of those are set
     * as the homepage.
     */
    public void loadHomepage() {
        if (mWebView == null) {
            return;
        }

        Preconditions.checkNonNull(sHomepage);
        switch (sHomepage) {
            case Constants.SCHEME_HOMEPAGE:
                loadStartpage();
                break;
            case Constants.SCHEME_BOOKMARKS:
                loadBookmarkpage();
                break;
            default:
                mWebView.loadUrl(sHomepage, mRequestHeaders);
                break;
        }
    }

    /**
     * This method gets the startpage URL from the {@link StartPage}
     * class asynchronously and loads the URL in the WebView on the
     * UI thread.
     */
    private void loadStartpage() {
        new StartPage().getHomepage()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    Preconditions.checkNonNull(item);
                    loadUrl(item);
                }
            });
    }

    /**
     * This method gets the bookmark page URL from the {@link BookmarkPage}
     * class asynchronously and loads the URL in the WebView on the
     * UI thread. It also caches the default folder icon locally.
     */
    public void loadBookmarkpage() {
        new BookmarkPage(mActivity).getBookmarkPage()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    Preconditions.checkNonNull(item);
                    loadUrl(item);
                }
            });
    }

    /**
     * This method gets the bookmark page URL from the {@link BookmarkPage}
     * class asynchronously and loads the URL in the WebView on the
     * UI thread. It also caches the default folder icon locally.
     */
    public void loadDownloadspage() {
        new DownloadsPage().getDownloadsPage()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    Preconditions.checkNonNull(item);
                    loadUrl(item);
                }
            });
    }

    /**
     * Initialize the preference driven settings of the WebView. This method
     * must be called whenever the preferences are changed within SharedPreferences.
     *
     * @param context the context in which the WebView was created, it is used
     *                to get the default UserAgent for the WebView.
     */
    @SuppressLint({"NewApi", "SetJavaScriptEnabled"})
    public synchronized void initializePreferences(@NonNull Context context) {
        if (mWebView == null) {
            return;
        }

        mLightningWebClient.updatePreferences();

        WebSettings settings = mWebView.getSettings();

        if (mPreferences.getDoNotTrackEnabled()) {
            mRequestHeaders.put(HEADER_DNT, "1");
        } else {
            mRequestHeaders.remove(HEADER_DNT);
        }

        if (mPreferences.getRemoveIdentifyingHeadersEnabled()) {
            mRequestHeaders.put(HEADER_REQUESTED_WITH, "");
            mRequestHeaders.put(HEADER_WAP_PROFILE, "");
        } else {
            mRequestHeaders.remove(HEADER_REQUESTED_WITH);
            mRequestHeaders.remove(HEADER_WAP_PROFILE);
        }

        settings.setDefaultTextEncodingName(mPreferences.getTextEncoding());
        sHomepage = mPreferences.getHomepage();
        setColorMode(mPreferences.getRenderingMode());

        if (!mIsIncognitoTab) {
            settings.setGeolocationEnabled(mPreferences.getLocationEnabled());
        } else {
            settings.setGeolocationEnabled(false);
        }
        if (API < Build.VERSION_CODES.KITKAT) {
            switch (mPreferences.getFlashSupport()) {
                case 0:
                    //noinspection deprecation
                    settings.setPluginState(PluginState.OFF);
                    break;
                case 1:
                    //noinspection deprecation
                    settings.setPluginState(PluginState.ON_DEMAND);
                    break;
                case 2:
                    //noinspection deprecation
                    settings.setPluginState(PluginState.ON);
                    break;
                default:
                    break;
            }
        }

        setUserAgent(context, mPreferences.getUserAgentChoice());

        if (mPreferences.getSavePasswordsEnabled() && !mIsIncognitoTab) {
            if (API < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                //noinspection deprecation
                settings.setSavePassword(true);
            }
            settings.setSaveFormData(true);
        } else {
            if (API < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                //noinspection deprecation
                settings.setSavePassword(false);
            }
            settings.setSaveFormData(false);
        }

        if (mPreferences.getJavaScriptEnabled()) {
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
        } else {
            settings.setJavaScriptEnabled(false);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
        }

        if (mPreferences.getTextReflowEnabled()) {
            settings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
            if (API >= android.os.Build.VERSION_CODES.KITKAT) {
                try {
                    settings.setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);
                } catch (Exception e) {
                    // This shouldn't be necessary, but there are a number
                    // of KitKat devices that crash trying to set this
                    Log.e(TAG, "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING");
                }
            }
        } else {
            settings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
        }

        settings.setBlockNetworkImage(mPreferences.getBlockImagesEnabled());
        if (!mIsIncognitoTab) {
            settings.setSupportMultipleWindows(mPreferences.getPopupsEnabled());
        } else {
            settings.setSupportMultipleWindows(false);
        }
        settings.setUseWideViewPort(mPreferences.getUseWideViewportEnabled());
        settings.setLoadWithOverviewMode(mPreferences.getOverviewModeEnabled());
        switch (mPreferences.getTextSize()) {
            case 0:
                settings.setTextZoom(200);
                break;
            case 1:
                settings.setTextZoom(150);
                break;
            case 2:
                settings.setTextZoom(125);
                break;
            case 3:
                settings.setTextZoom(100);
                break;
            case 4:
                settings.setTextZoom(75);
                break;
            case 5:
                settings.setTextZoom(50);
                break;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView,
                !mPreferences.getBlockThirdPartyCookiesEnabled());
        }
    }

    /**
     * Initialize the settings of the WebView that are intrinsic to Lightning and cannot
     * be altered by the user. Distinguish between Incognito and Regular tabs here.
     */
    @SuppressLint("NewApi")
    private void initializeSettings() {
        if (mWebView == null) {
            return;
        }
        final WebSettings settings = mWebView.getSettings();
        if (API < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            settings.setAppCacheMaxSize(Long.MAX_VALUE);
        }
        if (API < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //noinspection deprecation
            settings.setEnableSmoothTransition(true);
        }
        if (API > Build.VERSION_CODES.JELLY_BEAN) {
            settings.setMediaPlaybackRequiresUserGesture(true);
        }
        if (API >= Build.VERSION_CODES.LOLLIPOP && !mIsIncognitoTab) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        } else if (API >= Build.VERSION_CODES.LOLLIPOP) {
            // We're in Incognito mode, reject
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (!mIsIncognitoTab) {
            settings.setDomStorageEnabled(true);
            settings.setAppCacheEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setDatabaseEnabled(true);
        } else {
            settings.setDomStorageEnabled(false);
            settings.setAppCacheEnabled(false);
            settings.setDatabaseEnabled(false);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        if (API >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
        }

        getPathObservable("appcache").subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<File>() {
                @Override
                public void onItem(@Nullable File item) {
                    Preconditions.checkNonNull(item);
                    settings.setAppCachePath(item.getPath());
                }
            });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            getPathObservable("geolocation").subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(new SingleOnSubscribe<File>() {
                    @Override
                    public void onItem(@Nullable File item) {
                        Preconditions.checkNonNull(item);
                        //noinspection deprecation
                        settings.setGeolocationDatabasePath(item.getPath());
                    }
                });
        }

        getPathObservable("databases").subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<File>() {
                @Override
                public void onItem(@Nullable File item) {
                    if (API < Build.VERSION_CODES.KITKAT) {
                        Preconditions.checkNonNull(item);
                        //noinspection deprecation
                        settings.setDatabasePath(item.getPath());
                    }
                }

                @Override
                public void onComplete() {
                }
            });

    }

    @NonNull
    private Single<File> getPathObservable(final String subFolder) {
        return Single.create(new SingleAction<File>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<File> subscriber) {
                File file = mActivity.getDir(subFolder, 0);
                subscriber.onItem(file);
                subscriber.onComplete();
            }
        });
    }

    /**
     * Getter for the {@link LightningViewTitle} of the
     * current LightningView instance.
     *
     * @return a NonNull instance of LightningViewTitle
     */
    @NonNull
    public LightningViewTitle getTitleInfo() {
        return mTitle;
    }

    /**
     * Returns whether or not the current tab is incognito or not.
     *
     * @return true if this tab is incognito, false otherwise
     */
    public boolean isIncognito() {
        return mIsIncognitoTab;
    }

    /**
     * This method is used to toggle the user agent between desktop
     * and the current preference of the user.
     *
     * @param context the Context needed to set the user agent
     */
    public void toggleDesktopUA(@NonNull Context context) {
        if (mWebView == null)
            return;
        if (!mToggleDesktop)
            mWebView.getSettings().setUserAgentString(Constants.DESKTOP_USER_AGENT);
        else
            setUserAgent(context, mPreferences.getUserAgentChoice());
        mToggleDesktop = !mToggleDesktop;
    }

    /**
     * This method sets the user agent of the current tab.
     * There are four options, 1, 2, 3, 4.
     * <p/>
     * 1. use the default user agent
     * <p/>
     * 2. use the desktop user agent
     * <p/>
     * 3. use the mobile user agent
     * <p/>
     * 4. use a custom user agent, or the default user agent
     * if none was set.
     *
     * @param context the context needed to get the default user agent.
     * @param choice  the choice of user agent to use, see above comments.
     */
    @SuppressLint("NewApi")
    private void setUserAgent(Context context, int choice) {
        if (mWebView == null) return;
        WebSettings settings = mWebView.getSettings();
        switch (choice) {
            case 1:
                if (API >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    settings.setUserAgentString(WebSettings.getDefaultUserAgent(context));
                } else {
                    settings.setUserAgentString(sDefaultUserAgent);
                }
                break;
            case 2:
                settings.setUserAgentString(Constants.DESKTOP_USER_AGENT);
                break;
            case 3:
                settings.setUserAgentString(Constants.MOBILE_USER_AGENT);
                break;
            case 4:
                String ua = mPreferences.getUserAgentString(sDefaultUserAgent);
                if (ua == null || ua.isEmpty()) {
                    ua = " ";
                }
                settings.setUserAgentString(ua);
                break;
        }
    }

    /**
     * This method gets the additional headers that should be
     * added with each request the browser makes.
     *
     * @return a non null Map of Strings with the additional
     * request headers.
     */
    @NonNull
    Map<String, String> getRequestHeaders() {
        return mRequestHeaders;
    }

    /**
     * This method determines whether the current tab is visible or not.
     *
     * @return true if the WebView is non-null and visible, false otherwise.
     */
    public boolean isShown() {
        return mWebView != null && mWebView.isShown();
    }

    /**
     * Pause the current WebView instance.
     */
    public synchronized void onPause() {
        if (mWebView != null) {
            mWebView.onPause();
            Log.d(TAG, "WebView onPause: " + mWebView.getId());
        }
    }

    /**
     * Resume the current WebView instance.
     */
    public synchronized void onResume() {
        if (mWebView != null) {
            mWebView.onResume();
            Log.d(TAG, "WebView onResume: " + mWebView.getId());
        }
    }

    /**
     * Notify the LightningView that there is low memory and
     * for the WebView to free memory. Only applicable on
     * pre-Lollipop devices.
     */
    @Deprecated
    public synchronized void freeMemory() {
        if (mWebView != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //noinspection deprecation
            mWebView.freeMemory();
        }
    }

    /**
     * This method sets the tab as the foreground tab or
     * the background tab.
     *
     * @param isForeground true if the tab should be set as
     *                     foreground, false otherwise.
     */
    public void setIsForegroundTab(boolean isForeground) {
        mIsForegroundTab = isForeground;
        mUIController.tabChanged(this);
    }

    /**
     * Determines if the tab is in the foreground or not.
     *
     * @return true if the tab is the foreground tab,
     * false otherwise.
     */
    public boolean isForegroundTab() {
        return mIsForegroundTab;
    }

    /**
     * Gets the current progress of the WebView.
     *
     * @return returns a number between 0 and 100 with
     * the current progress of the WebView. If the WebView
     * is null, then the progress returned will be 100.
     */
    public int getProgress() {
        if (mWebView != null) {
            return mWebView.getProgress();
        } else {
            return 100;
        }
    }

    /**
     * Notify the WebView to stop the current load.
     */
    public synchronized void stopLoading() {
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    /**
     * This method forces the layer type to hardware, which
     * enables hardware rendering on the WebView instance
     * of the current LightningView.
     */
    private void setHardwareRendering() {
        if (mWebView == null) {
            return;
        }
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, mPaint);
    }

    /**
     * This method sets the layer type to none, which
     * means that either the GPU and CPU can both compose
     * the layers when necessary.
     */
    private void setNormalRendering() {
        if (mWebView == null) {
            return;
        }
        mWebView.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    /**
     * This method forces the layer type to software, which
     * disables hardware rendering on the WebView instance
     * of the current LightningView and makes the CPU render
     * the view.
     */
    public void setSoftwareRendering() {
        if (mWebView == null) {
            return;
        }
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * Sets the current rendering color of the WebView instance
     * of the current LightningView. The for modes are normal
     * rendering (0), inverted rendering (1), grayscale rendering (2),
     * and inverted grayscale rendering (3)
     *
     * @param mode the integer mode to set as the rendering mode.
     *             see the numbers in documentation above for the
     *             values this method accepts.
     */
    private void setColorMode(int mode) {
        mInvertPage = false;
        switch (mode) {
            case 0:
                mPaint.setColorFilter(null);
                // setSoftwareRendering(); // Some devices get segfaults
                // in the WebView with Hardware Acceleration enabled,
                // the only fix is to disable hardware rendering
                setNormalRendering();
                mInvertPage = false;
                break;
            case 1:
                ColorMatrixColorFilter filterInvert = new ColorMatrixColorFilter(
                    sNegativeColorArray);
                mPaint.setColorFilter(filterInvert);
                setHardwareRendering();

                mInvertPage = true;
                break;
            case 2:
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter filterGray = new ColorMatrixColorFilter(cm);
                mPaint.setColorFilter(filterGray);
                setHardwareRendering();
                break;
            case 3:
                ColorMatrix matrix = new ColorMatrix();
                matrix.set(sNegativeColorArray);
                ColorMatrix matrixGray = new ColorMatrix();
                matrixGray.setSaturation(0);
                ColorMatrix concat = new ColorMatrix();
                concat.setConcat(matrix, matrixGray);
                ColorMatrixColorFilter filterInvertGray = new ColorMatrixColorFilter(concat);
                mPaint.setColorFilter(filterInvertGray);
                setHardwareRendering();

                mInvertPage = true;
                break;

            case 4:
                ColorMatrixColorFilter IncreaseHighContrast = new ColorMatrixColorFilter(
                    sIncreaseContrastColorArray);
                mPaint.setColorFilter(IncreaseHighContrast);
                setHardwareRendering();
                break;

        }

    }

    /**
     * Pauses the JavaScript timers of the
     * WebView instance, which will trigger a
     * pause for all WebViews in the app.
     */
    public synchronized void pauseTimers() {
        if (mWebView != null) {
            mWebView.pauseTimers();
            Log.d(TAG, "Pausing JS timers");
        }
    }

    /**
     * Resumes the JavaScript timers of the
     * WebView instance, which will trigger a
     * resume for all WebViews in the app.
     */
    public synchronized void resumeTimers() {
        if (mWebView != null) {
            mWebView.resumeTimers();
            Log.d(TAG, "Resuming JS timers");
        }
    }

    /**
     * Requests focus down on the WebView instance
     * if the view does not already have focus.
     */
    public void requestFocus() {
        if (mWebView != null && !mWebView.hasFocus()) {
            mWebView.requestFocus();
        }
    }

    /**
     * Sets the visibility of the WebView to either
     * View.GONE, View.VISIBLE, or View.INVISIBLE.
     * other values passed in will have no effect.
     *
     * @param visible the visibility to set on the WebView.
     */
    public void setVisibility(int visible) {
        if (mWebView != null) {
            mWebView.setVisibility(visible);
        }
    }

    /**
     * Tells the WebView to reload the current page.
     * If the proxy settings are not ready then the
     * this method will not have an affect as the
     * proxy must start before the load occurs.
     */
    public synchronized void reload() {
        // Check if configured proxy is available
        if (!mProxyUtils.isProxyReady(mActivity)) {
            // User has been notified
            return;
        }

        if (mWebView != null) {
            mWebView.reload();
        }
    }

    /**
     * Finds all the instances of the text passed to this
     * method and highlights the instances of that text
     * in the WebView.
     *
     * @param text the text to search for.
     */
    @SuppressLint("NewApi")
    public synchronized void find(String text) {
        if (mWebView != null) {
            if (API >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mWebView.findAllAsync(text);
            } else {
                //noinspection deprecation
                mWebView.findAll(text);
            }
        }
    }

    /**
     * Notify the tab to shutdown and destroy
     * its WebView instance and to remove the reference
     * to it. After this method is called, the current
     * instance of the LightningView is useless as
     * the WebView cannot be recreated using the public
     * api.
     */
    // TODO fix bug where WebView.destroy is being called before the tab
    // is removed and would cause a memory leak if the parent check
    // was not in place.
    public synchronized void onDestroy() {
        if (mWebView != null) {
            // Check to make sure the WebView has been removed
            // before calling destroy() so that a memory leak is not created
            ViewGroup parent = (ViewGroup) mWebView.getParent();
            if (parent != null) {
                Log.e(TAG, "WebView was not detached from window before onDestroy");
                parent.removeView(mWebView);
            }
            mWebView.stopLoading();
            mWebView.onPause();
            mWebView.clearHistory();
            mWebView.setVisibility(View.GONE);
            mWebView.removeAllViews();
            mWebView.destroyDrawingCache();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                //this is causing the segfault occasionally below 4.2
                mWebView.destroy();
            }
            mWebView = null;
        }
    }

    /**
     * Tell the WebView to navigate backwards
     * in its history to the previous page.
     */
    public synchronized void goBack() {
        if (mWebView != null) {
            mWebView.goBack();
        }
    }

    /**
     * Tell the WebView to navigate forwards
     * in its history to the next page.
     */
    public synchronized void goForward() {
        if (mWebView != null) {
            mWebView.goForward();
        }
    }

    /**
     * Get the current user agent used
     * by the WebView.
     *
     * @return retuns the current user agent
     * of the WebView instance, or an empty
     * string if the WebView is null.
     */
    @NonNull
    private String getUserAgent() {
        if (mWebView != null) {
            return mWebView.getSettings().getUserAgentString();
        } else {
            return "";
        }
    }

    /**
     * Move the highlighted text in the WebView
     * to the next matched text. This method will
     * only have an affect after {@link LightningView#find(String)}
     * is called. Otherwise it will do nothing.
     */
    public synchronized void findNext() {
        if (mWebView != null) {
            mWebView.findNext(true);
        }
    }

    /**
     * Move the highlighted text in the WebView
     * to the previous matched text. This method will
     * only have an affect after {@link LightningView#find(String)}
     * is called. Otherwise it will do nothing.
     */
    public synchronized void findPrevious() {
        if (mWebView != null) {
            mWebView.findNext(false);
        }
    }

    /**
     * Clear the highlighted text in the WebView after
     * {@link LightningView#find(String)} has been called.
     * Otherwise it will have no affect.
     */
    public synchronized void clearFindMatches() {
        if (mWebView != null) {
            mWebView.clearMatches();
        }
    }

    /**
     * Gets whether or not the page rendering is inverted or not.
     * The main purpose of this is to indicate that JavaScript
     * should be run at the end of a page load to invert only
     * the images back to their uninverted states.
     *
     * @return true if the page is in inverted mode, false otherwise.
     */
    public boolean getInvertePage() {
        return mInvertPage;
    }

    /**
     * Handles a long click on the page and delegates the URL to the
     * proper dialog if it is not null, otherwise, it tries to get the
     * URL using HitTestResult.
     *
     * @param url the url that should have been obtained from the WebView touch node
     *            thingy, if it is null, this method tries to deal with it and find
     *            a workaround.
     */
    private void longClickPage(@Nullable final String url) {
        if (mWebView == null) {
            return;
        }
        final WebView.HitTestResult result = mWebView.getHitTestResult();
        String currentUrl = mWebView.getUrl();
        if (currentUrl != null && UrlUtils.isSpecialUrl(currentUrl)) {
            if (UrlUtils.isHistoryUrl(currentUrl)) {
                if (url != null) {
                    mDialogBuilder.showLongPressedHistoryLinkDialog(mActivity, mUIController, url);
                } else if (result != null && result.getExtra() != null) {
                    final String newUrl = result.getExtra();
                    mDialogBuilder.showLongPressedHistoryLinkDialog(mActivity, mUIController, newUrl);
                }
            } else if (UrlUtils.isBookmarkUrl(currentUrl)) {
                if (url != null) {
                    mDialogBuilder.showLongPressedDialogForBookmarkUrl(mActivity, mUIController, url);
                } else if (result != null && result.getExtra() != null) {
                    final String newUrl = result.getExtra();
                    mDialogBuilder.showLongPressedDialogForBookmarkUrl(mActivity, mUIController, newUrl);
                }
            } else if (UrlUtils.isDownloadsUrl(currentUrl)) {
                if (url != null) {
                    mDialogBuilder.showLongPressedDialogForDownloadUrl(mActivity, mUIController, url);
                } else if (result != null && result.getExtra() != null) {
                    final String newUrl = result.getExtra();
                    mDialogBuilder.showLongPressedDialogForDownloadUrl(mActivity, mUIController, newUrl);
                }
            }
        } else {
            if (url != null) {
                if (result != null) {
                    if (result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.getType() == WebView.HitTestResult.IMAGE_TYPE) {
                        mDialogBuilder.showLongPressImageDialog(mActivity, mUIController, url, getUserAgent());
                    } else {
                        mDialogBuilder.showLongPressLinkDialog(mActivity, mUIController, url);
                    }
                } else {
                    mDialogBuilder.showLongPressLinkDialog(mActivity, mUIController, url);
                }
            } else if (result != null && result.getExtra() != null) {
                final String newUrl = result.getExtra();
                if (result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.getType() == WebView.HitTestResult.IMAGE_TYPE) {
                    mDialogBuilder.showLongPressImageDialog(mActivity, mUIController, newUrl, getUserAgent());
                } else {
                    mDialogBuilder.showLongPressLinkDialog(mActivity, mUIController, newUrl);
                }
            }
        }
    }

    /**
     * Determines whether or not the WebView can go
     * backward or if it as the end of its history.
     *
     * @return true if the WebView can go back, false otherwise.
     */
    public boolean canGoBack() {
        return mWebView != null && mWebView.canGoBack();
    }

    /**
     * Determine whether or not the WebView can go
     * forward or if it is at the front of its history.
     *
     * @return true if it can go forward, false otherwise.
     */
    public boolean canGoForward() {
        return mWebView != null && mWebView.canGoForward();
    }

    /**
     * Gets the current WebView instance of the tab.
     *
     * @return the WebView instance of the tab, which
     * can be null.
     */
    @Nullable
    public synchronized WebView getWebView() {
        return mWebView;
    }

    /**
     * Gets the favicon currently in use by
     * the page. If the current page does not
     * have a favicon, it returns a default
     * icon.
     *
     * @return a non-null Bitmap with the
     * current favicon.
     */
    @NonNull
    public Bitmap getFavicon() {
        return mTitle.getFavicon(mUIController.getUseDarkTheme());
    }

    /**
     * Loads the URL in the WebView. If the proxy settings
     * are still initializing, then the URL will not load
     * as it is necessary to have the settings initialized
     * before a load occurs.
     *
     * @param url the non-null URL to attempt to load in
     *            the WebView.
     */
    public synchronized void loadUrl(@NonNull String url) {
        // Check if configured proxy is available
        if (!mProxyUtils.isProxyReady(mActivity)) {
            return;
        }

        if (mWebView != null) {
            mWebView.loadUrl(url, mRequestHeaders);
        }
    }

    /**
     * Get the current title of the page, retrieved from
     * the title object.
     *
     * @return the title of the page, or an empty string
     * if there is no title.
     */
    @NonNull
    public String getTitle() {
        return mTitle.getTitle();
    }

    /**
     * Get the current URL of the WebView, or an empty
     * string if the WebView is null or the URL is null.
     *
     * @return the current URL or an empty string.
     */
    @NonNull
    public String getUrl() {
        if (mWebView != null && mWebView.getUrl() != null) {
            return mWebView.getUrl();
        } else {
            return "";
        }
    }

    /**
     * The OnTouchListener used by the WebView so we can
     * get scroll events and show/hide the action bar when
     * the page is scrolled up/down.
     */
    private class TouchListener implements OnTouchListener {

        float mLocation;
        float mY;
        int mAction;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(@Nullable View view, @NonNull MotionEvent arg1) {
            if (view == null)
                return false;

            if (!view.hasFocus()) {
                view.requestFocus();
            }
            mAction = arg1.getAction();
            mY = arg1.getY();
            if (mAction == MotionEvent.ACTION_DOWN) {
                mLocation = mY;
            } else if (mAction == MotionEvent.ACTION_UP) {
                final float distance = (mY - mLocation);
                if (distance > SCROLL_UP_THRESHOLD && view.getScrollY() < SCROLL_UP_THRESHOLD) {
                    mUIController.showActionBar();
                } else if (distance < -SCROLL_UP_THRESHOLD) {
                    mUIController.hideActionBar();
                }
                mLocation = 0;
            }
            mGestureDetector.onTouchEvent(arg1);
            return false;
        }
    }

    /**
     * The SimpleOnGestureListener used by the {@link TouchListener}
     * in order to delegate show/hide events to the action bar when
     * the user flings the page. Also handles long press events so
     * that we can capture them accurately.
     */
    private class CustomGestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int power = (int) (velocityY * 100 / sMaxFling);
            if (power < -10) {
                mUIController.hideActionBar();
            } else if (power > 15) {
                mUIController.showActionBar();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        /**
         * Without this, onLongPress is not called when user is zooming using
         * two fingers, but is when using only one.
         * <p/>
         * The required behaviour is to not trigger this when the user is
         * zooming, it shouldn't matter how much fingers the user's using.
         */
        private boolean mCanTriggerLongPress = true;

        @Override
        public void onLongPress(MotionEvent e) {
            if (mCanTriggerLongPress) {
                Message msg = mWebViewHandler.obtainMessage();
                if (msg != null) {
                    msg.setTarget(mWebViewHandler);
                    if (mWebView == null) {
                        return;
                    }
                    mWebView.requestFocusNodeHref(msg);
                }
            }
        }

        /**
         * Is called when the user is swiping after the doubletap, which in our
         * case means that he is zooming.
         */
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            mCanTriggerLongPress = false;
            return false;
        }

        /**
         * Is called when something is starting being pressed, always before
         * onLongPress.
         */
        @Override
        public void onShowPress(MotionEvent e) {
            mCanTriggerLongPress = true;
        }
    }

    /**
     * A Handler used to get the URL from a long click
     * event on the WebView. It does not hold a hard
     * reference to the WebView and therefore will not
     * leak it if the WebView is garbage collected.
     */
    private static class WebViewHandler extends Handler {

        @NonNull private final WeakReference<LightningView> mReference;

        WebViewHandler(@NonNull LightningView view) {
            mReference = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            final String url = msg.getData().getString("url");
            LightningView view = mReference.get();
            if (view != null) {
                view.longClickPage(url);
            }
        }
    }
}
