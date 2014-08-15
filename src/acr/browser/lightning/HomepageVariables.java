/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

public class HomepageVariables {

	public static final String HEAD = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
			+ "<head>"
			+ "<meta content=\"en-us\" http-equiv=\"Content-Language\" />"
			+ "<meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />"
			+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
			+ "<title>"
			+ BrowserApp.getAppContext().getString(R.string.home)
			+ "</title>"
			+ "</head>"
			+ "<style>body{background:#f2f2f2;text-align:center;margin:0px;}#search_input{height:35px; "
			+ "width:100%;outline:none;border:none;font-size: 16px;background-color:transparent;}"
			+ "span { display: block; overflow: hidden; padding-left:5px;vertical-align:middle;}"
			+ ".search_bar{display:table;vertical-align:middle;width:90%;height:35px;max-width:500px;margin:0 auto;background-color:#fff;box-shadow: 0px 3px rgba( 0, 0, 0, 0.1 );"
			+ "font-family: Arial;color: #444;-moz-border-radius: 2px;-webkit-border-radius: 2px;border-radius: 2px;}"
			+ "#search_submit{outline:none;height:37px;float:right;color:#404040;font-size:16px;font-weight:bold;border:none;"
			+ "background-color:transparent;}.outer { display: table; position: absolute; height: 100%; width: 100%;}"
			+ ".middle { display: table-cell; vertical-align: middle;}.inner { margin-left: auto; margin-right: auto; "
			+ "margin-bottom:10%; <!-->maybe bad for small screens</!--> width: 100%;}img.smaller{width:50%;max-width:300px;}"
			+ ".box { vertical-align:middle;position:relative; display: block; margin: 10px;padding-left:10px;padding-right:10px;padding-top:5px;padding-bottom:5px;"
			+ " background-color:#fff;box-shadow: 0px 3px rgba( 0, 0, 0, 0.1 );font-family: Arial;color: #444;"
			+ "font-size: 12px;-moz-border-radius: 2px;-webkit-border-radius: 2px;"
			+ "border-radius: 2px;}</style><body> <div class=\"outer\"><div class=\"middle\"><div class=\"inner\"><img class=\"smaller\" src=\"";

	public static final String MIDDLE = "\" ></br></br><form onsubmit=\"return search()\" class=\"search_bar\">"
			+ "<input type=\"submit\" id=\"search_submit\" value=\"Search\" ><span><input class=\"search\" type=\"text\" value=\"\" id=\"search_input\" >"
			+ "</span></form></br></br></div></div></div><script type=\"text/javascript\">function search(){if(document.getElementById(\"search_input\").value != \"\"){window.location.href = \"";

	public static final String END = "\" + document.getElementById(\"search_input\").value;document.getElementById(\"search_input\").value = \"\";}return false;}</script></body></html>";
}
