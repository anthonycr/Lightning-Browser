package acr.browser.lightning.dialog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.squareup.otto.Bus;

import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BookmarkEvents;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;

/**
 * TODO Rename this class it doesn't build dialogs only for bookmarks
 * <p/>
 * Created by Stefano Pacifici on 02/09/15, based on Anthony C. Restaino's code.
 */
public class LightningDialogBuilder {

    @Inject BookmarkManager mBookmarkManager;
    @Inject PreferenceManager mPreferenceManager;
    @Inject HistoryDatabase mHistoryDatabase;
    @Inject Bus mEventBus;

    @Inject
    public LightningDialogBuilder() {
        BrowserApp.getAppComponent().inject(this);
    }

    /**
     * Show the appropriated dialog for the long pressed link. It means that we try to understand
     * if the link is relative to a bookmark or is just a folder.
     *
     * @param context used to show the dialog
     * @param url     the long pressed url
     */
    public void showLongPressedDialogForBookmarkUrl(@NonNull final Context context, @NonNull final String url) {
        final HistoryItem item;
        if (url.startsWith(Constants.FILE) && url.endsWith(BookmarkPage.FILENAME)) {
            // TODO hacky, make a better bookmark mechanism in the future
            final Uri uri = Uri.parse(url);
            final String filename = uri.getLastPathSegment();
            final String folderTitle = filename.substring(0, filename.length() - BookmarkPage.FILENAME.length() - 1);
            item = new HistoryItem();
            item.setIsFolder(true);
            item.setTitle(folderTitle);
            item.setImageId(R.drawable.ic_folder);
            item.setUrl(Constants.FOLDER + folderTitle);
        } else {
            item = mBookmarkManager.findBookmarkForUrl(url);
        }
        if (item != null) {
            if (item.isFolder()) {
                showBookmarkFolderLongPressedDialog(context, item);
            } else {
                showLongPressedDialogForBookmarkUrl(context, item);
            }
        }
    }

