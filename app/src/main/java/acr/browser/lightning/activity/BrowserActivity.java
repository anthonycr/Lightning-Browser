/*
 * Copyright 2015 Anthony Restaino
 */

package acr.browser.lightning.activity;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.VideoView;

import com.anthonycr.grant.PermissionsManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import acr.browser.lightning.BuildConfig;
import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.browser.BrowserPresenter;
import acr.browser.lightning.browser.BrowserView;
import acr.browser.lightning.browser.TabsView;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.bus.NavigationEvents;
import acr.browser.lightning.bus.TabEvents;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.fragment.BookmarksFragment;
import acr.browser.lightning.fragment.TabsFragment;
import acr.browser.lightning.object.SearchAdapter;
import acr.browser.lightning.react.Observable;
import acr.browser.lightning.react.Schedulers;
import acr.browser.lightning.receiver.NetworkReceiver;
import acr.browser.lightning.utils.DrawableUtils;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.utils.WebUtils;
import acr.browser.lightning.view.AnimatedProgressBar;
import acr.browser.lightning.view.LightningView;
import butterknife.Bind;
import butterknife.ButterKnife;

public abstract class BrowserActivity extends ThemableBrowserActivity implements BrowserView, UIController, OnClickListener, OnLongClickListener {

    private static final String TAG = BrowserActivity.class.getSimpleName();

    private static final String INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER";

    // Static Layout
    @Bind(Window.ID_ANDROID_CONTENT) View mRoot;
    @Bind(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @Bind(R.id.content_frame) FrameLayout mBrowserFrame;
    @Bind(R.id.left_drawer) ViewGroup mDrawerLeft;
    @Bind(R.id.right_drawer) ViewGroup mDrawerRight;
    @Bind(R.id.ui_layout) ViewGroup mUiLayout;
    @Bind(R.id.toolbar_layout) ViewGroup mToolbarLayout;
    @Bind(R.id.progress_view) AnimatedProgressBar mProgressBar;
    @Bind(R.id.search_bar) RelativeLayout mSearchBar;


    // Toolbar Views
    private Toolbar mToolbar;
    private AutoCompleteTextView mSearch;
    private ImageView mArrowImage;

    // Current tab view being displayed
    private View mCurrentView;

    // Full Screen Video Views
    private FrameLayout mFullscreenContainer;
    private VideoView mVideoView;
    private View mCustomView;

    // Adapter
    private SearchAdapter mSearchAdapter;

    // Callback
    private CustomViewCallback mCustomViewCallback;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Primatives
    private boolean mFullScreen;
    private boolean mDarkTheme;
    private boolean mIsFullScreen = false;
    private boolean mIsImmersive = false;
    private boolean mShowTabsInDrawer;
    private int mOriginalOrientation;
    private int mBackgroundColor;
    private int mIconColor;
    private int mDisabledIconColor;
    private int mCurrentUiColor = Color.BLACK;
    private String mSearchText;
    private String mUntitledTitle;
    private String mCameraPhotoPath;

    // The singleton BookmarkManager
    @Inject BookmarkManager mBookmarkManager;

    // Event bus
    @Inject Bus mEventBus;

    @Inject LightningDialogBuilder mBookmarksDialogBuilder;

    @Inject TabsManager mTabsManager;

    @Inject HistoryDatabase mHistoryDatabase;

    // Image
    private Bitmap mWebpageBitmap;
    private final ColorDrawable mBackground = new ColorDrawable();
    private Drawable mDeleteIcon, mRefreshIcon, mClearIcon, mIcon;

    private BrowserPresenter mPresenter;
    private TabsView mTabsView;

    // Proxy
    @Inject ProxyUtils mProxyUtils;

    // Constant
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private static final String NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final LayoutParams MATCH_PARENT = new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT);
    private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    protected abstract boolean isIncognito();

    public abstract void closeActivity();

    public abstract void updateHistory(@Nullable final String title, @NonNull final String url);

