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
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
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
import java.io.IOException;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.bus.NavigationEvents;
import acr.browser.lightning.bus.TabEvents;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.controller.BrowserController;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.fragment.TabsFragment;
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

public abstract class BrowserActivity extends ThemableBrowserActivity implements BrowserController, OnClickListener, OnLongClickListener {

    // Layout
    private DrawerLayout mDrawerLayout;
    private FrameLayout mBrowserFrame;
    private FullscreenHolder mFullscreenContainer;
    private ViewGroup mDrawerLeft, mDrawerRight, mUiLayout, mToolbarLayout;
    private RelativeLayout mSearchBar;

    // Views
    private AnimatedProgressBar mProgressBar;
    private AutoCompleteTextView mSearch;
    private ImageView mArrowImage;
    private VideoView mVideoView;
    private View mCustomView;

    // Adapter
    private SearchAdapter mSearchAdapter;

    // Callback
    private CustomViewCallback mCustomViewCallback;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Context
    private Activity mActivity;

    // Primatives
    private boolean mFullScreen, mColorMode, mDarkTheme,
            mIsNewIntent = false,
            mIsFullScreen = false,
            mIsImmersive = false,
            mShowTabsInDrawer;
    private int mOriginalOrientation, mBackgroundColor, mIdGenerator, mIconColor,
            mCurrentUiColor = Color.BLACK;
    private String mSearchText, mUntitledTitle, mHomepage, mCameraPhotoPath;

    // The singleton BookmarkManager
    @Inject
    BookmarkManager bookmarkManager;

    // Event bus
    @Inject
    Bus eventBus;

    @Inject
    BookmarkPage bookmarkPage;

    @Inject
    LightningDialogBuilder bookmarksDialogBuilder;

    @Inject
    TabsManager tabsManager;

    @Inject
    PreferenceManager mPreferences;

    @Inject
    HistoryDatabase mHistoryDatabase;

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

//    abstract void initializeTabs();

    abstract void closeActivity();

    public abstract void updateHistory(final String title, final String url);

    abstract void updateCookiePreference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        initialize();
    }

    private synchronized void initialize() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        //TODO make sure dark theme flag gets set correctly
        mDarkTheme = mPreferences.getUseTheme() != 0 || isIncognito();
        mIconColor = mDarkTheme ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        mShowTabsInDrawer = mPreferences.getShowTabsInDrawer(!isTablet());

        mActivity = this;

        mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
        mToolbarLayout = (LinearLayout) findViewById(R.id.toolbar_layout);
        // initialize background ColorDrawable
        mBackground.setColor(((ColorDrawable) mToolbarLayout.getBackground()).getColor());

        mUiLayout = (LinearLayout) findViewById(R.id.ui_layout);
        mProgressBar = (AnimatedProgressBar) findViewById(R.id.progress_view);
        mDrawerLeft = (FrameLayout) findViewById(R.id.left_drawer);
        // Drawer stutters otherwise
        mDrawerLeft.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerRight = (ViewGroup) findViewById(R.id.right_drawer);
        mDrawerRight.setLayerType(View.LAYER_TYPE_HARDWARE, null);
