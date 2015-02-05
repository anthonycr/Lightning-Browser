package acr.browser.lightning.Reading;

import org.jsoup.nodes.Element;

/**
 * Class which encapsulates the data from an image found under an element
 * 
 * @author Chris Alexander, chris@chris-alexander.co.uk
 */
public class ImageResult {

	public String src;
	public Integer weight;
	public String title;
	public int height;
	public int width;
	public String alt;
	public boolean noFollow;
	public Element element;

	public ImageResult(String src, Integer weight, String title, int height, int width, String alt,
			boolean noFollow) {
		this.src = src;
		this.weight = weight;
		this.title = title;
		this.height = height;
		this.width = width;
		this.alt = alt;
		this.noFollow = noFollow;
	}
}
