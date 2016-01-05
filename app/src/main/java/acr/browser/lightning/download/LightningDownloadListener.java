/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;

import acr.browser.lightning.R;
import acr.browser.lightning.constant.Constants;
import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import java.text.MessageFormat;

public class LightningDownloadListener implements DownloadListener {

    private final Activity mActivity;

    public LightningDownloadListener(Activity context) {
        mActivity = context;
    }

    @Override
    public void onDownloadStart(final String url, final String userAgent,
                                final String contentDisposition, final String mimetype, final long contentLength) {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(mActivity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        DownloadHandler.onDownloadStart(mActivity, url, userAgent,
                                                contentDisposition, mimetype);
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        break;
                                }
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
                        String downloadSize = null;
                        if (contentLength > 0) {
                            downloadSize = Formatter.formatFileSize(mActivity, contentLength);
                        } else {
                            downloadSize = mActivity.getResources().getString(R.string.unknown);
                        }
                        String message = mActivity.getResources().getString(R.string.dialog_download);
                        message = MessageFormat.format(message, downloadSize);
                        builder.setTitle(fileName)
                                .setMessage(message)
                                .setPositiveButton(mActivity.getResources().getString(R.string.action_download),
                                        dialogClickListener)
                                .setNegativeButton(mActivity.getResources().getString(R.string.action_cancel),
                                        dialogClickListener).show();
                        Log.i(Constants.TAG, "Downloading" + fileName);
                    }

                    @Override
                    public void onDenied(String permission) {
                        //TODO show message
                    }
                });
    }
}
