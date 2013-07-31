package acr.browser.barebones.activities;

import java.io.File;

import acr.browser.barebones.R;
import acr.browser.barebones.variables.BookmarkPageVariables;
import acr.browser.barebones.variables.FinalVariables;
import acr.browser.barebones.variables.Utils;
import acr.browser.barebones.activities.BarebonesActivity;
import android.os.Bundle;
import android.provider.Browser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebIconDatabase;
import android.webkit.WebViewDatabase;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AdvancedSettingsActivity extends Activity {

	// settings variables
	static boolean savePasswords, clearCache, enableJavaScript,
			enableTextReflow, blockImages, newWindow, incognitoAlways;
	static int defaultZoom, textSize;
	static final int API = FinalVariables.API;
	static final String preferences = "settings";
	static SharedPreferences settings;
	static SharedPreferences.Editor edit;
	static RelativeLayout r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12;
	static CheckBox cb1, cb2, cb3, cb4, cb5, cb6, cb7, cb8, cb9;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.advanced_settings);
		settings = getSharedPreferences(preferences, 0);
		edit = settings.edit();
		initialize();
	}

	void initialize() {

		r1 = (RelativeLayout) findViewById(R.id.r1);
		r2 = (RelativeLayout) findViewById(R.id.r2);
		r3 = (RelativeLayout) findViewById(R.id.r3);
		r4 = (RelativeLayout) findViewById(R.id.r4);
		r5 = (RelativeLayout) findViewById(R.id.r5);
		r6 = (RelativeLayout) findViewById(R.id.r6);
		r7 = (RelativeLayout) findViewById(R.id.r7);
		r8 = (RelativeLayout) findViewById(R.id.r8);
		r9 = (RelativeLayout) findViewById(R.id.r9);
		r10 = (RelativeLayout) findViewById(R.id.r10);
		r11 = (RelativeLayout) findViewById(R.id.r11);
		r12 = (RelativeLayout) findViewById(R.id.r12);

		cb1 = (CheckBox) findViewById(R.id.cb1);
		cb2 = (CheckBox) findViewById(R.id.cb2);
		cb3 = (CheckBox) findViewById(R.id.cb3);
		cb4 = (CheckBox) findViewById(R.id.cb4);
		cb5 = (CheckBox) findViewById(R.id.cb5);
		cb6 = (CheckBox) findViewById(R.id.cb6);
		cb7 = (CheckBox) findViewById(R.id.cb7);
		cb8 = (CheckBox) findViewById(R.id.cb8);
		cb9 = (CheckBox) findViewById(R.id.cb9);

		cb1.setChecked(settings.getBoolean("passwords", true));
		cb2.setChecked(settings.getBoolean("cache", false));
		cb3.setChecked(settings.getBoolean("java", true));
		cb4.setChecked(settings.getBoolean("textreflow", false));
		cb5.setChecked(settings.getBoolean("blockimages", false));
		cb6.setChecked(settings.getBoolean("newwindows", true));
		cb7.setChecked(settings.getBoolean("cookies", true));
		cb8.setChecked(settings.getBoolean("wideviewport", true));
		cb9.setChecked(settings.getBoolean("overviewmode", true));

		r1(r1);
		r2(r2);
		r3(r3);
		r4(r4);
		r5(r5);
		r6(r6);
		r7(r7);
		r8(r8);
		r9(r9);
		r10(r10);
		r11(r11);
		r12(r12);
		cb1(cb1);
		cb2(cb2);
		cb3(cb3);
		cb4(cb4);
		cb5(cb5);
		cb6(cb6);
		cb7(cb7);
		cb8(cb8);
		cb9(cb9);
		back();
		
		TextView importBookmarks = (TextView)findViewById(R.id.isImportAvailable);
		
		if(BarebonesActivity.noStockBrowser){
			importBookmarks.setText("(No Browser Available)");
		}
		else{
			importBookmarks.setText("(Supported Browser Detected)");
		}
	}

	void back() {
		ImageView back = (ImageView) findViewById(R.id.advanced_back);
		back.setBackgroundResource(R.drawable.button);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}

		});
	}

	static void cb1(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("passwords", isChecked);
				edit.commit();
			}

		});
	}

	void cb2(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("cache", isChecked);
				edit.commit();
			}

		});
	}

	void cb3(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("java", isChecked);
				edit.commit();
			}

		});
	}

	void cb4(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("textreflow", isChecked);
				edit.commit();
			}
		});
	}

	void cb5(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("blockimages", isChecked);
				edit.commit();
			}

		});
	}

	void cb6(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("newwindows", isChecked);
				edit.commit();
			}

		});
	}

	void cb7(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("cookies", isChecked);
				edit.commit();
			}

		});
	}
	
	void cb8(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("wideviewport", isChecked);
				edit.commit();
			}

		});
	}
	void cb9(CheckBox view) {
		view.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				edit.putBoolean("overviewmode", isChecked);
				edit.commit();
			}

		});
	}

	void r1(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb1.setChecked(!cb1.isChecked());
			}

		});
	}

	void r2(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb2.setChecked(!cb2.isChecked());
			}

		});
	}

	void r3(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb3.setChecked(!cb3.isChecked());
			}

		});
	}

	void r4(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb4.setChecked(!cb4.isChecked());
			}

		});
	}

	void r5(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb5.setChecked(!cb5.isChecked());
			}

		});
	}

	void r6(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb6.setChecked(!cb6.isChecked());
			}

		});
	}

	void r7(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb7.setChecked(!cb7.isChecked());
			}

		});
	}

	void r8(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						AdvancedSettingsActivity.this); // dialog
				builder.setTitle("Clear History");
				builder.setMessage(
						"Would you like to clear all browser history?")
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {
										Thread clear = new Thread(
												new Runnable() {

													@Override
													public void run() {
														clearHistory();
													}

												});
										clear.start();
									}

								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {
										// TODO Auto-generated method stub

									}

								}).show();
			}

		});
	}

	void r11(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb8.setChecked(!cb8.isChecked());
			}

		});
		
	}
	
	void r12(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				cb9.setChecked(!cb9.isChecked());
			}

		});
	}
	
	@SuppressWarnings("deprecation")
	public void clearHistory() {
		CookieManager c = CookieManager.getInstance();
		CookieSyncManager.createInstance(this);
		c.removeAllCookie();
		AdvancedSettingsActivity.this.deleteDatabase("historyManager");
		BarebonesActivity.main[0].clearCache(true);
		WebViewDatabase m = WebViewDatabase
				.getInstance(AdvancedSettingsActivity.this);
		m.clearFormData();
		m.clearHttpAuthUsernamePassword();
		if (API < 18) {
			m.clearUsernamePassword();
			WebIconDatabase.getInstance().removeAllIcons();
		}
		if (!BarebonesActivity.noStockBrowser) {
			try {
				Browser.clearHistory(getContentResolver());
			} catch (NullPointerException ignored) {
			}
		}
		trimCache(AdvancedSettingsActivity.this);
	}

	void r9(RelativeLayout view) {
		
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				importFromStockBrowser();
				
			}

		});
	}

	void r10(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder picker = new AlertDialog.Builder(
						AdvancedSettingsActivity.this);
				picker.setTitle("Text Size");
				CharSequence[] chars = { "Largest", "Large", "Normal", "Small", "Smallest"};
			
				int n = settings.getInt("textsize", 3);

				picker.setSingleChoiceItems(chars, n - 1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								edit.putInt("textsize", which+1);
								edit.commit();
								
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

	void trimCache(Context context) {
		try {
			File dir = context.getCacheDir();

			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception ignored) {

		}
	}

	boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(dir, aChildren));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	public void importFromStockBrowser() {
		if (!BarebonesActivity.noStockBrowser) {
			try {
				String[] proj = new String[] { Browser.BookmarkColumns.TITLE,
						Browser.BookmarkColumns.URL };
				String sel = Browser.BookmarkColumns.BOOKMARK + " = 1"; // 0 =
																		// history,
																		// 1 =
																		// bookmark
				Cursor mCur;
				mCur = getContentResolver().query(Browser.BOOKMARKS_URI, proj,
						sel, null, null);
				mCur.moveToFirst();

				String title = "";
				String url = "";

				if (mCur.moveToFirst() && mCur.getCount() > 0) {
					while (mCur.isAfterLast() == false) {

						title = mCur.getString(mCur
								.getColumnIndex(Browser.BookmarkColumns.TITLE));
						url = mCur.getString(mCur
								.getColumnIndex(Browser.BookmarkColumns.URL));
						Utils.addBookmark(getBaseContext(),title,url);
						mCur.moveToNext();
					}
				}
			} catch (NullPointerException ignored) {
			}
		}
		else{
			Utils.createInformativeDialog(getBaseContext(), "Error", "No browser was detected to import bookmarks from.");
		}
	}
	
}