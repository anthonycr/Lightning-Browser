package acr.browser.barebones.activities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import acr.browser.barebones.R;
import acr.browser.barebones.customwebview.IncognitoWebView;
import acr.browser.barebones.databases.DatabaseHandler;
import acr.browser.barebones.databases.SpaceTokenizer;
import acr.browser.barebones.utilities.BookmarkPageVariables;
import acr.browser.barebones.utilities.FinalVariables;
import acr.browser.barebones.utilities.HistoryPageVariables;
import acr.browser.barebones.utilities.Utils;
import acr.browser.barebones.incognitoclasses.IncognitoChromeClient;
import acr.browser.barebones.incognitoclasses.IncognitoDownloadListener;
import acr.browser.barebones.incognitoclasses.IncognitoWebViewClient;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteMisuseException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

@SuppressWarnings("deprecation")
public class IncognitoModeActivity extends Activity implements OnTouchListener {

	public static class ClickHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String url = null;
			url = msg.getData().getString("url");
			handleLongClickOnBookmarks(url, msg.arg1);
		}

	}

	static class Handle extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1: {
				currentTab.loadUrl(getUrl.getText().toString());
				break;
			}
			case 2: {
				// deleteTab(msg.arg1);
				break;
			}
			case 3: {
				currentTab.invalidate();
				break;
			}
			}
			super.handleMessage(msg);
		}

	}

	static class NewTabHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				newTab(homepage, true);
			}
			super.handleMessage(msg);
		}

	}

	private static IncognitoModeActivity ACTIVITY;

	private static int index = 0;

	// constants
	public static final int MAX_TABS = FinalVariables.MAX_TABS;

	public static final int MAX_BOOKMARKS = FinalVariables.MAX_BOOKMARKS;

	// variables

	public static final boolean PAID_VERSION = FinalVariables.PAID_VERSION;
	public static final String HOMEPAGE = FinalVariables.HOMEPAGE;
	public static final int API = FinalVariables.API;
	public static final String SEPARATOR = "\\|\\$\\|SEPARATOR\\|\\$\\|";
	// semi constants
	public static Context mContext;
	public static String SEARCH;

	public static List<Integer> tabList;
	// variables
	public static IncognitoWebView currentTab;
	public static TextView currentTabTitle;

	public static MultiAutoCompleteTextView getUrl;
	public static TextView[] urlTitle;
	public static ProgressBar browserProgress;
	public static IncognitoWebView[] main;
	public static Rect bounds;
	public static long timeTabPressed;
	public static boolean fullScreen;
	public static int[] tabOrder = new int[MAX_TABS];
	public static ValueCallback<Uri> mUploadMessage;
	public static ImageView refresh;
	public static ProgressBar progressBar;
	public static String defaultUser;
	public static Drawable incognitoPage;
	public static Drawable exitTab;
	public static long loadTime = 0;
	public static int currentId = 0;
	public static int height32;
	public static int height;
	public static int width;
	public static int pixels;
	public static int leftPad;
	public static int rightPad;
	public static int id;
	public static int tenPad;
	public static boolean isPhone = false;
	public static boolean showFullScreen = false;
	public static boolean noStockBrowser = true;
	public static SharedPreferences settings;
	public static SharedPreferences.Editor edit;
	public static String user;
	public static String[] bUrl;
	public static String[] bTitle;
	public static String[] columns;
	public static String homepage;
	public static String[][] urlToLoad;
	public static FrameLayout background;
	public static RelativeLayout uBar;
	public static RelativeLayout screen;
	public static HorizontalScrollView tabScroll;
	public static Animation slideUp;
	public static Animation slideDown;
	public static Animation fadeOut;
	public static Animation fadeIn;
	public static CookieManager cookieManager;
	public static Uri bookmarks;
	public static Handler handler, browserHandler;
	public static DatabaseHandler historyHandler;
	public static Drawable inactive;
	public static Drawable active;
	public static LinearLayout tabLayout;

	// creates the tab and returns the ID of the view
	public static int createTab(String theUrl, boolean display) {
		int id = -1;
		for (int n = 0; n < MAX_TABS; n++) {
			if (main[n] == null) {
				id = n;
				break;
			}
		}
		if (id != -1) {
			if (tabList.size() > 0) {
				if (display) {
					if (API < 16) {
						currentTabTitle.setBackgroundDrawable(inactive);
					} else {
						currentTabTitle.setBackground(inactive);
					}
					currentTabTitle.setPadding(leftPad, 0, rightPad, 0);
				}
			}
			final TextView title = new TextView(mContext);
			title.setText(mContext.getResources().getString(R.string.action_new_tab));
			if (display) {
				if (API < 16) {
					title.setBackgroundDrawable(active);
				} else {
					title.setBackground(active);
				}
			} else {
				if (API < 16) {
					title.setBackgroundDrawable(inactive);
				} else {
					title.setBackground(inactive);
				}
			}
			title.setSingleLine(true);
			title.setGravity(Gravity.CENTER_VERTICAL);
			title.setHeight(height32);
			title.setWidth(pixels);
			title.setPadding(leftPad, 0, rightPad, 0);
			title.setId(id);
			title.setGravity(Gravity.CENTER_VERTICAL);

			title.setCompoundDrawables(incognitoPage, null, exitTab, null);

			Drawable[] drawables = title.getCompoundDrawables();
			bounds = drawables[2].getBounds();
			title.setOnTouchListener(ACTIVITY);
			Animation holo = AnimationUtils.loadAnimation(mContext, R.anim.up);
			tabLayout.addView(title);
			title.setVisibility(View.INVISIBLE);
			holo.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationStart(Animation animation) {
					title.setVisibility(View.VISIBLE);
				}

			});
			title.startAnimation(holo);
			urlTitle[id] = title;

			urlTitle[id].setText(mContext.getResources().getString(R.string.action_new_tab));

			if (theUrl != null) {
				main[id] = generateTab(id, theUrl, display);
			} else {
				main[id] = generateTab(id, homepage, display);
			}

		} else {
			Utils.showToast(mContext, "Max number of tabs reached");
		}
		return id;
	}

	public static void deleteBookmark(String url) {
		File book = new File(mContext.getFilesDir(), "bookmarks");
		File bookUrl = new File(mContext.getFilesDir(), "bookurl");
		int n = 0;
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl));
			while (bUrl[n] != null && n < (MAX_BOOKMARKS - 1)) {
				if (!bUrl[n].equalsIgnoreCase(url)) {
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
		} catch (IOException e) {
		}
		for (int p = 0; p < MAX_BOOKMARKS; p++) {
			bUrl[p] = null;
			bTitle[p] = null;
		}
		try {
			BufferedReader readBook = new BufferedReader(new FileReader(book));
			BufferedReader readUrl = new BufferedReader(new FileReader(bookUrl));
			String t, u;
			int z = 0;
			while ((t = readBook.readLine()) != null
					&& (u = readUrl.readLine()) != null && z < MAX_BOOKMARKS) {
				bUrl[z] = u;
				bTitle[z] = t;
				z++;
			}
			readBook.close();
			readUrl.close();
		} catch (IOException ignored) {
		}
		openBookmarks(mContext, currentTab);
	}

	public static void generateHistory(final IncognitoWebView view,
			final Context context) {

		Thread history = new Thread(new Runnable() {

			@Override
			public void run() {
				String historyHtml = HistoryPageVariables.Heading;
				Cursor historyCursor = null;
				String[][] h = new String[50][3];

				try {
					SQLiteDatabase s = historyHandler.getReadableDatabase();
					historyCursor = s.query("history", // URI
														// of
							columns, // Which columns to return
							null, // Which rows to return (all rows)
							null, // Selection arguments (none)
							null, null, null);

					handler.sendEmptyMessage(1);

				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}

				try {
					if (historyCursor != null) {
						if (historyCursor.moveToLast()) {
							// Variable for holding the retrieved URL
							int urlColumn = historyCursor.getColumnIndex("url");
							int titleColumn = historyCursor
									.getColumnIndex("title");
							// Reference to the the column containing the URL
							int n = 0;
							do {

								h[n][0] = historyCursor.getString(urlColumn);
								h[n][2] = h[n][0].substring(0,
										Math.min(100, h[n][0].length()))
										+ "...";
								h[n][1] = historyCursor.getString(titleColumn);
								historyHtml += (HistoryPageVariables.Part1
										+ h[n][0] + HistoryPageVariables.Part2
										+ h[n][1] + HistoryPageVariables.Part3
										+ h[n][2] + HistoryPageVariables.Part4);
								n++;
							} while (n < 49 && historyCursor.moveToPrevious());
						}
					}
				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}

				historyHtml += BookmarkPageVariables.End;
				File historyWebPage = new File(context.getFilesDir(),
						"history.html");
				try {
					FileWriter hWriter = new FileWriter(historyWebPage, false);
					hWriter.write(historyHtml);
					hWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (uBar.isShown()) {
					currentTabTitle.setText("History");
					setUrlText("");
					getUrl.setPadding(tenPad, 0, tenPad, 0);
				}

				view.loadUrl("file://" + historyWebPage);
			}

		});
		history.run();
	}

	public static IncognitoWebView generateTab(final int pageToView,
			String Url, final boolean display) {
		IncognitoWebView view = new IncognitoWebView(mContext);
		view.setId(pageToView);
		view.setWebViewClient(new IncognitoWebViewClient(ACTIVITY));
		view.setWebChromeClient(new IncognitoChromeClient(ACTIVITY));
		if (API > 8) {
			view.setDownloadListener(new IncognitoDownloadListener(ACTIVITY));
		}
		main[pageToView] = view;
		if (display) {
			if (currentId != -1) {
				background.removeView(currentTab);
			}
			background.addView(view);
			view.requestFocus();
			currentId = pageToView;
			currentTab = main[pageToView];
			currentTabTitle = urlTitle[pageToView];
		}
		uBar.bringToFront();
		if (Url.contains("about:home")) {
			goBookmarks(mContext, view);
		} else if (Url.contains("about:blank")) {
			view.loadUrl("");
		} else {
			view.loadUrl(Url);
		}
		Log.i("Browser", "tab complete");
		return view;
	}

	public static void goBack(IncognitoWebView view) {
		if (view.isShown() && view.canGoBack()) {
			view.goBack();
		}
		Animation left = AnimationUtils.loadAnimation(mContext, R.anim.left);
		background.startAnimation(left);

	}

	static void goBookmarks(Context context, IncognitoWebView view) {
		File book = new File(context.getFilesDir(), "bookmarks");
		File bookUrl = new File(context.getFilesDir(), "bookurl");
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
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
		openBookmarks(context, view);
	}

	public static void goForward(IncognitoWebView view) {
		if (view.isShown() && view.canGoForward()) {
			view.goForward();
		}
		Animation right = AnimationUtils.loadAnimation(mContext, R.anim.right);
		background.startAnimation(right);
	}

	public static void handleLongClickOnBookmarks(final String clickedURL,
			final int n) {
		if (clickedURL != null) {

			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE: {
						renameBookmark(clickedURL);
						break;
					}
					case DialogInterface.BUTTON_NEGATIVE: {
						main[n].loadUrl(clickedURL);
						break;
					}
					case DialogInterface.BUTTON_NEUTRAL: {
						deleteBookmark(clickedURL);
						break;
					}
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext); // dialog
			builder.setMessage(mContext.getResources().getString(R.string.dialog_bookmark))
					.setPositiveButton(mContext.getResources().getString(R.string.action_edit), dialogClickListener)
					.setNegativeButton(mContext.getResources().getString(R.string.action_open), dialogClickListener)
					.setNeutralButton(mContext.getResources().getString(R.string.action_delete), dialogClickListener).show();
		}
	}

	public static int newId() {

		Random n = new Random();
		int id = n.nextInt();

		while (tabList.contains(id)) {
			id = n.nextInt();
		}
		return id;
	}

	// new tab method, takes the id of the tab to be created and the url to load
	public static int newTab(final String theUrl, final boolean display) {
		Log.i("Browser", "making tab");
		homepage = settings.getString("home", HOMEPAGE);
		int finalID = createTab(theUrl, display);
		if (finalID != -1) {
			tabList.add(finalID);
			if (display) {
				currentId = finalID;
				currentTab = main[finalID];
				currentTabTitle = urlTitle[finalID];
			}

			return finalID;
		} else {
			return 0;
		}
	}

	public static void onCreateWindow(Message resultMsg) {
		newTab("", true);
		WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
		transport.setWebView(currentTab);
		resultMsg.sendToTarget();
		browserHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				currentTab.loadUrl(getUrl.getText().toString());
			}
		}, 500);
	}

	public static void onHideCustomView(FrameLayout fullScreenContainer,
			CustomViewCallback mCustomViewCallback, int orientation) {
		if (!settings.getBoolean("hidestatus", false)) {
			ACTIVITY.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		FrameLayout screen = (FrameLayout) ACTIVITY.getWindow().getDecorView();
		screen.removeView(fullScreenContainer);
		fullScreenContainer = null;
		//causing video crashes
		//mCustomViewCallback.onCustomViewHidden();
		ACTIVITY.setRequestedOrientation(orientation);
		background.addView(currentTab);
		uBar.setVisibility(View.VISIBLE);
		uBar.bringToFront();
	}

	private static Message click;

	public static boolean onLongClick() {
		int n = currentId;
		if (currentId == -1 || currentTab == null) {
			return true;
		}
		final HitTestResult result = currentTab.getHitTestResult();

		if (currentTab.getUrl().contains(
				"file://" + mContext.getFilesDir() + "/bookmarks.html")) {
			click = new Message();
			click.arg1 = n;
			click.setTarget(new ClickHandler());
			currentTab.requestFocusNodeHref(click);

			return true;
		} else if (result != null) {
			if (result.getExtra() != null) {
				if (result.getType() == 5 && API > 8) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								int num = currentId;
								newTab(result.getExtra(), false);
								// urlTitle[num].performClick();
								currentId = num;
								currentTab = main[num];
								currentTabTitle = urlTitle[num];
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								currentTab.loadUrl(result.getExtra());
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API > 8) {
									String url = result.getExtra();

									Utils.downloadFile(mContext, url, null, null);

								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mContext); // dialog
					builder.setMessage(
							mContext.getResources().getString(R.string.dialog_image))
							.setPositiveButton(mContext.getResources().getString(R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(mContext.getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(mContext.getResources().getString(R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								int num = currentId;
								newTab(result.getExtra(), false);
								currentId = num;
								currentTab = main[num];
								currentTabTitle = urlTitle[num];
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								currentTab.loadUrl(result.getExtra());
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API < 11) {
									android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ACTIVITY
											.getSystemService(Context.CLIPBOARD_SERVICE);
									clipboard.setText(result.getExtra());
								} else {
									ClipboardManager clipboard = (ClipboardManager) ACTIVITY
											.getSystemService(CLIPBOARD_SERVICE);
									ClipData clip = ClipData.newPlainText(
											"label", result.getExtra());
									clipboard.setPrimaryClip(clip);
								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mContext); // dialog
					builder.setTitle(result.getExtra())
							.setMessage(
									mContext.getResources().getString(R.string.dialog_link))
							.setPositiveButton(mContext.getResources().getString(R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(mContext.getResources().getString(R.string.action_open),
									dialogClickListener)
							.setNeutralButton(mContext.getResources().getString(R.string.action_copy), dialogClickListener)
							.show();
				}
			}
			return true;

		} else {
			return false;
		}
	}

	public static void onPageFinished(WebView view, String url) {
		if (view.isShown()) {
			view.invalidate();
			progressBar.setVisibility(View.GONE);
			refresh.setVisibility(View.VISIBLE);

			if (showFullScreen && uBar.isShown()) {
				uBar.startAnimation(slideUp);
			}
		}
		view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		Log.i("Lightning", "Page Finished");
		loadTime = System.currentTimeMillis() - loadTime;
		Log.i("Lightning", "Load Time: " + loadTime);
	}

	private static int numberPage;

	public static void onPageStarted(WebView view, String url, Bitmap favicon) {
		Log.i("Lightning", "Page Started");
		loadTime = System.currentTimeMillis();
		numberPage = view.getId();

		if (url.startsWith("file://")) {
			view.getSettings().setUseWideViewPort(false);
		} else {
			view.getSettings().setUseWideViewPort(
					settings.getBoolean("wideviewport", true));
		}

		if (view.isShown()) {
			refresh.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.VISIBLE);
			setUrlText(url);
		}

		urlTitle[numberPage].setCompoundDrawables(incognitoPage, null, exitTab,
				null);
		if (favicon != null) {
			setFavicon(view.getId(), favicon);
		}

		getUrl.setPadding(tenPad, 0, tenPad, 0);
		urlToLoad[numberPage][0] = url;

		if (!uBar.isShown() && showFullScreen) {
			uBar.startAnimation(slideDown);
		}
	}

	public static void onProgressChanged(int id, int progress) {
		if (id == currentId) {
			browserProgress.setProgress(progress);
			if (progress < 100) {
				browserProgress.setVisibility(View.VISIBLE);
			} else {
				browserProgress.setVisibility(View.GONE);
			}
		}
	}

	public static void onReceivedTitle(int numberPage, String title) {
		if (title != null && title.length() != 0) {
			urlTitle[numberPage].setText(title);
			urlToLoad[numberPage][1] = title;
		}
	}

	public static void onShowCustomView() {
		ACTIVITY.getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		background.removeView(currentTab);
		uBar.setVisibility(View.GONE);
	}

	static void openBookmarks(Context context, IncognitoWebView view) {
		String bookmarkHtml = BookmarkPageVariables.Heading;

		for (int n = 0; n < MAX_BOOKMARKS; n++) {
			if (bUrl[n] != null) {
				bookmarkHtml += (BookmarkPageVariables.Part1 + bUrl[n]
						+ BookmarkPageVariables.Part2 + bUrl[n]
						+ BookmarkPageVariables.Part3 + bTitle[n] + BookmarkPageVariables.Part4);
			}
		}
		bookmarkHtml += BookmarkPageVariables.End;
		File bookmarkWebPage = new File(context.getFilesDir(), "bookmarks.html");
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkHtml);
			bookWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		view.loadUrl("file://" + bookmarkWebPage);

		if (uBar.isShown()) {
			currentTabTitle.setText("Bookmarks");
			setUrlText("");
			getUrl.setPadding(tenPad, 0, tenPad, 0);
		}

	}

	public static void openFileChooser(ValueCallback<Uri> uploadMsg) {
		mUploadMessage = uploadMsg;
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");
		ACTIVITY.startActivityForResult(
				Intent.createChooser(i, "File Chooser"), 1);
	}

	public static void reinitializeSettings() {
		int size = tabList.size();
		for (int n = 0; n < size; n++) {
			main[tabList.get(n)].settingsInitialization(mContext);
		}
	}

	public static void renameBookmark(String url) {
		index = 0;
		for (int n = 0; n < MAX_BOOKMARKS; n++) {
			if (bUrl[n] != null) {
				if (bUrl[n].equalsIgnoreCase(url)) {
					index = n;
					break;
				}
			}
		}

		final AlertDialog.Builder homePicker = new AlertDialog.Builder(mContext);
		homePicker.setTitle(mContext.getResources().getString(R.string.title_edit_bookmark));
		final EditText getText = new EditText(mContext);
		getText.setText(bTitle[index]);

		homePicker.setView(getText);
		homePicker.setPositiveButton(mContext.getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						bTitle[index] = getText.getText().toString();
						File book = new File(mContext.getFilesDir(), "bookmarks");
						File bookUrl = new File(mContext.getFilesDir(),
								"bookurl");
						int n = 0;
						try {
							BufferedWriter bookWriter = new BufferedWriter(
									new FileWriter(book));
							BufferedWriter urlWriter = new BufferedWriter(
									new FileWriter(bookUrl));
							while (bUrl[n] != null && n < (MAX_BOOKMARKS - 1)) {
								bookWriter.write(bTitle[n]);
								urlWriter.write(bUrl[n]);
								bookWriter.newLine();
								urlWriter.newLine();
								n++;
							}
							bookWriter.close();
							urlWriter.close();
						} catch (FileNotFoundException e) {
						} catch (IOException e) {
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
						} catch (IOException ignored) {
						}
						openBookmarks(mContext, currentTab);
					}
				});
		homePicker.show();

	}

	static void searchTheWeb(String query, Context context) {
		query = query.trim();
		currentTab.stopLoading();

		if (query.startsWith("www.")) {
			query = "http://" + query;
		} else if (query.startsWith("ftp.")) {
			query = "ftp://" + query;
		}

		boolean containsPeriod = query.contains(".");
		boolean isIPAddress = (TextUtils.isDigitsOnly(query.replace(".", "")) && (query
				.replace(".", "").length() >= 4));
		boolean aboutScheme = query.contains("about:");
		boolean validURL = (query.startsWith("ftp://")
				|| query.startsWith("http://") || query.startsWith("file://") || query
					.startsWith("https://")) || isIPAddress;
		boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

		if (query.contains("about:home") || query.contains("about:bookmarks")) {
			goBookmarks(context, currentTab);
		} else if (query.contains("about:history")) {
			generateHistory(currentTab, context);
		} else if (isSearch) {
			try {
				URLEncoder.encode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			currentTab.loadUrl(SEARCH + query);
		} else if (!validURL) {
			currentTab.loadUrl("http://" + query);
		} else {
			currentTab.loadUrl(query);
		}
	}

	public static void setFavicon(int id, Bitmap favicon) {
		Drawable icon;
		icon = new BitmapDrawable(null, favicon);
		icon.setBounds(0, 0, width / 2, height / 2);
		urlTitle[id].setCompoundDrawables(incognitoPage, null, exitTab, null);

	}

	public static void setUrlText(String url) {
		if (url != null) {
			if (!url.startsWith("file://")) {
				getUrl.setText(url);
			} else {
				getUrl.setText("");
			}
		}
	}

	static void share() {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

		// set the type
		shareIntent.setType("text/plain");

		// add a subject
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				urlToLoad[currentId][1]);

		// build the body of the message to be shared
		String shareMessage = urlToLoad[currentId][0];

		// add the message
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);

		// start the chooser for sharing
		mContext.startActivity(Intent.createChooser(shareIntent,
				mContext.getResources().getString(R.string.dialog_title_share)));
	}

	public static void toggleFullScreen() {
		showFullScreen = settings.getBoolean("fullscreen", false);
		IncognitoWebView.showFullScreen = showFullScreen;
		if (fullScreen) {
			background.removeView(uBar);
			screen.addView(uBar);
			fullScreen = false;
		} else {
			screen.removeView(uBar);
			background.addView(uBar);
			fullScreen = true;
		}
	}

	void back() {
		ImageView exit = (ImageView) findViewById(R.id.exit);
		exit.setBackgroundResource(R.drawable.button);
		if (isPhone) {
			RelativeLayout relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);
			relativeLayout1.removeView(exit);
		}
		exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (currentTab.canGoBack()) {
					currentTab.goBack();
				} else {
					deleteTab(currentId);
				}

			}

		});
		exit.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				clearCache();
				finish();
				return true;
			}

		});

	}

	void deleteTab(final int del) {
		if (API >= 11) {
			main[del].onPause();
		}
		main[del].stopLoading();
		main[del].clearHistory();
		tabScroll.smoothScrollTo(currentTabTitle.getLeft(), 0);
		edit.putString("oldPage", urlToLoad[del][0]);
		edit.commit();
		urlToLoad[del][0] = null;
		urlToLoad[del][1] = null;
		if (API < 16) {
			urlTitle[del].setBackgroundDrawable(active);
		} else {
			urlTitle[del].setBackground(active);
		}

		urlTitle[del].setPadding(leftPad, 0, rightPad, 0);
		Animation yolo = AnimationUtils.loadAnimation(this, R.anim.down);
		yolo.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				// urlTitle[del].setVisibility(View.GONE);
				tabLayout.post(new Runnable() {

					@Override
					public void run() {
						tabLayout.removeView(urlTitle[del]);
					}

				});
				findNewView(del);
				if (main[del] != null) {
					if (API > 11) {
						main[del].onPause();
					}
					if (main[del].isShown()) {
						background.removeView(main[del]);
					}
					main[del].removeAllViews();
					main[del] = null;
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}

		});
		urlTitle[del].startAnimation(yolo);
		uBar.bringToFront();
	}

	void enter() {
		getUrl.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

				switch (arg1) {
				case KeyEvent.KEYCODE_ENTER:
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					searchTheWeb(getUrl.getText().toString(), mContext);
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
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					searchTheWeb(getUrl.getText().toString(), mContext);
					return true;
				}
				return false;
			}

		});
	}

	static List<Map<String, String>> getBookmarks() {
		List<Map<String, String>> bookmarks = new ArrayList<Map<String, String>>();
		File bookUrl = new File(mContext.getFilesDir(), "bookurl");
		File book = new File(mContext.getFilesDir(), "bookmarks");
		try {
			BufferedReader readUrl = new BufferedReader(new FileReader(bookUrl));
			BufferedReader readBook = new BufferedReader(new FileReader(book));
			String u, t;
			while ((u = readUrl.readLine()) != null
					&& (t = readBook.readLine()) != null) {
				Map<String, String> map = new HashMap<String, String>();
				map.put("title", '\u2605' + " " + t);
				map.put("url", u);
				bookmarks.add(map);
			}
			readBook.close();
			readUrl.close();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
		return bookmarks;
	}

	@SuppressLint("HandlerLeak")
	void enterUrl() {
		getUrl = (MultiAutoCompleteTextView) findViewById(R.id.enterUrl);
		getUrl.setPadding(tenPad, 0, tenPad, 0);
		getUrl.setTextColor(getResources().getColor(android.R.color.black));
		getUrl.setPadding(tenPad, 0, tenPad, 0);
		getUrl.setBackgroundResource(R.drawable.book);
		getUrl.setPadding(tenPad, 0, tenPad, 0);
		final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		List<Map<String, String>> books = getBookmarks();
		Iterator<Map<String, String>> it = books.iterator();
		while (it.hasNext()) {
			list.add(it.next());
		}
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case 1: {
					SimpleAdapter adapter = new SimpleAdapter(mContext, list,
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

				Cursor c = null;
				Cursor managedCursor = null;
				columns = new String[] { "url", "title" };
				try {

					bookmarks = Browser.BOOKMARKS_URI;
					c = getContentResolver().query(bookmarks, columns, null,
							null, null);
				} catch (SQLiteException ignored) {
				} catch (IllegalStateException ignored) {
				} catch (NullPointerException ignored) {
				}

				if (c != null) {
					noStockBrowser = false;
					Log.i("Browser", "detected AOSP browser");
				} else {
					noStockBrowser = true;
					Log.e("Browser", "did not detect AOSP browser");
				}
				if (c != null) {
					c.close();
				}
				try {

					managedCursor = null;
					SQLiteDatabase s = historyHandler.getReadableDatabase();
					managedCursor = s.query("history", // URI
														// of
							columns, // Which columns to return
							null, // Which rows to return (all rows)
							null, // Selection arguments (none)
							null, null, null);

				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}

				try {
					if (managedCursor != null) {

						if (managedCursor.moveToLast()) {

							// Variable for holding the retrieved URL

							int urlColumn = managedCursor.getColumnIndex("url");
							int titleColumn = managedCursor
									.getColumnIndex("title");
							// Reference to the the column containing the URL
							do {
								String urlA = managedCursor
										.getString(urlColumn);
								String title = managedCursor
										.getString(titleColumn);
								Map<String, String> map = new HashMap<String, String>();
								map.put("title", title);
								map.put("url", urlA);
								list.add(map);
							} while (managedCursor.moveToPrevious());
						}
					}
					handler.sendEmptyMessage(1);
				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}
				managedCursor.close();
			}

		});
		try {
			updateAutoComplete.start();
		} catch (NullPointerException ignored) {
		} catch (SQLiteMisuseException ignored) {
		} catch (IllegalStateException ignored) {
		}

		getUrl.setThreshold(1);
		getUrl.setTokenizer(new SpaceTokenizer());
		getUrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				try {
					String url;
					url = ((TextView) arg1.findViewById(R.id.url)).getText()
							.toString();
					getUrl.setText(url);
					searchTheWeb(url, mContext);
					url = null;
					getUrl.setPadding(tenPad, 0, tenPad, 0);
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
				} catch (NullPointerException e) {
					Log.e("Browser Error: ",
							"NullPointerException on item click");
				}
			}

		});

		getUrl.setSelectAllOnFocus(true); // allows edittext to select all when
											// clicked
	}

	void findNewView(int id) {
		int delete = tabList.indexOf(id);
		int leftId = id;
		boolean right = false, left = false;
		if (id == currentId) {

			if (main[id].isShown()) {
				main[id].startAnimation(fadeOut);
				background.removeView(main[id]);
				uBar.bringToFront();
			}

			if (tabList.size() > delete + 1) {
				id = tabList.get(delete + 1);
				if (urlTitle[id].isShown()) {
					background.addView(main[id]);
					main[id].setVisibility(View.VISIBLE);
					uBar.bringToFront();
					if (API < 16) {
						urlTitle[id].setBackgroundDrawable(active);
					} else {
						urlTitle[id].setBackground(active);
					}
					urlTitle[id].setPadding(leftPad, 0, rightPad, 0);
					currentId = id;
					currentTab = main[id];
					currentTabTitle = urlTitle[id];
					setUrlText(urlToLoad[currentId][0]);
					getUrl.setPadding(tenPad, 0, tenPad, 0);
					right = true;
					if (main[id].getProgress() < 100) {
						onProgressChanged(id, main[id].getProgress());
						refresh.setVisibility(View.INVISIBLE);
						progressBar.setVisibility(View.VISIBLE);
					} else {
						onProgressChanged(id, main[id].getProgress());
						progressBar.setVisibility(View.GONE);
						refresh.setVisibility(View.VISIBLE);
					}
					// break;
				}

			}
			if (!right) {
				if (delete > 0) {
					leftId = tabList.get(delete - 1);
					if (urlTitle[leftId].isShown()) {
						background.addView(main[leftId]);
						main[leftId].setVisibility(View.VISIBLE);
						// uBar.bringToFront();
						if (API < 16) {
							urlTitle[leftId].setBackgroundDrawable(active);
						} else {
							urlTitle[leftId].setBackground(active);
						}
						urlTitle[leftId].setPadding(leftPad, 0, rightPad, 0);
						currentId = leftId;
						currentTab = main[leftId];
						currentTabTitle = urlTitle[leftId];
						setUrlText(urlToLoad[currentId][0]);
						getUrl.setPadding(tenPad, 0, tenPad, 0);
						left = true;
						if (main[leftId].getProgress() < 100) {
							refresh.setVisibility(View.INVISIBLE);
							progressBar.setVisibility(View.VISIBLE);
							onProgressChanged(leftId,
									main[leftId].getProgress());
						} else {
							progressBar.setVisibility(View.GONE);
							refresh.setVisibility(View.VISIBLE);
							onProgressChanged(leftId,
									main[leftId].getProgress());
						}
						// break;
					}

				}

			}

		} else {
			right = left = true;
		}
		tabList.remove(delete);
		if (!(right || left)) {
			if (API > 11) {
				currentTab.onPause();
			}
			currentTab.pauseTimers();
			clearCache();
			currentTab = null;
			finish();
		}
		uBar.bringToFront();
		tabScroll.smoothScrollTo(currentTabTitle.getLeft(), 0);
	}

	public void clearCache() {
		if (settings.getBoolean("cache", false) && currentTab != null) {
			currentTab.clearCache(true);
			Log.i("Lightning", "Cache Cleared");

		}
		for (int n = 0; n < MAX_TABS; n++) {
			if (main[n] != null) {
				main[n].removeAllViews();
				main[n] = null;
			}
		}
	}

	@Override
	public void finish() {
		background.clearDisappearingChildren();
		background.removeView(currentTab);
		tabScroll.clearDisappearingChildren();
		super.finish();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub

		super.onStop();
	}

	void forward() {
		ImageView forward = (ImageView) findViewById(R.id.forward);
		forward.setBackgroundResource(R.drawable.button);
		if (isPhone) {
			RelativeLayout relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);
			relativeLayout1.removeView(forward);
		}
		forward.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentTab.canGoForward()) {
					currentTab.goForward();
				}
			}

		});
	}

	@SuppressLint("InlinedApi")
	private void initialize() {

		tabList = new ArrayList<Integer>();
		bUrl = new String[MAX_BOOKMARKS];
		bTitle = new String[MAX_BOOKMARKS];
		main = new IncognitoWebView[MAX_TABS];
		urlTitle = new TextView[MAX_TABS];
		urlToLoad = new String[MAX_TABS][2];
		fullScreen = false;
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		historyHandler = new DatabaseHandler(this);
		cookieManager = CookieManager.getInstance();
		CookieSyncManager.createInstance(mContext);
		cookieManager.setAcceptCookie(false);

		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		browserProgress = (ProgressBar) findViewById(R.id.progressBar);
		browserProgress.setVisibility(View.GONE);

		if (API >= 11) {
			progressBar.setIndeterminateDrawable(getResources().getDrawable(
					R.drawable.ics_animation));
		} else {
			progressBar.setIndeterminateDrawable(getResources().getDrawable(
					R.drawable.ginger_animation));
		}

		showFullScreen = settings.getBoolean("fullscreen", false);
		uBar = (RelativeLayout) findViewById(R.id.urlBar);
		screen = (RelativeLayout) findViewById(R.id.background);
		slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
		slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
		fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		fadeOut.setDuration(250);
		fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		// mShortAnimationDuration = getResources().getInteger(
		// android.R.integer.config_mediumAnimTime);
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

		RelativeLayout refreshLayout = (RelativeLayout) findViewById(R.id.refreshLayout);
		refreshLayout.setBackgroundResource(R.drawable.button);

		// user agent
		if (API < 17) {
			user = new WebView(mContext).getSettings().getUserAgentString();
		} else {
			user = WebSettings.getDefaultUserAgent(this);
		}

		background = (FrameLayout) findViewById(R.id.holder);
		defaultUser = user; // setting mobile user
		// agent
		switch (settings.getInt("search", 1)) {
		case 1:
			SEARCH = FinalVariables.GOOGLE_SEARCH;
			break;
		case 2:
			SEARCH = FinalVariables.BING_SEARCH;
			break;
		case 3:
			SEARCH = FinalVariables.YAHOO_SEARCH;
			break;
		case 4:
			SEARCH = FinalVariables.STARTPAGE_SEARCH;
			break;
		case 5:
			SEARCH = FinalVariables.DUCK_SEARCH;
			break;
		case 6:
			SEARCH = FinalVariables.BAIDU_SEARCH;
			break;
		case 7:
			SEARCH = FinalVariables.YANDEX_SEARCH;
			break;
		case 8:
			SEARCH = FinalVariables.DUCK_LITE_SEARCH;
			break;
		}

		exitTab = getResources().getDrawable(R.drawable.stop); // user
		// agent
		homepage = settings.getString("home", HOMEPAGE); // initializing
															// the
															// stored
															// homepage
															// variable

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
		tabLayout = (LinearLayout) findViewById(R.id.tabLayout);
		tabScroll = (HorizontalScrollView) findViewById(R.id.tabScroll);
		tabScroll.setBackgroundColor(getResources().getColor(R.color.black));
		tabScroll.setHorizontalScrollBarEnabled(false);
		if (API > 8) {
			tabScroll.setOverScrollMode(View.OVER_SCROLL_NEVER); // disallow
																	// overscroll
		}

		// image dimensions and initialization
		final int dps = 175;
		final float scale = getApplicationContext().getResources()
				.getDisplayMetrics().density;
		pixels = (int) (dps * scale + 0.5f);
		leftPad = (int) (17 * scale + 0.5f);
		rightPad = (int) (15 * scale + 0.5f);
		height32 = (int) (32 * scale + 0.5f);
		tenPad = (int) (10 * scale + 0.5f);

		incognitoPage = getResources().getDrawable(R.drawable.incognito);
		incognitoPage.setBounds(0, 0, width / 2, height / 2);
		exitTab.setBounds(0, 0, width * 2 / 3, height * 2 / 3);

		Thread startup = new Thread(new Runnable() {

			@Override
			public void run() {
				reopenOldTabs(); // restores old tabs or creates a new one
			}

		});
		startup.run();

		// new tab button
		ImageView newTab = (ImageView) findViewById(R.id.newTab);
		newTab.setBackgroundResource(R.drawable.button);
		newTab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Handler click = new NewTabHandler();
				click.sendEmptyMessage(1);
				tabScroll.postDelayed(new Runnable() {
					@Override
					public void run() {
						tabScroll.smoothScrollTo(currentTabTitle.getLeft(), 0);
					}
				}, 100L);

			}
		});
		newTab.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (settings.getString("oldPage", "").length() > 0) {
					newTab(settings.getString("oldPage", ""), true);
					edit.putString("oldPage", "");
					edit.commit();
					tabScroll.postDelayed(new Runnable() {
						@Override
						public void run() {
							tabScroll.smoothScrollTo(currentTabTitle.getLeft(),
									0);
						}
					}, 100L);
				}
				return true;
			}

		});
		refresh = (ImageView) findViewById(R.id.refresh);
		refreshLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (currentTab.getProgress() < 100) {
					currentTab.stopLoading();
				} else {
					currentTab.reload();
				}
			}

		});

		enterUrl();
		if (showFullScreen) {
			toggleFullScreen();
		}
		browserHandler = new Handle();

	}

	private void newSettings() {
		startActivity(new Intent(FinalVariables.SETTINGS_INTENT));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == 1) {
			if (null == mUploadMessage)
				return;
			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;

		}
	}

	@Override
	public void onBackPressed() {
		try {
			if (showFullScreen && !uBar.isShown()) {
				uBar.startAnimation(slideDown);
			}
			if (currentTab.isShown() && currentTab.canGoBack()) {
				currentTab.goBack();
			} else {
				deleteTab(currentId);
				uBar.bringToFront();
			}
		} catch (NullPointerException ignored) {
		}
		return;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (settings.getBoolean("textreflow", false)) {
			currentTab.getSettings().setLayoutAlgorithm(
					LayoutAlgorithm.NARROW_COLUMNS);
		} else {
			currentTab.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); // displays main xml layout
		mContext = this;
		ACTIVITY = this;
		settings = getSharedPreferences("settings", 0);
		edit = settings.edit();

		if (settings.getBoolean("hidestatus", false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		inactive = getResources().getDrawable(R.drawable.bg_inactive);
		active = getResources().getDrawable(R.drawable.bg_press);
		initialize(); // sets up random stuff
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
		back();
		if (settings.getInt("firstIncognito", 0) == 0) {
			// navigation tips
			String message = "Incognito Mode keeps you safe while browsing the internet."
					+ " History is not saved, Cookies are disabled, and Location access is "
					+ "disabled to ensure as best we can that you are not being tracked. "
					+ "Happy Browsing!";

			Utils.createInformativeDialog(mContext, "About Incognito", message);
			edit.putInt("first", 1);
			edit.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.incognito_menu, menu);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_SEARCH: {
			getUrl.requestFocus();
			InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			manager.showSoftInput(getUrl, 0);

			break;
		}
		case KeyEvent.KEYCODE_F5: {
			currentTab.reload();
		}
		case KeyEvent.KEYCODE_ESCAPE: {
			currentTab.stopLoading();
		}
		case KeyEvent.KEYCODE_TAB: {
			InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (!manager.isActive()) {
				newTab(homepage, true);
			}

		}
		case KeyEvent.KEYCODE_F12: {
			clearCache();
			finish();
		}
		case KeyEvent.KEYCODE_F6: {
			getUrl.selectAll();
		}
		case KeyEvent.KEYCODE_F10: {
			startActivity(new Intent(FinalVariables.SETTINGS_INTENT));
		}
		case KeyEvent.KEYCODE_F11: {
			toggleFullScreen();
		}
		case KeyEvent.KEYCODE_DEL: {
			InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (!manager.isActive()) {
				currentTab.goBack();
			}
		}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!settings.getBoolean("restoreclosed", true)) {
				for (int n = 0; n < MAX_TABS; n++) {
					urlToLoad[n][0] = null;
				}
			}
			clearCache();
			finish();
			return true;
		} else
			return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public void onLowMemory() {
		for (int n = 0; n < MAX_TABS; n++) {
			if (n != currentId && main[n] != null) {
				main[n].freeMemory();
			}
		}
		super.onLowMemory();
	}

	@Override
	protected void onNewIntent(Intent intent) {

		String url = intent.getDataString();
		int id = -1;
		int download = -1;
		try {
			id = intent.getExtras().getInt("acr.browser.barebones.Origin") - 1;
		} catch (NullPointerException e) {
			id = -1;
		}
		try {
			download = intent.getExtras().getInt(
					"acr.browser.barebones.Download");
		} catch (NullPointerException e) {
			download = -1;
		}
		if (id >= 0) {
			main[id].loadUrl(url);
		} else if (download == 1) {
			Utils.downloadFile(mContext, url, null, null);
		} else if (url != null) {
			newTab(url, true);
		}

		super.onNewIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.history:
			generateHistory(currentTab, mContext);
			return true;
		case R.id.bookmark:
			if (urlToLoad[currentId][1] != null) {
				if (!urlToLoad[currentId][1].equals("Bookmarks")) {
					Utils.addBookmark(mContext, urlToLoad[currentId][1],
							urlToLoad[currentId][0]);
				}
			}
			return true;
		case R.id.settings:
			newSettings();
			return true;
		case R.id.allBookmarks:
			if (urlToLoad[currentId][1] == null) {
				goBookmarks(mContext, currentTab);
			} else if (!urlToLoad[currentId][1].equals("Bookmarks")) {
				goBookmarks(mContext, currentTab);
			}

			return true;
		case R.id.share:
			share();
			return true;
		case R.id.incognito:
			newTab(homepage, true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {

		if (currentTab != null) {
			if (API >= 11) {
				currentTab.onPause();
				for (int n = 0; n < MAX_TABS; n++) {
					if (main[n] != null)
						main[n].onPause();
				}
			}
			currentTab.pauseTimers();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		onProgressChanged(currentId, currentTab.getProgress());
		if (currentTab.getProgress() == 100) {
			progressBar.setVisibility(View.GONE);
			refresh.setVisibility(View.VISIBLE);

		}
		if (API >= 11) {
			currentTab.onResume();
		}
		reinitializeSettings();
		currentTab.resumeTimers();
		if (settings.getBoolean("fullscreen", false) != fullScreen) {
			toggleFullScreen();
		}

	}

	private int x;
	private int y;
	private Rect edge;
	private boolean xPress;
	private final GestureDetector mGestureDetector = new GestureDetector(
			mContext, new CustomGestureListener());

	private class CustomGestureListener extends SimpleOnGestureListener {
		@Override
		public void onLongPress(MotionEvent e) {
			deleteTab(id);
			super.onLongPress(e);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		try {
			id = v.getId();
			background.clearDisappearingChildren();
			xPress = false;
			x = (int) event.getX();
			y = (int) event.getY();
			edge = new Rect();
			v.getDrawingRect(edge);
			currentTabTitle.setPadding(leftPad, 0, rightPad, 0);
			if (event.getAction() == MotionEvent.ACTION_DOWN) {

			} else if (event.getAction() == MotionEvent.ACTION_UP) {

				if (x >= (edge.right - bounds.width() - v.getPaddingRight() - 10 * 3 / 2)
						&& x <= (edge.right - v.getPaddingRight() + 10 * 3 / 2)
						&& y >= (v.getPaddingTop() - 10 / 2)
						&& y <= (v.getHeight() - v.getPaddingBottom() + 10 / 2)) {
					xPress = true;
				}
				if (id == currentId) {
					if (xPress) {
						deleteTab(id);
						uBar.bringToFront();
					}
				} else if (id != currentId) {
					if (xPress) {
						deleteTab(id);
					} else {
						if (API < 16) {
							currentTabTitle.setBackgroundDrawable(inactive);
						} else if (API > 15) {
							currentTabTitle.setBackground(inactive);
						}
						currentTabTitle.setPadding(leftPad, 0, rightPad, 0);
						if (!showFullScreen) {
							background.addView(main[id]);
							main[id].startAnimation(fadeIn);
							currentTab.startAnimation(fadeOut);
							background.removeView(currentTab);
							uBar.bringToFront();
						} else if (API >= 12) {
							main[id].setAlpha(0f);
							background.addView(main[id]);
							try {
								main[id].animate().alpha(1f).setDuration(250);
							} catch (NullPointerException ignored) {
							}
							background.removeView(currentTab);
							uBar.bringToFront();
						} else {
							background.removeView(currentTab);
							background.addView(main[id]);
						}
						uBar.bringToFront();

						currentId = id;
						currentTab = main[id];
						currentTabTitle = urlTitle[id];
						setUrlText(urlToLoad[currentId][0]);
						getUrl.setPadding(tenPad, 0, tenPad, 0);
						if (API < 16) {
							currentTabTitle.setBackgroundDrawable(active);
						} else if (API > 15) {
							currentTabTitle.setBackground(active);
						}
						if (currentTab.getProgress() < 100) {
							refresh.setVisibility(View.INVISIBLE);

							progressBar.setVisibility(View.VISIBLE);

						} else {
							progressBar.setVisibility(View.GONE);
							refresh.setVisibility(View.VISIBLE);
						}
						onProgressChanged(currentId, currentTab.getProgress());
						tabScroll.smoothScrollTo(currentTabTitle.getLeft(), 0);
						currentTab.invalidate();
					}
				}

			}
			uBar.bringToFront();
			v.setPadding(leftPad, 0, rightPad, 0);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Lightning Error", "Well we dun messed up");
		}
		mGestureDetector.onTouchEvent(event);
		return true;
	}

	void options() {
		ImageView options = (ImageView) findViewById(R.id.options);
		options.setBackgroundResource(R.drawable.button);
		options.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (API >= 11) {
					PopupMenu menu = new PopupMenu(mContext, v);
					MenuInflater inflate = menu.getMenuInflater();
					inflate.inflate(R.menu.incognito_menu, menu.getMenu());
					menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {

							switch (item.getItemId()) {
							case R.id.history:
								generateHistory(currentTab, mContext);
								return true;
							case R.id.bookmark:
								if (urlToLoad[currentId][1] != null) {
									if (!urlToLoad[currentId][1]
											.equals("Bookmarks")) {
										Utils.addBookmark(mContext,
												urlToLoad[currentId][1],
												urlToLoad[currentId][0]);
									}
								}
								return true;
							case R.id.settings:
								newSettings();
								return true;
							case R.id.allBookmarks:
								if (urlToLoad[currentId][1] == null) {
									goBookmarks(mContext, currentTab);
								} else if (!urlToLoad[currentId][1]
										.equals("Bookmarks")) {
									goBookmarks(mContext, currentTab);
								}
								return true;
							case R.id.share:
								share();
								return true;
							case R.id.incognito:
								newTab(homepage, true);
								return true;
							default:
								return false;
							}

						}

					});
					menu.show();
				} else if (API < 11) {

					openOptionsMenu();
				}
			}

		});
	}

	void reopenOldTabs() {
		Intent url = getIntent();
		String URL = url.getDataString();
		if (URL != null) {
			// opens a new tab with the URL if its there
			int n = newTab(URL, true);
			main[n].resumeTimers();
		} else {
			// otherwise it opens the home-page
			int n = newTab(homepage, true);
			main[n].resumeTimers();
		}

	}
}