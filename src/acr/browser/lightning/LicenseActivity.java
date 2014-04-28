/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

/*
 *NOTE: This activity must not be removed in order to comply with the Mozilla Public License v. 2.0 
 *under which this code is licensed. Unless you plan on providing other attribution in the app to 
 *the original source in another visible way, it is advised against the removal of this Activity.
 */
public class LicenseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.license_activity);
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		LinearLayout thunder = (LinearLayout) findViewById(R.id.browserLicense);
		LinearLayout aosp = (LinearLayout) findViewById(R.id.licenseAOSP);
		LinearLayout hosts = (LinearLayout) findViewById(R.id.licenseHosts);
		thunder.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://www.mozilla.org/MPL/2.0/")));
				finish();
			}

		});
		aosp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://www.apache.org/licenses/LICENSE-2.0")));
				finish();
			}

		});
		
		hosts.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://hosts-file.net/")));
				finish();
			}

		});

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return super.onOptionsItemSelected(item);
	}

}
