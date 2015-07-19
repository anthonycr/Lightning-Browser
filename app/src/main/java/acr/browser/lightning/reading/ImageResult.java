package acr.browser.lightning.reading;

import org.jsoup.nodes.Element;

/**
 * Class which encapsulates the data from an image found under an element
 * 
 * @author Chris Alexander, chris@chris-alexander.co.uk
 */
public class ImageResult {

    public final String src;
    public final Integer weight;
    public final String title;
    public final int height;
    public final int width;
    public final String alt;
    public final boolean noFollow;
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
