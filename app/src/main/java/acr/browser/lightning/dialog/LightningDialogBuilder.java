package acr.browser.lightning.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.anthonycr.bonsai.CompletableOnSubscribe;
import com.anthonycr.bonsai.Schedulers;
import com.anthonycr.bonsai.SingleOnSubscribe;

import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.MainActivity;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkModel;
import acr.browser.lightning.database.downloads.DownloadsModel;
import acr.browser.lightning.database.history.HistoryModel;
import acr.browser.lightning.download.DownloadHandler;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.IntentUtils;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.UrlUtils;

/**
 * TODO Rename this class it doesn't build dialogs only for bookmarks
 * <p/>
 * Created by Stefano Pacifici on 02/09/15, based on Anthony C. Restaino's code.
 */
public class LightningDialogBuilder {
    private static final String TAG = "LightningDialogBuilder";

    public enum NewTab {
        FOREGROUND,
        BACKGROUND,
        INCOGNITO
    }

    @Inject BookmarkModel mBookmarkManager;
    @Inject DownloadsModel mDownloadsModel;
    @Inject HistoryModel mHistoryModel;
    @Inject PreferenceManager mPreferenceManager;
    @Inject DownloadHandler mDownloadHandler;

    @Inject
    public LightningDialogBuilder() {
        BrowserApp.getAppComponent().inject(this);
    }

    /**
     * Show the appropriated dialog for the long pressed link. It means that we try to understand
     * if the link is relative to a bookmark or is just a folder.
     *
     * @param activity used to show the dialog
     * @param url      the long pressed url
     */
    public void showLongPressedDialogForBookmarkUrl(@NonNull final Activity activity,
                                                    @NonNull final UIController uiController,
                                                    @NonNull final String url) {
        final HistoryItem item;
        if (UrlUtils.isBookmarkUrl(url)) {
            // TODO hacky, make a better bookmark mechanism in the future
            final Uri uri = Uri.parse(url);
            final String filename = uri.getLastPathSegment();
            final String folderTitle = filename.substring(0, filename.length() - BookmarkPage.FILENAME.length() - 1);
            item = new HistoryItem();
            item.setIsFolder(true);
            item.setTitle(folderTitle);
            item.setImageId(R.drawable.ic_folder);
            item.setUrl(Constants.FOLDER + folderTitle);
            showBookmarkFolderLongPressedDialog(activity, uiController, item);
        } else {
            mBookmarkManager.findBookmarkForUrl(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.main())
                .subscribe(new SingleOnSubscribe<HistoryItem>() {
                    @Override
                    public void onItem(@Nullable HistoryItem historyItem) {
                        // TODO: 6/14/17 figure out solution to case where slashes get appended to root urls causing the item to be null
                        if (historyItem != null) {
                            showLongPressedDialogForBookmarkUrl(activity, uiController, historyItem);
                        }
                    }
                });
        }
    }

