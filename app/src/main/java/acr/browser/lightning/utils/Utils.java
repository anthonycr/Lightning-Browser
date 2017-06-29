/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import acr.browser.lightning.R;
import acr.browser.lightning.MainActivity;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.dialog.BrowserDialog;

public final class Utils {

    private static final String TAG = "Utils";

    public static boolean doesSupportHeaders() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Creates a new intent that can launch the email
     * app with a subject, address, body, and cc. It
     * is used to handle mail:to links.
     *
     * @param address the address to send the email to.
     * @param subject the subject of the email.
     * @param body    the body of the email.
     * @param cc      extra addresses to CC.
     * @return a valid intent.
     */
    @NonNull
    public static Intent newEmailIntent(String address, String subject,
                                        String body, String cc) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_CC, cc);
        intent.setType("message/rfc822");
        return intent;
    }

    /**
     * Creates a dialog with only a title, message, and okay button.
     *
     * @param activity the activity needed to create a dialog.
     * @param title    the title of the dialog.
     * @param message  the message of the dialog.
     */
    public static void createInformativeDialog(@NonNull Activity activity, @StringRes int title, @StringRes int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message)
            .setCancelable(true)
            .setPositiveButton(activity.getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
        BrowserDialog.setDialogSize(activity, alert);
    }

    /**
     * Displays a snackbar to the user with a String resource.
     * <p>
     * NOTE: If there is an accessibility manager enabled on
     * the device, such as LastPass, then the snackbar animations
     * will not work.
     *
     * @param activity the activity needed to create a snackbar.
     * @param resource the string resource to show to the user.
     */
    public static void showSnackbar(@NonNull Activity activity, @StringRes int resource) {
        View view = activity.findViewById(android.R.id.content);
        if (view == null) {
            Log.e(TAG, "showSnackbar", new NullPointerException("Unable to find android.R.id.content"));
            return;
        }
        Snackbar.make(view, resource, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Displays a snackbar to the user with a string message.
     * <p>
     * NOTE: If there is an accessibility manager enabled on
     * the device, such as LastPass, then the snackbar animations
     * will not work.
     *
     * @param activity the activity needed to create a snackbar.
     * @param message  the string message to show to the user.
     */
    public static void showSnackbar(@NonNull Activity activity, @NonNull String message) {
        View view = activity.findViewById(android.R.id.content);
        if (view == null) {
            Log.e(TAG, "showSnackbar", new NullPointerException("Unable to find android.R.id.content"));
            return;
        }
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Shows a toast to the user.
     * Should only be used if an activity is
     * not available to show a snackbar.
     *
     * @param context  the context needed to show the toast.
     * @param resource the string shown by the toast to the user.
     */
    public static void showToast(@NonNull Context context, @StringRes int resource) {
        Toast.makeText(context, resource, Toast.LENGTH_SHORT).show();
    }

    /**
     * Converts Density Pixels (DP) to Pixels (PX).
     *
     * @param dp the number of density pixels to convert.
     * @return the number of pixels that the conversion generates.
     */
    public static int dpToPx(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return (int) (dp * metrics.density + 0.5f);
    }

    /**
     * Extracts the domain name from a URL.
     *
     * @param url the URL to extract the domain from.
     * @return the domain name, or the URL if the domain
     * could not be extracted. The domain name may include
     * HTTPS if the URL is an SSL supported URL.
     */
    @Nullable
    public static String getDomainName(@Nullable String url) {
        if (url == null || url.isEmpty()) return "";

        boolean ssl = URLUtil.isHttpsUrl(url);
        int index = url.indexOf('/', 8);
        if (index != -1) {
            url = url.substring(0, index);
        }

        URI uri;
        String domain;
        try {
            uri = new URI(url);
            domain = uri.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            domain = null;
        }

        if (domain == null || domain.isEmpty()) {
            return url;
        }
        if (ssl)
            return Constants.HTTPS + domain;
        else
            return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public static void trimCache(@NonNull Context context) {
        try {
            File dir = context.getCacheDir();

            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception ignored) {

        }
    }

    private static boolean deleteDir(@Nullable File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir != null && dir.delete();
    }

    /**
     * Creates and returns a new favicon which is the same as the provided
     * favicon but with horizontal or vertical padding of 4dp
     *
     * @param bitmap is the bitmap to pad.
     * @return the padded bitmap.
     */
    @NonNull
    public static Bitmap padFavicon(@NonNull Bitmap bitmap) {
        int padding = Utils.dpToPx(4);

        Bitmap paddedBitmap = Bitmap.createBitmap(bitmap.getWidth() + padding, bitmap.getHeight()
            + padding, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawARGB(0x00, 0x00, 0x00, 0x00); // this represents white color
        canvas.drawBitmap(bitmap, padding / 2, padding / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return paddedBitmap;
    }

    public static boolean isColorTooDark(int color) {
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        //final byte BLUE_CHANNEL = 0;

        int r = ((int) ((float) (color >> RED_CHANNEL & 0xff) * 0.3f)) & 0xff;
        int g = ((int) ((float) (color >> GREEN_CHANNEL & 0xff) * 0.59)) & 0xff;
        int b = ((int) ((float) (color /* >> BLUE_CHANNEL */ & 0xff) * 0.11)) & 0xff;
        int gr = (r + g + b) & 0xff;
        int gray = gr /* << BLUE_CHANNEL */ + (gr << GREEN_CHANNEL) + (gr << RED_CHANNEL);

        return gray < 0x727272;
    }

    public static int mixTwoColors(int color1, int color2, float amount) {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        //final byte BLUE_CHANNEL = 0;

        final float inverseAmount = 1.0f - amount;

        int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) + ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) + ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int b = ((int) (((float) (color1 & 0xff) * amount) + ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

        return 0xff << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b;
    }

    @SuppressLint("SimpleDateFormat")
    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + '_';
        File storageDir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        );
    }

    /**
     * Checks if flash player is installed
     *
     * @param context the context needed to obtain the PackageManager
     * @return true if flash is installed, false otherwise
     */
    public static boolean isFlashInstalled(@NonNull Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo("com.adobe.flashplayer", 0);
            if (ai != null) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return false;
    }

    /**
     * Quietly closes a closeable object like an InputStream or OutputStream without
     * throwing any errors or requiring you do do any checks.
     *
     * @param closeable the object to close
     */
    public static void close(@Nullable Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method to close cursors. Cursor did not
     * implement Closeable until API 16, so using this
     * method for when we want to close a cursor.
     *
     * @param cursor the cursor to close
     */
    public static void close(@Nullable Cursor cursor) {
        if (cursor == null) {
            return;
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Draws the trapezoid background for the horizontal tabs on a canvas object using
     * the specified color.
     *
     * @param canvas the canvas to draw upon
     * @param color  the color to use to draw the tab
     */
    public static void drawTrapezoid(@NonNull Canvas canvas, int color, boolean withShader) {

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
//        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        paint.setDither(true);
        if (withShader) {
            paint.setShader(new LinearGradient(0, 0.9f * canvas.getHeight(),
                0, canvas.getHeight(),
                color, mixTwoColors(Color.BLACK, color, 0.5f),
                Shader.TileMode.CLAMP));
        } else {
            paint.setShader(null);
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        double radians = Math.PI / 3;
        int base = (int) (height / Math.tan(radians));

        Path wallpath = new Path();
        wallpath.reset();
        wallpath.moveTo(0, height);
        wallpath.lineTo(width, height);
        wallpath.lineTo(width - base, 0);
        wallpath.lineTo(base, 0);
        wallpath.close();

        canvas.drawPath(wallpath, paint);
    }

    /**
     * Creates a shortcut on the homescreen using the
     * {@link HistoryItem} information that opens the
     * browser. The icon, URL, and title are used in
     * the creation of the shortcut.
     *
     * @param activity the activity needed to create
     *                 the intent and show a snackbar message
     * @param item     the HistoryItem to create the shortcut from
     */
    public static void createShortcut(@NonNull Activity activity, @NonNull HistoryItem item) {
        if (TextUtils.isEmpty(item.getUrl())) {
            return;
        }
        Log.d(TAG, "Creating shortcut: " + item.getTitle() + ' ' + item.getUrl());
        Intent shortcutIntent = new Intent(activity, MainActivity.class);
        shortcutIntent.setData(Uri.parse(item.getUrl()));

        final String title = TextUtils.isEmpty(item.getTitle()) ? activity.getString(R.string.untitled) : item.getTitle();

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, item.getBitmap());
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        activity.sendBroadcast(addIntent);
        Utils.showSnackbar(activity, R.string.message_added_to_homescreen);
    }

    public static int calculateInSampleSize(@NonNull BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Nullable
    public static String guessFileExtension(@NonNull String filename) {
        int lastIndex = filename.lastIndexOf('.') + 1;
        if (lastIndex > 0 && filename.length() > lastIndex) {
            return filename.substring(lastIndex, filename.length());
        }
        return null;
    }

}
