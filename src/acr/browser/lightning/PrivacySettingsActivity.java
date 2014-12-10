/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PrivacySettingsActivity extends Activity {

	// mPreferences variables
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor mEditPrefs;
	private CheckBox cbLocation, cbSavePasswords, cbClearCacheExit, cbClearHistoryExit, cbClearCookiesExit;
	private Context mContext;
	private boolean mSystemBrowser;
	private Handler messageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.privacy_settings);

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		// TODO WARNING: SharedPreferences.edit() without a corresponding
		// commit() or apply() call
		mEditPrefs = mPreferences.edit();

		mSystemBrowser = mPreferences.getBoolean(PreferenceConstants.SYSTEM_BROWSER_PRESENT, false);
		mContext = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout rLocation, rSavePasswords, rClearCacheExit, rClearHistoryExit, rClearCookiesExit, rClearCache, rClearHistory, rClearCookies;

		rLocation = (RelativeLayout) findViewById(R.id.rLocation);
		rSavePasswords = (RelativeLayout) findViewById(R.id.rSavePasswords);
		rClearCacheExit = (RelativeLayout) findViewById(R.id.rClearCacheExit);
		rClearHistoryExit = (RelativeLayout) findViewById(R.id.rClearHistoryExit);
		rClearCookiesExit = (RelativeLayout) findViewById(R.id.rClearCookiesExit);
		rClearCache = (RelativeLayout) findViewById(R.id.rClearCache);
		rClearHistory = (RelativeLayout) findViewById(R.id.rClearHistory);
		rClearCookies = (RelativeLayout) findViewById(R.id.rClearCookies);

		cbLocation = (CheckBox) findViewById(R.id.cbLocation);
		cbSavePasswords = (CheckBox) findViewById(R.id.cbSavePasswords);
		cbClearCacheExit = (CheckBox) findViewById(R.id.cbClearCacheExit);
		cbClearHistoryExit = (CheckBox) findViewById(R.id.cbClearHistoryExit);
		cbClearCookiesExit = (CheckBox) findViewById(R.id.cbClearCookiesExit);

		cbLocation.setChecked(mPreferences.getBoolean(PreferenceConstants.LOCATION, false));
		cbSavePasswords.setChecked(mPreferences.getBoolean(PreferenceConstants.SAVE_PASSWORDS, true));
		cbClearCacheExit.setChecked(mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT, false));
		cbClearHistoryExit.setChecked(mPreferences.getBoolean(
				PreferenceConstants.CLEAR_HISTORY_EXIT, false));
		cbClearCookiesExit.setChecked(mPreferences.getBoolean(
				PreferenceConstants.CLEAR_COOKIES_EXIT, false));

		rLocation(rLocation);
		rSavePasswords(rSavePasswords);
		rClearCacheExit(rClearCacheExit);
		rClearHistoryExit(rClearHistoryExit);
		rClearCookiesExit(rClearCookiesExit);
		rClearCache(rClearCache);
		rClearHistory(rClearHistory);
		rClearCookies(rClearCookies);
		cbLocation(cbLocation);
		cbSavePasswords(cbSavePasswords);
		cbClearCacheExit(cbClearCacheExit);
		cbClearHistoryExit(cbClearHistoryExit);
		cbClearCookiesExit(cbClearCookiesExit);

		TextView syncHistory = (TextView) findViewById(R.id.isBrowserAvailable);

		RelativeLayout layoutSyncHistory = (RelativeLayout) findViewById(R.id.rBrowserHistory);
		final CheckBox cbSyncHistory = (CheckBox) findViewById(R.id.cbBrowserHistory);
		layoutSyncHistory.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cbSyncHistory.setChecked(!cbSyncHistory.isChecked());
			}

		});
		cbSyncHistory.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.SYNC_HISTORY, isChecked).apply();
			}

		});

		if (!mSystemBrowser) {
			cbSyncHistory.setChecked(false);
			cbSyncHistory.setEnabled(false);
			syncHistory.setText(getResources().getString(R.string.stock_browser_unavailable));
		} else {
			cbSyncHistory.setEnabled(true);
			cbSyncHistory.setChecked(mPreferences
					.getBoolean(PreferenceConstants.SYNC_HISTORY, true));
			syncHistory.setText(getResources().getString(R.string.stock_browser_available));
		}

		messageHandler = new MessageHandler(mContext);
	}

	private static class MessageHandler extends Handler {

		Context mHandlerContext;

		public MessageHandler(Context context) {
			this.mHandlerContext = context;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 1:
					Utils.showToast(mHandlerContext,
							mHandlerContext.getResources()
									.getString(R.string.message_clear_history));
					break;
				case 2:
					Utils.showToast(
							mHandlerContext,
							mHandlerContext.getResources().getString(
									R.string.message_cookies_cleared));
					break;
			}
			super.handleMessage(msg);
		}
	}

	private void cbLocation(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.LOCATION, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbSavePasswords(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.SAVE_PASSWORDS, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbClearCacheExit(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.CLEAR_CACHE_EXIT, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbClearHistoryExit(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbClearCookiesExit(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void rLocation(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbLocation.setChecked(!cbLocation.isChecked());
			}

		});
	}

	private void rSavePasswords(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbSavePasswords.setChecked(!cbSavePasswords.isChecked());
			}

		});
	}

	private void rClearCacheExit(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbClearCacheExit.setChecked(!cbClearCacheExit.isChecked());
			}

		});
	}

	private void rClearHistoryExit(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbClearHistoryExit.setChecked(!cbClearHistoryExit.isChecked());
			}

		});
	}

	private void rClearCookiesExit(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbClearCookiesExit.setChecked(!cbClearCookiesExit.isChecked());
			}

		});
	}

	private void rClearHistory(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(PrivacySettingsActivity.this); // dialog
				builder.setTitle(getResources().getString(R.string.title_clear_history));
				builder.setMessage(getResources().getString(R.string.dialog_history))
						.setPositiveButton(getResources().getString(R.string.action_yes),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										Thread clear = new Thread(new Runnable() {

											@Override
											public void run() {
												clearHistory();
											}

										});
										clear.start();
									}

								})
						.setNegativeButton(getResources().getString(R.string.action_no),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										// TODO Auto-generated method stub

									}

								}).show();
			}

		});
	}

	private void rClearCookies(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(PrivacySettingsActivity.this); // dialog
				builder.setTitle(getResources().getString(R.string.title_clear_cookies));
				builder.setMessage(getResources().getString(R.string.dialog_cookies))
						.setPositiveButton(getResources().getString(R.string.action_yes),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										Thread clear = new Thread(new Runnable() {

											@Override
											public void run() {
												clearCookies();
											}

										});
										clear.start();
									}

								})
						.setNegativeButton(getResources().getString(R.string.action_no),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int arg1) {

									}

								}).show();
			}

		});
	}

	private void rClearCache(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				clearCache();
			}

		});

	}

	public void clearCache() {
		WebView webView = new WebView(this);
		webView.clearCache(true);
		webView.destroy();
		Utils.showToast(mContext, getResources().getString(R.string.message_cache_cleared));
	}

	@SuppressWarnings("deprecation")
	public void clearHistory() {
		deleteDatabase(HistoryDatabaseHandler.DATABASE_NAME);
		WebViewDatabase m = WebViewDatabase.getInstance(this);
		m.clearFormData();
		m.clearHttpAuthUsernamePassword();
		if (API < 18) {
			m.clearUsernamePassword();
			WebIconDatabase.getInstance().removeAllIcons();
		}
		if (mSystemBrowser) {
			try {
				Browser.clearHistory(getContentResolver());
			} catch (Exception ignored) {
			}
		}
		SettingsController.setClearHistory(true);
		Utils.trimCache(this);
		messageHandler.sendEmptyMessage(1);
	}

	public void clearCookies() {
		CookieManager c = CookieManager.getInstance();
		CookieSyncManager.createInstance(this);
		c.removeAllCookie();
		messageHandler.sendEmptyMessage(2);
	}
}
