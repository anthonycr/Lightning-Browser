/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.database.BookmarkManager;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;

public final class BookmarkPage {

    private static final String HEADING = "<!DOCTYPE html><html xmlns=http://www.w3.org/1999/xhtml>\n" +
            "<head>\n" +
            "<meta content=en-us http-equiv=Content-Language />\n" +
            "<meta content='text/html; charset=utf-8' http-equiv=Content-Type />\n" +
            "<meta name=viewport content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>\n" +
            "<title>" +
            BrowserApp.getAppContext().getString(R.string.action_bookmarks) +
            "</title>\n" +
            "</head>\n" +
            "<style>body{background:#e1e1e1;max-width:100%;min-height:100%}#content{width:100%;max-width:800px;margin:0 auto;text-align:center}.box{vertical-align:middle;text-align:center;position:relative;display:inline-block;height:45px;width:150px;margin:10px;background-color:#fff;box-shadow:0 3px 6px rgba(0,0,0,0.25);font-family:Arial;color:#444;font-size:12px;-moz-border-radius:2px;-webkit-border-radius:2px;border-radius:2px}.box-content{height:25px;width:100%;vertical-align:middle;text-align:center;display:table-cell}p.ellipses{" +
            "width:130px;font-size: small;font-family: Arial, Helvetica, 'sans-serif';white-space:nowrap;overflow:hidden;text-align:left;vertical-align:middle;margin:auto;text-overflow:ellipsis;-o-text-overflow:ellipsis;-ms-text-overflow:ellipsis}.box a{width:100%;height:100%;position:absolute;left:0;top:0}img{vertical-align:middle;margin-right:10px;width:20px;height:20px;}.margin{margin:10px}</style>\n" +
            "<body><div id=content>";

    private static final String PART1 = "<div class=box><a href='";

    private static final String PART2 = "'></a>\n" +
            "<div class=margin>\n" +
            "<div class=box-content>\n" +
            "<p class=ellipses>\n" +
            "<img src='";

    private static final String PART3 = "http://www.google.com/s2/favicons?domain=";

    private static final String PART4 = "' />";

    private static final String PART5 = "</p></div></div></div>";

    private static final String END = "</div></body></html>";

    @Inject
    BookmarkManager manager;

    private final File FILES_DIR;
    private final File CACHE_DIR;

    @Inject
    public BookmarkPage(Context context) {
        BrowserApp.getAppComponent().inject(this);
        FILES_DIR = context.getFilesDir();
        CACHE_DIR = context.getCacheDir();
    }

    public void buildBookmarkPage(final String folder) {
        final List<HistoryItem> list = manager.getBookmarksFromFolder(folder, true);
        final File bookmarkWebPage;
        if (folder == null || folder.isEmpty()) {
            bookmarkWebPage = new File(FILES_DIR, Constants.BOOKMARKS_FILENAME);
        } else {
            bookmarkWebPage = new File(FILES_DIR, folder + '-' + Constants.BOOKMARKS_FILENAME);
        }
        final StringBuilder bookmarkBuilder = new StringBuilder(BookmarkPage.HEADING);

        final String folderIconPath = Constants.FILE + CACHE_DIR + "/folder.png";
        for (int n = 0, size = list.size(); n < size; n++) {
            final HistoryItem item = list.get(n);
            bookmarkBuilder.append(BookmarkPage.PART1);
            if (item.isFolder()) {
                final File folderPage = new File(FILES_DIR, item.getTitle() + '-' + Constants.BOOKMARKS_FILENAME);
                bookmarkBuilder.append(Constants.FILE).append(folderPage);
                bookmarkBuilder.append(BookmarkPage.PART2);
                bookmarkBuilder.append(folderIconPath);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        buildBookmarkPage(item.getTitle());
                    }
                }).run();
            } else {
                bookmarkBuilder.append(item.getUrl());
                bookmarkBuilder.append(BookmarkPage.PART2).append(BookmarkPage.PART3);
                bookmarkBuilder.append(item.getUrl());
            }
            bookmarkBuilder.append(BookmarkPage.PART4);
            bookmarkBuilder.append(item.getTitle());
            bookmarkBuilder.append(BookmarkPage.PART5);
        }
        bookmarkBuilder.append(BookmarkPage.END);
        FileWriter bookWriter = null;
        try {
            bookWriter = new FileWriter(bookmarkWebPage, false);
            bookWriter.write(bookmarkBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookWriter);
        }
    }

}
