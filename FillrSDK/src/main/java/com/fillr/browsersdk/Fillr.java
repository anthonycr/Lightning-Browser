/*
 * Copyright 2015-present Pop Tech Pty Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fillr.browsersdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import com.fillr.browsersdk.model.FillrBrowserProperties;
import com.fillr.browsersdk.utilities.FillrUtils;
import com.fillr.browsersdk.FillrAuthenticationStore.WidgetSource;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Created by AlexZhaoBin on 13/05/15.
 */
public final class Fillr {

    public enum BROWSER_TYPE{WEB_KIT,GECKO}

    public interface FillrFormProcessListener {
        void getResult(HashMap<String, String> fieldsWithData, HashMap<String, String> allProfileData);
    }

    public interface FillInitListener {
        void onFillButtonClicked();
        boolean shouldShowFillrToolbar();
    }

    public static final int FILLR_REQUEST_CODE         = 101;
    private static final String MOBILE_BROWSER_WIDGET = "https://d2o8n2jotd2j7i.cloudfront.net/widget/android/sdk/FillrWidget-Android.js.gz";

    private static final String CAMPAIGN_NAME = "https://play.google.com/store/apps/details?id=com.fillr&referrer=utm_source%3D{devkey}%26utm_campaign%3DbrowserSDK";

    private static final String EXTRA_KEY_FIELDS      = "com.fillr.jsonfields";
    private static final String EXTRA_KEY_DEV_KEY     = "com.fillr.devkey";
    private static final String EXTRA_KEY_SDK_PACKAGE = "com.fillr.sdkpackage";

    private static final String EXTRA_KEY_VERSION     = "com.fillr.sdkversion";
    private static final String EXTRA_VALUE_VERSION   = "1.5";
    private static final String EXTRA_KEY_ADDITIONAL_INFO = "com.fillr.additionalinfo";

    public static final String FILLR_PACKAGE_NAME     = "com.fillr";


    private static String javascriptData              = null;
    private static final AsyncHttpClient client       = new AsyncHttpClient();
    private static Fillr fillrInstance = null;

    private FillrBrowserProperties mBrowserProps    = null;


    private WebView mWebView                        = null;
    private Activity parentActivity                 = null;
    private String devKey                           = null;
    private String mPackageName                     = null;
    private BROWSER_TYPE browser_type               = null;
    private FillInitListener fillInitListener       = null;
    private boolean hasSrolled                      = false;

    private Fillr(){
        //private constructor
    }

    public static Fillr getInstance() {
        if (fillrInstance == null) {
            fillrInstance = new Fillr();
        }
        return fillrInstance;
    }

    /**
     *
     * This method is the starting point of integrating the SDK.
     * Call this method with your developer key and your activity
     * @param devKey supplied by Fillr
     * @param parentAct parent activity
     * @param type the type of the browser implementation - webview or gecko
     */
    public final void initialise(String devKey,
                                 Activity parentAct,
                                 BROWSER_TYPE type) {
        initialise(devKey,parentAct,type,null);
    }


    public final void initialise(String devKey,
                                 Activity parentAct,
                                 BROWSER_TYPE type,  FillrBrowserProperties browserInfo) {

        if(parentAct!=null && devKey != null && type!=null) {

            parentActivity      = parentAct;
            this.devKey         = devKey;
            browser_type        = type;
            mBrowserProps       = browserInfo;

            fillrInstance.getWidget(false);

            Context applicationContext = parentActivity.getApplicationContext();
            if(applicationContext!=null) {
                mPackageName = applicationContext.getPackageName();
            }
        }else{
            throw new IllegalArgumentException("Please provide a valid activity, developer key and type");
        }
    }

    /**
     *
     * @return true if the webview that's being tracked has focus
     */
    public boolean webViewHasFocus() {
        if(mWebView!=null) {
            return mWebView.hasFocus();
        }else if(fillInitListener!=null){
            return fillInitListener.shouldShowFillrToolbar();
        }
        return true;
    }

    /**
     *
     *Needs to called every time a @android.wiWebView is attached     *
     * @param webView is attached.
     */
    public void trackWebView(WebView webView) {

        if (!FillrAuthenticationStore.isEnabled(parentActivity)) {
            return;
        }

        if (webView == null) {
            throw new IllegalArgumentException("Invalid webview, your webview instance is null");
        }
        this.mWebView = webView;
        initializeWebViewSettings(this.mWebView.getSettings());
        mWebView.addJavascriptInterface(new JSNativeInterface(), "androidInterface");
    }

