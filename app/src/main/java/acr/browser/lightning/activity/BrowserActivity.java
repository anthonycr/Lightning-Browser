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
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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
import android.view.animation.AccelerateInterpolator;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.VideoView;

import org.lucasr.twowayview.TwoWayView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.controller.BrowserController;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.object.ClickHandler;
import acr.browser.lightning.object.DrawerArrowDrawable;
import acr.browser.lightning.object.SearchAdapter;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.receiver.NetworkReceiver;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;
import acr.browser.lightning.utils.WebUtils;
import acr.browser.lightning.view.AnimatedProgressBar;
import acr.browser.lightning.view.LightningView;

public abstract class BrowserActivity extends ThemableBrowserActivity implements BrowserController, OnClickListener, OnLongClickListener {

    // Layout
    private DrawerLayout mDrawerLayout;
    private FrameLayout mBrowserFrame;
    private FullscreenHolder mFullscreenContainer;
    private ListView mDrawerListRight;
    private TwoWayView mDrawerListLeft;
    private LinearLayout mDrawerLeft, mDrawerRight, mUiLayout, mToolbarLayout;
    private RelativeLayout mSearchBar;

    // List
    private final List<LightningView> mWebViewList = new ArrayList<>();
    private final List<HistoryItem> mBookmarkList = new ArrayList<>();
    private LightningView mCurrentView;
    private WebView mWebView;

    // Views
    private AnimatedProgressBar mProgressBar;
    private AutoCompleteTextView mSearch;
    private ImageView mArrowImage, mBookmarkTitleImage, mBookmarkImage;
    private VideoView mVideoView;
    private View mCustomView, mVideoProgressView;

    // Adapter
    private BookmarkViewAdapter mBookmarkAdapter;
    private LightningViewAdapter mTitleAdapter;
    private SearchAdapter mSearchAdapter;

    // Callback
    private ClickHandler mClickHandler;
    private CustomViewCallback mCustomViewCallback;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Context
    private Activity mActivity;

    // Primatives
    private boolean mFullScreen, mColorMode, mDarkTheme,
            mSystemBrowser = false,
            mIsNewIntent = false,
            mIsFullScreen = false,
            mIsImmersive = false,
            mShowTabsInDrawer;
    private int mOriginalOrientation, mBackgroundColor, mIdGenerator, mIconColor;
    private String mSearchText, mUntitledTitle, mHomepage, mCameraPhotoPath;

    // Storage
    private HistoryDatabase mHistoryDatabase;
    private BookmarkManager mBookmarkManager;
    private PreferenceManager mPreferences;

    // Image
    private Bitmap mDefaultVideoPoster, mWebpageBitmap, mFolderBitmap;
    private final ColorDrawable mBackground = new ColorDrawable();
    private Drawable mDeleteIcon, mRefreshIcon, mCopyIcon, mIcon;
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

    abstract boolean isIncognito();

    abstract void initializeTabs();

    abstract void closeActivity();

    public abstract void updateHistory(final String title, final String url);

    abstract void updateCookiePreference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    private synchronized void initialize() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        mPreferences = PreferenceManager.getInstance();
        //TODO make sure dark theme flag gets set correctly
        mDarkTheme = mPreferences.getUseTheme() != 0 || isIncognito();
        mIconColor = mDarkTheme ? ThemeUtils.getIconDarkThemeColor(this) : ThemeUtils.getIconLightThemeColor(this);
        mShowTabsInDrawer = mPreferences.getShowTabsInDrawer(!isTablet());

        mActivity = this;
        mWebViewList.clear();

        mClickHandler = new ClickHandler(this);
        mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
        mToolbarLayout = (LinearLayout) findViewById(R.id.toolbar_layout);
        // initialize background ColorDrawable
        mBackground.setColor(((ColorDrawable) mToolbarLayout.getBackground()).getColor());

