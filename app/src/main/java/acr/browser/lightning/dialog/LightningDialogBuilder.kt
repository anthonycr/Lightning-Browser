package acr.browser.lightning.dialog

import acr.browser.lightning.R
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
        editBookmarkDialog.setTitle(R.string.dialog_edit_bookmark)
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
}
