package acr.browser.lightning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.Browser;

public class BookmarkManager {

	private Context mContext;
	private static final String TITLE = "title";
	private static final String URL = "url";
	private static final String FOLDER = "folder";
	private static final String ORDER = "order";
	private static final String FILE_BOOKMARKS = "bookmarks.dat";

	public BookmarkManager(Context context) {
		mContext = context;
	}

	/**
	 * This method adds the the HistoryItem item to permanent bookmark storage
	 * 
	 * @param item
	 */
	public void addBookmark(HistoryItem item) {
		File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
		try {
			BufferedWriter bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, true));
			JSONObject object = new JSONObject();
			object.put(TITLE, item.getTitle());
			object.put(URL, item.getUrl());
			object.put(FOLDER, item.getFolder());
			object.put(ORDER, item.getOrder());
			bookmarkWriter.write(object.toString());
			bookmarkWriter.newLine();
			bookmarkWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method adds the list of HistoryItems to permanent bookmark storage
	 * 
	 * @param list
	 */
	public void addBookmarkList(List<HistoryItem> list) {
		File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
		try {
			BufferedWriter bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, true));
			for (HistoryItem item : list) {
				JSONObject object = new JSONObject();
				object.put(TITLE, item.getTitle());
				object.put(URL, item.getUrl());
				object.put(FOLDER, item.getFolder());
				object.put(ORDER, item.getOrder());
				bookmarkWriter.write(object.toString());
				bookmarkWriter.newLine();
			}
			bookmarkWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method exports the stored bookmarks to a text file in the device's
	 * external download directory
	 */
	public void exportBookmarks() {
		List<HistoryItem> bookmarkList = Utils.getBookmarks(mContext);
		File bookmarksExport = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				"BookmarksExport.txt");
		try {
			BufferedWriter bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksExport,
					false));
			for (HistoryItem item : bookmarkList) {
				JSONObject object = new JSONObject();
				object.put(TITLE, item.getTitle());
				object.put(URL, item.getUrl());
				object.put(FOLDER, item.getFolder());
				object.put(ORDER, item.getOrder());
				bookmarkWriter.write(object.toString());
				bookmarkWriter.newLine();
			}
			bookmarkWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method returns a list of all stored bookmarks
	 * 
	 * @return
	 */
	public List<HistoryItem> getBookmarks() {
		List<HistoryItem> bookmarks = new ArrayList<HistoryItem>();
		File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
		try {
			BufferedReader bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
			String line;
			while ((line = bookmarksReader.readLine()) != null) {
				JSONObject object = new JSONObject(line);
				HistoryItem item = new HistoryItem();
				item.setTitle(object.getString(TITLE));
				item.setUrl(object.getString(URL));
				item.setFolder(object.getString(FOLDER));
				item.setOrder(object.getInt(ORDER));
				bookmarks.add(item);
			}
			bookmarksReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bookmarks;
	}

	/**
	 * This method returns a list of bookmarks located in the specified folder
	 * 
	 * @param folder
	 * @return
	 */
	public List<HistoryItem> getBookmarksFromFolder(String folder) {
		List<HistoryItem> bookmarks = new ArrayList<HistoryItem>();
		File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
		try {
			BufferedReader bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
			String line;
			while ((line = bookmarksReader.readLine()) != null) {
				JSONObject object = new JSONObject(line);
				if (object.getString(FOLDER).equals(folder)) {
					HistoryItem item = new HistoryItem();
					item.setTitle(object.getString(TITLE));
					item.setUrl(object.getString(URL));
					item.setFolder(object.getString(FOLDER));
					item.setOrder(object.getInt(ORDER));
					bookmarks.add(item);
				}
			}
			bookmarksReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bookmarks;
	}

	/**
	 * This method returns a list of all folders
	 * 
	 * @return
	 */
	public List<HistoryItem> getFolders() {
		List<HistoryItem> folders = new ArrayList<HistoryItem>();
		List<String> folderNameList = new ArrayList<String>();
		File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
		try {
			BufferedReader bookmarksReader = new BufferedReader(new FileReader(bookmarksFile));
			String line;
			while ((line = bookmarksReader.readLine()) != null) {
				JSONObject object = new JSONObject(line);
				String folderName = object.getString(FOLDER);
				if (!folderName.isEmpty() && !folderNameList.contains(folderName)) {
					HistoryItem item = new HistoryItem();
					item.setTitle(folderName);
					item.setUrl(Constants.FOLDER + folderName);
					folderNameList.add(folderName);
					folders.add(item);
				}
			}
			bookmarksReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return folders;
	}

	/**
	 * This method imports all bookmarks that are included in the device's
	 * permanent bookmark storage
	 */
	public void importBookmarksFromBrowser() {
		if (mContext.getSharedPreferences(PreferenceConstants.PREFERENCES, 0).getBoolean(
				PreferenceConstants.SYSTEM_BROWSER_PRESENT, false)) {

			List<HistoryItem> bookmarkList = new ArrayList<HistoryItem>();
			String[] columns = new String[] { Browser.BookmarkColumns.TITLE,
					Browser.BookmarkColumns.URL };
			String selection = Browser.BookmarkColumns.BOOKMARK + " = 1";
			Cursor cursor = mContext.getContentResolver().query(Browser.BOOKMARKS_URI, columns,
					selection, null, null);

			String title, url;
			int number = 0;

			if (cursor.moveToFirst()) {
				do {
					number++;
					title = cursor.getString(0);
					url = cursor.getString(1);
					if (title.isEmpty()) {
						title = Utils.getDomainName(url);
					}
					bookmarkList.add(new HistoryItem(url, title));
				} while (cursor.moveToNext());
			}

			cursor.close();
			addBookmarkList(bookmarkList);

			Utils.showToast(mContext,
					number + " " + mContext.getResources().getString(R.string.message_import));
		} else {
			Utils.createInformativeDialog(mContext,
					mContext.getResources().getString(R.string.title_error), mContext
							.getResources().getString(R.string.dialog_import_error));
		}
	}

	/**
	 * This method imports the bookmarks from a backup file that is located on
	 * external storage
	 * 
	 * @param dir
	 * @param file
	 */
	public void importBookmarksFromFile(File dir, String file) {
		File bookmarksImport = new File(dir, file);
		try {
			BufferedReader bookmarksReader = new BufferedReader(new FileReader(bookmarksImport));
			String line;
			while ((line = bookmarksReader.readLine()) != null) {
				JSONObject object = new JSONObject(line);
				HistoryItem item = new HistoryItem();
				item.setTitle(object.getString(TITLE));
				item.setUrl(object.getString(URL));
				item.setFolder(object.getString(FOLDER));
				item.setOrder(object.getInt(ORDER));
				addBookmark(item);
			}
			bookmarksReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method overwrites the entire bookmark file with the list of
	 * bookmarks. This is useful when an edit has been made to one or more
	 * bookmarks in the list
	 * 
	 * @param list
	 */
	public void overwriteBookmarks(List<HistoryItem> list) {
		File bookmarksFile = new File(mContext.getFilesDir(), FILE_BOOKMARKS);
		try {
			BufferedWriter bookmarkWriter = new BufferedWriter(new FileWriter(bookmarksFile, false));
			for (HistoryItem item : list) {
				JSONObject object = new JSONObject();
				object.put(TITLE, item.getTitle());
				object.put(URL, item.getUrl());
				object.put(FOLDER, item.getFolder());
				object.put(ORDER, item.getOrder());
				bookmarkWriter.write(object.toString());
				bookmarkWriter.newLine();
			}
			bookmarkWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
