package acr.browser.barebones.incognitoclasses;

import acr.browser.barebones.activities.IncognitoModeActivity;
import acr.browser.barebones.utilities.Utils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;

public class IncognitoDownloadListener implements DownloadListener {
	private static Context context;
	public IncognitoDownloadListener(IncognitoModeActivity activity){
		context = activity;
	}
	@Override
	public void onDownloadStart(final String url, String userAgent,
			final String contentDisposition, final String mimetype,
			long contentLength) {
		if (url.endsWith(".mp4") || url.endsWith(".m4a")) {

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("Open as...");
			builder.setMessage(
					"Do you want to download this video or watch it in an app?")
					.setCancelable(true)
					.setPositiveButton("Download",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									Utils.downloadFile(context, url,
											contentDisposition, mimetype);
								}
							})
					.setNegativeButton("Watch",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									Intent intent = new Intent(
											Intent.ACTION_VIEW);
									intent.setDataAndType(Uri.parse(url),
											"video/mp4");
									intent.putExtra(
											"acr.browser.barebones.Download",
											1);
									context.startActivity(intent);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		} else {
			Utils.downloadFile(context, url, contentDisposition, mimetype);
		}
	}

}
