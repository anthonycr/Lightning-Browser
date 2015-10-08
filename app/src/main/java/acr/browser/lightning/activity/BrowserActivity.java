/*
 * Copyright 2015 Anthony Restaino
 */

package acr.browser.lightning.activity;

import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
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

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.controller.BrowserController;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.dialog.BookmarksDialogBuilder;
import acr.browser.lightning.object.ClickHandler;
import acr.browser.lightning.object.SearchAdapter;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.receiver.NetworkReceiver;
import acr.browser.lightning.utils.PermissionsManager;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.UrlUtils;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.utils.WebUtils;
import acr.browser.lightning.view.AnimatedProgressBar;
import acr.browser.lightning.view.LightningView;
import butterknife.Bind;
import butterknife.ButterKnife;

public abstract class BrowserActivity extends ThemableBrowserActivity implements BrowserController, OnClickListener, OnLongClickListener {

    // Static Layout
    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.content_frame)
    FrameLayout mBrowserFrame;

    @Bind(R.id.left_drawer)
    ViewGroup mDrawerLeft;

    @Bind(R.id.right_drawer)
    ViewGroup mDrawerRight;

    @Bind(R.id.ui_layout)
    ViewGroup mUiLayout;

    @Bind(R.id.toolbar_layout)
    ViewGroup mToolbarLayout;

    @Bind(R.id.progress_view)
    AnimatedProgressBar mProgressBar;

    @Bind(R.id.search_bar)
    RelativeLayout mSearchBar;


    // Browser Views
    private final List<LightningView> mWebViewList = new ArrayList<>();
    private LightningView mCurrentView;
    private WebView mWebView;
    private RecyclerView mTabListView;

    // Toolbar Views
    private AutoCompleteTextView mSearch;
    private ImageView mArrowImage;

    // Full Screen Video Views
    private FrameLayout mFullscreenContainer;
    private VideoView mVideoView;
    private View mCustomView;

    // Adapter
    private LightningViewAdapter mTabAdapter;
    private SearchAdapter mSearchAdapter;

    // Callback
    private ClickHandler mClickHandler;
    private CustomViewCallback mCustomViewCallback;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Primatives
    private boolean mFullScreen, mColorMode, mDarkTheme,
            mIsNewIntent = false,
            mIsFullScreen = false,
            mIsImmersive = false,
            mShowTabsInDrawer;
    private int mOriginalOrientation, mBackgroundColor, mIdGenerator, mIconColor,
            mCurrentUiColor = Color.BLACK;
    private String mSearchText, mUntitledTitle, mHomepage, mCameraPhotoPath;

    // Storage
    private HistoryDatabase mHistoryDatabase;
    private final PreferenceManager mPreferences = PreferenceManager.getInstance();

    // The singleton BookmarkManager
    @Inject
    BookmarkManager mBookmarkManager;

    // Event bus
    @Inject
    Bus mEventBus;

    @Inject
    BookmarkPage mBookmarkPage;

    @Inject
    BookmarksDialogBuilder mBookmarksDialogBuilder;

    // Image
    private Bitmap mWebpageBitmap;
    private final ColorDrawable mBackground = new ColorDrawable();
    private Drawable mDeleteIcon, mRefreshIcon, mClearIcon, mIcon;
    private DrawerArrowDrawable mArrowDrawable;

    // Proxy
    private ProxyUtils mProxyUtils;

    // Constant
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private static final String NETWORK_BROADCAST_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final LayoutParams MATCH_PARENT = new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT);
    private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    public abstract boolean isIncognito();

    abstract void initializeTabs();

    abstract void closeActivity();

    public abstract void updateHistory(final String title, final String url);

    abstract void updateCookiePreference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
    }

    private synchronized void initialize() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        //TODO make sure dark theme flag gets set correctly
        mDarkTheme = mPreferences.getUseTheme() != 0 || isIncognito();
        mIconColor = mDarkTheme ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        mShowTabsInDrawer = mPreferences.getShowTabsInDrawer(!isTablet());

        mWebViewList.clear();

        mClickHandler = new ClickHandler(this);
        // initialize background ColorDrawable
        mBackground.setColor(((ColorDrawable) mToolbarLayout.getBackground()).getColor());

        setupFrameLayoutButton(R.id.new_tab_button, R.id.icon_plus);
        // Drawer stutters otherwise
        mDrawerLeft.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mDrawerRight.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        ImageView tabTitleImage = (ImageView) findViewById(R.id.plusIcon);
        tabTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mShowTabsInDrawer) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        setNavigationDrawerWidth();
        mDrawerLayout.setDrawerListener(new DrawerLocker());

        mWebpageBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_webpage, mDarkTheme);

        mHomepage = mPreferences.getHomepage();

        RecyclerView horizontalListView = (RecyclerView) findViewById(R.id.twv_list);


        if (mShowTabsInDrawer) {
            mTabAdapter = new LightningViewAdapter(this, R.layout.tab_list_item, mWebViewList);
            mTabListView = (RecyclerView) findViewById(R.id.left_drawer_list);
            mTabListView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            mTabListView.setLayoutManager(layoutManager);
            mTabListView.setHasFixedSize(true);
            mToolbarLayout.removeView(horizontalListView);
        } else {
            mTabAdapter = new LightningViewAdapter(this, R.layout.tab_list_item_horizontal, mWebViewList);
            mTabListView = horizontalListView;
            mTabListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerLeft);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            mTabListView.setLayoutManager(layoutManager);
            mTabListView.setHasFixedSize(true);
        }

        mTabListView.setAdapter(mTabAdapter);

        mHistoryDatabase = HistoryDatabase.getInstance();

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
            // Use hardware acceleration for the animation
            mArrowDrawable = new DrawerArrowDrawable(this);
            mArrowImage.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mArrowImage.setImageDrawable(mArrowDrawable);
        } else {
            mArrowImage.setImageResource(R.drawable.ic_action_home);
            mArrowImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        }
        arrowButton.setOnClickListener(this);

        mProxyUtils = ProxyUtils.getInstance();

        setupFrameLayoutButton(R.id.action_back, R.id.icon_back);
        setupFrameLayoutButton(R.id.action_forward, R.id.icon_forward);
        setupFrameLayoutButton(R.id.action_toggle_desktop, R.id.icon_desktop);
        setupFrameLayoutButton(R.id.action_reading, R.id.icon_reading);
        setupFrameLayoutButton(R.id.action_home, R.id.icon_home);

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

        new Thread(new Runnable() {

            @Override
            public void run() {
                initializeSearchSuggestions(mSearch);
            }

        }).run();

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);

        if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        }

        initializeTabs();

        mProxyUtils.checkForProxy(this);
    }

    private class SearchListenerClass implements OnKeyListener, OnEditorActionListener, OnFocusChangeListener, OnTouchListener {

        @Override
        public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

            switch (arg1) {
                case KeyEvent.KEYCODE_ENTER:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
                    searchTheWeb(mSearch.getText().toString());
                    if (mCurrentView != null) {
                        mCurrentView.requestFocus();
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
                if (mCurrentView != null) {
                    mCurrentView.requestFocus();
                }
                return true;
            }
            return false;
        }

        @Override
        public void onFocusChange(View v, final boolean hasFocus) {
            if (!hasFocus && mCurrentView != null) {
                setIsLoading(mCurrentView.getProgress() < 100);
                updateUrl(mCurrentView.getUrl(), true);
            } else if (hasFocus) {
                String url = mCurrentView.getUrl();
                if (url.startsWith(Constants.FILE)) {
                    mSearch.setText("");
                } else {
                    mSearch.setText(url);
                }
                ((AutoCompleteTextView) v).selectAll(); // Hack to make sure
                // the text gets
                // selected
                mIcon = mClearIcon;
                mSearch.setCompoundDrawables(null, null, mClearIcon, null);
            }
            final Animation anim = new Animation() {

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    if (!hasFocus) {
                        mArrowDrawable.setProgress(1.0f - interpolatedTime);
                    } else {
                        mArrowDrawable.setProgress(interpolatedTime);
                    }
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }

            };
            anim.setDuration(300);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!hasFocus) {
                        mArrowDrawable.setProgress(0.0f);
                    } else {
                        mArrowDrawable.setProgress(1.0f);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

            });
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (mArrowDrawable != null) {
                        mArrowImage.startAnimation(anim);
                    }
                }

            }, 100);

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

    void restoreOrNewTab() {
        mIdGenerator = 0;

        String url = null;
        if (getIntent() != null) {
            url = getIntent().getDataString();
            if (url != null) {
                if (url.startsWith(Constants.FILE)) {
                    Utils.showSnackbar(this, R.string.message_blocked_local);
                    url = null;
                }
            }
        }
        if (mPreferences.getRestoreLostTabsEnabled()) {
            String mem = mPreferences.getMemoryUrl();
            mPreferences.setMemoryUrl("");
            String[] array = Utils.getArray(mem);
            int count = 0;
            for (String urlString : array) {
                if (!urlString.isEmpty()) {
                    if (url != null && url.compareTo(urlString) == 0) {
                        url = null;
                    }
                    newTab(urlString, true);
                    count++;
                }
            }
            if (url != null) {
                newTab(url, true);
            } else if (count == 0) {
                newTab(null, true);
            }
        } else {
            newTab(url, true);
        }
    }

    private void initializePreferences() {
        mFullScreen = mPreferences.getFullScreenEnabled();
        mColorMode = mPreferences.getColorModeEnabled();
        mColorMode &= !mDarkTheme;
        if (!isIncognito() && !mColorMode && !mDarkTheme && mWebpageBitmap != null) {
            changeToolbarBackground(mWebpageBitmap, null);
            mTabAdapter.notifyDataSetChanged();
        } else if (!isIncognito() && mCurrentView != null && !mDarkTheme
                && mCurrentView.getFavicon() != null) {
            changeToolbarBackground(mCurrentView.getFavicon(), null);
            mTabAdapter.notifyDataSetChanged();
        }

        if (mFullScreen) {
            mToolbarLayout.setTranslationY(0);
            int height = mToolbarLayout.getHeight();
            if (height == 0) {
                mToolbarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                height = mToolbarLayout.getMeasuredHeight();
            }
            if (mWebView != null)
                mWebView.setTranslationY(height);
            mBrowserFrame.setLayoutTransition(null);
            if (mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
                mUiLayout.removeView(mToolbarLayout);
                mBrowserFrame.addView(mToolbarLayout);
                mToolbarLayout.bringToFront();
            }
        } else {
            mToolbarLayout.setTranslationY(0);
            if (mBrowserFrame.findViewById(R.id.toolbar_layout) != null) {
                mBrowserFrame.removeView(mToolbarLayout);
                mUiLayout.addView(mToolbarLayout, 0);
            }
            mBrowserFrame.setLayoutTransition(new LayoutTransition());
            if (mWebView != null)
                mWebView.setTranslationY(0);
        }
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

        updateCookiePreference();
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
        // Handle action buttons
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
                    mDrawerLayout.closeDrawer(mDrawerRight);
                }
                return true;
            case R.id.action_back:
                if (mCurrentView != null && mCurrentView.canGoBack()) {
                    mCurrentView.goBack();
                }
                return true;
            case R.id.action_forward:
                if (mCurrentView != null && mCurrentView.canGoForward()) {
                    mCurrentView.goForward();
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
                if (mCurrentView != null && !mCurrentView.getUrl().startsWith(Constants.FILE)) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, mCurrentView.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, mCurrentView.getUrl());
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.dialog_title_share)));
                }
                return true;
            case R.id.action_bookmarks:
                openBookmarks();
                return true;
            case R.id.action_copy:
                if (mCurrentView != null && !mCurrentView.getUrl().startsWith(Constants.FILE)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", mCurrentView.getUrl());
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
                if (mCurrentView != null && !mCurrentView.getUrl().startsWith(Constants.FILE)) {
                    mEventBus.post(new BrowserEvents.AddBookmark(mCurrentView.getTitle(),
                            mCurrentView.getUrl()));
                }
                return true;
            case R.id.action_find:
                findInPage();
                return true;
            case R.id.action_reading_mode:
                Intent read = new Intent(this, ReadingActivity.class);
                read.putExtra(Constants.LOAD_READING_URL, mCurrentView.getUrl());
                startActivity(read);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
        if (mCurrentView != null) {
            mCurrentView.find(text);
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

    private void showCloseDialog(final int position) {
        if (position < 0) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line);
        adapter.add(this.getString(R.string.close_tab));
        adapter.add(this.getString(R.string.close_all_tabs));
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        deleteTab(position);
                        break;
                    case 1:
                        closeBrowser();
                        break;
                    default:
                        break;
                }
            }
        });
        builder.show();
    }

    /**
     * The click listener for ListView in the navigation drawer
     */
    private class DrawerItemClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            int position = mTabListView.getChildAdapterPosition(v);
            if (mCurrentView != mWebViewList.get(position)) {
                mIsNewIntent = false;
                showTab(mWebViewList.get(position));
            }
        }
    }

    /**
     * long click listener for Navigation Drawer
     */
    private class DrawerItemLongClickListener implements OnLongClickListener {

        @Override
        public boolean onLongClick(View v) {
            int position = mTabListView.getChildAdapterPosition(v);
            showCloseDialog(position);
            return true;
        }
    }

    /**
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     *
     * @param view the LightningView to show
     */
    private synchronized void showTab(LightningView view) {
        // Set the background color so the color mode color doesn't show through
        mBrowserFrame.setBackgroundColor(mBackgroundColor);
        if (view == null || (view == mCurrentView && !mCurrentView.isShown())) {
            return;
        }
        final float translation = mToolbarLayout.getTranslationY();
        mBrowserFrame.removeAllViews();
        if (mCurrentView != null) {
            mCurrentView.setForegroundTab(false);
            mCurrentView.onPause();
        }
        mCurrentView = view;
        mWebView = view.getWebView();
        mCurrentView.setForegroundTab(true);
        if (mWebView != null) {
            updateUrl(mCurrentView.getUrl(), true);
            updateProgress(mCurrentView.getProgress());
        } else {
            updateUrl("", true);
            updateProgress(0);
        }

        mBrowserFrame.addView(mWebView, MATCH_PARENT);
        mCurrentView.requestFocus();
        mCurrentView.onResume();

        if (mFullScreen) {
            // mToolbarLayout has already been removed
            mBrowserFrame.addView(mToolbarLayout);
            mToolbarLayout.bringToFront();
            Log.d(Constants.TAG, "Move view to browser frame");
            int height = mToolbarLayout.getHeight();
            if (height == 0) {
                mToolbarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                height = mToolbarLayout.getMeasuredHeight();
            }
            mWebView.setTranslationY(translation + height);
            mToolbarLayout.setTranslationY(translation);
        } else {
            mWebView.setTranslationY(0);
        }

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

        // Should update the bookmark status in BookmarksFragment
        mEventBus.post(new BrowserEvents.CurrentPageUrl(mCurrentView.getUrl()));

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
        // Remove browser frame background to reduce overdraw
        //TODO evaluate performance
//                mBrowserFrame.setBackgroundColor(Color.TRANSPARENT);
//            }
//        }, 300);

    }

    void handleNewIntent(Intent intent) {

        String url = null;
        if (intent != null) {
            url = intent.getDataString();
        }
        int num = 0;
        String source = null;
        if (intent != null && intent.getExtras() != null) {
            num = intent.getExtras().getInt(getPackageName() + ".Origin");
            source = intent.getExtras().getString("SOURCE");
        }
        if (num == 1) {
            loadUrlInCurrentView(url);
        } else if (url != null) {
            if (url.startsWith(Constants.FILE)) {
                Utils.showSnackbar(this, R.string.message_blocked_local);
                url = null;
            }
            newTab(url, true);
            mIsNewIntent = (source == null);
        }
    }

    private void loadUrlInCurrentView(final String url) {
        if (mCurrentView == null) {
            return;
        }

        mCurrentView.loadUrl(url);
        mEventBus.post(new BrowserEvents.CurrentPageUrl(url));
    }

    @Override
    public void closeEmptyTab() {
        if (mWebView != null && mWebView.copyBackForwardList().getSize() == 0) {
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
            for (int n = 0, size = mWebViewList.size(); n < size; n++) {
                mWebViewList.get(n).freeMemory();
            }
        }
    }

    synchronized boolean newTab(String url, boolean show) {
        // Limit number of tabs for limited version of app
        if (!Constants.FULL_VERSION && mWebViewList.size() >= 10) {
            Utils.showSnackbar(this, R.string.max_tabs);
            return false;
        }
        mIsNewIntent = false;
        LightningView startingTab = new LightningView(this, url, mDarkTheme, isIncognito(), this);
        if (mIdGenerator == 0) {
            startingTab.resumeTimers();
        }
        mIdGenerator++;
        mWebViewList.add(startingTab);

        if (show) {
            showTab(startingTab);
        }
        updateTabs();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mTabListView.smoothScrollToPosition(mWebViewList.size() - 1);
            }
        }, 300);

        return true;
    }

    private synchronized void deleteTab(int position) {
        if (position >= mWebViewList.size()) {
            return;
        }

        int current = mWebViewList.indexOf(mCurrentView);
        if (current < 0) {
            return;
        }
        LightningView reference = mWebViewList.get(position);
        if (reference == null) {
            return;
        }
        if (!reference.getUrl().startsWith(Constants.FILE) && !isIncognito()) {
            mPreferences.setSavedUrl(reference.getUrl());
        }
        boolean isShown = reference.isShown();
        if (isShown) {
            mBrowserFrame.setBackgroundColor(mBackgroundColor);
        }
        if (current > position) {
            mWebViewList.remove(position);
            updateTabs();
            reference.onDestroy();
        } else if (mWebViewList.size() > position + 1) {
            if (current == position) {
                showTab(mWebViewList.get(position + 1));
                mWebViewList.remove(position);
                updateTabs();
            } else {
                mWebViewList.remove(position);
            }

            reference.onDestroy();
        } else if (mWebViewList.size() > 1) {
            if (current == position) {
                showTab(mWebViewList.get(position - 1));
                mWebViewList.remove(position);
                updateTabs();
            } else {
                mWebViewList.remove(position);
            }

            reference.onDestroy();
        } else {
            if (mCurrentView.getUrl().startsWith(Constants.FILE) || mCurrentView.getUrl().equals(mHomepage)) {
                closeActivity();
            } else {
                mWebViewList.remove(position);
                performExitCleanUp();
                reference.pauseTimers();
                reference.onDestroy();
                mCurrentView = null;
                mWebView = null;
                mTabAdapter.notifyDataSetChanged();
                finish();
            }
        }
        mTabAdapter.notifyDataSetChanged();

        if (mIsNewIntent && isShown) {
            mIsNewIntent = false;
            closeActivity();
        }

        Log.d(Constants.TAG, "deleted tab");
    }

    private void performExitCleanUp() {
        if (mPreferences.getClearCacheExit() && mCurrentView != null && !isIncognito()) {
            WebUtils.clearCache(mCurrentView.getWebView());
            Log.d(Constants.TAG, "Cache Cleared");
        }
        if (mPreferences.getClearHistoryExitEnabled() && !isIncognito()) {
            WebUtils.clearHistory(this);
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showCloseDialog(mWebViewList.indexOf(mCurrentView));
        }
        return true;
    }

    private void closeBrowser() {
        mBrowserFrame.setBackgroundColor(mBackgroundColor);
        performExitCleanUp();
        mCurrentView = null;
        mWebView = null;
        for (int n = 0, size = mWebViewList.size(); n < size; n++) {
            if (mWebViewList.get(n) != null) {
                mWebViewList.get(n).onDestroy();
            }
        }
        mWebViewList.clear();
        mTabAdapter.notifyDataSetChanged();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawer(mDrawerLeft);
        } else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
            mEventBus
                    .post(new BrowserEvents.UserPressedBack());
        } else {
            if (mCurrentView != null) {
                Log.d(Constants.TAG, "onBackPressed");
                if (mSearch.hasFocus()) {
                    mCurrentView.requestFocus();
                } else if (mCurrentView.canGoBack()) {
                    if (!mCurrentView.isShown()) {
                        onHideCustomView();
                    } else {
                        mCurrentView.goBack();
                    }
                } else {
                    deleteTab(mWebViewList.indexOf(mCurrentView));
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
        Log.d(Constants.TAG, "onPause");
        if (mCurrentView != null) {
            mCurrentView.pauseTimers();
            mCurrentView.onPause();
        }
        try {
            unregisterReceiver(mNetworkReceiver);
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
            StringBuilder s = new StringBuilder(mWebViewList.size() * 50);
            for (int n = 0, size = mWebViewList.size(); n < size; n++) {
                if (!mWebViewList.get(n).getUrl().isEmpty()) {
                    s.append(mWebViewList.get(n).getUrl()).append("|$|SEPARATOR|$|");
                }
            }
            mPreferences.setMemoryUrl(s.toString());
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
        Log.d(Constants.TAG, "onResume");
        if (mSearchAdapter != null) {
            mSearchAdapter.refreshPreferences();
            mSearchAdapter.refreshBookmarks();
        }
        if (mCurrentView != null) {
            mCurrentView.resumeTimers();
            mCurrentView.onResume();
        }
        mHistoryDatabase = HistoryDatabase.getInstance();
        initializePreferences();
        for (int n = 0, size = mWebViewList.size(); n < size; n++) {
            if (mWebViewList.get(n) != null) {
                mWebViewList.get(n).initializePreferences(null, this);
            } else {
                mWebViewList.remove(n);
            }
        }

        supportInvalidateOptionsMenu();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_BROADCAST_ACTION);
        registerReceiver(mNetworkReceiver, filter);

        mEventBus.register(mBusEventListener);
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private void searchTheWeb(@NonNull String query) {
        if (query.isEmpty()) {
            return;
        }
        String searchUrl = mSearchText + UrlUtils.QUERY_PLACE_HOLDER;
        query = query.trim();
        mCurrentView.stopLoading();
        if (mCurrentView != null) {
            loadUrlInCurrentView(UrlUtils.smartUrlFilter(query, true, searchUrl));
        }
    }

    private class LightningViewAdapter extends RecyclerView.Adapter<LightningViewAdapter.LightningViewHolder> {

        private final Context context;
        private final int layoutResourceId;
        private List<LightningView> data = null;
        private final CloseTabListener mExitListener;
        private final Drawable mBackgroundTabDrawable;
        private final Drawable mForegroundTabDrawable;
        private final Bitmap mForegroundTabBitmap;
        private final DrawerItemClickListener mClickListener;
        private final DrawerItemLongClickListener mLongClickListener;
        private ColorMatrix mColorMatrix;
        private Paint mPaint;
        private ColorFilter mFilter;
        private static final float DESATURATED = 0.5f;

        public LightningViewAdapter(Context context, int layoutResourceId, List<LightningView> data) {
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
            this.mExitListener = new CloseTabListener();
            this.mClickListener = new DrawerItemClickListener();
            this.mLongClickListener = new DrawerItemLongClickListener();

            if (mShowTabsInDrawer) {
                mBackgroundTabDrawable = null;
                mForegroundTabBitmap = null;
                mForegroundTabDrawable = ThemeUtils.getSelectedBackground(context, mDarkTheme);
            } else {
                int backgroundColor = Utils.mixTwoColors(ThemeUtils.getPrimaryColor(context), Color.BLACK, 0.75f);
                Bitmap backgroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
                Utils.drawTrapezoid(new Canvas(backgroundTabBitmap), backgroundColor, true);
                mBackgroundTabDrawable = new BitmapDrawable(getResources(), backgroundTabBitmap);

                int foregroundColor = ThemeUtils.getPrimaryColor(context);
                mForegroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
                Utils.drawTrapezoid(new Canvas(mForegroundTabBitmap), foregroundColor, false);
                mForegroundTabDrawable = null;
            }
        }

        @Override
        public LightningViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View view = inflater.inflate(layoutResourceId, viewGroup, false);
            return new LightningViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final LightningViewHolder holder, int position) {
            holder.exitButton.setTag(position);
            holder.exitButton.setOnClickListener(mExitListener);
            holder.layout.setOnClickListener(mClickListener);
            holder.layout.setOnLongClickListener(mLongClickListener);

            ViewCompat.jumpDrawablesToCurrentState(holder.exitButton);

            LightningView web = data.get(position);
            holder.txtTitle.setText(web.getTitle());

            final Bitmap favicon = web.getFavicon();
            if (web.isForegroundTab()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.txtTitle.setTextAppearance(R.style.boldText);
                } else {
                    //noinspection deprecation
                    holder.txtTitle.setTextAppearance(context, R.style.boldText);
                }
                Drawable foregroundDrawable;
                if (!mShowTabsInDrawer) {
                    foregroundDrawable = new BitmapDrawable(getResources(), mForegroundTabBitmap);
                    if (!isIncognito() && mColorMode) {
                        foregroundDrawable.setColorFilter(mCurrentUiColor, PorterDuff.Mode.SRC_IN);
                    }
                } else {
                    foregroundDrawable = mForegroundTabDrawable;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.layout.setBackground(foregroundDrawable);
                } else {
                    //noinspection deprecation
                    holder.layout.setBackgroundDrawable(foregroundDrawable);
                }
                if (!isIncognito() && mColorMode) {
                    changeToolbarBackground(favicon, foregroundDrawable);
                }
                holder.favicon.setImageBitmap(favicon);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.txtTitle.setTextAppearance(R.style.normalText);
                } else {
                    //noinspection deprecation
                    holder.txtTitle.setTextAppearance(context, R.style.normalText);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.layout.setBackground(mBackgroundTabDrawable);
                } else {
                    //noinspection deprecation
                    holder.layout.setBackgroundDrawable(mBackgroundTabDrawable);
                }

                holder.favicon.setImageBitmap(getDesaturatedBitmap(favicon));
            }
        }

        @Override
        public int getItemCount() {
            return (data != null) ? data.size() : 0;
        }

        public Bitmap getDesaturatedBitmap(Bitmap favicon) {
            Bitmap grayscaleBitmap = Bitmap.createBitmap(favicon.getWidth(),
                    favicon.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(grayscaleBitmap);
            if (mColorMatrix == null || mFilter == null || mPaint == null) {
                mPaint = new Paint();
                mColorMatrix = new ColorMatrix();
                mColorMatrix.setSaturation(DESATURATED);
                mFilter = new ColorMatrixColorFilter(mColorMatrix);
                mPaint.setColorFilter(mFilter);
            }

            c.drawBitmap(favicon, 0, 0, mPaint);
            return grayscaleBitmap;
        }

        public class LightningViewHolder extends RecyclerView.ViewHolder {

            public LightningViewHolder(View view) {
                super(view);
                txtTitle = (TextView) view.findViewById(R.id.textTab);
                favicon = (ImageView) view.findViewById(R.id.faviconTab);
                exit = (ImageView) view.findViewById(R.id.deleteButton);
                layout = (LinearLayout) view.findViewById(R.id.tab_item_background);
                exitButton = (FrameLayout) view.findViewById(R.id.deleteAction);
                exit.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
            }

            final TextView txtTitle;
            final ImageView favicon;
            final ImageView exit;
            final FrameLayout exitButton;
            final LinearLayout layout;
        }
    }

    private class CloseTabListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            deleteTab((int) v.getTag());
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
    private void changeToolbarBackground(@NonNull Bitmap favicon, @Nullable final Drawable tabBackground) {
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
    public void updateUrl(String url, boolean shortUrl) {
        if (url == null || mSearch == null || mSearch.hasFocus()) {
            return;
        }
        mEventBus.post(new BrowserEvents.CurrentPageUrl(url));
        if (shortUrl && !url.startsWith(Constants.FILE)) {
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
                    if (mCurrentView != null && !mCurrentView.getTitle().isEmpty()) {
                        mSearch.setText(mCurrentView.getTitle());
                    } else {
                        mSearch.setText(mUntitledTitle);
                    }
                    break;
            }
        } else {
            if (url.startsWith(Constants.FILE)) {
                url = "";
            }
            mSearch.setText(url);
        }
    }

    @Override
    public void updateProgress(int n) {
        setIsLoading(n < 100);
        mProgressBar.setProgress(n);
    }

    void addItemToHistory(final String title, final String url) {
        Runnable update = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mHistoryDatabase == null) {
                        mHistoryDatabase = HistoryDatabase.getInstance();
                    }
                    mHistoryDatabase.visitHistoryItem(url, title);
                } catch (IllegalStateException e) {
                    Log.e(Constants.TAG, "IllegalStateException in updateHistory", e);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "NullPointerException in updateHistory", e);
                } catch (SQLiteException e) {
                    Log.e(Constants.TAG, "SQLiteException in updateHistory", e);
                }
            }
        };
        if (url != null && !url.startsWith(Constants.FILE)) {
            new Thread(update).start();
        }
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

        getUrl.setThreshold(1);
        getUrl.setDropDownWidth(-1);
        getUrl.setDropDownAnchor(R.id.toolbar_layout);
        getUrl.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                try {
                    String url;
                    url = ((TextView) arg1.findViewById(R.id.url)).getText().toString();
                    if (url.startsWith(BrowserActivity.this.getString(R.string.suggestion))) {
                        url = ((TextView) arg1.findViewById(R.id.title)).getText().toString();
                    } else {
                        getUrl.setText(url);
                    }
                    searchTheWeb(url);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
                    if (mCurrentView != null) {
                        mCurrentView.requestFocus();
                    }
                } catch (NullPointerException e) {
                    Log.e("Browser Error: ", "NullPointerException on item click");
                }
            }

        });

        getUrl.setSelectAllOnFocus(true);
        mSearchAdapter = new SearchAdapter(this, mDarkTheme, isIncognito());
        getUrl.setAdapter(mSearchAdapter);
    }

    @Override
    public boolean proxyIsNotReady() {
        return !mProxyUtils.isProxyReady(this);
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private void openHistory() {
        // use a thread so that history retrieval doesn't block the UI
        Thread history = new Thread(new Runnable() {

            @Override
            public void run() {
                loadUrlInCurrentView(HistoryPage.getHistoryPage(BrowserActivity.this));
                mSearch.setText("");
            }

        });
        history.run();
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

    void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem back = menu.findItem(R.id.action_back);
        MenuItem forward = menu.findItem(R.id.action_forward);
        if (back != null && back.getIcon() != null)
            back.getIcon().setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        if (forward != null && forward.getIcon() != null)
            forward.getIcon().setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * open the HTML bookmarks page, parameter view is the WebView that should show the page
     */
    @Override
    public void openBookmarkPage(WebView view) {
        if (view == null)
            return;
        Bitmap folderIcon = ThemeUtils.getThemedBitmap(this, R.drawable.ic_folder, false);
        FileOutputStream outputStream = null;
        File image = new File(this.getCacheDir(), "folder.png");
        try {
            outputStream = new FileOutputStream(image);
            folderIcon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            folderIcon.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            Utils.close(outputStream);
        }
        File bookmarkWebPage = new File(this.getFilesDir(), Constants.BOOKMARKS_FILENAME);

        mBookmarkPage.buildBookmarkPage(null, mBookmarkManager.getBookmarksFromFolder(null, true));
        view.loadUrl(Constants.FILE + bookmarkWebPage);
    }

    @Override
    public void updateTabs() {
        mTabAdapter.notifyDataSetChanged();
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

        this.startActivityForResult(chooserIntent, 1);
    }

    /**
     * handles long presses for the browser, tries to get the
     * url of the item that was clicked and sends it (it can be null)
     * to the click handler that does cool stuff with it
     */
    @Override
    public void onLongPress() {
        if (mClickHandler == null) {
            mClickHandler = new ClickHandler(this);
        }
        Message click = mClickHandler.obtainMessage();
        if (click != null) {
            click.setTarget(mClickHandler);
            mWebView.requestFocusNodeHref(click);
        }
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        if (mCustomView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }
        try {
            view.setKeepScreenOn(true);
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "WebView is not allowed to keep the screen on");
        }
        mOriginalOrientation = getRequestedOrientation();
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        mFullscreenContainer = new FrameLayout(this);
        mFullscreenContainer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        mCustomView = view;
        mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        setFullscreen(true, true);
        mCurrentView.setVisibility(View.GONE);
        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                mVideoView.setOnErrorListener(new VideoCompletionListener());
                mVideoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
        mCustomViewCallback = callback;
    }

    @Override
    public void onHideCustomView() {
        if (mCustomView == null || mCustomViewCallback == null || mCurrentView == null) {
            return;
        }
        Log.d(Constants.TAG, "onHideCustomView");
        mCurrentView.setVisibility(View.VISIBLE);
        try {
            mCustomView.setKeepScreenOn(false);
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "WebView is not allowed to keep the screen on");
        }
        setFullscreen(mPreferences.getHideStatusBarEnabled(), false);
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        if (decor != null) {
            decor.removeView(mFullscreenContainer);
        }

        if (API < Build.VERSION_CODES.KITKAT) {
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Throwable ignored) {

            }
        }
        mFullscreenContainer = null;
        mCustomView = null;
        if (mVideoView != null) {
            mVideoView.setOnErrorListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView = null;
        }
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
     * This interface method is used by the LightningView to obtain an
     * image that is displayed as a placeholder on a video until the video
     * has initialized and can begin loading.
     *
     * @return a Bitmap that can be used as a place holder for videos.
     */
    @Override
    public Bitmap getDefaultVideoPoster() {
        return BitmapFactory.decodeResource(getResources(), android.R.drawable.spinner_background);
    }

    /**
     * An interface method so that we can inflate a view to send to
     * a LightningView when it needs to display a video and has to
     * show a loading dialog. Inflates a progress view and returns it.
     *
     * @return A view that should be used to display the state
     * of a video's loading progress.
     */
    @Override
    public View getVideoLoadingProgressView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        return inflater.inflate(R.layout.video_loading_progress, null);
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
    public void onCreateWindow(Message resultMsg) {
        if (resultMsg == null) {
            return;
        }
        if (newTab("", true)) {
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebView);
            resultMsg.sendToTarget();
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
        deleteTab(mWebViewList.indexOf(view));
    }

    /**
     * Hide the ActionBar using an animation if we are in full-screen
     * mode. This method also re-parents the ActionBar if its parent is
     * incorrect so that the animation can happen correctly.
     */
    @Override
    public void hideActionBar() {
        if (mFullScreen) {
            if (mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
                mUiLayout.removeView(mToolbarLayout);
                mBrowserFrame.addView(mToolbarLayout);
                mToolbarLayout.bringToFront();
                Log.d(Constants.TAG, "Move view to browser frame");
                mToolbarLayout.setTranslationY(0);
                mWebView.setTranslationY(mToolbarLayout.getHeight());
            }
            if (mToolbarLayout == null || mCurrentView == null)
                return;

            final int height = mToolbarLayout.getHeight();
            final WebView view = mWebView;
            if (mToolbarLayout.getTranslationY() > -0.01f) {
                Animation show = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float trans = (1.0f - interpolatedTime) * height;
                        mToolbarLayout.setTranslationY(trans - height);
                        if (view != null)
                            view.setTranslationY(trans);
                    }
                };
                show.setDuration(250);
                show.setInterpolator(new DecelerateInterpolator());
                mWebView.startAnimation(show);
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

            if (mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
                mUiLayout.removeView(mToolbarLayout);
                mBrowserFrame.addView(mToolbarLayout);
                mToolbarLayout.bringToFront();
                Log.d(Constants.TAG, "Move view to browser frame");
                mToolbarLayout.setTranslationY(0);
                mWebView.setTranslationY(height);
            }
            if (mCurrentView == null)
                return;

            final WebView view = mWebView;
            final int totalHeight = height;
            if (mToolbarLayout.getTranslationY() < -(height - 0.01f)) {
                Animation show = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        float trans = interpolatedTime * totalHeight;
                        mToolbarLayout.setTranslationY(trans - totalHeight);
                        // null pointer here on close
                        if (view != null)
                            view.setTranslationY(trans);
                    }
                };
                show.setDuration(250);
                show.setInterpolator(new DecelerateInterpolator());
                mWebView.startAnimation(show);
            }
        }
    }

    /**
     * Handle a long-press on the current web page. The WebView provides
     * a URL to this method that it thinks is what the user has long-pressed.
     * This may be different than what the user actually long-pressed, e.g. in
     * the case of an image link you may want to provide options for both the
     * image and the link. This method also handles the event that the WebView
     * was not able to get a URL from the event in which case we need to drill
     * down using {@link HitTestResult} and obtain the URL that is at the root
     * of their press. This method is responsible with delegating the dialog
     * creation to one of the several other methods in this class.
     *
     * @param url the URL that was retrieved from the WebView in
     *            the long-press event.
     */
    @Override
    public void longClickPage(final String url) {
        HitTestResult result = null;
        String currentUrl = null;
        if (mWebView != null) {
            result = mWebView.getHitTestResult();
            currentUrl = mWebView.getUrl();
        }
        if (currentUrl != null && currentUrl.startsWith(Constants.FILE)) {
            if (currentUrl.endsWith(HistoryPage.FILENAME)) {
                if (url != null) {
                    longPressHistoryLink(url);
                } else if (result != null && result.getExtra() != null) {
                    final String newUrl = result.getExtra();
                    longPressHistoryLink(newUrl);
                }
            } else if (currentUrl.endsWith(Constants.BOOKMARKS_FILENAME)) {
                if (url != null) {
                    mBookmarksDialogBuilder.showLongPressedDialogForUrl(this, url);
                } else if (result != null && result.getExtra() != null) {
                    final String newUrl = result.getExtra();
                    mBookmarksDialogBuilder.showLongPressedDialogForUrl(this, newUrl);
                }
            }
        } else {
            if (url != null) {
                if (result != null) {
                    if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.getType() == HitTestResult.IMAGE_TYPE) {
                        longPressImage(url);
                    } else {
                        longPressLink(url);
                    }
                } else {
                    longPressLink(url);
                }
            } else if (result != null && result.getExtra() != null) {
                final String newUrl = result.getExtra();
                if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE || result.getType() == HitTestResult.IMAGE_TYPE) {
                    longPressImage(newUrl);
                } else {
                    longPressLink(newUrl);
                }
            }
        }
    }

    /**
     * Handle the event that the user has long-pressed an item on the {@link HistoryPage}.
     * In this case we wish to present the user with a dialog with a few options:
     * open the history item in a new tab, delete the history item, or open the
     * item in the current tab.
     *
     * @param url the URL of the history item.
     */
    private void longPressHistoryLink(final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        newTab(url, false);
                        mDrawerLayout.closeDrawers();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        mHistoryDatabase.deleteHistoryItem(url);
                        openHistory();
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        if (mCurrentView != null) {
                            loadUrlInCurrentView(url);
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_history)
                .setMessage(R.string.dialog_history_long_press)
                .setCancelable(true)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .setNeutralButton(R.string.action_open, dialogClickListener)
                .show();
    }

    /**
     * Handle the event that the user has long-pressed on an image link
     * and provide them with various options for things to do with that image.
     * Options include opening the image in a new tab, loading the image in the
     * current tab, or downloading the image.
     *
     * @param url the URL of the image that was retrieved from long-press.
     */
    private void longPressImage(final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        newTab(url, false);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        loadUrlInCurrentView(url);
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        Utils.downloadFile(BrowserActivity.this, url,
                                mCurrentView.getUserAgent(), "attachment");
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(url.replace(Constants.HTTP, ""))
                .setCancelable(true)
                .setMessage(R.string.dialog_image)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_open, dialogClickListener)
                .setNeutralButton(R.string.action_download, dialogClickListener)
                .show();
    }

    /**
     * Handle the event that the user has long-pressed a link on a web page.
     * It creates a dialog with a few options on what the user can do with the
     * URL that has been retrieved from their long-press, namely, create a
     * new tab, load the URL in the current tab, or copy the link.
     *
     * @param url the URL that has been retrieved in the long-press event
     */
    private void longPressLink(final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        newTab(url, false);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        loadUrlInCurrentView(url);
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("label", url);
                        clipboard.setPrimaryClip(clip);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this); // dialog
        builder.setTitle(url)
                .setCancelable(true)
                .setMessage(R.string.dialog_link)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_open, dialogClickListener)
                .setNeutralButton(R.string.action_copy, dialogClickListener)
                .show();
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
        if (mCurrentView != null) {
            if (mCurrentView.getProgress() < 100) {
                mCurrentView.stopLoading();
            } else {
                mCurrentView.reload();
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
        switch (v.getId()) {
            case R.id.action_back:
                if (mCurrentView != null) {
                    if (mCurrentView.canGoBack()) {
                        mCurrentView.goBack();
                    } else {
                        deleteTab(mWebViewList.indexOf(mCurrentView));
                    }
                }
                break;
            case R.id.action_forward:
                if (mCurrentView != null) {
                    if (mCurrentView.canGoForward()) {
                        mCurrentView.goForward();
                    }
                }
                break;
            case R.id.arrow_button:
                if (mSearch != null && mSearch.hasFocus()) {
                    mCurrentView.requestFocus();
                } else if (mShowTabsInDrawer) {
                    mDrawerLayout.openDrawer(mDrawerLeft);
                } else if (mCurrentView != null) {
                    mCurrentView.loadHomepage();
                }
                break;
            case R.id.new_tab_button:
                newTab(null, true);
                break;
            case R.id.action_home:
                mCurrentView.loadHomepage();
                closeDrawers();
                break;
            case R.id.button_next:
                mWebView.findNext(false);
                break;
            case R.id.button_back:
                mWebView.findNext(true);
                break;
            case R.id.button_quit:
                mWebView.clearMatches();
                mSearchBar.setVisibility(View.GONE);
                break;
            case R.id.action_reading:
                Intent read = new Intent(this, ReadingActivity.class);
                read.putExtra(Constants.LOAD_READING_URL, mCurrentView.getUrl());
                startActivity(read);
                break;
            case R.id.action_toggle_desktop:
                mCurrentView.toggleDesktopUA(this);
                mCurrentView.reload();
                closeDrawers();
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
        switch (view.getId()) {
            case R.id.new_tab_button:
                String url = mPreferences.getSavedUrl();
                if (url != null) {
                    newTab(url, true);
                    Utils.showSnackbar(this, R.string.deleted_tab);
                }
                mPreferences.setSavedUrl(null);
                break;
        }
        return true;
    }

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
        View frameButton = findViewById(buttonId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        ImageView buttonImage = (ImageView) findViewById(imageId);
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
            for (int n = 0, size = mWebViewList.size(); n < size; n++) {
                WebView view = mWebViewList.get(n).getWebView();
                if (view != null)
                    view.setNetworkAvailable(isConnected);
            }
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
        PermissionsManager.getInstance().notifyPermissionsChange(permissions);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final Object mBusEventListener = new Object() {

        /**
         * Load the given bookmark in the current tab, used by the the
         * {@link acr.browser.lightning.fragment.BookmarksFragment}
         *
         * @param event   Bus event indicating that the user has clicked a bookmark
         */
        @Subscribe
        public void loadBookmarkInCurrentTab(final BookmarkEvents.Clicked event) {
            loadUrlInCurrentView(event.bookmark.getUrl());
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

        /**
         * Load the given bookmark in a new tab, used by the the
         * {@link acr.browser.lightning.fragment.BookmarksFragment}
         *
         * @param event   Bus event indicating that the user wishes
         *                to open a bookmark in a new tab
         */
        @Subscribe
        public void loadBookmarkInNewTab(final BookmarkEvents.AsNewTab event) {
            newTab(event.bookmark.getUrl(), true);
            mDrawerLayout.closeDrawers();
        }

        /**
         * When receive a {@link acr.browser.lightning.bus.BookmarkEvents.WantToBookmarkCurrentPage}
         * message this receiver answer firing the
         * {@link acr.browser.lightning.bus.BrowserEvents.AddBookmark} message
         *
         * @param event an event that the user wishes to bookmark the current page
         */
        @Subscribe
        public void bookmarkCurrentPage(final BookmarkEvents.WantToBookmarkCurrentPage event) {
            if (mCurrentView != null) {
                mEventBus.post(new BrowserEvents.AddBookmark(mCurrentView.getTitle(), mCurrentView.getUrl()));
            }
        }

        /**
         * This message is received when a bookmark was added by the
         * {@link acr.browser.lightning.fragment.BookmarksFragment}
         *
         * @param event the event that a bookmark has been added
         */
        @Subscribe
        public void bookmarkAdded(final BookmarkEvents.Added event) {
            mSearchAdapter.refreshBookmarks();
        }

        /**
         * This method is called when the user edits a bookmark.
         *
         * @param event the event that the bookmark has changed.
         */
        @Subscribe
        public void bookmarkChanged(final BookmarkEvents.BookmarkChanged event) {
            if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                    && mCurrentView.getUrl().endsWith(Constants.BOOKMARKS_FILENAME)) {
                openBookmarkPage(mWebView);
            }
            if (mCurrentView != null) {
                mEventBus.post(new BrowserEvents.CurrentPageUrl(mCurrentView.getUrl()));
            }
        }

        /**
         * Notify the browser that a bookmark was deleted.
         *
         * @param event the event that the bookmark has been deleted
         */
        @Subscribe
        public void bookmarkDeleted(final BookmarkEvents.Deleted event) {
            if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                    && mCurrentView.getUrl().endsWith(Constants.BOOKMARKS_FILENAME)) {
                openBookmarkPage(mWebView);
            }
            if (mCurrentView != null) {
                mEventBus.post(new BrowserEvents.CurrentPageUrl(mCurrentView.getUrl()));
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
    };
}
