package acr.browser.barebones;

class BookmarkPage {
	public static final String Heading = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\">"
			+ "<head><meta content=\"en-us\" http-equiv=\"Content-Language\" /><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\" />"
			+ "<title>Bookmarks</title></head><style>"
			+ "div.shadow{-moz-box-shadow: 0px 0px 6px #111;-webkit-box-shadow: 0px 0px 6px #111;box-shadow: 0px 0px 6px #111;}"
			+ "body{color: gray;}"
			+ "div	{vertical-align: middle;background-color: #ffffff;}"
			+ "div.clickable {position:relative;}"
			+ "p.font{font-size: 60px;font-family: \"Lucida Console\"}"
			+ "div.clickable a {position:absolute;width:100%;height:100%;top:0;left:0;text-decoration:none; z-index:10; background-color:white;opacity: 0; filter: alpha(opacity=1);}"
			+ "div.space {height: 10px;}"
			+ "p{padding-top: 3mm;padding-bottom: 3mm;padding-right: 2mm;padding-left: 2mm;}"
			+ "img{padding-left: 2mm;padding-right: 2mm;}"
			+ "</style><body bgcolor = #f2f2f2>";
	public static final String Part1 = "<div class=\"clickable\">"
			+ "<div class=\"shadow\">" + "<p class=\"font\">" + "<a href=\"";
	public static final String Part2 = "\"></a>"+
					 "<img height=\"32\" width=\"32\" src='http://getfavicon.appspot.com/";
	public static final String Part3 = "'/>";
	public static final String Part4 = "</p></div></div>";
	public static final String End = "</body></html>";
}
