package acr.browser.barebones.utilities;

public class HistoryPageVariables {
	public static final String Heading = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
			+ "<head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" /><title>History</title></head><style>"
			+ "div.shadow {-moz-box-shadow: 0px 0px 6px #111;-webkit-box-shadow: 0px 0px 6px #111;box-shadow: 0px 0px 6px #111;}"
			+ "body{color: gray;text-size: 10px}div.space {height: 0.7em;}	div.extra{text-align: center;}div	{vertical-align: middle;}"
			+ "div	{background-color: #ffffff;}div.clickable {position:relative;}p.black{color: black;font-size: 1em;font-family: \"Lucida Console\"}"
			+ "p.font{font-size: 1em;font-family: \"Lucida Console\"}p.black{padding-left: 0.5em;padding-right: 0.5em;padding-top: 0.5em;padding-bottom: 0em;}"
			+ "p.font{padding-left: 0.5em;padding-right: 0.5em;padding-top: 0em;padding-bottom: 0.5em;white-space: nowrap;overflow: hidden;text-overflow: ellipsis;}"
			+ "div.clickable a {position:absolute; width:100%;height:100%;top:0;left:0;text-decoration:none; z-index:10; background-color:white;opacity: 0;filter: alpha(opacity=1);}"
			+ "</style><body bgcolor = #f2f2f2>";

	public static final String Part1 = "<div class=\"clickable\"><div class=\"shadow\"><a href=\"";
	public static final String Part2 = "\"></a><p class=\"black\">";
	public static final String Part3 = "</p><p class=\"font\">";
	public static final String Part4 = "</p></div></div>";
	public static final String End = "</body></html>";
}
