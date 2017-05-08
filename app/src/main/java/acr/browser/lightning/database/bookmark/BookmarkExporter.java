package acr.browser.lightning.database.bookmark;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

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
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

/**
 * The class responsible for importing and exporting
 * bookmarks in the JSON format.
 * <p>
 * Created by anthonycr on 5/7/17.
 */
public class BookmarkExporter {

    private static final String TAG = "BookmarkExporter";

    private static final String KEY_URL = "url";
    private static final String KEY_TITLE = "title";
    private static final String KEY_FOLDER = "folder";
    private static final String KEY_ORDER = "order";

    /**
     * Retrieves all the default bookmarks stored
     * in the raw file within assets.
     *
     * @param context the context necessary to open assets.
     * @return a non null list of the bookmarks stored in assets.
     */
    @NonNull
    public static List<HistoryItem> importBookmarksFromAssets(@NonNull Context context) {
        List<HistoryItem> bookmarks = new ArrayList<>();
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
                    HistoryItem item = new HistoryItem();
                    item.setTitle(object.getString(KEY_TITLE));
                    final String url = object.getString(KEY_URL);
                    item.setUrl(url);
                    item.setFolder(object.getString(KEY_FOLDER));
                    item.setPosition(object.getInt(KEY_ORDER));
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
    public static Completable exportBookmarksToFile(@NonNull final List<HistoryItem> bookmarkList,
                                                    @NonNull final File file) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull final CompletableSubscriber subscriber) {
                Preconditions.checkNonNull(bookmarkList);
                BufferedWriter bookmarkWriter = null;
                try {
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    bookmarkWriter = new BufferedWriter(new FileWriter(file, false));

                    JSONObject object = new JSONObject();
                    for (HistoryItem item : bookmarkList) {
                        object.put(KEY_TITLE, item.getTitle());
                        object.put(KEY_URL, item.getUrl());
                        object.put(KEY_FOLDER, item.getFolder());
                        object.put(KEY_ORDER, item.getPosition());
                        bookmarkWriter.write(object.toString());
                        bookmarkWriter.newLine();
                    }
                    subscriber.onComplete();
                } catch (@NonNull IOException | JSONException e) {
                    subscriber.onError(e);
                } finally {
                    Utils.close(bookmarkWriter);
                }
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
    public static Single<List<HistoryItem>> importBookmarksFromFile(@NonNull final File file) {
        return Single.create(new SingleAction<List<HistoryItem>>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<List<HistoryItem>> subscriber) {
                BufferedReader bookmarksReader = null;
                try {
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    bookmarksReader = new BufferedReader(new FileReader(file));
                    String line;

                    List<HistoryItem> bookmarks = new ArrayList<>();
                    while ((line = bookmarksReader.readLine()) != null) {
                        JSONObject object = new JSONObject(line);
                        HistoryItem item = new HistoryItem();
                        item.setTitle(object.getString(KEY_TITLE));
                        item.setUrl(object.getString(KEY_URL));
                        item.setFolder(object.getString(KEY_FOLDER));
                        item.setPosition(object.getInt(KEY_ORDER));
                        bookmarks.add(item);
                    }

                    subscriber.onItem(bookmarks);
                    subscriber.onComplete();
                } catch (IOException | JSONException e) {
                    subscriber.onError(e);
                } finally {
                    Utils.close(bookmarksReader);
                }
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
