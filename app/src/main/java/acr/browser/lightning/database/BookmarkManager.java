package acr.browser.lightning.database;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.utils.Utils;

@Singleton
public class BookmarkManager {

    private static final String TAG = BookmarkManager.class.getSimpleName();

    private static final String TITLE = "title";
    private static final String URL = "url";
    private static final String FOLDER = "folder";
    private static final String ORDER = "order";
    private static final String FILE_BOOKMARKS = "bookmarks.dat";

    @NonNull private final String DEFAULT_BOOKMARK_TITLE;

    private Map<String, HistoryItem> mBookmarksMap;
    @NonNull private String mCurrentFolder = "";
    @NonNull private final ExecutorService mExecutor;
    private final File mFilesDir;

    @Inject
    public BookmarkManager(@NonNull Context context) {
        mExecutor = Executors.newSingleThreadExecutor();
        mFilesDir = context.getFilesDir();
        DEFAULT_BOOKMARK_TITLE = context.getString(R.string.untitled);
        mExecutor.execute(new BookmarkInitializer(context));
    }

    /**
     * Look for bookmark using the url
     *
     * @param url the lookup url
     * @return the bookmark as an {@link HistoryItem} or null
     */
    @Nullable
    public HistoryItem findBookmarkForUrl(final String url) {
        return mBookmarksMap.get(url);
    }

    /**
     * Initialize the BookmarkManager, it's a one-time operation and will be executed asynchronously.
     * When done, mReady flag will been set to true.
     */
    private class BookmarkInitializer implements Runnable {
        private final Context mContext;

        public BookmarkInitializer(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            synchronized (BookmarkManager.this) {
                final Map<String, HistoryItem> bookmarks = new HashMap<>();
                final File bookmarksFile = new File(mFilesDir, FILE_BOOKMARKS);

                BufferedReader bookmarksReader = null;
                InputStream inputStream = null;
                try {
                    if (bookmarksFile.exists() && bookmarksFile.isFile()) {
                        //noinspection IOResourceOpenedButNotSafelyClosed
                        inputStream = new FileInputStream(bookmarksFile);
                    } else {
                        inputStream = mContext.getResources().openRawResource(R.raw.default_bookmarks);
                    }
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    bookmarksReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = bookmarksReader.readLine()) != null) {
                        try {
                            JSONObject object = new JSONObject(line);
                            HistoryItem item = new HistoryItem();
                            item.setTitle(object.getString(TITLE));
                            final String url = object.getString(URL);
                            item.setUrl(url);
                            item.setFolder(object.getString(FOLDER));
                            item.setOrder(object.getInt(ORDER));
                            item.setImageId(R.drawable.ic_bookmark);
                            bookmarks.put(url, item);
                        } catch (JSONException e) {
                            Log.e(TAG, "Can't parse line " + line, e);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading the bookmarks file", e);
                } finally {
                    Utils.close(bookmarksReader);
                    Utils.close(inputStream);
                }
                mBookmarksMap = bookmarks;
            }
        }

    }

    /**
     * Dump all the given bookmarks to the bookmark file using a temporary file
     */
    private class BookmarksWriter implements Runnable {

        private final List<HistoryItem> mBookmarks;

        public BookmarksWriter(List<HistoryItem> bookmarks) {
            mBookmarks = bookmarks;
        }

        @Override
        public void run() {
            final File tempFile = new File(mFilesDir,
                    String.format(Locale.US, "bm_%d.dat", System.currentTimeMillis()));
            final File bookmarksFile = new File(mFilesDir, FILE_BOOKMARKS);
            boolean success = false;
            BufferedWriter bookmarkWriter = null;
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                bookmarkWriter = new BufferedWriter(new FileWriter(tempFile, false));
                JSONObject object = new JSONObject();
                for (HistoryItem item : mBookmarks) {
                    object.put(TITLE, item.getTitle());
                    object.put(URL, item.getUrl());
                    object.put(FOLDER, item.getFolder());
                    object.put(ORDER, item.getOrder());
                    bookmarkWriter.write(object.toString());
                    bookmarkWriter.newLine();
                }
                success = true;
            } catch (@NonNull IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                Utils.close(bookmarkWriter);
            }