    abstract Observable<Void> updateCookiePreference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build());
        }

        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mPresenter = new BrowserPresenter(this, isIncognito());

        initialize();

    }

    private synchronized void initialize() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();

        //TODO make sure dark theme flag gets set correctly
        mDarkTheme = mPreferences.getUseTheme() != 0 || isIncognito();
        mIconColor = mDarkTheme ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        mDisabledIconColor = mDarkTheme ? ContextCompat.getColor(this, R.color.icon_dark_theme_disabled) :
                ContextCompat.getColor(this, R.color.icon_light_theme_disabled);
        mShowTabsInDrawer = mPreferences.getShowTabsInDrawer(!isTablet());

        // initialize background ColorDrawable
        mBackground.setColor(((ColorDrawable) mToolbarLayout.getBackground()).getColor());

        // Drawer stutters otherwise
        mDrawerLeft.setLayerType(View.LAYER_TYPE_NONE, null);
        mDrawerRight.setLayerType(View.LAYER_TYPE_NONE, null);

        mDrawerLayout.addDrawerListener(new DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {}

            @Override
            public void onDrawerOpened(View drawerView) {}

            @Override
            public void onDrawerClosed(View drawerView) {}

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    mDrawerLeft.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    mDrawerRight.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                } else if (newState == DrawerLayout.STATE_IDLE) {
                    mDrawerLeft.setLayerType(View.LAYER_TYPE_NONE, null);
                    mDrawerRight.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mShowTabsInDrawer) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        setNavigationDrawerWidth();
        mDrawerLayout.setDrawerListener(new DrawerLocker());

        mWebpageBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_webpage, mDarkTheme);

        final TabsFragment tabsFragment = new TabsFragment();
        mTabsView = tabsFragment;
        final int containerId = mShowTabsInDrawer ? R.id.left_drawer : R.id.tabs_toolbar_container;
        final Bundle tabsFragmentArguments = new Bundle();
        tabsFragmentArguments.putBoolean(TabsFragment.IS_INCOGNITO, isIncognito());
        tabsFragmentArguments.putBoolean(TabsFragment.VERTICAL_MODE, mShowTabsInDrawer);
        tabsFragment.setArguments(tabsFragmentArguments);

        final BookmarksFragment bookmarksFragment = new BookmarksFragment();
        final Bundle bookmarksFragmentArguments = new Bundle();
        bookmarksFragmentArguments.putBoolean(BookmarksFragment.INCOGNITO_MODE, isIncognito());
        bookmarksFragment.setArguments(bookmarksFragmentArguments);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .add(containerId, tabsFragment)
                .add(R.id.right_drawer, bookmarksFragment)
                .commit();
        if (mShowTabsInDrawer) {
            mToolbarLayout.removeView(findViewById(R.id.tabs_toolbar_container));
        }

        if (actionBar == null)
            return;

        // set display options of the ActionBar
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.toolbar_content);

        View customView = actionBar.getCustomView();
        LayoutParams lp = customView.getLayoutParams();
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.MATCH_PARENT;
        customView.setLayoutParams(lp);

        mArrowImage = (ImageView) customView.findViewById(R.id.arrow);
        FrameLayout arrowButton = (FrameLayout) customView.findViewById(R.id.arrow_button);
        if (mShowTabsInDrawer) {
            if (mArrowImage.getWidth() <= 0) {
                mArrowImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            }
            updateTabNumber(0);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerLeft);
            mArrowImage.setImageResource(R.drawable.ic_action_home);
            mArrowImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        }
        arrowButton.setOnClickListener(this);

        // create the search EditText in the ToolBar
        mSearch = (AutoCompleteTextView) customView.findViewById(R.id.search);
        mUntitledTitle = getString(R.string.untitled);
        mBackgroundColor = ContextCompat.getColor(this, R.color.primary_color);
        mDeleteIcon = ThemeUtils.getLightThemedDrawable(this, R.drawable.ic_action_delete);
        mRefreshIcon = ThemeUtils.getLightThemedDrawable(this, R.drawable.ic_action_refresh);
        mClearIcon = ThemeUtils.getLightThemedDrawable(this, R.drawable.ic_action_delete);

        int iconBounds = Utils.dpToPx(30);
        mDeleteIcon.setBounds(0, 0, iconBounds, iconBounds);
        mRefreshIcon.setBounds(0, 0, iconBounds, iconBounds);
        mClearIcon.setBounds(0, 0, iconBounds, iconBounds);
        mIcon = mRefreshIcon;
        SearchListenerClass search = new SearchListenerClass();
        mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
        mSearch.setOnKeyListener(search);
        mSearch.setOnFocusChangeListener(search);
        mSearch.setOnEditorActionListener(search);
        mSearch.setOnTouchListener(search);

        initializeSearchSuggestions(mSearch);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);

        if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        }

        if (isPanicTrigger(getIntent())) {
            panicClean();
        } else {
            mPresenter.setupTabs(getIntent(), isIncognito());
            mProxyUtils.checkForProxy(BrowserActivity.this);
        }
    }

    static boolean isPanicTrigger(@Nullable Intent intent) {
        return intent != null && INTENT_PANIC_TRIGGER.equals(intent.getAction());
    }

    void panicClean() {
        Log.d(Constants.TAG, "Closing browser");
        mTabsManager.newTab(this, "", false);
        mTabsManager.switchToTab(0);
        mTabsManager.clearSavedState();
        HistoryPage.deleteHistoryPage(getApplication());
        closeBrowser();
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        System.exit(1);
    }

    private class SearchListenerClass implements OnKeyListener, OnEditorActionListener, OnFocusChangeListener, OnTouchListener {

        @Override
        public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

            switch (arg1) {
                case KeyEvent.KEYCODE_ENTER:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
                    searchTheWeb(mSearch.getText().toString());
                    final LightningView currentView = mTabsManager.getCurrentTab();
                    if (currentView != null) {
                        currentView.requestFocus();
                    }
                    return true;
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
            // hide the keyboard and search the web when the enter key
            // button is pressed
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (arg2.getAction() == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
                searchTheWeb(mSearch.getText().toString());
                final LightningView currentView = mTabsManager.getCurrentTab();
                if (currentView != null) {
                    currentView.requestFocus();
                }
                return true;
            }
            return false;
        }

        @Override
        public void onFocusChange(View v, final boolean hasFocus) {
            final LightningView currentView = mTabsManager.getCurrentTab();
            if (!hasFocus && currentView != null) {
                setIsLoading(currentView.getProgress() < 100);
                updateUrl(currentView.getUrl(), true);
            } else if (hasFocus && currentView != null) {
                String url = currentView.getUrl();
                if (UrlUtils.isSpecialUrl(url)) {
                    mSearch.setText("");
                } else {
                    mSearch.setText(url);
                }
                // Hack to make sure the text gets selected
                ((AutoCompleteTextView) v).selectAll();
                mIcon = mClearIcon;
                mSearch.setCompoundDrawables(null, null, mClearIcon, null);
            }

            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mSearch.getCompoundDrawables()[2] != null) {
                boolean tappedX = event.getX() > (mSearch.getWidth()
                        - mSearch.getPaddingRight() - mIcon.getIntrinsicWidth());
                if (tappedX) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (mSearch.hasFocus()) {
                            mSearch.setText("");
                        } else {
                            refreshOrStop();
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private class DrawerLocker implements DrawerListener {

        @Override
        public void onDrawerClosed(View v) {
            if (v == mDrawerRight && mShowTabsInDrawer) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLeft);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerRight);
            }
        }

        @Override
        public void onDrawerOpened(View v) {
            if (v == mDrawerRight) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerLeft);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerRight);
            }
        }

        @Override
        public void onDrawerSlide(View v, float arg) {
        }

        @Override
        public void onDrawerStateChanged(int arg) {
        }

    }

    private void setNavigationDrawerWidth() {
        int width = getResources().getDisplayMetrics().widthPixels - Utils.dpToPx(56);
        int maxWidth;
        if (isTablet()) {
            maxWidth = Utils.dpToPx(320);
        } else {
            maxWidth = Utils.dpToPx(300);
        }
        if (width > maxWidth) {
            DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerLeft
                    .getLayoutParams();
            params.width = maxWidth;
            mDrawerLeft.setLayoutParams(params);
            mDrawerLeft.requestLayout();
            DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight
                    .getLayoutParams();
            paramsRight.width = maxWidth;
            mDrawerRight.setLayoutParams(paramsRight);
            mDrawerRight.requestLayout();
        } else {
            DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerLeft
                    .getLayoutParams();
            params.width = width;
            mDrawerLeft.setLayoutParams(params);
            mDrawerLeft.requestLayout();
            DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight
                    .getLayoutParams();
            paramsRight.width = width;
            mDrawerRight.setLayoutParams(paramsRight);
            mDrawerRight.requestLayout();
        }
    }

    private void initializePreferences() {
        final LightningView currentView = mTabsManager.getCurrentTab();
        mFullScreen = mPreferences.getFullScreenEnabled();
        boolean colorMode = mPreferences.getColorModeEnabled();
        colorMode &= !mDarkTheme;
        if (!isIncognito() && !colorMode && !mDarkTheme && mWebpageBitmap != null) {
            changeToolbarBackground(mWebpageBitmap, null);
        } else if (!isIncognito() && currentView != null && !mDarkTheme) {
            changeToolbarBackground(currentView.getFavicon(), null);
        }

        mToolbarLayout.setTranslationY(0);
        // TODO layout transition causing memory leak
//        mBrowserFrame.setLayoutTransition(new LayoutTransition());
        initializeTabHeight();
        mBrowserFrame.setTranslationY(0);
        setFullscreen(mPreferences.getHideStatusBarEnabled(), false);

        switch (mPreferences.getSearchChoice()) {
            case 0:
                mSearchText = mPreferences.getSearchUrl();
                if (!mSearchText.startsWith(Constants.HTTP)
                        && !mSearchText.startsWith(Constants.HTTPS)) {
                    mSearchText = Constants.GOOGLE_SEARCH;
                }
                break;
            case 1:
                mSearchText = Constants.GOOGLE_SEARCH;
                break;
            case 2:
                mSearchText = Constants.ASK_SEARCH;
                break;
            case 3:
                mSearchText = Constants.BING_SEARCH;
                break;
            case 4:
                mSearchText = Constants.YAHOO_SEARCH;
                break;
            case 5:
                mSearchText = Constants.STARTPAGE_SEARCH;
                break;
            case 6:
                mSearchText = Constants.STARTPAGE_MOBILE_SEARCH;
                break;
            case 7:
                mSearchText = Constants.DUCK_SEARCH;
                break;
            case 8:
                mSearchText = Constants.DUCK_LITE_SEARCH;
                break;
            case 9:
                mSearchText = Constants.BAIDU_SEARCH;
                break;
            case 10:
                mSearchText = Constants.YANDEX_SEARCH;
                break;
        }

        updateCookiePreference().subscribeOn(Schedulers.worker()).subscribe();
        mProxyUtils.updateProxySettings(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mSearch.hasFocus()) {
                searchTheWeb(mSearch.getText().toString());
            }
        } else if ((keyCode == KeyEvent.KEYCODE_MENU)
                && (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN)
                && (Build.MANUFACTURER.compareTo("LGE") == 0)) {
            // Workaround for stupid LG devices that crash
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU)
                && (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN)
                && (Build.MANUFACTURER.compareTo("LGE") == 0)) {
            // Workaround for stupid LG devices that crash
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final LightningView currentView = mTabsManager.getCurrentTab();
        final String currentUrl = currentView != null ? currentView.getUrl() : null;
        // Handle action buttons
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
                    mDrawerLayout.closeDrawer(mDrawerRight);
                }
                return true;
            case R.id.action_back:
                if (currentView != null && currentView.canGoBack()) {
                    currentView.goBack();
                }
                return true;
            case R.id.action_forward:
                if (currentView != null && currentView.canGoForward()) {
                    currentView.goForward();
                }
                return true;
            case R.id.action_add_to_homescreen:
                if (currentView != null) {
                    HistoryItem shortcut = new HistoryItem(currentView.getUrl(), currentView.getTitle());
                    shortcut.setBitmap(currentView.getFavicon());
                    Utils.createShortcut(this, shortcut);
                }
                return true;
            case R.id.action_new_tab:
                newTab(null, true);
                return true;
            case R.id.action_incognito:
                startActivity(new Intent(this, IncognitoActivity.class));
                overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale);
                return true;
            case R.id.action_share:
                if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentView.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl);
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.dialog_title_share)));
                }
                return true;
            case R.id.action_bookmarks:
                openBookmarks();
                return true;
            case R.id.action_copy:
                if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", currentUrl);
                    clipboard.setPrimaryClip(clip);
                    Utils.showSnackbar(this, R.string.message_link_copied);
                }
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_history:
                openHistory();
                return true;
            case R.id.action_add_bookmark:
                if (currentUrl != null && !UrlUtils.isSpecialUrl(currentUrl)) {
                    addBookmark(currentView.getTitle(), currentUrl);
                }
                return true;
            case R.id.action_find:
                findInPage();
                return true;
            case R.id.action_reading_mode:
                if (currentUrl != null) {
                    Intent read = new Intent(this, ReadingActivity.class);
                    read.putExtra(Constants.LOAD_READING_URL, currentUrl);
                    startActivity(read);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // By using a manager, adds a bookmark and notifies third parties about that
    private void addBookmark(final String title, final String url) {
        final HistoryItem item = !mBookmarkManager.isBookmark(url)
                ? new HistoryItem(url, title)
                : null;
        if (item != null && mBookmarkManager.addBookmark(item)) {
            mSearchAdapter.refreshBookmarks();
            mEventBus.post(new BrowserEvents.BookmarkAdded(title, url));
        }
    }

    private void deleteBookmark(final String title, final String url) {
        final HistoryItem item = mBookmarkManager.isBookmark(url)
                ? new HistoryItem(url, title)
                : null;
        if (item != null && mBookmarkManager.deleteBookmark(item)) {
            mSearchAdapter.refreshBookmarks();
            mEventBus.post(new BrowserEvents.CurrentPageUrl(url));
        }
    }

    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    private void findInPage() {
        final AlertDialog.Builder finder = new AlertDialog.Builder(this);
        finder.setTitle(getResources().getString(R.string.action_find));
        final EditText getHome = new EditText(this);
        getHome.setHint(getResources().getString(R.string.search_hint));
        finder.setView(getHome);
        finder.setPositiveButton(getResources().getString(R.string.search_hint),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String query = getHome.getText().toString();
                        if (!query.isEmpty())
                            showSearchInterfaceBar(query);
                    }
                });
        finder.show();
    }

    private void showSearchInterfaceBar(String text) {
        final LightningView currentView = mTabsManager.getCurrentTab();
        if (currentView != null) {
            currentView.find(text);
        }
        mSearchBar.setVisibility(View.VISIBLE);

        TextView tw = (TextView) findViewById(R.id.search_query);
        tw.setText('\'' + text + '\'');

        ImageButton up = (ImageButton) findViewById(R.id.button_next);
        up.setOnClickListener(this);

        ImageButton down = (ImageButton) findViewById(R.id.button_back);
        down.setOnClickListener(this);

        ImageButton quit = (ImageButton) findViewById(R.id.button_quit);
        quit.setOnClickListener(this);
    }

    @Override
    public void showCloseDialog(final int position) {
        if (position < 0) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);
        adapter.add(this.getString(R.string.close_all_tabs));
        adapter.add(this.getString(R.string.close_other_tabs));
        adapter.add(this.getString(R.string.close_tab));
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        closeBrowser();
                        break;
                    case 1:
                        mPresenter.closeAllOtherTabs();
                        break;
                    case 2:
                        deleteTab(position);
                        break;
                    default:
                        break;
                }
            }
        });
        builder.show();
    }

    @Override
    public void notifyTabViewRemoved(int position) {
        Log.d(Constants.TAG, "Notify Tab Removed: " + position);
        mTabsView.tabRemoved(position);
    }

    @Override
    public void notifyTabViewAdded() {
        Log.d(Constants.TAG, "Notify Tab Added");
        mTabsView.tabAdded();
    }

    @Override
    public void notifyTabViewChanged(int position) {
        Log.d(Constants.TAG, "Notify Tab Changed: " + position);
        mTabsView.tabChanged(position);
    }

    @Override
    public void tabChanged(LightningView tab) {
        mPresenter.tabChangeOccurred(tab);
    }

    @Override
    public void removeTabView() {

        // Set the background color so the color mode color doesn't show through
        mBrowserFrame.setBackgroundColor(mBackgroundColor);

        mBrowserFrame.removeAllViews();

        removeViewFromParent(mCurrentView);

        mCurrentView = null;

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 200);

    }

    @Override
    public void setTabView(@NonNull final View view) {
        if (mCurrentView == view) {
            return;
        }

        // Set the background color so the color mode color doesn't show through
        mBrowserFrame.setBackgroundColor(mBackgroundColor);

        mBrowserFrame.removeAllViews();

        removeViewFromParent(view);
        removeViewFromParent(mCurrentView);

        mBrowserFrame.addView(view, MATCH_PARENT);

        view.requestFocus();

        mCurrentView = view;

        showActionBar();

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 200);

        // new Handler().postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        // Remove browser frame background to reduce overdraw
        //TODO evaluate performance
        //         mBrowserFrame.setBackgroundColor(Color.TRANSPARENT);
        //     }
        // }, 300);
    }

    @Override
    public void showBlockedLocalFileDialog(DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle(R.string.title_warning)
                .setMessage(R.string.message_blocked_local)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_open, listener)
                .show();
    }

    @Override
    public void showSnackbar(@StringRes int resource) {
        Utils.showSnackbar(this, resource);
    }

    /**
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     *
     * @param position the poition of the tab to display
     */
    // TODO move to presenter
    private synchronized void showTab(final int position) {
        mPresenter.tabChanged(position);
    }

    private static void removeViewFromParent(@Nullable View view) {
        if (view == null) {
            return;
        }
        ViewGroup parent = ((ViewGroup) view.getParent());
        if (parent != null) {
            parent.removeView(view);
        }
    }

    void handleNewIntent(Intent intent) {
        mPresenter.onNewIntent(intent);
    }

    @Override
    public void closeEmptyTab() {
        final WebView currentWebView = mTabsManager.getCurrentWebView();
        if (currentWebView != null && currentWebView.copyBackForwardList().getSize() == 0) {
            closeCurrentTab();
        }
    }

    private void closeCurrentTab() {
        // don't delete the tab because the browser will close and mess stuff up
    }

    @Override
    public void onTrimMemory(int level) {
        if (level > TRIM_MEMORY_MODERATE && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.d(Constants.TAG, "Low Memory, Free Memory");
            mTabsManager.freeMemory();
        }
    }

    // TODO move to presenter
    private synchronized boolean newTab(String url, boolean show) {
        return mPresenter.newTab(url, show);
    }

    @Override
    public void newTabClicked() {
        mPresenter.newTab(null, true);
    }

    // TODO move this to presenter
    private synchronized void deleteTab(int position) {
        mPresenter.deleteTab(position);
    }

    void performExitCleanUp() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (mPreferences.getClearCacheExit() && currentTab != null && !isIncognito()) {
            WebUtils.clearCache(currentTab.getWebView());
            Log.d(Constants.TAG, "Cache Cleared");
        }
        if (mPreferences.getClearHistoryExitEnabled() && !isIncognito()) {
            WebUtils.clearHistory(this, mHistoryDatabase);
            Log.d(Constants.TAG, "History Cleared");
        }
        if (mPreferences.getClearCookiesExitEnabled() && !isIncognito()) {
            WebUtils.clearCookies(this);
            Log.d(Constants.TAG, "Cookies Cleared");
        }
        if (mPreferences.getClearWebStorageExitEnabled() && !isIncognito()) {
            WebUtils.clearWebStorage();
            Log.d(Constants.TAG, "WebStorage Cleared");
        } else if (isIncognito()) {
            WebUtils.clearWebStorage();     // We want to make sure incognito mode is secure
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showCloseDialog(mTabsManager.positionOf(currentTab));
        }
        return true;
    }


    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged");

        if (mFullScreen) {
            showActionBar();
            mBrowserFrame.setTranslationY(0);
            mToolbarLayout.setTranslationY(0);
        }

        initializeTabHeight();

        doOnLayout(mUiLayout, new Runnable() {
            @Override
            public void run() {
                int toolbarSize;
                if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    toolbarSize = Utils.dpToPx(56);
                } else {
                    toolbarSize = Utils.dpToPx(48);
                }
                mToolbar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarSize));
                mToolbar.setMinimumHeight(toolbarSize);
                mToolbar.requestLayout();
            }
        });
    }

    public void closeBrowser() {
        mBrowserFrame.setBackgroundColor(mBackgroundColor);
        performExitCleanUp();
        mBrowserFrame.removeAllViews();
        int size = mTabsManager.size();
        mTabsManager.shutdown();
        mCurrentView = null;
        for (int n = 0; n < size; n++) {
            mTabsView.tabRemoved(0);
        }
        finish();
    }

    @Override
    public synchronized void onBackPressed() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawer(mDrawerLeft);
        } else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
            mEventBus.post(new BrowserEvents.UserPressedBack());
        } else {
            if (currentTab != null) {
                Log.d(Constants.TAG, "onBackPressed");
                if (mSearch.hasFocus()) {
                    currentTab.requestFocus();
                } else if (currentTab.canGoBack()) {
                    if (!currentTab.isShown()) {
                        onHideCustomView();
                    } else {
                        currentTab.goBack();
                    }
                } else {
                    if (mCustomView != null || mCustomViewCallback != null) {
                        onHideCustomView();
                    } else {
                        deleteTab(mTabsManager.positionOf(currentTab));
                    }
                }
            } else {
                Log.e(Constants.TAG, "This shouldn't happen ever");
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        final LightningView currentTab = mTabsManager.getCurrentTab();
        Log.d(Constants.TAG, "onPause");
        if (currentTab != null) {
            currentTab.pauseTimers();
            currentTab.onPause();
        }
        try {
            BrowserApp.get(this).unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (isIncognito() && isFinishing()) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_down_out);
        }

        mEventBus.unregister(mBusEventListener);
    }

    void saveOpenTabs() {
        if (mPreferences.getRestoreLostTabsEnabled()) {
            mTabsManager.saveState();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mProxyUtils.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(Constants.TAG, "onDestroy");

        mPresenter.shutdown();

        if (mHistoryDatabase != null) {
            mHistoryDatabase.close();
            mHistoryDatabase = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mProxyUtils.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final LightningView currentTab = mTabsManager.getCurrentTab();
        Log.d(Constants.TAG, "onResume");
        if (mSearchAdapter != null) {
            mSearchAdapter.refreshPreferences();
            mSearchAdapter.refreshBookmarks();
        }
        if (currentTab != null) {
            currentTab.resumeTimers();
            currentTab.onResume();
        }
        initializePreferences();
        mTabsManager.resume(this);

        supportInvalidateOptionsMenu();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_BROADCAST_ACTION);
        BrowserApp.get(this).registerReceiver(mNetworkReceiver, filter);

        mEventBus.register(mBusEventListener);
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private void searchTheWeb(@NonNull String query) {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (query.isEmpty()) {
            return;
        }
        String searchUrl = mSearchText + UrlUtils.QUERY_PLACE_HOLDER;
        query = query.trim();
        if (currentTab != null) {
            currentTab.stopLoading();
            mPresenter.loadUrlInCurrentView(UrlUtils.smartUrlFilter(query, true, searchUrl));
        }
    }

    /**
     * Animates the color of the toolbar from one color to another. Optionally animates
     * the color of the tab background, for use when the tabs are displayed on the top
     * of the screen.
     *
     * @param favicon       the Bitmap to extract the color from
     * @param tabBackground the optional LinearLayout to color
     */
    @Override
    public void changeToolbarBackground(@NonNull Bitmap favicon, @Nullable final Drawable tabBackground) {
        final int defaultColor = ContextCompat.getColor(this, R.color.primary_color);
        if (mCurrentUiColor == Color.BLACK) {
            mCurrentUiColor = defaultColor;
        }
        Palette.from(favicon).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {

                // OR with opaque black to remove transparency glitches
                int color = 0xff000000 | palette.getVibrantColor(defaultColor);

                int finalColor; // Lighten up the dark color if it is
                // too dark
                if (!mShowTabsInDrawer || Utils.isColorTooDark(color)) {
                    finalColor = Utils.mixTwoColors(defaultColor, color, 0.25f);
                } else {
                    finalColor = color;
                }

                ValueAnimator anim = ValueAnimator.ofInt(mCurrentUiColor, finalColor);
                anim.setEvaluator(new ArgbEvaluator());
                final Window window = getWindow();
                if (!mShowTabsInDrawer) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                }
                anim.addUpdateListener(new AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        final int color = (Integer) animation.getAnimatedValue();
                        if (mShowTabsInDrawer) {
                            mBackground.setColor(color);
                            window.setBackgroundDrawable(mBackground);
                        } else if (tabBackground != null) {
                            tabBackground.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        }
                        mCurrentUiColor = color;
                        mToolbarLayout.setBackgroundColor(color);
                    }

                });
                anim.setDuration(300);
                anim.start();
            }
        });
    }

    @Override
    public boolean getUseDarkTheme() {
        return mDarkTheme;
    }

    @ColorInt
    @Override
    public int getUiColor() {
        return mCurrentUiColor;
    }

    @Override
    public void updateUrl(@Nullable String url, boolean shortUrl) {
        if (url == null || mSearch == null || mSearch.hasFocus()) {
            return;
        }
        final LightningView currentTab = mTabsManager.getCurrentTab();
        mEventBus.post(new BrowserEvents.CurrentPageUrl(url));
        if (shortUrl && !UrlUtils.isSpecialUrl(url)) {
            switch (mPreferences.getUrlBoxContentChoice()) {
                case 0: // Default, show only the domain
                    url = url.replaceFirst(Constants.HTTP, "");
                    url = Utils.getDomainName(url);
                    mSearch.setText(url);
                    break;
                case 1: // URL, show the entire URL
                    mSearch.setText(url);
                    break;
                case 2: // Title, show the page's title
                    if (currentTab != null && !currentTab.getTitle().isEmpty()) {
                        mSearch.setText(currentTab.getTitle());
                    } else {
                        mSearch.setText(mUntitledTitle);
                    }
                    break;
            }
        } else {
            if (UrlUtils.isSpecialUrl(url)) {
                url = "";
            }
            mSearch.setText(url);
        }
    }

    @Override
    public void updateTabNumber(int number) {
        if (mArrowImage != null && mShowTabsInDrawer) {
            mArrowImage.setImageBitmap(DrawableUtils.getRoundedNumberImage(number, Utils.dpToPx(24),
                    Utils.dpToPx(24), ThemeUtils.getIconThemeColor(this, mDarkTheme), Utils.dpToPx(2.5f)));
        }
    }

    @Override
    public void updateProgress(int n) {
        setIsLoading(n < 100);
        mProgressBar.setProgress(n);
    }

    void addItemToHistory(@Nullable final String title, @NonNull final String url) {
        if (UrlUtils.isSpecialUrl(url)) {
            return;
        }
        BrowserApp.getIOThread().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mHistoryDatabase.visitHistoryItem(url, title);
                } catch (IllegalStateException e) {
                    Log.e(Constants.TAG, "IllegalStateException in updateHistory", e);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "NullPointerException in updateHistory", e);
                } catch (SQLiteException e) {
                    Log.e(Constants.TAG, "SQLiteException in updateHistory", e);
                }
            }
        });
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

        mSearchAdapter = new SearchAdapter(this, mDarkTheme, isIncognito());

        getUrl.setThreshold(1);
        getUrl.setDropDownWidth(-1);
        getUrl.setDropDownAnchor(R.id.toolbar_layout);
        getUrl.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                String url = null;
                CharSequence urlString = ((TextView) view.findViewById(R.id.url)).getText();
                if (urlString != null) {
                    url = urlString.toString();
                }
                if (url == null || url.startsWith(BrowserActivity.this.getString(R.string.suggestion))) {
                    CharSequence searchString = ((TextView) view.findViewById(R.id.title)).getText();
                    if (searchString != null) {
                        url = searchString.toString();
                    }
                }
                if (url == null) {
                    return;
                }
                getUrl.setText(url);
                searchTheWeb(url);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
                final LightningView currentTab = mTabsManager.getCurrentTab();
                if (currentTab != null) {
                    currentTab.requestFocus();
                }
            }

        });

        getUrl.setSelectAllOnFocus(true);
        getUrl.setAdapter(mSearchAdapter);
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private void openHistory() {
        new HistoryPage(mTabsManager.getCurrentTab(), getApplication(), mHistoryDatabase).load();
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private void openBookmarks() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawers();
        }
        mDrawerLayout.openDrawer(mDrawerRight);
    }

    /**
     * This method closes any open drawer and executes
     * the runnable after the drawers are completely closed.
     *
     * @param runnable an optional runnable to run after
     *                 the drawers are closed.
     */
    void closeDrawers(@Nullable final Runnable runnable) {
        if (!mDrawerLayout.isDrawerOpen(mDrawerLeft) && !mDrawerLayout.isDrawerOpen(mDrawerRight)) {
            if (runnable != null) {
                runnable.run();
                return;
            }
        }
        mDrawerLayout.closeDrawers();

        mDrawerLayout.addDrawerListener(new DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {}

            @Override
            public void onDrawerOpened(View drawerView) {}

            @Override
            public void onDrawerClosed(View drawerView) {
                if (runnable != null) {
                    runnable.run();
                }
                mDrawerLayout.removeDrawerListener(this);
            }

            @Override
            public void onDrawerStateChanged(int newState) {}
        });
    }

    @Override
    public void setForwardButtonEnabled(boolean enabled) {
        if (mForwardMenuItem != null && mForwardMenuItem.getIcon() != null) {
            int colorFilter;
            if (enabled) {
                colorFilter = mIconColor;
            } else {
                colorFilter = mDisabledIconColor;
            }
            mForwardMenuItem.getIcon().setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN);
            mForwardMenuItem.setIcon(mForwardMenuItem.getIcon());
        }
    }

    @Override
    public void setBackButtonEnabled(boolean enabled) {
        if (mBackMenuItem != null && mBackMenuItem.getIcon() != null) {
            int colorFilter;
            if (enabled) {
                colorFilter = mIconColor;
            } else {
                colorFilter = mDisabledIconColor;
            }
            mBackMenuItem.getIcon().setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN);
            mBackMenuItem.setIcon(mBackMenuItem.getIcon());
        }
    }

    private MenuItem mBackMenuItem;
    private MenuItem mForwardMenuItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mBackMenuItem = menu.findItem(R.id.action_back);
        mForwardMenuItem = menu.findItem(R.id.action_forward);
        if (mBackMenuItem != null && mBackMenuItem.getIcon() != null)
            mBackMenuItem.getIcon().setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        if (mForwardMenuItem != null && mForwardMenuItem.getIcon() != null)
            mForwardMenuItem.getIcon().setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * opens a file chooser
     * param ValueCallback is the message from the WebView indicating a file chooser
     * should be opened
     */
    @Override
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        mUploadMessage = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(Intent.createChooser(i, getString(R.string.title_file_chooser)), 1);
    }

    /**
     * used to allow uploading into the browser
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (API < Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == 1) {
                if (null == mUploadMessage) {
                    return;
                }
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;

            }
        }

        if (requestCode != 1 || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (intent == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = intent.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = Utils.createImageFile();
                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(Constants.TAG, "Unable to create Image File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        startActivityForResult(chooserIntent, 1);
    }

    @Override
    public synchronized void onShowCustomView(View view, CustomViewCallback callback) {
        int requestedOrientation = mOriginalOrientation = getRequestedOrientation();
        onShowCustomView(view, callback, requestedOrientation);
    }

    @Override
    public synchronized void onShowCustomView(final View view, CustomViewCallback callback, int requestedOrientation) {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (view == null || mCustomView != null) {
            if (callback != null) {
                try {
                    callback.onCustomViewHidden();
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Error hiding custom view", e);
                }
            }
            return;
        }
        try {
            view.setKeepScreenOn(true);
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "WebView is not allowed to keep the screen on");
        }
        mOriginalOrientation = getRequestedOrientation();
        mCustomViewCallback = callback;
        mCustomView = view;

        setRequestedOrientation(requestedOrientation);
        final FrameLayout decorView = (FrameLayout) getWindow().getDecorView();

        mFullscreenContainer = new FrameLayout(this);
        mFullscreenContainer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                mVideoView.setOnErrorListener(new VideoCompletionListener());
                mVideoView.setOnCompletionListener(new VideoCompletionListener());
            }
        } else if (view instanceof VideoView) {
            mVideoView = (VideoView) view;
            mVideoView.setOnErrorListener(new VideoCompletionListener());
            mVideoView.setOnCompletionListener(new VideoCompletionListener());
        }
        decorView.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
        decorView.requestLayout();
        setFullscreen(true, true);
        if (currentTab != null) {
            currentTab.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onHideCustomView() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (mCustomView == null || mCustomViewCallback == null || currentTab == null) {
            if (mCustomViewCallback != null) {
                try {
                    mCustomViewCallback.onCustomViewHidden();
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Error hiding custom view", e);
                }
                mCustomViewCallback = null;
            }
            return;
        }
        Log.d(Constants.TAG, "onHideCustomView");
        currentTab.setVisibility(View.VISIBLE);
        try {
            mCustomView.setKeepScreenOn(false);
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "WebView is not allowed to keep the screen on");
        }
        setFullscreen(mPreferences.getHideStatusBarEnabled(), false);
        if (mFullscreenContainer != null) {
            ViewGroup parent = (ViewGroup) mFullscreenContainer.getParent();
            if (parent != null) {
                parent.removeView(mFullscreenContainer);
            }
            mFullscreenContainer.removeAllViews();
        }

        mFullscreenContainer = null;
        mCustomView = null;
        if (mVideoView != null) {
            Log.d(Constants.TAG, "VideoView is being stopped");
            mVideoView.stopPlayback();
            mVideoView.setOnErrorListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView = null;
        }
        if (mCustomViewCallback != null) {
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Exception e) {
                Log.e(Constants.TAG, "Error hiding custom view", e);
            }
        }
        mCustomViewCallback = null;
        setRequestedOrientation(mOriginalOrientation);
    }

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "onWindowFocusChanged");
        if (hasFocus) {
            setFullscreen(mIsFullScreen, mIsImmersive);
        }
    }

    /**
     * This method sets whether or not the activity will display
     * in full-screen mode (i.e. the ActionBar will be hidden) and
     * whether or not immersive mode should be set. This is used to
     * set both parameters correctly as during a full-screen video,
     * both need to be set, but other-wise we leave it up to user
     * preference.
     *
     * @param enabled   true to enable full-screen, false otherwise
     * @param immersive true to enable immersive mode, false otherwise
     */
    private void setFullscreen(boolean enabled, boolean immersive) {
        mIsFullScreen = enabled;
        mIsImmersive = immersive;
        Window window = getWindow();
        View decor = window.getDecorView();
        if (enabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (immersive) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    /**
     * This method handles the JavaScript callback to create a new tab.
     * Basically this handles the event that JavaScript needs to create
     * a popup.
     *
     * @param resultMsg the transport message used to send the URL to
     *                  the newly created WebView.
     */
    @Override
    public synchronized void onCreateWindow(Message resultMsg) {
        if (resultMsg == null) {
            return;
        }
        if (newTab("", true)) {
            LightningView newTab = mTabsManager.getTabAtPosition(mTabsManager.size() - 1);
            if (newTab != null) {
                final WebView webView = newTab.getWebView();
                if (webView != null) {
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(webView);
                    resultMsg.sendToTarget();
                }
            }
        }
    }

    /**
     * Closes the specified {@link LightningView}. This implements
     * the JavaScript callback that asks the tab to close itself and
     * is especially helpful when a page creates a redirect and does
     * not need the tab to stay open any longer.
     *
     * @param view the LightningView to close, delete it.
     */
    @Override
    public void onCloseWindow(LightningView view) {
        deleteTab(mTabsManager.positionOf(view));
    }

    /**
     * Hide the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    @Override
    public void hideActionBar() {
        if (mFullScreen) {
            if (mToolbarLayout == null || mBrowserFrame == null)
                return;

            final int height = mToolbarLayout.getHeight();
            if (mToolbarLayout.getTranslationY() > -0.01f) {
                Animation show = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float trans = (1.0f - interpolatedTime) * height;
                        mToolbarLayout.setTranslationY(trans - height);
                        mBrowserFrame.setTranslationY(trans - height);
                    }
                };
                show.setDuration(250);
                show.setInterpolator(new DecelerateInterpolator());
                mBrowserFrame.startAnimation(show);
            }
        }
    }

    /**
     * Display the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    @Override
    public void showActionBar() {
        if (mFullScreen) {
            if (mToolbarLayout == null)
                return;

            int height = mToolbarLayout.getHeight();
            if (height == 0) {
                mToolbarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                height = mToolbarLayout.getMeasuredHeight();
            }

            final LightningView currentTab = mTabsManager.getCurrentTab();
            if (currentTab == null)
                return;

            final int totalHeight = height;
            if (mToolbarLayout.getTranslationY() < -(height - 0.01f)) {
                Animation show = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float trans = interpolatedTime * totalHeight;
                        mToolbarLayout.setTranslationY(trans - totalHeight);
                        mBrowserFrame.setTranslationY(trans - totalHeight);
                    }
                };
                show.setDuration(250);
                show.setInterpolator(new DecelerateInterpolator());
                mBrowserFrame.startAnimation(show);
            }
        }
    }

    /**
     * This method initializes the height of the
     * view that holds the current WebView. It waits
     * for the root layout to be laid out before setting
     * the height as it needs the root layout to be measured
     * first.
     */
    private void initializeTabHeight() {
        doOnLayout(mUiLayout, new Runnable() {
            @Override
            public void run() {
                setTabHeight();
            }
        });
    }

    /**
     * Performs an action when the provided view is laid out.
     *
     * @param view     the view to listen to for layouts.
     * @param runnable the runnable to run when the view is
     *                 laid out.
     */
    private static void doOnLayout(@NonNull final View view, @NonNull final Runnable runnable) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                runnable.run();
            }
        });
    }

    /**
     * This method sets the height of the browser
     * frame view that holds the current WebView.
     * It requires the root layout to be properly
     * laid out in order to set the correct height.
     */
    private void setTabHeight() {
        if (mRoot.getHeight() == 0) {
            mRoot.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        }

        if (mFullScreen) {
            int height = mRoot.getHeight() - mUiLayout.getTop();
            mBrowserFrame.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
        } else {
            mBrowserFrame.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        mBrowserFrame.requestLayout();
    }

    /**
     * This method lets the search bar know that the page is currently loading
     * and that it should display the stop icon to indicate to the user that
     * pressing it stops the page from loading
     */
    private void setIsLoading(boolean isLoading) {
        if (!mSearch.hasFocus()) {
            mIcon = isLoading ? mDeleteIcon : mRefreshIcon;
            mSearch.setCompoundDrawables(null, null, mIcon, null);
        }
    }

    /**
     * handle presses on the refresh icon in the search bar, if the page is
     * loading, stop the page, if it is done loading refresh the page.
     * See setIsFinishedLoading and setIsLoading for displaying the correct icon
     */
    private void refreshOrStop() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab != null) {
            if (currentTab.getProgress() < 100) {
                currentTab.stopLoading();
            } else {
                currentTab.reload();
            }
        }
    }

    /**
     * Handle the click event for the views that are using
     * this class as a click listener. This method should
     * distinguish between the various views using their IDs.
     *
     * @param v the view that the user has clicked
     */
    @Override
    public void onClick(View v) {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.arrow_button:
                if (mSearch != null && mSearch.hasFocus()) {
                    currentTab.requestFocus();
                } else if (mShowTabsInDrawer) {
                    mDrawerLayout.openDrawer(mDrawerLeft);
                } else {
                    currentTab.loadHomepage();
                }
                break;
            case R.id.button_next:
                currentTab.findNext();
                break;
            case R.id.button_back:
                currentTab.findPrevious();
                break;
            case R.id.button_quit:
                currentTab.clearFindMatches();
                mSearchBar.setVisibility(View.GONE);
                break;
            case R.id.action_reading:
                Intent read = new Intent(this, ReadingActivity.class);
                read.putExtra(Constants.LOAD_READING_URL, currentTab.getUrl());
                startActivity(read);
                break;
            case R.id.action_toggle_desktop:
                currentTab.toggleDesktopUA(this);
                currentTab.reload();
                closeDrawers(null);
                break;
        }
    }

    /**
     * Handle long presses on views that use this class
     * as their OnLongClickListener. This method should
     * distinguish between the IDs of the views that are
     * getting clicked.
     *
     * @param view the view that has been long pressed
     * @return returns true since the method handles the long press
     * event
     */
    @Override
    public boolean onLongClick(View view) {
        return true;
    }

    // TODO Check if all the calls are relative to TabsFragement

    /**
     * A utility method that creates a FrameLayout button with the given ID and
     * sets the image of the button to the given image ID. The OnClick and OnLongClick
     * listeners are set to this class, so BrowserActivity should handle those events
     * there. Additionally, it tints the images according to the current theme.
     * This method only is a convenience so that this code does not have to be repeated
     * for the several "Buttons" that use this.
     *
     * @param buttonId the view id of the button
     * @param imageId  the image to set as the button image
     */
    private void setupFrameLayoutButton(@IdRes int buttonId, @IdRes int imageId) {
        final View frameButton = findViewById(buttonId);
        final ImageView buttonImage = (ImageView) findViewById(imageId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    /**
     * This NetworkReceiver notifies each of the WebViews in the browser whether
     * the network is currently connected or not. This is important because some
     * JavaScript properties rely on the WebView knowing the current network state.
     * It is used to help the browser be compliant with the HTML5 spec, sec. 5.7.7
     */
    private final NetworkReceiver mNetworkReceiver = new NetworkReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            boolean isConnected = isConnected(context);
            Log.d(Constants.TAG, "Network Connected: " + String.valueOf(isConnected));
            mTabsManager.notifyConnectionStatus(isConnected);
        }
    };

    /**
     * Handle the callback that permissions requested have been granted or not.
     * This method should act upon the results of the permissions request.
     *
     * @param requestCode  the request code sent when initially making the request
     * @param permissions  the array of the permissions that was requested
     * @param grantResults the results of the permissions requests that provides
     *                     information on whether the request was granted or not
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final Object mBusEventListener = new Object() {

        /**
         * Load the given url in the current tab, used by the the
         * {@link acr.browser.lightning.fragment.BookmarksFragment} and by the
         * {@link LightningDialogBuilder}
         *
         * @param event   Bus event indicating that the user has clicked a bookmark
         */
        @Subscribe
        public void loadUrlInCurrentTab(final BrowserEvents.OpenUrlInCurrentTab event) {
            mPresenter.loadUrlInCurrentView(event.url);
            // keep any jank from happening when the drawer is closed after the
            // URL starts to load
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mDrawerRight);
                }
            }, 150);
        }

        @Subscribe
        public void loadHistory(final BrowserEvents.OpenHistoryInCurrentTab event) {
            new HistoryPage(mTabsManager.getCurrentTab(), getApplication(), mHistoryDatabase).load();
        }

        /**
         * Load the given url in a new tab, used by the the
         * {@link acr.browser.lightning.fragment.BookmarksFragment} and by the
         * {@link LightningDialogBuilder}
         *
         * @param event   Bus event indicating that the user wishes
         *                to open a bookmark in a new tab
         */
        @Subscribe
        public void loadUrlInNewTab(final BrowserEvents.OpenUrlInNewTab event) {
            BrowserActivity.this.newTab(event.url, true);
            mDrawerLayout.closeDrawers();
        }

        /**
         * When receive a {@link BookmarkEvents.ToggleBookmarkForCurrentPage}
         * message this receiver answer firing the
         * {@link BrowserEvents.BookmarkAdded} message
         *
         * @param event an event that the user wishes to bookmark the current page
         */
        @Subscribe
        public void bookmarkCurrentPage(final BookmarkEvents.ToggleBookmarkForCurrentPage event) {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            final String url = currentTab != null ? currentTab.getUrl() : null;
            final String title = currentTab != null ? currentTab.getTitle() : null;
            if (url == null) {
                return;
            }

            if (!mBookmarkManager.isBookmark(url)) {
                addBookmark(title, url);
            } else {
                deleteBookmark(title, url);
            }
        }

        /**
         * This method is called when the user edits a bookmark.
         *
         * @param event the event that the bookmark has changed.
         */
        @Subscribe
        public void bookmarkChanged(final BookmarkEvents.BookmarkChanged event) {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            if (currentTab != null && currentTab.getUrl().startsWith(Constants.FILE)
                    && currentTab.getUrl().endsWith(BookmarkPage.FILENAME)) {
                currentTab.loadBookmarkpage();
            }
            if (currentTab != null) {
                mEventBus.post(new BrowserEvents.CurrentPageUrl(currentTab.getUrl()));
            }
        }

        /**
         * Notify the browser that a bookmark was deleted.
         *
         * @param event the event that the bookmark has been deleted
         */
        @Subscribe
        public void bookmarkDeleted(final BookmarkEvents.Deleted event) {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            if (currentTab != null && currentTab.getUrl().startsWith(Constants.FILE)
                    && currentTab.getUrl().endsWith(BookmarkPage.FILENAME)) {
                currentTab.loadBookmarkpage();
            }
            if (currentTab != null) {
                mEventBus.post(new BrowserEvents.CurrentPageUrl(currentTab.getUrl()));
            }
        }

        /**
         * The {@link acr.browser.lightning.fragment.BookmarksFragment} send this message on reply
         * to {@link acr.browser.lightning.bus.BrowserEvents.UserPressedBack} message if the
         * fragement is showing the boomarks root folder.
         *
         * @param event an event notifying the browser that the bookmark drawer
         *              should be closed.
         */
        @Subscribe
        public void closeBookmarks(final BookmarkEvents.CloseBookmarks event) {
            mDrawerLayout.closeDrawer(mDrawerRight);
        }

        /**
         * The user wants to close a tab
         *
         * @param event contains the position inside the tabs adapter
         */
        @Subscribe
        public void closeTab(final TabEvents.CloseTab event) {
            deleteTab(event.position);
        }

        /**
         * The user clicked on a tab, let's show it
         *
         * @param event contains the tab position in the tabs adapter
         */
        @Subscribe
        public void showTab(final TabEvents.ShowTab event) {
            BrowserActivity.this.showTab(event.position);
        }

        /**
         * The user long pressed on a tab, ask him if he want to close the tab
         *
         * @param event contains the tab position in the tabs adapter
         */
        @Subscribe
        public void showCloseDialog(final TabEvents.ShowCloseDialog event) {
            BrowserActivity.this.showCloseDialog(event.position);
        }

        /**
         * The user wants to create a new tab
         *
         * @param event a marker
         */
        @Subscribe
        public void newTab(final TabEvents.NewTab event) {
            BrowserActivity.this.newTab(null, true);
        }

        /**
         * The user wants to go back on current tab
         *
         * @param event a marker
         */
        @Subscribe
        public void goBack(final NavigationEvents.GoBack event) {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            if (currentTab != null) {
                if (currentTab.canGoBack()) {
                    currentTab.goBack();
                } else {
                    deleteTab(mTabsManager.positionOf(currentTab));
                }
            }
        }

        /**
         * The user wants to go forward on current tab
         *
         * @param event a marker
         */
        @Subscribe
        public void goForward(final NavigationEvents.GoForward event) {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            if (currentTab != null) {
                if (currentTab.canGoForward()) {
                    currentTab.goForward();
                }
            }
        }

        @Subscribe
        public void goHome(final NavigationEvents.GoHome event) {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            if (currentTab != null) {
                currentTab.loadHomepage();
                closeDrawers(null);
            }
        }

        /**
         * The user long pressed the new tab button
         *
         * @param event a marker
         */
        @Subscribe
        public void newTabLongPress(final TabEvents.NewTabLongPress event) {
            String url = mPreferences.getSavedUrl();
            if (url != null) {
                BrowserActivity.this.newTab(url, true);

                Utils.showSnackbar(BrowserActivity.this, R.string.deleted_tab);
            }
            mPreferences.setSavedUrl(null);
        }

        @Subscribe
        public void displayInSnackbar(final BrowserEvents.ShowSnackBarMessage event) {
            if (event.message != null) {
                Utils.showSnackbar(BrowserActivity.this, event.message);
            } else {
                Utils.showSnackbar(BrowserActivity.this, event.stringRes);
            }
        }
    };
}
