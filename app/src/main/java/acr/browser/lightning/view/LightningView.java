/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.controller.BrowserController;
import acr.browser.lightning.download.LightningDownloadListener;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.AdBlock;
import acr.browser.lightning.utils.IntentUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

public class LightningView {

    private final Title mTitle;
    private WebView mWebView;
    private boolean mIsIncognitoTab;
    private BrowserController mBrowserController;
    private GestureDetector mGestureDetector;
    private final Activity mActivity;
    private static String mHomepage;
    private static String mDefaultUserAgent;
    // TODO fix so that mWebpageBitmap can be static - static changes the icon when switching from light to dark and then back to light
    private Bitmap mWebpageBitmap;
    private static PreferenceManager mPreferences;
    private final AdBlock mAdBlock;
    private IntentUtils mIntentUtils;
    private final Paint mPaint = new Paint();
    private boolean isForegroundTab;
    private boolean mTextReflow = false;
    private boolean mInvertPage = false;
    private boolean mToggleDesktop = false;
    private static float mMaxFling;
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private static final int SCROLL_UP_THRESHOLD = Utils.dpToPx(10);
    private static final float[] mNegativeColorArray = {-1.0f, 0, 0, 0, 255, // red
            0, -1.0f, 0, 0, 255, // green
            0, 0, -1.0f, 0, 255, // blue
            0, 0, 0, 1.0f, 0 // alpha
    };

    public LightningView(Activity activity, String url, boolean darkTheme, boolean isIncognito) {

        mActivity = activity;
        mWebView = new WebView(activity);
        mIsIncognitoTab = isIncognito;
        mTitle = new Title(activity, darkTheme);
        mAdBlock = AdBlock.getInstance(activity.getApplicationContext());

        mWebpageBitmap = mTitle.mDefaultIcon;

        mMaxFling = ViewConfiguration.get(activity).getScaledMaximumFlingVelocity();

        try {
            mBrowserController = (BrowserController) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement BrowserController");
        }
        mIntentUtils = new IntentUtils(mBrowserController);
        mWebView.setDrawingCacheBackgroundColor(0x00000000);
        mWebView.setFocusableInTouchMode(true);
        mWebView.setFocusable(true);
        mWebView.setAnimationCacheEnabled(false);
        mWebView.setDrawingCacheEnabled(false);
        mWebView.setWillNotCacheDrawing(true);
        mWebView.setAlwaysDrawnWithCacheEnabled(false);
        mWebView.setBackgroundColor(0);

        if (API >= Build.VERSION_CODES.JELLY_BEAN) {
            mWebView.setBackground(null);
            mWebView.getRootView().setBackground(null);
        } else if (mWebView.getRootView() != null) {
            mWebView.getRootView().setBackgroundDrawable(null);
        }
        mWebView.setScrollbarFadingEnabled(true);
        mWebView.setSaveEnabled(true);
        mWebView.setNetworkAvailable(true);
        mWebView.setWebChromeClient(new LightningChromeClient(activity));
        mWebView.setWebViewClient(new LightningWebClient(activity));
        mWebView.setDownloadListener(new LightningDownloadListener(activity));
        mGestureDetector = new GestureDetector(activity, new CustomGestureListener());
        mWebView.setOnTouchListener(new TouchListener());
        mDefaultUserAgent = mWebView.getSettings().getUserAgentString();
        initializeSettings(mWebView.getSettings(), activity);
        initializePreferences(mWebView.getSettings(), activity);

        if (url != null) {
            if (!url.trim().isEmpty()) {
                mWebView.loadUrl(url);
            } else {
                // don't load anything, the user is looking for a blank tab
            }
        } else {
            if (mHomepage.startsWith("about:home")) {
                mWebView.loadUrl(getHomepage());
            } else if (mHomepage.startsWith("about:bookmarks")) {
                mBrowserController.openBookmarkPage(mWebView);
            } else {
                mWebView.loadUrl(mHomepage);
            }
        }
    }

