/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import acr.browser.lightning.R;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.preference.PreferenceManager;

public class BookmarkSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String SETTINGS_EXPORT = "export_bookmark";
    private static final String SETTINGS_IMPORT = "import_bookmark";
    private static final String SETTINGS_BROWSER_IMPORT = "import_browser_bookmark";

    private Activity mActivity;
    private BookmarkManager mBookmarkManager;
    private File[] mFileList;
    private String[] mFileNameList;
    private static final File mPath = new File(Environment.getExternalStorageDirectory().toString());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_bookmarks);

        mActivity = getActivity();

        mBookmarkManager = BookmarkManager.getInstance(mActivity.getApplicationContext());

        initPrefs();
    }

    private void initPrefs() {
        // mPreferences storage
        PreferenceManager mPreferences = PreferenceManager.getInstance();

        Preference exportpref = findPreference(SETTINGS_EXPORT);
        Preference importpref = findPreference(SETTINGS_IMPORT);
        Preference importBrowserpref = findPreference(SETTINGS_BROWSER_IMPORT);

        exportpref.setOnPreferenceClickListener(this);
        importpref.setOnPreferenceClickListener(this);
        importBrowserpref.setOnPreferenceClickListener(this);

        if (mPreferences.getSystemBrowserPresent()) {
            importBrowserpref.setSummary(getResources().getString(R.string.stock_browser_available));
        } else {
            importBrowserpref.setSummary(getResources().getString(R.string.stock_browser_unavailable));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_EXPORT:
                mBookmarkManager.exportBookmarks(getActivity());
                return true;
            case SETTINGS_IMPORT:
                loadFileList(null);
                createDialog();
                return true;
            case SETTINGS_BROWSER_IMPORT:
                mBookmarkManager.importBookmarksFromBrowser(getActivity());
                return true;
            default:
                return false;
        }
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

    private void createDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        final String title = getString(R.string.title_chooser);
        builder.setTitle(title + ": " + Environment.getExternalStorageDirectory());
        if (mFileList == null) {
            builder.show();
        }
        builder.setItems(mFileNameList, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mFileList[which].isDirectory()) {
                    builder.setTitle(title + ": " + mFileList[which]);
                    loadFileList(mFileList[which]);
                    builder.setItems(mFileNameList, this);
                    builder.show();
                } else {
                    mBookmarkManager.importBookmarksFromFile(mFileList[which], getActivity());
                }
            }

        });
        builder.show();
    }
}
