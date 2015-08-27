package acr.browser.lightning.database;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.Browser;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;

public class BookmarkManager {

    private static final String TAG = BookmarkManager.class.getSimpleName();

    private final Context mContext;
    private static final String TITLE = "title";
    private static final String URL = "url";
    private static final String FOLDER = "folder";
    private static final String ORDER = "order";
    private static final String FILE_BOOKMARKS = "bookmarks.dat";
    private Set<String> mBookmarkSearchSet = new HashSet<>();
    private final List<HistoryItem> mBookmarkList = new ArrayList<>();
    private String mCurrentFolder = "";
    private static BookmarkManager mInstance;

    public static BookmarkManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BookmarkManager(context);
        }
        return mInstance;
    }

    private BookmarkManager(Context context) {
        mContext = context;
        mBookmarkList.clear();
        mBookmarkList.addAll(getAllBookmarks(true));
        mBookmarkSearchSet = getBookmarkUrls(mBookmarkList);
    }

    public boolean isBookmark(String url) {
        return mBookmarkSearchSet.contains(url);
    }

    /**
     * This method adds the the HistoryItem item to permanent bookmark storage.
     * It returns true if the operation was successful.
     *
     * @param item the item to add
     */
    public synchronized boolean addBookmark(HistoryItem item) {
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        if (item.getUrl() == null || mBookmarkSearchSet.contains(item.getUrl())) {
            return false;
        }
        mBookmarkList.add(item);
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
            mBookmarkSearchSet.add(item.getUrl());
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
                if (item.getUrl() != null && !mBookmarkSearchSet.contains(item.getUrl())) {
                    object.put(TITLE, item.getTitle());
                    object.put(URL, item.getUrl());
                    object.put(FOLDER, item.getFolder());
                    object.put(ORDER, item.getOrder());
                    bookmarkWriter.write(object.toString());
                    bookmarkWriter.newLine();
                    mBookmarkSearchSet.add(item.getUrl());
                    mBookmarkList.add(item);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarkWriter);
        }
    }

    /**
     * This method deletes the bookmark with the given url. It returns
     * true if the deletion was successful.
     *
     * @param deleteItem the bookmark item to delete
     */
    public synchronized boolean deleteBookmark(HistoryItem deleteItem) {
        if (deleteItem == null || deleteItem.isFolder()) {
            return false;
        }
        mBookmarkSearchSet.remove(deleteItem.getUrl());
        mBookmarkList.remove(deleteItem);
        overwriteBookmarks(mBookmarkList);
        return true;
    }

    /**
     * renames a folder and moves all it's contents to that folder
     *
     * @param oldName the folder to be renamed
     * @param newName the new name of the folder
     */
    public synchronized void renameFolder(@NonNull String oldName, @NonNull String newName) {
        if (newName.isEmpty()) {
            return;
        }
        for (int n = 0; n < mBookmarkList.size(); n++) {
            if (mBookmarkList.get(n).getFolder().equals(oldName)) {
                mBookmarkList.get(n).setFolder(newName);
            } else if (mBookmarkList.get(n).isFolder() && mBookmarkList.get(n).getTitle().equals(oldName)) {
                mBookmarkList.get(n).setTitle(newName);
                mBookmarkList.get(n).setUrl(Constants.FOLDER + newName);
            }
        }
        overwriteBookmarks(mBookmarkList);
    }

    /**
     * Delete the folder and move all bookmarks to the top level
     *
     * @param name the name of the folder to be deleted
     */
    public synchronized void deleteFolder(@NonNull String name) {
        Iterator<HistoryItem> iterator = mBookmarkList.iterator();
        while (iterator.hasNext()) {
            HistoryItem item = iterator.next();
            if (!item.isFolder() && item.getFolder().equals(name)) {
                item.setFolder("");
            } else if (item.getTitle().equals(name)) {
                iterator.remove();
            }
        }
        overwriteBookmarks(mBookmarkList);
    }

    /**
     * This method edits a particular bookmark in the bookmark database
     *
     * @param oldItem This is the old item that you wish to edit
     * @param newItem This is the new item that will overwrite the old item
     */
    public synchronized void editBookmark(HistoryItem oldItem, HistoryItem newItem) {
        if (oldItem == null || newItem == null || oldItem.isFolder()) {
            return;
        }
        mBookmarkList.remove(oldItem);
        mBookmarkList.add(newItem);
        if (!oldItem.getUrl().equals(newItem.getUrl())) {
            // Update the BookmarkMap if the URL has been changed
            mBookmarkSearchSet.remove(oldItem.getUrl());
            mBookmarkSearchSet.add(newItem.getUrl());
        }
        if (newItem.getUrl().isEmpty()) {
            deleteBookmark(oldItem);
            return;
        }
        if (newItem.getTitle().isEmpty()) {
            newItem.setTitle(mContext.getString(R.string.untitled));
        }
        overwriteBookmarks(mBookmarkList);
    }

    /**
     * This method exports the stored bookmarks to a text file in the device's
     * external download directory
     */
    public synchronized void exportBookmarks(Activity activity) {
        List<HistoryItem> bookmarkList = getAllBookmarks(true);
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
     * This method returns a list of ALL stored bookmarks.
     * This is a disk-bound operation and should not be
     * done very frequently.
     *
     * @return returns a list of bookmarks that can be sorted
     */
    public synchronized List<HistoryItem> getAllBookmarks(boolean sort) {
		final List<HistoryItem> bookmarks = new ArrayList<>();
		final File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);

        BufferedReader bookmarksReader = null;
        try {
			final InputStream inputStream;
			if (bookmarksFile.exists() && bookmarksFile.isFile()) {
				inputStream = new FileInputStream(bookmarksFile);
			} else {
				inputStream = mContext.getResources().openRawResource(R.raw.default_bookmarks);
			}
			bookmarksReader =
					new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
				try {
                    JSONObject object = new JSONObject(line);
                    HistoryItem item = new HistoryItem();
                    item.setTitle(object.getString(TITLE));
                    item.setUrl(object.getString(URL));
                    item.setFolder(object.getString(FOLDER));
                    item.setOrder(object.getInt(ORDER));
                    item.setImageId(R.drawable.ic_bookmark);
                    bookmarks.add(item);
				} catch (JSONException e) {
					Log.e(TAG, "Can't parse line " + line, e);
                }
			}
		} catch (IOException e) {
            Log.e(TAG, "Error reading the bookmarks file", e);
        } finally {
            Utils.close(bookmarksReader);
        }
        if (sort) {
            Collections.sort(bookmarks, new SortIgnoreCase());
        }
        return bookmarks;
    }

    /**
     * This method returns a list of bookmarks and folders located in the specified folder.
     * This method should generally be used by the UI when it needs a list to display to the
     * user as it returns a subset of all bookmarks and includes folders as well which are
     * really 'fake' bookmarks.
     *
     * @param folder the name of the folder to retrieve bookmarks from
     * @return a list of bookmarks found in that folder
     */
    public synchronized List<HistoryItem> getBookmarksFromFolder(String folder, boolean sort) {
        List<HistoryItem> bookmarks = new ArrayList<>();
        if (folder == null || folder.isEmpty()) {
            bookmarks.addAll(getFolders(sort));
            folder = "";
        }
        mCurrentFolder = folder;
        for (int n = 0; n < mBookmarkList.size(); n++) {
            if (mBookmarkList.get(n).getFolder().equals(folder))
                bookmarks.add(mBookmarkList.get(n));
        }
        if (sort) {
            Collections.sort(bookmarks, new SortIgnoreCase());
        }
        return bookmarks;
    }

    /**
     * Tells you if you are at the root folder or in a subfolder
     *
     * @return returns true if you are in the root folder
     */
    public boolean isRootFolder() {
        return mCurrentFolder.isEmpty();
    }

    /**
     * Method is used internally for searching the bookmarks
     *
     * @return a sorted map of all bookmarks, useful for seeing if a bookmark exists
     */
    private Set<String> getBookmarkUrls(List<HistoryItem> list) {
        Set<String> set = new HashSet<>();
        for (int n = 0; n < list.size(); n++) {
            if (!mBookmarkList.get(n).isFolder())
                set.add(mBookmarkList.get(n).getUrl());
        }
        return set;
    }

    /**
     * This method returns a list of all folders.
     * Folders cannot be empty as they are generated from
     * the list of bookmarks that have non-empty folder fields.
     *
     * @return a list of all folders
     */
    public synchronized List<HistoryItem> getFolders(boolean sort) {
        List<HistoryItem> folders = new ArrayList<>();
        Set<String> folderMap = new HashSet<>();
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                String folderName = object.getString(FOLDER);
                if (!folderName.isEmpty() && !folderMap.contains(folderName)) {
                    HistoryItem item = new HistoryItem();
                    item.setTitle(folderName);
                    item.setUrl(Constants.FOLDER + folderName);
                    item.setIsFolder(true);
                    folderMap.add(folderName);
                    folders.add(item);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarksReader);
        }
        if (sort) {
            Collections.sort(folders, new SortIgnoreCase());
        }
        return folders;
    }

    /**
     * returns a list of folder titles that can be used for suggestions in a
     * simple list adapter
     *
     * @return a list of folder title strings
     */
    public synchronized List<String> getFolderTitles() {
        List<String> folders = new ArrayList<>();
        Set<String> folderMap = new HashSet<>();
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedReader bookmarksReader = null;
        try {
            bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                JSONObject object = new JSONObject(line);
                String folderName = object.getString(FOLDER);
                if (!folderName.isEmpty() && !folderMap.contains(folderName)) {
                    folders.add(folderName);
                    folderMap.add(folderName);
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
            Utils.createInformativeDialog(activity, R.string.title_error, R.string.import_bookmark_error);
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
    private synchronized void overwriteBookmarks(List<HistoryItem> list) {
        File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
        BufferedWriter bookmarkWriter = null;
        try {
            bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, false));
            JSONObject object = new JSONObject();
            for (int n = 0; n < list.size(); n++) {
                HistoryItem item = list.get(n);
                if (!item.isFolder()) {
                    object.put(TITLE, item.getTitle());
                    object.put(URL, item.getUrl());
                    object.put(FOLDER, item.getFolder());
                    object.put(ORDER, item.getOrder());
                    bookmarkWriter.write(object.toString());
                    bookmarkWriter.newLine();
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            Utils.close(bookmarkWriter);
        }
    }

    /**
     * find the index of a bookmark in a list using only its URL
     *
     * @param list the list to search
     * @param url  the url to compare
     * @return returns the index of the bookmark or -1 if none was found
     */
    public static int getIndexOfBookmark(final List<HistoryItem> list, final String url) {
        for (int n = 0; n < list.size(); n++) {
            if (list.get(n).getUrl().equals(url)) {
                return n;
            }
        }
        return -1;
    }

    /**
     * This class sorts bookmarks alphabetically, with folders coming after bookmarks
     */
    public static class SortIgnoreCase implements Comparator<HistoryItem> {

        public int compare(HistoryItem o1, HistoryItem o2) {
            if (o1 == null || o2 == null || o1.getTitle() == null || o2.getTitle() == null) {
                return 0;
            }
            if (o1.isFolder() == o2.isFolder()) {
                return o1.getTitle().toLowerCase(Locale.getDefault())
                        .compareTo(o2.getTitle().toLowerCase(Locale.getDefault()));

            } else {
                return o1.isFolder() ? 1 : -1;
            }
        }

    }
}
