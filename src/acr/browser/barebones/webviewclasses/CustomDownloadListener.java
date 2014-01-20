package acr.browser.barebones.webviewclasses;

import acr.browser.barebones.utilities.Utils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;

public class CustomDownloadListener implements DownloadListener {
	private static Context mContext;
	public CustomDownloadListener(Context context){
		mContext = context;
	}
	
	
	@Override
	public void onDownloadStart(final String url, String userAgent,
			final String contentDisposition, final String mimetype,
			long contentLength) {
		if (url.endsWith(".mp4") || url.endsWith(".m4a")) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle("Open as...");
			builder.setMessage(
					"Do you want to download this video or watch it in an app?")
					.setCancelable(true)
					.setPositiveButton("Download",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									Utils.downloadFile(mContext, url,
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
									mContext.startActivity(intent);
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

		} else {
			Utils.downloadFile(mContext, url, contentDisposition, mimetype);
		}
	}

}
