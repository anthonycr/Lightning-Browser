package acr.browser.barebones.webviewclasses;

import acr.browser.barebones.activities.BrowserActivity;
import acr.browser.barebones.utilities.Utils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

public class CustomWebViewClient extends WebViewClient {
	private static Context context;
	public CustomWebViewClient(BrowserActivity browserActivity){
		context = browserActivity;
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.startsWith("market://")
				|| url.startsWith("http://play.google.com")
				|| url.startsWith("https://play.google.com")) {
			Intent urlIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(url));
			urlIntent.putExtra("acr.browser.barebones.Origin",
					view.getId() + 1);
			context.startActivity(urlIntent);
			return true;
		} else if (url.startsWith("http://www.youtube.com")
				|| url.startsWith("https://www.youtube.com")) {
			Intent urlIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(url));
			urlIntent.putExtra("acr.browser.barebones.Origin",
					view.getId() + 1);
			context.startActivity(urlIntent);
			return true;
		} else if (url.startsWith("http://maps.google.com")
				|| url.startsWith("https://maps.google.com")) {
			Intent urlIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(url));
			urlIntent.putExtra("acr.browser.barebones.Origin",
					view.getId() + 1);
			context.startActivity(urlIntent);
			return true;
		} else if (url.contains("tel:") || TextUtils.isDigitsOnly(url)) {
			context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
			return true;
		} else if (url.contains("mailto:")) {
			MailTo mailTo = MailTo.parse(url);
			Intent i = Utils.newEmailIntent(context,
					mailTo.getTo(), mailTo.getSubject(), mailTo.getBody(),
					mailTo.getCc());
			context.startActivity(i);
			view.reload();
			return true;
		}
		return super.shouldOverrideUrlLoading(view, url);
	}

	@Override
	public void onReceivedHttpAuthRequest(final WebView view,
			final HttpAuthHandler handler, final String host,
			final String realm) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final EditText name = new EditText(context);
		final EditText password = new EditText(context);
		LinearLayout passLayout = new LinearLayout(context);
		passLayout.setOrientation(LinearLayout.VERTICAL);

		passLayout.addView(name);
		passLayout.addView(password);

		name.setHint("Username");
		password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		password.setTransformationMethod(new PasswordTransformationMethod());
		password.setHint("Password");
		builder.setTitle("Sign in");
		builder.setView(passLayout);
		builder.setCancelable(true)
				.setPositiveButton("Sign in",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int id) {
								String user = name.getText().toString();
								String pass = password.getText().toString();
								handler.proceed(user.trim(), pass.trim());
								Log.i("Lightning", "Request Login");

							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int id) {
								handler.cancel();

							}
						});
		AlertDialog alert = builder.create();
		alert.show();

	}

	@Override
	public void onScaleChanged(WebView view, float oldScale, float newScale) {
		if (view.isShown()) {
			view.invalidate();
		}
		super.onScaleChanged(view, oldScale, newScale);
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		BrowserActivity.onPageFinished(view, url);
		super.onPageFinished(view, url);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		BrowserActivity.onPageStarted(view, url, favicon);
		super.onPageStarted(view, url, favicon);
	}

	@Override
	public void onReceivedSslError(WebView view,
			final SslErrorHandler handler, SslError error) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Warning");
		builder.setMessage(
				"The certificate of the site is not trusted. Proceed anyway?")
				.setCancelable(true)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int id) {
								handler.proceed();
							}
						})
				.setNegativeButton("No",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int id) {
								handler.cancel();
							}
						});
		AlertDialog alert = builder.create();
		if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
			alert.show();
		} else {
			handler.proceed();
		}

	}

	@Override
	public void onFormResubmission(WebView view, final Message dontResend,
			final Message resend) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Form Resubmission");
		builder.setMessage("Would you like to resend the data?")
				.setCancelable(true)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int id) {

								resend.sendToTarget();
							}
						})
				.setNegativeButton("No",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int id) {

								dontResend.sendToTarget();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
		super.onFormResubmission(view, dontResend, resend);
	}
}
