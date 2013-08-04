package acr.browser.barebones.activities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import acr.browser.barebones.R;
import acr.browser.barebones.customwebview.CustomWebView;
import acr.browser.barebones.databases.DatabaseHandler;
import acr.browser.barebones.databases.SpaceTokenizer;
import acr.browser.barebones.utilities.BookmarkPageVariables;
import acr.browser.barebones.utilities.FinalVariables;
import acr.browser.barebones.utilities.HistoryPageVariables;
import acr.browser.barebones.utilities.Utils;
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
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
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
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class BarebonesActivity extends Activity {

	@SuppressLint("SetJavaScriptEnabled")
	public class CustomChromeClient extends WebChromeClient {
		public Bitmap mDefaultVideoPoster;
		public View mVideoProgressView;
		public FrameLayout fullScreenContainer;
		public int orientation;

		@Override
		public void onExceededDatabaseQuota(String url,
				String databaseIdentifier, long quota,
				long estimatedDatabaseSize, long totalQuota,
				QuotaUpdater quotaUpdater) {
			quotaUpdater.updateQuota(totalQuota + estimatedDatabaseSize);

		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			
			
			super.onProgressChanged(view, newProgress);
		}

		@Override
		public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
				QuotaUpdater quotaUpdater) {
			quotaUpdater.updateQuota(quota + requiredStorage);
		}

		@Override
		public Bitmap getDefaultVideoPoster() {
			if (mDefaultVideoPoster == null) {
				mDefaultVideoPoster = BitmapFactory.decodeResource(
						getResources(), android.R.color.black);
			}
			return mDefaultVideoPoster;
		}

		@Override
		public View getVideoLoadingProgressView() {
			if (mVideoProgressView == null) {
				LayoutInflater inflater = LayoutInflater.from(getBaseContext());
				mVideoProgressView = inflater.inflate(
						android.R.layout.simple_spinner_item, null);
			}
			return mVideoProgressView;
		}

		@Override
		public void onCloseWindow(WebView window) {
			Message msg = Message.obtain();
			msg.what = 3;
			msg.arg1 = window.getId();
			browserHandler.sendMessage(msg);
			super.onCloseWindow(window);
		}

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog,
				boolean isUserGesture, final Message resultMsg) {

			if (isUserGesture) {
				newTab(number, "", true, false);
				WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
				transport.setWebView(main[pageId]);
				resultMsg.sendToTarget();
				browserHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						main[pageId].loadUrl(getUrl.getText().toString());
					}
				}, 500);
			}
			return true;
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(final String origin,
				final GeolocationPermissions.Callback callback) {

			if (!allowLocation) {
				callback.invoke(origin, false, false);
			}
			if (allowLocation) {
				final boolean remember = true;
				AlertDialog.Builder builder = new AlertDialog.Builder(CONTEXT);
				builder.setTitle("Location Access");
				String org = null;
				if (origin.length() > 50) {
					org = (String) origin.subSequence(0, 50) + "...";
				} else {
					org = origin;
				}
				builder.setMessage(org + "\nWould like to use your Location ")
						.setCancelable(true)
						.setPositiveButton("Allow",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										callback.invoke(origin, true, remember);
									}
								})
						.setNegativeButton("Don't Allow",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										callback.invoke(origin, false, remember);
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}

		@Override
		public void onHideCustomView() {
			if (mCustomView == null && mCustomViewCallback == null) {
				return;
			}
			mCustomView.setKeepScreenOn(false);
			FrameLayout screen = (FrameLayout) getWindow().getDecorView();
			screen.removeView(fullScreenContainer);
			fullScreenContainer = null;
			mCustomView = null;
			mCustomViewCallback.onCustomViewHidden();
			setRequestedOrientation(orientation);
			background.addView(main[pageId]);
			uBar.setVisibility(View.VISIBLE);
			uBar.bringToFront();
		}

		@Override
		public void onReceivedIcon(WebView view, Bitmap favicon) {
			setFavicon(view.getId(), favicon);
		}

		@Override
		public void onReceivedTitle(final WebView view, final String title) {
			numberPage = view.getId();
			if (title != null && title.length() != 0) {
				urlTitle[numberPage].setText(title);
				urlToLoad[numberPage][1] = title;
				Utils.updateHistory(CONTEXT, getContentResolver(),
						noStockBrowser, urlToLoad[numberPage][0], title);
			}
			super.onReceivedTitle(view, title);
		}

		@Override
		public void onShowCustomView(View view, int requestedOrientation,
				CustomViewCallback callback) {
			if (mCustomView != null) {
				callback.onCustomViewHidden();
				return;
			}
			view.setKeepScreenOn(true);
			orientation = getRequestedOrientation();
			FrameLayout screen = (FrameLayout) getWindow().getDecorView();
			fullScreenContainer = new FrameLayout(getBaseContext());
			fullScreenContainer.setBackgroundColor(getResources().getColor(
					R.color.black));
			background.removeView(main[pageId]);
			uBar.setVisibility(View.GONE);
			fullScreenContainer.addView(view,
					ViewGroup.LayoutParams.MATCH_PARENT);
			screen.addView(fullScreenContainer,
					ViewGroup.LayoutParams.MATCH_PARENT);
			mCustomView = view;
			mCustomViewCallback = callback;
			setRequestedOrientation(requestedOrientation);

		}

		@Override
		public void onShowCustomView(View view,
				WebChromeClient.CustomViewCallback callback) {
			if (mCustomView != null) {
				callback.onCustomViewHidden();
				return;
			}
			view.setKeepScreenOn(true);
			orientation = getRequestedOrientation();
			FrameLayout screen = (FrameLayout) getWindow().getDecorView();
			fullScreenContainer = new FrameLayout(getBaseContext());
			fullScreenContainer.setBackgroundColor(getResources().getColor(
					R.color.black));
			background.removeView(main[pageId]);
			uBar.setVisibility(View.GONE);
			fullScreenContainer.addView(view,
					ViewGroup.LayoutParams.MATCH_PARENT);
			screen.addView(fullScreenContainer,
					ViewGroup.LayoutParams.MATCH_PARENT);
			mCustomView = view;
			mCustomViewCallback = callback;
			setRequestedOrientation(getRequestedOrientation());
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg) {
			mUploadMessage = uploadMsg;  
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
            i.addCategory(Intent.CATEGORY_OPENABLE);  
            i.setType("image/*");  
            startActivityForResult(Intent.createChooser(i,"File Chooser"), 1);  
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType) {
			   mUploadMessage = uploadMsg;
	           Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	           i.addCategory(Intent.CATEGORY_OPENABLE);
	           i.setType("*/*");
	           startActivityForResult(
	           Intent.createChooser(i, "File Browser"),1);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType, String capture) {
			openFileChooser(uploadMsg);
		}
	}

	public class CustomDownloadListener implements DownloadListener {

		@Override
		public void onDownloadStart(final String url, String userAgent,
				final String contentDisposition, final String mimetype,
				long contentLength) {
			if (url.endsWith(".mp4")||url.endsWith(".m4a")) {

				AlertDialog.Builder builder = new AlertDialog.Builder(CONTEXT);
				builder.setTitle("Open as...");
				builder.setMessage(
						"Do you want to download this video or watch it in an app?")
						.setCancelable(true)
						.setPositiveButton("Download",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										Utils.downloadFile(CONTEXT, url,
												contentDisposition, mimetype);
									}
								})
						.setNegativeButton("Watch",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										Intent intent = new Intent(
												Intent.ACTION_VIEW);
										intent.setDataAndType(Uri.parse(url),
												"video/mp4");
										intent.putExtra(
												"acr.browser.barebones.Download",
												1);
										startActivity(intent);
									}
								});
				AlertDialog alert = builder.create();
				alert.show();

			} else {
				Utils.downloadFile(CONTEXT, url, contentDisposition, mimetype);
			}
		}

	}

	public class CustomWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			urlIntent
					.putExtra("acr.browser.barebones.Origin", view.getId() + 1);
			if (url.startsWith("market://")
					|| url.startsWith("http://play.google.com")
					|| url.startsWith("https://play.google.com")) {
				startActivity(urlIntent);
				return true;
			} else if (url.startsWith("http://www.youtube.com")
					|| url.startsWith("https://www.youtube.com")) {
				startActivity(urlIntent);
				return true;
			} else if (url.startsWith("http://maps.google.com")
					|| url.startsWith("https://maps.google.com")) {
				startActivity(urlIntent);
				return true;
			} else if (url.contains("tel:") || TextUtils.isDigitsOnly(url)) {
				startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
				return true;
			} else if (url.contains("mailto:")) {
				MailTo mailTo = MailTo.parse(url);
                Intent i = Utils.newEmailIntent(BarebonesActivity.this, mailTo.getTo(), mailTo.getSubject(), mailTo.getBody(), mailTo.getCc());
                startActivity(i);
                view.reload();
				return true;
			}
			return false;
		}

		@Override
		public void onReceivedHttpAuthRequest(final WebView view,
				final HttpAuthHandler handler, final String host,
				final String realm) {

			AlertDialog.Builder builder = new AlertDialog.Builder(CONTEXT);
			final EditText name = new EditText(CONTEXT);
			final EditText password = new EditText(CONTEXT);
			LinearLayout passLayout = new LinearLayout(CONTEXT);
			passLayout.setOrientation(LinearLayout.VERTICAL);

			passLayout.addView(name);
			passLayout.addView(password);

			name.setHint("Username");
			password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			password.setTransformationMethod(new PasswordTransformationMethod());
			password.setHint("Password");
			builder.setTitle("Sign in");
			builder.setView(passLayout);
			builder.setCancelable(true)
					.setPositiveButton("Sign in",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									String user = name.getText().toString();
									String pass = password.getText().toString();
									handler.proceed(user.trim(), pass.trim());
									Log.i("Lightning", "Request Login");

								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									handler.cancel();

								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		}

		@Override
		public void onScaleChanged(WebView view, float oldScale, float newScale) {
			if (view.isShown()) {
				view.invalidate();
			}
			super.onScaleChanged(view, oldScale, newScale);
		}

		@Override
		public void onPageFinished(WebView view, final String url) {
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
			super.onPageFinished(view, url);
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.i("Lightning", "Page Started");
			loadTime = System.currentTimeMillis();
			numberPage = view.getId();

			if (url.startsWith("file:///")) {
				view.getSettings().setUseWideViewPort(false);
			} else {
				view.getSettings().setUseWideViewPort(settings.getBoolean("wideviewport", true));
			}

			if (view.isShown()) {
				refresh.setVisibility(View.INVISIBLE);
				progressBar.setVisibility(View.VISIBLE);
				setUrlText(url);
			}

			urlTitle[numberPage].setCompoundDrawables(webpageOther, null,
					exitTab, null);
			if (favicon != null) {
				setFavicon(view.getId(), favicon);
			}

			getUrl.setPadding(tenPad, 0, tenPad, 0);
			urlToLoad[numberPage][0] = url;

			if (!uBar.isShown() && showFullScreen) {
				uBar.startAnimation(slideDown);
			}
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onReceivedSslError(WebView view,
				final SslErrorHandler handler, SslError error) {
			AlertDialog.Builder builder = new AlertDialog.Builder(CONTEXT);
			builder.setTitle("Warning");
			builder.setMessage(
					"The certificate of the site is not trusted. Proceed anyway?")
					.setCancelable(true)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									handler.proceed();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
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
		public void onFormResubmission(WebView view, final Message dontResend,
				final Message resend) {
			AlertDialog.Builder builder = new AlertDialog.Builder(CONTEXT);
			builder.setTitle("Form Resubmission");
			builder.setMessage("Would you like to resend the data?")
					.setCancelable(true)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {

									resend.sendToTarget();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {

									dontResend.sendToTarget();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
			super.onFormResubmission(view, dontResend, resend);
		}
	}

	public void generateHistory(final CustomWebView view) {

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

				list = new ArrayList<Map<String, String>>();
				try {
					if (historyCursor != null) {
						if (historyCursor.moveToLast()) {
							// Variable for holding the retrieved URL
							urlColumn = historyCursor.getColumnIndex("url");
							titleColumn = historyCursor.getColumnIndex("title");
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
				File historyWebPage = new File(getBaseContext().getFilesDir(),
						"history.html");
				try {
					FileWriter hWriter = new FileWriter(historyWebPage, false);
					hWriter.write(historyHtml);
					hWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (uBar.isShown()) {
					urlTitle[pageId].setText("History");
					setUrlText("");
					getUrl.setPadding(tenPad, 0, tenPad, 0);
				}

				view.loadUrl("file:///" + historyWebPage);
			}

		});
		history.run();
	}

	public void setUrlText(String url) {
		if (url != null) {
			if (!url.startsWith("file://")) {
				getUrl.setText(url);
			} else {
				getUrl.setText("");
			}
		}
	}

	public class TabTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			id = v.getId();
			background.clearDisappearingChildren();
			boolean xPress = false;
			int x = (int) event.getX();
			int y = (int) event.getY();
			Rect edge = new Rect();
			v.getLocalVisibleRect(edge);

			urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (x >= (edge.right - bounds.width() - v.getPaddingRight() - 10 * 3 / 2)
						&& x <= (edge.right - v.getPaddingRight() + 10 * 3 / 2)
						&& y >= (v.getPaddingTop() - 10 / 2)
						&& y <= (v.getHeight() - v.getPaddingBottom() + 10 / 2)) {
					xPress = true;
				}
				if (id == pageId) {
					if (xPress) {
						deleteTab(id);
						uBar.bringToFront();
					}
				} else if (id != pageId) {
					if (xPress) {
						deleteTab(id);
					} else {
						if (API < 16) {
							urlTitle[pageId].setBackgroundDrawable(inactive);
						} else if (API > 15) {
							urlTitle[pageId].setBackground(inactive);
						}
						urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
						if (!showFullScreen) {
							background.addView(main[id]);
							main[id].startAnimation(fadeIn);
							main[pageId].startAnimation(fadeOut);
							background.removeView(main[pageId]);
							uBar.bringToFront();
						} else if (API >= 12) {
							main[id].setAlpha(0f);
							background.addView(main[id]);
							try {
								main[id].animate().alpha(1f)
										.setDuration(mShortAnimationDuration);
							} catch (NullPointerException ignored) {
							}
							background.removeView(main[pageId]);
							uBar.bringToFront();
						} else {
							background.removeView(main[pageId]);
							background.addView(main[id]);
						}
						uBar.bringToFront();

						pageId = id;
						setUrlText(urlToLoad[pageId][0]);
						getUrl.setPadding(tenPad, 0, tenPad, 0);
						if (API < 16) {
							urlTitle[pageId].setBackgroundDrawable(active);
						} else if (API > 15) {
							urlTitle[pageId].setBackground(active);
						}
						if (main[pageId].getProgress() < 100) {
							refresh.setVisibility(View.INVISIBLE);
							progressBar.setVisibility(View.VISIBLE);
						} else {
							progressBar.setVisibility(View.GONE);
							refresh.setVisibility(View.VISIBLE);
						}
						tabScroll.smoothScrollTo(urlTitle[pageId].getLeft(), 0);
						main[pageId].invalidate();
					}
				}

			}
			uBar.bringToFront();
			urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
			return true;
		}

	}

	public void removeView(WebView view) {
		if (!showFullScreen) {
			view.startAnimation(fadeOut);
		}
		background.removeView(view);
		uBar.bringToFront();
	}

	public void deleteBookmark(String url) {
		File book = new File(getBaseContext().getFilesDir(), "bookmarks");
		File bookUrl = new File(getBaseContext().getFilesDir(), "bookurl");
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
		openBookmarks(main[pageId]);
	}

	public class WebPageLongClickListener implements OnLongClickListener {

		@Override
		public boolean onLongClick(View v) {
			final HitTestResult result = main[pageId].getHitTestResult();
			if (main[pageId].getUrl().contains(
					"file:///" + getBaseContext().getFilesDir()
							+ "/bookmarks.html")) {
				if (result.getExtra() != null) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								int num = pageId;
								newTab(number, result.getExtra(), false, false);
								// urlTitle[num].performClick();
								pageId = num;
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								main[pageId].loadUrl(result.getExtra());
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {

								deleteBookmark(result.getExtra());
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							CONTEXT); // dialog
					builder.setMessage(
							"What would you like to do with this link?")
							.setPositiveButton("Open in New Tab",
									dialogClickListener)
							.setNegativeButton("Open Normally",
									dialogClickListener)
							.setNeutralButton("Delete", dialogClickListener)
							.show();
				}
				return true;
			} else if (result.getExtra() != null) {
				if (result.getType() == 5 && API > 8) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								int num = pageId;
								newTab(number, result.getExtra(), false, false);
								// urlTitle[num].performClick();
								pageId = num;
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								main[pageId].loadUrl(result.getExtra());
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API > 8) {
									String url = result.getExtra();

									Utils.downloadFile(CONTEXT, url, null, null);

								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							CONTEXT); // dialog
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
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								int num = pageId;
								newTab(number, result.getExtra(), false, false);
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
									clipboard.setText(result.getExtra());
								} else {
									ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
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
							CONTEXT); // dialog
					builder.setMessage(
							"What would you like to do with this link?")
							.setPositiveButton("Open in New Tab",
									dialogClickListener)
							.setNegativeButton("Open Normally",
									dialogClickListener)
							.setNeutralButton("Copy link", dialogClickListener)
							.show();
				}
				return true;
			} else {
				return false;
			}
		}

	}

	// variables to differentiate free from paid
	public static final int MAX_TABS = FinalVariables.MAX_TABS;
	public static final int MAX_BOOKMARKS = FinalVariables.MAX_BOOKMARKS;
	public static final boolean PAID_VERSION = FinalVariables.PAID_VERSION;

	public final Context CONTEXT = BarebonesActivity.this;

	public static final String HOMEPAGE = FinalVariables.HOMEPAGE;
	public static String SEARCH;

	public static SimpleAdapter adapter;
	public static MultiAutoCompleteTextView getUrl;
	public static final TextView[] urlTitle = new TextView[MAX_TABS];
	public final static CustomWebView[] main = new CustomWebView[MAX_TABS];
	public static Rect bounds;
	public static ValueCallback<Uri> mUploadMessage;
	public static ImageView refresh;
	public static ProgressBar progressBar;
	public static String defaultUser;
	public static Drawable webpageOther;
	public static Drawable incognitoPage;
	public static Drawable exitTab;
	public static int numberPage;
	public static long loadTime = 0;
	public static int number;
	public static int pageId = 0;
	public static int agentPicker;
	public static int enableFlash;
	public static int height32;
	public static int height;
	public static int width;
	public static int pixels;
	public static int leftPad;
	public static int rightPad;
	public static final int API = FinalVariables.API;
	public static int mShortAnimationDuration;
	public static int id;
	public static int tenPad;
	public static int urlColumn;
	public static int titleColumn;
	public static int closeWindow;
	public static View mCustomView = null;
	public static CustomViewCallback mCustomViewCallback;
	public static boolean isPhone = false;
	public static boolean allowLocation;
	public static boolean savePasswords;
	public static boolean deleteHistory;
	public static boolean saveTabs;
	public static boolean showFullScreen;
	public static boolean noStockBrowser = true;
	public static SharedPreferences settings;
	public static SharedPreferences.Editor edit;
	public static String desktop;
	public static String mobile;
	public static String user;
	public static String urlA;
	public static String title;
	public static String[] memoryURL = new String[MAX_TABS];
	public static final String[] bUrl = new String[MAX_BOOKMARKS];
	public static final String[] bTitle = new String[MAX_BOOKMARKS];
	public static String[] columns;
	public static String homepage;
	public static final String preferences = "settings";
	public static String userAgent;
	public static final String[][] urlToLoad = new String[MAX_TABS][2];
	public static FrameLayout background;
	public static RelativeLayout uBar;
	public static HorizontalScrollView tabScroll;
	public static Animation slideUp;
	public static Animation slideDown;
	public static Animation fadeOut;
	public static Animation fadeIn;

	public static CookieManager cookieManager;

	public static Uri bookmarks;
	public static List<Map<String, String>> list;
	public static Map<String, String> map;

	public static Handler handler, browserHandler;

	public static DatabaseHandler historyHandler;

	public static StringBuilder sb;

	public static Runnable update;

	public static SQLiteDatabase s;

	public static Drawable inactive;

	public static Drawable active;

	public static LinearLayout tabLayout;
	
	public static String[] GetArray(String input) {
		return input.split("\\|\\$\\|SEPARATOR\\|\\$\\|");
	}

	@SuppressWarnings("unused")
	public static void setFavicon(int id, Bitmap favicon) {
		Drawable icon = null;
		icon = new BitmapDrawable(null, favicon);
		icon.setBounds(0, 0, width / 2, height / 2);
		if (icon != null) {
			urlTitle[id].setCompoundDrawables(icon, null, exitTab, null);
		} else {
			urlTitle[id]
					.setCompoundDrawables(webpageOther, null, exitTab, null);
		}
		icon = null;

	}

	@SuppressLint("SetJavaScriptEnabled")
	CustomWebView browserSettings(CustomWebView view) {
		view.setAnimationCacheEnabled(false);
		view.setDrawingCacheEnabled(true);
		view.setBackgroundColor(getResources().getColor(android.R.color.white));
		view.setDrawingCacheBackgroundColor(getResources().getColor(
				android.R.color.white));
		view.setWillNotCacheDrawing(false);
		view.setLongClickable(true);
		view.setAlwaysDrawnWithCacheEnabled(true);
		view.setScrollbarFadingEnabled(true);
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.setSaveEnabled(true);
		view.setBackgroundColor(0xFFFFFFFF);
		WebSettings webViewSettings = view.getSettings();
		if (settings.getBoolean("java", true)) {
			webViewSettings.setJavaScriptEnabled(true);
			webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true); // TODO
																			// not
																			// sure
																			// whether
																			// to
																			// enable
																			// or
																			// disable
		}
		
		webViewSettings.setAllowFileAccess(true);
		if (API < 14) {
			switch (settings.getInt("textsize", 3)) {
			case 1:
				webViewSettings.setTextSize(WebSettings.TextSize.LARGEST);
				break;
			case 2:
				webViewSettings.setTextSize(WebSettings.TextSize.LARGER);
				break;
			case 3:
				webViewSettings.setTextSize(WebSettings.TextSize.NORMAL);
				break;
			case 4:
				webViewSettings.setTextSize(WebSettings.TextSize.SMALLER);
				break;
			case 5:
				webViewSettings.setTextSize(WebSettings.TextSize.SMALLEST);
				break;
			}

		} else {
			switch (settings.getInt("textsize", 3)) {
			case 1:
				webViewSettings.setTextZoom(200);
				break;
			case 2:
				webViewSettings.setTextZoom(150);
				break;
			case 3:
				webViewSettings.setTextZoom(100);
				break;
			case 4:
				webViewSettings.setTextZoom(75);
				break;
			case 5:
				webViewSettings.setTextZoom(50);
				break;
			}
		}
		webViewSettings.setSupportMultipleWindows(settings.getBoolean(
				"newwindow", true));
		webViewSettings.setDomStorageEnabled(true);
		webViewSettings.setAppCacheEnabled(true);
		webViewSettings.setAppCachePath(getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/cache");

		switch (settings.getInt("enableflash", 0)) {
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

		if (API < 18) {
			if (settings.getBoolean("passwords", false)) {
				webViewSettings.setSavePassword(true);
				webViewSettings.setSaveFormData(true);
			}
			try {
				webViewSettings.setLightTouchEnabled(true);
				webViewSettings.setRenderPriority(RenderPriority.HIGH);

			} catch (SecurityException ignored) {

			}
		} else {
			if (settings.getBoolean("passwords", false)) {
				webViewSettings.setSaveFormData(true);
			}
		}
		webViewSettings.setGeolocationEnabled(true);
		webViewSettings.setGeolocationDatabasePath(getApplicationContext()
				.getFilesDir().getAbsolutePath());
		webViewSettings.setDatabaseEnabled(true);
		webViewSettings.setDatabasePath(getApplicationContext().getFilesDir()
				.getAbsolutePath() + "/databases");

		webViewSettings.setUserAgentString(userAgent);
		webViewSettings.setSupportZoom(true);
		webViewSettings.setBuiltInZoomControls(true);
		webViewSettings.setUseWideViewPort(settings.getBoolean("wideviewport",
				true));
		webViewSettings.setLoadWithOverviewMode(settings.getBoolean(
				"overviewmode", true));
		if (API >= 11) {
			webViewSettings.setDisplayZoomControls(false);
			webViewSettings.setAllowContentAccess(true);
		}
		if (settings.getBoolean("textreflow", false)) {
			webViewSettings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
		} else {
			webViewSettings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		}

		webViewSettings.setBlockNetworkImage(settings.getBoolean("blockimages",
				false));
		webViewSettings.setLoadsImagesAutomatically(true);
		return view;
	}

	void deleteTab(final int del) {
		if (API >= 11) {
			main[del].onPause();
		}
		main[del].stopLoading();
		main[del].clearHistory();
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
				urlTitle[del].setVisibility(View.GONE);
				findNewView(del);
				main[del] = null;
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

	void findNewView(int id) {
		int leftId = id;
		boolean right = false, left = false;
		if (id == pageId) {

			if (main[id].isShown()) {
				// background.removeView(main[id]);
				removeView(main[id]);
			}
			for (; id <= (number - 1); id++) {
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
					pageId = id;
					setUrlText(urlToLoad[pageId][0]);
					getUrl.setPadding(tenPad, 0, tenPad, 0);
					right = true;
					if (main[id].getProgress() < 100) {
						refresh.setVisibility(View.INVISIBLE);
						progressBar.setVisibility(View.VISIBLE);
					} else {
						progressBar.setVisibility(View.GONE);
						refresh.setVisibility(View.VISIBLE);
					}
					break;
				}

			}
			if (!right) {
				for (; leftId >= 0; leftId--) {

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
						pageId = leftId;
						setUrlText(urlToLoad[pageId][0]);
						getUrl.setPadding(tenPad, 0, tenPad, 0);
						left = true;
						if (main[leftId].getProgress() < 100) {
							refresh.setVisibility(View.INVISIBLE);
							progressBar.setVisibility(View.VISIBLE);
						} else {
							progressBar.setVisibility(View.GONE);
							refresh.setVisibility(View.VISIBLE);
						}
						break;
					}

				}

			}

		} else {
			right = left = true;
		}

		if (!(right || left)) {
			finish();
		}
		uBar.bringToFront();
		tabScroll.smoothScrollTo(urlTitle[pageId].getLeft(), 0);
	}

	@Override
	public void onLowMemory() {
		for (int n = 0; n < MAX_TABS; n++) {
			if (n != pageId && main[n] != null) {
				main[n].freeMemory();
			}
		}
		super.onLowMemory();
	}

	void enter() {
		getUrl.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

				switch (arg1) {
				case KeyEvent.KEYCODE_ENTER:
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					searchTheWeb(getUrl.getText().toString());
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
					searchTheWeb(getUrl.getText().toString());
					return true;
				}
				return false;
			}

		});
	}

	@SuppressLint("HandlerLeak")
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
					adapter = new SimpleAdapter(CONTEXT, list,
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
					Log.i("Barebones", "detected AOSP browser");
				} else {
					noStockBrowser = true;
					Log.e("Barebones", "did not detect AOSP browser");
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

					handler.sendEmptyMessage(1);

				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}

				list = new ArrayList<Map<String, String>>();
				try {
					if (managedCursor != null) {

						if (managedCursor.moveToLast()) {

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
							} while (managedCursor.moveToPrevious());
						}
					}
				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}
				managedCursor.close();
			}

		});
		updateAutoComplete.setPriority(3);
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
					searchTheWeb(url);
					url = null;
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

				if (main[pageId].canGoBack()) {
					main[pageId].goBack();
				} else {
					deleteTab(pageId);
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

	@Override
	public void finish() {
		background.clearDisappearingChildren();
		tabScroll.clearDisappearingChildren();
		if (settings.getBoolean("cache", false)) {
			main[pageId].clearCache(true);
			Log.i("Lightning", "Cache Cleared");
		}
		super.finish();
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
				if (main[pageId].canGoForward()) {
					main[pageId].goForward();
				}
			}

		});
	}

	void goBookmarks(CustomWebView view) {
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
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
		openBookmarks(view);
	}

	@SuppressLint("InlinedApi")
	void initialize() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		historyHandler = new DatabaseHandler(this);
		cookieManager = CookieManager.getInstance();
		CookieSyncManager.createInstance(CONTEXT);
		cookieManager.setAcceptCookie(settings.getBoolean("cookies", true));

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
		RelativeLayout bg = (RelativeLayout) findViewById(R.id.background);
		slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
		slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
		fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		fadeOut.setDuration(250);
		fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		// mShortAnimationDuration = getResources().getInteger(
		// android.R.integer.config_mediumAnimTime);
		mShortAnimationDuration = 250;
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
			user = new WebView(CONTEXT).getSettings().getUserAgentString();
		} else {
			user = WebSettings.getDefaultUserAgent(this);
		}

		background = (FrameLayout) findViewById(R.id.holder);
		defaultUser = user; // setting mobile user
		// agent
		mobile = FinalVariables.MOBILE_USER_AGENT;
		desktop = FinalVariables.DESKTOP_USER_AGENT; // setting
		// desktop user agent

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
		}

		exitTab = getResources().getDrawable(R.drawable.stop); // user
		// agent
		homepage = settings.getString("home", HOMEPAGE); // initializing
															// the
															// stored
															// homepage
															// variable

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
		number = 0;

		webpageOther = getResources().getDrawable(R.drawable.webpage);
		incognitoPage = getResources().getDrawable(R.drawable.incognito);
		webpageOther.setBounds(0, 0, width / 2, height / 2);
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
				newTab(number, homepage, true, false);
				tabScroll.postDelayed(new Runnable() {
					@Override
					public void run() {
						tabScroll.smoothScrollTo(urlTitle[pageId].getLeft(), 0);
					}
				}, 100L);

			}
		});
		newTab.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (settings.getString("oldPage", "").length() > 0) {
					newTab(number, settings.getString("oldPage", ""), true,
							false);
					edit.putString("oldPage", "");
					edit.commit();
					tabScroll.postDelayed(new Runnable() {
						@Override
						public void run() {
							tabScroll.smoothScrollTo(
									urlTitle[pageId].getLeft(), 0);
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

				if (main[pageId].getProgress() < 100) {
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
		browserHandler = new Handle();

	}

	class Handle extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1: {
				main[pageId].loadUrl(getUrl.getText().toString());
				break;
			}
			case 2: {
				deleteTab(msg.arg1);
				break;
			}
			case 3: {
				main[pageId].invalidate();
				break;
			}
			}
			super.handleMessage(msg);
		}

	}

	void reopenOldTabs() {
		Intent url = getIntent();
		String URL = url.getDataString();
		boolean oldTabs = false;

		if (saveTabs) {
			if (URL != null) {
				// opens a new tab with the url if its there
				newTab(number, URL, true, false);
				main[number - 1].resumeTimers();
				oldTabs = true;

			}
			for (String aMemoryURL : memoryURL) {
				if (aMemoryURL.length() > 0) {
					if (number == 0) {
						newTab(number, "", !oldTabs, false);
						main[pageId].resumeTimers();
						main[pageId].getSettings().setCacheMode(
								WebSettings.LOAD_CACHE_ELSE_NETWORK);
						main[pageId].loadUrl(aMemoryURL);
					} else {
						newTab(number, "", false, false);
						main[number - 1].getSettings().setCacheMode(
								WebSettings.LOAD_CACHE_ELSE_NETWORK);
						main[number - 1].loadUrl(aMemoryURL);
					}
					oldTabs = true;
				}

			}

			if (!oldTabs) {
				newTab(number, homepage, true, false);
				main[number - 1].resumeTimers();
			}
		} else {
			if (URL != null) {
				// opens a new tab with the URL if its there
				newTab(number, URL, true, false);
				main[number - 1].resumeTimers();

			} else {
				// otherwise it opens the home-page
				newTab(number, homepage, true, false);
				main[number - 1].resumeTimers();

			}
		}
	}

	public CustomWebView makeTab(final int pageToView, String Url,
			final boolean display) {
		CustomWebView view = new CustomWebView(CONTEXT);
		view.setId(pageToView);
		allowLocation = settings.getBoolean("location", false);
		view.setWebViewClient(new CustomWebViewClient());
		view.setWebChromeClient(new CustomChromeClient());
		if (API > 8) {
			view.setDownloadListener(new CustomDownloadListener());
		}

		view.setOnLongClickListener(new WebPageLongClickListener());
		view = browserSettings(view);
		agentPicker = settings.getInt("agentchoose", 1);
		switch (agentPicker) {
		case 1:
			view.getSettings().setUserAgentString(defaultUser);
			break;
		case 2:
			view.getSettings().setUserAgentString(desktop);
			break;
		case 3:
			view.getSettings().setUserAgentString(mobile);
			break;
		}
		if (display) {
			background.removeView(main[pageId]);
			background.addView(view);
			view.requestFocus();
			pageId = pageToView;
		}
		uBar.bringToFront();
		if (Url.contains("about:home")) {
			goBookmarks(view);
		} else if (Url.contains("about:blank")) {
			view.loadUrl("");
		} else {
			if (!Url.startsWith("http") && Url != "") {
				Url = "http://" + Url;
			}
			view.loadUrl(Url);

		}
		Log.i("Barebones", "tab complete");
		return view;
	}

	void newSettings() {
		startActivity(new Intent(FinalVariables.SETTINGS_INTENT));
	}

	// new tab method, takes the id of the tab to be created and the url to load
	int newTab(int theId, final String theUrl, final boolean display,
			final boolean incognito_mode) {
		Log.i("Barebones", "making tab");

		int finalID = 0;
		homepage = settings.getString("home", HOMEPAGE);
		allowLocation = settings.getBoolean("location", false);
		boolean reuseWebView = false;

		for (int num = 0; num < number; num++) {
			if (urlTitle[num].getVisibility() == View.GONE) {

				final int n = num;
				Animation holo = AnimationUtils.loadAnimation(this, R.anim.up);
				holo.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationEnd(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationStart(Animation animation) {
						urlTitle[n].setVisibility(View.VISIBLE);
					}

				});
				urlTitle[n].startAnimation(holo);
				urlTitle[num].setText("New Tab");

				if (display) {
					if (API < 16) {
						urlTitle[num].setBackgroundDrawable(active);
					} else {
						urlTitle[num].setBackground(active);
					}
				} else {
					if (API < 16) {
						urlTitle[num].setBackgroundDrawable(inactive);
					} else {
						urlTitle[num].setBackground(inactive);
					}
				}
				urlTitle[num].setPadding(leftPad, 0, rightPad, 0);
				if (display) {
					if (API < 16) {
						urlTitle[pageId].setBackgroundDrawable(inactive);
					} else {
						urlTitle[pageId].setBackground(inactive);
					}
				}
				urlTitle[num].setCompoundDrawables(webpageOther, null, exitTab,
						null);
				urlTitle[num].setPadding(leftPad, 0, rightPad, 0);
				urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
				main[num] = makeTab(num, theUrl, display);
				finalID = num;
				pageId = num;

				uBar.bringToFront();

				if (API >= 11) {
					main[num].onResume();
				}

				reuseWebView = true;
				break;
			}
		}
		if (!reuseWebView) {
			if (number < MAX_TABS) {
				if (number > 0) {
					if (display) {
						if (API < 16) {
							urlTitle[pageId].setBackgroundDrawable(inactive);
						} else {
							urlTitle[pageId].setBackground(inactive);
						}

						urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
					}
				}
				final TextView title = new TextView(CONTEXT);
				title.setText("New Tab");
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
				title.setId(number);
				title.setGravity(Gravity.CENTER_VERTICAL);

				title.setCompoundDrawables(webpageOther, null, exitTab, null);

				Drawable[] drawables = title.getCompoundDrawables();
				bounds = drawables[2].getBounds();
				title.setOnTouchListener(new TabTouchListener());
				Animation holo = AnimationUtils.loadAnimation(this, R.anim.up);
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
				urlTitle[number] = title;

				urlTitle[number].setText("New Tab");

				if (theUrl != null) {
					main[number] = makeTab(number, theUrl, display);
				} else {
					main[number] = makeTab(number, homepage, display);
				}
				finalID = number;
				number = number + 1;
			}
		}
		if (!reuseWebView && number >= MAX_TABS) {
			Toast.makeText(CONTEXT, "Maximum number of tabs reached...",
					Toast.LENGTH_SHORT).show();
		}
		return finalID;

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
		if (showFullScreen && !uBar.isShown()) {
			uBar.startAnimation(slideDown);
		}
		if (main[pageId].isShown() && main[pageId].canGoBack()) {
			main[pageId].goBack();
		} else {
			deleteTab(pageId);
			uBar.bringToFront();
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO
		super.onConfigurationChanged(newConfig);
		if (settings.getBoolean("textreflow", false)) {
			main[pageId].getSettings().setLayoutAlgorithm(
					LayoutAlgorithm.NARROW_COLUMNS);
		} else {
			main[pageId].getSettings().setLayoutAlgorithm(
					LayoutAlgorithm.NORMAL);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); // displays main xml layout
		settings = getSharedPreferences(preferences, 0);
		edit = settings.edit();

		saveTabs = settings.getBoolean("savetabs", true);
		if (saveTabs) {
			String mem = settings.getString("memory", "");
			edit.putString("memory", "");
			memoryURL = GetArray(mem);
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
		int first = settings.getInt("first", 0);

		if (first == 0) { // This dialog alerts the user to some navigation
							// techniques
			String message = "\nLong-press back button to exit browser"
					+ "\n\nSet your homepage in settings"
					+ "\n\nTurn on Flash in settings"
					+ "\n\nTurn on Full-Screen mode in settings"
					+ "\n\nLong-press a link for more options"
					+ "\n\nCheck out advanced settings for more stuff!";
			Utils.createInformativeDialog(CONTEXT, "Browser Tips", message);
			edit.putInt("first", 1);
			edit.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(!settings.getBoolean("restoreclosed", true)){
				for(int n = 0; n<MAX_TABS; n++){
					urlToLoad[n][0] = null;
				}
			}
			finish();
			return true;
		} else
			return super.onKeyLongPress(keyCode, event);
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
			Utils.downloadFile(CONTEXT, url, null, null);
		} else if (url != null) {
			newTab(number, url, true, false);
		}

		super.onNewIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.history:
			generateHistory(main[pageId]);
			return true;
		case R.id.bookmark:
			if (urlToLoad[pageId][1] != null) {
				if (!urlToLoad[pageId][1].equals("Bookmarks")) {
					Utils.addBookmark(CONTEXT, urlToLoad[pageId][1],
							urlToLoad[pageId][0]);
				}
			}
			return true;
		case R.id.settings:
			newSettings();
			return true;
		case R.id.allBookmarks:
			if (urlToLoad[pageId][1] == null) {
				goBookmarks(main[pageId]);
			} else if (!urlToLoad[pageId][1].equals("Bookmarks")) {
				goBookmarks(main[pageId]);
			}

			return true;
		case R.id.share:
			share();
			return true;
		case R.id.incognito:
			startActivity(new Intent(FinalVariables.INCOGNITO_INTENT));
			// newTab(number, homepage, true, true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (main[pageId] != null) {
			if (API >= 11) {
				main[pageId].onPause();
			}
			main[pageId].pauseTimers();
		}
		Thread remember = new Thread(new Runnable() {

			@Override
			public void run() {
				String s = "";
				for (int n = 0; n < MAX_TABS; n++) {
					if (urlToLoad[n][0] != null) {
						s = s + urlToLoad[n][0] + "|$|SEPARATOR|$|";
					}
				}
				edit.putString("memory", s);
				edit.commit();
			}
		});
		remember.start();

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (main[pageId].getProgress() == 100) {
			progressBar.setVisibility(View.GONE);
			refresh.setVisibility(View.VISIBLE);
		}
		if (API >= 11) {
			main[pageId].onResume();
		}
		main[pageId].resumeTimers();

	}

	void openBookmarks(CustomWebView view) {
		String bookmarkHtml = BookmarkPageVariables.Heading;

		for (int n = 0; n < MAX_BOOKMARKS; n++) {
			if (bUrl[n] != null) {
				bookmarkHtml += (BookmarkPageVariables.Part1 + bUrl[n]
						+ BookmarkPageVariables.Part2 + bUrl[n]
						+ BookmarkPageVariables.Part3 + bTitle[n] + BookmarkPageVariables.Part4);
			}
		}
		bookmarkHtml += BookmarkPageVariables.End;
		File bookmarkWebPage = new File(getBaseContext().getFilesDir(),
				"bookmarks.html");
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkHtml);
			bookWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		view.loadUrl("file:///" + bookmarkWebPage);

		if (uBar.isShown()) {
			urlTitle[pageId].setText("Bookmarks");
			setUrlText("");
			getUrl.setPadding(tenPad, 0, tenPad, 0);
		}

	}

	void options() {
		ImageView options = (ImageView) findViewById(R.id.options);
		options.setBackgroundResource(R.drawable.button);
		options.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (API >= 11) {
					PopupMenu menu = new PopupMenu(CONTEXT, v);
					MenuInflater inflate = menu.getMenuInflater();
					inflate.inflate(R.menu.menu, menu.getMenu());
					menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {

							switch (item.getItemId()) {
							case R.id.history:
								generateHistory(main[pageId]);
								return true;
							case R.id.bookmark:
								if (urlToLoad[pageId][1] != null) {
									if (!urlToLoad[pageId][1]
											.equals("Bookmarks")) {
										Utils.addBookmark(CONTEXT,
												urlToLoad[pageId][1],
												urlToLoad[pageId][0]);
									}
								}
								return true;
							case R.id.settings:
								newSettings();
								return true;
							case R.id.allBookmarks:
								if (urlToLoad[pageId][1] == null) {
									goBookmarks(main[pageId]);
								} else if (!urlToLoad[pageId][1]
										.equals("Bookmarks")) {
									goBookmarks(main[pageId]);
								}
								return true;
							case R.id.share:
								share();
								return true;
							case R.id.incognito:
								startActivity(new Intent(
										FinalVariables.INCOGNITO_INTENT));
								// newTab(number, homepage, true, true);
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
		options.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				return true;
			}

		});
	}

	void share() {
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

	void searchTheWeb(String query) {
		query = query.trim();
		main[pageId].stopLoading();

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
			goBookmarks(main[pageId]);
		} else if (query.contains("about:history")) {
			generateHistory(main[pageId]);
		} else if (isSearch) {
			query.replaceAll(" ", "+");
			main[pageId].loadUrl(SEARCH + query);
		} else if (!validURL) {
			main[pageId].loadUrl("http://" + query);
		} else {
			main[pageId].loadUrl(query);
		}
	}
}