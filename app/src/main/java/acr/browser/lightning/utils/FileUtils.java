package acr.browser.lightning.utils;

import android.app.Application;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import acr.browser.lightning.constant.Constants;

/**
 * A utility class containing helpful methods
 * pertaining to file storage.
 */
public class FileUtils {

    /**
     * Writes a bundle to persistent storage in the files directory
     * using the specified file name. This method is a blocking
     * operation.
     *
     * @param app    the application needed to obtain the file directory.
     * @param bundle the bundle to store in persistent storage.
     * @param name   the name of the file to store the bundle in.
     */
    public static void writeBundleToStorage(Application app, Bundle bundle, String name) {
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
            Log.e(Constants.TAG, "Unable to write bundle to storage");
        } finally {
            Utils.close(outputStream);
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
    public static Bundle readBundleFromStorage(Application app, String name) {
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
            Log.e(Constants.TAG, "Unable to read bundle from storage");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            inputFile.delete();
            Utils.close(inputStream);
        }
        return null;
    }

}