            if (success) {
                // Overwrite the bookmarks file by renaming the temp file
                //noinspection ResultOfMethodCallIgnored
                tempFile.renameTo(bookmarksFile);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        mExecutor.shutdownNow();
        super.finalize();
    }

    public boolean isBookmark(String url) {
        return mBookmarksMap.containsKey(url);
    }

    /**
     * This method adds the the HistoryItem item to permanent bookmark storage.<br>
     * This operation is blocking if the manager is still not ready.
     *
     * @param item the item to add
     * @return It returns true if the operation was successful.
     */
    public synchronized boolean addBookmark(@NonNull HistoryItem item) {
        final String url = item.getUrl();
        if (mBookmarksMap.containsKey(url)) {
            return false;
        }
        mBookmarksMap.put(url, item);
        mExecutor.execute(new BookmarksWriter(new LinkedList<>(mBookmarksMap.values())));
        return true;
    }

    /**
     * This method adds the list of HistoryItems to permanent bookmark storage
     *
     * @param list the list of HistoryItems to add to bookmarks
     */
    public synchronized void addBookmarkList(@Nullable List<HistoryItem> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (HistoryItem item : list) {
            final String url = item.getUrl();
            if (!mBookmarksMap.containsKey(url)) {
                mBookmarksMap.put(url, item);
            }
        }
        mExecutor.execute(new BookmarksWriter(new LinkedList<>(mBookmarksMap.values())));
    }

    /**
     * This method deletes the bookmark with the given url. It returns
     * true if the deletion was successful.
     *
     * @param deleteItem the bookmark item to delete
     */
    public synchronized boolean deleteBookmark(@Nullable HistoryItem deleteItem) {
        if (deleteItem == null || deleteItem.isFolder()) {
            return false;
        }
        mBookmarksMap.remove(deleteItem.getUrl());
        mExecutor.execute(new BookmarksWriter(new LinkedList<>(mBookmarksMap.values())));
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
        for (HistoryItem item : mBookmarksMap.values()) {
            if (item.getFolder().equals(oldName)) {
                item.setFolder(newName);
            } else if (item.isFolder() && item.getTitle().equals(oldName)) {
                item.setTitle(newName);
                item.setUrl(Constants.FOLDER + newName);
            }
        }
        mExecutor.execute(new BookmarksWriter(new LinkedList<>(mBookmarksMap.values())));
    }

    /**
     * Delete the folder and move all bookmarks to the top level
     *
     * @param name the name of the folder to be deleted
     */
    public synchronized void deleteFolder(@NonNull String name) {
        final Map<String, HistoryItem> bookmarks = new HashMap<>();
        for (HistoryItem item : mBookmarksMap.values()) {
            final String url = item.getUrl();
            if (item.isFolder()) {
                if (!item.getTitle().equals(name)) {
                    bookmarks.put(url, item);
                }
            } else {
                if (item.getFolder().equals(name)) {
                    item.setFolder("");
                }
                bookmarks.put(url, item);
            }
        }
        mBookmarksMap = bookmarks;
        mExecutor.execute(new BookmarksWriter(new LinkedList<>(mBookmarksMap.values())));
    }

    /**
     * This method edits a particular bookmark in the bookmark database
     *
     * @param oldItem This is the old item that you wish to edit
     * @param newItem This is the new item that will overwrite the old item
     */
    public synchronized void editBookmark(@Nullable HistoryItem oldItem, @Nullable HistoryItem newItem) {
        if (oldItem == null || newItem == null || oldItem.isFolder()) {
            return;
        }
        if (newItem.getUrl().isEmpty()) {
            deleteBookmark(oldItem);
            return;
        }
        if (newItem.getTitle().isEmpty()) {
            newItem.setTitle(DEFAULT_BOOKMARK_TITLE);
        }
        final String oldUrl = oldItem.getUrl();
        final String newUrl = newItem.getUrl();
        if (!oldUrl.equals(newUrl)) {
            // The url has been changed, remove the old one
            mBookmarksMap.remove(oldUrl);
        }
        mBookmarksMap.put(newUrl, newItem);
        mExecutor.execute(new BookmarksWriter(new LinkedList<>(mBookmarksMap.values())));
    }

