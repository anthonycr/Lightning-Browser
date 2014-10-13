/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.content.Context;

public class HistoryPage {

	private static final String FILENAME = "history.html";

	private static final String HEADING = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>"
			+ BrowserApp.getAppContext().getString(R.string.action_history)
			+ "</title></head><style>body { background: #e1e1e1;}.box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px; background-color:#fff;box-shadow: 0px 3px rgba( 0, 0, 0, 0.1 );font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}.box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}.black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}.font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}</style><body><div id=\"content\">";

	private static final String PART1 = "<div class=\"box\"><a href=\"";

	private static final String PART2 = "\"></a><p class=\"black\">";

	private static final String PART3 = "</p><p class=\"font\">";

	private static final String PART4 = "</p></div></div>";

	private static final String END = "</div></body></html>";

	public static String getHistoryPage(Context context) {
		String historyHtml = HistoryPage.HEADING;
		List<HistoryItem> historyList = getWebHistory(context);
		Iterator<HistoryItem> it = historyList.iterator();
		HistoryItem helper;
		while (it.hasNext()) {
			helper = it.next();
			historyHtml += HistoryPage.PART1 + helper.getUrl() + HistoryPage.PART2
					+ helper.getTitle() + HistoryPage.PART3 + helper.getUrl() + HistoryPage.PART4;
		}

		historyHtml += HistoryPage.END;
		File historyWebPage = new File(context.getFilesDir(), FILENAME);
		try {
			FileWriter historyWriter = new FileWriter(historyWebPage, false);
			historyWriter.write(historyHtml);
			historyWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Constants.FILE + historyWebPage;
	}

	private static List<HistoryItem> getWebHistory(Context context) {
		HistoryDatabaseHandler databaseHandler = new HistoryDatabaseHandler(context);
		return databaseHandler.getLastHundredItems();
	}
}
