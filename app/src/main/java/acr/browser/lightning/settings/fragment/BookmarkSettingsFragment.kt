/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.settings.fragment

import acr.browser.lightning.R
import acr.browser.lightning.bookmark.LegacyBookmarkImporter
import acr.browser.lightning.bookmark.NetscapeBookmarkFormatImporter
import acr.browser.lightning.browser.di.injector
import acr.browser.lightning.concurrency.CoroutineDispatchers
import acr.browser.lightning.database.bookmark.BookmarkExporter
import acr.browser.lightning.database.bookmark.BookmarkRepository
import acr.browser.lightning.dialog.BrowserDialog
import acr.browser.lightning.dialog.DialogItem
import acr.browser.lightning.extensions.fileInputStream
import acr.browser.lightning.extensions.fileName
import acr.browser.lightning.extensions.fileOutputStream
import acr.browser.lightning.extensions.snackbar
import acr.browser.lightning.extensions.toast
import acr.browser.lightning.log.Logger
import acr.browser.lightning.utils.Utils
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class BookmarkSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var bookmarkRepository: BookmarkRepository
    @Inject internal lateinit var application: Application
    @Inject internal lateinit var netscapeBookmarkFormatImporter: NetscapeBookmarkFormatImporter
    @Inject internal lateinit var legacyBookmarkImporter: LegacyBookmarkImporter
    @Inject internal lateinit var logger: Logger
    @Inject internal lateinit var appCoroutineScope: CoroutineScope
    @Inject internal lateinit var coroutineDispatchers: CoroutineDispatchers

    override fun providePreferencesXmlResource() = R.xml.preference_bookmarks

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        injector.inject(this)

        clickablePreference(preference = SETTINGS_EXPORT, onClick = this::showBookmarkExportChooser)
        clickablePreference(preference = SETTINGS_IMPORT, onClick = this::showFileChooser)
        clickablePreference(
            preference = SETTINGS_DELETE_BOOKMARKS,
            onClick = this::deleteAllBookmarks
        )
    }

    private fun exportBookmarksToUri(uri: Uri) {
        appCoroutineScope.launch {
            val list = bookmarkRepository.getAllBookmarksSorted()
            if (!isAdded) {
                return@launch
            }

            val fileName = activity?.fileName(uri).orEmpty()
            val outputStream = activity?.fileOutputStream(uri, coroutineDispatchers.io)
                ?: return@launch showExportError()

            try {
                BookmarkExporter.exportBookmarksToOutputStream(
                    list,
                    outputStream,
                    coroutineDispatchers.io
                )
                activity?.apply {
                    snackbar("${getString(R.string.bookmark_export_path)} $fileName")
                }
            } catch (ioException: IOException) {
                logger.log(TAG, "onError: exporting bookmarks", ioException)
                showExportError()
            }
        }
    }

    private fun showExportError() {
        val activity = activity
        if (activity != null && !activity.isFinishing && isAdded) {
            Utils.createInformativeDialog(
                activity,
                R.string.title_error,
                R.string.bookmark_export_failure
            )
        } else {
            application.toast(R.string.bookmark_export_failure)
        }
    }

    private fun deleteAllBookmarks() {
        showDeleteBookmarksDialog()
    }

    private fun showDeleteBookmarksDialog() {
        BrowserDialog.showPositiveNegativeDialog(
            activity = requireActivity(),
            title = R.string.action_delete,
            message = R.string.action_delete_all_bookmarks,
            positiveButton = DialogItem(title = R.string.yes) {
                appCoroutineScope.launch {
                    bookmarkRepository.deleteAllBookmarks()
                }
            },
            negativeButton = DialogItem(title = R.string.no) {},
            onCancel = {}
        )
    }

    private fun showBookmarkExportChooser() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, BOOKMARK_EXPORT_FILE)
        }

        startActivityForResult(intent, EXPORT_FILE_REQUEST_CODE)
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = TEXT_MIME_TYPE
        }

        startActivityForResult(intent, IMPORT_FILE_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IMPORT_FILE_REQUEST_CODE,
            EXPORT_FILE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && requestCode == IMPORT_FILE_REQUEST_CODE) {
                    data?.data?.also(::importBookmarksFromUri)
                } else if (resultCode == Activity.RESULT_OK && requestCode == EXPORT_FILE_REQUEST_CODE) {
                    data?.data?.also(::exportBookmarksToUri)
                } else {
                    activity?.toast(R.string.action_message_canceled)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun importBookmarksFromUri(uri: Uri) {
        appCoroutineScope.launch(coroutineDispatchers.io) {
            try {
                val fileName = activity?.fileName(uri)
                val inputStream = activity?.fileInputStream(uri) ?: return@launch

                val bookmarks = if (fileName?.endsWith(EXTENSION_HTML) == true) {
                    netscapeBookmarkFormatImporter.importBookmarks(inputStream)
                } else {
                    legacyBookmarkImporter.importBookmarks(inputStream)
                }
                bookmarkRepository.addBookmarkList(bookmarks)
                withContext(coroutineDispatchers.main) {
                    activity?.apply {
                        snackbar("${bookmarks.size} ${getString(R.string.message_import)}")
                    }
                }
            } catch (ioException: IOException) {
                logger.log(TAG, "onError: importing bookmarks", ioException)
                showImportError()
            }
        }
    }

    private fun showImportError() {
        val activity = activity
        if (activity != null && !activity.isFinishing && isAdded) {
            Utils.createInformativeDialog(
                activity,
                R.string.title_error,
                R.string.import_bookmark_error
            )
        } else {
            application.toast(R.string.import_bookmark_error)
        }
    }

    companion object {

        private const val IMPORT_FILE_REQUEST_CODE = 100
        private const val EXPORT_FILE_REQUEST_CODE = 101
        private const val TEXT_MIME_TYPE = "text/*"

        private const val TAG = "BookmarkSettingsFrag"

        private const val EXTENSION_HTML = "html"

        private const val BOOKMARK_EXPORT_FILE = "ExportedBookmarks.txt"

        private const val SETTINGS_EXPORT = "export_bookmark"
        private const val SETTINGS_IMPORT = "import_bookmark"
        private const val SETTINGS_DELETE_BOOKMARKS = "delete_bookmarks"

    }
}
