package acr.browser.barebones;

import android.app.Activity;
import android.database.Cursor;
import android.provider.Browser;
import android.util.Log;

public class Bookmarks extends Activity{
	public void GetBookmarks(){
    String[] projection = new String[] {
   		Browser.BookmarkColumns.TITLE
   		, Browser.BookmarkColumns.URL
    };
    Cursor mCur = getContentResolver().query(android.provider.Browser.BOOKMARKS_URI, projection, null, null, null);
    mCur.moveToFirst();
    int titleIdx = mCur.getColumnIndex(Browser.BookmarkColumns.TITLE);
    int urlIdx = mCur.getColumnIndex(Browser.BookmarkColumns.URL);
    String[] android = (Browser.HISTORY_PROJECTION);
    while (mCur.isAfterLast() == false) {
    	Log.i("Title", mCur.getString(titleIdx));
    	Log.i("Title",mCur.getString(urlIdx));
    	mCur.moveToNext();
    }
	}
}
