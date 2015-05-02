/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;

import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.R;

public class DisplaySettingsActivity extends ThemableSettingsActivity {

	// mPreferences variables
	private PreferenceManager mPreferences;
	private CheckBox cbHideStatusBar, cbFullScreen, cbWideViewPort, cbOverView, cbTextReflow,
			cbDarkTheme;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_settings);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mPreferences = PreferenceManager.getInstance();
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		RelativeLayout rHideStatusBar, rFullScreen, rWideViewPort, rOverView, rTextReflow, rTextSize, rDarkTheme;
		LayoutClickListener clickListener = new LayoutClickListener();
		CheckBoxToggleListener toggleListener = new CheckBoxToggleListener();

		rHideStatusBar = (RelativeLayout) findViewById(R.id.rHideStatusBar);
		rFullScreen = (RelativeLayout) findViewById(R.id.rFullScreen);
		rWideViewPort = (RelativeLayout) findViewById(R.id.rWideViewPort);
		rOverView = (RelativeLayout) findViewById(R.id.rOverView);
		rTextReflow = (RelativeLayout) findViewById(R.id.rTextReflow);
		rTextSize = (RelativeLayout) findViewById(R.id.rTextSize);
		rDarkTheme = (RelativeLayout) findViewById(R.id.rDarkTheme);
		
		rHideStatusBar.setOnClickListener(clickListener);
		rFullScreen.setOnClickListener(clickListener);
		rWideViewPort.setOnClickListener(clickListener);
		rOverView.setOnClickListener(clickListener);
		rTextReflow.setOnClickListener(clickListener);
		rTextSize.setOnClickListener(clickListener);
		rDarkTheme.setOnClickListener(clickListener);

		cbHideStatusBar = (CheckBox) findViewById(R.id.cbHideStatusBar);
		cbFullScreen = (CheckBox) findViewById(R.id.cbFullScreen);
		cbWideViewPort = (CheckBox) findViewById(R.id.cbWideViewPort);
		cbOverView = (CheckBox) findViewById(R.id.cbOverView);
		cbTextReflow = (CheckBox) findViewById(R.id.cbTextReflow);
		cbDarkTheme = (CheckBox) findViewById(R.id.cbDarkTheme);

		cbHideStatusBar.setChecked(mPreferences.getHideStatusBarEnabled());
		cbFullScreen.setChecked(mPreferences.getFullScreenEnabled());
		cbWideViewPort.setChecked(mPreferences.getUseWideViewportEnabled());
		cbOverView.setChecked(mPreferences.getOverviewModeEnabled());
		cbTextReflow.setChecked(mPreferences.getTextReflowEnabled());
		cbDarkTheme.setChecked(mPreferences.getUseDarkTheme());

		cbHideStatusBar.setOnCheckedChangeListener(toggleListener);
		cbFullScreen.setOnCheckedChangeListener(toggleListener);
		cbWideViewPort.setOnCheckedChangeListener(toggleListener);
		cbOverView.setOnCheckedChangeListener(toggleListener);
		cbTextReflow.setOnCheckedChangeListener(toggleListener);
		cbDarkTheme.setOnCheckedChangeListener(toggleListener);
	}

	private class LayoutClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.rHideStatusBar:
					cbHideStatusBar.setChecked(!cbHideStatusBar.isChecked());
					break;
				case R.id.rFullScreen:
					cbFullScreen.setChecked(!cbFullScreen.isChecked());
					break;
				case R.id.rWideViewPort:
					cbWideViewPort.setChecked(!cbWideViewPort.isChecked());
					break;
				case R.id.rOverView:
					cbOverView.setChecked(!cbOverView.isChecked());
					break;
				case R.id.rTextReflow:
					cbTextReflow.setChecked(!cbTextReflow.isChecked());
					break;
				case R.id.rTextSize:
					textSizePicker();
					break;
				case R.id.rDarkTheme:
					cbDarkTheme.setChecked(!cbDarkTheme.isChecked());
					break;
			}
		}

	}

	private class CheckBoxToggleListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			switch (buttonView.getId()) {
				case R.id.cbHideStatusBar:
					mPreferences.setHideStatusBarEnabled(isChecked);
					break;
				case R.id.cbFullScreen:
					mPreferences.setFullScreenEnabled(isChecked);
					break;
				case R.id.cbWideViewPort:
					mPreferences.setUseWideViewportEnabled(isChecked);
					break;
				case R.id.cbOverView:
					mPreferences.setOverviewModeEnabled(isChecked);
					break;
				case R.id.cbTextReflow:
					mPreferences.setTextReflowEnabled(isChecked);
					break;
				case R.id.cbDarkTheme:
					mPreferences.setUseDarkTheme(isChecked);
					restart();
					break;
			}
		}

	}

	private void textSizePicker() {
		AlertDialog.Builder picker = new AlertDialog.Builder(DisplaySettingsActivity.this);
		picker.setTitle(getResources().getString(R.string.title_text_size));

		int n = mPreferences.getTextSize();

		picker.setSingleChoiceItems(R.array.text_size, n - 1,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPreferences.setTextSize(which + 1);
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
}
