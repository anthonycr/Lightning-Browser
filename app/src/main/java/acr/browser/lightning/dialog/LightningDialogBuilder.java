package acr.browser.lightning.dialog;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.MenuItem;
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
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryDatabase;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;

/**
 * TODO Rename this class it doesn't build dialogs only for bookmarks
 *
 * Created by Stefano Pacifici on 02/09/15, based on Anthony C. Restaino's code.
 */
public class LightningDialogBuilder {

    @Inject
    BookmarkManager bookmarkManager;

    @Inject
    HistoryDatabase mHistoryDatabase;

    @Inject
    Bus eventBus;

    @Inject
    public LightningDialogBuilder() {
        BrowserApp.getAppComponent().inject(this);
    }

    /**
     * Show the appropriated dialog for the long pressed link. It means that we try to understand
     * if the link is relative to a bookmark or is just a folder.
     * @param context   used to show the dialog
     * @param url   the long pressed url
     */
    public void showLongPressedDialogForBookmarkUrl(final Context context, final String url) {
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
            item = bookmarkManager.findBookmarkForUrl(url);
        }
        if (item != null) {
            if (item.isFolder()) {
                showBookmarkFolderLongPressedDialog(context, item);
            } else {
                showLongPressedDialogForBookmarkUrl(context, item);
            }
        }
    }

    public void showLongPressedDialogForBookmarkUrl(final Context context, final HistoryItem item) {
        final DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                eventBus.post(new BrowserEvents.OpenUrlInNewTab(item.getUrl()));
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                if (bookmarkManager.deleteBookmark(item)) {
                                    eventBus.post(new BookmarkEvents.Deleted(item));
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

    private void showEditBookmarkDialog(final Context context, final HistoryItem item) {
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
        final List<String> folders = bookmarkManager.getFolderTitles();
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
                        bookmarkManager.editBookmark(item, editedItem);
                        eventBus.post(new BookmarkEvents.BookmarkChanged(item, editedItem));
                    }
                });
        editBookmarkDialog.show();
    }

    public void showBookmarkFolderLongPressedDialog(final Context context, final HistoryItem item) {
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
                                bookmarkManager.deleteFolder(item.getTitle());
                                // setBookmarkDataSet(mBookmarkManager.getBookmarksFromFolder(null, true), false);
                                eventBus.post(new BookmarkEvents.Deleted(item));
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

    private void showRenameFolderDialog(final Context context, final HistoryItem item) {
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
                        bookmarkManager.renameFolder(oldTitle, newTitle);
                        eventBus.post(new BookmarkEvents.BookmarkChanged(item, editedItem));
                    }
                });
        editFolderDialog.show();
    }

    public void buildLongPressedHistoryLinkDialog(final Context context, final String url, ContextMenu menu) {
        MenuItem item = menu.add(0, R.string.action_open, 0, R.string.action_open);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                return true;
            }
        });

        item = menu.add(0, R.string.action_new_tab, 0, R.string.action_new_tab);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                return true;
            }
        });

        item = menu.add(0, R.string.action_delete, 0, R.string.action_delete);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mHistoryDatabase.deleteHistoryItem(url);
                // openHistory();
                eventBus.post(new BrowserEvents.OpenUrlInCurrentTab(HistoryPage.getHistoryPage(context)));
                return true;
            }
        });
    }


    // TODO There should be a way in which we do not need an activity reference to dowload a file
    public void buildLongPressImageMenu(@NonNull final Activity activity, @NonNull final String url,
                                        @NonNull final String userAgent, final ContextMenu menu) {

        MenuItem item = menu.add(0, R.string.action_open, 0, R.string.action_open);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                return true;
            }
        });

        item = menu.add(0, R.string.action_new_tab, 0, R.string.action_new_tab);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                return true;
            }
        });

        item = menu.add(0, R.string.action_download, 0, R.string.action_download);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Utils.downloadFile(activity, url,
                        userAgent, "attachment");
                return true;
            }
        });
    }

    public void buildLongPressLinkMenu(final Context context, final String url, final ContextMenu menu) {
        MenuItem item = menu.add(0, R.string.action_open, 0, R.string.action_open);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInCurrentTab(url));
                return true;
            }
        });

        item = menu.add(0, R.string.action_new_tab, 0, R.string.action_new_tab);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInNewTab(url));
                return true;
            }
        });

        item = menu.add(0, R.string.action_new_background_tab, 0, R.string.action_new_background_tab);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                eventBus.post(new BrowserEvents.OpenUrlInNewTab(url, false));
                return true;
            }
        });


        item = menu.add(0, R.string.action_copy, 0, R.string.action_copy);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", url);
                clipboard.setPrimaryClip(clip);
                return true;
            }
        });

        item = menu.add(0, R.string.action_share, 0, R.string.action_share);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

                shareIntent.putExtra(Intent.EXTRA_TEXT, url);

                context.startActivity(shareIntent);

                return true;
            }
        });

    }

}
