package acr.browser.lightning.utils;

import android.app.Application;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.anthonycr.bonsai.Schedulers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * A utility class containing helpful methods
 * pertaining to file storage.
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    public static final String DEFAULT_DOWNLOAD_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

    /**
     * Writes a bundle to persistent storage in the files directory
     * using the specified file name. This method is a blocking
     * operation.
     *
     * @param app    the application needed to obtain the file directory.
     * @param bundle the bundle to store in persistent storage.
     * @param name   the name of the file to store the bundle in.
     */
    public static void writeBundleToStorage(final @NonNull Application app, final Bundle bundle, final @NonNull String name) {
        Schedulers.io().execute(new Runnable() {
            @Override
            public void run() {
                File outputFile = new File(app.getFilesDir(), name);
                FileOutputStream outputStream = null;
                try {
                    //noinspection IOResourceOpenedButNotSafelyClosed
                    outputStream = new FileOutputStream(outputFile);
                    Parcel parcel = Parcel.obtain();
                    parcel.writeBundle(bundle);
                    outputStream.write(parcel.marshall());
                    outputStream.flush();
                    parcel.recycle();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to write bundle to storage");
                } finally {
                    Utils.close(outputStream);
                }
            }
        });
    }

    /**
     * Use this method to delete the bundle with the specified name.
     * This is a blocking call and should be used within a worker
     * thread unless immediate deletion is necessary.
     *
     * @param app  the application object needed to get the file.
     * @param name the name of the file.
     */
    public static void deleteBundleInStorage(final @NonNull Application app, final @NonNull String name) {
        File outputFile = new File(app.getFilesDir(), name);
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    /**
     * Reads a bundle from the file with the specified
     * name in the peristent storage files directory.
     * This method is a blocking operation.
     *
     * @param app  the application needed to obtain the files directory.
     * @param name the name of the file to read from.
     * @return a valid Bundle loaded using the system class loader
     * or null if the method was unable to read the Bundle from storage.
     */
    @Nullable
    public static Bundle readBundleFromStorage(@NonNull Application app, @NonNull String name) {
        File inputFile = new File(app.getFilesDir(), name);
        FileInputStream inputStream = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            inputStream = new FileInputStream(inputFile);
            Parcel parcel = Parcel.obtain();
            byte[] data = new byte[(int) inputStream.getChannel().size()];

            //noinspection ResultOfMethodCallIgnored
            inputStream.read(data, 0, data.length);
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            Bundle out = parcel.readBundle(ClassLoader.getSystemClassLoader());
            out.putAll(out);
            parcel.recycle();
            return out;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to read bundle from storage");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            inputFile.delete();
            Utils.close(inputStream);
        }
        return null;
    }

    /**
     * Writes a stacktrace to the downloads folder with
     * the following filename: [EXCEPTION]_[TIME OF CRASH IN MILLIS].txt
     *
     * @param throwable the Throwable to log to external storage
     */
    public static void writeCrashToStorage(@NonNull Throwable throwable) {
        String fileName = throwable.getClass().getSimpleName() + '_' + System.currentTimeMillis() + ".txt";
        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        FileOutputStream outputStream = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            outputStream = new FileOutputStream(outputFile);
            throwable.printStackTrace(new PrintStream(outputStream));
            outputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Unable to write bundle to storage");
        } finally {
            Utils.close(outputStream);
        }
    }

    @NonNull
    public static String readStringFromStream(@NonNull InputStream inputStream,
                                              @NonNull String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    /**
     * Converts megabytes to bytes.
     *
     * @param megaBytes the number of megabytes.
     * @return the converted bytes.
     */
    public static long megabytesToBytes(long megaBytes) {
        return megaBytes * 1024 * 1024;
    }

    /**
     * Determine whether there is write access in the given directory. Returns false if a
     * file cannot be created in the directory or if the directory does not exist.
     *
     * @param directory the directory to check for write access
     * @return returns true if the directory can be written to or is in a directory that can
     * be written to. false if there is no write access.
     */
    public static boolean isWriteAccessAvailable(@Nullable String directory) {
        if (directory == null || directory.isEmpty()) {
            return false;
        }

        final String sFileName = "test";
        final String sFileExtension = ".txt";
        String dir = addNecessarySlashes(directory);
        dir = getFirstRealParentDirectory(dir);
        File file = new File(dir + sFileName + sFileExtension);
        for (int n = 0; n < 100; n++) {
            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                    return true;
                } catch (IOException ignored) {
                    return false;
                }
            } else {
                file = new File(dir + sFileName + '-' + n + sFileExtension);
            }
        }
        return file.canWrite();
    }

    /**
     * Returns the first parent directory of a directory that exists. This is useful
     * for subdirectories that do not exist but their parents do.
     *
     * @param directory the directory to find the first existent parent
     * @return the first existent parent
     */
    @Nullable
    private static String getFirstRealParentDirectory(@Nullable String directory) {
        while (true) {
            if (directory == null || directory.isEmpty()) {
                return "/";
            }
            directory = addNecessarySlashes(directory);
            File file = new File(directory);
            if (!file.isDirectory()) {
                int indexSlash = directory.lastIndexOf('/');
                if (indexSlash > 0) {
                    String parent = directory.substring(0, indexSlash);
                    int previousIndex = parent.lastIndexOf('/');
                    if (previousIndex > 0) {
                        directory = parent.substring(0, previousIndex);
                    } else {
                        return "/";
                    }
                } else {
                    return "/";
                }
            } else {
                return directory;
            }
        }
    }

    @NonNull
    public static String addNecessarySlashes(@Nullable String originalPath) {
        if (originalPath == null || originalPath.length() == 0) {
            return "/";
        }
        if (originalPath.charAt(originalPath.length() - 1) != '/') {
            originalPath = originalPath + '/';
        }
        if (originalPath.charAt(0) != '/') {
            originalPath = '/' + originalPath;
        }
        return originalPath;
    }
}