    public void trackGeckoView(FillInitListener listener){
        this.fillInitListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initializeWebViewSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
    }

    void showPinScreen() {
        if (browser_type == BROWSER_TYPE.WEB_KIT) {
            injectJavascriptIntoWebView();
        } else if (browser_type == BROWSER_TYPE.GECKO) {
            if (this.fillInitListener!=null ){
                this.fillInitListener.onFillButtonClicked();
            } else {
                throw new ExceptionInInitializerError("provide a fillrinitlistener");
            }
        }
    }

    public String getWidget(final boolean loadWidgetAfterFinish)
    {
        String javascript = null;

        if (getWidgetSource() == WidgetSource.REMOTE)
        {
            javascript = getWidgetInfoFromServer(loadWidgetAfterFinish);
        }
        else
        {
            javascript = getWidgetInfoFromAssets(loadWidgetAfterFinish);
        }

        return javascript;
    }

    /**
     * Downloads the Javascript, and injects it into the current webview
     */
    public void injectJavascriptIntoWebView() {
        if(FillrAuthenticationStore.isEnabled(parentActivity)) {
            String javascript = getWidget(true);

            if (javascript != null) {
                loadWidget();
            }
        }
    }

    /**
     *
     *
     *
     * @param context context object
     * @param origin identifies the caller of this method, used for analytics purposes if available
     */
    public void showDownloadDialog(Context context, final int origin) {

        LayoutInflater inflater     = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        ContextThemeWrapper ctw     = new ContextThemeWrapper( context, R.style.transparent_dialog );
        AlertDialog.Builder builder = new AlertDialog.Builder(ctw);

        View viewCreated            = inflater.inflate(R.layout.com_fillr_dialog_fragment_install_fillr, null);

        Button closeDialog = (Button)viewCreated.findViewById(R.id.id_btn_no);
        Button approveDialog = (Button) viewCreated.findViewById(R.id.id_btn_yes);

        if (mBrowserProps!=null) {
            mBrowserProps.setDialogProps(viewCreated,context);
        }

        closeDialog.setTransformationMethod(null);
        approveDialog.setTransformationMethod(null);

        builder.setView(viewCreated);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        approveDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                downloadFillrApp();
            }
        });
        alertDialog.show();
    }

    public void downloadFillrApp() {
        // Save return package name
        if (parentActivity!=null) {

            String browserPackageName = parentActivity.getApplicationContext().getPackageName();
            setClipboardData("ReturnPackageName", getAdditionalInfo());
            String campaignUrl = CAMPAIGN_NAME.replace("{devkey}",getDeveloperKey());

            try {
                parentActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(campaignUrl)));
            } catch (android.content.ActivityNotFoundException anfe) {

            }
        }
    }

    @SuppressLint("NewApi")
    private void setClipboardData(String key, String value) {
        if (FillrUtils.androidApiHoneycombOrHigher()) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) parentActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(key, value);
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) parentActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(value);
        }
    }

    public class JSNativeInterface {
        @JavascriptInterface
        public void setFields(final String json){
            startProcess(json);
        }
    }

    //step 1
    private final void loadWidget() {
        mWebView.loadUrl("javascript: " + javascriptData);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript: PopWidgetInterface.getFields();");
            }
        }, 100);
    }

    private final String getWidgetInfoFromServer(final boolean loadWidgetAfterFinish) {

        if (javascriptData != null) return javascriptData;

        client.get(MOBILE_BROWSER_WIDGET, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
                String decoded;
                try {
                    decoded = new String(arg2, "UTF-8");
                    javascriptData = decoded;
                    if (loadWidgetAfterFinish) {
                        if (mWebView != null && mWebView.getVisibility() == View.VISIBLE) {
                            loadWidget();
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
        return null;
    }

    private final String getWidgetInfoFromAssets(final boolean loadWidgetAfterFinish) {

        if (javascriptData != null) return javascriptData;

        StringBuilder buf = new StringBuilder();
        BufferedReader in = null;
        InputStream json = null;

        try {
            json = parentActivity.getAssets().open("FillrWidget-Android.js");
            in = new BufferedReader(new InputStreamReader(json, "UTF-8"));

            String str;

            while ((str=in.readLine()) != null) {
                buf.append(str);
            }

            in.close();

            javascriptData = buf.toString();

        } catch (IOException e) {
            javascriptData = null;
        } finally {
            try {
                if (json != null) json.close();
                if (in != null) in.close();
            } catch (IOException ex) {

            }
        }

        if (loadWidgetAfterFinish && javascriptData != null) {
            if (mWebView != null && mWebView.getVisibility() == View.VISIBLE) {
                loadWidget();
            }
        }

        return javascriptData;
    }

    private final String getDeveloperKey() {
        return devKey;
    }

    public void processForm(Intent data) {
        //DO NOT CHANGE THESE
        String payload  = data.getStringExtra("com.fillr.payload");
        String mappings = data.getStringExtra("com.fillr.mappings");

        if (payload != null && mappings != null) {
            mWebView.loadUrl("javascript:PopWidgetInterface.populateWithMappings(JSON.parse('" +
                    mappings.replaceAll("(\\\\t|\\\\n|\\\\r')", " ") + "'), JSON.parse('" + payload + "'));");
        }
    }

    public void startProcess(final String json) {
        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FillrUtils.hideKeyboard(parentActivity);
                // DO NOT CHANGE
                Intent newIntent = buildIntent(json);
                parentActivity.startActivityForResult(newIntent, FILLR_REQUEST_CODE);
            }
        });
    }

    private Intent buildIntent(String json) {
        Intent newIntent = new Intent();
        newIntent.setComponent(new ComponentName("com.fillr", "com.fillr.browsersdk.activities.FillrBSDKProfileDataApproveActivity"));
        newIntent.putExtra(EXTRA_KEY_FIELDS, json);
        newIntent.putExtra(EXTRA_KEY_DEV_KEY, getDeveloperKey());
        newIntent.putExtra(EXTRA_KEY_SDK_PACKAGE, mPackageName);
        newIntent.putExtra(EXTRA_KEY_VERSION, EXTRA_VALUE_VERSION);
        String additionalInfo = getAdditionalInfo();
        if (additionalInfo!=null) {
            newIntent.putExtra(EXTRA_KEY_ADDITIONAL_INFO, additionalInfo);
        }
        return newIntent;
    }

    private String getAdditionalInfo(){

        String retVal = null;
        try {
            JSONObject json = new JSONObject();
            json.put("app_package", mPackageName);
            json.put("developer_key", getDeveloperKey());
            json.put("version", EXTRA_VALUE_VERSION);
            retVal = json.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return retVal;
    }

    Activity getParentActivity() {
        return parentActivity;
    }

    @SuppressLint("NewApi")
    public void onResume() {

        String shouldTrigger = null;

        if (FillrUtils.androidApiHoneycombOrHigher()) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) parentActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = clipboard.getPrimaryClip();
            if (data != null && data.getItemCount() > 0) {
                shouldTrigger = String.valueOf(data.getItemAt(0).getText());
            }
        } else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) parentActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.getText() != null) {
                shouldTrigger = clipboard.getText().toString();
            }
        }

        boolean trigger = shouldTrigger != null && shouldTrigger.equals("com.fillr.load.yes");

        if (parentActivity!=null && mWebView!=null) {
            if (trigger) {
                if (mWebView.getVisibility() == View.VISIBLE) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadWidget();
                        }
                    }, 300);
                }
            }
        } else if (fillInitListener!=null) {
            if (trigger) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fillInitListener.onFillButtonClicked();
                    }
                }, 300);
            }
        }
        setClipboardData("com.fillr.triggerautofill", "");
    }

    public void onPause() {

    }

    public void scrollWebView() {
        if (mWebView != null && mWebView.getVisibility() == View.VISIBLE && !hasSrolled) {
            int x = mWebView.getScrollX();
            int y = mWebView.getScrollY();
            // Hard coded scroll value. Compensates for the fill toolbar.
            // Scrolls the webview down a bit so the focussed field isn't obscured
            y = y + FillrUtils.convertDpToPixels(20);
            if (y < 0) {
                y = 0;
            }
            mWebView.scrollTo(x,y);
            hasSrolled = true;
        }
    }

    public void resetScroll() {
        hasSrolled = false;
    }

    public void setEnabled(boolean value) {
        FillrAuthenticationStore.setEnabled(parentActivity,value);
    }

    public FillrBrowserProperties getBrowserProps() {
        return mBrowserProps;
    }

    public WidgetSource getWidgetSource() {
        return FillrAuthenticationStore.widgetSource(parentActivity);
    }

    public void setWidgetSource(WidgetSource source) {
        FillrAuthenticationStore.setWidgetSource(parentActivity,source);
    }
}
