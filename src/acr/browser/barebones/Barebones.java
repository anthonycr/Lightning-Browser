package acr.browser.barebones;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ZoomButtonsController;

public class Barebones extends Activity implements OnLongClickListener,
		OnTouchListener{
	public static final String preferences = "settings";
	int MAX_TABS=5, MAX_BOOKMARKS=5;
	long lastTime = 0;
	EditText getUrl; // edittext that gets the url entered
	String query, userAgent; // query is what is entered into get url, userAgent
								// is the userAgent applied to the view
	String[][] urlToLoad = new String[MAX_TABS][2]; // String array containing page
												// title and url
	TextView[] urlTitle = new TextView[MAX_TABS]; // textview array for the tabs
	AnthonyWebView[] main = new AnthonyWebView[MAX_TABS]; // WebView array for all windows
	Rect[] bounds = new Rect[MAX_TABS]; // bounds on the exit button on each tab
	int number, pageId = 0, agentPicker; // number = counter for how many tabs
											// created, pageId = tab currently
											// viewed, agent picker = an int
											// showing which agent to use
	boolean tabsAreDisplayed=true,isPhone=false, pageIsLoading=false,java;
	
	
	private ValueCallback<Uri> mUploadMessage;			//stuff for uploading
	private final static int FILECHOOSER_RESULTCODE = 1;
	
	
	ImageView refresh;
	ScrollView backgroundScroll;
	int statusBar;
	RelativeLayout refreshLayout;
	ProgressBar progressBar; // progress bar displayed in the bar
	Drawable icon; // an icon to place somewhere (i forget)
	FrameLayout webFrame; // frame that holds the webviews
	HorizontalScrollView tabScroll;
	int height56, height32;
	int height, width, pixels, leftPad, rightPad, pixelHeight, bookHeight, API;// all
																				// are
																				// dimensions
																				// to
																				// be
																				// used
																				// on
																				// various
																				// drawables,
																				// exept
																				// API
																				// which
																				// is
																				// used
																				// for
																				// lint
																				// checking
	Drawable loading, webpage, webpageOther; // drawables displayed in urlbar
												// and next to the tabs
	Drawable exitTab;// button to exit a tab
	boolean allowLocation, savePasswords, deleteHistory;
	int enableFlash; // booleans
																		// used
																		// in
																		// settings
	View mCustomView = null;
	CustomViewCallback mCustomViewCallback;
	RelativeLayout barLayout;
	boolean fullScreen;
	boolean urlBarShows=true;
	Animation anim;
	SharedPreferences settings;
	SharedPreferences.Editor edit;
	String desktop, mobile, user; // useragent strings
	String[] bUrl = new String[MAX_BOOKMARKS]; // bookmark url
	String[] bTitle = new String[MAX_BOOKMARKS]; // bookmark title
	RelativeLayout background; // the relativelayout encasing the entire browser
	ScrollView scrollBookmarks; // scrollview holding all the bookmarks
	boolean isBookmarkShowing = false; // boolean used to determine if the
										// webview is showing or the bookmarks
	String homepage; // variable for the desired home page

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
        double sizeInInches = Math.sqrt(Math.pow(widthInInches, 2) + Math.pow(heightInInches, 2));
      //0.5" buffer for 7" devices
        isPhone = sizeInInches < 6.5; 
        

		forward();// forward button
		exit();
		int first = settings.getInt("first", 0);
		
		if(first==0){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,
					int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE: 
					break;
				
				
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(
				Barebones.this); // dialog
		builder.setMessage(
				"TIPS:\n" +
				"\nLong-press a tab to close it\n\nLong-press back button to exit browser" +
				"\n\nSet your homepage in settings to about:blank to set a blank page as your default\n" +
				"\nSet the homepage to about:home to set bookmarks as your homepage")
				.setPositiveButton("Ok",
						dialogClickListener).show();
		edit.putInt("first", 1);
		edit.commit();
		}
	}

	public void init() {
		barLayout = (RelativeLayout)findViewById(R.id.relativeLayout1);
		refreshLayout = (RelativeLayout)findViewById(R.id.refreshLayout);
		refreshLayout.setBackgroundResource(R.drawable.button);
		anim = AnimationUtils.loadAnimation(Barebones.this, R.anim.rotate);
		 // get settings
		WebView test = new WebView(Barebones.this); // getting default webview
													// user agent
		user = test.getSettings().getUserAgentString();
		background = (RelativeLayout) findViewById(R.id.holder);
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
		API = Integer.valueOf(android.os.Build.VERSION.SDK_INT); // gets the sdk
																	// level
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
		webFrame = (FrameLayout) findViewById(R.id.webFrame);
		height = getResources().getDrawable(R.drawable.loading)
				.getMinimumHeight();
		width = getResources().getDrawable(R.drawable.loading)
				.getMinimumWidth();
		getUrl = (EditText) findViewById(R.id.enterUrl);
		getUrl.setSelectAllOnFocus(true); // allows edittext to select all when
											// clicked

		// hides keyboard so it doesn't default pop up
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		// opens icondatabase so that favicons can be stored
		WebIconDatabase.getInstance().open(
				getDir("icons", MODE_PRIVATE).getPath());

		// scroll view containing tabs
		tabScroll = (HorizontalScrollView) findViewById(R.id.tabScroll);
		tabScroll.setBackgroundColor(getResources().getColor(R.color.dark));
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
		leftPad = (int) (10 * scale + 0.5f);
		rightPad = (int) (10 * scale + 0.5f);
		height32 = (int) (32 * scale + 0.5f);
		statusBar = (int) (25 * scale + 0.5f);
		number = 0;
		loading = getResources().getDrawable(R.drawable.loading);
		webpage = getResources().getDrawable(R.drawable.webpage);
		webpageOther = getResources().getDrawable(R.drawable.webpage);
		loading.setBounds(0, 0, width * 2 / 3, height * 2 / 3);
		webpage.setBounds(0, 0, width * 2 / 3, height * 2 / 3);
		webpageOther.setBounds(0, 0, width * 1 / 2, height * 1 / 2);
		exitTab.setBounds(0, 0, width * 2 / 3, height * 2 / 3);
		Intent url = getIntent().addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // Receives
																			// intent
																			// when
																			// a
																			// link
																			// is
																			// opened
																			// in
		// another app
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
		refreshLayout.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(pageIsLoading){
					main[pageId].stopLoading();
				}else{
				main[pageId].reload();
				}
			}
			
		});
		DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if(isPhone){
        	fullScreen = settings.getBoolean("fullscreen", true);
        }else{
        fullScreen = settings.getBoolean("fullscreen", false);}
        if(fullScreen==true){
        int height = metrics.heightPixels;
        RelativeLayout holder = (RelativeLayout)findViewById(R.id.holder);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.getLayoutParams();
        params.height = height-statusBar;
        holder.setLayoutParams(params);
        }
        else{
        	int height = metrics.heightPixels-pixelHeight-bookHeight;
            RelativeLayout holder = (RelativeLayout)findViewById(R.id.holder);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.getLayoutParams();
            params.height = height-statusBar;
            holder.setLayoutParams(params);
        }
        backgroundScroll = (ScrollView)findViewById(R.id.backgroundScroll);
        if(API>=9){
        backgroundScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);}
        background.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				return true;
			}
        	
        });
        backgroundScroll.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				backgroundScroll.requestDisallowInterceptTouchEvent(true);
				return true;
			}
        	
        });
		backgroundScroll.requestDisallowInterceptTouchEvent(true);
		backgroundScroll.smoothScrollTo(0, backgroundScroll.getTop());
		backgroundScroll.requestDisallowInterceptTouchEvent(true);
	}

	// new tab method, takes the id of the tab to be created and the url to load
	private void newTab(int theId, String theUrl) {
		if (isBookmarkShowing) {
			background.removeView(scrollBookmarks);
			background.addView(webFrame);
			isBookmarkShowing = false;
		}
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
				webFrame.removeAllViews();
				webFrame.addView(main[num]);
				main[num] = settings(main[num]);
				main[num].loadUrl(theUrl);
				pageId = num;
				isEmptyWebViewAvailable = true;
				break;
			}
		}
		if (number < MAX_TABS) {
			if (isEmptyWebViewAvailable == false) {
				webFrame.removeView(main[pageId]);
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
				title.setHeight(pixelHeight);
				title.setWidth(pixels);
				title.setPadding(leftPad, 0, rightPad, 0);
				title.setId(number);
				title.setGravity(Gravity.CENTER_VERTICAL);
				title.setCompoundDrawables(null, null, exitTab, null);
				Drawable[] drawables = title.getCompoundDrawables();
				bounds[number] = drawables[2].getBounds();
				title.setOnLongClickListener(Barebones.this);
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
			} else {
			}
		} else {
			Toast.makeText(Barebones.this, "Maximum number of tabs reached...",
					Toast.LENGTH_SHORT).show();
		}
		
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE||newConfig.orientation==Configuration.ORIENTATION_PORTRAIT)
		{
			DisplayMetrics metrics = new DisplayMetrics();
	        getWindowManager().getDefaultDisplay().getMetrics(metrics);
	        if(isPhone){
	        	fullScreen = settings.getBoolean("fullscreen", true);
	        }else{
	        fullScreen = settings.getBoolean("fullscreen", false);}	        
	        if(fullScreen==true){
	        int height = metrics.heightPixels;
	        RelativeLayout holder = (RelativeLayout)findViewById(R.id.holder);
	        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.getLayoutParams();
	        params.height = height-statusBar;
	        holder.setLayoutParams(params);
	        }
	        else{
	        	int height = metrics.heightPixels-pixelHeight-bookHeight;
	            RelativeLayout holder = (RelativeLayout)findViewById(R.id.holder);
	            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.getLayoutParams();
	            params.height = height-statusBar;
	            holder.setLayoutParams(params);
	        }
		}
		super.onConfigurationChanged(newConfig);
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
		main[pageToView].setFocusable(true);
		main[pageToView].setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				// TODO Auto-generated method stub
				final HitTestResult result = main[pageId].getHitTestResult();
				boolean image = false;
				if(result.getType() == HitTestResult.IMAGE_TYPE&&API>8){
					image=true;
				}
				
				if (result.getExtra() != null) {
					if (image) {
						DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case DialogInterface.BUTTON_POSITIVE: {
									newTab(number, result.getExtra());
									break;
								}
								case DialogInterface.BUTTON_NEGATIVE: {
									main[pageId].loadUrl(result.getExtra());
									break;
								}
								case DialogInterface.BUTTON_NEUTRAL: {
									if(API>8){
									DownloadManager download = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
									Uri nice = Uri.parse(result.getExtra());
									DownloadManager.Request it = new DownloadManager.Request(
											nice);
									String fileName = result
											.getExtra()
											.substring(
													result.getExtra()
															.lastIndexOf('/') + 1,
													result.getExtra().length());
									it.setDestinationInExternalPublicDir(
											Environment.DIRECTORY_DOWNLOADS,
											fileName);
									Log.i("Barebones", "Downloading" + fileName);
									download.enqueue(it);}
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
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case DialogInterface.BUTTON_POSITIVE: {
									newTab(number, result.getExtra());
									break;
								}
								case DialogInterface.BUTTON_NEGATIVE: {
									main[pageId].loadUrl(result.getExtra());
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
										dialogClickListener).show();
					}
					return true;
				} else {
					return false;
				}

			}

		});
					
					main[pageToView].setOnTouchListener(new OnTouchListener() {

						@Override
						public boolean onTouch(View v, MotionEvent event) {
							
							
							return false;
							
							
							
						}
					});
					
		
		main[pageToView] = settings(main[pageToView]);
		agentPicker = settings.getInt("agentchoose", 1);
		switch (agentPicker) {
		case 1:
			main[pageToView].getSettings().setUserAgentString(mobile);
			Log.i("lightning", mobile);
			break;
		case 2:
			main[pageToView].getSettings().setUserAgentString(desktop);
			Log.i("lightning", desktop);
			break;
		case 3:
			userAgent = settings.getString("agent", user);
			main[pageToView].getSettings().setUserAgentString(userAgent);
			Log.i("lightning", userAgent);
			break;
		}
		webFrame.addView(main[pageToView]);
		if(Url.contains("about:home")){
			goBookmarks();
		}else if(Url.contains("about:blank")){
		}else{
		main[pageToView].loadUrl(Url);}

	}
	public class AnthonyWebView extends WebView {

		
	    public AnthonyWebView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
	    
		@Override
		public void flingScroll(int vx, int vy) {
			// TODO Auto-generated method stub
			super.flingScroll(vx, vy);
		}



		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			super.onDraw(canvas);
			invalidate();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			// TODO Auto-generated method stub
			backgroundScroll.requestDisallowInterceptTouchEvent(true);
			if(API<=10&&main[pageId].hasFocus()==false&&event.getAction()==MotionEvent.ACTION_DOWN){
				main[pageId].requestFocus();}
			if(event.getAction()==MotionEvent.ACTION_DOWN){
			}
			
			if(main[pageId].getScrollY()<5&&event.getAction()==MotionEvent.ACTION_UP&&fullScreen){
				backgroundScroll.requestDisallowInterceptTouchEvent(true);
				backgroundScroll.smoothScrollTo(0, backgroundScroll.getTop());
				backgroundScroll.requestDisallowInterceptTouchEvent(true);
			
			}
			else if(main[pageId].getScrollY()>=5&&event.getAction()==MotionEvent.ACTION_UP&&fullScreen){
				backgroundScroll.requestDisallowInterceptTouchEvent(true);
				backgroundScroll.smoothScrollTo(0, backgroundScroll.getBottom());
				backgroundScroll.requestDisallowInterceptTouchEvent(true);
				}
			backgroundScroll.requestDisallowInterceptTouchEvent(true);
			if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){

			}
			return super.onTouchEvent(event);
		}

		@Override
		protected void onOverScrolled(int scrollX, int scrollY,
				boolean clampedX, boolean clampedY) {
			// TODO Auto-generated method stub
			super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			// TODO Auto-generated method stub
			
			super.onScrollChanged(l, t, oldl, oldt);
		}

		

		
		   
		   
	}
	
	private class AnthonyWebViewClient extends WebViewClient {
		
		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			// TODO Auto-generated method stub
			super.onReceivedHttpAuthRequest(view, handler, host, realm);
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			// TODO Auto-generated method stub
			super.onReceivedError(view, errorCode, description, failingUrl);
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			// TODO Auto-generated method stub
			handler.proceed();
			super.onReceivedSslError(view, handler, error);
		}

		@Override
		public void onReceivedLoginRequest(WebView view, String realm,
				String account, String args) {
			// TODO Auto-generated method stub
			super.onReceivedLoginRequest(view, realm, account, args);
		}


		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			backgroundScroll.requestDisallowInterceptTouchEvent(true);
			backgroundScroll.smoothScrollTo(0, backgroundScroll.getTop());
			backgroundScroll.requestDisallowInterceptTouchEvent(true);
			pageIsLoading = true;
			refresh.startAnimation(anim);
			getUrl.setText(url);
			urlToLoad[pageId][0] = url;
			urlTitle[pageId].setCompoundDrawables(webpageOther, null, exitTab,
					null);
			
		}

		public void onPageFinished(WebView view, String url) {
			pageIsLoading = false;
			anim.cancel();
			anim.reset();
			
		}
	}

	private class AnthonyDownload implements DownloadListener {

		@Override
		public void onDownloadStart(String url, String userAgent,
				String contentDisposition, String mimetype, long contentLength) {
			DownloadManager download = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			Uri nice = Uri.parse(url);
			DownloadManager.Request it = new DownloadManager.Request(nice);
			String fileName = url.substring(url.lastIndexOf('/') + 1,
					url.length());
			it.setDestinationInExternalPublicDir(
					Environment.DIRECTORY_DOWNLOADS, fileName);
			Log.i("Barebones", "Downloading" + fileName);
			download.enqueue(it);
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
		
		@Override
		public void onReceivedIcon(WebView view, Bitmap favicon) {
			// TODO Auto-generated method stub
			super.onReceivedIcon(view, favicon);
			icon = null;
			icon = new BitmapDrawable(getResources(), favicon);
			icon.setBounds(0, 0, width * 1 / 2, height * 1 / 2);
			if (icon != null) {
				urlTitle[pageId]
						.setCompoundDrawables(icon, null, exitTab, null);
			} else {
				urlTitle[pageId].setCompoundDrawables(webpageOther, null,
						exitTab, null);
			}
		}
		public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
			mUploadMessage = uploadMsg;
	        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	        i.addCategory(Intent.CATEGORY_OPENABLE);
	        i.setType("image/*");
	        Barebones.this.startActivityForResult(
	                Intent.createChooser(i, "Image Browser"),
	                FILECHOOSER_RESULTCODE);
		}
		public void openFileChooser( ValueCallback<Uri> uploadMsg, String acceptType ) 
	    {  
			mUploadMessage = uploadMsg;
	        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	        i.addCategory(Intent.CATEGORY_OPENABLE);
	        i.setType("image/*");
	        Barebones.this.startActivityForResult(
	                Intent.createChooser(i, "Image Browser"),
	                FILECHOOSER_RESULTCODE); }
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
				callback.invoke(origin, false, false);
			}

		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			// TODO Auto-generated method stub
			super.onReceivedTitle(view, title);
			urlTitle[pageId].setText(title);
			urlToLoad[pageId][1] = title;
		}
		private Bitmap      mDefaultVideoPoster;
        private View        mVideoProgressView;

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback)
        {
            //Log.i(LOGTAG, "here in on ShowCustomView");
            main[pageId].setVisibility(View.GONE);

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            webFrame.removeView(main[pageId]);
            webFrame.addView(view);
            mCustomView = view;
            mCustomView.setVisibility(View.VISIBLE);
            mCustomViewCallback = callback;
            
        }

        @Override
        public void onHideCustomView() {
            if (mCustomView == null)
                return;        

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            webFrame.removeView(mCustomView);
            mCustomView = null;
            webFrame.addView(main[pageId]);
            mCustomViewCallback.onCustomViewHidden();

            main[pageId].setVisibility(View.VISIBLE);
            main[pageId].goBack();
            //Log.i(LOGTAG, "set it to webVew");
        }
	}

	
	
	private AnthonyWebView settings(AnthonyWebView view) {
		java = settings.getBoolean("java", true);
		if(java){
			view.getSettings().setJavaScriptEnabled(true);
		
		view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);}
		view.getSettings().setAllowFileAccess(true);
		view.getSettings().setLightTouchEnabled(true);
		view.setAlwaysDrawnWithCacheEnabled(true);
		view.setFocusableInTouchMode(true);
		view.setSaveEnabled(true);
		view.getSettings().setDomStorageEnabled(true);
		view.getSettings().setRenderPriority(RenderPriority.HIGH);
		view.getSettings().setGeolocationEnabled(true);
		view.getSettings().setGeolocationDatabasePath(
				getApplicationContext().getFilesDir().getAbsolutePath());
		
		view.getSettings().setDatabaseEnabled(true);
		enableFlash = settings.getInt("enableflash", 0);
		if (enableFlash == 2) {
			view.getSettings().setPluginState(PluginState.ON);
		}else if(enableFlash==1){
			view.getSettings().setPluginState(PluginState.ON_DEMAND);
		}

		view.getSettings().setUserAgentString(userAgent);
		savePasswords = settings.getBoolean("passwords", false);
		if (savePasswords == true) {
			view.getSettings().setSavePassword(true);
		}
		if (API > 8) {
			view.getSettings().setAppCacheEnabled(true);
		}
		if(API<11){
		view.getSettings().setBuiltInZoomControls(true);}

		view.getSettings().setSupportZoom(true);
		view.getSettings().setUseWideViewPort(true);
		view.getSettings().setLoadWithOverviewMode(true);
		if (API >= 11) {
			view.getSettings().setBuiltInZoomControls(true);
			view.getSettings().setDisplayZoomControls(false);
			view.getSettings().setAllowContentAccess(true);
		}

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
		description.setBackgroundColor(0xff0099cc);
		description.setTextColor(0xffffffff);
		description.setText("Bookmarks (long-press to remove)");
		description.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
		description.setTextSize(bookHeight/3);
		description.setPadding(rightPad, 0, rightPad, 0);
		bookmarkLayout.addView(description);

		int n = 0;
		for (; n < MAX_BOOKMARKS; n++) {
			if (bUrl[n] != null) {
				TextView b = new TextView(Barebones.this);
				b.setId(n);
				b.setSingleLine(true);
				b.setGravity(Gravity.CENTER_VERTICAL);
				b.setTextSize(pixelHeight/3);
				b.setBackgroundResource(R.drawable.bookmark);
				b.setHeight(height56);
				b.setText(bTitle[n]);
				b.setCompoundDrawables(webpage, null, null, null);
				b.setOnClickListener(new bookmarkListener());
				b.setOnLongClickListener(new bookmarkLongClick());
				b.setPadding(rightPad, 0, rightPad, 0);
				bookmarkLayout.addView(b);
			}
		}
		urlTitle[pageId].setText("Bookmarks");
		getUrl.setText("Bookmarks");
		scrollBookmarks.addView(bookmarkLayout);
		background.removeView(webFrame);
		isBookmarkShowing = true;
		background.addView(scrollBookmarks);
	}

	class bookmarkLongClick implements OnLongClickListener {

		@Override
		public boolean onLongClick(final View arg0) {
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE: {
						int delete = arg0.getId();
						File book = new File(getBaseContext().getFilesDir(), "bookmarks");
						File bookUrl = new File(getBaseContext().getFilesDir(), "bookurl");
						int n = 0;
						try {
							BufferedWriter bookWriter = new BufferedWriter(new FileWriter(
									book));
							BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
									bookUrl));
							while (bUrl[n] != null) {
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						for (int p = 0; p < MAX_BOOKMARKS; p++) {
							bUrl[p] = null;
							bTitle[p] = null;
						}
						try {
							BufferedReader readBook = new BufferedReader(new FileReader(
									book));
							BufferedReader readUrl = new BufferedReader(new FileReader(
									bookUrl));
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
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();

						}
						background.removeView(scrollBookmarks);
						openBookmarks();
						// TODO Auto-generated method stub
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
			builder.setMessage(
					"Do you want to delete this bookmark?")
					.setPositiveButton("Yes",
							dialogClickListener)
					.setNegativeButton("No",
							dialogClickListener).show();
			return allowLocation;
			

		}

	}

	class bookmarkListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			background.removeView(scrollBookmarks);
			isBookmarkShowing = false;
			int number = arg0.getId();
			background.addView(webFrame);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		openBookmarks();
	}

	@Override
	public boolean onLongClick(View v) {
		int id = v.getId();
		if (pageId == id && isBookmarkShowing) {
			background.removeView(scrollBookmarks);
			background.addView(webFrame);
			isBookmarkShowing = false;
		}
		main[id].clearView();
		deleteTab(id);

		return true;

	}

	public void deleteTab(int id) {
		int leftId = id;
		boolean right = false, left = false;
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
			main[pageId].stopLoading();
			if (isBookmarkShowing) {
				background.removeView(scrollBookmarks);
				background.addView(webFrame);
				isBookmarkShowing = false;
			}
			webFrame.removeView(main[pageId]);

			for (; id <= (number - 1);) {
				if (urlTitle[id].getVisibility() == View.VISIBLE) {
					webFrame.addView(main[id]);
					if (API < 16) {
						urlTitle[id].setBackgroundDrawable(getResources()
								.getDrawable(R.drawable.bg_press));
					} else {
						urlTitle[id].setBackground(getResources().getDrawable(
								R.drawable.bg_press));
					}
					urlTitle[id].setPadding(leftPad, 0, rightPad, 0);
					pageId = id;
					right = true;
					break;
				}
				id = id + 1;
			}
			if (right == false) {
				for (; leftId >= 0;) {

					if (urlTitle[leftId].getVisibility() == View.VISIBLE) {
						webFrame.addView(main[leftId]);
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
						left = true;
						break;
					}
					leftId = leftId - 1;
				}

			}
			getUrl.setText(urlToLoad[pageId][0]);
			if (right == false && left == false) {
				finish();
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub

		switch (item.getItemId()) {
		case R.id.refresh:
			if (main[pageId].getProgress() < 100) {
				main[pageId].stopLoading();
			} else {
				main[pageId].reload();
			}
			return true;
		case R.id.bookmark:
			addBookmark();
			return true;
		case R.id.settings:
			newSettings();
			return true;
		case R.id.allBookmarks:
			if(!isBookmarkShowing){
			goBookmarks();}
			return true;
		case R.id.share:
			share();
			return true;
		case R.id.forward:
			if(main[pageId].canGoForward()){
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
				// TODO Auto-generated method stub
				if(API>=11){
				PopupMenu menu = new PopupMenu(Barebones.this,v);
				MenuInflater inflate = menu.getMenuInflater();
				inflate.inflate(R.menu.menu, menu.getMenu());
				menu.setOnMenuItemClickListener(new OnMenuItemClickListener(){

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						// TODO Auto-generated method stub

						// TODO Auto-generated method stub
						switch (item.getItemId()) {
						case R.id.refresh:
							if (main[pageId].getProgress() < 100) {
								main[pageId].stopLoading();
							} else {
								main[pageId].reload();
							}
							return true;
						case R.id.bookmark:
							addBookmark();
							return true;
						case R.id.settings:
							newSettings();
							return true;
						case R.id.allBookmarks:
							if(!isBookmarkShowing){
							goBookmarks();}
							return true;
						case R.id.share:
							share();
							return true;
						case R.id.forward:
							if(main[pageId].canGoForward()){
								main[pageId].goForward();
							}
							return true;
						default:
							return false;
						}
					
					}
					
				});
				menu.show();}
				else if(API<11){
				openOptionsMenu();}
			}

		});
		options.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View arg0) {
				// TODO Auto-generated method stub
				return true;
			}
			
		});
	}

	
	
	public void enter() {
		getUrl.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
				// TODO Auto-generated method stub
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
			background.removeView(scrollBookmarks);
			background.addView(webFrame);
			isBookmarkShowing = false;
		}
		if (fixedQuery.contains(" ") || period == false) {
			fixedQuery.replaceAll(" ", "+");
			main[pageId]
					.loadUrl("http://www.google.com/search?q=" + fixedQuery);
		} else if (fixedQuery.contains("http//") == false
				&& fixedQuery.contains("https//") == false
				&& fixedQuery.contains("http://") == false
				&& fixedQuery.contains("https://") == false) {
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
		if(isPhone){
			RelativeLayout relativeLayout1 = (RelativeLayout)findViewById(R.id.relativeLayout1);
			relativeLayout1.removeView(exit);
		}
			exit.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (isBookmarkShowing) {
					background.removeView(scrollBookmarks);
					background.addView(webFrame);
					urlTitle[pageId].setText(urlToLoad[pageId][1]);
					getUrl.setText(urlToLoad[pageId][0]);
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
		if(isPhone){
			RelativeLayout relativeLayout1 = (RelativeLayout)findViewById(R.id.relativeLayout1);
			relativeLayout1.removeView(forward);
		}
		forward.setOnClickListener(new OnClickListener() {

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
			background.removeView(scrollBookmarks);
			background.addView(webFrame);
			urlTitle[pageId].setText(urlToLoad[pageId][1]);
			getUrl.setText(urlToLoad[pageId][0]);
			isBookmarkShowing = false;
		} else if (mCustomView!=null&&mCustomView.isShown()&&!main[pageId].isShown()){
			// Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            webFrame.removeView(mCustomView);
            mCustomView = null;
            webFrame.addView(main[pageId]);
            mCustomViewCallback.onCustomViewHidden();

            main[pageId].setVisibility(View.VISIBLE);
           
		}else {
		
			if (main[pageId].canGoBack()) {
				main[pageId].goBack();
			} else {
				deleteTab(pageId);
			}
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {

			deleteHistory = settings.getBoolean("history", false);
			if (deleteHistory == true) {
				for (int num = 0; num <= pageId; num++) {
					main[pageId].clearHistory();
				}
			}
			trimCache(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void trimCache(Context context) {
		try {
			File dir = context.getCacheDir();
			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception e) {
			// TODO: handle exception
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
		// TODO Auto-generated method stub

		final int x = (int) event.getX();
		final int y = (int) event.getY();
		final int fuzz = 10;
		final Rect edge = new Rect();
		v.getLocalVisibleRect(edge);
		if (x >= (edge.right - bounds[v.getId()].width() - fuzz)
				&& x <= (edge.right - v.getPaddingRight() + fuzz)
				&& y >= (v.getPaddingTop() - fuzz)
				&& y <= (v.getHeight() - v.getPaddingBottom()) + fuzz
				&& event.getActionMasked() == event.ACTION_UP) {
			if (pageId == v.getId() && isBookmarkShowing) {
				background.removeView(scrollBookmarks);
				background.addView(webFrame);
				isBookmarkShowing = false;
			}
			main[v.getId()].clearView();
			deleteTab(v.getId());
		} else if (pageId == v.getId()) {
		} else {
			if (isBookmarkShowing) {
				background.removeView(scrollBookmarks);
				background.addView(webFrame);
				isBookmarkShowing = false;
			}
			if (API < 16) {
				urlTitle[pageId].setBackgroundDrawable(getResources()
						.getDrawable(R.drawable.bg_inactive));
			} else {
				urlTitle[pageId].setBackground(getResources().getDrawable(
						R.drawable.bg_inactive));
			}
			urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
			webFrame.removeView(main[pageId]);
			pageId = v.getId();
			if (API < 16) {
				urlTitle[pageId].setBackgroundDrawable(getResources()
						.getDrawable(R.drawable.bg_press));
			} else {
				urlTitle[pageId].setBackground(getResources().getDrawable(
						R.drawable.bg_press));
			}
			urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
			webFrame.addView(main[pageId]);
			getUrl.setText(urlToLoad[pageId][0]);
		}
		return false;
	}


}
