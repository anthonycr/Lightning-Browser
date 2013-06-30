package acr.browser.barebones;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteMisuseException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
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
import android.webkit.WebViewDatabase;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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

public class Barebones extends Activity {

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
			super.onExceededDatabaseQuota(url, databaseIdentifier, quota,
					estimatedDatabaseSize, totalQuota, quotaUpdater);
		}

		@Override
		public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
				QuotaUpdater quotaUpdater) {
			quotaUpdater.updateQuota(quota + requiredStorage);
			super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
		}

		@Override
		public void onCloseWindow(WebView window) {
			closeWindow = window.getId();
			browserHandler.sendEmptyMessage(2);
			super.onCloseWindow(window);
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
		public boolean onCreateWindow(WebView view, boolean isDialog,
				boolean isUserGesture, final Message resultMsg) {

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
				builder.setTitle("Locations");
				String org = (String) origin.subSequence(0, 50);
				builder.setMessage(
						org + " Would like to use your Current Location ")
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
			if (!incognito[view.getId()]) {
				setFavicon(view.getId(), favicon);
			}
			super.onReceivedIcon(view, favicon);
		}

		@Override
		public void onReceivedTitle(final WebView view, final String title) {
			numberPage = view.getId();
			urlTitle[numberPage].setText(title);
			urlToLoad[numberPage][1] = title;
			if (title != null && !incognito[numberPage]) {
				updateHistory(urlToLoad[numberPage][0], title);
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
			Barebones.this.startActivityForResult(
					Intent.createChooser(i, "File Browser"), 1);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType) {
			mUploadMessage = uploadMsg;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("image/*");
			Barebones.this.startActivityForResult(
					Intent.createChooser(i, "File Browser"), 1);
		}

		public void openFileChooser(ValueCallback<Uri> uploadMsg,
				String acceptType, String capture) {
			mUploadMessage = uploadMsg;
			Intent i = new Intent(Intent.ACTION_GET_CONTENT);
			i.addCategory(Intent.CATEGORY_OPENABLE);
			i.setType("image/*");
			Barebones.this.startActivityForResult(
					Intent.createChooser(i, "File Browser"), 1);
		}
	}

	public class CustomDownloadListener implements DownloadListener {

		@Override
		public void onDownloadStart(final String url, String userAgent,
				final String contentDisposition, final String mimetype,
				long contentLength) {
			try {
				Thread downloader = new Thread(new Runnable() {
					@SuppressLint("InlinedApi")
					@Override
					public void run() {
						DownloadManager download = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
						Uri nice = Uri.parse(url);
						DownloadManager.Request it = new DownloadManager.Request(
								nice);
						String fileName = URLUtil.guessFileName(url,
								contentDisposition, mimetype);
						if (API >= 11) {
							it.allowScanningByMediaScanner();
							it.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
						}

						it.setDestinationInExternalPublicDir(
								Environment.DIRECTORY_DOWNLOADS, fileName);
						Log.i("Barebones", "Downloading" + fileName);
						download.enqueue(it);
					}
				});
				downloader.run();
			} catch (NullPointerException e) {
				Log.e("Barebones", "Problem downloading");
				Toast.makeText(CONTEXT, "Error Downloading File",
						Toast.LENGTH_SHORT).show();
			} catch (IllegalArgumentException e) {
				Log.e("Barebones", "Problem downloading");
				Toast.makeText(CONTEXT, "Error Downloading File",
						Toast.LENGTH_SHORT).show();
			} catch (SecurityException ignored) {

			}
		}

		// }

	}

	public class CustomWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			if (url.contains("market://") || url.contains("play.google.com")) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				return true;
			} else if (url.contains("youtube.com")) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				return true;
			} else if (url.contains("maps.google.com")) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				return true;
			}
			return false;
		}

		@Override
		public void onScaleChanged(WebView view, float oldScale, float newScale) {
			if (view != null) {
				view.invalidate();
			}
			super.onScaleChanged(view, oldScale, newScale);
		}

		@Override
		public void onPageFinished(WebView view, final String url) {
			if (view != null) {
				view.invalidate();
			}
			if (view.isShown()) {
				progressBar.setVisibility(View.GONE);
				refresh.setVisibility(View.VISIBLE);
				if (showFullScreen && uBar.isShown()) {
					uBar.startAnimation(slideUp);
				}
			}
			view.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

			pageIsLoading = false;

		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {

			numberPage = view.getId();
			if (view.isShown()) {
				refresh.setVisibility(View.INVISIBLE);
				progressBar.setVisibility(View.VISIBLE);
				setUrlText(url);
				pageIsLoading = true;
			}
			if (incognito[numberPage]) {
				urlTitle[numberPage].setCompoundDrawables(incognitoPage, null,
						exitTab, null);
			} else {
				urlTitle[numberPage].setCompoundDrawables(webpageOther, null,
						exitTab, null);
				if (favicon != null) {
					setFavicon(view.getId(), favicon);
				}
			}
			getUrl.setPadding(tenPad, 0, tenPad, 0);
			urlToLoad[numberPage][0] = url;

			if (!uBar.isShown() && showFullScreen) {
				uBar.startAnimation(slideDown);
			}
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
			alert.show();
			super.onReceivedSslError(view, handler, error);
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
				String historyHtml = HistoryPage.Heading;
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
								historyHtml += (HistoryPage.Part1 + h[n][0]
										+ HistoryPage.Part2 + h[n][1]
										+ HistoryPage.Part3 + h[n][2] + HistoryPage.Part4);
								n++;
							} while (n < 49 && historyCursor.moveToPrevious());
						}
					}
				} catch (SQLiteException ignored) {
				} catch (NullPointerException ignored) {
				} catch (IllegalStateException ignored) {
				}

				historyHtml += BookmarkPage.End;
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
		if (!url.contains("file:///" + getBaseContext().getFilesDir()
				+ "/bookmarks.html")
				&& !url.contains("file:///" + getBaseContext().getFilesDir()
						+ "/history.html")) {
			getUrl.setText(url);
		} else {
			getUrl.setText("");
		}
	}

	public class TabTouchListener implements OnTouchListener {

		@SuppressWarnings("deprecation")
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
				if (x >= (edge.right - bounds.width() - v.getPaddingRight() - fuzz * 3 / 2)
						&& x <= (edge.right - v.getPaddingRight() + fuzz * 3 / 2)
						&& y >= (v.getPaddingTop() - fuzz / 2)
						&& y <= (v.getHeight() - v.getPaddingBottom() + fuzz / 2)) {
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
									try {
										Thread down = new Thread(
												new Runnable() {
													@SuppressLint("InlinedApi")
													@Override
													public void run() {

														DownloadManager download = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
														Uri nice = Uri.parse(result
																.getExtra());
														DownloadManager.Request it = new DownloadManager.Request(
																nice);
														String fileName = URLUtil.guessFileName(
																result.getExtra(),
																null, null);

														if (API >= 11) {
															it.allowScanningByMediaScanner();
															it.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
														}

														it.setDestinationInExternalPublicDir(
																Environment.DIRECTORY_DOWNLOADS,
																fileName);
														Log.i("Barebones",
																"Downloading"
																		+ fileName);
														download.enqueue(it);
													}
												});
										down.run();
									} catch (NullPointerException e) {
										Log.e("Barebones",
												"Problem downloading");
										Toast.makeText(CONTEXT,
												"Error Downloading File",
												Toast.LENGTH_SHORT).show();
									} catch (IllegalArgumentException e) {
										Log.e("Barebones",
												"Problem downloading");
										Toast.makeText(CONTEXT,
												"Error Downloading File",
												Toast.LENGTH_SHORT).show();
									} catch (SecurityException ignored) {

									}
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
						@SuppressWarnings("deprecation")
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
	public static final int MAX_TABS = FinalVars.MAX_TABS;
	public static final int MAX_BOOKMARKS = FinalVars.MAX_BOOKMARKS;
	public static final boolean PAID_VERSION = FinalVars.PAID_VERSION;
	public final Context CONTEXT = Barebones.this;
	public static final String HOMEPAGE = FinalVars.HOMEPAGE;
	public static final String SEARCH = FinalVars.GOOGLE_SEARCH;
	public static SimpleAdapter adapter;
	public static MultiAutoCompleteTextView getUrl;
	public static final TextView[] urlTitle = new TextView[MAX_TABS];
	public static final CustomWebView[] main = new CustomWebView[MAX_TABS];
	public static Rect bounds;
	public static ValueCallback<Uri> mUploadMessage;
	public static ImageView refresh;
	public static ProgressBar progressBar;
	public static Drawable webpageOther;
	public static Drawable incognitoPage;
	public static Drawable exitTab;
	public static int numberPage;
	public static final int fuzz = 10;
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
	public static final int API = FinalVars.API;
	public static int mShortAnimationDuration;
	public static int id;
	public static int tenPad;
	public static int urlColumn;
	public static int titleColumn;
	public static int closeWindow;
	public static View mCustomView = null;
	public static CustomViewCallback mCustomViewCallback;
	public static final boolean[] incognito = new boolean[MAX_TABS];
	public static boolean isPhone = false;
	public static boolean pageIsLoading = false;
	public static boolean allowLocation;
	public static boolean savePasswords;
	public static boolean deleteHistory;
	public static boolean saveTabs;
	static boolean showFullScreen;
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
	public static String str;
	public static final String preferences = "settings";
	public static String query;
	public static String userAgent;
	public static final String[][] urlToLoad = new String[MAX_TABS][2];
	public static FrameLayout background;
	static RelativeLayout uBar;
	public static HorizontalScrollView tabScroll;
	static Animation slideUp;
	static Animation slideDown;
	public static Animation fadeOut;
	public static Animation fadeIn;
	public static TextView txt;

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
	}

	void addBookmark() {
		File book = new File(getBaseContext().getFilesDir(), "bookmarks");
		File bookUrl = new File(getBaseContext().getFilesDir(), "bookurl");
		try {
			BufferedReader readUrlRead = new BufferedReader(new FileReader(
					bookUrl));
			String u;
			int n = 0;
			while ((u = readUrlRead.readLine()) != null && n < MAX_BOOKMARKS) {
				if (u.contentEquals(urlToLoad[pageId][0])) {

					readUrlRead.close();
					return;
				}
				n++;
			}
			readUrlRead.close();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
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
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	CustomWebView browserSettings(CustomWebView view) {
		view.setAnimationCacheEnabled(false);
		view.setDrawingCacheEnabled(false);
		view.setDrawingCacheBackgroundColor(getResources().getColor(
				android.R.color.background_light));
		// view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

		view.setWillNotCacheDrawing(true);
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.setSaveEnabled(true);

		WebSettings webViewSettings = view.getSettings();

		boolean java = settings.getBoolean("java", true);
		if (java) {
			webViewSettings.setJavaScriptEnabled(true);
			webViewSettings.setJavaScriptCanOpenWindowsAutomatically(false);
		}
		webViewSettings.setBlockNetworkImage(false);
		webViewSettings.setAllowFileAccess(true);
		webViewSettings.setLightTouchEnabled(true);
		webViewSettings.setSupportMultipleWindows(true);
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
		if (savePasswords) {
			webViewSettings.setSavePassword(true);
			webViewSettings.setSaveFormData(true);
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
		webViewSettings.setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		webViewSettings.setLoadsImagesAutomatically(true);
		return view;
	}

	boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(dir, aChildren));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	@SuppressWarnings("deprecation")
	void deleteTab(final int del) {
		main[del].stopLoading();
		main[del].clearHistory();
		// main[del].clearView();
		urlToLoad[del][0] = null;
		urlToLoad[del][1] = null;
		if (API >= 11) {
			main[del].onPause();
		}

		// background.clearDisappearingChildren();
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

	@SuppressWarnings("deprecation")
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

	void enter() {
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
					txt = (TextView) arg1.findViewById(R.id.url);
					str = txt.getText().toString();
					main[pageId].loadUrl(str);
					setUrlText(str);
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
		try {
			deleteHistory = settings.getBoolean("history", false);
			if (deleteHistory) {
				cookieManager.removeAllCookie();
				main[0].clearCache(true);
				WebViewDatabase m = WebViewDatabase.getInstance(this);
				m.clearFormData();
				m.clearHttpAuthUsernamePassword();
				m.clearUsernamePassword();
				CONTEXT.deleteDatabase("historyManager");
				if (!noStockBrowser) {
					try {
						Browser.clearHistory(getContentResolver());
					} catch (NullPointerException ignored) {
					}
				}
				trimCache(CONTEXT);
			}

		} catch (Exception e) {
			Log.e("Lightning", "Error Clearing data");
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
	void init() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		historyHandler = new DatabaseHandler(this);
		cookieManager = CookieManager.getInstance();

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

		RelativeLayout refreshLayout = (RelativeLayout) findViewById(R.id.refreshLayout);
		refreshLayout.setBackgroundResource(R.drawable.button);

		// user agent
		user = new WebView(CONTEXT).getSettings().getUserAgentString();

		background = (FrameLayout) findViewById(R.id.holder);
		mobile = user; // setting mobile user
						// agent
		desktop = FinalVars.DESKTOP_USER_AGENT; // setting
		// desktop user agent
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
				initializeTabs(); // restores old tabs or creates a new one

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
		browserHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 1: {
					main[pageId].loadUrl(getUrl.getText().toString());
					break;
				}
				case 2: {
					deleteTab(closeWindow);
					break;
				}
				case 3: {
					main[pageId].invalidate();
					break;
				}
				}
			}
		};
	}

	void initializeTabs() {
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
						main[number - 1].resumeTimers();
						main[number - 1].getSettings().setCacheMode(
								WebSettings.LOAD_CACHE_ELSE_NETWORK);
						main[number - 1].loadUrl(aMemoryURL);
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
				// opens a new tab with the url if its there
				newTab(number, URL, true, false);
				main[number - 1].resumeTimers();

			} else {
				// otherwise it opens the homepage
				newTab(number, homepage, true, false);
				main[number - 1].resumeTimers();

			}
		}
	}

	public CustomWebView makeTab(final int pageToView, final String Url,
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
			view.getSettings().setUserAgentString(mobile);
			break;
		case 2:
			view.getSettings().setUserAgentString(desktop);
			break;
		case 3:
			userAgent = settings.getString("agent", user);
			view.getSettings().setUserAgentString(userAgent);
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

			view.loadUrl("about:blank");

		} else {

			view.loadUrl(Url);

		}
		Log.i("Barebones", "tab complete");
		return view;
	}

	void newSettings() {
		Intent set = new Intent(FinalVars.SETTINGS_INTENT);
		startActivity(set);
	}

	// new tab method, takes the id of the tab to be created and the url to load
	@SuppressWarnings("deprecation")
	int newTab(int theId, final String theUrl, final boolean display,
			final boolean incognito_mode) {
		Log.i("Barebones", "making tab");

		int finalID = 0;
		homepage = settings.getString("home", HOMEPAGE);
		allowLocation = settings.getBoolean("location", false);
		boolean isEmptyWebViewAvailable = false;

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
				incognito[num] = incognito_mode;
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
				urlTitle[pageId].setCompoundDrawables(webpageOther, null,
						exitTab, null);
				urlTitle[pageId].setPadding(leftPad, 0, rightPad, 0);
				main[num] = makeTab(num, theUrl, display);
				finalID = num;
				pageId = num;

				uBar.bringToFront();

				if (API >= 11) {
					main[num].onResume();
				}

				isEmptyWebViewAvailable = true;
				break;
			}
		}
		if (!isEmptyWebViewAvailable) {
			if (number < MAX_TABS) {
				incognito[number] = incognito_mode;
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
				if (incognito[number]) {
					title.setCompoundDrawables(incognitoPage, null, exitTab,
							null);
				} else {
					title.setCompoundDrawables(webpageOther, null, exitTab,
							null);
				}
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
				if (incognito[number]) {
					urlTitle[number].setText("Incognito");

				}
				if (theUrl != null) {
					main[number] = makeTab(number, theUrl, display);
				} else {
					main[number] = makeTab(number, homepage, display);
				}
				finalID = number;
				number = number + 1;
			}
		}
		if (!isEmptyWebViewAvailable && number >= MAX_TABS) {
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

		if (main[pageId] != null) {
			main[pageId].stopLoading();

			if (showFullScreen && !uBar.isShown()) {
				uBar.startAnimation(slideDown);
			}
			if (main[pageId].canGoBack()) {
				main[pageId].goBack();
			} else {
				deleteTab(pageId);
				uBar.bringToFront();
			}
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		main[pageId].getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
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
			memoryURL = null;
			memoryURL = GetArray(mem);
		}

		inactive = getResources().getDrawable(R.drawable.bg_inactive);
		active = getResources().getDrawable(R.drawable.bg_press);
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
		back();
		int first = settings.getInt("first", 0);

		if (first == 0) { // This dialog alerts the user to some navigation
							// techniques
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						break;

					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(CONTEXT); // dialog
			builder.setTitle("Browser Tips");
			builder.setMessage(
					"\nLong-press back button to exit browser"
							+ "\n\nSet your homepage in settings to about:blank to set a blank page as your default"
							+ "\n\nSet the homepage to about:home to set bookmarks as your homepage"
							+ "\n\nLong-press a link to open in a new tab"
							+ "\n\nCheck out the settings for more stuff!")
					.setPositiveButton("Ok", dialogClickListener).show();
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
			finish();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	protected void onNewIntent(Intent intent) {

		String url = intent.getDataString();
		if (url != null) {
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
			if (!urlToLoad[pageId][1].equals("Bookmarks")) {
				addBookmark();
			}
			return true;
		case R.id.settings:
			newSettings();
			return true;
		case R.id.allBookmarks:
			if (!urlToLoad[pageId][1].equals("Bookmarks")) {
				goBookmarks(main[pageId]);
			}
			return true;
		case R.id.share:
			share();
			return true;
		case R.id.incognito:
			startActivity(new Intent(FinalVars.INCOGNITO_INTENT));
			// newTab(number, homepage, true, true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
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
		// remember.setPriority(10);
		remember.start();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (API >= 11) {
			main[pageId].onResume();
		}
		main[0].resumeTimers();

	}

	void openBookmarks(CustomWebView view) {
		String bookmarkHtml = BookmarkPage.Heading;
		for (int n = 0; n < MAX_BOOKMARKS; n++) {
			if (bUrl[n] != null) {
				bookmarkHtml += (BookmarkPage.Part1 + bUrl[n]
						+ BookmarkPage.Part2 + bUrl[n] + BookmarkPage.Part3
						+ bTitle[n] + BookmarkPage.Part4);
			}
		}
		bookmarkHtml += BookmarkPage.End;
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
								if (!urlToLoad[pageId][1].equals("Bookmarks")) {
									addBookmark();
								}
								return true;
							case R.id.settings:
								newSettings();
								return true;
							case R.id.allBookmarks:
								if (!urlToLoad[pageId][1].equals("Bookmarks")) {
									goBookmarks(main[pageId]);
								}
								return true;
							case R.id.share:
								share();
								return true;
							case R.id.incognito:
								startActivity(new Intent(
										FinalVars.INCOGNITO_INTENT));
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

	void testForSearch() {
		String fixedQuery = query.trim();
		main[pageId].stopLoading();
		boolean period = fixedQuery.contains(".");

		if (fixedQuery.contains("about:home")
				|| fixedQuery.contains("about:bookmarks")) {
			goBookmarks(main[pageId]);
		} else if (fixedQuery.contains("about:history")) {
			generateHistory(main[pageId]);
		} else if (fixedQuery.contains(" ") || !period) {
			fixedQuery.replaceAll(" ", "+");
			main[pageId].loadUrl(SEARCH + fixedQuery);
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

	void trimCache(Context context) {
		try {
			File dir = context.getCacheDir();

			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception ignored) {

		}
	}

	void updateHistory(final String url, final String pageTitle) {
		update = new Runnable() {
			@Override
			public void run() {
				if (!noStockBrowser) {
					try {
						Browser.updateVisitedHistory(getContentResolver(), url,
								false);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					sb = new StringBuilder("url" + " = ");
					DatabaseUtils.appendEscapedSQLString(sb, url);
					s = historyHandler.getReadableDatabase();
					Cursor cursor = s.query("history", new String[] { "id",
							"url", "title" }, sb.toString(), null, null, null,
							null);
					if (!cursor.moveToFirst()) {
						historyHandler.addHistoryItem(new HistoryItem(url,
								pageTitle));
					}
					cursor.close();
					s.close();
				} catch (IllegalStateException e) {
					Log.e("Barebones", "ERRRRROOORRRR 1");
				} catch (NullPointerException e) {
					Log.e("Barebones", "ERRRRROOORRRR 2");
				} catch (SQLiteException e) {
					Log.e("Barebones", "SQLiteException");
				}
			}
		};
		if (!url.contains("file:///" + getBaseContext().getFilesDir()
				+ "/bookmarks.html")
				&& !url.contains("file:///" + getBaseContext().getFilesDir()
						+ "/history.html")) {

			new Thread(update).start();
		}
	}
}