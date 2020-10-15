/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.database.HistoryEntry;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.extensions.ActivityExtensions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableKt;

public final class Utils {

    private static final String TAG = "Utils";

    private Utils() {}

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
                (dialog, id) -> {
                });
        AlertDialog alert = builder.create();
        alert.show();
        BrowserDialog.setDialogSize(activity, alert);
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
     * NOTE: Should be used for display only.
     *
     * @param url the URL to extract the domain from.
     * @return the domain name, or the URL if the domain
     * could not be extracted. The domain name may include
     * HTTPS if the URL is an SSL supported URL.
     */
    @NonNull
    public static String getDisplayDomainName(@Nullable String url) {
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
            Log.e(TAG, "Unable to parse URI", e);
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
     * Quietly closes a closeable object like an InputStream or OutputStream without
     * throwing any errors or requiring you do do any checks.
     *
     * @param closeable the object to close
     */
    public static void close(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to close closeable", e);
        }
    }

    /**
     * Creates a shortcut on the homescreen using the
     * {@link HistoryEntry} information that opens the
     * browser. The icon, URL, and title are used in
     * the creation of the shortcut.
     *
     * @param activity     the activity needed to create
     *                     the intent and show a snackbar message
     * @param historyEntry the HistoryEntity to create the shortcut from
     */
    public static void createShortcut(@NonNull Activity activity,
                                      @NonNull HistoryEntry historyEntry,
                                      @NonNull Bitmap favicon) {
        createShortcut(activity, historyEntry.getUrl(), historyEntry.getTitle(), favicon);
    }

    public static void createShortcut(@NonNull Activity activity,
                                      @NonNull String url,
                                      @NonNull String unsafeTitle,
                                      @Nullable Bitmap unsafeFavicon) {
        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
        shortcutIntent.setData(Uri.parse(url));

        final String title = TextUtils.isEmpty(unsafeTitle) ? activity.getString(R.string.untitled) : unsafeTitle;
        final Drawable webPageDrawable = ContextCompat.getDrawable(activity, R.drawable.ic_webpage);
        Preconditions.checkNonNull(webPageDrawable);
        final Bitmap webPageBitmap = DrawableKt.toBitmap(webPageDrawable,
            webPageDrawable.getIntrinsicWidth(),
            webPageDrawable.getIntrinsicHeight(), null);

        final Bitmap favicon = unsafeFavicon != null ? unsafeFavicon : webPageBitmap;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, favicon);
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            activity.sendBroadcast(addIntent);
            ActivityExtensions.snackbar(activity, R.string.message_added_to_homescreen);
        } else {
            ShortcutManager shortcutManager = activity.getSystemService(ShortcutManager.class);
            if (shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo pinShortcutInfo =
                    new ShortcutInfo.Builder(activity, "browser-shortcut-" + url.hashCode())
                        .setIntent(shortcutIntent)
                        .setIcon(Icon.createWithBitmap(favicon))
                        .setShortLabel(title)
                        .build();

                shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                ActivityExtensions.snackbar(activity, R.string.message_added_to_homescreen);
            } else {
                ActivityExtensions.snackbar(activity, R.string.shortcut_message_failed_to_add);
            }
        }
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
            return filename.substring(lastIndex);
        }
        return null;
    }

}
