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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AdvancedSettingsActivity extends Activity {

	// mPreferences variables
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor mEditPrefs;
	private CheckBox cbAllowPopups, cbAllowCookies, cbAllowIncognitoCookies, cbRestoreTabs;
	private Context mContext;
	private TextView mRenderText;
	private Activity mActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.advanced_settings);

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

		mContext = this;
		mActivity = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout rAllowPopups, rAllowCookies, rAllowIncognitoCookies, rRestoreTabs;

		rAllowPopups = (RelativeLayout) findViewById(R.id.rAllowPopups);
		rAllowCookies = (RelativeLayout) findViewById(R.id.rAllowCookies);
		rAllowIncognitoCookies = (RelativeLayout) findViewById(R.id.rAllowIncognitoCookies);
		rRestoreTabs = (RelativeLayout) findViewById(R.id.rRestoreTabs);

		cbAllowPopups = (CheckBox) findViewById(R.id.cbAllowPopups);
		cbAllowCookies = (CheckBox) findViewById(R.id.cbAllowCookies);
		cbAllowIncognitoCookies = (CheckBox) findViewById(R.id.cbAllowIncognitoCookies);
		cbRestoreTabs = (CheckBox) findViewById(R.id.cbRestoreTabs);

		cbAllowPopups.setChecked(mPreferences.getBoolean(PreferenceConstants.POPUPS, true));
		cbAllowCookies.setChecked(mPreferences.getBoolean(PreferenceConstants.COOKIES, true));
		cbAllowIncognitoCookies.setChecked(mPreferences.getBoolean(
				PreferenceConstants.INCOGNITO_COOKIES, false));
		cbRestoreTabs.setChecked(mPreferences.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true));

		mRenderText = (TextView) findViewById(R.id.renderText);

		switch (mPreferences.getInt(PreferenceConstants.RENDERING_MODE, 0)) {
			case 0:
				mRenderText.setText(mContext.getString(R.string.name_normal));
				break;
			case 1:
				mRenderText.setText(mContext.getString(R.string.name_inverted));
				break;
			case 2:
				mRenderText.setText(mContext.getString(R.string.name_grayscale));
				break;
			case 3:
				mRenderText.setText(mContext.getString(R.string.name_inverted_grayscale));
				break;
		}

		rAllowPopups(rAllowPopups);
		rAllowCookies(rAllowCookies);
		rAllowIncognitoCookies(rAllowIncognitoCookies);
		rRestoreTabs(rRestoreTabs);
		cbAllowPopups(cbAllowPopups);
		cbAllowCookies(cbAllowCookies);
		cbAllowIncognitoCookies(cbAllowIncognitoCookies);
		cbRestoreTabs(cbRestoreTabs);
		renderPicker();
	}

	private void cbAllowPopups(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.POPUPS, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbAllowCookies(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.COOKIES, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbAllowIncognitoCookies(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.INCOGNITO_COOKIES, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbRestoreTabs(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.RESTORE_LOST_TABS, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void rAllowPopups(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbAllowPopups.setChecked(!cbAllowPopups.isChecked());
			}

		});
	}

	private void rAllowCookies(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbAllowCookies.setChecked(!cbAllowCookies.isChecked());
			}

		});
	}

	private void rAllowIncognitoCookies(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbAllowIncognitoCookies.setChecked(!cbAllowIncognitoCookies.isChecked());
			}

		});

	}

	private void rRestoreTabs(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cbRestoreTabs.setChecked(!cbRestoreTabs.isChecked());
			}

		});
	}

	public void renderPicker() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.layoutRendering);
		layout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
				picker.setTitle(getResources().getString(R.string.rendering_mode));
				CharSequence[] chars = { mContext.getString(R.string.name_normal),
						mContext.getString(R.string.name_inverted),
						mContext.getString(R.string.name_grayscale),
						mContext.getString(R.string.name_inverted_grayscale) };

				int n = mPreferences.getInt(PreferenceConstants.RENDERING_MODE, 0);

				picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mEditPrefs.putInt(PreferenceConstants.RENDERING_MODE, which).apply();
						switch (which) {
							case 0:
								mRenderText.setText(mContext.getString(R.string.name_normal));
								break;
							case 1:
								mRenderText.setText(mContext.getString(R.string.name_inverted));
								break;
							case 2:
								mRenderText.setText(mContext.getString(R.string.name_grayscale));
								break;
							case 3:
								mRenderText.setText(mContext
										.getString(R.string.name_inverted_grayscale));
								break;
						}
					}
				});
				picker.setNeutralButton(getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

							}
						});
				picker.show();
			}

		});
	}

	public void importFromStockBrowser() {
		BookmarkManager manager = new BookmarkManager(this);
		manager.importBookmarksFromBrowser();
	}
}
