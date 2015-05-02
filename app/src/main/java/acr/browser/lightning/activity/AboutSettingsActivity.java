/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import acr.browser.lightning.R;

public class AboutSettingsActivity extends ThemableSettingsActivity implements OnClickListener {

	private int mEasterEggCounter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_settings);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		initialize();
	}

	private void initialize() {
		String code = "1.0";
		try {
			PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
			code = p.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		TextView versionCode = (TextView) findViewById(R.id.versionCode);
		versionCode.setText(code);

		RelativeLayout licenses = (RelativeLayout) findViewById(R.id.layoutLicense);
		RelativeLayout source = (RelativeLayout) findViewById(R.id.layoutSource);
		RelativeLayout version = (RelativeLayout) findViewById(R.id.layoutVersion);
		licenses.setOnClickListener(this);
		source.setOnClickListener(this);
		version.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.layoutLicense:
				// NOTE: In order to comply legally with open source licenses,
				// it is advised that you leave this code so that the License
				// Activity may be viewed by the user.
				startActivity(new Intent(this, LicenseActivity.class));
				break;
			case R.id.layoutSource:
				startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://twitter.com/RestainoAnthony"), this, MainActivity.class));
				finish();
				break;
			case R.id.layoutVersion:
				mEasterEggCounter++;
				if (mEasterEggCounter == 10) {
					startActivity(new Intent(Intent.ACTION_VIEW,
							Uri.parse("http://imgs.xkcd.com/comics/compiling.png"), this,
							MainActivity.class));
					finish();
					mEasterEggCounter = 0;
				}
				break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

}
