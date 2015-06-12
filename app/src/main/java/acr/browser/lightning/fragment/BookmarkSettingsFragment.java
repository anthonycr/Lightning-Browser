/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;

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
    private PreferenceManager mPreferences;
    private BookmarkManager mBookmarkManager;
    private File[] mFileList;
    private String[] mFileNameList;
    private static final File mPath = new File(Environment.getExternalStorageDirectory().toString());
    private static final int DIALOG_LOAD_FILE = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_bookmarks);

        mActivity = getActivity();

        mBookmarkManager = BookmarkManager.getInstance(mActivity);

        initPrefs();
    }

    private void initPrefs() {
        // mPreferences storage
        mPreferences = PreferenceManager.getInstance();

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
                mBookmarkManager.exportBookmarks();
                return true;
            case SETTINGS_IMPORT:
                loadFileList(null);
                onCreateDialog(DIALOG_LOAD_FILE);
                return true;
            case SETTINGS_BROWSER_IMPORT:
                mBookmarkManager.importBookmarksFromBrowser(mActivity);
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

    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

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
                            mBookmarkManager.importBookmarksFromFile(mFileList[which], mActivity);
                        }
                    }

                });
                break;
        }
        dialog = builder.show();
        return dialog;
    }
}
