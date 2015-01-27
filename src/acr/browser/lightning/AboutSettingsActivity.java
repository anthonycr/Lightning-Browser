/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AboutSettingsActivity extends Activity {

	// mPreferences variables
	private SharedPreferences mPreferences;
	private int mEasterEggCounter;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_settings);

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

		mContext = this;
		initialize();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void initialize() {

		String code = "HOLO";

		try {
			PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
			code = p.versionName;
		} catch (NameNotFoundException e) {
			// TODO add logging
			e.printStackTrace();
		}

		TextView version = (TextView) findViewById(R.id.versionCode);
		version.setText(code + "");

		RelativeLayout licenses;
		licenses = (RelativeLayout) findViewById(R.id.layoutLicense);

		licenses.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// NOTE: In order to comply with the open source license,
				// it is advised that you leave this code so that the License
				// Activity may be viewed by the user.
				startActivity(new Intent(mContext, LicenseActivity.class));
			}

		});

		RelativeLayout source = (RelativeLayout) findViewById(R.id.layoutSource);

		source(source);
		easterEgg();
	}

	public void easterEgg() {
		RelativeLayout easter = (RelativeLayout) findViewById(R.id.layoutVersion);
		easter.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mEasterEggCounter++;
				if (mEasterEggCounter == 10) {

					startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse("http://imgs.xkcd.com/comics/compiling.png"), mContext,
							MainActivity.class));
					finish();
					mEasterEggCounter = 0;
				}
			}

		});
	}

	public void source(RelativeLayout view) {
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://twitter.com/RestainoAnthony"), mContext, MainActivity.class));
				finish();
			}

		});
	}

}