    public void showLongPressedDialogForBookmarkUrl(@NonNull final Context context, @NonNull final HistoryItem item) {
        final DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                mEventBus.post(new BrowserEvents.OpenUrlInNewTab(item.getUrl()));
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                if (mBookmarkManager.deleteBookmark(item)) {
                                    mEventBus.post(new BookmarkEvents.Deleted(item));
                                }
                                break;
                            case DialogInterface.BUTTON_NEUTRAL:
                                showEditBookmarkDialog(context, item);
                                break;
                        }
                    }
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_bookmarks)
                .setMessage(R.string.dialog_bookmark)
                .setCancelable(true)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .setNeutralButton(R.string.action_edit, dialogClickListener)
                .show();
    }

    private void showEditBookmarkDialog(@NonNull final Context context, @NonNull final HistoryItem item) {
        final AlertDialog.Builder editBookmarkDialog = new AlertDialog.Builder(context);
        editBookmarkDialog.setTitle(R.string.title_edit_bookmark);
        final View dialogLayout = View.inflate(context, R.layout.dialog_edit_bookmark, null);
        final EditText getTitle = (EditText) dialogLayout.findViewById(R.id.bookmark_title);
        getTitle.setText(item.getTitle());
        final EditText getUrl = (EditText) dialogLayout.findViewById(R.id.bookmark_url);
        getUrl.setText(item.getUrl());
        final AutoCompleteTextView getFolder =
                (AutoCompleteTextView) dialogLayout.findViewById(R.id.bookmark_folder);
        getFolder.setHint(R.string.folder);
        getFolder.setText(item.getFolder());
        final List<String> folders = mBookmarkManager.getFolderTitles();
        final ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, folders);
        getFolder.setThreshold(1);
        getFolder.setAdapter(suggestionsAdapter);
        editBookmarkDialog.setView(dialogLayout);
        editBookmarkDialog.setPositiveButton(context.getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HistoryItem editedItem = new HistoryItem();
                        editedItem.setTitle(getTitle.getText().toString());
                        editedItem.setUrl(getUrl.getText().toString());
                        editedItem.setUrl(getUrl.getText().toString());
                        editedItem.setFolder(getFolder.getText().toString());
                        mBookmarkManager.editBookmark(item, editedItem);
                        mEventBus.post(new BookmarkEvents.BookmarkChanged(item, editedItem));
                    }
                });
        editBookmarkDialog.show();
    }

    public void showBookmarkFolderLongPressedDialog(@NonNull final Context context, @NonNull final HistoryItem item) {
        // assert item.isFolder();
        final DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                showRenameFolderDialog(context, item);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                mBookmarkManager.deleteFolder(item.getTitle());
                                // setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
                                mEventBus.post(new BookmarkEvents.Deleted(item));
                                break;
                        }
                    }
                };

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_folder)
                .setMessage(R.string.dialog_folder)
                .setCancelable(true)
                .setPositiveButton(R.string.action_rename, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .show();
    }

    private void showRenameFolderDialog(@NonNull final Context context, @NonNull final HistoryItem item) {
        // assert item.isFolder();
        final AlertDialog.Builder editFolderDialog = new AlertDialog.Builder(context);
        editFolderDialog.setTitle(R.string.title_rename_folder);
        final EditText getTitle = new EditText(context);
        getTitle.setHint(R.string.hint_title);
        getTitle.setText(item.getTitle());
        getTitle.setSingleLine();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(10);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(getTitle);
        editFolderDialog.setView(layout);
        editFolderDialog.setPositiveButton(context.getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String oldTitle = item.getTitle();
                        final String newTitle = getTitle.getText().toString();
                        final HistoryItem editedItem = new HistoryItem();
                        editedItem.setTitle(newTitle);
                        editedItem.setUrl(Constants.FOLDER + newTitle);
                        editedItem.setFolder(item.getFolder());
                        editedItem.setIsFolder(true);
                        mBookmarkManager.renameFolder(oldTitle, newTitle);
                        mEventBus.post(new BookmarkEvents.BookmarkChanged(item, editedItem));
                    }
                });
        editFolderDialog.show();
    }

    public void showLongPressedHistoryLinkDialog(final Context context, @NonNull final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mEventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        BrowserApp.getIOThread().execute(new Runnable() {
                            @Override
                            public void run() {
                                mHistoryDatabase.deleteHistoryItem(url);
                                // openHistory();
                                mEventBus.post(new BrowserEvents.OpenHistoryInCurrentTab());
                            }
                        });
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                        break;
                    default:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_history)
                .setMessage(R.string.dialog_history_long_press)
                .setCancelable(true)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_delete, dialogClickListener)
                .setNeutralButton(R.string.action_open, dialogClickListener)
                .show();
    }

    // TODO There should be a way in which we do not need an activity reference to dowload a file
    public void showLongPressImageDialog(@NonNull final Activity activity, @NonNull final String url,
                                         @NonNull final String userAgent) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mEventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        Utils.downloadFile(activity, mPreferenceManager, url, userAgent, "attachment");
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(url.replace(Constants.HTTP, ""))
                .setCancelable(true)
                .setMessage(R.string.dialog_image)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_open, dialogClickListener)
                .setNeutralButton(R.string.action_download, dialogClickListener)
                .show();
    }

    public void showLongPressLinkDialog(@NonNull final Context context, final String url) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mEventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                        break;

                    case DialogInterface.BUTTON_NEUTRAL:
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("label", url);
                        clipboard.setPrimaryClip(clip);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context); // dialog
        builder.setTitle(url)
                .setCancelable(true)
                .setMessage(R.string.dialog_link)
                .setPositiveButton(R.string.action_new_tab, dialogClickListener)
                .setNegativeButton(R.string.action_open, dialogClickListener)
                .setNeutralButton(R.string.action_copy, dialogClickListener)
                .show();
    }

}
