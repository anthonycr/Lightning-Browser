/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.R;

public class AdvancedSettingsActivity extends ThemableSettingsActivity {

	private CheckBox cbAllowPopups, cbAllowCookies, cbAllowIncognitoCookies, cbRestoreTabs;
	private Context mContext;
	private TextView mRenderText;
	private TextView mUrlText;
	private CharSequence[] mUrlOptions;
	private PreferenceManager mPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.advanced_settings);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mContext = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		mPreferences = PreferenceManager.getInstance();

		RelativeLayout rAllowPopups, rAllowCookies, rAllowIncognitoCookies, rRestoreTabs;
		LinearLayout lRenderPicker, lUrlContent;

		rAllowPopups = (RelativeLayout) findViewById(R.id.rAllowPopups);
		rAllowCookies = (RelativeLayout) findViewById(R.id.rAllowCookies);
		rAllowIncognitoCookies = (RelativeLayout) findViewById(R.id.rAllowIncognitoCookies);
		rRestoreTabs = (RelativeLayout) findViewById(R.id.rRestoreTabs);
		lRenderPicker = (LinearLayout) findViewById(R.id.layoutRendering);
		lUrlContent = (LinearLayout) findViewById(R.id.rUrlBarContents);

		cbAllowPopups = (CheckBox) findViewById(R.id.cbAllowPopups);
		cbAllowCookies = (CheckBox) findViewById(R.id.cbAllowCookies);
		cbAllowIncognitoCookies = (CheckBox) findViewById(R.id.cbAllowIncognitoCookies);
		cbRestoreTabs = (CheckBox) findViewById(R.id.cbRestoreTabs);

		cbAllowPopups.setChecked(mPreferences.getPopupsEnabled());
		cbAllowCookies.setChecked(mPreferences.getCookiesEnabled());
		cbAllowIncognitoCookies.setChecked(mPreferences.getIncognitoCookiesEnabled());
		cbRestoreTabs.setChecked(mPreferences.getRestoreLostTabsEnabled());

		mRenderText = (TextView) findViewById(R.id.renderText);
		mUrlText = (TextView) findViewById(R.id.urlText);

		switch (mPreferences.getRenderingMode()) {
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

		mUrlOptions = this.getResources().getStringArray(R.array.url_content_array);
		int option = mPreferences.getUrlBoxContentChoice();
		mUrlText.setText(mUrlOptions[option]);

		LayoutClickListener listener = new LayoutClickListener();
		CheckListener cListener = new CheckListener();

		rAllowPopups.setOnClickListener(listener);
		rAllowCookies.setOnClickListener(listener);
		rAllowIncognitoCookies.setOnClickListener(listener);
		rRestoreTabs.setOnClickListener(listener);
		lRenderPicker.setOnClickListener(listener);
		lUrlContent.setOnClickListener(listener);

		cbAllowPopups.setOnCheckedChangeListener(cListener);
		cbAllowCookies.setOnCheckedChangeListener(cListener);
		cbAllowIncognitoCookies.setOnCheckedChangeListener(cListener);
		cbRestoreTabs.setOnCheckedChangeListener(cListener);

	}

	private class LayoutClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.rAllowPopups:
					cbAllowPopups.setChecked(!cbAllowPopups.isChecked());
					break;
				case R.id.rAllowIncognitoCookies:
					cbAllowIncognitoCookies.setChecked(!cbAllowIncognitoCookies.isChecked());
					break;
				case R.id.rAllowCookies:
					cbAllowCookies.setChecked(!cbAllowCookies.isChecked());
					break;
				case R.id.rRestoreTabs:
					cbRestoreTabs.setChecked(!cbRestoreTabs.isChecked());
					break;
				case R.id.layoutRendering:
					renderPicker();
					break;
				case R.id.rUrlBarContents:
					urlBoxPicker();
					break;
			}
		}

	}

	private class CheckListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			switch (buttonView.getId()) {
				case R.id.cbAllowPopups:
					mPreferences.setPopupsEnabled(isChecked);
					break;
				case R.id.cbAllowCookies:
					mPreferences.setCookiesEnabled(isChecked);
					break;
				case R.id.cbAllowIncognitoCookies:
					mPreferences.setIncognitoCookiesEnabled(isChecked);
					break;
				case R.id.cbRestoreTabs:
					mPreferences.setRestoreLostTabsEnabled(isChecked);
					break;
			}
		}

	}

	private void renderPicker() {

		AlertDialog.Builder picker = new AlertDialog.Builder(mContext);
		picker.setTitle(getResources().getString(R.string.rendering_mode));
		CharSequence[] chars = { mContext.getString(R.string.name_normal),
				mContext.getString(R.string.name_inverted),
				mContext.getString(R.string.name_grayscale),
				mContext.getString(R.string.name_inverted_grayscale) };

		int n = mPreferences.getRenderingMode();

		picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mPreferences.setRenderingMode(which);
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
						mRenderText.setText(mContext.getString(R.string.name_inverted_grayscale));
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

	private void urlBoxPicker() {

		AlertDialog.Builder picker = new AlertDialog.Builder(mContext);
		picker.setTitle(getResources().getString(R.string.url_contents));

		int n = mPreferences.getUrlBoxContentChoice();

		picker.setSingleChoiceItems(mUrlOptions, n, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mPreferences.setUrlBoxContentChoice(which);
				if (which < mUrlOptions.length) {
					mUrlText.setText(mUrlOptions[which]);
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

}
