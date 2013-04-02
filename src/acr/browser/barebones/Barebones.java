package acr.browser.barebones;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.MultiAutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class Barebones extends Activity implements OnLongClickListener,
		OnTouchListener {

	// variables to differentiate free from paid
	static final int MAX_TABS = 5;
	static final int MAX_BOOKMARKS = 5;
	static final boolean PAID_VERSION = false;

	// variable declaration

	static Rect edge;
	static SimpleAdapter adapter;
	static MultiAutoCompleteTextView getUrl;
	static TextView[] urlTitle = new TextView[MAX_TABS];
	static AnthonyWebView[] main = new AnthonyWebView[MAX_TABS];
	static Rect[] bounds = new Rect[MAX_TABS];
	static private ValueCallback<Uri> mUploadMessage;
	static ImageView refresh;
	static ProgressBar progressBar;
	static Drawable icon;
	static Drawable loading, webpage, webpageOther;
	static Drawable exitTab;
	final static int FILECHOOSER_RESULTCODE = 1;
	static int num, x, y;
	static final int fuzz = 10;
	static int statusBar;
	static int number, pageId = 0, agentPicker;
	static int enableFlash, lastVisibleWebView;
	static int height56, height32;
	static int height, width, pixels, leftPad, rightPad, pixelHeight;
	static int bookHeight, API;
	static int mShortAnimationDuration;
	static int id, tenPad;
	static int hitTest;
	static int urlColumn, titleColumn;
	static View mCustomView = null;
	static CustomViewCallback mCustomViewCallback;
	static boolean xPress;
	static boolean tabsAreDisplayed = true, isPhone = false;
	static boolean pageIsLoading = false, java;
	static boolean allowLocation, savePasswords, deleteHistory;
	static boolean showFullScreen, pageIdIsVisible = true;
	static boolean urlBarShows = true, move = false;
	static boolean isBookmarkShowing = false;
	static boolean uBarShows = true;
	static boolean noStockBrowser = true;
	static SharedPreferences settings;
	static SharedPreferences.Editor edit;
	static String desktop, mobile, user;
	static String urlA, title;
	static String[] bUrl = new String[MAX_BOOKMARKS];
	static String[] bTitle = new String[MAX_BOOKMARKS];
	static String[] columns;
	static String homepage, str;
	static final String preferences = "settings";
	static String query, userAgent;
	static String[][] urlToLoad = new String[MAX_TABS][2];
	static FrameLayout background;
	static ScrollView scrollBookmarks;
	static RelativeLayout uBar, bg;
	static RelativeLayout barLayout;
	static RelativeLayout refreshLayout;
	static HorizontalScrollView tabScroll;
	static Animation slideUp;
	static Animation slideDown;
	static Animation fadeOut, fadeIn;
	static long clock = 0;
	static long timeBetweenDownPress = System.currentTimeMillis();
	static TextView txt;
	static Uri bookmarks;
	static Cursor managedCursor;
	static List<Map<String, String>> list;
	static Map<String, String> map;
	static Handler handler;
	static DatabaseHandler historyHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); // displays main xml layout
		settings = getSharedPreferences(preferences, 0);
		edit = settings.edit();
		init(); // sets up random stuff
		options(); // allows options to be opened
		enter();// enter url bar
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		float widthInInches = metrics.widthPixels / metrics.xdpi;
		float heightInInches = metrics.heightPixels / metrics.ydpi;
		double sizeInInches = Math.sqrt(Math.pow(widthInInches, 2)
				+ Math.pow(heightInInches, 2));
		// 0.5" buffer for 7" devices
		isPhone = sizeInInches < 6.5;

		forward();// forward button
		exit();
		int first = settings.getInt("first", 0);

		if (first == 0) {
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						break;

					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(
					Barebones.this); // dialog
			builder.setMessage(
					"TIPS:\n"
							+ "\nLong-press a tab to close it\n\nLong-press back button to exit browser"
							+ "\n\nSet your homepage in settings to about:blank to set a blank page as your default\n"
							+ "\nSet the homepage to about:home to set bookmarks as your homepage")
					.setPositiveButton("Ok", dialogClickListener).show();
			edit.putInt("first", 1);
			edit.commit();
		}
	}

	public void init() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		historyHandler = new DatabaseHandler(this);
		API = Integer.valueOf(android.os.Build.VERSION.SDK_INT); // gets the sdk
		// level
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		if (API >= 11) {
			progressBar.setIndeterminateDrawable(getResources().getDrawable(
					R.drawable.ics_animation));
		} else {
			progressBar.setIndeterminateDrawable(getResources().getDrawable(
					R.drawable.ginger_animation));
		}
		showFullScreen = settings.getBoolean("fullscreen", false);
		uBar = (RelativeLayout) findViewById(R.id.urlBar);
		bg = (RelativeLayout) findViewById(R.id.background);
		slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
		slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
		fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		mShortAnimationDuration = getResources().getInteger(
				android.R.integer.config_mediumAnimTime);
		slideUp.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {

				uBar.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {

			}

			@Override
			public void onAnimationStart(Animation arg0) {

			}

		});
		slideDown.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationStart(Animation animation) {

				uBar.setVisibility(View.VISIBLE);
			}

		});

		barLayout = (RelativeLayout) findViewById(R.id.relativeLayout1);
		refreshLayout = (RelativeLayout) findViewById(R.id.refreshLayout);
		refreshLayout.setBackgroundResource(R.drawable.button);
		// get settings
		WebView test = new WebView(Barebones.this); // getting default webview
													// user agent
		user = test.getSettings().getUserAgentString();
		background = (FrameLayout) findViewById(R.id.holder);
		mobile = user; // setting mobile user
						// agent
		desktop = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17"; // setting
		// desktop user agent
		exitTab = getResources().getDrawable(R.drawable.stop); // user
		// agent
		homepage = settings.getString("home", "http://www.google.com"); // initializing
																		// the
																		// stored
																		// homepage
																		// variable

		test.destroy();
		userAgent = settings.getString("agent", mobile); // initializing
															// useragent string
		allowLocation = settings.getBoolean("location", false); // initializing
																// location
																// variable
		savePasswords = settings.getBoolean("passwords", false); // initializing
																	// save
																	// passwords
																	// variable
		enableFlash = settings.getInt("enableflash", 0); // enable flash
															// boolean
		agentPicker = settings.getInt("agentchoose", 1); // which user agent to
															// use, 1=mobile,
															// 2=desktop,
															// 3=custom

		deleteHistory = settings.getBoolean("history", false); // delete history
																// on exit
																// boolean
		// initializing variables declared

		height = getResources().getDrawable(R.drawable.loading)
				.getMinimumHeight();
		width = getResources().getDrawable(R.drawable.loading)
				.getMinimumWidth();

		// hides keyboard so it doesn't default pop up
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		// opens icondatabase so that favicons can be stored
		WebIconDatabase.getInstance().open(
				getDir("icons", MODE_PRIVATE).getPath());

		// scroll view containing tabs
		tabScroll = (HorizontalScrollView) findViewById(R.id.tabScroll);
		tabScroll.setBackgroundColor(getResources().getColor(R.color.black));
		tabScroll.setHorizontalScrollBarEnabled(false);
		if (API > 8) {
			tabScroll.setOverScrollMode(View.OVER_SCROLL_NEVER); // disallow
																	// overscroll
																	// (only
																	// available
																	// in 2.3
																	// and up)
		}

		// image dimensions and initialization
		final int dps = 175;
		final float scale = getApplicationContext().getResources()
				.getDisplayMetrics().density;
		pixels = (int) (dps * scale + 0.5f);
		pixelHeight = (int) (36 * scale + 0.5f);
		bookHeight = (int) (48 * scale + 0.5f);
		height56 = (int) (56 * scale + 0.5f);
		leftPad = (int) (17 * scale + 0.5f);
		rightPad = (int) (15 * scale + 0.5f);
		height32 = (int) (32 * scale + 0.5f);
		tenPad = (int) (10 * scale + 0.5f);
		statusBar = (int) (25 * scale + 0.5f);
		number = 0;
		loading = getResources().getDrawable(R.drawable.loading);
		webpage = getResources().getDrawable(R.drawable.webpage);
		webpageOther = getResources().getDrawable(R.drawable.webpage);
		loading.setBounds(0, 0, width * 2 / 3, height * 2 / 3);
		webpage.setBounds(0, 0, width * 2 / 3, height * 2 / 3);
		webpageOther.setBounds(0, 0, width * 1 / 2, height * 1 / 2);
		exitTab.setBounds(0, 0, width * 2 / 3, height * 2 / 3);
		Intent url = getIntent();
		String URL = null; // that opens the browser
		// gets the string passed into the browser
		URL = url.getDataString();
		if (URL != null) {
			// opens a new tab with the url if its there
			newTab(number, URL);
		} else {
			// otherwise it opens the homepage
			newTab(number, homepage);
		}

		// new tab button
		ImageView newTab = (ImageView) findViewById(R.id.newTab);
		newTab.setBackgroundResource(R.drawable.button);
		newTab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				newTab(number, homepage);
			}
		});
		refresh = (ImageView) findViewById(R.id.refresh);
		refreshLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (pageIsLoading) {
					main[pageId].stopLoading();
				} else {
					main[pageId].reload();
				}
			}

		});

		enterUrl();
		if (showFullScreen) {
			bg.removeView(uBar);
			background.addView(uBar);
		}

	}

	public class SpaceTokenizer implements Tokenizer {

		public int findTokenStart(CharSequence text, int cursor) {
			int i = cursor;

			while (i > 0 && text.charAt(i - 1) != ' ') {
				i--;
			}
			while (i < cursor && text.charAt(i) == ' ') {
				i++;
			}

			return i;
		}

		public int findTokenEnd(CharSequence text, int cursor) {
			int i = cursor;
			int len = text.length();

			while (i < len) {
				if (text.charAt(i) == ' ') {
					return i;
				} else {
					i++;
				}
			}

			return len;
		}

		public CharSequence terminateToken(CharSequence text) {
			int i = text.length();

			while (i > 0 && text.charAt(i - 1) == ' ') {
				i--;
			}

			if (i > 0 && text.charAt(i - 1) == ' ') {
				return text;
			} else {
				if (text instanceof Spanned) {
					SpannableString sp = new SpannableString(text + " ");
					TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
							Object.class, sp, 0);
					return sp;
				} else {
					return text + " ";
				}
			}
		}
	}

	void enterUrl() {
		getUrl = (MultiAutoCompleteTextView) findViewById(R.id.enterUrl);
		getUrl.setPadding(tenPad, 0, tenPad, 0);
		getUrl.setTextColor(getResources().getColor(android.R.color.black));
		getUrl.setPadding(tenPad, 0, tenPad, 0);
		getUrl.setBackgroundResource(R.drawable.book);
		getUrl.setPadding(tenPad, 0, tenPad, 0);
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 1: {
					adapter = new SimpleAdapter(Barebones.this, list,
							R.layout.two_line_autocomplete, new String[] {
									"title", "url" }, new int[] { R.id.title,
									R.id.url });
					getUrl.setAdapter(adapter);

					break;
				}
				case 2: {

					break;
				}
				}
			}
		};

		Thread updateAutoComplete = new Thread(new Runnable() {

			@Override
			public void run() {

				// bookmarks = Browser.BOOKMARKS_URI;

				columns = new String[] { "url", "title" };

				try {

					managedCursor = null;
					SQLiteDatabase s = historyHandler.getReadableDatabase();
					managedCursor = s.query("history", // URI
														// of
							columns, // Which columns to return
							null, // Which rows to return (all rows)
							null, // Selection arguments (none)
							null, null, null);
					handler.sendEmptyMessage(1);

				} catch (SQLiteException e) {
					handler.sendEmptyMessage(2);
					Log.e("Barebones: ", "SQLite Error!!! " + e);
				}

				list = new ArrayList<Map<String, String>>();
				noStockBrowser = true;
				if (managedCursor != null) {
					noStockBrowser = false;

					if (managedCursor.moveToFirst()) {

						// Variable for holding the retrieved URL

						urlColumn = managedCursor.getColumnIndex("url");
						titleColumn = managedCursor.getColumnIndex("title");
						// Reference to the the column containing the URL
						do {
							urlA = managedCursor.getString(urlColumn);
							title = managedCursor.getString(titleColumn);
							map = new HashMap<String, String>();
							map.put("title", title);
							map.put("url", urlA);
							list.add(map);
						} while (managedCursor.moveToNext());
					}
				}

			}

		});

		updateAutoComplete.start();

		getUrl.setThreshold(2);

		getUrl.setTokenizer(new SpaceTokenizer());
		getUrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				try {
					txt = (TextView) arg1.findViewById(R.id.url);
					str = txt.getText().toString();
					if (!pageIdIsVisible && isBookmarkShowing) {
						scrollBookmarks.startAnimation(fadeOut);
						background.removeView(scrollBookmarks);
						background.addView(main[pageId]);
						pageIdIsVisible = true;
						isBookmarkShowing = false;
					}
					main[pageId].loadUrl(str);
					getUrl.setText(str);
					getUrl.setPadding(tenPad, 0, tenPad, 0);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
				} catch (NullPointerException e) {

					Log.e("Barebones Error: ",
							"NullPointerException on item click");
				}
			}

		});

		getUrl.setSelectAllOnFocus(true); // allows edittext to select all when
											// clicked
	}

	// new tab method, takes the id of the tab to be created and the url to load
	void newTab(int theId, String theUrl) {
		lastVisibleWebView = pageId;
		if (isBookmarkShowing) {
			background.addView(main[pageId]);
			main[pageId].startAnimation(fadeIn);
			scrollBookmarks.startAnimation(fadeOut);
			background.removeView(scrollBookmarks);
			uBar.bringToFront();
			isBookmarkShowing = false;
		}
		pageIdIsVisible = false;
		homepage = settings.getString("home", "http://www.google.com");
		allowLocation = settings.getBoolean("location", false);
		final LinearLayout tabLayout = (LinearLayout) findViewById(R.id.tabLayout);
		boolean isEmptyWebViewAvailable = false;

		for (int num = 0; num < number; num++) {
			if (urlTitle[num].getVisibility() == View.GONE) {
				urlTitle[num].setVisibility(View.VISIBLE);
				urlTitle[num].setText("Google");
				if (API < 16) {
					urlTitle[num].setBackgroundDrawable(getResources()
							.getDrawable(R.drawable.bg_press));
				} else {
					urlTitle[num].setBackground(getResources().getDrawable(
							R.drawable.bg_press));
				}
				urlTitle[num].setPadding(leftPad, 0, rightPad, 0);
				if (API < 16) {
					urlTitle[pageId].setBackgroundDrawable(getResources()
							.getDrawable(R.drawable.bg_inactive));
				} else {
					urlTitle[pageId].setBackground(getResources().getDrawable(
							R.drawable.bg_inactive));
				}
				urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
				background.addView(main[num]);
				background.removeView(main[pageId]);

				uBar.bringToFront();
				main[num] = settings(main[num]);
				main[num].loadUrl(theUrl);
				pageId = num;
				pageIdIsVisible = true;
				isEmptyWebViewAvailable = true;
				break;
			}
		}
		if (isEmptyWebViewAvailable == false) {
			if (number < MAX_TABS) {
				pageIdIsVisible = false;
				background.removeView(main[pageId]);

				if (number > 0) {
					if (API < 16) {
						urlTitle[pageId].setBackgroundDrawable(getResources()
								.getDrawable(R.drawable.bg_inactive));
					} else {
						urlTitle[pageId].setBackground(getResources()
								.getDrawable(R.drawable.bg_inactive));
					}
					urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
				}
				final TextView title = new TextView(Barebones.this);
				title.setText("Google");
				if (API < 16) {
					title.setBackgroundDrawable(getResources().getDrawable(
							R.drawable.bg_press));
				} else {
					title.setBackground(getResources().getDrawable(
							R.drawable.bg_press));
				}

				title.setSingleLine(true);
				title.setGravity(Gravity.CENTER_VERTICAL);
				title.setHeight(height32);
				title.setWidth(pixels);
				title.setPadding(leftPad, 0, rightPad, 0);
				title.setId(number);
				title.setGravity(Gravity.CENTER_VERTICAL);
				title.setCompoundDrawables(null, null, exitTab, null);
				Drawable[] drawables = title.getCompoundDrawables();
				bounds[number] = drawables[2].getBounds();
				title.setOnLongClickListener(Barebones.this);
				title.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						id = arg0.getId();
						if (API < 16) {
							urlTitle[pageId]
									.setBackgroundDrawable(getResources()
											.getDrawable(R.drawable.bg_inactive));
						} else if (API > 15) {
							urlTitle[pageId].setBackground(getResources()
									.getDrawable(R.drawable.bg_inactive));
						}
						urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);

						if (isBookmarkShowing) {

							background.addView(main[id]);
							main[id].startAnimation(fadeIn);
							scrollBookmarks.startAnimation(fadeOut);
							background.removeView(scrollBookmarks);
							isBookmarkShowing = false;
							uBar.bringToFront();
						} else if (!isBookmarkShowing) {
							if (!showFullScreen) {
								background.addView(main[id]);
								main[id].startAnimation(fadeIn);
								main[pageId].startAnimation(fadeOut);
								pageIdIsVisible = false;
								background.removeView(main[pageId]);
								uBar.bringToFront();
							} else if (API >= 12) {
								pageIdIsVisible = false;
								main[id].setAlpha(0f);
								main[id].clearAnimation();
								background.addView(main[id]);
								main[id].animate().alpha(1f)
										.setDuration(mShortAnimationDuration);
								main[pageId].clearAnimation();
								// main[pageId].animate().alpha(0f)
								// .setDuration(mShortAnimationDuration);

								// main[pageId].setAlpha(1f);
								background.removeView(main[pageId]);

								uBar.bringToFront();
							} else {
								pageIdIsVisible = false;
								background.removeView(main[pageId]);
								background.addView(main[id]);
							}
							uBar.bringToFront();
						}
						pageId = id;
						pageIdIsVisible = true;
						getUrl.setText(urlToLoad[pageId][0]);
						getUrl.setPadding(tenPad, 0, tenPad, 0);
						if (API < 16) {
							urlTitle[pageId]
									.setBackgroundDrawable(getResources()
											.getDrawable(R.drawable.bg_press));
						} else if (API > 15) {
							urlTitle[pageId].setBackground(getResources()
									.getDrawable(R.drawable.bg_press));
						}
						urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
					}

				});
				title.setOnTouchListener(Barebones.this);
				tabLayout.addView(title);
				urlTitle[number] = title;
				pageId = number;
				if (theUrl != null) {
					makeTab(number, theUrl);
				} else {
					makeTab(number, homepage);
				}
				number = number + 1;
			}
		}
		if (isEmptyWebViewAvailable == false && number >= MAX_TABS) {
			Toast.makeText(Barebones.this, "Maximum number of tabs reached...",
					Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		main[pageId].getSettings().setLayoutAlgorithm(
				LayoutAlgorithm.NARROW_COLUMNS);
		main[pageId].invalidate();
	}

	public void makeTab(final int pageToView, String Url) {
		AnthonyWebView newTab = new AnthonyWebView(Barebones.this);
		main[pageToView] = newTab;
		main[pageToView].setId(pageToView);

		allowLocation = settings.getBoolean("location", false);
		main[pageToView].setWebViewClient(new AnthonyWebViewClient());
		main[pageToView].setWebChromeClient(new AnthonyChromeClient());
		if (API > 8) {
			main[pageToView].setDownloadListener(new AnthonyDownload());
		}
		main[pageToView].requestFocus();
		// main[pageToView].setFocusable(true);
		main[pageToView].setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {

				final HitTestResult result = main[pageId].getHitTestResult();
				boolean image = false;
				if (result.getType() == HitTestResult.IMAGE_TYPE && API > 8) {
					image = true;
				}

				if (result.getExtra() != null) {
					if (image) {
						DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case DialogInterface.BUTTON_POSITIVE: {
									int num;
									num = pageId;
									newTab(number, result.getExtra());

									urlTitle[num].performClick();
									pageId = num;
									break;
								}
								case DialogInterface.BUTTON_NEGATIVE: {
									main[pageId].loadUrl(result.getExtra());
									break;
								}
								case DialogInterface.BUTTON_NEUTRAL: {
									if (API > 8) {
										DownloadManager download = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
										Uri nice = Uri.parse(result.getExtra());
										DownloadManager.Request it = new DownloadManager.Request(
												nice);
										String fileName = result
												.getExtra()
												.substring(
														result.getExtra()
																.lastIndexOf(
																		'/') + 1,
														result.getExtra()
																.length());
										it.setDestinationInExternalPublicDir(
												Environment.DIRECTORY_DOWNLOADS,
												fileName);
										Log.i("Barebones", "Downloading"
												+ fileName);
										download.enqueue(it);
									}
									break;
								}
								}
							}
						};

						AlertDialog.Builder builder = new AlertDialog.Builder(
								Barebones.this); // dialog
						builder.setMessage(
								"What would you like to do with this link?")
								.setPositiveButton("Open in New Tab",
										dialogClickListener)
								.setNegativeButton("Open Normally",
										dialogClickListener)
								.setNeutralButton("Download Image",
										dialogClickListener).show();

					} else {
						DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case DialogInterface.BUTTON_POSITIVE: {
									int num = pageId;
									newTab(number, result.getExtra());

									urlTitle[num].performClick();
									pageId = num;
									break;
								}
								case DialogInterface.BUTTON_NEGATIVE: {
									main[pageId].loadUrl(result.getExtra());
									break;
								}
								case DialogInterface.BUTTON_NEUTRAL: {

									if (API < 11) {
										android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
										clipboard.setText(main[pageId].getUrl());
									} else {
										ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
										ClipData clip = ClipData.newPlainText(
												"label", main[pageId].getUrl());
										clipboard.setPrimaryClip(clip);
									}
									break;
								}
								}
							}
						};

						AlertDialog.Builder builder = new AlertDialog.Builder(
								Barebones.this); // dialog
						builder.setMessage(
								"What would you like to do with this link?")
								.setPositiveButton("Open in New Tab",
										dialogClickListener)
								.setNegativeButton("Open Normally",
										dialogClickListener)
								.setNeutralButton("Copy link",
										dialogClickListener).show();
					}
					return true;
				} else {
					return false;
				}

			}

		});

		main[pageToView] = settings(main[pageToView]);
		agentPicker = settings.getInt("agentchoose", 1);
		switch (agentPicker) {
		case 1:
			main[pageToView].getSettings().setUserAgentString(mobile);
			Log.i("barebones", mobile);
			break;
		case 2:
			main[pageToView].getSettings().setUserAgentString(desktop);
			Log.i("barebones", desktop);
			break;
		case 3:
			userAgent = settings.getString("agent", user);
			main[pageToView].getSettings().setUserAgentString(userAgent);
			Log.i("barebones", userAgent);
			break;
		}
		background.addView(main[pageToView]);
		// main[pageToView].startAnimation(fadeIn);
		if (lastVisibleWebView != pageToView) {
			// main[lastVisibleWebView].startAnimation(fadeOut);
			pageIdIsVisible = false;
			background.removeView(main[lastVisibleWebView]);
		}
		uBar.bringToFront();
		if (Url.contains("about:home") && !showFullScreen) {
			pageIdIsVisible = false;
			goBookmarks();
		} else if (Url.contains("about:home")) {
			pageIdIsVisible = true;
			main[pageToView].loadUrl("about:blank");

		} else if (Url.contains("about:blank")) {
			pageIdIsVisible = true;
			main[pageToView].loadUrl("about:blank");

		} else {
			pageIdIsVisible = true;
			main[pageToView].loadUrl(Url);

		}

	}

	private class AnthonyWebViewClient extends WebViewClient {

		@Override
		public void doUpdateVisitedHistory(WebView view, String url,
				boolean isReload) {

			return;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			pageIdIsVisible = true;
			return super.shouldOverrideUrlLoading(view, url);

		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			// handler.proceed(username, password);
			super.onReceivedHttpAuthRequest(view, handler, host, realm);
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {

			Log.e("Barebones: ", "Error: " + description);

			super.onReceivedError(view, errorCode, description, failingUrl);
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {

			handler.proceed();
			super.onReceivedSslError(view, handler, error);
		}

		@Override
		public void onReceivedLoginRequest(WebView view, String realm,
				String account, String args) {

			super.onReceivedLoginRequest(view, realm, account, args);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			int num = view.getId();

			pageIsLoading = true;
			getUrl.setText(url);
			refresh.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
			getUrl.setPadding(tenPad, 0, tenPad, 0);
			urlToLoad[num][0] = url;
			urlTitle[num].setCompoundDrawables(webpageOther, null, exitTab,
					null);

			if (uBarShows == false) {
				uBar.startAnimation(slideDown);
				uBarShows = true;
			}
		}

		@Override
		public void onPageFinished(WebView view, final String url) {
			progressBar.setVisibility(View.GONE);
			refresh.setVisibility(View.VISIBLE);
			if (!noStockBrowser) {
				Thread history = new Thread(new Runnable() {
					@Override
					public void run() {
						Browser.updateVisitedHistory(getContentResolver(), url,
								true);
					}

				});
				history.start();
			}
			pageIsLoading = false;

		}
	}

	private class AnthonyDownload implements DownloadListener {

		@Override
		public void onDownloadStart(String url, String userAgent,
				String contentDisposition, String mimetype, long contentLength) {
			if (contentDisposition == null
	                || !contentDisposition.regionMatches(
	                        true, 0, "attachment", 0, 10)) {
	            // query the package manager to see if there's a registered handler
	            //     that matches.
	            Intent intent = new Intent(Intent.ACTION_VIEW);
	            intent.setDataAndType(Uri.parse(url), mimetype);
	            ResolveInfo info = getPackageManager().resolveActivity(intent,
	                    PackageManager.MATCH_DEFAULT_ONLY);
	            if (info != null) {
	                ComponentName myName = getComponentName();
	                // If we resolved to ourselves, we don't want to attempt to
	                // load the url only to try and download it again.
	                if (!myName.getPackageName().equals(
	                        info.activityInfo.packageName)
	                        || !myName.getClassName().equals(
	                                info.activityInfo.name)) {
	                    // someone (other than us) knows how to handle this mime
	                    // type with this scheme, don't download.
	                    try {
	                        startActivity(intent);
	                        return;
	                    } catch (ActivityNotFoundException ex) {
	                        
	                            Log.d("Barebones", "activity not found for " + mimetype
	                                    + " over " + Uri.parse(url).getScheme(),
	                                    ex);
	                        
	                        // Best behavior is to fall back to a download in this
	                        // case
	                            try {
	                				DownloadManager download = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
	                				Uri nice = Uri.parse(url);
	                				DownloadManager.Request it = new DownloadManager.Request(nice);
	                				 String fileName = URLUtil.guessFileName(url,
	                			                contentDisposition, mimetype);
	                				it.setDestinationInExternalPublicDir(
	                						Environment.DIRECTORY_DOWNLOADS, fileName);
	                				Log.i("Barebones", "Downloading" + fileName);
	                				download.enqueue(it);
	                			} catch (NullPointerException e) {
	                				Log.e("Barebones", "Problem downloading");
	                				Toast.makeText(Barebones.this, "Error Downloading File",
	                						Toast.LENGTH_SHORT).show();
	                			}
	                			catch (IllegalArgumentException e){
	                				Log.e("Barebones", "Problem downloading");
	                				Toast.makeText(Barebones.this, "Error Downloading File",
	                						Toast.LENGTH_SHORT).show();
	                			}
	                    }
	                }
	            }
	        }
			
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == FILECHOOSER_RESULTCODE) {
			if (null == mUploadMessage)
				return;
			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;

		}
	}

	protected class AnthonyChromeClient extends WebChromeClient {
		private Bitmap mDefaultVideoPoster;
		private View mVideoProgressView;

		@Override
		public void onReceivedIcon(WebView view, Bitmap favicon) {

			icon = null;
			icon = new BitmapDrawable(getResources(), favicon);
			int num = view.getId();
			icon.setBounds(0, 0, width * 1 / 2, height * 1 / 2);
			if (icon != null) {
				urlTitle[num].setCompoundDrawables(icon, null, exitTab, null);
			} else {
				urlTitle[num].setCompoundDrawables(webpageOther, null, exitTab,
						null);
			}
			super.onReceivedIcon(view, favicon);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType, String capture) {
			mUploadMessage = uploadMsg;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("image/*");
			Barebones.this.startActivityForResult(
					Intent.createChooser(i, "Image Browser"),
					FILECHOOSER_RESULTCODE);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType) {
			mUploadMessage = uploadMsg;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("image/*");
			Barebones.this.startActivityForResult(
					Intent.createChooser(i, "Image Browser"),
					FILECHOOSER_RESULTCODE);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg) {

			mUploadMessage = uploadMsg;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("image/*");
			Barebones.this.startActivityForResult(
					Intent.createChooser(i, "Image Browser"),
					FILECHOOSER_RESULTCODE);
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(final String origin,
				final GeolocationPermissions.Callback callback) {

			if (allowLocation == true) {
				callback.invoke(origin, true, false);
			} else {
				Log.i("Barebones: ", "onGeolocationPermissionsShowPrompt()");

				final boolean remember = true;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Barebones.this);
				builder.setTitle("Locations");
				builder.setMessage(
						origin + " Would like to use your Current Location ")
						.setCancelable(true)
						.setPositiveButton("Allow",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										// origin, allow, remember
										callback.invoke(origin, true, remember);
									}
								})
						.setNegativeButton("Don't Allow",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										// origin, allow, remember
										callback.invoke(origin, false, remember);
									}
								});
				AlertDialog alert = builder.create();
				// alert.show();
			}
		}

		@Override
		public void onReceivedTitle(final WebView view, final String title) {

			int num = view.getId();
			urlTitle[num].setText(title);
			urlToLoad[num][1] = title;
			Thread up = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						StringBuilder sb = new StringBuilder("url" + " = ");
						DatabaseUtils.appendEscapedSQLString(sb, view.getUrl());
						SQLiteDatabase s = historyHandler.getReadableDatabase();
						Cursor c = s.query("history", new String[] { "id",
								"url", "title" }, sb.toString(), null, null,
								null, null);
						if (c.moveToFirst()) {

						} else {
							historyHandler.addHistoryItem(new HistoryItem(view
									.getUrl(), title));
						}

					} catch (IllegalStateException e) {
						Log.e("Barebones", "ERRRRROOORRRR 1");
					} catch (NullPointerException e) {
						Log.e("Barebones", "ERRRRROOORRRR 2");
					}
				}
			});
			up.run();
			super.onReceivedTitle(view, title);
		}

		@Override
		public void onShowCustomView(View view,
				WebChromeClient.CustomViewCallback callback) {
			// Log.i(LOGTAG, "here in on ShowCustomView");
			main[pageId].setVisibility(View.GONE);

			// if a view already exists then immediately terminate the new one
			if (mCustomView != null) {
				callback.onCustomViewHidden();
				return;
			}

			background.addView(view);
			mCustomView = view;
			mCustomViewCallback = callback;
			background.setVisibility(View.VISIBLE);
		}

		@Override
		public void onHideCustomView() {

			if (mCustomView == null)
				return;

			// Hide the custom view.
			mCustomView.setVisibility(View.GONE);

			// Remove the custom view from its container.
			background.removeView(mCustomView);
			mCustomView = null;
			background.setVisibility(View.VISIBLE);
			mCustomViewCallback.onCustomViewHidden();

			main[pageId].setVisibility(View.VISIBLE);

			// Log.i(LOGTAG, "set it to webVew");
		}

		@Override
		public Bitmap getDefaultVideoPoster() {
			// Log.i(LOGTAG, "here in on getDefaultVideoPoster");
			if (mDefaultVideoPoster == null) {
				mDefaultVideoPoster = BitmapFactory.decodeResource(
						getResources(), android.R.color.black);
			}
			return mDefaultVideoPoster;
		}

		@Override
		public View getVideoLoadingProgressView() {
			// Log.i(LOGTAG, "here in on getVideoLoadingPregressView");

			if (mVideoProgressView == null) {
				LayoutInflater inflater = LayoutInflater.from(getBaseContext());
				mVideoProgressView = inflater.inflate(
						android.R.layout.simple_spinner_item, null);
			}
			return mVideoProgressView;
		}
	}

	private AnthonyWebView settings(AnthonyWebView view) {
		WebSettings webViewSettings = view.getSettings();
		java = settings.getBoolean("java", true);
		if (java) {
			webViewSettings.setJavaScriptEnabled(true);
			webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		}

		webViewSettings.setAllowFileAccess(true);
		webViewSettings.setLightTouchEnabled(true);
		view.setAnimationCacheEnabled(false);
		// view.setDrawingCacheEnabled(true);
		view.setDrawingCacheBackgroundColor(getResources().getColor(
				android.R.color.background_light));
		// view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		// view.setAlwaysDrawnWithCacheEnabled(true);
		view.setWillNotCacheDrawing(true);
		// view.setPersistentDrawingCache(ViewGroup.PERSISTENT_ALL_CACHES);
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.setSaveEnabled(true);
		webViewSettings.setDomStorageEnabled(true);
		webViewSettings.setAppCacheEnabled(true);
		webViewSettings.setAppCachePath(getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/cache");
		webViewSettings.setRenderPriority(RenderPriority.HIGH);
		webViewSettings.setGeolocationEnabled(true);
		webViewSettings.setGeolocationDatabasePath(getApplicationContext()
				.getFilesDir().getAbsolutePath());

		webViewSettings.setDatabaseEnabled(true);
		webViewSettings.setDatabasePath(getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/databases");
		enableFlash = settings.getInt("enableflash", 0);
		switch (enableFlash) {
		case 0:
			break;
		case 1: {
			webViewSettings.setPluginState(PluginState.ON_DEMAND);
			break;
		}
		case 2: {
			webViewSettings.setPluginState(PluginState.ON);
			break;
		}
		default:
			break;
		}

		webViewSettings.setUserAgentString(userAgent);
		savePasswords = settings.getBoolean("passwords", false);
		if (savePasswords == true) {
			webViewSettings.setSavePassword(true);
		}

		webViewSettings.setBuiltInZoomControls(true);
		webViewSettings.setSupportZoom(true);
		webViewSettings.setUseWideViewPort(true);
		webViewSettings.setLoadWithOverviewMode(true); // Seems to be causing
														// the performance
														// to drop
		if (API >= 11) {
			webViewSettings.setDisplayZoomControls(false);
			webViewSettings.setAllowContentAccess(true);
		}
		webViewSettings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
		webViewSettings.setLoadsImagesAutomatically(true);
		// webViewSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		return view;
	}

	public void openBookmarks() {
		scrollBookmarks = new ScrollView(Barebones.this);
		RelativeLayout.LayoutParams g = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		g.addRule(RelativeLayout.BELOW, R.id.relativeLayout1);
		scrollBookmarks.setLayoutParams(g);
		LinearLayout bookmarkLayout = new LinearLayout(Barebones.this);
		bookmarkLayout.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		bookmarkLayout.setOrientation(LinearLayout.VERTICAL);
		TextView description = new TextView(Barebones.this);
		description.setHeight(height56);
		description.setBackgroundColor(0xff33b5e5);
		description.setTextColor(0xffffffff);
		description.setText("Bookmarks");
		description.setGravity(Gravity.CENTER_VERTICAL
				| Gravity.CENTER_HORIZONTAL);
		description.setTextSize(bookHeight / 3);
		description.setPadding(rightPad, 0, rightPad, 0);
		bookmarkLayout.addView(description);

		for (int n = 0; n < MAX_BOOKMARKS; n++) {
			if (bUrl[n] != null) {
				TextView b = new TextView(Barebones.this);
				b.setId(n);
				b.setSingleLine(true);
				b.setGravity(Gravity.CENTER_VERTICAL);
				b.setTextSize(pixelHeight / 3);
				b.setBackgroundResource(R.drawable.bookmark);
				b.setHeight(height56);
				b.setText(bTitle[n]);
				b.setCompoundDrawables(webpage, null, null, null);
				b.setOnClickListener(new BookmarkListener());
				b.setOnLongClickListener(new BookmarkLongClick());
				b.setPadding(rightPad, 0, rightPad, 0);
				bookmarkLayout.addView(b);
			}
		}
		pageIdIsVisible = false;

		if (uBar.isShown()) {
			urlTitle[pageId].setText("Bookmarks");
			getUrl.setText("Bookmarks");
			getUrl.setPadding(tenPad, 0, tenPad, 0);
		}

		// main[pageId].startAnimation(fadeOut);
		background.removeView(main[pageId]);

		if (showFullScreen) {
			// uBar.startAnimation(fadeOut);
			background.removeView(uBar);
		}
		scrollBookmarks.addView(bookmarkLayout);
		background.addView(scrollBookmarks);
		scrollBookmarks.startAnimation(fadeIn);
		isBookmarkShowing = true;

	}

	class BookmarkLongClick implements OnLongClickListener {

		@Override
		public boolean onLongClick(final View arg0) {
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE: {
						int delete = arg0.getId();
						File book = new File(getBaseContext().getFilesDir(),
								"bookmarks");
						File bookUrl = new File(getBaseContext().getFilesDir(),
								"bookurl");
						int n = 0;
						try {
							BufferedWriter bookWriter = new BufferedWriter(
									new FileWriter(book));
							BufferedWriter urlWriter = new BufferedWriter(
									new FileWriter(bookUrl));
							while (bUrl[n] != null && n < (MAX_BOOKMARKS - 1)) {
								if (delete != n) {
									bookWriter.write(bTitle[n]);
									urlWriter.write(bUrl[n]);
									bookWriter.newLine();
									urlWriter.newLine();
								}
								n++;
							}
							bookWriter.close();
							urlWriter.close();
						} catch (FileNotFoundException e) {

							e.printStackTrace();
						} catch (IOException e) {

							e.printStackTrace();
						}
						for (int p = 0; p < MAX_BOOKMARKS; p++) {
							bUrl[p] = null;
							bTitle[p] = null;
						}
						try {
							BufferedReader readBook = new BufferedReader(
									new FileReader(book));
							BufferedReader readUrl = new BufferedReader(
									new FileReader(bookUrl));
							String t, u;
							int z = 0;
							while ((t = readBook.readLine()) != null
									&& (u = readUrl.readLine()) != null
									&& z < MAX_BOOKMARKS) {
								bUrl[z] = u;
								bTitle[z] = t;
								z++;
							}
							readBook.close();
							readUrl.close();
						} catch (FileNotFoundException e) {

							e.printStackTrace();
						} catch (IOException e) {

							e.printStackTrace();

						}
						// scrollBookmarks.startAnimation(fadeOut);
						background.removeView(scrollBookmarks);
						isBookmarkShowing = false;
						openBookmarks();

						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {

						break;
					}
					default:

						break;
					}

				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(
					Barebones.this); // dialog
			builder.setMessage("Do you want to delete this bookmark?")
					.setPositiveButton("Yes", dialogClickListener)
					.setNegativeButton("No", dialogClickListener).show();
			return allowLocation;

		}

	}

	static class BookmarkListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			int number = arg0.getId();
			background.addView(main[pageId]);
			pageIdIsVisible = true;
			main[pageId].startAnimation(fadeIn);
			if (showFullScreen) {
				background.addView(uBar);
				uBar.startAnimation(fadeIn);
			}
			scrollBookmarks.startAnimation(fadeOut);
			background.removeView(scrollBookmarks);
			isBookmarkShowing = false;

			uBar.bringToFront();
			main[pageId].loadUrl(bUrl[number]);
		}

	}
	public void addBookmark() {
		File book = new File(getBaseContext().getFilesDir(), "bookmarks");
		File bookUrl = new File(getBaseContext().getFilesDir(), "bookurl");
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book,
					true));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl, true));
			bookWriter.write(urlToLoad[pageId][1]);
			urlWriter.write(urlToLoad[pageId][0]);
			bookWriter.newLine();
			urlWriter.newLine();
			bookWriter.close();
			urlWriter.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	public void goBookmarks() {
		File book = new File(getBaseContext().getFilesDir(), "bookmarks");
		File bookUrl = new File(getBaseContext().getFilesDir(), "bookurl");
		try {
			BufferedReader readBook = new BufferedReader(new FileReader(book));
			BufferedReader readUrl = new BufferedReader(new FileReader(bookUrl));
			String t, u;
			int n = 0;
			while ((t = readBook.readLine()) != null
					&& (u = readUrl.readLine()) != null && n < MAX_BOOKMARKS) {
				bUrl[n] = u;
				bTitle[n] = t;
				n++;
			}
			readBook.close();
			readUrl.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();

		}
		openBookmarks();
	}

	@Override
	public boolean onLongClick(View v) {
		int id = v.getId();
		if (pageId == id && isBookmarkShowing) {

			background.addView(main[pageId]);
			// main[pageId].startAnimation(fadeIn);
			if (showFullScreen) {
				background.addView(uBar);
				// uBar.startAnimation(fadeIn);
			}
			// scrollBookmarks.startAnimation(fadeOut);
			background.removeView(scrollBookmarks);
			uBar.bringToFront();
			isBookmarkShowing = false;
		}
		pageIdIsVisible = true;
		deleteTab(id);

		return true;

	}

	public void deleteTab(int id) {
		int leftId = id;
		pageIdIsVisible = false;
		main[id].stopLoading();
		if (API >= 11) {
			main[id].onPause();
		}
		main[id].clearView();
		boolean right = false, left = false;
		// background.clearDisappearingChildren();
		if (API < 16) {
			urlTitle[id].setBackgroundDrawable(getResources().getDrawable(
					R.drawable.bg_press));
		} else {
			urlTitle[id].setBackground(getResources().getDrawable(
					R.drawable.bg_press));
		}
		urlTitle[id].setPadding(leftPad, 0, rightPad, 0);
		urlTitle[id].setVisibility(View.GONE);
		if (id == pageId) {

			if (isBookmarkShowing) {
				if (showFullScreen) {
					background.addView(uBar);
					// uBar.startAnimation(fadeIn);
					uBar.bringToFront();
				}
				// scrollBookmarks.startAnimation(fadeOut);
				background.removeView(scrollBookmarks);
				uBar.bringToFront();
				isBookmarkShowing = false;

			} else if (main[id].isShown()) {
				background.removeView(main[id]);
			}
			for (; id <= (number - 1); id++) {
				if (urlTitle[id].isShown()) {
					background.addView(main[id]);
					main[id].setVisibility(View.VISIBLE);
					uBar.bringToFront();
					if (API < 16) {
						urlTitle[id].setBackgroundDrawable(getResources()
								.getDrawable(R.drawable.bg_press));
					} else {
						urlTitle[id].setBackground(getResources().getDrawable(
								R.drawable.bg_press));
					}
					urlTitle[id].setPadding(leftPad, 0, rightPad, 0);
					pageId = id;
					getUrl.setText(urlToLoad[pageId][0]);
					getUrl.setPadding(tenPad, 0, tenPad, 0);
					right = true;
					break;
				}

			}
			if (right == false) {
				for (; leftId >= 0; leftId--) {

					if (urlTitle[leftId].isShown()) {
						background.addView(main[leftId]);
						main[leftId].setVisibility(View.VISIBLE);
						// uBar.bringToFront();
						if (API < 16) {
							urlTitle[leftId]
									.setBackgroundDrawable(getResources()
											.getDrawable(R.drawable.bg_press));
						} else {
							urlTitle[leftId].setBackground(getResources()
									.getDrawable(R.drawable.bg_press));
						}
						urlTitle[leftId].setPadding(leftPad, 0, rightPad, 0);
						pageId = leftId;
						getUrl.setText(urlToLoad[pageId][0]);
						getUrl.setPadding(tenPad, 0, tenPad, 0);
						left = true;
						break;
					}

				}

			}

		} else {
			right = left = true;
		}

		if (right == false && left == false) {
			finish();
		} else {
			pageIdIsVisible = true;
			main[pageId].invalidate();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem refresh = menu.findItem(R.id.refresh);

		if (main[pageId].getProgress() < 100) {
			refresh.setTitle("Stop");
		} else {
			refresh.setTitle("Refresh");
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.refresh:
			if (main[pageId].getProgress() < 100) {
				main[pageId].stopLoading();
			} else {
				main[pageId].reload();
			}
			return true;
		case R.id.bookmark:
			if (!isBookmarkShowing) {
				addBookmark();
			}
			return true;
		case R.id.settings:
			newSettings();
			return true;
		case R.id.allBookmarks:
			if (!isBookmarkShowing) {
				goBookmarks();
			}
			return true;
		case R.id.share:
			share();
			return true;
		case R.id.forward:
			if (main[pageId].canGoForward()) {
				main[pageId].goForward();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void newSettings() {
		Intent set = new Intent("android.intent.action.BAREBONESSETTINGS");
		startActivity(set);
	}

	public void share() {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

		// set the type
		shareIntent.setType("text/plain");

		// add a subject
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				urlToLoad[pageId][1]);

		// build the body of the message to be shared
		String shareMessage = urlToLoad[pageId][0];

		// add the message
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);

		// start the chooser for sharing
		startActivity(Intent.createChooser(shareIntent, "Share this page"));
	}

	public void options() {
		ImageView options = (ImageView) findViewById(R.id.options);
		options.setBackgroundResource(R.drawable.button);
		options.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (API >= 11) {
					PopupMenu menu = new PopupMenu(Barebones.this, v);
					MenuInflater inflate = menu.getMenuInflater();
					inflate.inflate(R.menu.menu, menu.getMenu());
					menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {

							switch (item.getItemId()) {
							case R.id.refresh:
								if (main[pageId].getProgress() < 100) {
									main[pageId].stopLoading();
								} else {
									main[pageId].reload();
								}
								return true;
							case R.id.bookmark:
								if (!isBookmarkShowing) {
									addBookmark();
								}
								return true;
							case R.id.settings:
								newSettings();
								return true;
							case R.id.allBookmarks:
								if (!isBookmarkShowing) {
									goBookmarks();
								}
								return true;
							case R.id.share:
								share();
								return true;
							case R.id.forward:
								if (main[pageId].canGoForward()) {
									main[pageId].goForward();
								}
								return true;
							default:
								return false;
							}

						}

					});
					menu.show();
				} else if (API < 11) {
					/*
					 * LayoutInflater
					 * inflater=(LayoutInflater)Barebones.this.getSystemService
					 * (Context.LAYOUT_INFLATER_SERVICE); Display
					 * display=getWindowManager().getDefaultDisplay();
					 * 
					 * int width=display.getWidth()/2; int
					 * height=display.getHeight()/2;
					 * 
					 * View pop = inflater.inflate(R.layout.menu,null,false);
					 * pop
					 * .measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.
					 * UNSPECIFIED); height=pop.getMeasuredHeight();
					 * width=pop.getMeasuredWidth(); PopupWindow pu = new
					 * PopupWindow(pop,width,height,true);
					 * pu.showAtLocation(findViewById
					 * (v.getId()),Gravity.NO_GRAVITY
					 * ,v.getRight(),v.getBottom()+80);
					 */

					openOptionsMenu();
				}
			}

		});
		options.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {

				return true;
			}

		});
	}

	public void enter() {
		getUrl.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

				switch (arg1) {
				case KeyEvent.KEYCODE_ENTER:
					query = getUrl.getText().toString();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					testForSearch();
					return true;
				default:
					break;
				}
				return false;
			}

		});
		getUrl.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView arg0, int actionId,
					KeyEvent arg2) {
				if (actionId == EditorInfo.IME_ACTION_GO
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT
						|| actionId == EditorInfo.IME_ACTION_SEND
						|| actionId == EditorInfo.IME_ACTION_SEARCH
						|| (arg2.getAction() == KeyEvent.KEYCODE_ENTER)) {
					query = getUrl.getText().toString();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					testForSearch();
					return true;
				}
				return false;
			}

		});
	}

	public void testForSearch() {
		String fixedQuery = query.trim();
		boolean period = fixedQuery.contains(".");
		if (isBookmarkShowing) {
			scrollBookmarks.startAnimation(fadeOut);
			background.removeView(scrollBookmarks);
			isBookmarkShowing = false;
			background.addView(main[pageId]);
			main[pageId].startAnimation(fadeIn);
			uBar.bringToFront();

		}
		pageIdIsVisible = true;
		if (fixedQuery.contains("about:home")) {
			goBookmarks();
		} else if (fixedQuery.contains(" ") || period == false) {
			fixedQuery.replaceAll(" ", "+");
			main[pageId]
					.loadUrl("http://www.google.com/search?q=" + fixedQuery);
		} else if (!fixedQuery.contains("http//")
				&& !fixedQuery.contains("https//")
				&& !fixedQuery.contains("http://")
				&& !fixedQuery.contains("https://")) {
			fixedQuery = "http://" + fixedQuery;
			main[pageId].loadUrl(fixedQuery);
		} else {
			fixedQuery = fixedQuery.replaceAll("http//", "http://");
			fixedQuery = fixedQuery.replaceAll("https//", "https://");
			main[pageId].loadUrl(fixedQuery);
		}
	}

	public void exit() {
		ImageView exit = (ImageView) findViewById(R.id.exit);
		exit.setBackgroundResource(R.drawable.button);
		if (isPhone) {
			RelativeLayout relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);
			relativeLayout1.removeView(exit);
		}
		exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isBookmarkShowing) {
					background.addView(main[pageId]);
					main[pageId].startAnimation(fadeIn);
					scrollBookmarks.startAnimation(fadeOut);
					background.removeView(scrollBookmarks);
					uBar.bringToFront();
					urlTitle[pageId].setText(urlToLoad[pageId][1]);
					getUrl.setText(urlToLoad[pageId][0]);
					getUrl.setPadding(tenPad, 0, tenPad, 0);
					pageIdIsVisible = true;
					isBookmarkShowing = false;
				} else {
					if (main[pageId].canGoBack()) {
						main[pageId].goBack();
					} else {
						deleteTab(pageId);
					}
				}
			}

		});
		exit.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				finish();
				return true;
			}

		});

	}

	public void forward() {
		ImageView forward = (ImageView) findViewById(R.id.forward);
		forward.setBackgroundResource(R.drawable.button);
		if (isPhone) {
			RelativeLayout relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);
			relativeLayout1.removeView(forward);
		}
		forward.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (main[pageId].canGoForward()) {
					main[pageId].goForward();
				} else {

				}
			}

		});
	}

	@Override
	public void onBackPressed() {
		if (isBookmarkShowing) {

			if (showFullScreen && !uBar.isShown()) {
				background.addView(uBar);
				uBar.startAnimation(fadeIn);
				uBar.bringToFront();
			}
			background.addView(main[pageId]);
			main[pageId].startAnimation(fadeIn);
			scrollBookmarks.startAnimation(fadeOut);
			background.removeView(scrollBookmarks);
			urlTitle[pageId].setText(urlToLoad[pageId][1]);
			getUrl.setText(urlToLoad[pageId][0]);
			getUrl.setPadding(tenPad, 0, tenPad, 0);
			pageIdIsVisible = true;
			isBookmarkShowing = false;
			uBar.bringToFront();
		} else if (main[pageId].canGoBack()) {
			main[pageId].goBack();
		} else {
			deleteTab(pageId);
			uBar.bringToFront();
		}

	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public void finish() {
		pageIdIsVisible = false;
		try {

			deleteHistory = settings.getBoolean("history", false);
			if (deleteHistory == true) {
				this.deleteDatabase("historyManager");
				if (!noStockBrowser) {
					Browser.clearHistory(getContentResolver());
				}
			}
			// trimCache(this);
		} catch (Exception e) {

			e.printStackTrace();
		}
		this.onPause();
		super.finish();
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}

	public static void trimCache(Context context) {
		try {
			File dir = context.getCacheDir();
			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception e) {

		}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		pageIdIsVisible = false;
		id = v.getId();
		xPress = false;
		x = (int) event.getX();
		y = (int) event.getY();
		edge = new Rect();
		v.getLocalVisibleRect(edge);

		if (x >= (edge.right - bounds[id].width() - fuzz)
				&& x <= (edge.right - v.getPaddingRight() + fuzz)
				&& y >= (v.getPaddingTop() - fuzz)
				&& y <= (v.getHeight() - v.getPaddingBottom()) + fuzz) {
			xPress = true;
		}

		urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (id == pageId) {
				if (xPress) {
					if (isBookmarkShowing) {
						background.removeView(scrollBookmarks);
						isBookmarkShowing = false;

					} else if (!isBookmarkShowing) {

					}
					deleteTab(id);
					uBar.bringToFront();
				} else if (!xPress) {

				}
			} else if (id != pageId) {
				if (xPress) {
					deleteTab(id);
				} else if (!xPress) {
					if (API < 16) {
						urlTitle[pageId].setBackgroundDrawable(getResources()
								.getDrawable(R.drawable.bg_inactive));
					} else if (API > 15) {
						urlTitle[pageId].setBackground(getResources()
								.getDrawable(R.drawable.bg_inactive));
					}
					urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);

					if (isBookmarkShowing) {

						background.addView(main[id]);
						main[id].startAnimation(fadeIn);
						scrollBookmarks.startAnimation(fadeOut);
						background.removeView(scrollBookmarks);
						isBookmarkShowing = false;
						uBar.bringToFront();
					} else if (!isBookmarkShowing) {
						if (!showFullScreen) {
							background.addView(main[id]);
							main[id].startAnimation(fadeIn);
							main[pageId].startAnimation(fadeOut);
							pageIdIsVisible = false;
							background.removeView(main[pageId]);
							uBar.bringToFront();
						} else if (API >= 12) {
							pageIdIsVisible = false;
							main[id].setAlpha(0f);
							main[id].clearAnimation();
							background.addView(main[id]);
							main[id].animate().alpha(1f)
									.setDuration(mShortAnimationDuration);
							main[pageId].clearAnimation();
							// main[pageId].animate().alpha(0f)
							// .setDuration(mShortAnimationDuration);

							// main[pageId].setAlpha(1f);
							background.removeView(main[pageId]);

							uBar.bringToFront();
						} else {
							pageIdIsVisible = false;
							background.removeView(main[pageId]);
							background.addView(main[id]);
						}
						uBar.bringToFront();
					}
					pageId = id;
					pageIdIsVisible = true;
					getUrl.setText(urlToLoad[pageId][0]);
					getUrl.setPadding(tenPad, 0, tenPad, 0);
				}
			}

			if (API < 16) {
				urlTitle[pageId].setBackgroundDrawable(getResources()
						.getDrawable(R.drawable.bg_press));
			} else if (API > 15) {
				urlTitle[pageId].setBackground(getResources().getDrawable(
						R.drawable.bg_press));
			}
		}
		uBar.bringToFront();
		urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);

		pageIdIsVisible = true;
		return true;
	}

	@Override
	protected void onPause() {
		if (API >= 11) {
			main[pageId].onPause();
		}
		main[pageId].pauseTimers();
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (API >= 11) {
			main[pageId].onResume();
		}
		main[pageId].resumeTimers();
		super.onResume();

	}

	@Override
	protected void onNewIntent(Intent intent) {

		String url = null;
		url = intent.getDataString();
		if (url != null) {
			newTab(number, url);
		}
		super.onNewIntent(intent);
	}

}
