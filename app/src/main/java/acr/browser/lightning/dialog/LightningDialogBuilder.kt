package acr.browser.lightning.dialog

import acr.browser.lightning.R
import acr.browser.lightning._browser2.BrowserContract
import acr.browser.lightning._browser2.DefaultBrowserActivity
import acr.browser.lightning.databinding.DialogEditBookmarkBinding
import acr.browser.lightning.extensions.resizeAndShow
import android.app.Activity
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import dagger.Reusable
import javax.inject.Inject

/**
 * A builder of various dialogs.
 */
@Reusable
class LightningDialogBuilder @Inject constructor() {

    /**
     * Show the appropriated dialog for the long pressed link. It means that we try to understand
     * if the link is relative to a bookmark or is just a folder.
     *
     * @param activity used to show the dialog
     * @param url      the long pressed url
     */
    fun showLongPressedDialogForBookmarkUrl(
        activity: Activity,
        onClick: (BrowserContract.BookmarkOptionEvent) -> Unit
    ) = BrowserDialog.show(activity, R.string.action_bookmarks,
        DialogItem(title = R.string.dialog_open_new_tab) {
            onClick(BrowserContract.BookmarkOptionEvent.NEW_TAB)
        },
        DialogItem(title = R.string.dialog_open_background_tab) {
            onClick(BrowserContract.BookmarkOptionEvent.BACKGROUND_TAB)
        },
        DialogItem(
            title = R.string.dialog_open_incognito_tab,
            isConditionMet = activity is DefaultBrowserActivity
        ) {
            onClick(BrowserContract.BookmarkOptionEvent.INCOGNITO_TAB)
        },
        DialogItem(title = R.string.action_share) {
            onClick(BrowserContract.BookmarkOptionEvent.SHARE)
        },
        DialogItem(title = R.string.dialog_copy_link) {
            onClick(BrowserContract.BookmarkOptionEvent.COPY_LINK)
        },
        DialogItem(title = R.string.dialog_remove_bookmark) {
            onClick(BrowserContract.BookmarkOptionEvent.REMOVE)
        },
        DialogItem(title = R.string.dialog_edit_bookmark) {
            onClick(BrowserContract.BookmarkOptionEvent.EDIT)
        })

    /**
     * Show the appropriated dialog for the long pressed link.
     *
     * @param activity used to show the dialog
     * @param url      the long pressed url
     */
    // TODO allow individual downloads to be deleted.
    fun showLongPressedDialogForDownloadUrl(
        activity: Activity,
        onClick: (BrowserContract.DownloadOptionEvent) -> Unit
    ) = BrowserDialog.show(activity, R.string.action_downloads,
        DialogItem(title = R.string.dialog_delete_all_downloads) {
            onClick(BrowserContract.DownloadOptionEvent.DELETE_ALL)
        },
        DialogItem(title = R.string.dialog_delete_all_downloads) {
            onClick(BrowserContract.DownloadOptionEvent.DELETE)
        }
    )

