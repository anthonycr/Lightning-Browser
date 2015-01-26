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
import android.widget.RelativeLayout;

public class DisplaySettingsActivity extends Activity {

	// mPreferences variables
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor mEditPrefs;
	private CheckBox cbHideStatusBar, cbFullScreen, cbWideViewPort, cbOverView, cbTextReflow;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_settings);

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
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout rHideStatusBar, rFullScreen, rWideViewPort, rOverView, rTextReflow, rTextSize;

		rHideStatusBar = (RelativeLayout) findViewById(R.id.rHideStatusBar);
		rFullScreen = (RelativeLayout) findViewById(R.id.rFullScreen);
		rWideViewPort = (RelativeLayout) findViewById(R.id.rWideViewPort);
		rOverView = (RelativeLayout) findViewById(R.id.rOverView);
		rTextReflow = (RelativeLayout) findViewById(R.id.rTextReflow);
		rTextSize = (RelativeLayout) findViewById(R.id.rTextSize);

		cbHideStatusBar = (CheckBox) findViewById(R.id.cbHideStatusBar);
		cbFullScreen = (CheckBox) findViewById(R.id.cbFullScreen);
		cbWideViewPort = (CheckBox) findViewById(R.id.cbWideViewPort);
		cbOverView = (CheckBox) findViewById(R.id.cbOverView);
		cbTextReflow = (CheckBox) findViewById(R.id.cbTextReflow);

		cbHideStatusBar.setChecked(mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false));
		cbFullScreen.setChecked(mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN, false));
		cbWideViewPort.setChecked(mPreferences.getBoolean(PreferenceConstants.USE_WIDE_VIEWPORT, true));
		cbOverView.setChecked(mPreferences.getBoolean(PreferenceConstants.OVERVIEW_MODE, true));
		cbTextReflow.setChecked(mPreferences.getBoolean(PreferenceConstants.TEXT_REFLOW, false));

		rHideStatusBar(rHideStatusBar);
		rFullScreen(rFullScreen);
		rWideViewPort(rWideViewPort);
		rOverView(rOverView);
		rTextReflow(rTextReflow);
		rTextSize(rTextSize);
		cbHideStatusBar(cbHideStatusBar);
		cbFullScreen(cbFullScreen);
		cbWideViewPort(cbWideViewPort);
		cbOverView(cbOverView);
		cbTextReflow(cbTextReflow);
	}

	private void cbHideStatusBar(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.HIDE_STATUS_BAR, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbFullScreen(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.FULL_SCREEN, isChecked);
				mEditPrefs.commit();
			}

		});
	}


	private void cbWideViewPort(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.USE_WIDE_VIEWPORT, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbOverView(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.OVERVIEW_MODE, isChecked);
				mEditPrefs.commit();
			}

		});
	}

	private void cbTextReflow(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.TEXT_REFLOW, isChecked);
				mEditPrefs.commit();
			}
		});
	}

	private void rHideStatusBar(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cbHideStatusBar.setChecked(!cbHideStatusBar.isChecked());
			}

		});
	}

	private void rFullScreen(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cbFullScreen.setChecked(!cbFullScreen.isChecked());
			}

		});
	}

	private void rWideViewPort(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbWideViewPort.setChecked(!cbWideViewPort.isChecked());
			}

		});

	}

	private void rOverView(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cbOverView.setChecked(!cbOverView.isChecked());
			}

		});
	}

	private void rTextReflow(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cbTextReflow.setChecked(!cbTextReflow.isChecked());
			}

		});
	}

	private void rTextSize(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(DisplaySettingsActivity.this);
				picker.setTitle(getResources().getString(R.string.title_text_size));

				int n = mPreferences.getInt(PreferenceConstants.TEXT_SIZE, 3);

				picker.setSingleChoiceItems(R.array.text_size, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								mEditPrefs.putInt(PreferenceConstants.TEXT_SIZE, which + 1);
								mEditPrefs.commit();

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
}