    /**
     * This method builds the homepage and returns the local URL to be loaded
     * when it finishes building.
     *
     * @return the URL to load
     */
    private String getHomepage() {
        StringBuilder homepageBuilder = new StringBuilder();
        homepageBuilder.append(StartPage.HEAD);
        String icon;
        String searchUrl;
        switch (mPreferences.getSearchChoice()) {
            case 0:
                // CUSTOM SEARCH
                icon = "file:///android_asset/lightning.png";
                searchUrl = mPreferences.getSearchUrl();
                break;
            case 1:
                // GOOGLE_SEARCH;
                icon = "file:///android_asset/google.png";
                // "https://www.google.com/images/srpr/logo11w.png";
                searchUrl = Constants.GOOGLE_SEARCH;
                break;
            case 2:
                // ANDROID SEARCH;
                icon = "file:///android_asset/ask.png";
                searchUrl = Constants.ASK_SEARCH;
                break;
            case 3:
                // BING_SEARCH;
                icon = "file:///android_asset/bing.png";
                // "http://upload.wikimedia.org/wikipedia/commons/thumb/b/b1/Bing_logo_%282013%29.svg/500px-Bing_logo_%282013%29.svg.png";
                searchUrl = Constants.BING_SEARCH;
                break;
            case 4:
                // YAHOO_SEARCH;
                icon = "file:///android_asset/yahoo.png";
                // "http://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Yahoo%21_logo.svg/799px-Yahoo%21_logo.svg.png";
                searchUrl = Constants.YAHOO_SEARCH;
                break;
            case 5:
                // STARTPAGE_SEARCH;
                icon = "file:///android_asset/startpage.png";
                // "https://startpage.com/graphics/startp_logo.gif";
                searchUrl = Constants.STARTPAGE_SEARCH;
                break;
            case 6:
                // STARTPAGE_MOBILE
                icon = "file:///android_asset/startpage.png";
                // "https://startpage.com/graphics/startp_logo.gif";
                searchUrl = Constants.STARTPAGE_MOBILE_SEARCH;
                break;
            case 7:
                // DUCK_SEARCH;
                icon = "file:///android_asset/duckduckgo.png";
                // "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
                searchUrl = Constants.DUCK_SEARCH;
                break;
            case 8:
                // DUCK_LITE_SEARCH;
                icon = "file:///android_asset/duckduckgo.png";
                // "https://duckduckgo.com/assets/logo_homepage.normal.v101.png";
                searchUrl = Constants.DUCK_LITE_SEARCH;
                break;
            case 9:
                // BAIDU_SEARCH;
                icon = "file:///android_asset/baidu.png";
                // "http://www.baidu.com/img/bdlogo.gif";
                searchUrl = Constants.BAIDU_SEARCH;
                break;
            case 10:
                // YANDEX_SEARCH;
                icon = "file:///android_asset/yandex.png";
                // "http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png";
                searchUrl = Constants.YANDEX_SEARCH;
                break;
            default:
                // DEFAULT GOOGLE_SEARCH;
                icon = "file:///android_asset/google.png";
                searchUrl = Constants.GOOGLE_SEARCH;
                break;

        }

        homepageBuilder.append(icon);
        homepageBuilder.append(StartPage.MIDDLE);
        homepageBuilder.append(searchUrl);
        homepageBuilder.append(StartPage.END);

        File homepage = new File(mActivity.getFilesDir(), StartPage.FILENAME);
        FileWriter hWriter = null;
        try {
            hWriter = new FileWriter(homepage, false);
            hWriter.write(homepageBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(hWriter);
        }

        return Constants.FILE + homepage;
    }

    /**
     * Initialize the preference driven settings of the WebView
     *
     * @param settings the WebSettings object to use, you can pass in null
     *                 if you don't have a reference to them
     * @param context the context in which the WebView was created
     */
    public synchronized void initializePreferences(@Nullable WebSettings settings, Context context) {
        if (settings == null && mWebView == null) {
            return;
        } else if (settings == null) {
            settings = mWebView.getSettings();
        }
        mPreferences = PreferenceManager.getInstance();

        settings.setDefaultTextEncodingName(mPreferences.getTextEncoding());
        mHomepage = mPreferences.getHomepage();
        mAdBlock.updatePreference();

        setColorMode(mPreferences.getRenderingMode());

        if (!mIsIncognitoTab) {
            settings.setGeolocationEnabled(mPreferences.getLocationEnabled());
        } else {
            settings.setGeolocationEnabled(false);
        }
        if (API < 19) {
            switch (mPreferences.getFlashSupport()) {
                case 0:
                    settings.setPluginState(PluginState.OFF);
                    break;
                case 1:
                    settings.setPluginState(PluginState.ON_DEMAND);
                    break;
                case 2:
                    settings.setPluginState(PluginState.ON);
                    break;
                default:
                    break;
            }
        }

        setUserAgent(context, mPreferences.getUserAgentChoice());

        if (mPreferences.getSavePasswordsEnabled() && !mIsIncognitoTab) {
            if (API < 18) {
                settings.setSavePassword(true);
            }
            settings.setSaveFormData(true);
        } else {
            if (API < 18) {
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
            mTextReflow = true;
            settings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
            if (API >= android.os.Build.VERSION_CODES.KITKAT) {
                try {
                    settings.setLayoutAlgorithm(LayoutAlgorithm.TEXT_AUTOSIZING);
                } catch (Exception e) {
                    // This shouldn't be necessary, but there are a number
                    // of KitKat devices that crash trying to set this
                    Log.e(Constants.TAG, "Problem setting LayoutAlgorithm to TEXT_AUTOSIZING");
                }
            }
        } else {
            mTextReflow = false;
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
     *
     * @param settings the WebSettings object to use.
     * @param context the Context which was used to construct the WebView.
     */
    private void initializeSettings(WebSettings settings, Context context) {
        if (API < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            settings.setAppCacheMaxSize(Long.MAX_VALUE);
        }
        if (API < Build.VERSION_CODES.JELLY_BEAN_MR1) {
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

        settings.setAppCachePath(context.getDir("appcache", 0).getPath());
        settings.setGeolocationDatabasePath(context.getDir("geolocation", 0).getPath());
        if (API < Build.VERSION_CODES.KITKAT) {
            settings.setDatabasePath(context.getDir("databases", 0).getPath());
        }
    }

    public void toggleDesktopUA(@NonNull Context context) {
        if (mWebView == null)
            return;
        if (!mToggleDesktop)
            mWebView.getSettings().setUserAgentString(Constants.DESKTOP_USER_AGENT);
        else
            setUserAgent(context, mPreferences.getUserAgentChoice());
        mToggleDesktop = !mToggleDesktop;
    }

    public void setUserAgent(Context context, int choice) {
        if (mWebView == null) return;
        WebSettings settings = mWebView.getSettings();
        switch (choice) {
            case 1:
                if (API > 16) {
                    settings.setUserAgentString(WebSettings.getDefaultUserAgent(context));
                } else {
                    settings.setUserAgentString(mDefaultUserAgent);
                }
                break;
            case 2:
                settings.setUserAgentString(Constants.DESKTOP_USER_AGENT);
                break;
            case 3:
                settings.setUserAgentString(Constants.MOBILE_USER_AGENT);
                break;
            case 4:
                settings.setUserAgentString(mPreferences.getUserAgentString(mDefaultUserAgent));
                break;
        }
    }

    public boolean isShown() {
        return mWebView != null && mWebView.isShown();
    }

    public synchronized void onPause() {
        if (mWebView != null)
            mWebView.onPause();
    }

    public synchronized void onResume() {
        if (mWebView != null)
            mWebView.onResume();
    }

    public synchronized void freeMemory() {
        if (mWebView != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            mWebView.freeMemory();
    }

    public void setForegroundTab(boolean isForeground) {
        isForegroundTab = isForeground;
        mBrowserController.update();
    }

    public boolean isForegroundTab() {
        return isForegroundTab;
    }

    public int getProgress() {
        if (mWebView != null) {
            return mWebView.getProgress();
        } else {
            return 100;
        }
    }

    public synchronized void stopLoading() {
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    private void setHardwareRendering() {
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, mPaint);
    }

    private void setNormalRendering() {
        mWebView.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    public void setSoftwareRendering() {
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

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
                        mNegativeColorArray);
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
                matrix.set(mNegativeColorArray);
                ColorMatrix matrixGray = new ColorMatrix();
                matrixGray.setSaturation(0);
                ColorMatrix concat = new ColorMatrix();
                concat.setConcat(matrix, matrixGray);
                ColorMatrixColorFilter filterInvertGray = new ColorMatrixColorFilter(concat);
                mPaint.setColorFilter(filterInvertGray);
                setHardwareRendering();

                mInvertPage = true;
                break;

        }

    }

    public synchronized void pauseTimers() {
        if (mWebView != null) {
            mWebView.pauseTimers();
        }
    }

    public synchronized void resumeTimers() {
        if (mWebView != null) {
            mWebView.resumeTimers();
        }
    }

    public void requestFocus() {
        if (mWebView != null && !mWebView.hasFocus()) {
            mWebView.requestFocus();
        }
    }

    public void setVisibility(int visible) {
        if (mWebView != null) {
            mWebView.setVisibility(visible);
        }
    }

    public synchronized void reload() {
        // Check if configured proxy is available
        if (mBrowserController.proxyIsNotReady()) {
            // User has been notified
            return;
        }

        if (mWebView != null) {
            mWebView.reload();
        }
    }

    private void cacheFavicon(Bitmap icon) {
        if (icon == null) return;

        String hash = String.valueOf(Utils.getDomainName(getUrl()).hashCode());
        Log.d(Constants.TAG, "Caching icon for " + Utils.getDomainName(getUrl()));
        FileOutputStream fos = null;
        try {
            File image = new File(mActivity.getCacheDir(), hash + ".png");
            fos = new FileOutputStream(image);
            icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(fos);
        }
    }

    public synchronized void find(String text) {
        if (mWebView != null) {
            if (API > 16) {
                mWebView.findAllAsync(text);
            } else {
                mWebView.findAll(text);
            }
        }
    }

    public synchronized void onDestroy() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.onPause();
            mWebView.clearHistory();
            mWebView.setVisibility(View.GONE);
            mWebView.removeAllViews();
            mWebView.destroyDrawingCache();
            // mWebView.destroy(); //this is causing the segfault
            mWebView = null;
        }
    }

    public synchronized void goBack() {
        if (mWebView != null) {
            mWebView.goBack();
        }
    }

    public String getUserAgent() {
        if (mWebView != null) {
            return mWebView.getSettings().getUserAgentString();
        } else {
            return "";
        }
    }

    public synchronized void goForward() {
        if (mWebView != null) {
            mWebView.goForward();
        }
    }

    public boolean canGoBack() {
        return mWebView != null && mWebView.canGoBack();
    }

    public boolean canGoForward() {
        return mWebView != null && mWebView.canGoForward();
    }

    @Nullable
    public WebView getWebView() {
        return mWebView;
    }

    public Bitmap getFavicon() {
        return mTitle.getFavicon();
    }

    public synchronized void loadUrl(String url) {
        // Check if configured proxy is available
        if (mBrowserController.proxyIsNotReady()) {
            // User has been notified
            return;
        }

        if (mWebView != null) {
            mWebView.loadUrl(url);
        }
    }

    public synchronized void invalidate() {
        if (mWebView != null) {
            mWebView.invalidate();
        }
    }

    public String getTitle() {
        return mTitle.getTitle();
    }

    @NonNull
    public String getUrl() {
        if (mWebView != null && mWebView.getUrl() != null) {
            return mWebView.getUrl();
        } else {
            return "";
        }
    }

    public class LightningWebClient extends WebViewClient {

        final Context mActivity;

        LightningWebClient(Context context) {
            mActivity = context;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mAdBlock.isAd(request.getUrl().toString())) {
                    ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
                    return new WebResourceResponse("text/plain", "utf-8", EMPTY);
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (mAdBlock.isAd(url)) {
                ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
                return new WebResourceResponse("text/plain", "utf-8", EMPTY);
            }
            return null;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (view.isShown()) {
                mBrowserController.updateUrl(url, true);
                mBrowserController.updateBookmarkIndicator(url);
                view.postInvalidate();
            }
            if (view.getTitle() == null || view.getTitle().isEmpty()) {
                mTitle.setTitle(mActivity.getString(R.string.untitled));
            } else {
                mTitle.setTitle(view.getTitle());
            }
            if (API >= android.os.Build.VERSION_CODES.KITKAT && mInvertPage) {
                view.evaluateJavascript(Constants.JAVASCRIPT_INVERT_PAGE, null);
            }
            mBrowserController.update();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (isShown()) {
                mBrowserController.updateUrl(url, false);
                mBrowserController.updateBookmarkIndicator(url);
                mBrowserController.showActionBar();
            }
            mTitle.setFavicon(mWebpageBitmap);
            mBrowserController.update();
        }

        @Override
        public void onReceivedHttpAuthRequest(final WebView view, @NonNull final HttpAuthHandler handler,
                                              final String host, final String realm) {

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            final EditText name = new EditText(mActivity);
            final EditText password = new EditText(mActivity);
            LinearLayout passLayout = new LinearLayout(mActivity);
            passLayout.setOrientation(LinearLayout.VERTICAL);

            passLayout.addView(name);
            passLayout.addView(password);

            name.setHint(mActivity.getString(R.string.hint_username));
            name.setSingleLine();
            password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.setSingleLine();
            password.setTransformationMethod(new PasswordTransformationMethod());
            password.setHint(mActivity.getString(R.string.hint_password));
            builder.setTitle(mActivity.getString(R.string.title_sign_in));
            builder.setView(passLayout);
            builder.setCancelable(true)
                    .setPositiveButton(mActivity.getString(R.string.title_sign_in),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    String user = name.getText().toString();
                                    String pass = password.getText().toString();
                                    handler.proceed(user.trim(), pass.trim());
                                    Log.d(Constants.TAG, "Request Login");

                                }
                            })
                    .setNegativeButton(mActivity.getString(R.string.action_cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    handler.cancel();

                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();

        }

        private boolean mIsRunning = false;
        private float mZoomScale = 0.0f;

        @Override
        public void onScaleChanged(final WebView view, final float oldScale, final float newScale) {
            if (view.isShown() && mTextReflow && API >= android.os.Build.VERSION_CODES.KITKAT) {
                if (mIsRunning)
                    return;
                if (Math.abs(mZoomScale - newScale) > 0.01f) {
                    mIsRunning = view.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            mZoomScale = newScale;
                            view.evaluateJavascript(Constants.JAVASCRIPT_TEXT_REFLOW, null);
                            mIsRunning = false;
                        }

                    }, 100);
                }

            }
        }

        @Override
        public void onReceivedSslError(WebView view, @NonNull final SslErrorHandler handler, SslError error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(mActivity.getString(R.string.title_warning));
            builder.setMessage(mActivity.getString(R.string.message_untrusted_certificate))
                    .setCancelable(true)
                    .setPositiveButton(mActivity.getString(R.string.action_yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    handler.proceed();
                                }
                            })
                    .setNegativeButton(mActivity.getString(R.string.action_no),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    handler.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
                alert.show();
            } else {
                handler.proceed();
            }

        }

