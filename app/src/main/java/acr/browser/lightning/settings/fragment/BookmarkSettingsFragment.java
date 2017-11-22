/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.anthonycr.bonsai.CompletableOnSubscribe;
import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.Subscription;
import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkExporter;
import acr.browser.lightning.database.bookmark.BookmarkRepository;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.SubscriptionUtils;
import acr.browser.lightning.utils.Utils;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class BookmarkSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final String TAG = "BookmarkSettingsFrag";

    private static final String SETTINGS_EXPORT = "export_bookmark";
    private static final String SETTINGS_IMPORT = "import_bookmark";
    private static final String SETTINGS_DELETE_BOOKMARKS = "delete_bookmarks";

    @Nullable private Activity mActivity;

    @Inject BookmarkRepository mBookmarkManager;
    @Inject Application mApplication;
    @Inject @Named("database") Scheduler databaseScheduler;

    private File[] mFileList;
    private String[] mFileNameList;

    @Nullable private Subscription mImportSubscription;
    @Nullable private Subscription mExportSubscription;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final File mPath = new File(Environment.getExternalStorageDirectory().toString());

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
                                .subscribeOn(databaseScheduler)
                                .subscribe(new Consumer<List<HistoryItem>>() {
                                    @Override
                                    public void accept(List<HistoryItem> list) throws Exception {
                                        if (!isAdded()) {
                                            return;
                                        }

                                        final File exportFile = BookmarkExporter.createNewExportFile();
                                        SubscriptionUtils.safeUnsubscribe(mExportSubscription);
                                        mExportSubscription = BookmarkExporter.exportBookmarksToFile(list, exportFile)
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
                mBookmarkManager.deleteAllBookmarks().subscribeOn(databaseScheduler).subscribe();
            }
        });
        Dialog dialog = builder.show();
        BrowserDialog.setDialogSize(activity, dialog);
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
            Log.e(TAG, "Unable to make directory", e);
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
                                    .subscribeOn(databaseScheduler)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Action() {
                                        @Override
                                        public void run() throws Exception {
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
