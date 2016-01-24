package acr.browser.lightning.view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.squareup.otto.Bus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.controller.UIController;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import acr.browser.lightning.utils.Utils;

/**
 * @author Stefano Pacifici based on Anthony C. Restaino code
 * @date 2015/09/21
 */
class LightningChromeClient extends WebChromeClient {

    private static final String[] PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

    private final Activity mActivity;
    private final LightningView mLightningView;
    private final UIController mUIController;
    private final Bus eventBus;

    LightningChromeClient(Activity activity, LightningView lightningView) {
        mActivity = activity;
        mUIController = (UIController) activity;
        mLightningView = lightningView;
        eventBus = BrowserApp.getBus(activity);
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (mLightningView.isShown()) {
            mUIController.updateProgress(newProgress);
        }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        mLightningView.getTitleInfo().setFavicon(icon);
        eventBus.post(new BrowserEvents.TabsChanged());
        cacheFavicon(view.getUrl(), icon, mActivity);
    }

    /**
     * Naive caching of the favicon according to the domain name of the URL
     *
     * @param icon the icon to cache
     */
    private static void cacheFavicon(final String url, final Bitmap icon, final Context context) {
        if (icon == null) return;
        final Uri uri = Uri.parse(url);
        if (uri.getHost() == null) {
            return;
        }
        BrowserApp.getIOThread().execute(new IconCacheTask(uri, icon, BrowserApp.get(context)));
    }


    @Override
    public void onReceivedTitle(WebView view, String title) {
        if (title != null && !title.isEmpty()) {
            mLightningView.getTitleInfo().setTitle(title);
        } else {
            mLightningView.getTitleInfo().setTitle(mActivity.getString(R.string.untitled));
        }
        eventBus.post(new BrowserEvents.TabsChanged());
        if (view != null) {
            mUIController.updateHistory(title, view.getUrl());
        }
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(final String origin,
                                                   final GeolocationPermissions.Callback callback) {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(mActivity, PERMISSIONS, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                final boolean remember = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setTitle(mActivity.getString(R.string.location));
                String org;
                if (origin.length() > 50) {
                    org = origin.subSequence(0, 50) + "...";
                } else {
                    org = origin;
                }
                builder.setMessage(org + mActivity.getString(R.string.message_location))
                        .setCancelable(true)
                        .setPositiveButton(mActivity.getString(R.string.action_allow),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        callback.invoke(origin, true, remember);
                                    }
                                })
                        .setNegativeButton(mActivity.getString(R.string.action_dont_allow),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        callback.invoke(origin, false, remember);
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void onDenied(String permission) {
                //TODO show message and/or turn off setting
            }
        });
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                  Message resultMsg) {
        mUIController.onCreateWindow(resultMsg);
        return true;
    }

    @Override
    public void onCloseWindow(WebView window) {
        mUIController.onCloseWindow(mLightningView);
    }

    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        mUIController.openFileChooser(uploadMsg);
    }

    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        mUIController.openFileChooser(uploadMsg);
    }

    @SuppressWarnings("unused")
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        mUIController.openFileChooser(uploadMsg);
    }

    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                     WebChromeClient.FileChooserParams fileChooserParams) {
        mUIController.showFileChooser(filePathCallback);
        return true;
    }

    /**
     * Obtain an image that is displayed as a placeholder on a video until the video has initialized
     * and can begin loading.
     *
     * @return a Bitmap that can be used as a place holder for videos.
     */
    @Override
    public Bitmap getDefaultVideoPoster() {
        if (mActivity == null) {
            return null;
        }
        final Resources resources = mActivity.getResources();
        return BitmapFactory.decodeResource(resources, android.R.drawable.spinner_background);
    }

    /**
     * Inflate a view to send to a LightningView when it needs to display a video and has to
     * show a loading dialog. Inflates a progress view and returns it.
     *
     * @return A view that should be used to display the state
     * of a video's loading progress.
     */
    @Override
    public View getVideoLoadingProgressView() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        return inflater.inflate(R.layout.video_loading_progress, null);
    }

    @Override
    public void onHideCustomView() {
        mUIController.onHideCustomView();
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        mUIController.onShowCustomView(view, callback);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onShowCustomView(View view, int requestedOrientation,
                                 CustomViewCallback callback) {
        mUIController.onShowCustomView(view, callback, requestedOrientation);
    }
}