        @Override
        public void onFormResubmission(WebView view, @NonNull final Message dontResend, final Message resend) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(mActivity.getString(R.string.title_form_resubmission));
            builder.setMessage(mActivity.getString(R.string.message_form_resubmission))
                    .setCancelable(true)
                    .setPositiveButton(mActivity.getString(R.string.action_yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    resend.sendToTarget();
                                }
                            })
                    .setNegativeButton(mActivity.getString(R.string.action_no),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    dontResend.sendToTarget();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Check if configured proxy is available
            if (mBrowserController.proxyIsNotReady()) {
                // User has been notified
                return true;
            }

            if (mIsIncognitoTab) {
                return super.shouldOverrideUrlLoading(view, url);
            }
            if (url.startsWith("about:")) {
                return super.shouldOverrideUrlLoading(view, url);
            }
            if (url.contains("mailto:")) {
                MailTo mailTo = MailTo.parse(url);
                Intent i = Utils.newEmailIntent(mailTo.getTo(), mailTo.getSubject(),
                        mailTo.getBody(), mailTo.getCc());
                mActivity.startActivity(i);
                view.reload();
                return true;
            } else if (url.startsWith("intent://")) {
                Intent intent;
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                } catch (URISyntaxException ex) {
                    return false;
                }
                if (intent != null) {
                    try {
                        mActivity.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(Constants.TAG, "ActivityNotFoundException");
                    }
                    return true;
                }
            }
            return mIntentUtils.startActivityForUrl(mWebView, url);
        }
    }

    public class LightningChromeClient extends WebChromeClient {

        final Context mActivity;

        LightningChromeClient(Context context) {
            mActivity = context;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (isShown()) {
                mBrowserController.updateProgress(newProgress);
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            if (icon == null)
                return;
            mTitle.setFavicon(icon);
            mBrowserController.update();
            cacheFavicon(icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (title != null && !title.isEmpty()) {
                mTitle.setTitle(title);
            } else {
                mTitle.setTitle(mActivity.getString(R.string.untitled));
            }
            mBrowserController.update();
            if (view != null)
                mBrowserController.updateHistory(title, view.getUrl());
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin,
                                                       final GeolocationPermissions.Callback callback) {
            final boolean remember = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(mActivity.getString(R.string.location));
            String org;
            if (origin.length() > 50) {
                org = origin.subSequence(0, 50) + "...";
            } else {
                org = origin;
            }
            builder.setMessage(org + mActivity.getString(R.string.message_location))
                    .setCancelable(true)
                    .setPositiveButton(mActivity.getString(R.string.action_allow),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    callback.invoke(origin, true, remember);
                                }
                            })
                    .setNegativeButton(mActivity.getString(R.string.action_dont_allow),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    callback.invoke(origin, false, remember);
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();

        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                      Message resultMsg) {
            mBrowserController.onCreateWindow(resultMsg);
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            mBrowserController.onCloseWindow(LightningView.this);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            mBrowserController.openFileChooser(uploadMsg);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            mBrowserController.openFileChooser(uploadMsg);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mBrowserController.openFileChooser(uploadMsg);
        }

        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams) {
            mBrowserController.showFileChooser(filePathCallback);
            return true;
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            return mBrowserController.getDefaultVideoPoster();
        }

        @Override
        public View getVideoLoadingProgressView() {
            return mBrowserController.getVideoLoadingProgressView();
        }

        @Override
        public void onHideCustomView() {
            mBrowserController.onHideCustomView();
            super.onHideCustomView();
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // While these lines might look like they work, in practice,
            // Full-screen videos won't work correctly. I may test this out some
            // more
            // if (view instanceof FrameLayout) {
            // FrameLayout frame = (FrameLayout) view;
            // if (frame.getFocusedChild() instanceof VideoView) {
            // VideoView video = (VideoView) frame.getFocusedChild();
            // video.stopPlayback();
            // frame.removeView(video);
            // video.setVisibility(View.GONE);
            // }
            // } else {
            mBrowserController.onShowCustomView(view, callback);

            // }

            super.onShowCustomView(view, callback);
        }

        @Override
        @Deprecated
        public void onShowCustomView(View view, int requestedOrientation,
                                     CustomViewCallback callback) {
            // While these lines might look like they work, in practice,
            // Full-screen videos won't work correctly. I may test this out some
            // more
            // if (view instanceof FrameLayout) {
            // FrameLayout frame = (FrameLayout) view;
            // if (frame.getFocusedChild() instanceof VideoView) {
            // VideoView video = (VideoView) frame.getFocusedChild();
            // video.stopPlayback();
            // frame.removeView(video);
            // video.setVisibility(View.GONE);
            // }
            // } else {
            mBrowserController.onShowCustomView(view, callback);

            // }

            super.onShowCustomView(view, requestedOrientation, callback);
        }
    }

    public class Title {

        private Bitmap mFavicon;
        private String mTitle;
        private final Bitmap mDefaultIcon;

        public Title(Context context, boolean darkTheme) {
            mDefaultIcon = ThemeUtils.getThemedBitmap(context, R.drawable.ic_webpage, darkTheme);
            mFavicon = mDefaultIcon;
            mTitle = mActivity.getString(R.string.action_new_tab);
        }

        public void setFavicon(Bitmap favicon) {
            if (favicon == null) {
                mFavicon = mDefaultIcon;
            } else {
                mFavicon = Utils.padFavicon(favicon);
            }
        }

        public void setTitle(String title) {
            if (title == null) {
                mTitle = "";
            } else {
                mTitle = title;
            }
        }

        public void setTitleAndFavicon(String title, Bitmap favicon) {
            mTitle = title;

            if (favicon == null) {
                mFavicon = mDefaultIcon;
            } else {
                mFavicon = Utils.padFavicon(favicon);
            }
        }

        public String getTitle() {
            return mTitle;
        }

        public Bitmap getFavicon() {
            return mFavicon;
        }

    }

    private class TouchListener implements OnTouchListener {

        float mLocation;
        float mY;
        int mAction;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent arg1) {
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
                    mBrowserController.showActionBar();
                } else if (distance < -SCROLL_UP_THRESHOLD) {
                    mBrowserController.hideActionBar();
                }
                mLocation = 0;
            }
            mGestureDetector.onTouchEvent(arg1);
            return false;
        }
    }

    private class CustomGestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int power = (int) (velocityY * 100 / mMaxFling);
            if (power < -10) {
                mBrowserController.hideActionBar();
            } else if (power > 15) {
                mBrowserController.showActionBar();
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
            if (mCanTriggerLongPress)
                mBrowserController.onLongPress();
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
}
