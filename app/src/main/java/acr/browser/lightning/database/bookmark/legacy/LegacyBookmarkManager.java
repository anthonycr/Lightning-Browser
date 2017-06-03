package acr.browser.lightning.database.bookmark.legacy;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Utils;

@Deprecated
public class LegacyBookmarkManager {

    private static final String TAG = "LegacyBookmarkManager";

    private static final String TITLE = "title";
    private static final String URL = "url";
    private static final String FOLDER = "folder";
    private static final String ORDER = "order";
    private static final String FILE_BOOKMARKS = "bookmarks.dat";

    /**
     * Gets all bookmarks from the old bookmark file
     * and then deletes the file.
     *
     * @param application the context needed to open the file.
     * @return a list of bookmarks from the old bookmark file.
     */
    @WorkerThread
    @NonNull
    public static List<HistoryItem> destructiveGetBookmarks(@NonNull Application application) {
        File filesDir = application.getFilesDir();
        List<HistoryItem> bookmarks = new ArrayList<>();
        final File bookmarksFile = new File(filesDir, FILE_BOOKMARKS);

        BufferedReader bookmarksReader = null;
        InputStream inputStream = null;
        try {
            if (bookmarksFile.exists() && bookmarksFile.isFile()) {
                //noinspection IOResourceOpenedButNotSafelyClosed
                inputStream = new FileInputStream(bookmarksFile);
            } else {
                return bookmarks;
            }
            //noinspection IOResourceOpenedButNotSafelyClosed
            bookmarksReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                try {
                    JSONObject object = new JSONObject(line);

                    HistoryItem item = new HistoryItem();

                    item.setTitle(object.getString(TITLE));
                    item.setUrl(object.getString(URL));
                    item.setFolder(object.getString(FOLDER));
                    item.setPosition(object.getInt(ORDER));
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
            Utils.close(inputStream);
        }

        bookmarksFile.delete();

        return bookmarks;
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
