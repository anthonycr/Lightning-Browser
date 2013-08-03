package acr.browser.barebones.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import acr.browser.barebones.databases.DatabaseHandler;
import acr.browser.barebones.databases.HistoryItem;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Browser;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;



public class Utils {
	
	public static DatabaseHandler historyHandler;
	public static SQLiteDatabase history;
	
	public static void createInformativeDialog(Context context, String title,
			String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message)
				.setCancelable(true)
				.setPositiveButton("Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public static void addBookmark(Context context, String title, String url) {
		File book = new File(context.getFilesDir(), "bookmarks");
		File bookUrl = new File(context.getFilesDir(), "bookurl");
		try {
			BufferedReader readUrlRead = new BufferedReader(new FileReader(
					bookUrl));
			String u;
			int n = 0;
			while ((u = readUrlRead.readLine()) != null
					&& n < FinalVariables.MAX_BOOKMARKS) {
				if (u.contentEquals(url)) {
					readUrlRead.close();
					return;
				}
				n++;
			}
			readUrlRead.close();

		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book,
					true));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl, true));
			bookWriter.write(title);
			urlWriter.write(url);
			bookWriter.newLine();
			urlWriter.newLine();
			bookWriter.close();
			urlWriter.close();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
	}

	public static void updateHistory(final Context context,
			final ContentResolver content, final boolean noStockBrowser,
			final String url, final String title) {
		Runnable update = new Runnable() {
			@Override
			public void run() {
				if (!noStockBrowser) {
					try {
						Browser.updateVisitedHistory(content, url, true);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					StringBuilder sb = new StringBuilder("url" + " = ");
					DatabaseUtils.appendEscapedSQLString(sb, url);
					historyHandler = new DatabaseHandler(
							context);
					history = historyHandler.getReadableDatabase();
					Cursor cursor = history.query("history", new String[] { "id",
							"url", "title" }, sb.toString(), null, null, null,
							null);
					if (!cursor.moveToFirst()) {
						historyHandler.addHistoryItem(new HistoryItem(url,
								title));
					} else {
						historyHandler.delete(url);
						historyHandler.addHistoryItem(new HistoryItem(url,
								title));
					}
					historyHandler.close();
					cursor.close();
					history.close();
				} catch (IllegalStateException e) {
					Log.e("Barebones", "IllegalStateException in updateHistory");
				} catch (NullPointerException e) {
					Log.e("Barebones", "NullPointerException in updateHistory");
				} catch (SQLiteException e) {
					Log.e("Barebones", "SQLiteException in updateHistory");
				}
			}
		};
		if (url != null) {
			if (!url.startsWith("file://")) {
				new Thread(update).start();
			}
		}
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static void downloadFile(final Context context, final String url, final String contentDisposition, final String mimetype){
		try {
			Thread downloader = new Thread(new Runnable() {
				@SuppressLint("InlinedApi")
				@Override
				public void run() {
					DownloadManager download = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
					Uri nice = Uri.parse(url);
					DownloadManager.Request it = new DownloadManager.Request(
							nice);
					String fileName = URLUtil.guessFileName(url,
							contentDisposition, mimetype);
					if (FinalVariables.API >= 11) {
						it.allowScanningByMediaScanner();
						it.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					}

					it.setDestinationInExternalPublicDir(
							Environment.DIRECTORY_DOWNLOADS, fileName);
					Log.i("Barebones", "Downloading" + fileName);
					download.enqueue(it);
				}
			});
			downloader.run();
		} catch (NullPointerException e) {
			Log.e("Barebones", "Problem downloading");
			Toast.makeText(context, "Error Downloading File",
					Toast.LENGTH_SHORT).show();
		} catch (IllegalArgumentException e) {
			Log.e("Barebones", "Problem downloading");
			Toast.makeText(context, "Error Downloading File",
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException ignored) {

		}
	}
	
}
