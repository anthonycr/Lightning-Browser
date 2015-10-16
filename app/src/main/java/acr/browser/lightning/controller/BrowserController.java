/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.controller;

import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;

import acr.browser.lightning.view.LightningView;

public interface BrowserController {

    void updateUrl(String title, boolean shortUrl);

    void updateProgress(int n);

    void updateHistory(String title, String url);

    void openFileChooser(ValueCallback<Uri> uploadMsg);

//    void onLongPress();

    void onShowCustomView(View view, CustomViewCallback callback);

    void onHideCustomView();

    void onCreateWindow(Message resultMsg);

    void onCloseWindow(LightningView view);

    void hideActionBar();

    void showActionBar();

    // void longClickPage(String url);

    void showFileChooser(ValueCallback<Uri[]> filePathCallback);

    void closeEmptyTab();

   //  void updateBookmarkIndicator(String url);

}