// TODO Probably should be moved to the TabsFragement
//        ImageView tabTitleImage = (ImageView) findViewById(R.id.plusIcon);
//        tabTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mShowTabsInDrawer) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        setNavigationDrawerWidth();
        mDrawerLayout.setDrawerListener(new DrawerLocker());

        mWebpageBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_webpage, mDarkTheme);

        mHomepage = mPreferences.getHomepage();

        final TabsFragment tabsFragment = new TabsFragment();
        final int containerId = mShowTabsInDrawer ? R.id.left_drawer : R.id.tabs_toolbar_container;
        final Bundle arguments = new Bundle();
        arguments.putBoolean(TabsFragment.VERTICAL_MODE, mShowTabsInDrawer);
        arguments.putBoolean(TabsFragment.USE_DARK_THEME, mDarkTheme);
        tabsFragment.setArguments(arguments);
        getSupportFragmentManager()
                .beginTransaction()
                .add(containerId, tabsFragment)
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

        View v = actionBar.getCustomView();
        LayoutParams lp = v.getLayoutParams();
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.MATCH_PARENT;
        v.setLayoutParams(lp);

        mArrowImage = (ImageView) actionBar.getCustomView().findViewById(R.id.arrow);
        FrameLayout arrowButton = (FrameLayout) actionBar.getCustomView().findViewById(
                R.id.arrow_button);
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

        setupFrameLayoutButton(R.id.action_reading, R.id.icon_reading);
        setupFrameLayoutButton(R.id.action_toggle_desktop, R.id.icon_desktop);

        // create the search EditText in the ToolBar
        mSearch = (AutoCompleteTextView) actionBar.getCustomView().findViewById(R.id.search);
        mUntitledTitle = getString(R.string.untitled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBackgroundColor = getColor(R.color.primary_color);
        } else {
            mBackgroundColor = getResources().getColor(R.color.primary_color);
        }
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
            WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        }

        tabsManager.restoreTabs(this, mDarkTheme, isIncognito());
        // At this point we always have at least a tab in the tab manager
        showTab(0);

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
                    final LightningView currentView = tabsManager.getCurrentTab();
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
                final LightningView currentView = tabsManager.getCurrentTab();
                if (currentView != null) {
                    currentView.requestFocus();
                }
                return true;
            }
            return false;
        }

        @Override
        public void onFocusChange(View v, final boolean hasFocus) {
            final LightningView currentView = tabsManager.getCurrentTab();
            if (!hasFocus && currentView != null) {
                if (currentView.getProgress() < 100) {
                    setIsLoading();
                } else {
                    setIsFinishedLoading();
                }
                updateUrl(currentView.getUrl(), true);
            } else if (hasFocus) {
                String url = currentView.getUrl();
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

    private void initializePreferences() {
        final LightningView currentView = tabsManager.getCurrentTab();
        final WebView currentWebView = currentView.getWebView();
        mFullScreen = mPreferences.getFullScreenEnabled();
        mColorMode = mPreferences.getColorModeEnabled();
        mColorMode &= !mDarkTheme;
        if (!isIncognito() && !mColorMode && !mDarkTheme && mWebpageBitmap != null) {
            changeToolbarBackground(mWebpageBitmap, null);
        } else if (!isIncognito() && currentView != null && !mDarkTheme
                && currentView.getFavicon() != null) {
            changeToolbarBackground(currentView.getFavicon(), null);
        }

        if (mFullScreen) {
            mToolbarLayout.setTranslationY(0);
            int height = mToolbarLayout.getHeight();
            if (height == 0) {
                mToolbarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                height = mToolbarLayout.getMeasuredHeight();
            }
            if (currentWebView != null)
                currentWebView.setTranslationY(height);
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
            if (currentWebView != null)
                currentWebView.setTranslationY(0);
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
        final LightningView currentView = tabsManager.getCurrentTab();
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
            case R.id.action_new_tab:
                newTab(null, true);
                return true;
            case R.id.action_incognito:
                startActivity(new Intent(this, IncognitoActivity.class));
                overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out_scale);
                return true;
            case R.id.action_share:
                if (currentView != null && !currentView.getUrl().startsWith(Constants.FILE)) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentView.getTitle());
                    shareIntent.putExtra(Intent.EXTRA_TEXT, currentView.getUrl());
                    startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.dialog_title_share)));
                }
                return true;
            case R.id.action_bookmarks:
                openBookmarks();
                return true;
            case R.id.action_copy:
                if (currentView != null && !currentView.getUrl().startsWith(Constants.FILE)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", currentView.getUrl());
                    clipboard.setPrimaryClip(clip);
                    Utils.showSnackbar(mActivity, R.string.message_link_copied);
                }
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_history:
                openHistory();
                return true;
            case R.id.action_add_bookmark:
                if (currentView != null && !currentView.getUrl().startsWith(Constants.FILE)) {
                    eventBus.post(new BrowserEvents.AddBookmark(currentView.getTitle(),
                            currentView.getUrl()));
                }
                return true;
            case R.id.action_find:
                findInPage();
                return true;
            case R.id.action_reading_mode:
                Intent read = new Intent(this, ReadingActivity.class);
                read.putExtra(Constants.LOAD_READING_URL, currentView.getUrl());
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
        final AlertDialog.Builder finder = new AlertDialog.Builder(mActivity);
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
        final LightningView currentView = tabsManager.getCurrentTab();
        if (currentView != null) {
            currentView.find(text);
        }
        mSearchBar = (RelativeLayout) findViewById(R.id.search_bar);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line);
        adapter.add(mActivity.getString(R.string.close_tab));
        adapter.add(mActivity.getString(R.string.close_all_tabs));
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
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     *
     * @param position  the poition of the tab to display
     */
    private synchronized void showTab(final int position) {
        final LightningView currentView = tabsManager.getCurrentTab();
        final WebView currentWebView = currentView != null ? currentView.getWebView() : null;
        final LightningView newView = tabsManager.switchToTab(position);
        final WebView newWebView = newView != null ? newView.getWebView() : null;

        // Set the background color so the color mode color doesn't show through
        mBrowserFrame.setBackgroundColor(mBackgroundColor);
        if (newView == null || currentView == newView) {
            return;
        }
        mIsNewIntent = false;

        final float translation = mToolbarLayout.getTranslationY();
        mBrowserFrame.removeAllViews();
        if (currentView != null) {
            currentView.setForegroundTab(false);
            currentView.onPause();
        }
        newView.setForegroundTab(true);
        if (currentWebView != null) {
            updateUrl(newView.getUrl(), true);
            updateProgress(newView.getProgress());
        } else {
            updateUrl("", true);
            updateProgress(0);
        }

        mBrowserFrame.addView(newWebView, MATCH_PARENT);
        newView.requestFocus();
        newView.onResume();

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
            newWebView.setTranslationY(translation + height);
            mToolbarLayout.setTranslationY(translation);
        } else {
            newWebView.setTranslationY(0);
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
        eventBus.post(new BrowserEvents.CurrentPageUrl(newView.getUrl()));

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
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (currentTab == null) {
            // This is a problem, probably an assert will be better than a return
            return;
        }

        currentTab.loadUrl(url);
        eventBus.post(new BrowserEvents.CurrentPageUrl(url));
    }

    @Override
    public void closeEmptyTab() {
        final WebView currentWebView = tabsManager.getCurrentWebView();
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
            tabsManager.freeMemory();
        }
    }

    synchronized boolean newTab(String url, boolean show) {
        // Limit number of tabs for limited version of app
        if (!Constants.FULL_VERSION && tabsManager.size() >= 10) {
            Utils.showSnackbar(this, R.string.max_tabs);
            return false;
        }
        mIsNewIntent = false;
        LightningView startingTab = tabsManager.newTab(this, url, mDarkTheme, isIncognito());
        if (mIdGenerator == 0) {
            startingTab.resumeTimers();
        }
        mIdGenerator++;

        if (show) {
            showTab(tabsManager.size() - 1);
        }
        // TODO Check is this is callable directly from LightningView
        eventBus.post(new BrowserEvents.TabsChanged());

//      TODO Restore this
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mDrawerListLeft.smoothScrollToPosition(tabsManager.size() - 1);
//            }
//        }, 300);

        return true;
    }

    private synchronized void deleteTab(int position) {
        final LightningView tabToDelete = tabsManager.getTabAtPosition(position);
        final LightningView currentTab = tabsManager.getCurrentTab();

        if (tabToDelete == null) {
            return;
        }

//  What?
        int current = tabsManager.getPositionForTab(currentTab);
        if (current < 0) {
            return;
        }
        if (!tabToDelete.getUrl().startsWith(Constants.FILE) && !isIncognito()) {
            mPreferences.setSavedUrl(tabToDelete.getUrl());
        }
        final boolean isShown = tabToDelete.isShown();
        if (isShown) {
            mBrowserFrame.setBackgroundColor(mBackgroundColor);
        }
        if (current > position) {
            tabsManager.deleteTab(position);
            showTab(current - 1);
            eventBus.post(new BrowserEvents.TabsChanged());
            tabToDelete.onDestroy();
        } else if (tabsManager.size() > position + 1) {
            if (current == position) {
                showTab(position + 1);
                tabsManager.deleteTab(position);
                showTab(position);
                eventBus.post(new BrowserEvents.TabsChanged());
            } else {
                tabsManager.deleteTab(position);
            }

            tabToDelete.onDestroy();
        } else if (tabsManager.size() > 1) {
            if (current == position) {
                showTab(position - 1);
                tabsManager.deleteTab(position);
                showTab(position - 1);
                eventBus.post(new BrowserEvents.TabsChanged());
            } else {
                tabsManager.deleteTab(position);
            }

            tabToDelete.onDestroy();
        } else {
            if (currentTab.getUrl().startsWith(Constants.FILE) || currentTab.getUrl().equals(mHomepage)) {
                closeActivity();
            } else {
                tabsManager.deleteTab(position);
                performExitCleanUp();
                tabToDelete.pauseTimers();
                tabToDelete.onDestroy();
                eventBus.post(new BrowserEvents.TabsChanged());
                finish();
            }
        }
        eventBus.post(new BrowserEvents.TabsChanged());

        if (mIsNewIntent && isShown) {
            mIsNewIntent = false;
            closeActivity();
        }

        Log.d(Constants.TAG, "deleted tab");
    }

    private void performExitCleanUp() {
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (mPreferences.getClearCacheExit() && currentTab != null && !isIncognito()) {
            WebUtils.clearCache(currentTab.getWebView());
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
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showCloseDialog(tabsManager.positionOf(currentTab));
        }
        return true;
    }

    private void closeBrowser() {
        mBrowserFrame.setBackgroundColor(mBackgroundColor);
        performExitCleanUp();
        tabsManager.shutdown();
        eventBus.post(new BrowserEvents.TabsChanged());
        finish();
    }

    @Override
    public void onBackPressed() {
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawer(mDrawerLeft);
        } else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
            eventBus
                    .post(new BrowserEvents.UserPressedBack());
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
                    deleteTab(tabsManager.positionOf(currentTab));
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
        final LightningView currentTab = tabsManager.getCurrentTab();
        Log.d(Constants.TAG, "onPause");
        if (currentTab != null) {
            currentTab.pauseTimers();
            currentTab.onPause();
        }
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        if (isIncognito() && isFinishing()) {
            overridePendingTransition(R.anim.fade_in_scale, R.anim.slide_down_out);
        }

        eventBus.unregister(busEventListener);
    }

    void saveOpenTabs() {
        if (mPreferences.getRestoreLostTabsEnabled()) {
            final String s = tabsManager.tabsString();
            mPreferences.setMemoryUrl(s);
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
        final LightningView currentTab = tabsManager.getCurrentTab();
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
        tabsManager.resume(this);

        supportInvalidateOptionsMenu();

        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_BROADCAST_ACTION);
        registerReceiver(mNetworkReceiver, filter);

        eventBus.register(busEventListener);
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private void searchTheWeb(@NonNull String query) {
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (query.isEmpty()) {
            return;
        }
        String searchUrl = mSearchText + UrlUtils.QUERY_PLACE_HOLDER;
        query = query.trim();
        currentTab.stopLoading();
        if (currentTab != null) {
            loadUrlInCurrentView(UrlUtils.smartUrlFilter(query, true, searchUrl));
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
        final int defaultColor;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            defaultColor = getResources().getColor(R.color.primary_color);
        } else {
            defaultColor = getColor(R.color.primary_color);
        }
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
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (url == null || mSearch == null || mSearch.hasFocus()) {
            return;
        }
        eventBus.post(new BrowserEvents.CurrentPageUrl(url));
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
                    if (currentTab != null && !currentTab.getTitle().isEmpty()) {
                        mSearch.setText(currentTab.getTitle());
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
        if (n >= 100) {
            setIsFinishedLoading();
        } else {
            setIsLoading();
        }
        mProgressBar.setProgress(n);
    }

    void addItemToHistory(final String title, final String url) {
        Runnable update = new Runnable() {
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
        final LightningView currentTab = tabsManager.getCurrentTab();
        getUrl.setThreshold(1);
        getUrl.setDropDownWidth(-1);
        getUrl.setDropDownAnchor(R.id.toolbar_layout);
        getUrl.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                try {
                    String url;
                    url = ((TextView) arg1.findViewById(R.id.url)).getText().toString();
                    if (url.startsWith(mActivity.getString(R.string.suggestion))) {
                        url = ((TextView) arg1.findViewById(R.id.title)).getText().toString();
                    } else {
                        getUrl.setText(url);
                    }
                    searchTheWeb(url);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
                    if (currentTab != null) {
                        currentTab.requestFocus();
                    }
                } catch (NullPointerException e) {
                    Log.e("Browser Error: ", "NullPointerException on item click");
                }
            }

        });

        getUrl.setSelectAllOnFocus(true);
        mSearchAdapter = new SearchAdapter(mActivity, mDarkTheme, isIncognito());
        getUrl.setAdapter(mSearchAdapter);
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private void openHistory() {
        // use a thread so that history retrieval doesn't block the UI
        Thread history = new Thread(new Runnable() {

            @Override
            public void run() {
                loadUrlInCurrentView(HistoryPage.getHistoryPage(mActivity));
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
        if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
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
        contentSelectionIntent.setType("image/*");

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

        mActivity.startActivityForResult(chooserIntent, 1);
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        final LightningView currentTab = tabsManager.getCurrentTab();
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
        mFullscreenContainer = new FullscreenHolder(this);
        mCustomView = view;
        mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        setFullscreen(true, true);
        currentTab.setVisibility(View.GONE);
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
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (mCustomView == null || mCustomViewCallback == null || currentTab == null) {
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
     * turns on fullscreen mode in the app
     *
     * @param enabled whether to enable fullscreen or not
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
     * a class extending FramLayout used to display fullscreen videos
     */
    private class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setBackgroundColor(ctx.getColor(android.R.color.black));
            } else {
                setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
            }
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent evt) {
            return true;
        }

    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return BitmapFactory.decodeResource(getResources(), android.R.drawable.spinner_background);
    }

    @Override
    public View getVideoLoadingProgressView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        return inflater.inflate(R.layout.video_loading_progress, null);
    }

    /**
     * handles javascript requests to create a new window in the browser
     */
    @Override
    public void onCreateWindow(Message resultMsg) {
        if (resultMsg == null) {
            return;
        }
        if (newTab("", true)) {
            // TODO Review this
            final WebView webView = tabsManager.getTabAtPosition(tabsManager.size() - 1).getWebView();
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(webView);
            resultMsg.sendToTarget();
        }
    }

    /**
     * Closes the specified view, implementing the JavaScript callback to close a window
     *
     * @param view the LightningView to close
     */
    @Override
    public void onCloseWindow(LightningView view) {
        deleteTab(tabsManager.positionOf(view));
    }

    /**
     * returns the Activity instance for this activity,
     * very helpful when creating things in other classes... I think
     */
    @Override
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * it hides the action bar, seriously what else were you expecting
     */
    @Override
    public void hideActionBar() {
        final LightningView currentTab = tabsManager.getCurrentTab();
        final WebView currentWebView = currentTab.getWebView();
        if (mFullScreen) {
            if (mBrowserFrame.findViewById(R.id.toolbar_layout) == null) {
                mUiLayout.removeView(mToolbarLayout);
                mBrowserFrame.addView(mToolbarLayout);
                mToolbarLayout.bringToFront();
                Log.d(Constants.TAG, "Move view to browser frame");
                mToolbarLayout.setTranslationY(0);
                currentWebView.setTranslationY(mToolbarLayout.getHeight());
            }
            if (mToolbarLayout == null || currentTab == null)
                return;

            final int height = mToolbarLayout.getHeight();
            final WebView view = currentWebView;
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
//                show.setFillAfter(true);
                currentWebView.startAnimation(show);
            }
        }
    }

    /**
     * obviously it shows the action bar if it's hidden
     */
    @Override
    public void showActionBar() {
        if (mFullScreen) {
            final WebView view = tabsManager.getCurrentWebView();

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
                if (view != null) {
                    view.setTranslationY(height);
                }
            }
            final LightningView currentTab = tabsManager.getCurrentTab();
            if (currentTab == null)
                return;

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
//                show.setFillAfter(true);
                if (view != null) {
                    view.startAnimation(show);
                }
            }
        }
    }

    /**
     * This method lets the search bar know that the page is currently loading
     * and that it should display the stop icon to indicate to the user that
     * pressing it stops the page from loading
     */
    private void setIsLoading() {
        if (!mSearch.hasFocus()) {
            mIcon = mDeleteIcon;
            mSearch.setCompoundDrawables(null, null, mDeleteIcon, null);
        }
    }

    /**
     * This tells the search bar that the page is finished loading and it should
     * display the refresh icon
     */
    private void setIsFinishedLoading() {
        if (!mSearch.hasFocus()) {
            mIcon = mRefreshIcon;
            mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
        }
    }

    /**
     * handle presses on the refresh icon in the search bar, if the page is
     * loading, stop the page, if it is done loading refresh the page.
     * <p/>
     * See setIsFinishedLoading and setIsLoading for displaying the correct icon
     */
    private void refreshOrStop() {
        final LightningView currentTab = tabsManager.getCurrentTab();
        if (currentTab != null) {
            if (currentTab.getProgress() < 100) {
                currentTab.stopLoading();
            } else {
                currentTab.reload();
            }
        }
    }

    @Override
    public void onClick(View v) {
        final LightningView currentTab = tabsManager.getCurrentTab();
        final WebView currentWebView = currentTab.getWebView();
        switch (v.getId()) {
            case R.id.arrow_button:
                if (mSearch != null && mSearch.hasFocus()) {
                    currentTab.requestFocus();
                } else if (mShowTabsInDrawer) {
                    mDrawerLayout.openDrawer(mDrawerLeft);
                } else if (currentTab != null) {
                    currentTab.loadHomepage();
                }
                break;
            case R.id.button_next:
                currentWebView.findNext(false);
                break;
            case R.id.button_back:
                currentWebView.findNext(true);
                break;
            case R.id.button_quit:
                currentWebView.clearMatches();
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
                closeDrawers();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        return true;
    }

    // TODO Check if all the calls are relative to TabsFragement
    private void setupFrameLayoutButton(@IdRes int buttonId, @IdRes int imageId) {
        final View frameButton = findViewById(buttonId);
        final ImageView buttonImage = (ImageView) findViewById(imageId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    private final NetworkReceiver mNetworkReceiver = new NetworkReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            boolean isConnected = isConnected(context);
            Log.d(Constants.TAG, "Network Connected: " + String.valueOf(isConnected));
            tabsManager.notifyConnectioneStatus(isConnected);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private final Object busEventListener = new Object() {
        /**
         * Load the given url in the current tab, used by the the
         * {@link acr.browser.lightning.fragment.BookmarksFragment} and by the
         * {@link LightningDialogBuilder}
         * @param event   The event as it comes from the bus
         */
        @Subscribe
        public void loadUrlInCurrentTab(final BrowserEvents.OpenUrlInCurrentTab event) {
            loadUrlInCurrentView(event.url);
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
         * Load the given url in a new tab, used by the the
         * {@link acr.browser.lightning.fragment.BookmarksFragment} and by the
         * {@link LightningDialogBuilder}
         * @param event   The event as it comes from the bus
         */
        @Subscribe
        public void loadUrlInNewTab(final BrowserEvents.OpenUrlInNewTab event) {
            BrowserActivity.this.newTab(event.url, true);
            mDrawerLayout.closeDrawers();
        }

        /**
         * When receive a {@link acr.browser.lightning.bus.BookmarkEvents.WantToBookmarkCurrentPage}
         * message this receiver answer firing the
         * {@link acr.browser.lightning.bus.BrowserEvents.AddBookmark} message
         *
         * @param event basically a marker
         */
        @Subscribe
        public void bookmarkCurrentPage(final BookmarkEvents.WantToBookmarkCurrentPage event) {
            final LightningView currentTab = tabsManager.getCurrentTab();
            if (currentTab != null) {
                eventBus.post(new BrowserEvents.AddBookmark(currentTab.getTitle(), currentTab.getUrl()));
            }
        }

        /**
         * This message is received when a bookmark was added by the
         * {@link acr.browser.lightning.fragment.BookmarksFragment}
         *
         * @param event a marker
         */
        @Subscribe
        public void bookmarkAdded(final BookmarkEvents.Added event) {
            mSearchAdapter.refreshBookmarks();
        }

        /**
         * This is received when the user edit a bookmark
         *
         * @param event the event that the bookmark has changed
         */
        @Subscribe
        public void bookmarkChanged(final BookmarkEvents.BookmarkChanged event) {
            final LightningView currentTab = tabsManager.getCurrentTab();
            final WebView currentWebView = currentTab.getWebView();
            if (currentTab != null && currentTab.getUrl().startsWith(Constants.FILE)
                    && currentTab.getUrl().endsWith(Constants.BOOKMARKS_FILENAME)) {
                currentTab.loadBookmarkpage();
            }
            if (currentTab != null) {
                eventBus.post(new BrowserEvents.CurrentPageUrl(currentTab.getUrl()));
            }
        }

        /**
         * Notify the browser that a bookmark was deleted
         *
         * @param event the event that the bookmark has been deleted
         */
        @Subscribe
        public void bookmarkDeleted(final BookmarkEvents.Deleted event) {
            final LightningView currentTab = tabsManager.getCurrentTab();
            final WebView currentWebView = currentTab.getWebView();
            if (currentTab != null && currentTab.getUrl().startsWith(Constants.FILE)
                    && currentTab.getUrl().endsWith(Constants.BOOKMARKS_FILENAME)) {
                currentTab.loadBookmarkpage();
            }
            if (currentTab != null) {
                eventBus.post(new BrowserEvents.CurrentPageUrl(currentTab.getUrl()));
            }
        }

        /**
         * The {@link acr.browser.lightning.fragment.BookmarksFragment} send this message on reply
         * to {@link acr.browser.lightning.bus.BrowserEvents.UserPressedBack} message if the
         * fragement is showing the boomarks root folder.
         *
         * @param event a marker
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
            final LightningView currentTab = tabsManager.getCurrentTab();
            if (currentTab != null) {
                if (currentTab.canGoBack()) {
                    currentTab.goBack();
                } else {
                    deleteTab(tabsManager.positionOf(currentTab));
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
            final LightningView currentTab = tabsManager.getCurrentTab();
            if (currentTab != null) {
                if (currentTab.canGoForward()) {
                    currentTab.goForward();
                }
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

                    Utils.showSnackbar(mActivity, R.string.deleted_tab);
                }
                mPreferences.setSavedUrl(null);
        }

        @Subscribe
        public void displayInSnackbar(final BrowserEvents.ShowSnackBarMessage event) {
            if (event.message != null) {
                Utils.showSnackbar(BrowserActivity.this, event.message);
            } else {
                Utils.showSnackbar(mActivity, event.stringRes);            }
        }
    };
}
