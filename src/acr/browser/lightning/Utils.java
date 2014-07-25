/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public final class Utils {

	private Utils() {
	}

	public static void downloadFile(final Activity activity, final String url, final String userAgent,
			final String contentDisposition, final boolean privateBrowsing) {
		String fileName = URLUtil.guessFileName(url, null,
				null);
		DownloadHandler.onDownloadStart(activity, url, userAgent, contentDisposition, null, privateBrowsing);
		Log.i(Constants.TAG, "Downloading" + fileName);
	}

	public static synchronized void addBookmark(Context context, String title, String url) {
		File book = new File(context.getFilesDir(), "bookmarks");
		File bookUrl = new File(context.getFilesDir(), "bookurl");
		if (("Bookmarks".equals(title) || "History".equals(title)) && url.startsWith("file://")) {
			return;
		}
		try {
			BufferedReader readUrlRead = new BufferedReader(new FileReader(
					bookUrl));
			String u;
			while ((u = readUrlRead.readLine()) != null) {
				if (u.contentEquals(url)) {
					readUrlRead.close();
					return;
				}
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

	public static Intent newEmailIntent(Context context, String address,
			String subject, String body, String cc) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
		intent.putExtra(Intent.EXTRA_TEXT, body);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_CC, cc);
		intent.setType("message/rfc822");
		return intent;
	}

	public static void createInformativeDialog(Context context, String title,
			String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(message).setCancelable(true)
				.setPositiveButton(context.getResources().getString(R.string.action_ok),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Returns the number of pixels corresponding to the passed density pixels
	 */
	public static int convertToDensityPixels(Context context, int densityPixels) {
		float scale = context.getResources().getDisplayMetrics().density;
		return (int) (densityPixels * scale + 0.5f);
	}

	public static String getDomainName(String url) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			return url;
		}
		String domain = uri.getHost();
		if (domain == null) {
			return url;
		}
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	public static List<HistoryItem> getBookmarks(Context context) {
		List<HistoryItem> bookmarks = new ArrayList<HistoryItem>();
		File bookUrl = new File(context.getFilesDir(),
				"bookurl");
		File book = new File(context.getFilesDir(), "bookmarks");
		try {
			BufferedReader readUrl = new BufferedReader(new FileReader(bookUrl));
			BufferedReader readBook = new BufferedReader(new FileReader(book));
			String u, t;
			while ((u = readUrl.readLine()) != null
					&& (t = readBook.readLine()) != null) {
				HistoryItem map = new HistoryItem(u, t, R.drawable.ic_bookmark);
				bookmarks.add(map);
			}
			readBook.close();
			readUrl.close();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
		return bookmarks;
	}

	public static String[] getArray(String input) {
		return input.split("\\|\\$\\|SEPARATOR\\|\\$\\|");
	}

	public static void trimCache(Context context) {
		try {
			File dir = context.getCacheDir();

			if (dir != null && dir.isDirectory()) {
				deleteDir(dir);
			}
		} catch (Exception ignored) {

		}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(dir, aChildren));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir != null && dir.delete();
	}
}
