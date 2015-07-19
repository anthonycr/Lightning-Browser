package acr.browser.lightning.database;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.Browser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;

public class BookmarkManager {

    private final Context mContext;
    private static final String TITLE = "title";
    private static final String URL = "url";
    private static final String FOLDER = "folder";
    private static final String ORDER = "order";
    private static final String FILE_BOOKMARKS = "bookmarks.dat";
    private static SortedMap<String, Integer> mBookmarkMap = new TreeMap<>(
            String.CASE_INSENSITIVE_ORDER);
    private static BookmarkManager mInstance;

    public static BookmarkManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BookmarkManager(context);
        }
        return mInstance;
    }

    private BookmarkManager(Context context) {
        mContext = context;
        mBookmarkMap = getBookmarkUrls();
    }

    /**
     * This method adds the the HistoryItem item to permanent bookmark storage
     *
     * @param item the item to add
     */
    public synchronized boolean addBookmark(HistoryItem item) {
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        if (item.getUrl() == null || mBookmarkMap.containsKey(item.getUrl())) {
            return false;
        }
        BufferedWriter bookmarkWriter = null;
        try {
            bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, true));
            JSONObject object = new JSONObject();
            object.put(TITLE, item.getTitle());
            object.put(URL, item.getUrl());
            object.put(FOLDER, item.getFolder());
            object.put(ORDER, item.getOrder());
            bookmarkWriter.write(object.toString());
            bookmarkWriter.newLine();
            bookmarkWriter.close();
            mBookmarkMap.put(item.getUrl(), 1);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarkWriter);
        }
        return true;
    }

    /**
     * This method adds the list of HistoryItems to permanent bookmark storage
     *
     * @param list the list of HistoryItems to add to bookmarks
     */
    private synchronized void addBookmarkList(List<HistoryItem> list) {
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedWriter bookmarkWriter = null;
        try {
            bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, true));
            JSONObject object = new JSONObject();
            for (HistoryItem item : list) {
                if (item.getUrl() != null && !mBookmarkMap.containsKey(item.getUrl())) {
                    object.put(TITLE, item.getTitle());
                    object.put(URL, item.getUrl());
                    object.put(FOLDER, item.getFolder());
                    object.put(ORDER, item.getOrder());
                    bookmarkWriter.write(object.toString());
                    bookmarkWriter.newLine();
                    mBookmarkMap.put(item.getUrl(), 1);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarkWriter);
        }
    }

    /**
     * This method deletes the bookmark with the given url
     *
     * @param url the url of the bookmark to delete
     */
    public synchronized boolean deleteBookmark(String url) {
        List<HistoryItem> list;
        if (url == null) {
            return false;
        }
        mBookmarkMap.remove(url);
        list = getBookmarks(false);
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        boolean bookmarkDeleted = false;
        BufferedWriter fileWriter = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(bookmarksFile, false));
            JSONObject object = new JSONObject();
            for (HistoryItem item : list) {
                if (!item.getUrl().equalsIgnoreCase(url)) {
                    object.put(TITLE, item.getTitle());
                    object.put(URL, item.getUrl());
                    object.put(FOLDER, item.getFolder());
                    object.put(ORDER, item.getOrder());
                    fileWriter.write(object.toString());
                    fileWriter.newLine();
                } else {
                    bookmarkDeleted = true;
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(fileWriter);
        }
        return bookmarkDeleted;
    }

    /**
     * This method exports the stored bookmarks to a text file in the device's
     * external download directory
     */
    public synchronized void exportBookmarks(Activity activity) {
        List<HistoryItem> bookmarkList = getBookmarks(true);
        File bookmarksExport = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "BookmarksExport.txt");
        int counter = 0;
        while (bookmarksExport.exists()) {
            counter++;
            bookmarksExport = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "BookmarksExport-" + counter + ".txt");
        }
        BufferedWriter bookmarkWriter = null;
        try {
            bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksExport,
                    false));
            JSONObject object = new JSONObject();
            for (HistoryItem item : bookmarkList) {
                object.put(TITLE, item.getTitle());
                object.put(URL, item.getUrl());
                object.put(FOLDER, item.getFolder());
                object.put(ORDER, item.getOrder());
                bookmarkWriter.write(object.toString());
                bookmarkWriter.newLine();
            }
            Utils.showSnackbar(activity, activity.getString(R.string.bookmark_export_path)
                    + ' ' + bookmarksExport.getPath());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarkWriter);
        }

    }

    /**
     * This method returns a list of all stored bookmarks
     *
     * @return returns a list of bookmarks that can be sorted
     */
    public synchronized List<HistoryItem> getBookmarks(boolean sort) {
        List<HistoryItem> bookmarks = new ArrayList<>();
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                HistoryItem item = new HistoryItem();
                item.setTitle(object.getString(TITLE));
                item.setUrl(object.getString(URL));
                item.setFolder(object.getString(FOLDER));
                item.setOrder(object.getInt(ORDER));
                item.setImageId(R.drawable.ic_bookmark);
                bookmarks.add(item);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarksReader);
        }
        if (sort) {
            Collections.sort(bookmarks, new SortIgnoreCase());
        }
        return bookmarks;
    }

    /**
     * This method returns a list of bookmarks located in the specified folder
     *
     * @param folder the name of the folder to retrieve bookmarks from
     * @return a list of bookmarks found in that folder
     */
    public synchronized List<HistoryItem> getBookmarksFromFolder(String folder) {
        List<HistoryItem> bookmarks = new ArrayList<>();
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                if (object.getString(FOLDER).equals(folder)) {
                    HistoryItem item = new HistoryItem();
                    item.setTitle(object.getString(TITLE));
                    item.setUrl(object.getString(URL));
                    item.setFolder(object.getString(FOLDER));
                    item.setOrder(object.getInt(ORDER));
                    item.setImageId(R.drawable.ic_bookmark);
                    bookmarks.add(item);
                }
            }
            bookmarksReader.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarksReader);
        }
        return bookmarks;
    }

    /**
     * Method is used internally for searching the bookmarks
     *
     * @return a sorted map of all bookmarks, useful for seeing if a bookmark exists
     */
    private synchronized SortedMap<String, Integer> getBookmarkUrls() {
        SortedMap<String, Integer> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                map.put(object.getString(URL), 1);
            }
            bookmarksReader.close();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarksReader);
        }
        return map;
    }

    /**
     * This method returns a list of all folders
     *
     * @return a list of all folders
     */
    public synchronized List<HistoryItem> getFolders() {
        List<HistoryItem> folders = new ArrayList<>();
        SortedMap<String, Integer> folderMap = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER);
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                String folderName = object.getString(FOLDER);
                if (!folderName.isEmpty() && !folderMap.containsKey(folderName)) {
                    HistoryItem item = new HistoryItem();
                    item.setTitle(folderName);
                    item.setUrl(Constants.FOLDER + folderName);
                    folderMap.put(folderName, 1);
                    folders.add(item);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarksReader);
        }
        return folders;
    }

    /**
     * This method imports all bookmarks that are included in the device's
     * permanent bookmark storage
     */
    public synchronized void importBookmarksFromBrowser(Activity activity) {
        if (PreferenceManager.getInstance().getSystemBrowserPresent()) {

            List<HistoryItem> bookmarkList = new ArrayList<>();
            String[] columns = new String[]{Browser.BookmarkColumns.TITLE,
                    Browser.BookmarkColumns.URL};
            String selection = Browser.BookmarkColumns.BOOKMARK + " = 1";
            Cursor cursor = mContext.getContentResolver().query(Browser.BOOKMARKS_URI, columns,
                    selection, null, null);
            if (cursor == null)
                return;
            String title, url;
            int number = 0;
            if (cursor.moveToFirst()) {
                do {
                    title = cursor.getString(0);
                    url = cursor.getString(1);
                    if (title.isEmpty()) {
                        title = Utils.getDomainName(url);
                    }
                    number++;
                    bookmarkList.add(new HistoryItem(url, title));
                } while (cursor.moveToNext());
            }

            cursor.close();
            addBookmarkList(bookmarkList);

            Utils.showSnackbar(activity, number + " " + mContext.getResources().getString(R.string.message_import));
        } else {
            String title = activity.getResources().getString(R.string.title_error);
            String message = activity.getResources().getString(R.string.dialog_import_error);
            Utils.createInformativeDialog(activity, title, message);
        }
    }

    /**
     * This method imports the bookmarks from a backup file that is located on
     * external storage
     *
     * @param file the file to attempt to import bookmarks from
     */
    public synchronized void importBookmarksFromFile(File file, Activity activity) {
        if (file == null) {
            return;
        }
        List<HistoryItem> list = new ArrayList<>();
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(file));
            String line;
            int number = 0;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                HistoryItem item = new HistoryItem();
                item.setTitle(object.getString(TITLE));
                item.setUrl(object.getString(URL));
                item.setFolder(object.getString(FOLDER));
                item.setOrder(object.getInt(ORDER));
                list.add(item);
                number++;
            }
            addBookmarkList(list);
            String message = activity.getResources().getString(R.string.message_import);
            Utils.showSnackbar(activity, number + " " + message);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            String title = activity.getResources().getString(R.string.title_error);
            String message = activity.getResources().getString(R.string.import_bookmark_error);
            Utils.createInformativeDialog(activity, title, message);
        } finally {
            Utils.close(bookmarksReader);
        }
    }

    /**
     * This method overwrites the entire bookmark file with the list of
     * bookmarks. This is useful when an edit has been made to one or more
     * bookmarks in the list
     *
     * @param list the list of bookmarks to overwrite the old ones with
     */
    public synchronized void overwriteBookmarks(List<HistoryItem> list) {
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedWriter bookmarkWriter = null;
        try {
            bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, false));
            JSONObject object = new JSONObject();
            for (HistoryItem item : list) {
                object.put(TITLE, item.getTitle());
                object.put(URL, item.getUrl());
                object.put(FOLDER, item.getFolder());
                object.put(ORDER, item.getOrder());
                bookmarkWriter.write(object.toString());
                bookmarkWriter.newLine();
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarkWriter);
        }
    }

    private class SortIgnoreCase implements Comparator<HistoryItem> {

        public int compare(HistoryItem o1, HistoryItem o2) {
            if (o1 == null || o2 == null || o1.getTitle() == null || o2.getTitle() == null) {
                return 0;
            }
            return o1.getTitle().toLowerCase(Locale.getDefault())
                    .compareTo(o2.getTitle().toLowerCase(Locale.getDefault()));
        }

    }

    private static final String[] DEV = {"https://twitter.com/RestainoAnthony", "The Developer"};
    private static final String[] FACEBOOK = {"https://www.facebook.com/", "Facebook"};
    private static final String[] TWITTER = {"https://twitter.com", "Twitter"};
    private static final String[] GOOGLE = {"https://www.google.com/", "Google"};
    private static final String[] YAHOO = {"https://www.yahoo.com/", "Yahoo"};
    public static final String[][] DEFAULT_BOOKMARKS = {
            DEV,
            FACEBOOK,
            TWITTER,
            GOOGLE,
            YAHOO
    };
}
