package acr.browser.barebones.activities;

import acr.browser.barebones.R;
import acr.browser.barebones.R.drawable;
import acr.browser.barebones.R.id;
import acr.browser.barebones.R.layout;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	static int API = FinalVariables.API;
	static final String preferences = "settings";
	static SharedPreferences.Editor edit;
	static int agentChoice;
	static String homepage;
	static TextView agentText;
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
		edit = settings.edit();

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
				FinalVariables.DOWNLOAD_LOCATION);

		download.setText(downloadLocation);

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
			homepageText.setText("Bookmarks");
		} else if (homepage.contains("about:blank")) {
			homepageText.setText("Blank Page");
		} else {
			homepageText.setText(homepage);
		}

		switch (agentChoice) {
		case 1:
			agentText.setText("Default");
			break;
		case 2:
			agentText.setText("Desktop");
			break;
		case 3:
			agentText.setText("Mobile");
			break;
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
				picker.setTitle("Search Engine");
				CharSequence[] chars = { "Google (Suggested)", "Bing", "Yahoo",
						"StartPage", "DuckDuckGo (Privacy)" ,"Baidu"};

				int n = settings.getInt("search", 1);

				picker.setSingleChoiceItems(chars, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								edit.putInt("search", which + 1);
								edit.commit();
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
					startActivity(new Intent(
							Intent.ACTION_VIEW,
							Uri.parse("http://imgs.xkcd.com/comics/compiling.png")));
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
				edit.putBoolean("location", isChecked);
				edit.commit();

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
				edit.putInt("enableflash", n);
				edit.commit();
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
					Utils.createInformativeDialog(SettingsActivity.this,
							"Warning", "Adobe Flash Player was not detected.\n"
									+ "Please install Flash Player.");
					buttonView.setChecked(false);
					edit.putInt("enableflash", 0);
					edit.commit();

				} else if ((API > 17) && isChecked) {
					Utils.createInformativeDialog(
							SettingsActivity.this,
							"Warning",
							"Adobe Flash does not support Android 4.3 and will "
									+ "crash the browser, please do not report crashes that occur if you enable flash.");
				}
			}

		});
		fullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("fullscreen", isChecked);
				edit.commit();

			}

		});
	}

	public void initCheckBox(CheckBox location, CheckBox fullscreen,
			CheckBox flash) {
		location.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("location", isChecked);
				edit.commit();

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
				edit.putInt("enableflash", n);
				edit.commit();
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
					Utils.createInformativeDialog(SettingsActivity.this,
							"Warning", "Adobe Flash Player was not detected.\n"
									+ "Please install Flash Player.");
					buttonView.setChecked(false);
					edit.putInt("enableflash", 0);
					edit.commit();

				} else if ((API > 17) && isChecked) {
					Utils.createInformativeDialog(
							SettingsActivity.this,
							"Warning",
							"Adobe Flash does not support Android 4.3 and will"
									+ "crash the browser, please do not report crashes that occur if you enable flash.");
				}

			}

		});
		fullscreen.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("fullscreen", isChecked);
				edit.commit();

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
				agentPicker.setTitle("User Agent");
				CharSequence[] chars = { "Default", "Desktop", "Mobile" };
				agentChoice = settings.getInt("agentchoose", 1);
				agentPicker.setSingleChoiceItems(chars, agentChoice - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								edit.putInt("agentchoose", which + 1);
								edit.commit();
								switch (which + 1) {
								case 1:
									agentText.setText("Default");
									break;
								case 2:
									agentText.setText("Desktop");
									break;
								case 3:
									agentText.setText("Mobile");
									break;
								}
							}
						});
				agentPicker.setNeutralButton("OK",
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

	public void download(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				AlertDialog.Builder picker = new AlertDialog.Builder(
						SettingsActivity.this);
				picker.setTitle("Download Location");
				CharSequence[] chars = { "Default", "Custom" };
				downloadLocation = settings.getString("download",
						FinalVariables.DOWNLOAD_LOCATION);
				int n = -1;
				if (downloadLocation.contains(FinalVariables.DOWNLOAD_LOCATION)) {
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
									edit.putString("download",
											FinalVariables.DOWNLOAD_LOCATION);
									edit.commit();
									download.setText(FinalVariables.DOWNLOAD_LOCATION);
									break;
								case 2:
									downPicker();

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

	public void homePicker() {
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(
				SettingsActivity.this);
		homePicker.setTitle("Custom Homepage");
		final EditText getHome = new EditText(SettingsActivity.this);
		homepage = settings.getString("home", FinalVariables.HOMEPAGE);
		if (!homepage.startsWith("about:")) {
			getHome.setText(homepage);
		} else {
			getHome.setText("http://www.google.com");
		}
		homePicker.setView(getHome);
		homePicker.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getHome.getText().toString();
						edit.putString("home", text);
						edit.commit();
						homepageText.setText(text);
					}
				});
		homePicker.show();
	}

	public void downPicker() {
		final AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(
				SettingsActivity.this);
		downLocationPicker.setTitle("Custom Location");
		final EditText getDownload = new EditText(SettingsActivity.this);
		downloadLocation = settings.getString("download",
				FinalVariables.DOWNLOAD_LOCATION);
		getDownload.setText(downloadLocation);
		downLocationPicker.setView(getDownload);
		downLocationPicker.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getDownload.getText().toString();
						edit.putString("download", text);
						edit.commit();
						download.setText(text);
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
									edit.putString("home", "about:home");
									edit.commit();
									homepageText.setText("Bookmarks");
									break;
								case 2:
									edit.putString("home", "about:blank");
									edit.commit();
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
						"android.intent.action.ADVANCEDSETTINGS"));
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
