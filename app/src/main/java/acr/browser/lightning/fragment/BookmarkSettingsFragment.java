/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.BookmarkLocalSync;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.PermissionsManager;
import acr.browser.lightning.utils.Utils;

public class BookmarkSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String SETTINGS_EXPORT = "export_bookmark";
    private static final String SETTINGS_IMPORT = "import_bookmark";
    private static final String SETTINGS_IMPORT_BROWSER = "import_browser";

    private Activity mActivity;
    @Inject
    BookmarkManager mBookmarkManager;
    private File[] mFileList;
    private String[] mFileNameList;
    private BookmarkLocalSync mSync;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final File mPath = new File(Environment.getExternalStorageDirectory().toString());

    private class ImportBookmarksTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            List<HistoryItem> list = null;
            if (mSync.isStockSupported()) {
                list = mSync.getBookmarksFromStockBrowser();
            } else if (mSync.isChromeSupported()) {
                list = mSync.getBookmarksFromChrome();
            }
            int count = 0;
            if (list != null && !list.isEmpty()) {
                mBookmarkManager.addBookmarkList(list);
                count = list.size();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer num) {
            super.onPostExecute(num);
            if (mActivity != null) {
                int number = num;
                final String message = mActivity.getResources().getString(R.string.message_import);
                Utils.showSnackbar(mActivity, number + " " + message);
            }
        }
    }

    ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_bookmarks);

        mActivity = getActivity();

        initPrefs();

        PermissionsManager permissionsManager = PermissionsManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            permissionsManager.requestPermissionsIfNecessary(getActivity(), REQUIRED_PERMISSIONS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity = null;
    }

    private void initPrefs() {

        Preference exportpref = findPreference(SETTINGS_EXPORT);
        Preference importpref = findPreference(SETTINGS_IMPORT);
        Preference importStock = findPreference(SETTINGS_IMPORT_BROWSER);

        mSync = new BookmarkLocalSync(mActivity);

        importStock.setEnabled(mSync.isStockSupported() || mSync.isChromeSupported());

        exportpref.setOnPreferenceClickListener(this);
        importpref.setOnPreferenceClickListener(this);
        importStock.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_EXPORT:
                if (PermissionsManager.checkPermissions(getActivity(), REQUIRED_PERMISSIONS)) {
                    mBookmarkManager.exportBookmarks(getActivity());
                }
                return true;
            case SETTINGS_IMPORT:
                if (PermissionsManager.checkPermissions(getActivity(), REQUIRED_PERMISSIONS)) {
                    loadFileList(null);
                    createDialog();
                }
                return true;
            case SETTINGS_IMPORT_BROWSER:
                new ImportBookmarksTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

        if (mFileList == null) {
            mFileNameList = new String[0];
            mFileList = new File[0];
        } else {
            Arrays.sort(mFileList, new SortName());
            mFileNameList = new String[mFileList.length];
        }
        for (int n = 0; n < mFileList.length; n++) {
            mFileNameList[n] = mFileList[n].getName();
        }
    }

    private static class SortName implements Comparator<File> {

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
