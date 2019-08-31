package acr.browser.lightning.database.bookmark;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.database.Bookmark;
import acr.browser.lightning.database.WebPageKt;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * The class responsible for importing and exporting
 * bookmarks in the JSON format.
 * <p>
 * Created by anthonycr on 5/7/17.
 */
public final class BookmarkExporter {

    private static final String TAG = "BookmarkExporter";

    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_FOLDER = "folder";
    private static final String KEY_ORDER = "order";

    private BookmarkExporter() {}

    /**
     * Retrieves all the default bookmarks stored
     * in the raw file within assets.
     *
     * @param context the context necessary to open assets.
     * @return a non null list of the bookmarks stored in assets.
     */
    @NonNull
    public static List<Bookmark.Entry> importBookmarksFromAssets(@NonNull Context context) {
        List<Bookmark.Entry> bookmarks = new ArrayList<>();
        BufferedReader bookmarksReader = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().openRawResource(R.raw.default_bookmarks);
            //noinspection IOResourceOpenedButNotSafelyClosed
            bookmarksReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bookmarksReader.readLine()) != null) {
                try {
                    JSONObject object = new JSONObject(line);
                    final String folderTitle = object.getString(KEY_FOLDER);
                    bookmarks.add(
                        new Bookmark.Entry(
                            object.getString(KEY_URL),
                            object.getString(KEY_TITLE),
                            object.getInt(KEY_ORDER),
                            WebPageKt.asFolder(folderTitle)
                        )
                    );
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

        return bookmarks;
    }

    /**
     * Exports the list of bookmarks to a file.
     *
     * @param bookmarkList the bookmarks to export.
     * @param file         the file to export to.
     * @return an observable that emits a completion
     * event when the export is complete, or an error
     * event if there is a problem.
     */
    @NonNull
    public static Completable exportBookmarksToFile(@NonNull final List<Bookmark.Entry> bookmarkList,
                                                    @NonNull final File file) {
        return Completable.fromAction(() -> {
            Preconditions.checkNonNull(bookmarkList);
            BufferedWriter bookmarkWriter = null;
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                bookmarkWriter = new BufferedWriter(new FileWriter(file, false));

                JSONObject object = new JSONObject();
                for (Bookmark.Entry item : bookmarkList) {
                    object.put(KEY_TITLE, item.getTitle());
                    object.put(KEY_URL, item.getUrl());
                    object.put(KEY_FOLDER, item.getFolder().getTitle());
                    object.put(KEY_ORDER, item.getPosition());
                    bookmarkWriter.write(object.toString());
                    bookmarkWriter.newLine();
                }
            } finally {
                Utils.close(bookmarkWriter);
            }
        });
    }

    /**
     * Attempts to import bookmarks from the
     * given file. If the file is not in a
     * supported format, it will fail.
     *
     * @param file the file to import from.
     * @return an observable that emits the
     * imported bookmarks, or an error if the
     * file cannot be imported.
     */
    @NonNull
    public static Single<List<Bookmark.Entry>> importBookmarksFromFile(@NonNull final File file) {
        return Single.fromCallable(() -> {
            BufferedReader bookmarksReader = null;
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                bookmarksReader = new BufferedReader(new FileReader(file));
                String line;

                List<Bookmark.Entry> bookmarks = new ArrayList<>();
                while ((line = bookmarksReader.readLine()) != null) {
                    JSONObject object = new JSONObject(line);
                    final String folderName = object.getString(KEY_FOLDER);
                    final Bookmark.Entry entry = new Bookmark.Entry(
                        object.getString(KEY_TITLE),
                        object.getString(KEY_URL),
                        object.getInt(KEY_ORDER),
                        WebPageKt.asFolder(folderName)
                    );
                    bookmarks.add(entry);
                }

                return bookmarks;
            } finally {
                Utils.close(bookmarksReader);
            }
        });
    }

    /**
     * A blocking call that creates a new export file with
     * the name "BookmarkExport.txt" and an appropriate
     * numerical appendage if a file already exists with
     * that name.
     *
     * @return a non null empty file that can be used
     * to export bookmarks to.
     */
    @WorkerThread
    @NonNull
    public static File createNewExportFile() {
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

        return bookmarksExport;
    }

}