    /**
     * This method exports the stored bookmarks to a text file in the device's
     * external download directory
     */
    public synchronized void exportBookmarks(@NonNull Activity activity) {
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
            //noinspection IOResourceOpenedButNotSafelyClosed
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
        } catch (@NonNull IOException | JSONException e) {
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
     * @param sort force to sort the returned bookmarkList
     * @return returns a list of bookmarks that can be sorted
     */
    @NonNull
    public synchronized List<HistoryItem> getAllBookmarks(boolean sort) {
        final List<HistoryItem> bookmarks = new ArrayList<>(mBookmarksMap.values());
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
    @NonNull
    public synchronized List<HistoryItem> getBookmarksFromFolder(@Nullable String folder, boolean sort) {
        List<HistoryItem> bookmarks = new ArrayList<>();
        if (folder == null || folder.isEmpty()) {
            bookmarks.addAll(getFolders(sort));
            folder = "";
        }
        mCurrentFolder = folder;
        for (HistoryItem item : mBookmarksMap.values()) {
            if (item.getFolder().equals(folder))
                bookmarks.add(item);
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
     * Returns the current folder
     *
     * @return the current folder
     */
    @Nullable
    public String getCurrentFolder() {
        return mCurrentFolder;
    }

    /**
     * This method returns a list of all folders.
     * Folders cannot be empty as they are generated from
     * the list of bookmarks that have non-empty folder fields.
     *
     * @return a list of all folders
     */
    @NonNull
    private synchronized List<HistoryItem> getFolders(boolean sort) {
        final HashMap<String, HistoryItem> folders = new HashMap<>();
        for (HistoryItem item : mBookmarksMap.values()) {
            final String folderName = item.getFolder();
            if (!folderName.isEmpty() && !folders.containsKey(folderName)) {
                final HistoryItem folder = new HistoryItem();
                folder.setIsFolder(true);
                folder.setTitle(folderName);
                folder.setImageId(R.drawable.ic_folder);
                folder.setUrl(Constants.FOLDER + folderName);
                folders.put(folderName, folder);
            }
        }
        final List<HistoryItem> result = new ArrayList<>(folders.values());
        if (sort) {
            Collections.sort(result, new SortIgnoreCase());
        }
        return result;
    }

    /**
     * returns a list of folder titles that can be used for suggestions in a
     * simple list adapter
     *
     * @return a list of folder title strings
     */
    @NonNull
    public synchronized List<String> getFolderTitles() {
        final Set<String> folders = new HashSet<>();
        for (HistoryItem item : mBookmarksMap.values()) {
            final String folderName = item.getFolder();
            if (!folderName.isEmpty()) {
                folders.add(folderName);
            }
        }
        return new ArrayList<>(folders);
    }

    /**
     * This method imports the bookmarks from a backup file that is located on
     * external storage
     *
     * @param file the file to attempt to import bookmarks from
     */
    public synchronized void importBookmarksFromFile(@Nullable File file, @NonNull Activity activity) {
        if (file == null) {
            return;
        }
        List<HistoryItem> list = new ArrayList<>();
        BufferedReader bookmarksReader = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
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
        } catch (@NonNull IOException | JSONException e) {
            e.printStackTrace();
            Utils.createInformativeDialog(activity, R.string.title_error, R.string.import_bookmark_error);
        } finally {
            Utils.close(bookmarksReader);
        }
    }

    /**
     * This class sorts bookmarks alphabetically, with folders coming after bookmarks
     */
    private static class SortIgnoreCase implements Comparator<HistoryItem> {

        public int compare(@Nullable HistoryItem o1, @Nullable HistoryItem o2) {
            if (o1 == null || o2 == null) {
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
