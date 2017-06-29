/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.anthonycr.bonsai.CompletableOnSubscribe;
import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.Subscription;
import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkExporter;
import acr.browser.lightning.database.bookmark.BookmarkLocalSync;
import acr.browser.lightning.database.bookmark.BookmarkLocalSync.Source;
import acr.browser.lightning.database.bookmark.BookmarkModel;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.SubscriptionUtils;
import acr.browser.lightning.utils.Utils;

public class BookmarkSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String TAG = "BookmarkSettingsFrag";

    private static final String SETTINGS_EXPORT = "export_bookmark";
    private static final String SETTINGS_IMPORT = "import_bookmark";
    private static final String SETTINGS_IMPORT_BROWSER = "import_browser";
    private static final String SETTINGS_DELETE_BOOKMARKS = "delete_bookmarks";

    @Nullable private Activity mActivity;

    @Inject BookmarkModel mBookmarkManager;
    @Inject Application mApplication;

    private File[] mFileList;
    private String[] mFileNameList;

    @Nullable private BookmarkLocalSync mSync;

    @Nullable private Subscription mImportSubscription;
    @Nullable private Subscription mExportSubscription;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final File mPath = new File(Environment.getExternalStorageDirectory().toString());

    private class ImportBookmarksTask extends AsyncTask<Void, Void, Integer> {

        @NonNull private final WeakReference<Activity> mActivityReference;
        private final Source mSource;

        public ImportBookmarksTask(Activity activity, Source source) {
            mActivityReference = new WeakReference<>(activity);
            mSource = source;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            List<HistoryItem> list;
            Log.d(TAG, "Loading bookmarks from: " + mSource.name());
            switch (mSource) {
                case STOCK:
                    list = getSync().getBookmarksFromStockBrowser();
                    break;
                case CHROME_STABLE:
                    list = getSync().getBookmarksFromChrome();
                    break;
                case CHROME_BETA:
                    list = getSync().getBookmarksFromChromeBeta();
                    break;
                case CHROME_DEV:
                    list = getSync().getBookmarksFromChromeDev();
                    break;
                default:
                    list = new ArrayList<>(0);
                    break;
            }
            int count = 0;
            if (!list.isEmpty()) {
                mBookmarkManager.addBookmarkList(list);
                count = list.size();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer num) {
            super.onPostExecute(num);
            Activity activity = mActivityReference.get();
            if (activity != null) {
                int number = num;
                final String message = activity.getResources().getString(R.string.message_import);
                Utils.showSnackbar(activity, number + " " + message);
            }
        }
    }

    @NonNull
    private BookmarkLocalSync getSync() {
        Preconditions.checkNonNull(mActivity);
        if (mSync == null) {
            mSync = new BookmarkLocalSync(mActivity);
        }

        return mSync;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_bookmarks);

        mActivity = getActivity();
        mSync = new BookmarkLocalSync(mActivity);

        initPrefs();

        PermissionsManager permissionsManager = PermissionsManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            permissionsManager.requestPermissionsIfNecessaryForResult(getActivity(), REQUIRED_PERMISSIONS, null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        SubscriptionUtils.safeUnsubscribe(mExportSubscription);
        SubscriptionUtils.safeUnsubscribe(mImportSubscription);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SubscriptionUtils.safeUnsubscribe(mExportSubscription);
        SubscriptionUtils.safeUnsubscribe(mImportSubscription);

        mActivity = null;
    }

    private void initPrefs() {

        Preference exportPref = findPreference(SETTINGS_EXPORT);
        Preference importPref = findPreference(SETTINGS_IMPORT);
        Preference deletePref = findPreference(SETTINGS_DELETE_BOOKMARKS);

        exportPref.setOnPreferenceClickListener(this);
        importPref.setOnPreferenceClickListener(this);
        deletePref.setOnPreferenceClickListener(this);

        getSync().isBrowserImportSupported()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<Boolean>() {
                @Override
                public void onItem(@Nullable Boolean supported) {
                    Preconditions.checkNonNull(supported);
                    Preference importStock = findPreference(SETTINGS_IMPORT_BROWSER);
                    importStock.setEnabled(supported);
                    importStock.setOnPreferenceClickListener(BookmarkSettingsFragment.this);
                }
            });

    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_EXPORT:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(getActivity(), REQUIRED_PERMISSIONS,
                    new PermissionsResultAction() {
                        @Override
                        public void onGranted() {
                            mBookmarkManager.getAllBookmarks()
                                .subscribeOn(Schedulers.io())
                                .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                                    @Override
                                    public void onItem(@Nullable List<HistoryItem> item) {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        Preconditions.checkNonNull(item);
                                        final File exportFile = BookmarkExporter.createNewExportFile();
                                        SubscriptionUtils.safeUnsubscribe(mExportSubscription);
                                        mExportSubscription = BookmarkExporter.exportBookmarksToFile(item, exportFile)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(Schedulers.main())
                                            .subscribe(new CompletableOnSubscribe() {
                                                @Override
                                                public void onComplete() {
                                                    mExportSubscription = null;

                                                    Activity activity = getActivity();
                                                    if (activity != null) {
                                                        Utils.showSnackbar(activity, activity.getString(R.string.bookmark_export_path)
                                                            + ' ' + exportFile.getPath());
                                                    }
                                                }

                                                @Override
                                                public void onError(@NonNull Throwable throwable) {
                                                    mExportSubscription = null;

                                                    Log.e(TAG, "onError: exporting bookmarks", throwable);
                                                    Activity activity = getActivity();
                                                    if (activity != null && !activity.isFinishing() && isAdded()) {
                                                        Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure);
                                                    } else {
                                                        Utils.showToast(mApplication, R.string.bookmark_export_failure);
                                                    }
                                                }
                                            });
                                    }
                                });
                        }

                        @Override
                        public void onDenied(String permission) {
                            Activity activity = getActivity();
                            if (activity != null && !activity.isFinishing() && isAdded()) {
                                Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure);
                            } else {
                                Utils.showToast(mApplication, R.string.bookmark_export_failure);
                            }
                        }
                    });
                return true;
            case SETTINGS_IMPORT:
                PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(getActivity(), REQUIRED_PERMISSIONS,
                    new PermissionsResultAction() {
                        @Override
                        public void onGranted() {
                            loadFileList(null);
                            createDialog();
                        }

                        @Override
                        public void onDenied(String permission) {
                            //TODO Show message
                        }
                    });
                return true;
            case SETTINGS_IMPORT_BROWSER:
                getSync().getSupportedBrowsers().subscribeOn(Schedulers.worker())
                    .observeOn(Schedulers.main()).subscribe(new SingleOnSubscribe<List<Source>>() {
                    @Override
                    public void onItem(@Nullable List<Source> item) {
                        Activity activity = getActivity();
                        if (item == null || activity == null) {
                            return;
                        }
                        List<String> titles = buildTitleList(activity, item);
                        showChooserDialog(activity, titles);
                    }
                });
                return true;
            case SETTINGS_DELETE_BOOKMARKS:
                showDeleteBookmarksDialog();
                return true;
            default:
                return false;
        }
    }

    private void showDeleteBookmarksDialog() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.action_delete);
        builder.setMessage(R.string.action_delete_all_bookmarks);
        builder.setNegativeButton(R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mBookmarkManager.deleteAllBookmarks().subscribeOn(Schedulers.io()).subscribe();
            }
        });
        Dialog dialog = builder.show();
        BrowserDialog.setDialogSize(activity, dialog);
    }

    @NonNull
    private List<String> buildTitleList(@NonNull Activity activity, @NonNull List<Source> items) {
        List<String> titles = new ArrayList<>();
        String title;
        for (Source source : items) {
            switch (source) {
                case STOCK:
                    titles.add(getString(R.string.stock_browser));
                    break;
                case CHROME_STABLE:
                    title = getTitle(activity, "com.android.chrome");
                    if (title != null) {
                        titles.add(title);
                    }
                    break;
                case CHROME_BETA:
                    title = getTitle(activity, "com.chrome.beta");
                    if (title != null) {
                        titles.add(title);
                    }
                    break;
                case CHROME_DEV:
                    title = getTitle(activity, "com.chrome.beta");
                    if (title != null) {
                        titles.add(title);
                    }
                    break;
                default:
                    break;
            }
        }
        return titles;
    }

    private void showChooserDialog(final Activity activity, List<String> list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
            android.R.layout.simple_list_item_1);
        for (String title : list) {
            adapter.add(title);
        }
        builder.setTitle(R.string.supported_browsers_title);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = adapter.getItem(which);
                Preconditions.checkNonNull(title);
                Source source = null;
                if (title.equals(getString(R.string.stock_browser))) {
                    source = Source.STOCK;
                } else if (title.equals(getTitle(activity, "com.android.chrome"))) {
                    source = Source.CHROME_STABLE;
                } else if (title.equals(getTitle(activity, "com.android.beta"))) {
                    source = Source.CHROME_BETA;
                } else if (title.equals(getTitle(activity, "com.android.dev"))) {
                    source = Source.CHROME_DEV;
                }
                if (source != null) {
                    new ImportBookmarksTask(activity, source).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
        Dialog dialog = builder.show();
        BrowserDialog.setDialogSize(activity, dialog);
    }

    @Nullable
    private static String getTitle(@NonNull Activity activity, @NonNull String packageName) {
        PackageManager pm = activity.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            CharSequence title = pm.getApplicationLabel(info);
            if (title != null) {
                return title.toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadFileList(@Nullable File path) {
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
        public int compare(@NonNull File a, @NonNull File b) {
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
        if (mActivity == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        final String title = getString(R.string.title_chooser);
        builder.setTitle(title + ": " + Environment.getExternalStorageDirectory());
        if (mFileList == null) {
            Dialog dialog = builder.show();
            BrowserDialog.setDialogSize(mActivity, dialog);
            return;
        }
        builder.setItems(mFileNameList, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mFileList[which].isDirectory()) {
                    builder.setTitle(title + ": " + mFileList[which]);
                    loadFileList(mFileList[which]);
                    builder.setItems(mFileNameList, this);
                    Dialog dialog1 = builder.show();
                    BrowserDialog.setDialogSize(mActivity, dialog1);
                } else {
                    SubscriptionUtils.safeUnsubscribe(mImportSubscription);
                    mImportSubscription = BookmarkExporter.importBookmarksFromFile(mFileList[which])
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                            @Override
                            public void onItem(@Nullable final List<HistoryItem> importList) {
                                mImportSubscription = null;

                                Preconditions.checkNonNull(importList);
                                mBookmarkManager.addBookmarkList(importList)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.main())
                                    .subscribe(new CompletableOnSubscribe() {
                                        @Override
                                        public void onComplete() {
                                            Activity activity = getActivity();
                                            if (activity != null) {
                                                String message = activity.getString(R.string.message_import);
                                                Utils.showSnackbar(activity, importList.size() + " " + message);
                                            }
                                        }
                                    });
                            }

                            @Override
                            public void onError(@NonNull Throwable throwable) {
                                mImportSubscription = null;

                                Log.e(TAG, "onError: importing bookmarks", throwable);
                                Activity activity = getActivity();
                                if (activity != null && !activity.isFinishing() && isAdded()) {
                                    Utils.createInformativeDialog(activity, R.string.title_error, R.string.import_bookmark_error);
                                } else {
                                    Utils.showToast(mApplication, R.string.import_bookmark_error);
                                }
                            }
                        });
                }
            }

        });
        Dialog dialog = builder.show();
        BrowserDialog.setDialogSize(mActivity, dialog);
    }
}
