/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.*;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.R;
import acr.browser.lightning.utils.Utils;

public class PrivacySettingsActivity extends ThemableSettingsActivity {

	// mPreferences variables
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private PreferenceManager mPreferences;
	private CheckBox cbLocation, cbSavePasswords, cbClearCacheExit, cbClearHistoryExit,
			cbClearCookiesExit, cbThirdParty;
	private Context mContext;
	private boolean mSystemBrowser;
	private Handler messageHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.privacy_settings);

		// set up ActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mPreferences = PreferenceManager.getInstance();

		mSystemBrowser = mPreferences.getSystemBrowserPresent();
		mContext = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout rLocation, rSavePasswords, rClearCacheExit, rClearHistoryExit, rClearCookiesExit, rClearCache, rClearHistory, rClearCookies, rThirdParty;

		rLocation = (RelativeLayout) findViewById(R.id.rLocation);
		rSavePasswords = (RelativeLayout) findViewById(R.id.rSavePasswords);
		rClearCacheExit = (RelativeLayout) findViewById(R.id.rClearCacheExit);
		rClearHistoryExit = (RelativeLayout) findViewById(R.id.rClearHistoryExit);
		rClearCookiesExit = (RelativeLayout) findViewById(R.id.rClearCookiesExit);
		rClearCache = (RelativeLayout) findViewById(R.id.rClearCache);
		rClearHistory = (RelativeLayout) findViewById(R.id.rClearHistory);
		rClearCookies = (RelativeLayout) findViewById(R.id.rClearCookies);
		rThirdParty = (RelativeLayout) findViewById(R.id.rThirdParty);

		cbLocation = (CheckBox) findViewById(R.id.cbLocation);
		cbSavePasswords = (CheckBox) findViewById(R.id.cbSavePasswords);
		cbClearCacheExit = (CheckBox) findViewById(R.id.cbClearCacheExit);
		cbClearHistoryExit = (CheckBox) findViewById(R.id.cbClearHistoryExit);
		cbClearCookiesExit = (CheckBox) findViewById(R.id.cbClearCookiesExit);
		cbThirdParty = (CheckBox) findViewById(R.id.cbThirdParty);

		cbLocation.setChecked(mPreferences.getLocationEnabled());
		cbSavePasswords.setChecked(mPreferences.getSavePasswordsEnabled());
		cbClearCacheExit.setChecked(mPreferences.getClearCacheExit());
		cbClearHistoryExit.setChecked(mPreferences.getClearHistoryExitEnabled());
		cbClearCookiesExit.setChecked(mPreferences.getClearCookiesExitEnabled());
		cbThirdParty.setChecked(mPreferences.getBlockThirdPartyCookiesEnabled());

		cbThirdParty.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

		rLocation(rLocation);
		rSavePasswords(rSavePasswords);
		rClearCacheExit(rClearCacheExit);
		rClearHistoryExit(rClearHistoryExit);
		rClearCookiesExit(rClearCookiesExit);
		rClearCache(rClearCache);
		rClearHistory(rClearHistory);
		rClearCookies(rClearCookies);
		rThirdParty(rThirdParty);
		cbLocation(cbLocation);
		cbSavePasswords(cbSavePasswords);
		cbClearCacheExit(cbClearCacheExit);
		cbClearHistoryExit(cbClearHistoryExit);
		cbClearCookiesExit(cbClearCookiesExit);
		cbThirdParty(cbThirdParty);

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
				mPreferences.setSyncHistoryEnabled(isChecked);
			}

		});

		if (!mSystemBrowser) {
			cbSyncHistory.setChecked(false);
			cbSyncHistory.setEnabled(false);
			syncHistory.setText(getResources().getString(R.string.stock_browser_unavailable));
		} else {
			cbSyncHistory.setEnabled(true);
			cbSyncHistory.setChecked(mPreferences.getSyncHistoryEnabled());
			syncHistory.setText(getResources().getString(R.string.stock_browser_available));
		}

		messageHandler = new MessageHandler(mContext);
	}

	private static class MessageHandler extends Handler {

		final Context mHandlerContext;

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
				mPreferences.setLocationEnabled(isChecked);
			}

		});
	}

	private void cbSavePasswords(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setSavePasswordsEnabled(isChecked);
			}

		});
	}

	private void cbClearCacheExit(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setClearCacheExit(isChecked);
			}

		});
	}

	private void cbClearHistoryExit(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setClearHistoryExitEnabled(isChecked);
			}

		});
	}

	private void cbThirdParty(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setBlockThirdPartyCookiesEnabled(isChecked);
			}

		});
	}

	private void cbClearCookiesExit(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setClearCookiesExitEnabled(isChecked);
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

	private void rThirdParty(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					cbThirdParty.setChecked(!cbThirdParty.isChecked());
				} else {
					Utils.showToast(mContext, mContext.getString(R.string.available_lollipop));
				}
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
		deleteDatabase(HistoryDatabase.DATABASE_NAME);
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
		Utils.trimCache(this);
		messageHandler.sendEmptyMessage(1);
	}

	@SuppressWarnings("deprecation")
	public void clearCookies() {
		// TODO Break out web storage deletion into its own option/action
		// TODO clear web storage for all sites that are visited in Incognito mode
		WebStorage storage = WebStorage.getInstance();
		storage.deleteAllData();
		CookieManager c = CookieManager.getInstance();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			c.removeAllCookies(null);
		} else {
			CookieSyncManager.createInstance(this);
			c.removeAllCookie();
		}
		messageHandler.sendEmptyMessage(2);
	}
}
