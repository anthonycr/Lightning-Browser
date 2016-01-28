package acr.browser.lightning.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.squareup.otto.Bus;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.bus.BrowserEvents;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.controller.UIController;
import acr.browser.lightning.utils.AdBlock;
import acr.browser.lightning.utils.IntentUtils;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.utils.Utils;

public class LightningWebClient extends WebViewClient {


    @NonNull private final Activity mActivity;
    @NonNull private final LightningView mLightningView;
    @NonNull private final UIController mUIController;
    @NonNull private final Bus mEventBus;
    @NonNull private final IntentUtils mIntentUtils;

    @Inject ProxyUtils mProxyUtils;
    @Inject AdBlock mAdBlock;

    LightningWebClient(@NonNull Activity activity, @NonNull LightningView lightningView) {
        BrowserApp.getAppComponent().inject(this);
        Preconditions.checkNonNull(activity);
        Preconditions.checkNonNull(lightningView);
        mActivity = activity;
        mUIController = (UIController) activity;
        mLightningView = lightningView;
        mAdBlock.updatePreference();
        mEventBus = BrowserApp.getBus(activity);
        mIntentUtils = new IntentUtils(activity);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request) {
        if (mAdBlock.isAd(request.getUrl().toString())) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (mAdBlock.isAd(url)) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onPageFinished(@NonNull WebView view, String url) {
        if (view.isShown()) {
            mUIController.updateUrl(url, true);
            view.postInvalidate();
        }
        if (view.getTitle() == null || view.getTitle().isEmpty()) {
            mLightningView.getTitleInfo().setTitle(mActivity.getString(R.string.untitled));
        } else {
            mLightningView.getTitleInfo().setTitle(view.getTitle());
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                mLightningView.getInvertePage()) {
            view.evaluateJavascript(Constants.JAVASCRIPT_INVERT_PAGE, null);
        }
        mEventBus.post(new BrowserEvents.TabsChanged());
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        mLightningView.getTitleInfo().setFavicon(null);
        if (mLightningView.isShown()) {
            mUIController.updateUrl(url, false);
            mUIController.showActionBar();
        }
        mEventBus.post(new BrowserEvents.TabsChanged());
    }

    @Override
    public void onReceivedHttpAuthRequest(final WebView view, @NonNull final HttpAuthHandler handler,
                                          final String host, final String realm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        final EditText name = new EditText(mActivity);
        final EditText password = new EditText(mActivity);
        LinearLayout passLayout = new LinearLayout(mActivity);
        passLayout.setOrientation(LinearLayout.VERTICAL);

        passLayout.addView(name);
        passLayout.addView(password);

        name.setHint(mActivity.getString(R.string.hint_username));
        name.setSingleLine();
        password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSingleLine();
        password.setTransformationMethod(new PasswordTransformationMethod());
        password.setHint(mActivity.getString(R.string.hint_password));
        builder.setTitle(mActivity.getString(R.string.title_sign_in));
        builder.setView(passLayout);
        builder.setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.title_sign_in),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String user = name.getText().toString();
                                String pass = password.getText().toString();
                                handler.proceed(user.trim(), pass.trim());
                                Log.d(Constants.TAG, "Request Login");

                            }
                        })
                .setNegativeButton(mActivity.getString(R.string.action_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handler.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private boolean mIsRunning = false;
    private float mZoomScale = 0.0f;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onScaleChanged(@NonNull final WebView view, final float oldScale, final float newScale) {
        if (view.isShown() && mLightningView.mPreferences.getTextReflowEnabled() &&
                Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mIsRunning)
                return;
            if (Math.abs(mZoomScale - newScale) > 0.01f) {
                mIsRunning = view.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mZoomScale = newScale;
                        view.evaluateJavascript(Constants.JAVASCRIPT_TEXT_REFLOW, null);
                        mIsRunning = false;
                    }

                }, 100);
            }

        }
    }

    @NonNull
    private static List<Integer> getAllSslErrorMessageCodes(@NonNull SslError error) {
        List<Integer> errorCodeMessageCodes = new ArrayList<>();

        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_date_invalid);
        }
        if (error.hasError(SslError.SSL_EXPIRED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_expired);
        }
        if (error.hasError(SslError.SSL_IDMISMATCH)) {
            errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch);
        }
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_not_yet_valid);
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_untrusted);
        }
        if (error.hasError(SslError.SSL_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_invalid);
        }

        return errorCodeMessageCodes;
    }

    @Override
    public void onReceivedSslError(WebView view, @NonNull final SslErrorHandler handler, @NonNull SslError error) {
        List<Integer> errorCodeMessageCodes = getAllSslErrorMessageCodes(error);

        StringBuilder stringBuilder = new StringBuilder();
        for (Integer messageCode : errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(mActivity.getString(messageCode)).append('\n');
        }
        String alertMessage =
                mActivity.getString(R.string.message_insecure_connection, stringBuilder.toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.title_warning));
        builder.setMessage(alertMessage)
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handler.proceed();
                            }
                        })
                .setNegativeButton(mActivity.getString(R.string.action_no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handler.cancel();
                            }
                        });
        builder.create().show();
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message dontResend, @NonNull final Message resend) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.title_form_resubmission));
        builder.setMessage(mActivity.getString(R.string.message_form_resubmission))
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                resend.sendToTarget();
                            }
                        })
                .setNegativeButton(mActivity.getString(R.string.action_no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dontResend.sendToTarget();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull String url) {
        // Check if configured proxy is available
        if (!mProxyUtils.isProxyReady()) {
            // User has been notified
            return true;
        }

        Map<String, String> headers = mLightningView.getRequestHeaders();

        if (mLightningView.isIncognito()) {
            view.loadUrl(url, headers);
            return true;
        }
        if (url.startsWith("about:")) {
            view.loadUrl(url, headers);
            return true;
        }
        if (url.startsWith("mailto:")) {
            MailTo mailTo = MailTo.parse(url);
            Intent i = Utils.newEmailIntent(mailTo.getTo(), mailTo.getSubject(),
                    mailTo.getBody(), mailTo.getCc());
            mActivity.startActivity(i);
            view.reload();
            return true;
        } else if (url.startsWith("intent://")) {
            Intent intent;
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ignored) {
                intent = null;
            }
            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    intent.setSelector(null);
                }
                try {
                    mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(Constants.TAG, "ActivityNotFoundException");
                }
                return true;
            }
        }

        if (!mIntentUtils.startActivityForUrl(view, url)) {
            view.loadUrl(url, headers);
        }
        return true;
    }
}
