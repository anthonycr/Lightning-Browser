package acr.browser.lightning.reading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * @author goose | jim
 * @author karussell
 * 
 *         this class will be responsible for taking our top node and stripping
 *         out junk we don't want and getting it ready for how we want it
 *         presented to the user
 */
public class OutputFormatter {

    public static final int MIN_PARAGRAPH_TEXT = 50;
    private static final List<String> NODES_TO_REPLACE = Arrays.asList("strong", "b", "i");
    private Pattern unlikelyPattern = Pattern.compile("display\\:none|visibility\\:hidden");
    protected final int minParagraphText;
    protected final List<String> nodesToReplace;
    protected String nodesToKeepCssSelector = "p";

    public OutputFormatter() {
        this(MIN_PARAGRAPH_TEXT, NODES_TO_REPLACE);
    }

    public OutputFormatter(int minParagraphText) {
        this(minParagraphText, NODES_TO_REPLACE);
    }

    public OutputFormatter(int minParagraphText, List<String> nodesToReplace) {
        this.minParagraphText = minParagraphText;
        this.nodesToReplace = nodesToReplace;
    }

    /**
     * set elements to keep in output text
     */
    public void setNodesToKeepCssSelector(String nodesToKeepCssSelector) {
        this.nodesToKeepCssSelector = nodesToKeepCssSelector;
    }

    /**
     * takes an element and turns the P tags into \n\n
     */
    public String getFormattedText(Element topNode) {
        removeNodesWithNegativeScores(topNode);
        StringBuilder sb = new StringBuilder();
        append(topNode, sb, nodesToKeepCssSelector);
        String str = SHelper.innerTrim(sb.toString());
        if (str.length() > 100)
            return str;

        // no subelements
        if (str.isEmpty() || !topNode.text().isEmpty()
                && str.length() <= topNode.ownText().length())
            str = topNode.text();

        // if jsoup failed to parse the whole html now parse this smaller
        // snippet again to avoid html tags disturbing our text:
        return Jsoup.parse(str).text();
    }

    /**
     * Takes an element and returns a list of texts extracted from the P tags
     */
    public List<String> getTextList(Element topNode) {
        List<String> texts = new ArrayList<>();
        for (Element element : topNode.select(this.nodesToKeepCssSelector)) {
            if (element.hasText()) {
                texts.add(element.text());
            }
        }
        return texts;
    }

    /**
     * If there are elements inside our top node that have a negative gravity
     * score remove them
     */
    protected void removeNodesWithNegativeScores(Element topNode) {
        Elements gravityItems = topNode.select("*[gravityScore]");
        for (Element item : gravityItems) {
            int score = Integer.parseInt(item.attr("gravityScore"));
            if (score < 0 || item.text().length() < minParagraphText)
                item.remove();
        }
    }

    protected void append(Element node, StringBuilder sb, String tagName) {
        // is select more costly then getElementsByTag?
        MAIN: for (Element e : node.select(tagName)) {
            Element tmpEl = e;
            // check all elements until 'node'
            while (tmpEl != null && !tmpEl.equals(node)) {
                if (unlikely(tmpEl))
                    continue MAIN;
                tmpEl = tmpEl.parent();
            }

            String text = node2Text(e);
            if (text.isEmpty() || text.length() < minParagraphText
                    || text.length() > SHelper.countLetters(text) * 2)
                continue;

            sb.append(text);
            sb.append("\n\n");
        }
    }

    boolean unlikely(Node e) {
        if (e.attr("class") != null
                && e.attr("class").toLowerCase(Locale.getDefault()).contains("caption"))
            return true;

        String style = e.attr("style");
        String clazz = e.attr("class");
        return unlikelyPattern.matcher(style).find() || unlikelyPattern.matcher(clazz).find();
    }

    void appendTextSkipHidden(Element e, StringBuilder accum) {
        for (Node child : e.childNodes()) {
            if (unlikely(child))
                continue;
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                String txt = textNode.text();
                accum.append(txt);
            } else if (child instanceof Element) {
                Element element = (Element) child;
                if (accum.length() > 0 && element.isBlock() && !lastCharIsWhitespace(accum))
                    accum.append(" ");
                else if (element.tagName().equals("br"))
                    accum.append(" ");
                appendTextSkipHidden(element, accum);
            }
        }
    }

    boolean lastCharIsWhitespace(StringBuilder accum) {
        return (accum.length() != 0) && Character.isWhitespace(accum.charAt(accum.length() - 1));
    }

    protected String node2TextOld(Element el) {
        return el.text();
    }

    protected String node2Text(Element el) {
        StringBuilder sb = new StringBuilder(200);
        appendTextSkipHidden(el, sb);
        return sb.toString();
    }

    public OutputFormatter setUnlikelyPattern(String unlikelyPattern) {
        this.unlikelyPattern = Pattern.compile(unlikelyPattern);
        return this;
    }

    public OutputFormatter appendUnlikelyPattern(String str) {
        return setUnlikelyPattern(unlikelyPattern.toString() + "|" + str);
    }
}
