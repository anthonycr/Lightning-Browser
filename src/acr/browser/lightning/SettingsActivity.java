/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import info.guardianproject.onionkit.ui.OrbotHelper;

public class SettingsActivity extends ThemableSettingsActivity {

	private static int API = android.os.Build.VERSION.SDK_INT;
	private PreferenceManager mPreferences;
	private Context mContext;
	private Activity mActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		mContext = this;
		mActivity = this;
		init();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	@SuppressLint("NewApi")
	public void init() {
		// set up ActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// mPreferences storage
		mPreferences = PreferenceManager.getInstance();

		// initialize UI
		RelativeLayout layoutFlash = (RelativeLayout) findViewById(R.id.layoutFlash);
		RelativeLayout layoutBlockAds = (RelativeLayout) findViewById(R.id.layoutAdBlock);
		RelativeLayout layoutImages = (RelativeLayout) findViewById(R.id.layoutImages);
		RelativeLayout layoutEnableJS = (RelativeLayout) findViewById(R.id.layoutEnableJS);
		RelativeLayout layoutOrbot = (RelativeLayout) findViewById(R.id.layoutUseOrbot);
		RelativeLayout layoutColor = (RelativeLayout) findViewById(R.id.layoutColorMode);
		RelativeLayout layoutBookmarks = (RelativeLayout) findViewById(R.id.layoutBookmarks);

		layoutBookmarks.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, BookmarkActivity.class));
			}

		});

		if (API >= 19) {
			mPreferences.setFlashSupport(0);
		}
		int flashNum = mPreferences.getFlashSupport();
		boolean imagesBool = mPreferences.getBlockImagesEnabled();
		boolean enableJSBool = mPreferences.getJavaScriptEnabled();

		CheckBox flash = (CheckBox) findViewById(R.id.cbFlash);
		CheckBox adblock = (CheckBox) findViewById(R.id.cbAdblock);
		CheckBox images = (CheckBox) findViewById(R.id.cbImageBlock);
		CheckBox enablejs = (CheckBox) findViewById(R.id.cbJavascript);
		CheckBox orbot = (CheckBox) findViewById(R.id.cbOrbot);
		CheckBox color = (CheckBox) findViewById(R.id.cbColorMode);

		images.setChecked(imagesBool);
		enablejs.setChecked(enableJSBool);
		if (flashNum > 0) {
			flash.setChecked(true);
		} else {
			flash.setChecked(false);
		}
		adblock.setChecked(mPreferences.getAdBlockEnabled());
		orbot.setChecked(mPreferences.getUseProxy());
		color.setChecked(mPreferences.getColorModeEnabled());

		initCheckBox(flash, adblock, images, enablejs, orbot, color);
		clickListenerForCheckBoxes(layoutFlash, layoutBlockAds, layoutImages, layoutEnableJS,
				layoutOrbot, layoutColor, flash, adblock, images, enablejs, orbot, color);

		RelativeLayout general = (RelativeLayout) findViewById(R.id.layoutGeneral);
		RelativeLayout display = (RelativeLayout) findViewById(R.id.layoutDisplay);
		RelativeLayout privacy = (RelativeLayout) findViewById(R.id.layoutPrivacy);
		RelativeLayout advanced = (RelativeLayout) findViewById(R.id.layoutAdvanced);
		RelativeLayout about = (RelativeLayout) findViewById(R.id.layoutAbout);

		general(general);
		display(display);
		privacy(privacy);
		advanced(advanced);
		about(about);
	}

	public void clickListenerForCheckBoxes(RelativeLayout layoutFlash,
			RelativeLayout layoutBlockAds, RelativeLayout layoutImages,
			RelativeLayout layoutEnableJS, RelativeLayout layoutOrbot, RelativeLayout layoutColor,
			final CheckBox flash, final CheckBox adblock, final CheckBox images,
			final CheckBox enablejs, final CheckBox orbot, final CheckBox color) {
		layoutFlash.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (API < 19) {
					flash.setChecked(!flash.isChecked());
				} else {
					Utils.createInformativeDialog(mContext,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_dead));
				}
			}

		});
		layoutBlockAds.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				adblock.setChecked(!adblock.isChecked());
			}

		});
		layoutImages.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				images.setChecked(!images.isChecked());
			}

		});
		layoutEnableJS.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				enablejs.setChecked(!enablejs.isChecked());
			}

		});
		layoutOrbot.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (orbot.isEnabled()) {
					orbot.setChecked(!orbot.isChecked());
				} else {
					Utils.showToast(mContext, getResources().getString(R.string.install_orbot));
				}
			}

		});
		layoutColor.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				color.setChecked(!color.isChecked());
			}

		});
	}

	public void initCheckBox(CheckBox flash, CheckBox adblock, CheckBox images, CheckBox enablejs,
			CheckBox orbot, CheckBox color) {
		flash.setEnabled(API < 19);
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					getFlashChoice();
				} else {
					mPreferences.setFlashSupport(0);
				}

				boolean flashInstalled = false;
				try {
					PackageManager pm = getPackageManager();
					ApplicationInfo ai = pm.getApplicationInfo("com.adobe.flashplayer", 0);
					if (ai != null) {
						flashInstalled = true;
					}
				} catch (NameNotFoundException e) {
					flashInstalled = false;
				}
				if (!flashInstalled && isChecked) {
					Utils.createInformativeDialog(SettingsActivity.this,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_not_installed));
					buttonView.setChecked(false);
					mPreferences.setFlashSupport(0);

				} else if ((API >= 17) && isChecked) {
					Utils.createInformativeDialog(SettingsActivity.this,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_unsupported));
				}
			}

		});
		adblock.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setAdBlockEnabled(isChecked);
			}

		});
		images.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setBlockImagesEnabled(isChecked);

			}

		});
		enablejs.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setJavaScriptEnabled(isChecked);
			}

		});
		OrbotHelper oh = new OrbotHelper(this);
		if (!oh.isOrbotInstalled()) {
			orbot.setEnabled(false);
		}

		orbot.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setUseProxy(isChecked);

			}

		});
		color.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mPreferences.setColorModeEnabled(isChecked);

			}

		});
	}

	private void getFlashChoice() {
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(mContext.getResources().getString(R.string.title_flash));
		builder.setMessage(getResources().getString(R.string.flash))
				.setCancelable(true)
				.setPositiveButton(getResources().getString(R.string.action_manual),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								mPreferences.setFlashSupport(1);
							}
						})
				.setNegativeButton(getResources().getString(R.string.action_auto),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								mPreferences.setFlashSupport(2);
							}
						}).setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						mPreferences.setFlashSupport(0);
					}

				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void agentPicker() {
		final AlertDialog.Builder agentStringPicker = new AlertDialog.Builder(mActivity);

		agentStringPicker.setTitle(getResources().getString(R.string.title_user_agent));
		final EditText getAgent = new EditText(this);
		getAgent.append(mPreferences.getUserAgentString(""));
		agentStringPicker.setView(getAgent);
		agentStringPicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getAgent.getText().toString();
						mPreferences.setUserAgentString(text);
						getAgent.setText(getResources().getString(R.string.agent_custom));
					}
				});
		agentStringPicker.show();
	}

	public void general(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, GeneralSettingsActivity.class));
			}

		});
	}

	public void display(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, DisplaySettingsActivity.class));
			}

		});
	}

	public void privacy(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, PrivacySettingsActivity.class));
			}

		});
	}

	public void advanced(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, AdvancedSettingsActivity.class));
			}

		});
	}

	public void about(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, AboutSettingsActivity.class));
			}

		});
	}
}
