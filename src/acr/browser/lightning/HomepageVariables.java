/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

public class HomepageVariables {
	public static final String HEAD = "<!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.0//EN\" \"http://www.wapforum.org/DTD/xhtml-mobile10.dtd\">"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
			+ "<head>"
			+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
			+ "<title>Homepage</title>"
			+ "</head>"
			+ "<style>#search_input{height:2em; width:70%;font-size:100%;padding-right:10px;padding-left:10px;vertical-align:middle;}#search_submit{font-family:'Arial';"
			+ "color:#585858;height:2.5em;font-weight:bold;vertical-align:middle;}div.center{display:block;margin-left:auto; margin-right:auto;text-align:center;vertical-align:middle;}img.smaller{width:50%;}"
			+ "div.fill{height:100%;}</style><body><div class=\"fill\"></br></br></br></br></br></br></br> <div class=\"center\"><img class=\"smaller\" src=\"";

	public static final String MIDDLE = "\" ></br><form onsubmit=\"return search()\" ><input class=\"search\" type=\"text\" value=\"\" id=\"search_input\" ><input "
			+ "type=\"submit\" id=\"search_submit\" value=\"Search\" ></form> </div></div></br></br></br></br></br></br></br></br>"
			+ "<script type=\"text/javascript\">function "
			+ "search(){if(document.getElementById(\"search_input\").value != \"\"){window.location.href = \"";

	public static final String END = "\" + document.getElementById(\"search_input\").value;}return false;}</script></body></html>";
}
