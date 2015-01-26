/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

public class BookmarkPage {

	public static final String FILENAME = "bookmarks.html";
	
	public static final String HEADING = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"><title>"
			+ BrowserApp.getAppContext().getString(R.string.action_bookmarks)
			+ "</title></head><style>body { background: #e1e1e1; max-width:100%; min-height:100%;}#content {width:100%; max-width:800px; margin:0 auto; text-align:center;}.box { vertical-align:middle;text-align:center;position:relative; display: inline-block; height: 100px; width: 100px; margin: 10px; background-color:#fff;box-shadow: 0px 2px 3px rgba( 0, 0, 0, 0.25 );font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}.stuff {height: 100px; width: 100px;vertical-align:middle;text-align:center; display: table-cell;}p.ellipses {width:90px; white-space: nowrap; overflow: hidden;text-align:center;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}.box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}</style><body> <div id=\"content\">";

	public static final String PART1 = "<div class=\"box\"><a href=\"";

	public static final String PART2 = "\" ></a><div class=\"stuff\" ><img height=\"20\" width=\"20\" src='http://www.google.com/s2/favicons?domain=";

	public static final String PART3 = "' /><p class=\"ellipses\">";

	public static final String PART4 = "</p></div></div>";

	public static final String END = "</div></body></html>";
	
}