        mUiLayout = (LinearLayout) findViewById(R.id.ui_layout);
        mProgressBar = (AnimatedProgressBar) findViewById(R.id.progress_view);
        setupFrameLayoutButton(R.id.new_tab_button, R.id.icon_plus);
        mDrawerLeft = (LinearLayout) findViewById(R.id.left_drawer);
        // Drawer stutters otherwise
        mDrawerLeft.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerRight = (LinearLayout) findViewById(R.id.right_drawer);
        mDrawerRight.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);
        mBookmarkTitleImage = (ImageView) findViewById(R.id.starIcon);
        mBookmarkTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        ImageView tabTitleImage = (ImageView) findViewById(R.id.plusIcon);
        tabTitleImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !mShowTabsInDrawer) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        setNavigationDrawerWidth();
        mDrawerLayout.setDrawerListener(new DrawerLocker());

        mWebpageBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_webpage, mDarkTheme);
        mFolderBitmap = ThemeUtils.getThemedBitmap(this, R.drawable.ic_folder, mDarkTheme);

        mHomepage = mPreferences.getHomepage();

        TwoWayView horizontalListView = (TwoWayView) findViewById(R.id.twv_list);

        if (mShowTabsInDrawer) {
            mTitleAdapter = new LightningViewAdapter(this, R.layout.tab_list_item, mWebViewList);
            mDrawerListLeft = (TwoWayView) findViewById(R.id.left_drawer_list);
            mDrawerListLeft.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            mToolbarLayout.removeView(horizontalListView);
        } else {
            mTitleAdapter = new LightningViewAdapter(this, R.layout.tab_list_item_horizontal, mWebViewList);
            mDrawerListLeft = horizontalListView;
            mDrawerListLeft.setOverScrollMode(View.OVER_SCROLL_NEVER);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerLeft);
        }

        mDrawerListLeft.setAdapter(mTitleAdapter);
        mDrawerListLeft.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerListLeft.setOnItemLongClickListener(new DrawerItemLongClickListener());

        mDrawerListRight.setOnItemClickListener(new BookmarkItemClickListener());
        mDrawerListRight.setOnItemLongClickListener(new BookmarkItemLongClickListener());

        mHistoryDatabase = HistoryDatabase.getInstance(getApplicationContext());

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

        LinearLayout searchContainer = (LinearLayout) v.findViewById(R.id.search_container);
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) searchContainer.getLayoutParams();
        int leftMargin = !mShowTabsInDrawer ? Utils.dpToPx(10) : Utils.dpToPx(2);
        p.setMargins(leftMargin, Utils.dpToPx(8), Utils.dpToPx(2), Utils.dpToPx(6));
        searchContainer.setLayoutParams(p);

        mArrowDrawable = new DrawerArrowDrawable(this);
        mArrowImage = (ImageView) actionBar.getCustomView().findViewById(R.id.arrow);
        // Use hardware acceleration for the animation
        mArrowImage.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mArrowImage.setImageDrawable(mArrowDrawable);
        FrameLayout arrowButton = (FrameLayout) actionBar.getCustomView().findViewById(
                R.id.arrow_button);
        if (mShowTabsInDrawer) {
            arrowButton.setOnClickListener(this);
        } else {
            arrowButton.setVisibility(View.GONE);
        }

        mProxyUtils = ProxyUtils.getInstance(this);

        setupFrameLayoutButton(R.id.action_back, R.id.icon_back);
        setupFrameLayoutButton(R.id.action_forward, R.id.icon_forward);
        setupFrameLayoutButton(R.id.action_add_bookmark, R.id.icon_star);
        setupFrameLayoutButton(R.id.action_toggle_desktop, R.id.icon_desktop);
        setupFrameLayoutButton(R.id.action_reading, R.id.icon_reading);

        mBookmarkImage = (ImageView) findViewById(R.id.icon_star);

        // create the search EditText in the ToolBar
        mSearch = (AutoCompleteTextView) actionBar.getCustomView().findViewById(R.id.search);
        mUntitledTitle = getString(R.string.untitled);
        mBackgroundColor = getResources().getColor(R.color.primary_color);
        mDeleteIcon = ThemeUtils.getLightThemedDrawable(this, R.drawable.ic_action_delete);
        mRefreshIcon = ThemeUtils.getLightThemedDrawable(this, R.drawable.ic_action_refresh);
        mCopyIcon = ThemeUtils.getLightThemedDrawable(this, R.drawable.ic_action_copy);

        int iconBounds = Utils.dpToPx(30);
        mDeleteIcon.setBounds(0, 0, iconBounds, iconBounds);
        mRefreshIcon.setBounds(0, 0, iconBounds, iconBounds);
        mCopyIcon.setBounds(0, 0, iconBounds, iconBounds);
        mIcon = mRefreshIcon;
        SearchClass search = new SearchClass();
        mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
        mSearch.setOnKeyListener(search.new KeyListener());
        mSearch.setOnFocusChangeListener(search.new FocusChangeListener());
        mSearch.setOnEditorActionListener(search.new EditorActionListener());
        mSearch.setOnTouchListener(search.new TouchListener());

        mSystemBrowser = getSystemBrowser();
        new Thread(new Runnable() {

            @Override
            public void run() {
                mBookmarkManager = BookmarkManager.getInstance(mActivity.getApplicationContext());
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
                if (mBookmarkList.size() == 0 && mPreferences.getDefaultBookmarks()) {
                    for (String[] array : BookmarkManager.DEFAULT_BOOKMARKS) {
                        HistoryItem bookmark = new HistoryItem(array[0], array[1]);
                        if (mBookmarkManager.addBookmark(bookmark)) {
                            mBookmarkList.add(bookmark);
                        }
                    }
                    Collections.sort(mBookmarkList, new BookmarkManager.SortIgnoreCase());
                    mPreferences.setDefaultBookmarks(false);
                }
                mBookmarkAdapter = new BookmarkViewAdapter(mActivity, R.layout.bookmark_list_item,
                        mBookmarkList);
                mDrawerListRight.setAdapter(mBookmarkAdapter);
                initializeSearchSuggestions(mSearch);
            }

        }).run();

        View view = findViewById(R.id.bookmark_back_button);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBookmarkManager == null)
                    return;
                if (!mBookmarkManager.isRootFolder()) {
                    setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
                }
            }
        });

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow, GravityCompat.END);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow, GravityCompat.START);

        if (API <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        }

        initializeTabs();

        mProxyUtils.checkForProxy(this);
    }

    private class SearchClass {

        public class KeyListener implements OnKeyListener {

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

        }

        public class EditorActionListener implements OnEditorActionListener {
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
        }

        public class FocusChangeListener implements OnFocusChangeListener {
            @Override
            public void onFocusChange(View v, final boolean hasFocus) {
                if (!hasFocus && mCurrentView != null) {
                    if (mCurrentView.getProgress() < 100) {
                        setIsLoading();
                    } else {
                        setIsFinishedLoading();
                    }
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
                    mIcon = mCopyIcon;
                    mSearch.setCompoundDrawables(null, null, mCopyIcon, null);
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
                        mArrowImage.startAnimation(anim);
                    }

                }, 100);

                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
                }
            }
        }

        public class TouchListener implements OnTouchListener {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSearch.getCompoundDrawables()[2] != null) {
                    boolean tappedX = event.getX() > (mSearch.getWidth()
                            - mSearch.getPaddingRight() - mIcon.getIntrinsicWidth());
                    if (tappedX) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (mSearch.hasFocus()) {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("label", mSearch.getText()
                                        .toString());
                                clipboard.setPrimaryClip(clip);
                                Utils.showSnackbar(mActivity, R.string.message_text_copied);
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
                if (urlString.length() > 0) {
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
        if (mPreferences == null) {
            mPreferences = PreferenceManager.getInstance();
        }
        mFullScreen = mPreferences.getFullScreenEnabled();
        mColorMode = mPreferences.getColorModeEnabled();
        mColorMode &= !mDarkTheme;
        if (!isIncognito() && !mColorMode && !mDarkTheme && mWebpageBitmap != null) {
            //TODO fix toolbar coloring
//            changeToolbarBackground(mWebpageBitmap, null);
        } else if (!isIncognito() && mCurrentView != null && !mDarkTheme
                && mCurrentView.getFavicon() != null) {
//            changeToolbarBackground(mCurrentView.getFavicon(), null);
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
        } else if ((keyCode == KeyEvent.KEYCODE_MENU) && (Build.VERSION.SDK_INT <= 16)
                && (Build.MANUFACTURER.compareTo("LGE") == 0)) {
            // Workaround for stupid LG devices that crash
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_MENU) && (Build.VERSION.SDK_INT <= 16)
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
                if (mCurrentView != null && !mCurrentView.getUrl().startsWith(Constants.FILE)) {
                    addBookmark(mCurrentView.getTitle(), mCurrentView.getUrl());
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
     * refreshes the underlying list of the Bookmark adapter since the bookmark
     * adapter doesn't always change when notifyDataChanged gets called.
     */
    private void notifyBookmarkDataSetChanged() {
        if (mBookmarkAdapter != null)
            mBookmarkAdapter.notifyDataSetChanged();
    }

    private void addBookmark(String title, String url) {
        HistoryItem bookmark = new HistoryItem(url, title);
        if (mBookmarkManager.addBookmark(bookmark)) {
            mBookmarkList.add(bookmark);
            Collections.sort(mBookmarkList, new BookmarkManager.SortIgnoreCase());
            notifyBookmarkDataSetChanged();
            mSearchAdapter.refreshBookmarks();
            updateBookmarkIndicator(mCurrentView.getUrl());
        }
    }

    private void setBookmarkDataSet(List<HistoryItem> items, boolean animate) {
        mBookmarkList.clear();
        mBookmarkList.addAll(items);
        notifyBookmarkDataSetChanged();
        final int resource;
        if (mBookmarkManager.isRootFolder())
            resource = R.drawable.ic_action_star;
        else
            resource = R.drawable.ic_action_back;

        final Animation startRotation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mBookmarkTitleImage.setRotationY(90 * interpolatedTime);
            }
        };
        final Animation finishRotation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mBookmarkTitleImage.setRotationY((-90) + (90 * interpolatedTime));
            }
        };
        startRotation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBookmarkTitleImage.setImageResource(resource);
                mBookmarkTitleImage.startAnimation(finishRotation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startRotation.setInterpolator(new AccelerateInterpolator());
        finishRotation.setInterpolator(new DecelerateInterpolator());
        startRotation.setDuration(250);
        finishRotation.setDuration(250);

        if (animate) {
            mBookmarkTitleImage.startAnimation(startRotation);
        } else {
            mBookmarkTitleImage.setImageResource(resource);
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
                        if (query.length() > 0)
                            showSearchInterfaceBar(query);
                    }
                });
        finder.show();
    }

    private void showSearchInterfaceBar(String text) {
        if (mCurrentView != null) {
            mCurrentView.find(text);
        }
        mSearchBar = (RelativeLayout) findViewById(R.id.search_bar);
        mSearchBar.setVisibility(View.VISIBLE);

        TextView tw = (TextView) findViewById(R.id.search_query);
        tw.setText("'" + text + "'");

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
     * The click listener for ListView in the navigation drawer
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mIsNewIntent = false;
            showTab(mWebViewList.get(position));
        }
    }

    /**
     * long click listener for Navigation Drawer
     */
    private class DrawerItemLongClickListener implements ListView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
            showCloseDialog(position);
            return true;
        }
    }

    private class BookmarkItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mBookmarkList.get(position).isFolder()) {
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(mBookmarkList.get(position).getTitle(), true), true);
                return;
            }
            if (mCurrentView != null) {
                mCurrentView.loadUrl(mBookmarkList.get(position).getUrl());
            }
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
    }

    private class BookmarkItemLongClickListener implements ListView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
            longPressBookmarkLink(mBookmarkList.get(position).getUrl());
            return true;
        }

    }

    /**
     * Takes in the id of which bookmark was selected and shows a dialog that
     * allows the user to rename and change the url of the bookmark
     *
     * @param id which id in the list was chosen
     */
    private synchronized void editBookmark(final int id) {
        final AlertDialog.Builder editBookmarkDialog = new AlertDialog.Builder(mActivity);
        editBookmarkDialog.setTitle(R.string.title_edit_bookmark);
        final EditText getTitle = new EditText(mActivity);
        getTitle.setHint(R.string.hint_title);
        getTitle.setText(mBookmarkList.get(id).getTitle());
        getTitle.setSingleLine();
        final EditText getUrl = new EditText(mActivity);
        getUrl.setHint(R.string.hint_url);
        getUrl.setText(mBookmarkList.get(id).getUrl());
        getUrl.setSingleLine();
        final AutoCompleteTextView getFolder = new AutoCompleteTextView(mActivity);
        getFolder.setHint(R.string.folder);
        getFolder.setText(mBookmarkList.get(id).getFolder());
        getFolder.setSingleLine();
        List<String> folders = mBookmarkManager.getFolderTitles();
        ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, folders);
        getFolder.setThreshold(1);
        getFolder.setAdapter(suggestionsAdapter);
        LinearLayout layout = new LinearLayout(mActivity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(10);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(getTitle);
        layout.addView(getUrl);
        layout.addView(getFolder);
        editBookmarkDialog.setView(layout);
        editBookmarkDialog.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HistoryItem item = new HistoryItem();
                        String currentFolder = mBookmarkList.get(id).getFolder();
                        item.setTitle(getTitle.getText().toString());
                        item.setUrl(getUrl.getText().toString());
                        item.setUrl(getUrl.getText().toString());
                        item.setFolder(getFolder.getText().toString());
                        mBookmarkManager.editBookmark(mBookmarkList.get(id), item);

                        List<HistoryItem> list = mBookmarkManager.getBookmarksFromFolder(currentFolder, true);
                        if (list.size() == 0) {
                            setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
                        } else {
                            setBookmarkDataSet(list, false);
                        }

                        if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                                && mCurrentView.getUrl().endsWith(BookmarkPage.FILENAME)) {
                            openBookmarkPage(mWebView);
                        }
                        if (mCurrentView != null) {
                            updateBookmarkIndicator(mCurrentView.getUrl());
                        }
                    }
                });
        editBookmarkDialog.show();
    }

    /**
     * Show a dialog to rename a folder
     *
     * @param id the position of the HistoryItem (folder) in the bookmark list
     */
    private synchronized void renameFolder(final int id) {
        final AlertDialog.Builder editFolderDialog = new AlertDialog.Builder(mActivity);
        editFolderDialog.setTitle(R.string.title_rename_folder);
        final EditText getTitle = new EditText(mActivity);
        getTitle.setHint(R.string.hint_title);
        getTitle.setText(mBookmarkList.get(id).getTitle());
        getTitle.setSingleLine();
        LinearLayout layout = new LinearLayout(mActivity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(10);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(getTitle);
        editFolderDialog.setView(layout);
        editFolderDialog.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String oldTitle = mBookmarkList.get(id).getTitle();
                        String newTitle = getTitle.getText().toString();

                        mBookmarkManager.renameFolder(oldTitle, newTitle);

                        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);

                        if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                                && mCurrentView.getUrl().endsWith(BookmarkPage.FILENAME)) {
                            openBookmarkPage(mWebView);
                        }
                        if (mCurrentView != null) {
                            updateBookmarkIndicator(mCurrentView.getUrl());
                        }
                    }
                });
        editFolderDialog.show();
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
        if (view == null) {
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
        }, 150);

        updateBookmarkIndicator(mWebView.getUrl());

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // Remove browser frame background to reduce overdraw
//                //TODO evaluate performance
//                mBrowserFrame.setBackgroundColor(0);
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
            mCurrentView.loadUrl(url);
        } else if (url != null) {
            if (url.startsWith(Constants.FILE)) {
                Utils.showSnackbar(this, R.string.message_blocked_local);
                url = null;
            }
            newTab(url, true);
            mIsNewIntent = (source == null);
        }
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
            for (int n = 0; n < mWebViewList.size(); n++) {
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
        LightningView startingTab = new LightningView(mActivity, url, mDarkTheme, isIncognito());
        if (mIdGenerator == 0) {
            startingTab.resumeTimers();
        }
        mIdGenerator++;
        mWebViewList.add(startingTab);

        mTitleAdapter.notifyDataSetChanged();
        if (show) {
            mDrawerListLeft.setItemChecked(mWebViewList.size() - 1, true);
            showTab(startingTab);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerListLeft.smoothScrollToPosition(mWebViewList.size() - 1);
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
            mDrawerListLeft.setItemChecked(current - 1, true);
            reference.onDestroy();
        } else if (mWebViewList.size() > position + 1) {
            if (current == position) {
                showTab(mWebViewList.get(position + 1));
                mWebViewList.remove(position);
                mDrawerListLeft.setItemChecked(position, true);
            } else {
                mWebViewList.remove(position);
            }

            reference.onDestroy();
        } else if (mWebViewList.size() > 1) {
            if (current == position) {
                showTab(mWebViewList.get(position - 1));
                mWebViewList.remove(position);
                mDrawerListLeft.setItemChecked(position - 1, true);
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
                mTitleAdapter.notifyDataSetChanged();
                finish();

            }
        }
        mTitleAdapter.notifyDataSetChanged();

        if (mIsNewIntent && isShown) {
            mIsNewIntent = false;
            closeActivity();
        }

        Log.d(Constants.TAG, "deleted tab");
    }

    public void performExitCleanUp() {
        if (mPreferences.getClearCacheExit() && mCurrentView != null && !isIncognito()) {
            WebUtils.clearCache(mCurrentView.getWebView());
            Log.d(Constants.TAG, "Cache Cleared");

        }
        if (mPreferences.getClearHistoryExitEnabled() && !isIncognito()) {
            WebUtils.clearHistory(this, mSystemBrowser);
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
        for (int n = 0; n < mWebViewList.size(); n++) {
            if (mWebViewList.get(n) != null) {
                mWebViewList.get(n).onDestroy();
            }
        }
        mWebViewList.clear();
        mTitleAdapter.notifyDataSetChanged();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawer(mDrawerLeft);
        } else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
            if (!mBookmarkManager.isRootFolder()) {
                setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), true);
            } else {
                mDrawerLayout.closeDrawer(mDrawerRight);
            }
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
    }

    void saveOpenTabs() {
        if (mPreferences.getRestoreLostTabsEnabled()) {
            String s = "";
            for (int n = 0; n < mWebViewList.size(); n++) {
                if (mWebViewList.get(n).getUrl().length() > 0) {
                    s = s + mWebViewList.get(n).getUrl() + "|$|SEPARATOR|$|";
                }
            }
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
        Log.d(Constants.TAG, "onResume");
        if (mSearchAdapter != null) {
            mSearchAdapter.refreshPreferences();
            mSearchAdapter.refreshBookmarks();
        }
        if (mCurrentView != null) {
            mCurrentView.resumeTimers();
            mCurrentView.onResume();
        }
        mHistoryDatabase = HistoryDatabase.getInstance(getApplicationContext());
        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
        initializePreferences();
        for (int n = 0; n < mWebViewList.size(); n++) {
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
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    private void searchTheWeb(@NonNull String query) {
        if (query.isEmpty()) {
            return;
        }
        String SEARCH = mSearchText;
        query = query.trim();
        mCurrentView.stopLoading();

        if (query.startsWith("www.")) {
            query = Constants.HTTP + query;
        } else if (query.startsWith("ftp.")) {
            query = "ftp://" + query;
        }

        boolean containsPeriod = query.contains(".");
        boolean isIPAddress = (TextUtils.isDigitsOnly(query.replace(".", ""))
                && (query.replace(".", "").length() >= 4) && query.contains("."));
        boolean aboutScheme = query.contains("about:");
        boolean validURL = (query.startsWith("ftp://") || query.startsWith(Constants.HTTP)
                || query.startsWith(Constants.FILE) || query.startsWith(Constants.HTTPS))
                || isIPAddress;
        boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

        if (isIPAddress
                && (!query.startsWith(Constants.HTTP) || !query.startsWith(Constants.HTTPS))) {
            query = Constants.HTTP + query;
        }

        if (isSearch) {
            try {
                query = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            mCurrentView.loadUrl(SEARCH + query);
        } else if (!validURL) {
            mCurrentView.loadUrl(Constants.HTTP + query);
        } else {
            mCurrentView.loadUrl(query);
        }
    }

    public class LightningViewAdapter extends ArrayAdapter<LightningView> {

        final Context context;
        ColorMatrix colorMatrix;
        ColorMatrixColorFilter filter;
        Paint paint;
        final int layoutResourceId;
        List<LightningView> data = null;
        final CloseTabListener mExitListener;
        private final Drawable mBackgroundTabDrawable;
        private final Drawable mForegroundTabDrawable;

        public LightningViewAdapter(Context context, int layoutResourceId, List<LightningView> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
            this.mExitListener = new CloseTabListener();


            int backgroundColor = Utils.mixTwoColors(ThemeUtils.getPrimaryColor(mActivity), Color.BLACK, 0.75f);
            Bitmap backgroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
            Utils.drawTrapezoid(new Canvas(backgroundTabBitmap), backgroundColor, true);
            mBackgroundTabDrawable = new BitmapDrawable(getResources(), backgroundTabBitmap);

            int foregroundColor = ThemeUtils.getPrimaryColor(context);
            Bitmap foregroundTabBitmap = Bitmap.createBitmap(Utils.dpToPx(175), Utils.dpToPx(30), Bitmap.Config.ARGB_8888);
            Utils.drawTrapezoid(new Canvas(foregroundTabBitmap), foregroundColor, false);
            mForegroundTabDrawable = new BitmapDrawable(getResources(), foregroundTabBitmap);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View row = convertView;
            LightningViewHolder holder;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new LightningViewHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.textTab);
                holder.favicon = (ImageView) row.findViewById(R.id.faviconTab);
                holder.exit = (ImageView) row.findViewById(R.id.deleteButton);
                if (!mShowTabsInDrawer) {
                    holder.layout = (LinearLayout) row.findViewById(R.id.tab_item_background);
                }
                holder.exitButton = (FrameLayout) row.findViewById(R.id.deleteAction);
                holder.exit.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
                row.setTag(holder);
            } else {
                holder = (LightningViewHolder) row.getTag();
            }

            holder.exitButton.setTag(position);
            holder.exitButton.setOnClickListener(mExitListener);

            ViewCompat.jumpDrawablesToCurrentState(holder.exitButton);

            LightningView web = data.get(position);
            holder.txtTitle.setText(web.getTitle());

            Bitmap favicon = web.getFavicon();
            if (web.isForegroundTab()) {
                holder.txtTitle.setTextAppearance(context, R.style.boldText);
                holder.favicon.setImageBitmap(favicon);
                if (!mShowTabsInDrawer) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        holder.layout.setBackground(mForegroundTabDrawable);
                    } else {
                        holder.layout.setBackgroundDrawable(mForegroundTabDrawable);
                    }
                }
                if (!isIncognito() && mColorMode) {
                    // TODO fix toolbar coloring
//                    changeToolbarBackground(favicon, mForegroundTabDrawable);
                }
            } else {
                holder.txtTitle.setTextAppearance(context, R.style.normalText);
                if (!mShowTabsInDrawer) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        holder.layout.setBackground(mBackgroundTabDrawable);
                    } else {
                        holder.layout.setBackgroundDrawable(mBackgroundTabDrawable);
                    }
                }
                Bitmap grayscaleBitmap = Bitmap.createBitmap(favicon.getWidth(),
                        favicon.getHeight(), Bitmap.Config.ARGB_8888);

                Canvas c = new Canvas(grayscaleBitmap);
                if (colorMatrix == null || filter == null || paint == null) {
                    paint = new Paint();
                    colorMatrix = new ColorMatrix();
                    colorMatrix.setSaturation(0.5f);
                    filter = new ColorMatrixColorFilter(colorMatrix);
                    paint.setColorFilter(filter);
                }

                c.drawBitmap(favicon, 0, 0, paint);
                holder.favicon.setImageBitmap(grayscaleBitmap);
            }
            return row;
        }

        class LightningViewHolder {
            TextView txtTitle;
            ImageView favicon;
            ImageView exit;
            FrameLayout exitButton;
            LinearLayout layout;
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
        Palette.from(favicon).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {

                // OR with opaque black to remove transparency glitches
                int color = 0xff000000 | palette.getVibrantColor(mActivity.getResources()
                        .getColor(R.color.primary_color));

                int finalColor; // Lighten up the dark color if it is
                // too dark
                if (Utils.isColorTooDark(color)) {
                    finalColor = Utils.mixTwoColors(
                            mActivity.getResources().getColor(R.color.primary_color),
                            color, 0.25f);
                } else {
                    finalColor = color;
                }

                ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(),
                        mBackground.getColor(), finalColor);
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
                        }
                        mToolbarLayout.setBackgroundColor(color);
                        if (tabBackground != null) {
                            tabBackground.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                        }
                    }

                });
                anim.setDuration(300);
                anim.start();
            }
        });
    }

    public class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

        final Context context;
        List<HistoryItem> data = null;
        final int layoutResourceId;
        final Bitmap folderIcon;

        public BookmarkViewAdapter(Context context, int layoutResourceId, List<HistoryItem> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
            this.folderIcon = mFolderBitmap;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            BookmarkViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new BookmarkViewHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.textBookmark);
                holder.favicon = (ImageView) row.findViewById(R.id.faviconBookmark);
                row.setTag(holder);
            } else {
                holder = (BookmarkViewHolder) row.getTag();
            }

            ViewCompat.jumpDrawablesToCurrentState(row);

            HistoryItem web = data.get(position);
            holder.txtTitle.setText(web.getTitle());
            holder.favicon.setImageBitmap(mWebpageBitmap);
            if (web.isFolder()) {
                holder.favicon.setImageBitmap(this.folderIcon);
            } else if (web.getBitmap() == null) {
                getImage(holder.favicon, web);
            } else {
                holder.favicon.setImageBitmap(web.getBitmap());
            }
            return row;
        }

        class BookmarkViewHolder {
            TextView txtTitle;
            ImageView favicon;
        }
    }

    private void getImage(ImageView image, HistoryItem web) {
        new DownloadImageTask(image, web).execute(web.getUrl());
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        final ImageView bmImage;
        final HistoryItem mWeb;

        public DownloadImageTask(ImageView bmImage, HistoryItem web) {
            this.bmImage = bmImage;
            this.mWeb = web;
        }

        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap mIcon = null;
            // unique path for each url that is bookmarked.
            String hash = String.valueOf(Utils.getDomainName(url).hashCode());
            File image = new File(mActivity.getCacheDir(), hash + ".png");
            String urldisplay;
            try {
                urldisplay = Utils.getProtocol(url) + getDomainName(url) + "/favicon.ico";
            } catch (URISyntaxException e) {
                e.printStackTrace();
                urldisplay = "https://www.google.com/s2/favicons?domain_url=" + url;
            }
            // checks to see if the image exists
            if (!image.exists()) {
                FileOutputStream fos = null;
                InputStream in = null;
                try {
                    // if not, download it...
                    URL urlDownload = new URL(urldisplay);
                    HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    in = connection.getInputStream();

                    if (in != null) {
                        mIcon = BitmapFactory.decodeStream(in);
                    }
                    // ...and cache it
                    if (mIcon != null) {
                        fos = new FileOutputStream(image);
                        mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        Log.d(Constants.TAG, "Downloaded: " + urldisplay);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Utils.close(in);
                    Utils.close(fos);
                }
            } else {
                // if it exists, retrieve it from the cache
                mIcon = BitmapFactory.decodeFile(image.getPath());
            }
            if (mIcon == null) {
                InputStream in = null;
                FileOutputStream fos = null;
                try {
                    // if not, download it...
                    URL urlDownload = new URL("https://www.google.com/s2/favicons?domain_url="
                            + url);
                    HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    in = connection.getInputStream();

                    if (in != null) {
                        mIcon = BitmapFactory.decodeStream(in);
                    }
                    // ...and cache it
                    if (mIcon != null) {
                        fos = new FileOutputStream(image);
                        mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Utils.close(in);
                    Utils.close(fos);
                }
            }
            if (mIcon == null) {
                return mWebpageBitmap;
            } else {
                return mIcon;
            }
        }

        protected void onPostExecute(Bitmap result) {
            Bitmap fav = Utils.padFavicon(result);
            bmImage.setImageBitmap(fav);
            mWeb.setBitmap(fav);
            notifyBookmarkDataSetChanged();
        }
    }

    private static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            return url;
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    @Override
    public void updateUrl(String url, boolean shortUrl) {
        if (url == null || mSearch == null || mSearch.hasFocus()) {
            return;
        }
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
                if (isSystemBrowserAvailable() && mPreferences.getSyncHistoryEnabled()) {
                    try {
                        Browser.updateVisitedHistory(getContentResolver(), url, true);
                    } catch (Exception ignored) {
                        // ignored
                    }
                }
                try {
                    if (mHistoryDatabase == null) {
                        mHistoryDatabase = HistoryDatabase.getInstance(mActivity.getApplicationContext());
                    }
                    mHistoryDatabase.visitHistoryItem(url, title);
                } catch (IllegalStateException e) {
                    Log.e(Constants.TAG, "IllegalStateException in updateHistory");
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "NullPointerException in updateHistory");
                } catch (SQLiteException e) {
                    Log.e(Constants.TAG, "SQLiteException in updateHistory");
                }
            }
        };
        if (url != null && !url.startsWith(Constants.FILE)) {
            new Thread(update).start();
        }
    }

    private boolean isSystemBrowserAvailable() {
        return mSystemBrowser;
    }

    private boolean getSystemBrowser() {
        Cursor c = null;
        String[] columns = new String[]{"url", "title"};
        boolean browserFlag;
        try {
            Uri bookmarks = Browser.BOOKMARKS_URI;
            c = getContentResolver().query(bookmarks, columns, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (c != null) {
            Log.d("Browser", "System Browser Available");
            browserFlag = true;
        } else {
            Log.e("Browser", "System Browser Unavailable");
            browserFlag = false;
        }
        if (c != null) {
            c.close();
        }
        mPreferences.setSystemBrowserPresent(browserFlag);
        return browserFlag;
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
                    if (url.startsWith(mActivity.getString(R.string.suggestion))) {
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
        mSearchAdapter = new SearchAdapter(mActivity, mDarkTheme, isIncognito());
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
                mCurrentView.loadUrl(HistoryPage.getHistoryPage(mActivity));
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
        File image = new File(mActivity.getCacheDir(), "folder.png");
        try {
            outputStream = new FileOutputStream(image);
            folderIcon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            folderIcon.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            Utils.close(outputStream);
        }
        File bookmarkWebPage = new File(mActivity.getFilesDir(), BookmarkPage.FILENAME);

        buildBookmarkPage(null, mBookmarkManager.getBookmarksFromFolder(null, true));
        view.loadUrl(Constants.FILE + bookmarkWebPage);
    }

    private void buildBookmarkPage(final String folder, final List<HistoryItem> list) {
        File bookmarkWebPage;
        if (folder == null || folder.length() == 0) {
            bookmarkWebPage = new File(mActivity.getFilesDir(), BookmarkPage.FILENAME);
        } else {
            bookmarkWebPage = new File(mActivity.getFilesDir(), folder + '-' + BookmarkPage.FILENAME);
        }
        final StringBuilder bookmarkBuilder = new StringBuilder(BookmarkPage.HEADING);

        String folderIconPath = Constants.FILE + mActivity.getCacheDir() + "/folder.png";
        for (int n = 0; n < list.size(); n++) {
            final HistoryItem item = list.get(n);
            bookmarkBuilder.append(BookmarkPage.PART1);
            if (item.isFolder()) {
                File folderPage = new File(mActivity.getFilesDir(), item.getTitle() + '-' + BookmarkPage.FILENAME);
                bookmarkBuilder.append(Constants.FILE).append(folderPage);
                bookmarkBuilder.append(BookmarkPage.PART2);
                bookmarkBuilder.append(folderIconPath);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        buildBookmarkPage(item.getTitle(), mBookmarkManager.getBookmarksFromFolder(item.getTitle(), true));
                    }
                }).run();
            } else {
                bookmarkBuilder.append(item.getUrl());
                bookmarkBuilder.append(BookmarkPage.PART2).append(BookmarkPage.PART3);
                bookmarkBuilder.append(item.getUrl());
            }
            bookmarkBuilder.append(BookmarkPage.PART4);
            bookmarkBuilder.append(item.getTitle());
            bookmarkBuilder.append(BookmarkPage.PART5);
        }
        bookmarkBuilder.append(BookmarkPage.END);
        FileWriter bookWriter = null;
        try {
            bookWriter = new FileWriter(bookmarkWebPage, false);
            bookWriter.write(bookmarkBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookWriter);
        }
    }

    @Override
    public void update() {
        mTitleAdapter.notifyDataSetChanged();
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

    /**
     * handles long presses for the browser, tries to get the
     * url of the item that was clicked and sends it (it can be null)
     * to the click handler that does cool stuff with it
     */
    @Override
    public void onLongPress() {
        if (mClickHandler == null) {
            mClickHandler = new ClickHandler(mActivity);
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
        mFullscreenContainer = new FullscreenHolder(this);
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

        if (API < 19) {
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
    static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent evt) {
            return true;
        }

    }

    /**
     * a stupid method that returns the bitmap image to display in place of
     * a loading video
     */
    @Override
    public Bitmap getDefaultVideoPoster() {
        if (mDefaultVideoPoster == null) {
            mDefaultVideoPoster = BitmapFactory.decodeResource(getResources(),
                    android.R.drawable.ic_media_play);
        }
        return mDefaultVideoPoster;
    }

    /**
     * dumb method that returns the loading progress for a video
     */
    @Override
    public View getVideoLoadingProgressView() {
        if (mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, null);
        }
        return mVideoProgressView;
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
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebView);
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
        deleteTab(mWebViewList.indexOf(view));
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
//                show.setFillAfter(true);
                mWebView.startAnimation(show);
            }
        }
    }

    /**
     * obviously it shows the action bar if it's hidden
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
//                show.setFillAfter(true);
                mWebView.startAnimation(show);
            }
        }
    }

    /**
     * handles a long click on the page, parameter String url
     * is the url that should have been obtained from the WebView touch node
     * thingy, if it is null, this method tries to deal with it and find a workaround
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
            } else if (currentUrl.endsWith(BookmarkPage.FILENAME)) {
                if (url != null) {
                    longPressBookmarkLink(url);
                } else if (result != null && result.getExtra() != null) {
                    final String newUrl = result.getExtra();
                    longPressBookmarkLink(newUrl);
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

    private void longPressFolder(String url) {
        String title;
        if (url.startsWith(Constants.FILE)) {
            // We are getting the title from the url
            // Strip '-bookmarks.html' from the end of the url
            title = url.substring(0, url.length() - BookmarkPage.FILENAME.length() - 1);

            // Strip the beginning of the url off and leave only the title
            title = title.substring(Constants.FILE.length() + mActivity.getFilesDir().toString().length() + 1);
        } else if (url.startsWith(Constants.FOLDER)) {
            title = url.substring(Constants.FOLDER.length());
        } else {
            title = url;
        }
        final int position = BookmarkManager.getIndexOfBookmark(mBookmarkList, Constants.FOLDER + title);
        if (position == -1) {
            return;
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        renameFolder(position);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        mBookmarkManager.deleteFolder(mBookmarkList.get(position).getTitle());

                        setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);

                        if (mCurrentView != null && mCurrentView.getUrl().startsWith(Constants.FILE)
                                && mCurrentView.getUrl().endsWith(BookmarkPage.FILENAME)) {
                            openBookmarkPage(mWebView);
                        }
                        if (mCurrentView != null) {
                            updateBookmarkIndicator(mCurrentView.getUrl());
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.action_folder)
                .setMessage(R.string.dialog_folder)
                .setCancelable(true)
                .setPositiveButton(R.string.action_rename, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .show();
    }

    private void longPressBookmarkLink(final String url) {
        if (url.startsWith(Constants.FILE) || url.startsWith(Constants.FOLDER)) {
            longPressFolder(url);
            return;
        }
        final int position = BookmarkManager.getIndexOfBookmark(mBookmarkList, url);
        if (position == -1) {
            return;
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        newTab(mBookmarkList.get(position).getUrl(), false);
                        mDrawerLayout.closeDrawers();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        if (mBookmarkManager.deleteBookmark(mBookmarkList.get(position))) {
                            mBookmarkList.remove(position);
                            notifyBookmarkDataSetChanged();
                            mSearchAdapter.refreshBookmarks();
                            openBookmarks();
                            if (mCurrentView != null) {
                                updateBookmarkIndicator(mCurrentView.getUrl());
                            }
                        }
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        editBookmark(position);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.action_bookmarks)
                .setMessage(R.string.dialog_bookmark)
                .setCancelable(true)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .setNeutralButton(R.string.action_edit, dialogClickListener)
                .show();
    }

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
                            mCurrentView.loadUrl(url);
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.action_history)
                .setMessage(R.string.dialog_history_long_press)
                .setCancelable(true)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .setNeutralButton(R.string.action_open, dialogClickListener)
                .show();
    }

    private void longPressImage(final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        newTab(url, false);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        mCurrentView.loadUrl(url);
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        if (API > 8) {
                            Utils.downloadFile(mActivity, url,
                                    mCurrentView.getUserAgent(), "attachment");
                        }
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(url.replace(Constants.HTTP, ""))
                .setCancelable(true)
                .setMessage(R.string.dialog_image)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_open, dialogClickListener)
                .setNeutralButton(R.string.action_download, dialogClickListener)
                .show();
    }

    private void longPressLink(final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        newTab(url, false);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        mCurrentView.loadUrl(url);
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("label", url);
                        clipboard.setPrimaryClip(clip);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
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
        if (mCurrentView != null) {
            if (mCurrentView.getProgress() < 100) {
                mCurrentView.stopLoading();
            } else {
                mCurrentView.reload();
            }
        }
    }

    @Override
    public void updateBookmarkIndicator(String url) {
        if (url == null || !mBookmarkManager.isBookmark(url)) {
            mBookmarkImage.setImageResource(R.drawable.ic_action_star);
            mBookmarkImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
        } else {
            mBookmarkImage.setImageResource(R.drawable.ic_bookmark);
            mBookmarkImage.setColorFilter(ThemeUtils.getAccentColor(this), PorterDuff.Mode.SRC_IN);
        }
    }

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
                }
                break;
            case R.id.new_tab_button:
                newTab(null, true);
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
            case R.id.action_add_bookmark:
                if (mCurrentView != null && !mCurrentView.getUrl().startsWith(Constants.FILE)) {
                    addBookmark(mCurrentView.getTitle(), mCurrentView.getUrl());
                }
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.new_tab_button:
                String url = mPreferences.getSavedUrl();
                if (url != null) {
                    newTab(url, true);
                    Utils.showSnackbar(mActivity, R.string.deleted_tab);
                }
                mPreferences.setSavedUrl(null);
                break;
        }
        return true;
    }

    private void setupFrameLayoutButton(@IdRes int buttonId, @IdRes int imageId) {
        FrameLayout frameButton = (FrameLayout) findViewById(buttonId);
        frameButton.setOnClickListener(this);
        frameButton.setOnLongClickListener(this);
        ImageView buttonImage = (ImageView) findViewById(imageId);
        buttonImage.setColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);
    }

    private final NetworkReceiver mNetworkReceiver = new NetworkReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            boolean isConnected = isConnected(context);
            Log.d(Constants.TAG, "Network Connected: " + String.valueOf(isConnected));
            for (int n = 0; n < mWebViewList.size(); n++) {
                WebView view = mWebViewList.get(n).getWebView();
                if (view != null)
                    view.setNetworkAvailable(isConnected);
            }
        }
    };
}
