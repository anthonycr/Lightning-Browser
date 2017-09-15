/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleOnSubscribe;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkModel;
import acr.browser.lightning.favicon.FaviconModel;
import acr.browser.lightning.html.bookmark.BookmarkPageBuilder;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

public final class BookmarkPage {

    /**
     * The bookmark page standard suffix
     */
    public static final String FILENAME = "bookmarks.html";

    private static final String FOLDER_ICON = "folder.png";
    private static final String DEFAULT_ICON = "default.png";

    @NonNull
    public static File getBookmarkPage(@NonNull Application application, @Nullable String folder) {
        String prefix = !TextUtils.isEmpty(folder) ? folder + '-' : "";
        return new File(application.getFilesDir(), prefix + FILENAME);
    }

    @NonNull
    private static File getFaviconFile(@NonNull Application application) {
        return new File(application.getCacheDir(), FOLDER_ICON);
    }

    @NonNull
    private static File getDefaultIconFile(@NonNull Application application) {
        return new File(application.getCacheDir(), DEFAULT_ICON);
    }

    @Inject Application mApp;
    @Inject BookmarkModel mBookmarkModel;
    @Inject FaviconModel mFaviconModel;

    @NonNull private final Bitmap mFolderIcon;

    public BookmarkPage(@NonNull Activity activity) {
        BrowserApp.getAppComponent().inject(this);
        mFolderIcon = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_folder, false);
    }

    @NonNull
    public Single<String> getBookmarkPage() {
        return Single.create(new SingleAction<String>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<String> subscriber) {
                cacheIcon(mFolderIcon, getFaviconFile(mApp));
                cacheIcon(mFaviconModel.getDefaultBitmapForString(null), getDefaultIconFile(mApp));
                buildBookmarkPage(null);

                File bookmarkWebPage = getBookmarkPage(mApp, null);

                subscriber.onItem(Constants.FILE + bookmarkWebPage);
                subscriber.onComplete();
            }
        });
    }

    private void cacheIcon(@NonNull Bitmap icon, @NonNull File file) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            icon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            icon.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            Utils.close(outputStream);
        }
    }

    private void buildBookmarkPage(@Nullable final String folder) {
        mBookmarkModel.getBookmarksFromFolderSorted(folder)
            .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                @Override
                public void onItem(@Nullable final List<HistoryItem> list) {
                    Preconditions.checkNonNull(list);

                    if (folder == null) {
                        mBookmarkModel.getFoldersSorted()
                            .subscribe(new SingleOnSubscribe<List<HistoryItem>>() {
                                @Override
                                public void onItem(@Nullable List<HistoryItem> item) {
                                    Preconditions.checkNonNull(item);

                                    list.addAll(item);

                                    buildPageHtml(list, null);
                                }
                            });
                    } else {
                        buildPageHtml(list, folder);
                    }
                }
            });
    }

    private void buildPageHtml(@NonNull List<HistoryItem> bookmarksAndFolders, @Nullable String folder) {
        final File bookmarkWebPage = getBookmarkPage(mApp, folder);

        BookmarkPageBuilder builder = new BookmarkPageBuilder(mFaviconModel, mApp);

        FileWriter bookWriter = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            bookWriter = new FileWriter(bookmarkWebPage, false);
            bookWriter.write(builder.buildPage(bookmarksAndFolders));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookWriter);
        }

        for (HistoryItem item : bookmarksAndFolders) {
            if (item.isFolder()) {
                buildBookmarkPage(item.getTitle());
            }
        }
    }

}
