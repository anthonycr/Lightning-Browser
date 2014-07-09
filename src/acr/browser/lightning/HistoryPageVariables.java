/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

public class HistoryPageVariables {
	public static final String Heading = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>"
			+ BrowserApp.getAppContext().getString(R.string.action_history)
			+ "</title></head><style>body { background: #e1e1e1;}.box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px; background-color:#fff;box-shadow: 0px 3px rgba( 0, 0, 0, 0.1 );font-family: Arial;color: #444;font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}.box a { width: 100%; height: 100%; position: absolute; left: 0; top: 0;}.black {color: black;font-size: 15px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}.font {color: gray;font-size: 10px;font-family: Arial; white-space: nowrap; overflow: hidden;margin:auto; text-overflow: ellipsis; -o-text-overflow: ellipsis; -ms-text-overflow: ellipsis;}</style><body><div id=\"content\">";
	public static final String Part1 = "<div class=\"box\"><a href=\"";
	public static final String Part2 = "\"></a><p class=\"black\">";
	public static final String Part3 = "</p><p class=\"font\">";
	public static final String Part4 = "</p></div></div>";
	public static final String End = "</div></body></html>";
}
