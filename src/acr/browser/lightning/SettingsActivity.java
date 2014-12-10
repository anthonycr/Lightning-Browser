/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Switch;
import info.guardianproject.onionkit.ui.OrbotHelper;

public class SettingsActivity extends Activity {

	private static int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences.Editor mEditPrefs;
	private SharedPreferences mPreferences;
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
		// mPreferences storage
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

		mEditPrefs = mPreferences.edit();

		// initialize UI
		RelativeLayout layoutFlash = (RelativeLayout) findViewById(R.id.layoutFlash);
		RelativeLayout layoutBlockAds = (RelativeLayout) findViewById(R.id.layoutAdBlock);
		RelativeLayout layoutImages = (RelativeLayout) findViewById(R.id.layoutImages);
		RelativeLayout layoutEnableJS = (RelativeLayout) findViewById(R.id.layoutEnableJS);
		RelativeLayout layoutOrbot = (RelativeLayout) findViewById(R.id.layoutUseOrbot);
		RelativeLayout layoutBookmarks = (RelativeLayout) findViewById(R.id.layoutBookmarks);
		
		layoutBookmarks.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, BookmarkActivity.class));
			}
			
		});

		if (API >= 19) {
			mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
			mEditPrefs.commit();
		}
		int flashNum = mPreferences.getInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
		boolean imagesBool = mPreferences.getBoolean(PreferenceConstants.BLOCK_IMAGES, false);
		boolean enableJSBool = mPreferences.getBoolean(PreferenceConstants.JAVASCRIPT, true);

		RelativeLayout r1, r2, r3, r4, r5;
		r1 = (RelativeLayout) findViewById(R.id.setR1);
		r2 = (RelativeLayout) findViewById(R.id.setR2);
		r3 = (RelativeLayout) findViewById(R.id.setR3);
		r4 = (RelativeLayout) findViewById(R.id.setR4);
		r5 = (RelativeLayout) findViewById(R.id.setR5);

		Switch flash = new Switch(this);
		Switch adblock = new Switch(this);
		Switch images = new Switch(this);
		Switch enablejs = new Switch(this);
		Switch orbot = new Switch(this);

		r1.addView(flash);
		r2.addView(adblock);
		r3.addView(images);
		r4.addView(enablejs);
		r5.addView(orbot);
		images.setChecked(imagesBool);
		enablejs.setChecked(enableJSBool);
		if (flashNum > 0) {
			flash.setChecked(true);
		} else {
			flash.setChecked(false);
		}
		adblock.setChecked(mPreferences.getBoolean(PreferenceConstants.BLOCK_ADS, false));
		orbot.setChecked(mPreferences.getBoolean(PreferenceConstants.USE_PROXY, false));

		initSwitch(flash, adblock, images, enablejs, orbot);
		clickListenerForSwitches(layoutFlash, layoutBlockAds, layoutImages, layoutEnableJS, 
				layoutOrbot, flash, adblock, images, enablejs, orbot);

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

	public void clickListenerForSwitches(RelativeLayout layoutFlash, RelativeLayout layoutBlockAds,
			RelativeLayout layoutImages, RelativeLayout layoutEnableJS, RelativeLayout layoutOrbot,
			final Switch flash, final Switch adblock, final Switch images, final Switch enablejs, 
			final Switch orbot) {
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
	}

	public void initSwitch(Switch flash, Switch adblock, Switch images, Switch enablejs,
			Switch orbot) {
		flash.setEnabled(API < 19);
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					getFlashChoice();
				} else {
					mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
					mEditPrefs.commit();
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
					mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
					mEditPrefs.commit();

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
				mEditPrefs.putBoolean(PreferenceConstants.BLOCK_ADS, isChecked);
				mEditPrefs.commit();
			}

		});
		images.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.BLOCK_IMAGES, isChecked);
				mEditPrefs.commit();

			}

		});
		enablejs.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.JAVASCRIPT, isChecked);
				mEditPrefs.commit();

			}

		});
		OrbotHelper oh = new OrbotHelper(this);
		if (!oh.isOrbotInstalled()) {
			orbot.setEnabled(false);
		}

		orbot.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.USE_PROXY, isChecked);
				mEditPrefs.commit();

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
								mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 1);
								mEditPrefs.commit();
							}
						})
				.setNegativeButton(getResources().getString(R.string.action_auto),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 2);
								mEditPrefs.commit();
							}
						}).setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
						mEditPrefs.commit();
					}

				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void initCheckBox(CheckBox flash, CheckBox images, CheckBox enablejs) {
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				int n = 0;
				if (isChecked) {
					n = 1;
				}
				mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, n);
				mEditPrefs.commit();
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
					mEditPrefs.putInt(PreferenceConstants.ADOBE_FLASH_SUPPORT, 0);
					mEditPrefs.commit();

				} else if ((API > 17) && isChecked) {
					Utils.createInformativeDialog(SettingsActivity.this,
							getResources().getString(R.string.title_warning), getResources()
									.getString(R.string.dialog_adobe_unsupported));
				}
			}

		});
		images.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.BLOCK_IMAGES, isChecked);
				mEditPrefs.commit();

			}

		});
		enablejs.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.JAVASCRIPT, isChecked);
				mEditPrefs.commit();

			}

		});
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
