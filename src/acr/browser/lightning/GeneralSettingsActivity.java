/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GeneralSettingsActivity extends ThemableSettingsActivity {

	// mPreferences variables
	private static final int API = android.os.Build.VERSION.SDK_INT;
	private SharedPreferences mPreferences;
	private SharedPreferences.Editor mEditPrefs;
	private int mAgentChoice;
	private String mHomepage;
	private TextView mAgentTextView;
	private TextView mDownloadTextView;
	private String mDownloadLocation;
	private TextView mHomepageText;
	private TextView mSearchText;
	private CheckBox cbSearchSuggestions;
	private Activity mActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.general_settings);

		// set up ActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// TODO WARNING: SharedPreferences.edit() without a corresponding
		// commit() or apply() call
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mEditPrefs = mPreferences.edit();

		mActivity = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		mSearchText = (TextView) findViewById(R.id.searchText);

		switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
			case 0:
				mSearchText.setText(getResources().getString(R.string.custom_url));
				break;
			case 1:
				mSearchText.setText("Google");
				break;
			case 2:
				mSearchText.setText("Ask");
				break;
			case 3:
				mSearchText.setText("Bing");
				break;
			case 4:
				mSearchText.setText("Yahoo");
				break;
			case 5:
				mSearchText.setText("StartPage");
				break;
			case 6:
				mSearchText.setText("StartPage (Mobile)");
				break;
			case 7:
				mSearchText.setText("DuckDuckGo");
				break;
			case 8:
				mSearchText.setText("DuckDuckGo Lite");
				break;
			case 9:
				mSearchText.setText("Baidu");
				break;
			case 10:
				mSearchText.setText("Yandex");
		}

		mAgentTextView = (TextView) findViewById(R.id.agentText);
		mHomepageText = (TextView) findViewById(R.id.homepageText);
		mDownloadTextView = (TextView) findViewById(R.id.downloadText);
		mAgentChoice = mPreferences.getInt(PreferenceConstants.USER_AGENT, 1);
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
		mDownloadLocation = mPreferences.getString(PreferenceConstants.DOWNLOAD_DIRECTORY,
				Environment.DIRECTORY_DOWNLOADS);

		mDownloadTextView.setText(Constants.EXTERNAL_STORAGE + '/' + mDownloadLocation);

		if (mHomepage.contains("about:home")) {
			mHomepageText.setText(getResources().getString(R.string.action_homepage));
		} else if (mHomepage.contains("about:blank")) {
			mHomepageText.setText(getResources().getString(R.string.action_blank));
		} else if (mHomepage.contains("about:bookmarks")) {
			mHomepageText.setText(getResources().getString(R.string.action_bookmarks));
		} else {
			mHomepageText.setText(mHomepage);
		}

		switch (mAgentChoice) {
			case 1:
				mAgentTextView.setText(getResources().getString(R.string.agent_default));
				break;
			case 2:
				mAgentTextView.setText(getResources().getString(R.string.agent_desktop));
				break;
			case 3:
				mAgentTextView.setText(getResources().getString(R.string.agent_mobile));
				break;
			case 4:
				mAgentTextView.setText(getResources().getString(R.string.agent_custom));
		}

		RelativeLayout rSearchSuggestions;
		rSearchSuggestions = (RelativeLayout) findViewById(R.id.rGoogleSuggestions);

		cbSearchSuggestions = (CheckBox) findViewById(R.id.cbGoogleSuggestions);

		cbSearchSuggestions.setChecked(mPreferences.getBoolean(
				PreferenceConstants.GOOGLE_SEARCH_SUGGESTIONS, true));

		RelativeLayout agent = (RelativeLayout) findViewById(R.id.layoutUserAgent);
		RelativeLayout download = (RelativeLayout) findViewById(R.id.layoutDownload);
		RelativeLayout homepage = (RelativeLayout) findViewById(R.id.layoutHomepage);

		agent(agent);
		download(download);
		homepage(homepage);
		search();

		rSearchSuggestions(rSearchSuggestions);
		cbSearchSuggestions(cbSearchSuggestions);
	}

	public void search() {
		RelativeLayout search = (RelativeLayout) findViewById(R.id.layoutSearch);
		search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
				picker.setTitle(getResources().getString(R.string.title_search_engine));
				CharSequence[] chars = { getResources().getString(R.string.custom_url), "Google",
						"Ask", "Bing", "Yahoo", "StartPage", "StartPage (Mobile)",
						"DuckDuckGo (Privacy)", "DuckDuckGo Lite (Privacy)", "Baidu (Chinese)",
						"Yandex (Russian)" };

				int n = mPreferences.getInt(PreferenceConstants.SEARCH, 1);

				picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mEditPrefs.putInt(PreferenceConstants.SEARCH, which);
						mEditPrefs.apply();
						switch (which) {
							case 0:
								searchUrlPicker();
								break;
							case 1:
								mSearchText.setText("Google");
								break;
							case 2:
								mSearchText.setText("Ask");
								break;
							case 3:
								mSearchText.setText("Bing");
								break;
							case 4:
								mSearchText.setText("Yahoo");
								break;
							case 5:
								mSearchText.setText("StartPage");
								break;
							case 6:
								mSearchText.setText("StartPage (Mobile)");
								break;
							case 7:
								mSearchText.setText("DuckDuckGo");
								break;
							case 8:
								mSearchText.setText("DuckDuckGo Lite");
								break;
							case 9:
								mSearchText.setText("Baidu");
								break;
							case 10:
								mSearchText.setText("Yandex");
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

	public void searchUrlPicker() {
		final AlertDialog.Builder urlPicker = new AlertDialog.Builder(this);

		urlPicker.setTitle(getResources().getString(R.string.custom_url));
		final EditText getSearchUrl = new EditText(this);

		String mSearchUrl = mPreferences.getString(PreferenceConstants.SEARCH_URL,
				Constants.GOOGLE_SEARCH);
		getSearchUrl.setText(mSearchUrl);
		urlPicker.setView(getSearchUrl);
		urlPicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getSearchUrl.getText().toString();
						mEditPrefs.putString(PreferenceConstants.SEARCH_URL, text);
						mEditPrefs.apply();
						mSearchText.setText(getResources().getString(R.string.custom_url) + ": "
								+ text);
					}
				});
		urlPicker.show();
	}

	public void agent(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder agentPicker = new AlertDialog.Builder(mActivity);
				agentPicker.setTitle(getResources().getString(R.string.title_user_agent));
				mAgentChoice = mPreferences.getInt(PreferenceConstants.USER_AGENT, 1);
				agentPicker.setSingleChoiceItems(R.array.user_agent, mAgentChoice - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								mEditPrefs.putInt(PreferenceConstants.USER_AGENT, which + 1);
								mEditPrefs.apply();
								switch (which + 1) {
									case 1:
										mAgentTextView.setText(getResources().getString(
												R.string.agent_default));
										break;
									case 2:
										mAgentTextView.setText(getResources().getString(
												R.string.agent_desktop));
										break;
									case 3:
										mAgentTextView.setText(getResources().getString(
												R.string.agent_mobile));
										break;
									case 4:
										mAgentTextView.setText(getResources().getString(
												R.string.agent_custom));
										agentPicker();
										break;
								}
							}
						});
				agentPicker.setNeutralButton(getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub

							}

						});
				agentPicker.setOnCancelListener(new DialogInterface.OnCancelListener() {

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
		final AlertDialog.Builder agentStringPicker = new AlertDialog.Builder(mActivity);

		agentStringPicker.setTitle(getResources().getString(R.string.title_user_agent));
		final EditText getAgent = new EditText(this);
		agentStringPicker.setView(getAgent);
		agentStringPicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getAgent.getText().toString();
						mEditPrefs.putString(PreferenceConstants.USER_AGENT_STRING, text);
						mEditPrefs.apply();
						mAgentTextView.setText(getResources().getString(R.string.agent_custom));
					}
				});
		agentStringPicker.show();
	}

	public void download(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
				picker.setTitle(getResources().getString(R.string.title_download_location));
				mDownloadLocation = mPreferences.getString(PreferenceConstants.DOWNLOAD_DIRECTORY,
						Environment.DIRECTORY_DOWNLOADS);
				int n;
				if (mDownloadLocation.contains(Environment.DIRECTORY_DOWNLOADS)) {
					n = 1;
				} else {
					n = 2;
				}

				picker.setSingleChoiceItems(R.array.download_folder, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

								switch (which + 1) {
									case 1:
										mEditPrefs.putString(
												PreferenceConstants.DOWNLOAD_DIRECTORY,
												Environment.DIRECTORY_DOWNLOADS);
										mEditPrefs.apply();
										mDownloadTextView.setText(Constants.EXTERNAL_STORAGE + '/'
												+ Environment.DIRECTORY_DOWNLOADS);
										break;
									case 2:
										downPicker();

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

	public void homePicker() {
		final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
		homePicker.setTitle(getResources().getString(R.string.title_custom_homepage));
		final EditText getHome = new EditText(this);
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
		if (!mHomepage.startsWith("about:")) {
			getHome.setText(mHomepage);
		} else {
			getHome.setText("http://www.google.com");
		}
		homePicker.setView(getHome);
		homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getHome.getText().toString();
						mEditPrefs.putString(PreferenceConstants.HOMEPAGE, text);
						mEditPrefs.apply();
						mHomepageText.setText(text);
					}
				});
		homePicker.show();
	}

	@SuppressWarnings("deprecation")
	public void downPicker() {
		final AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(mActivity);
		LinearLayout layout = new LinearLayout(this);
		downLocationPicker.setTitle(getResources().getString(R.string.title_download_location));
		final EditText getDownload = new EditText(this);
		getDownload.setBackgroundResource(0);
		mDownloadLocation = mPreferences.getString(PreferenceConstants.DOWNLOAD_DIRECTORY,
				Environment.DIRECTORY_DOWNLOADS);
		int padding = Utils.convertDpToPixels(10);

		LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

		getDownload.setLayoutParams(lparams);
		getDownload.setTextColor(Color.DKGRAY);
		getDownload.setText(mDownloadLocation);
		getDownload.setPadding(0, padding, padding, padding);

		TextView v = new TextView(this);
		v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		v.setTextColor(Color.DKGRAY);
		v.setText(Constants.EXTERNAL_STORAGE + '/');
		v.setPadding(padding, padding, 0, padding);
		layout.addView(v);
		layout.addView(getDownload);
		if (API < 16) {
			layout.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.edit_text));
		} else {
			layout.setBackground(getResources().getDrawable(android.R.drawable.edit_text));
		}
		downLocationPicker.setView(layout);
		downLocationPicker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getDownload.getText().toString();
						mEditPrefs.putString(PreferenceConstants.DOWNLOAD_DIRECTORY, text);
						mEditPrefs.apply();
						mDownloadTextView.setText(Constants.EXTERNAL_STORAGE + '/' + text);
					}
				});
		downLocationPicker.show();
	}

	public void homepage(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
				picker.setTitle(getResources().getString(R.string.home));
				mHomepage = mPreferences
						.getString(PreferenceConstants.HOMEPAGE, Constants.HOMEPAGE);
				int n;
				if (mHomepage.contains("about:home")) {
					n = 1;
				} else if (mHomepage.contains("about:blank")) {
					n = 2;
				} else if (mHomepage.contains("about:bookmarks")) {
					n = 3;
				} else {
					n = 4;
				}

				picker.setSingleChoiceItems(R.array.homepage, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {

								switch (which + 1) {
									case 1:
										mEditPrefs.putString(PreferenceConstants.HOMEPAGE,
												"about:home");
										mEditPrefs.apply();
										mHomepageText.setText(getResources().getString(
												R.string.action_homepage));
										break;
									case 2:
										mEditPrefs.putString(PreferenceConstants.HOMEPAGE,
												"about:blank");
										mEditPrefs.apply();
										mHomepageText.setText(getResources().getString(
												R.string.action_blank));
										break;
									case 3:
										mEditPrefs.putString(PreferenceConstants.HOMEPAGE,
												"about:bookmarks");
										mEditPrefs.apply();
										mHomepageText.setText(getResources().getString(
												R.string.action_bookmarks));

										break;
									case 4:
										homePicker();

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

	private void cbSearchSuggestions(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mEditPrefs.putBoolean(PreferenceConstants.GOOGLE_SEARCH_SUGGESTIONS, isChecked);
				mEditPrefs.apply();
			}

		});
	}

	private void rSearchSuggestions(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				cbSearchSuggestions.setChecked(!cbSearchSuggestions.isChecked());
			}

		});
	}

}
