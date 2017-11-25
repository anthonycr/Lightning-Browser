/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment

import acr.browser.lightning.BrowserApp
import acr.browser.lightning.R
import acr.browser.lightning.database.HistoryItem
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.utils.SubscriptionUtils
import acr.browser.lightning.utils.Utils
import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.util.Log
import com.anthonycr.bonsai.CompletableOnSubscribe
import com.anthonycr.bonsai.Schedulers
import com.anthonycr.bonsai.SingleOnSubscribe
import com.anthonycr.bonsai.Subscription
import com.anthonycr.grant.PermissionsManager
import com.anthonycr.grant.PermissionsResultAction
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class BookmarkSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var bookmarkRepository: BookmarkRepository
    @Inject internal lateinit var application: Application
    @Inject @field:Named("database") internal lateinit var databaseScheduler: Scheduler

    private var importSubscription: Subscription? = null
    private var exportSubscription: Subscription? = null

    override fun providePreferencesResource() = R.xml.preference_bookmarks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserApp.appComponent.inject(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            PermissionsManager
                    .getInstance()
                    .requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS, null)
        }

        clickablePreference(preference = SETTINGS_EXPORT, onClick = this::exportBookmarks)
        clickablePreference(preference = SETTINGS_IMPORT, onClick = this::importBookmarks)
        clickablePreference(preference = SETTINGS_DELETE_BOOKMARKS, onClick = this::deleteAllBookmarks)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        exportSubscription?.unsubscribe()
        importSubscription?.unsubscribe()
    }

    override fun onDestroy() {
        super.onDestroy()

        exportSubscription?.unsubscribe()
        importSubscription?.unsubscribe()
    }

    private fun exportBookmarks() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS,
                object : PermissionsResultAction() {
                    override fun onGranted() {
                        bookmarkRepository.getAllBookmarks()
                                .subscribeOn(databaseScheduler)
                                .subscribe(Consumer { list ->
                                    if (!isAdded) {
                                        return@Consumer
                                    }

                                    val exportFile = BookmarkExporter.createNewExportFile()
                                    SubscriptionUtils.safeUnsubscribe(exportSubscription)
                                    exportSubscription = BookmarkExporter.exportBookmarksToFile(list, exportFile)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(Schedulers.main())
                                            .subscribe(object : CompletableOnSubscribe() {
                                                override fun onComplete() {
                                                    exportSubscription = null

                                                    activity?.let {
                                                        Utils.showSnackbar(it, "${it.getString(R.string.bookmark_export_path)} ${exportFile.path}")
                                                    }
                                                }

                                                override fun onError(throwable: Throwable) {
                                                    exportSubscription = null

                                                    Log.e(TAG, "onError: exporting bookmarks", throwable)
                                                    val activity = activity
                                                    if (activity != null && !activity.isFinishing && isAdded) {
                                                        Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure)
                                                    } else {
                                                        Utils.showToast(application, R.string.bookmark_export_failure)
                                                    }
                                                }
                                            })
                                })
                    }

                    override fun onDenied(permission: String) {
                        val activity = activity
                        if (activity != null && !activity.isFinishing && isAdded) {
                            Utils.createInformativeDialog(activity, R.string.title_error, R.string.bookmark_export_failure)
                        } else {
                            Utils.showToast(application, R.string.bookmark_export_failure)
                        }
                    }
                })
    }

    private fun importBookmarks() {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(activity, REQUIRED_PERMISSIONS,
                object : PermissionsResultAction() {
                    override fun onGranted() {
                        showImportBookmarkDialog(null)
                    }

                    override fun onDenied(permission: String) {
                        //TODO Show message
                    }
                })
    }

    private fun deleteAllBookmarks() {
        showDeleteBookmarksDialog()
    }

    private fun showDeleteBookmarksDialog() {
        BrowserDialog.showPositiveNegativeDialog(
                activity = activity,
                title = R.string.action_delete,
                message = R.string.action_delete_all_bookmarks,
                positiveButton = DialogItem(R.string.yes) {
                    bookmarkRepository
                            .deleteAllBookmarks()
                            .subscribeOn(databaseScheduler)
                            .subscribe()
                },
                negativeButton = DialogItem(R.string.no) {},
                onCancel = {}
        )
    }

    private fun loadFileList(path: File?): Array<File> {
        val file: File = path ?: File(Environment.getExternalStorageDirectory().toString())

        try {
            file.mkdirs()
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to make directory", e)
        }

        return (if (file.exists()) {
            file.listFiles()
        } else {
            arrayOf()
        }).apply {
            sortWith(SortName())
        }
    }

    private class SortName : Comparator<File> {

        override fun compare(a: File, b: File): Int {
            return if (a.isDirectory && b.isDirectory) {
                a.name.compareTo(b.name)
            } else if (a.isDirectory) {
                -1
            } else if (b.isDirectory) {
                1
            } else if (a.isFile && b.isFile) {
                a.name.compareTo(b.name)
            } else {
                1
            }
        }
    }

    private fun showImportBookmarkDialog(path: File?) {
        val builder = AlertDialog.Builder(activity)

        val title = getString(R.string.title_chooser)
        builder.setTitle(title + ": " + Environment.getExternalStorageDirectory())

        val fileList = loadFileList(path)
        val fileNames = fileList.map { it.name }.toTypedArray()

        builder.setItems(fileNames) { _, which ->
            if (fileList[which].isDirectory) {
                showImportBookmarkDialog(fileList[which])
            } else {
                SubscriptionUtils.safeUnsubscribe(importSubscription)
                importSubscription = BookmarkExporter.importBookmarksFromFile(fileList[which])
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(object : SingleOnSubscribe<List<HistoryItem>>() {
                            override fun onItem(importList: List<HistoryItem>?) {
                                importSubscription = null

                                val importedBookmarks = requireNotNull(importList)
                                bookmarkRepository.addBookmarkList(importedBookmarks)
                                        .subscribeOn(databaseScheduler)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            activity?.let {
                                                val message = it.getString(R.string.message_import)
                                                Utils.showSnackbar(it, importedBookmarks.size.toString() + " " + message)
                                            }
                                        }
                            }

                            override fun onError(throwable: Throwable) {
                                importSubscription = null

                                Log.e(TAG, "onError: importing bookmarks", throwable)
                                val activity = activity
                                if (activity != null && !activity.isFinishing && isAdded) {
                                    Utils.createInformativeDialog(activity, R.string.title_error, R.string.import_bookmark_error)
                                } else {
                                    Utils.showToast(application, R.string.import_bookmark_error)
                                }
                            }
                        })
            }
        }
        val dialog = builder.show()
        BrowserDialog.setDialogSize(activity, dialog)
    }

    companion object {

        private const val TAG = "BookmarkSettingsFrag"

        private const val SETTINGS_EXPORT = "export_bookmark"
        private const val SETTINGS_IMPORT = "import_bookmark"
        private const val SETTINGS_DELETE_BOOKMARKS = "delete_bookmarks"

        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
