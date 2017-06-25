/*
 * Copyright 2015 Anthony Restaino
 */

package acr.browser.lightning.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.VideoView;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableOnSubscribe;
import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.progress.AnimatedProgressBar;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.browser.BookmarksView;
import acr.browser.lightning.browser.BrowserPresenter;
import acr.browser.lightning.browser.BrowserView;
import acr.browser.lightning.browser.SearchBoxModel;
import acr.browser.lightning.browser.TabsView;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.DownloadsPage;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkModel;
import acr.browser.lightning.database.history.HistoryModel;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.fragment.BookmarksFragment;
import acr.browser.lightning.fragment.TabsFragment;
import acr.browser.lightning.interpolator.BezierDecelerateInterpolator;
import acr.browser.lightning.receiver.NetworkReceiver;
import acr.browser.lightning.search.SearchEngineProvider;
import acr.browser.lightning.search.SuggestionsAdapter;
import acr.browser.lightning.search.engine.BaseSearchEngine;
import acr.browser.lightning.utils.DrawableUtils;
import acr.browser.lightning.utils.IntentUtils;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.utils.WebUtils;
import acr.browser.lightning.view.Handlers;
import acr.browser.lightning.view.LightningView;
import acr.browser.lightning.view.SearchView;
import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class BrowserActivity extends ThemableBrowserActivity implements BrowserView, UIController, OnClickListener {

    private static final String TAG = "BrowserActivity";

    private static final String INTENT_PANIC_TRIGGER = "info.guardianproject.panic.action.TRIGGER";

    private static final String TAG_BOOKMARK_FRAGMENT = "TAG_BOOKMARK_FRAGMENT";
    private static final String TAG_TABS_FRAGMENT = "TAG_TABS_FRAGMENT";

    // Static Layout
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.content_frame) FrameLayout mBrowserFrame;
    @BindView(R.id.left_drawer) ViewGroup mDrawerLeft;
    @BindView(R.id.right_drawer) ViewGroup mDrawerRight;
    @BindView(R.id.ui_layout) ViewGroup mUiLayout;
    @BindView(R.id.toolbar_layout) ViewGroup mToolbarLayout;
    @BindView(R.id.progress_view) AnimatedProgressBar mProgressBar;
    @BindView(R.id.search_bar) RelativeLayout mSearchBar;

    // Toolbar Views
    @BindView(R.id.toolbar) Toolbar mToolbar;
    private View mSearchBackground;
    private SearchView mSearch;
    private ImageView mArrowImage;

    // Current tab view being displayed
    @Nullable private View mCurrentView;

    // Full Screen Video Views
    private FrameLayout mFullscreenContainer;
    private VideoView mVideoView;
    private View mCustomView;

    // Adapter
    private SuggestionsAdapter mSuggestionsAdapter;

    // Callback
    private CustomViewCallback mCustomViewCallback;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Primitives
    private boolean mFullScreen;
    private boolean mDarkTheme;
    private boolean mIsFullScreen = false;
    private boolean mIsImmersive = false;
    private boolean mShowTabsInDrawer;
    private boolean mSwapBookmarksAndTabs;
    private int mOriginalOrientation;
    private int mBackgroundColor;
    private int mIconColor;
    private int mDisabledIconColor;
    private int mCurrentUiColor = Color.BLACK;
    private long mKeyDownStartTime;
    private String mSearchText;
    private String mUntitledTitle;
    private String mCameraPhotoPath;

    // The singleton BookmarkManager
    @Inject BookmarkModel mBookmarkManager;

    @Inject HistoryModel mHistoryModel;

    @Inject LightningDialogBuilder mBookmarksDialogBuilder;

    @Inject SearchBoxModel mSearchBoxModel;

    @Inject SearchEngineProvider mSearchEngineProvider;

    private TabsManager mTabsManager;

    // Image
    private Bitmap mWebpageBitmap;
    private final ColorDrawable mBackground = new ColorDrawable();
    private Drawable mDeleteIcon, mRefreshIcon, mClearIcon, mIcon;

    private BrowserPresenter mPresenter;
    private TabsView mTabsView;
    private BookmarksView mBookmarksView;

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

    @NonNull
    abstract Completable updateCookiePreference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mTabsManager = new TabsManager();
        mPresenter = new BrowserPresenter(this, isIncognito());

        initialize(savedInstanceState);
    }

    private synchronized void initialize(Bundle savedInstanceState) {
        initializeToolbarHeight(getResources().getConfiguration());
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();

        //TODO make sure dark theme flag gets set correctly
        mDarkTheme = mPreferences.getUseTheme() != 0 || isIncognito();
        mIconColor = mDarkTheme ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        mDisabledIconColor = mDarkTheme ? ContextCompat.getColor(this, R.color.icon_dark_theme_disabled) :
            ContextCompat.getColor(this, R.color.icon_light_theme_disabled);
        mShowTabsInDrawer = mPreferences.getShowTabsInDrawer(!isTablet());
        mSwapBookmarksAndTabs = mPreferences.getBookmarksAndTabsSwapped();

        // initialize background ColorDrawable
        int primaryColor = ThemeUtils.getPrimaryColor(this);
        mBackground.setColor(primaryColor);

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
        mDrawerLayout.addDrawerListener(new DrawerLocker());

        mWebpageBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_webpage, mDarkTheme);

        final FragmentManager fragmentManager = getSupportFragmentManager();

        TabsFragment tabsFragment = (TabsFragment) fragmentManager.findFragmentByTag(TAG_TABS_FRAGMENT);
        BookmarksFragment bookmarksFragment = (BookmarksFragment) fragmentManager.findFragmentByTag(TAG_BOOKMARK_FRAGMENT);

        if (tabsFragment != null) {
            fragmentManager.beginTransaction().remove(tabsFragment).commit();
        }
        tabsFragment = TabsFragment.createTabsFragment(isIncognito(), mShowTabsInDrawer);

        mTabsView = tabsFragment;

        if (bookmarksFragment != null) {
            fragmentManager.beginTransaction().remove(bookmarksFragment).commit();
        }
        bookmarksFragment = BookmarksFragment.createFragment(isIncognito());

        mBookmarksView = bookmarksFragment;

        fragmentManager.executePendingTransactions();

        fragmentManager
            .beginTransaction()
            .replace(getTabsFragmentViewId(), tabsFragment, TAG_TABS_FRAGMENT)
            .replace(getBookmarksFragmentViewId(), bookmarksFragment, TAG_BOOKMARK_FRAGMENT)
            .commit();
        if (mShowTabsInDrawer) {
            mToolbarLayout.removeView(findViewById(R.id.tabs_toolbar_container));
        }

        Preconditions.checkNonNull(actionBar);

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

        mArrowImage = customView.findViewById(R.id.arrow);
        FrameLayout arrowButton = customView.findViewById(R.id.arrow_button);
        if (mShowTabsInDrawer) {
            if (mArrowImage.getWidth() <= 0) {
                mArrowImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            }
            updateTabNumber(0);

            // Post drawer locking in case the activity is being recreated
            Handlers.MAIN.post(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getTabDrawer());
                }
            });
        } else {

            // Post drawer locking in case the activity is being recreated
            Handlers.MAIN.post(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, getTabDrawer());
                }
            });
            mArrowImage.setImageResource(R.drawable.ic_action_home);
            mArrowImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        }

        // Post drawer locking in case the activity is being recreated
        Handlers.MAIN.post(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, getBookmarkDrawer());
            }
        });

        arrowButton.setOnClickListener(this);

        // create the search EditText in the ToolBar
        mSearch = customView.findViewById(R.id.search);
        mSearchBackground = customView.findViewById(R.id.search_container);

        // initialize search background color
        mSearchBackground.getBackground().setColorFilter(getSearchBarColor(primaryColor, primaryColor), PorterDuff.Mode.SRC_IN);
        mSearch.setHintTextColor(ThemeUtils.getThemedTextHintColor(mDarkTheme));
        mSearch.setTextColor(mDarkTheme ? Color.WHITE : Color.BLACK);

        mUntitledTitle = getString(R.string.untitled);
        mBackgroundColor = ThemeUtils.getPrimaryColor(this);
        mDeleteIcon = ThemeUtils.getThemedDrawable(this, R.drawable.ic_action_delete, mDarkTheme);
        mRefreshIcon = ThemeUtils.getThemedDrawable(this, R.drawable.ic_action_refresh, mDarkTheme);
        mClearIcon = ThemeUtils.getThemedDrawable(this, R.drawable.ic_action_delete, mDarkTheme);

        int iconBounds = Utils.dpToPx(24);
        mDeleteIcon.setBounds(0, 0, iconBounds, iconBounds);
        mRefreshIcon.setBounds(0, 0, iconBounds, iconBounds);
        mClearIcon.setBounds(0, 0, iconBounds, iconBounds);
        mIcon = mRefreshIcon;
        SearchListenerClass search = new SearchListenerClass();
        mSearch.setCompoundDrawablePadding(Utils.dpToPx(3));
        mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
        mSearch.setOnKeyListener(search);
        mSearch.setOnFocusChangeListener(search);
        mSearch.setOnEditorActionListener(search);
        mSearch.setOnTouchListener(search);
        mSearch.setOnPreFocusListener(search);

        initializeSearchSuggestions(mSearch);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);

        if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        }

        @SuppressWarnings("VariableNotUsedInsideIf")
        Intent intent = savedInstanceState == null ? getIntent() : null;

        boolean launchedFromHistory = intent != null && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;

        if (isPanicTrigger(intent)) {
            setIntent(null);
            panicClean();
        } else {
            if (launchedFromHistory) {
                intent = null;
            }
            mPresenter.setupTabs(intent);
            setIntent(null);
            mProxyUtils.checkForProxy(this);
        }
    }

    @IdRes
    private int getBookmarksFragmentViewId() {
        return mSwapBookmarksAndTabs ? R.id.left_drawer : R.id.right_drawer;
    }

    private int getTabsFragmentViewId() {
        if (mShowTabsInDrawer) {
            return mSwapBookmarksAndTabs ? R.id.right_drawer : R.id.left_drawer;
        } else {
            return R.id.tabs_toolbar_container;
        }
    }

    /**
     * Determines if an intent is originating
     * from a panic trigger.
     *
     * @param intent the intent to check.
     * @return true if the panic trigger sent
     * the intent, false otherwise.
     */
    static boolean isPanicTrigger(@Nullable Intent intent) {
        return intent != null && INTENT_PANIC_TRIGGER.equals(intent.getAction());
    }

    void panicClean() {
        Log.d(TAG, "Closing browser");
        mTabsManager.newTab(this, "", false);
        mTabsManager.switchToTab(0);
        mTabsManager.clearSavedState();
        HistoryPage.deleteHistoryPage(getApplication()).subscribe();
        closeBrowser();
        // System exit needed in the case of receiving
        // the panic intent since finish() isn't completely
        // closing the browser
        System.exit(1);
    }

    private class SearchListenerClass implements OnKeyListener, OnEditorActionListener,
        OnFocusChangeListener, OnTouchListener, SearchView.PreFocusListener {

        @Override
        public boolean onKey(View searchView, int keyCode, KeyEvent keyEvent) {

            switch (keyCode) {
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
        public void onFocusChange(final View v, final boolean hasFocus) {
            final LightningView currentView = mTabsManager.getCurrentTab();
            if (!hasFocus && currentView != null) {
                setIsLoading(currentView.getProgress() < 100);
                updateUrl(currentView.getUrl(), false);
            } else if (hasFocus && currentView != null) {

                // Hack to make sure the text gets selected
                ((SearchView) v).selectAll();
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

        @Override
        public void onPreFocus() {
            final LightningView currentView = mTabsManager.getCurrentTab();
            if (currentView == null) {
                return;
            }
            String url = currentView.getUrl();
            if (!UrlUtils.isSpecialUrl(url)) {
                if (!mSearch.hasFocus()) {
                    mSearch.setText(url);
                }
            }
        }
    }

    private class DrawerLocker implements DrawerListener {

        @Override
        public void onDrawerClosed(View v) {
            View tabsDrawer = getTabDrawer();
            View bookmarksDrawer = getBookmarkDrawer();

            if (v == tabsDrawer) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, bookmarksDrawer);
            } else if (mShowTabsInDrawer) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, tabsDrawer);
            }
        }

        @Override
        public void onDrawerOpened(View v) {
            View tabsDrawer = getTabDrawer();
            View bookmarksDrawer = getBookmarkDrawer();

            if (v == tabsDrawer) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, bookmarksDrawer);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, tabsDrawer);
            }
        }

        @Override
        public void onDrawerSlide(View v, float arg) {}

        @Override
        public void onDrawerStateChanged(int arg) {}

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
        } else if (!isIncognito() && !mDarkTheme && mWebpageBitmap != null) {
            changeToolbarBackground(mWebpageBitmap, null);
        }

        FragmentManager manager = getSupportFragmentManager();
        Fragment tabsFragment = manager.findFragmentByTag(TAG_TABS_FRAGMENT);
        if (tabsFragment instanceof TabsFragment) {
            ((TabsFragment) tabsFragment).reinitializePreferences();
        }
        Fragment bookmarksFragment = manager.findFragmentByTag(TAG_BOOKMARK_FRAGMENT);
        if (bookmarksFragment instanceof BookmarksFragment) {
            ((BookmarksFragment) bookmarksFragment).reinitializePreferences();
        }

        // TODO layout transition causing memory leak