    /**
     * Show the add bookmark dialog. Shows a dialog with the title and URL pre-populated.
     */
    fun showAddBookmarkDialog(
        activity: Activity,
        currentTitle: String,
        currentUrl: String,
        folders: List<String>,
        onSave: (title: String, url: String, folder: String) -> Unit
    ) {
        val editBookmarkDialog = AlertDialog.Builder(activity)
        editBookmarkDialog.setTitle(R.string.action_add_bookmark)
        val dialogLayout = View.inflate(activity, R.layout.dialog_edit_bookmark, null)
        val binding = DialogEditBookmarkBinding.bind(dialogLayout)
        binding.bookmarkTitle.setText(currentTitle)
        binding.bookmarkUrl.setText(currentUrl)
        binding.bookmarkFolder.setText("")

        val suggestionsAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_dropdown_item_1line, folders
        )
        binding.bookmarkFolder.setAdapter(suggestionsAdapter)
        editBookmarkDialog.setView(dialogLayout)
        editBookmarkDialog.setPositiveButton(activity.getString(R.string.action_ok)) { _, _ ->
            onSave(
                binding.bookmarkTitle.text.toString(),
                binding.bookmarkUrl.text.toString(),
                binding.bookmarkFolder.text.toString()
            )
        }
        editBookmarkDialog.setNegativeButton(R.string.action_cancel) { _, _ -> }
        editBookmarkDialog.resizeAndShow()
    }

    fun showEditBookmarkDialog(
        activity: Activity,
        currentTitle: String,
        currentUrl: String,
        currentFolder: String,
        folders: List<String>,
        onSave: (title: String, url: String, folder: String) -> Unit
    ) {
        val editBookmarkDialog = AlertDialog.Builder(activity)
        editBookmarkDialog.setTitle(R.string.action_add_bookmark)
        val dialogLayout = View.inflate(activity, R.layout.dialog_edit_bookmark, null)
        val binding = DialogEditBookmarkBinding.bind(dialogLayout)
        binding.bookmarkTitle.setText(currentTitle)
        binding.bookmarkUrl.setText(currentUrl)
        binding.bookmarkFolder.setText(currentFolder)

        val suggestionsAdapter = ArrayAdapter(
            activity,
            android.R.layout.simple_dropdown_item_1line, folders
        )
        binding.bookmarkFolder.setAdapter(suggestionsAdapter)
        editBookmarkDialog.setView(dialogLayout)
        editBookmarkDialog.setPositiveButton(activity.getString(R.string.action_ok)) { _, _ ->
            onSave(
                binding.bookmarkTitle.text.toString(),
                binding.bookmarkUrl.text.toString(),
                binding.bookmarkFolder.text.toString()
            )
        }
        editBookmarkDialog.resizeAndShow()
    }

    fun showBookmarkFolderLongPressedDialog(
        activity: Activity,
        onClick: (BrowserContract.FolderOptionEvent) -> Unit
    ) = BrowserDialog.show(activity, R.string.action_folder,
        DialogItem(title = R.string.dialog_rename_folder) {
            onClick(BrowserContract.FolderOptionEvent.RENAME)
        },
        DialogItem(title = R.string.dialog_remove_folder) {
            onClick(BrowserContract.FolderOptionEvent.REMOVE)
        })

    fun showRenameFolderDialog(
        activity: Activity,
        oldTitle: String,
        onSave: (oldTitle: String, newTitle: String) -> Unit
    ) = BrowserDialog.showEditText(
        activity,
        R.string.title_rename_folder,
        R.string.hint_title,
        oldTitle,
        R.string.action_ok
    ) { text ->
        if (text.isNotBlank()) {
            onSave(oldTitle, text)
        }
    }

    fun showLongPressedHistoryLinkDialog(
        activity: Activity,
        onClick: (BrowserContract.HistoryOptionEvent) -> Unit
    ) = BrowserDialog.show(activity, R.string.action_history,
        DialogItem(title = R.string.dialog_open_new_tab) {
            onClick(BrowserContract.HistoryOptionEvent.NEW_TAB)
        },
        DialogItem(title = R.string.dialog_open_background_tab) {
            onClick(BrowserContract.HistoryOptionEvent.BACKGROUND_TAB)
        },
        DialogItem(
            title = R.string.dialog_open_incognito_tab,
            isConditionMet = activity is DefaultBrowserActivity
        ) {
            onClick(BrowserContract.HistoryOptionEvent.INCOGNITO_TAB)
        },
        DialogItem(title = R.string.action_share) {
            onClick(BrowserContract.HistoryOptionEvent.SHARE)
        },
        DialogItem(title = R.string.dialog_copy_link) {
            onClick(BrowserContract.HistoryOptionEvent.COPY_LINK)
        },
        DialogItem(title = R.string.dialog_remove_from_history) {
            onClick(BrowserContract.HistoryOptionEvent.REMOVE)
        })
}
