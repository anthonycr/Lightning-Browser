/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.constant;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.BrowserApp;

public class BookmarkPage {

    public static final String FILENAME = "bookmarks.html";

    public static final String HEADING = "<!DOCTYPE html><html xmlns=http://www.w3.org/1999/xhtml>\n" +
            "<head>\n" +
            "<meta content=en-us http-equiv=Content-Language />\n" +
            "<meta content='text/html; charset=utf-8' http-equiv=Content-Type />\n" +
            "<meta name=viewport content='width=device-width, initial-scale=1.0'>\n" +
            "<title>" +
            BrowserApp.getAppContext().getString(R.string.action_bookmarks) +
            "</title>\n" +
            "</head>\n" +
            "<style>body{background:#e1e1e1;max-width:100%;min-height:100%}#content{width:100%;max-width:800px;margin:0 auto;text-align:center}.box{vertical-align:middle;text-align:center;position:relative;display:inline-block;height:45px;width:150px;margin:10px;background-color:#fff;box-shadow:0 3px 6px rgba(0,0,0,0.25);font-family:Arial;color:#444;font-size:12px;-moz-border-radius:2px;-webkit-border-radius:2px;border-radius:2px}.box-content{height:25px;width:100%;vertical-align:middle;text-align:center;display:table-cell}p.ellipses{" +
            "width:130px;font-size: small;font-family: Arial, Helvetica, 'sans-serif';white-space:nowrap;overflow:hidden;text-align:left;vertical-align:middle;margin:auto;text-overflow:ellipsis;-o-text-overflow:ellipsis;-ms-text-overflow:ellipsis}.box a{width:100%;height:100%;position:absolute;left:0;top:0}img{vertical-align:middle;margin-right:10px;width:20px;height:20px;}.margin{margin:10px}</style>\n" +
            "<body><div id=content>";

    public static final String PART1 = "<div class=box><a href='";

    public static final String PART2 = "'></a>\n" +
            "<div class=margin>\n" +
            "<div class=box-content>\n" +
            "<p class=ellipses>\n" +
            "<img src='";

    public static final String PART3 = "http://www.google.com/s2/favicons?domain=";

    public static final String PART4 = "' />";

    public static final String PART5 = "</p></div></div></div>";

    public static final String END = "</div></body></html>";

}