    public void showLongPressedDialogForBookmarkUrl(@NonNull final Activity activity,
                                                    @NonNull final UIController uiController,
                                                    @NonNull final HistoryItem item) {
        BrowserDialog.show(activity, R.string.action_bookmarks,
            new BrowserDialog.Item(R.string.dialog_open_new_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.FOREGROUND, item.getUrl());
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_background_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.BACKGROUND, item.getUrl());
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_incognito_tab, activity instanceof MainActivity) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.INCOGNITO, item.getUrl());
                }
            },
            new BrowserDialog.Item(R.string.action_share) {
                @Override
                public void onClick() {
                    new IntentUtils(activity).shareUrl(item.getUrl(), item.getTitle());
                }
            },
            new BrowserDialog.Item(R.string.dialog_copy_link) {
                @Override
                public void onClick() {
                    BrowserApp.copyToClipboard(activity, item.getUrl());
                }
            },
            new BrowserDialog.Item(R.string.dialog_remove_bookmark) {
                @Override
                public void onClick() {
                    mBookmarkManager.deleteBookmark(item)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(new SingleOnSubscribe<Boolean>() {
                            @Override
                            public void onItem(@Nullable Boolean success) {
                                Preconditions.checkNonNull(success);
                                if (success) {
                                    uiController.handleBookmarkDeleted(item);
                                }
                            }
                        });
                }
            },
            new BrowserDialog.Item(R.string.dialog_edit_bookmark) {
                @Override
                public void onClick() {
                    showEditBookmarkDialog(activity, uiController, item);
                }
            });
    }

    /**
     * Show the appropriated dialog for the long pressed link.
     *
     * @param activity used to show the dialog
     * @param url      the long pressed url
     */
    public void showLongPressedDialogForDownloadUrl(@NonNull final Activity activity,
                                                    @NonNull final UIController uiController,
                                                    @NonNull final String url) {

        BrowserDialog.show(activity, R.string.action_downloads,
            new BrowserDialog.Item(R.string.dialog_delete_all_downloads) {
                @Override
                public void onClick() {
                    mDownloadsModel.deleteAllDownloads()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(new CompletableOnSubscribe() {
                            @Override
                            public void onComplete() {
                                uiController.handleDownloadDeleted();
                            }
                        });
                }
            });
    }

    private void showEditBookmarkDialog(@NonNull final Activity activity,
                                        @NonNull final UIController uiController,
                                        @NonNull final HistoryItem item) {
        final AlertDialog.Builder editBookmarkDialog = new AlertDialog.Builder(activity);
        editBookmarkDialog.setTitle(R.string.title_edit_bookmark);
        final View dialogLayout = View.inflate(activity, R.layout.dialog_edit_bookmark, null);
        final EditText getTitle = dialogLayout.findViewById(R.id.bookmark_title);
        getTitle.setText(item.getTitle());
        final EditText getUrl = dialogLayout.findViewById(R.id.bookmark_url);
        getUrl.setText(item.getUrl());
        final AutoCompleteTextView getFolder =
            dialogLayout.findViewById(R.id.bookmark_folder);
        getFolder.setHint(R.string.folder);
        getFolder.setText(item.getFolder());

        mBookmarkManager.getFolderNames()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.main())
            .subscribe(new SingleOnSubscribe<List<String>>() {
                @Override
                public void onItem(@Nullable List<String> folders) {
                    Preconditions.checkNonNull(folders);
                    final ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_dropdown_item_1line, folders);
                    getFolder.setThreshold(1);
                    getFolder.setAdapter(suggestionsAdapter);
                    editBookmarkDialog.setView(dialogLayout);
                    editBookmarkDialog.setPositiveButton(activity.getString(R.string.action_ok),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                HistoryItem editedItem = new HistoryItem();
                                editedItem.setTitle(getTitle.getText().toString());
                                editedItem.setUrl(getUrl.getText().toString());
                                editedItem.setUrl(getUrl.getText().toString());
                                editedItem.setFolder(getFolder.getText().toString());
                                mBookmarkManager.editBookmark(item, editedItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.main())
                                    .subscribe(new CompletableOnSubscribe() {
                                        @Override
                                        public void onComplete() {
                                            uiController.handleBookmarksChange();
                                        }
                                    });
                            }
                        });
                    Dialog dialog = editBookmarkDialog.show();
                    BrowserDialog.setDialogSize(activity, dialog);
                }
            });
    }

    public void showBookmarkFolderLongPressedDialog(@NonNull final Activity activity,
                                                    @NonNull final UIController uiController,
                                                    @NonNull final HistoryItem item) {

        BrowserDialog.show(activity, R.string.action_folder,
            new BrowserDialog.Item(R.string.dialog_rename_folder) {
                @Override
                public void onClick() {
                    showRenameFolderDialog(activity, uiController, item);
                }
            },
            new BrowserDialog.Item(R.string.dialog_remove_folder) {
                @Override
                public void onClick() {
                    mBookmarkManager.deleteFolder(item.getTitle())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(new CompletableOnSubscribe() {
                            @Override
                            public void onComplete() {
                                uiController.handleBookmarkDeleted(item);
                            }
                        });
                }
            });
    }

    private void showRenameFolderDialog(@NonNull final Activity activity,
                                        @NonNull final UIController uiController,
                                        @NonNull final HistoryItem item) {
        BrowserDialog.showEditText(activity, R.string.title_rename_folder,
            R.string.hint_title, item.getTitle(),
            R.string.action_ok, new BrowserDialog.EditorListener() {
                @Override
                public void onClick(@NonNull String text) {
                    if (!TextUtils.isEmpty(text)) {
                        final String oldTitle = item.getTitle();
                        final HistoryItem editedItem = new HistoryItem();
                        editedItem.setTitle(text);
                        editedItem.setUrl(Constants.FOLDER + text);
                        editedItem.setFolder(item.getFolder());
                        editedItem.setIsFolder(true);
                        mBookmarkManager.renameFolder(oldTitle, text)
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.main())
                            .subscribe(new CompletableOnSubscribe() {
                                @Override
                                public void onComplete() {
                                    uiController.handleBookmarksChange();
                                }
                            });
                    }
                }
            });
    }

    public void showLongPressedHistoryLinkDialog(@NonNull final Activity activity,
                                                 @NonNull final UIController uiController,
                                                 @NonNull final String url) {
        BrowserDialog.show(activity, R.string.action_history,
            new BrowserDialog.Item(R.string.dialog_open_new_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.FOREGROUND, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_background_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.BACKGROUND, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_incognito_tab, activity instanceof MainActivity) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.INCOGNITO, url);
                }
            },
            new BrowserDialog.Item(R.string.action_share) {
                @Override
                public void onClick() {
                    new IntentUtils(activity).shareUrl(url, null);
                }
            },
            new BrowserDialog.Item(R.string.dialog_copy_link) {
                @Override
                public void onClick() {
                    BrowserApp.copyToClipboard(activity, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_remove_from_history) {
                @Override
                public void onClick() {
                    mHistoryModel.deleteHistoryItem(url)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.main())
                        .subscribe(new CompletableOnSubscribe() {
                            @Override
                            public void onComplete() {
                                uiController.handleHistoryChange();
                            }
                        });
                }
            });
    }

    // TODO There should be a way in which we do not need an activity reference to dowload a file
    public void showLongPressImageDialog(@NonNull final Activity activity,
                                         @NonNull final UIController uiController,
                                         @NonNull final String url,
                                         @NonNull final String userAgent) {
        BrowserDialog.show(activity, url.replace(Constants.HTTP, ""),
            new BrowserDialog.Item(R.string.dialog_open_new_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.FOREGROUND, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_background_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.BACKGROUND, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_incognito_tab, activity instanceof MainActivity) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.INCOGNITO, url);
                }
            },
            new BrowserDialog.Item(R.string.action_share) {
                @Override
                public void onClick() {
                    new IntentUtils(activity).shareUrl(url, null);
                }
            },
            new BrowserDialog.Item(R.string.dialog_copy_link) {
                @Override
                public void onClick() {
                    BrowserApp.copyToClipboard(activity, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_download_image) {
                @Override
                public void onClick() {
                    mDownloadHandler.onDownloadStart(activity, mPreferenceManager, url, userAgent, "attachment", null, "");
                }
            });
    }

    public void showLongPressLinkDialog(@NonNull final Activity activity,
                                        @NonNull final UIController uiController,
                                        @NonNull final String url) {
        BrowserDialog.show(activity, url,
            new BrowserDialog.Item(R.string.dialog_open_new_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.FOREGROUND, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_background_tab) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.BACKGROUND, url);
                }
            },
            new BrowserDialog.Item(R.string.dialog_open_incognito_tab, activity instanceof MainActivity) {
                @Override
                public void onClick() {
                    uiController.handleNewTab(NewTab.INCOGNITO, url);
                }
            },
            new BrowserDialog.Item(R.string.action_share) {
                @Override
                public void onClick() {
                    new IntentUtils(activity).shareUrl(url, null);
                }
            },
            new BrowserDialog.Item(R.string.dialog_copy_link) {
                @Override
                public void onClick() {
                    BrowserApp.copyToClipboard(activity, url);
                }
            });
    }

}
