/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.BuildConfig;
import acr.browser.lightning.R;
import acr.browser.lightning.browser.DefaultBrowserActivity;
import acr.browser.lightning.browser.di.MainScheduler;
import acr.browser.lightning.browser.di.NetworkScheduler;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.dialog.BrowserDialog;
import acr.browser.lightning.extensions.ActivityExtensions;
import acr.browser.lightning.log.Logger;
import acr.browser.lightning.preference.UserPreferences;
import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

/**
 * Handle download requests
 */
@Singleton
public class DownloadHandler {

    private static final String TAG = "DownloadHandler";

    private static final String COOKIE_REQUEST_HEADER = "Cookie";

    private final DownloadManager downloadManager;
    private final Scheduler networkScheduler;
    private final Scheduler mainScheduler;
    private final Logger logger;

    @Inject
    public DownloadHandler(DownloadManager downloadManager,
                           @NetworkScheduler Scheduler networkScheduler,
                           @MainScheduler Scheduler mainScheduler,
                           Logger logger) {
        this.downloadManager = downloadManager;
        this.networkScheduler = networkScheduler;
        this.mainScheduler = mainScheduler;
        this.logger = logger;
    }

    /**
     * Notify the host application a download should be done, or that the data
     * should be streamed if a streaming viewer is available.
     *
     * @param context            The context in which the download was requested.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimeType           The mimeType of the content reported by the server
     * @param contentSize        The size of the content
     */
    public void onDownloadStart(@NonNull Activity context,
                                @NonNull UserPreferences manager,
                                @NonNull String url, String userAgent,
                                @Nullable String contentDisposition,
                                @Nullable String mimeType,
                                @NonNull String contentSize) {

        logger.log(TAG, "DOWNLOAD: Trying to download from URL: " + url);
        logger.log(TAG, "DOWNLOAD: Content disposition: " + contentDisposition);
        logger.log(TAG, "DOWNLOAD: MimeType: " + mimeType);
        logger.log(TAG, "DOWNLOAD: User agent: " + userAgent);

        // if we're dealing wih A/V content that's not explicitly marked
        // for download, check if it's streamable.
        if (contentDisposition == null
            || !contentDisposition.regionMatches(true, 0, "attachment", 0, 10)) {
            // query the package manager to see if there's a registered handler
            // that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            intent.setSelector(null);
            ResolveInfo info = context.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                // If we resolved to ourselves, we don't want to attempt to
                // load the url only to try and download it again.
                if (BuildConfig.APPLICATION_ID.equals(info.activityInfo.packageName)
                    || DefaultBrowserActivity.class.getName().equals(info.activityInfo.name)) {
                    // someone (other than us) knows how to handle this mime
                    // type with this scheme, don't download.
                    try {
                        context.startActivity(intent);
                        return;
                    } catch (ActivityNotFoundException ex) {
                        // Best behavior is to fall back to a download in this
                        // case
                    }
                }
            }
        }
        onDownloadStartNoStream(context, manager, url, userAgent, contentDisposition, mimeType, contentSize);
    }

    // This is to work around the fact that java.net.URI throws Exceptions
    // instead of just encoding URL's properly
    // Helper method for onDownloadStartNoStream
    @NonNull
    private static String encodePath(@NonNull String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (!needed) {
            return path;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Notify the host application a download should be done, even if there is a
     * streaming viewer available for thise type.
     *
     * @param context            The context in which the download is requested.
     * @param url                The full url to the content that should be downloaded
     * @param userAgent          User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype           The mimetype of the content reported by the server
     * @param contentSize        The size of the content
     */
    /* package */
    private void onDownloadStartNoStream(@NonNull final Activity context,
                                         @NonNull UserPreferences preferences,
                                         @NonNull String url, String userAgent,
                                         @Nullable String contentDisposition,
                                         @Nullable String mimetype,
                                         @NonNull String contentSize) {
        final String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);

        // Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = context.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = context.getString(R.string.download_no_sdcard_dlg_msg);
                title = R.string.download_no_sdcard_dlg_title;
            }

            Dialog dialog = new AlertDialog.Builder(context).setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert).setMessage(msg)
                .setPositiveButton(R.string.action_ok, null).show();
            BrowserDialog.setDialogSize(context, dialog);
            return;
        }

        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        WebAddress webAddress;
        try {
            webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
        } catch (Exception e) {
            // This only happens for very bad urls, we want to catch the
            // exception here
            logger.log(TAG, "Exception while trying to parse url '" + url + '\'', e);
            ActivityExtensions.snackbar(context, R.string.problem_download);
            return;
        }

        String addressString = webAddress.toString();
        Uri uri = Uri.parse(addressString);
        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(uri);
        } catch (IllegalArgumentException e) {
            ActivityExtensions.snackbar(context, R.string.cannot_download);
            return;
        }

        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs
        // depending on mimetype?
        String location = preferences.getDownloadDirectory();
        location = FileUtils.addNecessarySlashes(location);
        Uri downloadFolder = Uri.parse(location);

        if (!isWriteAccessAvailable(downloadFolder)) {
            ActivityExtensions.snackbar(context, R.string.problem_location_download);
            return;
        }
        String newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Utils.guessFileExtension(filename));
        logger.log(TAG, "New mimetype: " + newMimeType);
        request.setMimeType(newMimeType);
        request.setDestinationUri(Uri.parse(Constants.FILE + location + filename));
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.setVisibleInDownloadsUi(true);
        request.allowScanningByMediaScanner();
        request.setDescription(webAddress.getHost());
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader(COOKIE_REQUEST_HEADER, cookies);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        //noinspection VariableNotUsedInsideIf
        if (mimetype == null) {
            logger.log(TAG, "Mimetype is null");
            if (TextUtils.isEmpty(addressString)) {
                return;
            }
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            final Disposable disposable = new FetchUrlMimeType(downloadManager, request, addressString, cookies, userAgent)
                .create()
                .subscribeOn(networkScheduler)
                .observeOn(mainScheduler)
                .subscribe(result -> {
                    switch (result) {
                        case FAILURE_ENQUEUE:
                            ActivityExtensions.snackbar(context, R.string.cannot_download);
                            break;
                        case FAILURE_LOCATION:
                            ActivityExtensions.snackbar(context, R.string.problem_location_download);
                            break;
                        case SUCCESS:
                            ActivityExtensions.snackbar(context, R.string.download_pending);
                            break;
                    }
                });
        } else {
            logger.log(TAG, "Valid mimetype, attempting to download");
            try {
                downloadManager.enqueue(request);
            } catch (IllegalArgumentException e) {
                // Probably got a bad URL or something
                logger.log(TAG, "Unable to enqueue request", e);
                ActivityExtensions.snackbar(context, R.string.cannot_download);
            } catch (SecurityException e) {
                // TODO write a download utility that downloads files rather than rely on the system
                // because the system can only handle Environment.getExternal... as a path
                ActivityExtensions.snackbar(context, R.string.problem_location_download);
            }
            ActivityExtensions.snackbar(context, context.getString(R.string.download_pending) + ' ' + filename);
        }
    }

    private static boolean isWriteAccessAvailable(@NonNull Uri fileUri) {
        if (fileUri.getPath() == null) {
            return false;
        }
        File file = new File(fileUri.getPath());

        if (!file.isDirectory() && !file.mkdirs()) {
            return false;
        }

        try {
            if (file.createNewFile()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
