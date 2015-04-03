/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;

public class DisplaySettingsActivity extends ThemableSettingsActivity {

	// mPreferences variables
	private SharedPreferences mPreferences;
	private CheckBox cbHideStatusBar, cbFullScreen, cbWideViewPort, cbOverView, cbTextReflow,
			cbDarkTheme;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_settings);

		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);

		// set up ActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout rHideStatusBar, rFullScreen, rWideViewPort, rOverView, rTextReflow, rTextSize, rDarkTheme;

		rHideStatusBar = (RelativeLayout) findViewById(R.id.rHideStatusBar);
		rFullScreen = (RelativeLayout) findViewById(R.id.rFullScreen);
		rWideViewPort = (RelativeLayout) findViewById(R.id.rWideViewPort);
		rOverView = (RelativeLayout) findViewById(R.id.rOverView);
		rTextReflow = (RelativeLayout) findViewById(R.id.rTextReflow);
		rTextSize = (RelativeLayout) findViewById(R.id.rTextSize);
		rDarkTheme = (RelativeLayout) findViewById(R.id.rDarkTheme);

		cbHideStatusBar = (CheckBox) findViewById(R.id.cbHideStatusBar);
		cbFullScreen = (CheckBox) findViewById(R.id.cbFullScreen);
		cbWideViewPort = (CheckBox) findViewById(R.id.cbWideViewPort);
		cbOverView = (CheckBox) findViewById(R.id.cbOverView);
		cbTextReflow = (CheckBox) findViewById(R.id.cbTextReflow);
		cbDarkTheme = (CheckBox) findViewById(R.id.cbDarkTheme);

		cbHideStatusBar.setChecked(mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR,
				false));
		cbFullScreen.setChecked(mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN, false));
		cbWideViewPort.setChecked(mPreferences.getBoolean(PreferenceConstants.USE_WIDE_VIEWPORT,
				true));
		cbOverView.setChecked(mPreferences.getBoolean(PreferenceConstants.OVERVIEW_MODE, true));
		cbTextReflow.setChecked(mPreferences.getBoolean(PreferenceConstants.TEXT_REFLOW, false));
		cbDarkTheme.setChecked(mPreferences.getBoolean(PreferenceConstants.DARK_THEME, false));

		rHideStatusBar(rHideStatusBar);
		rFullScreen(rFullScreen);
		rWideViewPort(rWideViewPort);
		rOverView(rOverView);
		rTextReflow(rTextReflow);
		rTextSize(rTextSize);
		rDarkTheme(rDarkTheme);
		cbHideStatusBar(cbHideStatusBar);
		cbFullScreen(cbFullScreen);
		cbWideViewPort(cbWideViewPort);
		cbOverView(cbOverView);
		cbTextReflow(cbTextReflow);
		cbDarkTheme(cbDarkTheme);
	}

	private void cbHideStatusBar(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.edit().putBoolean(PreferenceConstants.HIDE_STATUS_BAR, isChecked)
						.apply();
			}

		});
	}

	private void cbFullScreen(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.edit().putBoolean(PreferenceConstants.FULL_SCREEN, isChecked).apply();
			}

		});
	}

	private void cbDarkTheme(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.edit().putBoolean(PreferenceConstants.DARK_THEME, isChecked).apply();
				restart();
			}

		});
	}

	private void cbWideViewPort(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.edit().putBoolean(PreferenceConstants.USE_WIDE_VIEWPORT, isChecked)
						.apply();
			}

		});
	}

	private void cbOverView(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.edit().putBoolean(PreferenceConstants.OVERVIEW_MODE, isChecked)
						.apply();
			}

		});
	}

	private void cbTextReflow(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.edit().putBoolean(PreferenceConstants.TEXT_REFLOW, isChecked).apply();
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

	private void rDarkTheme(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cbDarkTheme.setChecked(!cbDarkTheme.isChecked());
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
								mPreferences.edit()
										.putInt(PreferenceConstants.TEXT_SIZE, which + 1).apply();

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
