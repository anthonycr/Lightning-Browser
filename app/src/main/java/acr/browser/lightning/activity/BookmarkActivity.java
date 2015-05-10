package acr.browser.lightning.activity;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.R;

public class BookmarkActivity extends ThemableSettingsActivity implements OnClickListener {

	private BookmarkManager mBookmarkManager;
	private File[] mFileList;
	private String[] mFileNameList;
	private static final File mPath = new File(Environment.getExternalStorageDirectory().toString());
	private static final int DIALOG_LOAD_FILE = 1000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookmark_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		LinearLayout exportBackup = (LinearLayout) findViewById(R.id.exportBackup);
		LinearLayout importBackup = (LinearLayout) findViewById(R.id.importBackup);
		LinearLayout importFromBrowser = (LinearLayout) findViewById(R.id.importFromBrowser);

		TextView importBookmarks = (TextView) findViewById(R.id.isImportBrowserAvailable);

		mBookmarkManager = BookmarkManager.getInstance(getApplicationContext());
		PreferenceManager mPreferences = PreferenceManager.getInstance();

		boolean systemBrowser = mPreferences.getSystemBrowserPresent();

		exportBackup.setOnClickListener(this);
		importBackup.setOnClickListener(this);
		importFromBrowser.setOnClickListener(this);

		if (systemBrowser) {
			importBookmarks.setText(getResources().getString(R.string.stock_browser_available));
		} else {
			importBookmarks.setText(getResources().getString(R.string.stock_browser_unavailable));
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.importBackup:
				loadFileList(null);
				onCreateDialog(DIALOG_LOAD_FILE);
				break;
			case R.id.importFromBrowser:
				mBookmarkManager.importBookmarksFromBrowser(BookmarkActivity.this);
				break;
			case R.id.exportBackup:
				mBookmarkManager.exportBookmarks();
				break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		finish();
		return true;
	}

	private void loadFileList(File path) {
		File file;
		if (path != null) {
			file = path;
		} else {
			file = mPath;
		}
		try {
			file.mkdirs();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if (file.exists()) {
			mFileList = file.listFiles();
		} else {
			mFileList = new File[0];
		}

		Arrays.sort(mFileList, new SortName());

		if (mFileList == null) {
			mFileNameList = new String[0];
			mFileList = new File[0];
		} else {
			mFileNameList = new String[mFileList.length];
		}
		for (int n = 0; n < mFileList.length; n++) {
			mFileNameList[n] = mFileList[n].getName();
		}
	}
	
	private class SortName implements Comparator<File> {

		@Override
		public int compare(File a, File b) {
			if (a.isDirectory() && b.isDirectory())
				return a.getName().compareTo(b.getName());

			if (a.isDirectory())
				return -1;

			if (b.isDirectory())
				return 1;

			if (a.isFile() && b.isFile())
				return a.getName().compareTo(b.getName());
			else
				return 1;
		}
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		final AlertDialog.Builder builder = new Builder(this);

		switch (id) {
			case DIALOG_LOAD_FILE:
				builder.setTitle(R.string.title_chooser);
				if (mFileList == null) {
					dialog = builder.create();
					return dialog;
				}
				builder.setItems(mFileNameList, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mFileList[which].isDirectory()) {
							loadFileList(mFileList[which]);
							builder.setItems(mFileNameList, this);
							builder.show();
						} else {
							mBookmarkManager.importBookmarksFromFile(mFileList[which], BookmarkActivity.this);
						}
					}

				});
				break;
		}
		dialog = builder.show();
		return dialog;
	}
}
