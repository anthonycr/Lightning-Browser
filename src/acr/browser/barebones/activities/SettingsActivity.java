package acr.browser.barebones.activities;

import acr.browser.barebones.R;
import acr.browser.barebones.utilities.FinalVariables;
import acr.browser.barebones.utilities.Utils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
	static int API = FinalVariables.API;
	static final String preferences = "settings";
	static SharedPreferences.Editor mEditPrefs;
	static int agentChoice;
	static String homepage;
	static TextView agentText;
	static String agent;
	static TextView download;
	static int egg = 0;
	static String downloadLocation;
	static TextView homepageText;
	static SharedPreferences settings;
	static TextView searchText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		init();
	}

	@SuppressLint("NewApi")
	public void init() {
		// settings storage
		settings = getSharedPreferences(preferences, 0);
		if (settings.getBoolean("hidestatus", false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		mEditPrefs= settings.edit();

		// initialize UI
		RelativeLayout layoutLocation = (RelativeLayout) findViewById(R.id.layoutLocation);
		RelativeLayout layoutFullScreen = (RelativeLayout) findViewById(R.id.layoutFullScreen);
		RelativeLayout layoutFlash = (RelativeLayout) findViewById(R.id.layoutFlash);
		ImageView back = (ImageView) findViewById(R.id.back);

		searchText = (TextView) findViewById(R.id.searchText);

		switch (settings.getInt("search", 1)) {
		case 1:
			searchText.setText("Google");
			break;
		case 2:
			searchText.setText("Bing");
			break;
		case 3:
			searchText.setText("Yahoo");
			break;
		case 4:
			searchText.setText("StartPage");
			break;
		case 5:
			searchText.setText("DuckDuckGo");
			break;
		case 6:
			searchText.setText("Baidu");
			break;
		case 7:
			searchText.setText("Yandex");
			break;
		case 8:
			searchText.setText("DuckDuckGo Lite");
			break;
		}

		back.setBackgroundResource(R.drawable.button);
		agentText = (TextView) findViewById(R.id.agentText);
		homepageText = (TextView) findViewById(R.id.homepageText);
		download = (TextView) findViewById(R.id.downloadText);

		boolean locationBool = settings.getBoolean("location", false);
		int flashNum = settings.getInt("enableflash", 0);
		boolean fullScreenBool = settings.getBoolean("fullscreen", false);
		agentChoice = settings.getInt("agentchoose", 1);
		homepage = settings.getString("home", FinalVariables.HOMEPAGE);
		downloadLocation = settings.getString("download",
				Environment.DIRECTORY_DOWNLOADS);

		download.setText(FinalVariables.EXTERNAL_STORAGE + "/"
				+ downloadLocation);

		String code = "HOLO";

		try {
			PackageInfo p = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			code = p.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TextView version = (TextView) findViewById(R.id.versionCode);
		version.setText(code + "");

		if (homepage.contains("about:home")) {
			homepageText.setText(getResources().getString(
					R.string.action_homepage));
		} else if (homepage.contains("about:blank")) {
			homepageText.setText(getResources()
					.getString(R.string.action_blank));
		} else {
			homepageText.setText(homepage);
		}

		switch (agentChoice) {
		case 1:
			agentText.setText(getResources().getString(R.string.agent_default));
			break;
		case 2:
			agentText.setText(getResources().getString(R.string.agent_desktop));
			break;
		case 3:
			agentText.setText(getResources().getString(R.string.agent_mobile));
			break;
		case 4:
			agentText.setText(getResources().getString(R.string.agent_custom));
		}
		RelativeLayout r1, r2, r3;
		r1 = (RelativeLayout) findViewById(R.id.setR1);
		r2 = (RelativeLayout) findViewById(R.id.setR2);
		r3 = (RelativeLayout) findViewById(R.id.setR3);
		if (API >= 14) {
			Switch location = new Switch(this);
			Switch fullScreen = new Switch(this);
			Switch flash = new Switch(this);

			r1.addView(location);
			r2.addView(fullScreen);
			r3.addView(flash);
			location.setChecked(locationBool);
			fullScreen.setChecked(fullScreenBool);
			if (flashNum > 0) {
				flash.setChecked(true);
			} else {
				flash.setChecked(false);
			}

			initSwitch(location, fullScreen, flash);
			clickListenerForSwitches(layoutLocation, layoutFullScreen,
					layoutFlash, location, fullScreen, flash);

		} else {
			CheckBox location = new CheckBox(this);
			CheckBox fullScreen = new CheckBox(this);
			CheckBox flash = new CheckBox(this);

			r1.addView(location);
			r2.addView(fullScreen);
			r3.addView(flash);

			location.setChecked(locationBool);
			fullScreen.setChecked(fullScreenBool);
			if (flashNum > 0) {
				flash.setChecked(true);
			} else {
				flash.setChecked(false);
			}
			initCheckBox(location, fullScreen, flash);
			clickListenerForCheckBoxes(layoutLocation, layoutFullScreen,
					layoutFlash, location, fullScreen, flash);
		}

		RelativeLayout agent = (RelativeLayout) findViewById(R.id.layoutUserAgent);
		RelativeLayout download = (RelativeLayout) findViewById(R.id.layoutDownload);
		RelativeLayout homepage = (RelativeLayout) findViewById(R.id.layoutHomepage);
		RelativeLayout advanced = (RelativeLayout) findViewById(R.id.layoutAdvanced);
		RelativeLayout source = (RelativeLayout) findViewById(R.id.layoutSource);
		RelativeLayout license = (RelativeLayout) findViewById(R.id.layoutLicense);

		back(back);
		agent(agent);
		download(download);
		homepage(homepage);
		advanced(advanced);
		source(source);
		license(license);
		search();
		easterEgg();
	}

	public void search() {
		RelativeLayout search = (RelativeLayout) findViewById(R.id.layoutSearch);
		search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(
						SettingsActivity.this);
				picker.setTitle(getResources().getString(
						R.string.title_search_engine));
				CharSequence[] chars = { "Google", "Bing", "Yahoo",
						"StartPage", "DuckDuckGo (Privacy)" , "Baidu (Chinese)", "Yandex (Russian)", "DuckDuckGo Lite (Privacy)"};

				int n = settings.getInt("search", 1);

				picker.setSingleChoiceItems(chars, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mEditPrefs.putInt("search", which + 1);
								mEditPrefs.commit();
								switch (which + 1) {
								case 1:
									searchText.setText("Google");
									break;
								case 2:
									searchText.setText("Bing");
									break;
								case 3:
									searchText.setText("Yahoo");
									break;
								case 4:
									searchText.setText("StartPage");
									break;
								case 5:
									searchText.setText("DuckDuckGo");
									break;
								case 6:
									searchText.setText("Baidu");
									break;
								case 7:
									searchText.setText("Yandex");
									break;
								case 8:
									searchText.setText("DuckDuckGo Lite");
									break;
								}
							}
						});
				picker.setNeutralButton(getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						});
				picker.show();
			}

		});
	}

	public void clickListenerForCheckBoxes(RelativeLayout one,
			RelativeLayout two, RelativeLayout three, final CheckBox loc,
			final CheckBox full, final CheckBox flash) {
		one.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				loc.setChecked(!loc.isChecked());
			}

		});
		two.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				full.setChecked(!full.isChecked());
			}

		});
		three.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				flash.setChecked(!flash.isChecked());
			}

		});
	}

	public void clickListenerForSwitches(RelativeLayout one,
			RelativeLayout two, RelativeLayout three, final Switch loc,
			final Switch full, final Switch flash) {
		one.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				loc.setChecked(!loc.isChecked());
			}

		});
		two.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				full.setChecked(!full.isChecked());
			}

		});
		three.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				flash.setChecked(!flash.isChecked());
			}

		});
	}

	public void easterEgg() {
		RelativeLayout easter = (RelativeLayout) findViewById(R.id.layoutVersion);
		easter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				egg++;
				if (egg == 10) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse("http://imgs.xkcd.com/comics/compiling.png")));
					finish();
					egg = 0;
				}
			}

		});
	}

	public void initSwitch(Switch location, Switch fullscreen, Switch flash) {
		location.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEditPrefs.putBoolean("location", isChecked);
				mEditPrefs.commit();

			}

		});
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				int n = 0;
				if (isChecked) {
					n = 1;
				}
				mEditPrefs.putInt("enableflash", n);
				mEditPrefs.commit();
				boolean flashInstalled = false;
				try {
					PackageManager pm = getPackageManager();
					ApplicationInfo ai = pm.getApplicationInfo(
							"com.adobe.flashplayer", 0);
					if (ai != null)
						flashInstalled = true;
				} catch (NameNotFoundException e) {
					flashInstalled = false;
				}
				if (!flashInstalled && isChecked) {
					Utils.createInformativeDialog(
							SettingsActivity.this,
							getResources().getString(R.string.title_warning),
							getResources().getString(
									R.string.dialog_adobe_not_installed));
					buttonView.setChecked(false);
					mEditPrefs.putInt("enableflash", 0);
					mEditPrefs.commit();

				} else if ((API >= 17) && isChecked) {
					Utils.createInformativeDialog(
							SettingsActivity.this,
							getResources().getString(R.string.title_warning),
							getResources().getString(
									R.string.dialog_adobe_unsupported));
				}
			}

		});
		fullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEditPrefs.putBoolean("fullscreen", isChecked);
				mEditPrefs.commit();

			}

		});
	}

	public void initCheckBox(CheckBox location, CheckBox fullscreen,
			CheckBox flash) {
		location.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEditPrefs.putBoolean("location", isChecked);
				mEditPrefs.commit();

			}

		});
		flash.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				int n = 0;
				if (isChecked) {
					n = 1;
				}
				mEditPrefs.putInt("enableflash", n);
				mEditPrefs.commit();
				boolean flashInstalled = false;
				try {
					PackageManager pm = getPackageManager();
					ApplicationInfo ai = pm.getApplicationInfo(
							"com.adobe.flashplayer", 0);
					if (ai != null)
						flashInstalled = true;
				} catch (NameNotFoundException e) {
					flashInstalled = false;
				}
				if (!flashInstalled && isChecked) {
					Utils.createInformativeDialog(
							SettingsActivity.this,
							getResources().getString(R.string.title_warning),
							getResources().getString(
									R.string.dialog_adobe_not_installed));
					buttonView.setChecked(false);
					mEditPrefs.putInt("enableflash", 0);
					mEditPrefs.commit();

				} else if ((API > 17) && isChecked) {
					Utils.createInformativeDialog(
							SettingsActivity.this,
							getResources().getString(R.string.title_warning),
							getResources().getString(
									R.string.dialog_adobe_unsupported));
				}
			}

		});
		fullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEditPrefs.putBoolean("fullscreen", isChecked);
				mEditPrefs.commit();

			}

		});
	}

	public void back(ImageView view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}

		});
	}

	public void agent(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder agentPicker = new AlertDialog.Builder(
						SettingsActivity.this);
				agentPicker.setTitle(getResources().getString(
						R.string.title_user_agent));
				CharSequence[] chars = {
						getResources().getString(R.string.agent_default),
						getResources().getString(R.string.agent_desktop),
						getResources().getString(R.string.agent_mobile),
						getResources().getString(R.string.agent_custom) };
				agentChoice = settings.getInt("agentchoose", 1);
				agentPicker.setSingleChoiceItems(chars, agentChoice - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mEditPrefs.putInt("agentchoose", which + 1);
								mEditPrefs.commit();
								switch (which + 1) {
								case 1:
									agentText.setText(getResources().getString(
											R.string.agent_default));
									break;
								case 2:
									agentText.setText(getResources().getString(
											R.string.agent_desktop));
									break;
								case 3:
									agentText.setText(getResources().getString(
											R.string.agent_mobile));
									break;
								case 4:
									agentText.setText(getResources().getString(
											R.string.agent_custom));
									agentPicker();
									break;
								}
							}
						});
				agentPicker.setNeutralButton(
						getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub

							}

						});
				agentPicker
						.setOnCancelListener(new DialogInterface.OnCancelListener() {

							@Override
							public void onCancel(DialogInterface dialog) {
								// TODO Auto-generated method stub
								Log.i("Cancelled", "");
							}
						});
				agentPicker.show();

			}

		});
	}
	
	public void agentPicker() {
		final AlertDialog.Builder agentStringPicker = new AlertDialog.Builder(
				SettingsActivity.this);
		
		agentStringPicker.setTitle(getResources().getString(
				R.string.title_user_agent));
		final EditText getAgent = new EditText(SettingsActivity.this);
		agentStringPicker.setView(getAgent);
		agentStringPicker.setPositiveButton(
				getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getAgent.getText().toString();
						mEditPrefs.putString("userAgentString", text);
						mEditPrefs.commit();
						agentText.setText(getResources().getString(
								R.string.agent_custom));
					}
				});
		agentStringPicker.show();
	}

	public void download(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				AlertDialog.Builder picker = new AlertDialog.Builder(
						SettingsActivity.this);
				picker.setTitle(getResources().getString(
						R.string.title_download_location));
				CharSequence[] chars = {
						getResources().getString(R.string.agent_default),
						getResources().getString(R.string.agent_custom) };
				downloadLocation = settings.getString("download",
						Environment.DIRECTORY_DOWNLOADS);
				int n = -1;
				if (downloadLocation.contains(Environment.DIRECTORY_DOWNLOADS)) {
					n = 1;
				} else {
					n = 2;
				}

				picker.setSingleChoiceItems(chars, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								switch (which + 1) {
								case 1:
									mEditPrefs.putString("download",
											Environment.DIRECTORY_DOWNLOADS);
									mEditPrefs.commit();
									download.setText(FinalVariables.EXTERNAL_STORAGE
											+ "/"
											+ Environment.DIRECTORY_DOWNLOADS);
									break;
								case 2:
									downPicker();

									break;
								}
							}
						});
				picker.setNeutralButton(
						getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						});
				picker.show();
			}

		});
	}

	public void homePicker() {
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(
				this);
		homePicker.setTitle(getResources().getString(
				R.string.title_custom_homepage));
		final EditText getHome = new EditText(SettingsActivity.this);
		homepage = settings.getString("home", FinalVariables.HOMEPAGE);
		if (!homepage.startsWith("about:")) {
			getHome.setText(homepage);
		} else {
			getHome.setText("http://www.google.com");
		}
		homePicker.setView(getHome);
		homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getHome.getText().toString();
						mEditPrefs.putString("home", text);
						mEditPrefs.commit();
						homepageText.setText(text);
					}
				});
		homePicker.show();
	}

	@SuppressWarnings("deprecation")
	public void downPicker() {
		final AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(
				this);
		LinearLayout layout = new LinearLayout(this);
		downLocationPicker.setTitle(getResources().getString(
				R.string.title_download_location));
		final EditText getDownload = new EditText(SettingsActivity.this);
		getDownload.setBackgroundResource(0);
		downloadLocation = settings.getString("download",
				Environment.DIRECTORY_DOWNLOADS);
		int padding = Utils.convertDensityPixels(this, 10);

		LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);

		getDownload.setLayoutParams(lparams);
		getDownload.setTextColor(Color.DKGRAY);
		getDownload.setText(downloadLocation);
		getDownload.setPadding(0, padding, padding, padding);

		TextView v = new TextView(this);
		v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		v.setTextColor(Color.DKGRAY);
		v.setText(FinalVariables.EXTERNAL_STORAGE + "/");
		v.setPadding(padding, padding, 0, padding);
		layout.addView(v);
		layout.addView(getDownload);
		if (API < 16) {
			layout.setBackgroundDrawable(getResources().getDrawable(
					android.R.drawable.edit_text));
		} else {
			layout.setBackground(getResources().getDrawable(
					android.R.drawable.edit_text));
		}
		downLocationPicker.setView(layout);
		downLocationPicker.setPositiveButton(
				getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getDownload.getText().toString();
						mEditPrefs.putString("download", text);
						mEditPrefs.commit();
						download.setText(FinalVariables.EXTERNAL_STORAGE + "/"
								+ text);
					}
				});
		downLocationPicker.show();
	}

	public void homepage(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(
						SettingsActivity.this);
				picker.setTitle("Homepage");
				CharSequence[] chars = { "Bookmarks", "Blank Page", "Webpage" };
				homepage = settings.getString("home", FinalVariables.HOMEPAGE);
				int n = -1;
				if (homepage.contains("about:home")) {
					n = 1;
				} else if (homepage.contains("about:blank")) {
					n = 2;
				} else {
					n = 3;
				}

				picker.setSingleChoiceItems(chars, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								switch (which + 1) {
								case 1:
									mEditPrefs.putString("home", "about:home");
									mEditPrefs.commit();
									homepageText.setText("Bookmarks");
									break;
								case 2:
									mEditPrefs.putString("home", "about:blank");
									mEditPrefs.commit();
									homepageText.setText("Blank Page");
									break;
								case 3:
									homePicker();

									break;
								}
							}
						});
				picker.setNeutralButton("OK",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						});
				picker.show();
			}

		});
	}

	public void advanced(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(
						FinalVariables.ADVANCED_SETTINGS_INTENT));
			}

		});
	}

	public void source(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("https://github.com/anthonycr/Lightning-Browser")));
				finish();
			}

		});
	}

	public void license(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://www.apache.org/licenses/LICENSE-2.0")));
				finish();
			}

		});
	}
}
