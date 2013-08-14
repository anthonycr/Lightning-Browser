package acr.browser.barebones.utilities;

public class BookmarkPageVariables {
	public static final String Heading = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
			+ "<head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />"
			+ "<title>Bookmarks</title></head><style>"
			+ "div.shadow{-moz-box-shadow: 0px 0px 6px #111;-webkit-box-shadow: 0px 0px 6px #111;box-shadow: 0px 0px 6px #111;}"
			+ "body{color: gray;text-size: 10px}"
			+ "div	{vertical-align: middle;background-color: #ffffff;}"
			+ "div.clickable {position:relative;}"
			+ "p.font{font-size: 1em;font-family: \"Lucida Console\"}"
			+ "div.clickable a {position:absolute;width:100%;height:100%;top:0;left:0;text-decoration:none; z-index:10; background-color:white;opacity: 0; filter: alpha(opacity=1);}"
			+ "div.space {height: 0.7em;}"
			+ "p{padding-top: 0.5em;padding-bottom: 0.5em;padding-right: 0.5em;padding-left: 0.5em;}"
			+ "img{padding-left: 0em;padding-right: 0.5em;}"
			+ "</style><body bgcolor = #f2f2f2>";
	public static final String Part1 = "<div class=\"clickable\">"
			+ "<div class=\"shadow\">" + "<p class=\"font\">" + "<a href=\"";
	public static final String Part2 = "\"></a>"+
					 "<img height=\"15px\" width=\"15px\" src='https://www.google.com/s2/favicons?domain_url=";
	public static final String Part3 = "'/>";
	public static final String Part4 = "</p></div></div>";
	public static final String End = "</body></html>";
}