//        mBrowserFrame.setLayoutTransition(new LayoutTransition());

        setFullscreen(mPreferences.getHideStatusBarEnabled(), false);

        BaseSearchEngine currentSearchEngine = mSearchEngineProvider.getCurrentSearchEngine();
        mSearchText = currentSearchEngine.getQueryUrl();

        updateCookiePreference().subscribeOn(Schedulers.worker()).subscribe();
        mProxyUtils.updateProxySettings(this);
    }

    @Override
    public void onWindowVisibleToUserAfterResume() {
        super.onWindowVisibleToUserAfterResume();
        mToolbarLayout.setTranslationY(0);
        setWebViewTranslation(mToolbarLayout.getHeight());
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
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            mKeyDownStartTime = System.currentTimeMillis();
            Handlers.MAIN.postDelayed(mLongPressBackRunnable, ViewConfiguration.getLongPressTimeout());
        }
        return super.onKeyDown(keyCode, event);
    }

    private final Runnable mLongPressBackRunnable = new Runnable() {
        @Override
        public void run() {
            final LightningView currentTab = mTabsManager.getCurrentTab();
            showCloseDialog(mTabsManager.positionOf(currentTab));
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU)
            && (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN)
            && (Build.MANUFACTURER.compareTo("LGE") == 0)) {
            // Workaround for stupid LG devices that crash
            openOptionsMenu();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            Handlers.MAIN.removeCallbacks(mLongPressBackRunnable);
            if ((System.currentTimeMillis() - mKeyDownStartTime) > ViewConfiguration.getLongPressTimeout()) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Keyboard shortcuts
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.isCtrlPressed()) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_F:
                        // Search in page
                        findInPage();
                        return true;
                    case KeyEvent.KEYCODE_T:
                        // Open new tab
                        newTab(null, true);
                        return true;
                    case KeyEvent.KEYCODE_W:
                        // Close current tab
                        mPresenter.deleteTab(mTabsManager.indexOfCurrentTab());
                        return true;
                    case KeyEvent.KEYCODE_Q:
                        // Close browser
                        closeBrowser();
                        return true;
                    case KeyEvent.KEYCODE_R:
                        // Refresh current tab
                        LightningView currentTab = mTabsManager.getCurrentTab();
                        if (currentTab != null) {
                            currentTab.reload();
                        }
                        return true;
                    case KeyEvent.KEYCODE_TAB:
                        int nextIndex;
                        if (event.isShiftPressed()) {
                            // Go back one tab
                            if (mTabsManager.indexOfCurrentTab() > 0) {
                                nextIndex = mTabsManager.indexOfCurrentTab() - 1;
                            } else {
                                nextIndex = mTabsManager.last();
                            }
                        } else {
                            // Go forward one tab
                            if (mTabsManager.indexOfCurrentTab() < mTabsManager.last()) {
                                nextIndex = mTabsManager.indexOfCurrentTab() + 1;
                            } else {
                                nextIndex = 0;
                            }
                        }
                        mPresenter.tabChanged(nextIndex);
                        return true;
                }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                // Highlight search field
                mSearch.requestFocus();
                mSearch.selectAll();
                return true;
            } else if (event.isAltPressed()) {
                // Alt + tab number
                if (KeyEvent.KEYCODE_0 <= event.getKeyCode() && event.getKeyCode() <= KeyEvent.KEYCODE_9) {
                    int nextIndex;
                    if (event.getKeyCode() > mTabsManager.last() + KeyEvent.KEYCODE_1 || event.getKeyCode() == KeyEvent.KEYCODE_0) {
                        nextIndex = mTabsManager.last();
                    } else {
                        nextIndex = event.getKeyCode() - KeyEvent.KEYCODE_1;
                    }
                    mPresenter.tabChanged(nextIndex);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final LightningView currentView = mTabsManager.getCurrentTab();
        final String currentUrl = currentView != null ? currentView.getUrl() : null;
        // Handle action buttons
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(getBookmarkDrawer())) {
                    mDrawerLayout.closeDrawer(getBookmarkDrawer());
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
                new IntentUtils(this).shareUrl(currentUrl, currentView != null ? currentView.getTitle() : null);
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
            case R.id.action_downloads:
                openDownloads();
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

        final HistoryItem item = new HistoryItem(url, title);
        mBookmarkManager.addBookmarkIfNotExists(item)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<Boolean>() {
                @Override
                public void onItem(@Nullable Boolean item) {
                    if (Boolean.TRUE.equals(item)) {
                        mSuggestionsAdapter.refreshBookmarks();
                        mBookmarksView.handleUpdatedUrl(url);
                    }
                }
            });
    }

    private void deleteBookmark(final String title, final String url) {
        final HistoryItem item = new HistoryItem(url, title);

        mBookmarkManager.deleteBookmark(item)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<Boolean>() {
                @Override
                public void onItem(@Nullable Boolean item) {
                    if (Boolean.TRUE.equals(item)) {
                        mSuggestionsAdapter.refreshBookmarks();
                        mBookmarksView.handleUpdatedUrl(url);
                    }
                }
            });
    }

    private void putToolbarInRoot() {
        if (mToolbarLayout.getParent() != mUiLayout) {
            if (mToolbarLayout.getParent() != null) {
                ((ViewGroup) mToolbarLayout.getParent()).removeView(mToolbarLayout);
            }

            mUiLayout.addView(mToolbarLayout, 0);
            mUiLayout.requestLayout();
        }
        setWebViewTranslation(0);
    }

    private void overlayToolbarOnWebView() {
        if (mToolbarLayout.getParent() != mBrowserFrame) {
            if (mToolbarLayout.getParent() != null) {
                ((ViewGroup) mToolbarLayout.getParent()).removeView(mToolbarLayout);
            }

            mBrowserFrame.addView(mToolbarLayout);
            mBrowserFrame.requestLayout();
        }
        setWebViewTranslation(mToolbarLayout.getHeight());
    }

    private void setWebViewTranslation(float translation) {
        if (mFullScreen && mCurrentView != null) {
            mCurrentView.setTranslationY(translation);
        } else if (mCurrentView != null) {
            mCurrentView.setTranslationY(0);
        }
    }

    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    private void findInPage() {
        BrowserDialog.showEditText(this,
            R.string.action_find,
            R.string.search_hint,
            R.string.search_hint, new BrowserDialog.EditorListener() {
                @Override
                public void onClick(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        mPresenter.findInPage(text);
                        showFindInPageControls(text);
                    }
                }
            });
    }

    private void showFindInPageControls(@NonNull String text) {
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
    public TabsManager getTabModel() {
        return mTabsManager;
    }

    @Override
    public void showCloseDialog(final int position) {
        if (position < 0) {
            return;
        }
        BrowserDialog.show(this, R.string.dialog_title_close_browser,
            new BrowserDialog.Item(R.string.close_tab) {
                @Override
                public void onClick() {
                    mPresenter.deleteTab(position);
                }
            },
            new BrowserDialog.Item(R.string.close_other_tabs) {
                @Override
                public void onClick() {
                    mPresenter.closeAllOtherTabs();
                }
            },
            new BrowserDialog.Item(R.string.close_all_tabs) {
                @Override
                public void onClick() {
                    closeBrowser();
                }
            });
    }

    @Override
    public void notifyTabViewRemoved(int position) {
        Log.d(TAG, "Notify Tab Removed: " + position);
        mTabsView.tabRemoved(position);
    }

    @Override
    public void notifyTabViewAdded() {
        Log.d(TAG, "Notify Tab Added");
        mTabsView.tabAdded();
    }

    @Override
    public void notifyTabViewChanged(int position) {
        Log.d(TAG, "Notify Tab Changed: " + position);
        mTabsView.tabChanged(position);
    }

    @Override
    public void notifyTabViewInitialized() {
        Log.d(TAG, "Notify Tabs Initialized");
        mTabsView.tabsInitialized();
    }

    @Override
    public void tabChanged(LightningView tab) {
        mPresenter.tabChangeOccurred(tab);
    }

    @Override
    public void removeTabView() {

        Log.d(TAG, "Remove the tab view");

        // Set the background color so the color mode color doesn't show through
        mBrowserFrame.setBackgroundColor(mBackgroundColor);

        removeViewFromParent(mCurrentView);

        mCurrentView = null;

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        Handlers.MAIN.postDelayed(new Runnable() {
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

        Log.d(TAG, "Setting the tab view");

        // Set the background color so the color mode color doesn't show through
        mBrowserFrame.setBackgroundColor(mBackgroundColor);

        removeViewFromParent(view);
        removeViewFromParent(mCurrentView);

        mBrowserFrame.addView(view, 0, MATCH_PARENT);
        if (mFullScreen) {
            view.setTranslationY(mToolbarLayout.getHeight() + mToolbarLayout.getTranslationY());
        } else {
            view.setTranslationY(0);
        }

        view.requestFocus();

        mCurrentView = view;

        showActionBar();

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        Handlers.MAIN.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 200);

        // Handlers.MAIN.postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        // Remove browser frame background to reduce overdraw
        //TODO evaluate performance
        //         mBrowserFrame.setBackgroundColor(Color.TRANSPARENT);
        //     }
        // }, 300);
    }

    @Override
    public void showBlockedLocalFileDialog(@NonNull DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = builder.setCancelable(true)
            .setTitle(R.string.title_warning)
            .setMessage(R.string.message_blocked_local)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_open, listener)
            .show();

        BrowserDialog.setDialogSize(this, dialog);
    }

    @Override
    public void showSnackbar(@StringRes int resource) {
        Utils.showSnackbar(this, resource);
    }

    @Override
    public void tabCloseClicked(int position) {
        mPresenter.deleteTab(position);
    }

    @Override
    public void tabClicked(int position) {
        showTab(position);
    }

    @Override
    public void newTabButtonClicked() {
        mPresenter.newTab(null, true);
    }

    @Override
    public void newTabButtonLongClicked() {
        String url = mPreferences.getSavedUrl();
        if (url != null) {
            newTab(url, true);

            Utils.showSnackbar(this, R.string.deleted_tab);
        }
        mPreferences.setSavedUrl(null);
    }

    @Override
    public void bookmarkButtonClicked() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        final String url = currentTab != null ? currentTab.getUrl() : null;
        final String title = currentTab != null ? currentTab.getTitle() : null;
        if (url == null) {
            return;
        }

        if (!UrlUtils.isSpecialUrl(url)) {
            mBookmarkManager.isBookmark(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(new SingleOnSubscribe<Boolean>() {
                    @Override
                    public void onItem(@Nullable Boolean item) {
                        if (Boolean.TRUE.equals(item)) {
                            deleteBookmark(title, url);
                        } else {
                            addBookmark(title, url);
                        }
                    }
                });
        }
    }

    @Override
    public void bookmarkItemClicked(@NonNull HistoryItem item) {
        mPresenter.loadUrlInCurrentView(item.getUrl());
        // keep any jank from happening when the drawer is closed after the
        // URL starts to load
        Handlers.MAIN.postDelayed(new Runnable() {
            @Override
            public void run() {
                closeDrawers(null);
            }
        }, 150);
    }

    @Override
    public void handleHistoryChange() {
        openHistory();
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
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

    void handleNewIntent(Intent intent) {
        mPresenter.onNewIntent(intent);
    }

    @Override
    public void closeEmptyTab() {
        // Currently do nothing
        // Possibly closing the current tab might close the browser
        // and mess stuff up
    }

    @Override
    public void onTrimMemory(int level) {
        if (level > TRIM_MEMORY_MODERATE && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.d(TAG, "Low Memory, Free Memory");
            mPresenter.onAppLowMemory();
        }
    }

    // TODO move to presenter
    private synchronized boolean newTab(String url, boolean show) {
        return mPresenter.newTab(url, show);
    }

    void performExitCleanUp() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (mPreferences.getClearCacheExit() && currentTab != null && !isIncognito()) {
            WebUtils.clearCache(currentTab.getWebView());
            Log.d(TAG, "Cache Cleared");
        }
        if (mPreferences.getClearHistoryExitEnabled() && !isIncognito()) {
            WebUtils.clearHistory(this, mHistoryModel);
            Log.d(TAG, "History Cleared");
        }
        if (mPreferences.getClearCookiesExitEnabled() && !isIncognito()) {
            WebUtils.clearCookies(this);
            Log.d(TAG, "Cookies Cleared");
        }
        if (mPreferences.getClearWebStorageExitEnabled() && !isIncognito()) {
            WebUtils.clearWebStorage();
            Log.d(TAG, "WebStorage Cleared");
        } else if (isIncognito()) {
            WebUtils.clearWebStorage();     // We want to make sure incognito mode is secure
        }
        mSuggestionsAdapter.clearCache();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged");

        if (mFullScreen) {
            showActionBar();
            mToolbarLayout.setTranslationY(0);
            setWebViewTranslation(mToolbarLayout.getHeight());
        }

        supportInvalidateOptionsMenu();
        initializeToolbarHeight(newConfig);
    }

    private void initializeToolbarHeight(@NonNull final Configuration configuration) {
        // TODO externalize the dimensions
        doOnLayout(mUiLayout, new Runnable() {
            @Override
            public void run() {
                int toolbarSize;
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // In portrait toolbar should be 56 dp tall
                    toolbarSize = Utils.dpToPx(56);
                } else {
                    // In landscape toolbar should be 48 dp tall
                    toolbarSize = Utils.dpToPx(52);
                }
                mToolbar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarSize));
                mToolbar.setMinimumHeight(toolbarSize);
                doOnLayout(mToolbar, new Runnable() {
                    @Override
                    public void run() {
                        setWebViewTranslation(mToolbarLayout.getHeight());
                    }
                });
                mToolbar.requestLayout();

            }
        });
    }

    public void closeBrowser() {
        mBrowserFrame.setBackgroundColor(mBackgroundColor);
        removeViewFromParent(mCurrentView);
        performExitCleanUp();
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
        if (mDrawerLayout.isDrawerOpen(getTabDrawer())) {
            mDrawerLayout.closeDrawer(getTabDrawer());
        } else if (mDrawerLayout.isDrawerOpen(getBookmarkDrawer())) {
            mBookmarksView.navigateBack();
        } else {
            if (currentTab != null) {
                Log.d(TAG, "onBackPressed");
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
                        mPresenter.deleteTab(mTabsManager.positionOf(currentTab));
                    }
                }
            } else {
                Log.e(TAG, "This shouldn't happen ever");
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mTabsManager.pauseAll();
        try {
            getApplication().unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver was not registered", e);
        }
        if (isIncognito() && isFinishing()) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_down_out);
        }
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
        Log.d(TAG, "onDestroy");

        Handlers.MAIN.removeCallbacksAndMessages(null);

        mPresenter.shutdown();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mProxyUtils.onStart(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTabsManager.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mSwapBookmarksAndTabs != mPreferences.getBookmarksAndTabsSwapped()) {
            restart();
        }

        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.refreshPreferences();
            mSuggestionsAdapter.refreshBookmarks();
        }
        mTabsManager.resumeAll(this);
        initializePreferences();

        supportInvalidateOptionsMenu();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_BROADCAST_ACTION);
        getApplication().registerReceiver(mNetworkReceiver, filter);

        if (mFullScreen) {
            overlayToolbarOnWebView();
        } else {
            putToolbarInRoot();
        }
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

                final int finalColor; // Lighten up the dark color if it is
                // too dark
                if (!mShowTabsInDrawer || Utils.isColorTooDark(color)) {
                    finalColor = Utils.mixTwoColors(defaultColor, color, 0.25f);
                } else {
                    finalColor = color;
                }

                final Window window = getWindow();
                if (!mShowTabsInDrawer) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                }

                final int startSearchColor = getSearchBarColor(mCurrentUiColor, defaultColor);
                final int finalSearchColor = getSearchBarColor(finalColor, defaultColor);

                Animation animation = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        final int color = DrawableUtils.mixColor(interpolatedTime, mCurrentUiColor, finalColor);
                        if (mShowTabsInDrawer) {
                            mBackground.setColor(color);
                            Handlers.MAIN.post(new Runnable() {
                                @Override
                                public void run() {
                                    window.setBackgroundDrawable(mBackground);
                                }
                            });
                        } else if (tabBackground != null) {
                            tabBackground.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        }
                        mCurrentUiColor = color;
                        mToolbarLayout.setBackgroundColor(color);
                        mSearchBackground.getBackground().setColorFilter(DrawableUtils.mixColor(interpolatedTime,
                            startSearchColor, finalSearchColor), PorterDuff.Mode.SRC_IN);
                    }
                };
                animation.setDuration(300);
                mToolbarLayout.startAnimation(animation);
            }
        });
    }

    private int getSearchBarColor(int requestedColor, int defaultColor) {
        if (requestedColor == defaultColor) {
            return mDarkTheme ? DrawableUtils.mixColor(0.25f, defaultColor, Color.WHITE) : Color.WHITE;
        } else {
            return DrawableUtils.mixColor(0.25f, requestedColor, Color.WHITE);
        }
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
    public void updateUrl(@Nullable String url, boolean isLoading) {
        if (url == null || mSearch == null || mSearch.hasFocus()) {
            return;
        }
        final LightningView currentTab = mTabsManager.getCurrentTab();
        mBookmarksView.handleUpdatedUrl(url);

        String currentTitle = currentTab != null ? currentTab.getTitle() : null;

        mSearch.setText(mSearchBoxModel.getDisplayContent(url, currentTitle, isLoading));
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

        mHistoryModel.visitHistoryItem(url, title)
            .subscribeOn(Schedulers.io())
            .subscribe(new CompletableOnSubscribe() {
                @Override
                public void onError(@NonNull Throwable throwable) {
                    Log.e(TAG, "Exception while updating history", throwable);
                }
            });
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

        mSuggestionsAdapter = new SuggestionsAdapter(this, mDarkTheme, isIncognito());

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
                if (url == null || url.startsWith(getString(R.string.suggestion))) {
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
                mPresenter.onAutoCompleteItemPressed();
            }

        });

        getUrl.setSelectAllOnFocus(true);
        getUrl.setAdapter(mSuggestionsAdapter);
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private void openHistory() {
        new HistoryPage().getHistoryPage()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    Preconditions.checkNonNull(item);
                    LightningView view = mTabsManager.getCurrentTab();
                    if (view != null) {
                        view.loadUrl(item);
                    }
                }
            });
    }

    private void openDownloads() {
        new DownloadsPage().getDownloadsPage()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<String>() {
                @Override
                public void onItem(@Nullable String item) {
                    Preconditions.checkNonNull(item);
                    LightningView view = mTabsManager.getCurrentTab();
                    if (view != null) {
                        view.loadUrl(item);
                    }
                }
            });
    }

    private View getBookmarkDrawer() {
        return mSwapBookmarksAndTabs ? mDrawerLeft : mDrawerRight;
    }

    private View getTabDrawer() {
        return mSwapBookmarksAndTabs ? mDrawerRight : mDrawerLeft;
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private void openBookmarks() {
        if (mDrawerLayout.isDrawerOpen(getTabDrawer())) {
            mDrawerLayout.closeDrawers();
        }
        mDrawerLayout.openDrawer(getBookmarkDrawer());
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
                Log.e(TAG, "Unable to create Image File", ex);
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
                    Log.e(TAG, "Error hiding custom view", e);
                }
            }
            return;
        }
        try {
            view.setKeepScreenOn(true);
        } catch (SecurityException e) {
            Log.e(TAG, "WebView is not allowed to keep the screen on");
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
    public void closeBookmarksDrawer() {
        mDrawerLayout.closeDrawer(getBookmarkDrawer());
    }

    @Override
    public void onHideCustomView() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (mCustomView == null || mCustomViewCallback == null || currentTab == null) {
            if (mCustomViewCallback != null) {
                try {
                    mCustomViewCallback.onCustomViewHidden();
                } catch (Exception e) {
                    Log.e(TAG, "Error hiding custom view", e);
                }
                mCustomViewCallback = null;
            }
            return;
        }
        Log.d(TAG, "onHideCustomView");
        currentTab.setVisibility(View.VISIBLE);
        try {
            mCustomView.setKeepScreenOn(false);
        } catch (SecurityException e) {
            Log.e(TAG, "WebView is not allowed to keep the screen on");
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
            Log.d(TAG, "VideoView is being stopped");
            mVideoView.stopPlayback();
            mVideoView.setOnErrorListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView = null;
        }
        if (mCustomViewCallback != null) {
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Exception e) {
                Log.e(TAG, "Error hiding custom view", e);
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

    @Override
    public void onBackButtonPressed() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab != null) {
            if (currentTab.canGoBack()) {
                currentTab.goBack();
                closeDrawers(null);
            } else {
                mPresenter.deleteTab(mTabsManager.positionOf(currentTab));
            }
        }
    }

    @Override
    public void onForwardButtonPressed() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab != null) {
            if (currentTab.canGoForward()) {
                currentTab.goForward();
                closeDrawers(null);
            }
        }
    }

    @Override
    public void onHomeButtonPressed() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab != null) {
            currentTab.loadHomepage();
            closeDrawers(null);
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
            if (immersive) {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        mPresenter.deleteTab(mTabsManager.positionOf(view));
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
                        float trans = interpolatedTime * height;
                        mToolbarLayout.setTranslationY(-trans);
                        setWebViewTranslation(height - trans);
                    }
                };
                show.setDuration(250);
                show.setInterpolator(new BezierDecelerateInterpolator());
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
            Log.d(TAG, "showActionBar");
            if (mToolbarLayout == null)
                return;

            int height = mToolbarLayout.getHeight();
            if (height == 0) {
                mToolbarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                height = mToolbarLayout.getMeasuredHeight();
            }

            final int totalHeight = height;
            if (mToolbarLayout.getTranslationY() < -(height - 0.01f)) {
                Animation show = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float trans = interpolatedTime * totalHeight;
                        mToolbarLayout.setTranslationY(trans - totalHeight);
                        setWebViewTranslation(trans);
                    }
                };
                show.setDuration(250);
                show.setInterpolator(new BezierDecelerateInterpolator());
                mBrowserFrame.startAnimation(show);
            }
        }
    }

    @Override
    public void handleBookmarksChange() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab != null && UrlUtils.isBookmarkUrl(currentTab.getUrl())) {
            currentTab.loadBookmarkpage();
        }
        if (currentTab != null) {
            mBookmarksView.handleUpdatedUrl(currentTab.getUrl());
        }
    }

    @Override
    public void handleDownloadDeleted() {
        final LightningView currentTab = mTabsManager.getCurrentTab();
        if (currentTab != null && UrlUtils.isDownloadsUrl(currentTab.getUrl())) {
            currentTab.loadDownloadspage();
        }
        if (currentTab != null) {
            mBookmarksView.handleUpdatedUrl(currentTab.getUrl());
        }
    }

    @Override
    public void handleBookmarkDeleted(@NonNull HistoryItem item) {
        mBookmarksView.handleBookmarkDeleted(item);
        handleBookmarksChange();
    }

    @Override
    public void handleNewTab(@NonNull LightningDialogBuilder.NewTab newTabType, @NonNull String url) {
        mDrawerLayout.closeDrawers();
        switch (newTabType) {
            case FOREGROUND:
                newTab(url, true);
                break;
            case BACKGROUND:
                newTab(url, false);
                break;
            case INCOGNITO:
                Intent intent = new Intent(BrowserActivity.this, IncognitoActivity.class);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale);
                break;
        }
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
                    //noinspection deprecation
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                runnable.run();
            }
        });
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
                    mDrawerLayout.openDrawer(getTabDrawer());
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
     * This NetworkReceiver notifies each of the WebViews in the browser whether
     * the network is currently connected or not. This is important because some
     * JavaScript properties rely on the WebView knowing the current network state.
     * It is used to help the browser be compliant with the HTML5 spec, sec. 5.7.7
     */
    private final NetworkReceiver mNetworkReceiver = new NetworkReceiver() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d(TAG, "Network Connected: " + isConnected);
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

}
