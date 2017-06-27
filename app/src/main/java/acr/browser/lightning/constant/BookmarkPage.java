/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
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

import acr.browser.lightning.R;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.database.bookmark.BookmarkModel;
import acr.browser.lightning.favicon.FaviconModel;
import acr.browser.lightning.favicon.FaviconUtils;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.ThemeUtils;
import acr.browser.lightning.utils.Utils;

public final class BookmarkPage {

    /**
     * The bookmark page standard suffix
     */
    public static final String FILENAME = "bookmarks.html";

    private static final String HEADING_1 = "<!DOCTYPE html><html xmlns=http://www.w3.org/1999/xhtml>\n" +
        "<head>\n" +
        "<meta content=en-us http-equiv=Content-Language />\n" +
        "<meta content='text/html; charset=utf-8' http-equiv=Content-Type />\n" +
        "<meta name=viewport content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
        "<title>";

    private static final String HEADING_2 = "</title>\n" +
        "</head>\n" +
        "<style>body{background: #E5E5E5; padding-top: 5px;max-width:100%;min-height:100%}" +
        "#content{width:100%;max-width:800px;margin:0 auto;text-align:center}" +
        ".box{vertical-align:middle;text-align:center;position:relative;display:inline-block;height:45px;width:150px;margin:6px;padding:4px;background-color:#fff;border: 1px solid #d2d2d2;border-top-width: 0;border-bottom-width: 2px;font-family:Arial;color:#444;font-size:12px;-moz-border-radius:2px;-webkit-border-radius:2px;border-radius:2px}" +
        ".box-content{height:25px;width:100%;vertical-align:middle;text-align:center;display:table-cell}" +
        "p.ellipses{width:130px;font-size: small;font-family: Arial, Helvetica, 'sans-serif';white-space:nowrap;overflow:hidden;text-align:left;vertical-align:middle;margin:auto;text-overflow:ellipsis;-o-text-overflow:ellipsis;-ms-text-overflow:ellipsis}" +
        ".box a{width:100%;height:100%;position:absolute;left:0;top:0}" +
        "img{vertical-align:middle;margin-right:10px;width:20px;height:20px;}" +
        ".margin{margin:10px}</style>\n" +
        "<body><div id=content>";

    private static final String PART1 = "<div class=box><a href='";

    private static final String PART2 = "'></a>\n" +
        "<div class=margin>\n" +
        "<div class=box-content>\n" +
        "<p class=ellipses>\n" +
        "<img src='";

    private static final String PART3 = "https://www.google.com/s2/favicons?domain=";

    private static final String PART4 = "' />";

    private static final String PART5 = "</p></div></div></div>";

    private static final String END = "</div></body></html>";

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
    @NonNull private final String mTitle;

    public BookmarkPage(@NonNull Activity activity) {
        BrowserApp.getAppComponent().inject(this);
        mFolderIcon = ThemeUtils.getThemedBitmap(activity, R.drawable.ic_folder, false);
        mTitle = mApp.getString(R.string.action_bookmarks);
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

        final StringBuilder bookmarkBuilder = new StringBuilder(HEADING_1 + mTitle + HEADING_2);

        final String folderIconPath = getFaviconFile(mApp).toString();

        for (int n = 0, size = bookmarksAndFolders.size(); n < size; n++) {
            final HistoryItem item = bookmarksAndFolders.get(n);
            bookmarkBuilder.append(PART1);
            if (item.isFolder()) {
                final File folderPage = getBookmarkPage(mApp, item.getTitle());
                bookmarkBuilder.append(Constants.FILE).append(folderPage);
                bookmarkBuilder.append(PART2);
                bookmarkBuilder.append(folderIconPath);
                buildBookmarkPage(item.getTitle());
            } else {

                Uri bookmarkUri = FaviconUtils.safeUri(item.getUrl());

                String faviconFileUrl;

                if (bookmarkUri != null) {
                    File faviconFile = FaviconModel.getFaviconCacheFile(mApp, bookmarkUri);
                    if (!faviconFile.exists()) {
                        Bitmap defaultFavicon = mFaviconModel.getDefaultBitmapForString(item.getTitle());
                        mFaviconModel.cacheFaviconForUrl(defaultFavicon, item.getUrl()).subscribe();
                    }

                    faviconFileUrl = Constants.FILE + faviconFile;
                } else {
                    faviconFileUrl = Constants.FILE + getDefaultIconFile(mApp);
                }


                bookmarkBuilder.append(item.getUrl());
                bookmarkBuilder.append(PART2).append(faviconFileUrl);
            }
            bookmarkBuilder.append(PART4);
            bookmarkBuilder.append(item.getTitle());
            bookmarkBuilder.append(PART5);
        }
        bookmarkBuilder.append(END);
        FileWriter bookWriter = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            bookWriter = new FileWriter(bookmarkWebPage, false);
            bookWriter.write(bookmarkBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookWriter);
        }
    }

}
