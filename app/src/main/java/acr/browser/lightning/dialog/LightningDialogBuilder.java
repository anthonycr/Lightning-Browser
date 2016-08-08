package acr.browser.lightning.dialog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

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
    public void showLongPressedDialogForBookmarkUrl(@NonNull final Activity context, @NonNull final String url) {
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

    public void showLongPressedDialogForBookmarkUrl(@NonNull final Activity context, @NonNull final HistoryItem item) {
        BrowserDialog.show(context, R.string.action_bookmarks,
            new BrowserDialog.Item(R.string.action_new_tab) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInNewTab(item.getUrl()));
                }
            },
            new BrowserDialog.Item(R.string.action_delete) {
                @Override
                public void onClick() {
                    if (mBookmarkManager.deleteBookmark(item)) {
                        mEventBus.post(new BookmarkEvents.Deleted(item));
                    }
                }
            },
            new BrowserDialog.Item(R.string.action_edit) {
                @Override
                public void onClick() {
                    showEditBookmarkDialog(context, item);
                }
            });
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

    public void showBookmarkFolderLongPressedDialog(@NonNull final Activity context, @NonNull final HistoryItem item) {

        BrowserDialog.show(context, R.string.action_folder,
            new BrowserDialog.Item(R.string.action_rename) {
                @Override
                public void onClick() {
                    showRenameFolderDialog(context, item);
                }
            },
            new BrowserDialog.Item(R.string.action_delete) {
                @Override
                public void onClick() {
                    mBookmarkManager.deleteFolder(item.getTitle());
                    mEventBus.post(new BookmarkEvents.Deleted(item));
                }
            });
    }

    private void showRenameFolderDialog(@NonNull final Activity context, @NonNull final HistoryItem item) {
        BrowserDialog.showEditText(context, R.string.title_rename_folder,
            R.string.hint_title, item.getTitle(),
            R.string.action_ok, new BrowserDialog.EditorListener() {
                @Override
                public void onClick(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        final String oldTitle = item.getTitle();
                        final HistoryItem editedItem = new HistoryItem();
                        editedItem.setTitle(text);
                        editedItem.setUrl(Constants.FOLDER + text);
                        editedItem.setFolder(item.getFolder());
                        editedItem.setIsFolder(true);
                        mBookmarkManager.renameFolder(oldTitle, text);
                        mEventBus.post(new BookmarkEvents.BookmarkChanged(item, editedItem));
                    }
                }
            });
    }

    public void showLongPressedHistoryLinkDialog(final Activity context, @NonNull final String url) {
        BrowserDialog.show(context, R.string.action_history,
            new BrowserDialog.Item(R.string.action_new_tab) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                }
            },
            new BrowserDialog.Item(R.string.action_delete) {
                @Override
                public void onClick() {
                    BrowserApp.getIOThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            mHistoryDatabase.deleteHistoryItem(url);
                            // openHistory();
                            mEventBus.post(new BrowserEvents.OpenHistoryInCurrentTab());
                        }
                    });
                }
            },
            new BrowserDialog.Item(R.string.action_open) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                }
            });
    }

    // TODO There should be a way in which we do not need an activity reference to dowload a file
    public void showLongPressImageDialog(@NonNull final Activity activity, @NonNull final String url,
                                         @NonNull final String userAgent) {
        BrowserDialog.show(activity, url.replace(Constants.HTTP, ""),
            new BrowserDialog.Item(R.string.action_new_tab) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                }
            },
            new BrowserDialog.Item(R.string.action_open) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                }
            },
            new BrowserDialog.Item(R.string.action_download) {
                @Override
                public void onClick() {
                    Utils.downloadFile(activity, mPreferenceManager, url, userAgent, "attachment");
                }
            });
    }

    public void showLongPressLinkDialog(@NonNull final Activity context, final String url) {
        BrowserDialog.show(context, url,
            new BrowserDialog.Item(R.string.action_new_tab) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                }
            },
            new BrowserDialog.Item(R.string.action_open) {
                @Override
                public void onClick() {
                    mEventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                }
            },
            new BrowserDialog.Item(R.string.action_copy) {
                @Override
                public void onClick() {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", url);
                    clipboard.setPrimaryClip(clip);
                }
            });
    }

}
