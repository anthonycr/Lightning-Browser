package acr.browser.lightning.reading;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * @author goose | jim
 * @author karussell
 *         <p/>
 *         this class will be responsible for taking our top node and stripping out junk
 *         we don't want and getting it ready for how we want it presented to the user
 */
public class OutputFormatter {

    private static final int MIN_FIRST_PARAGRAPH_TEXT = 50; // Min size of first paragraph
    private static final int MIN_PARAGRAPH_TEXT = 30;       // Min size of any other paragraphs
    private static final List<String> NODES_TO_REPLACE = Arrays.asList("strong", "b", "i");
    private Pattern unlikelyPattern = Pattern.compile("display:none|visibility:hidden");
    private final int minFirstParagraphText;
    private final int minParagraphText;
    private final List<String> nodesToReplace;
    private String nodesToKeepCssSelector = "p, ol";

    public OutputFormatter() {
        this(MIN_FIRST_PARAGRAPH_TEXT, MIN_PARAGRAPH_TEXT, NODES_TO_REPLACE);
    }

    public OutputFormatter(int minParagraphText) {
        this(minParagraphText, minParagraphText, NODES_TO_REPLACE);
    }

    public OutputFormatter(int minFirstParagraphText, int minParagraphText) {
        this(minFirstParagraphText, minParagraphText, NODES_TO_REPLACE);
    }

    private OutputFormatter(int minFirstParagraphText, int minParagraphText,
                            List<String> nodesToReplace) {
        this.minFirstParagraphText = minFirstParagraphText;
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
        setParagraphIndex(topNode, nodesToKeepCssSelector);
        removeNodesWithNegativeScores(topNode);
        StringBuilder sb = new StringBuilder();
        int countOfP = append(topNode, sb, nodesToKeepCssSelector);
        String str = SHelper.innerTrim(sb.toString());

        int topNodeLength = topNode.text().length();
        if (topNodeLength == 0) {
            topNodeLength = 1;
        }


        boolean lowTextRatio = ((str.length() / (topNodeLength * 1.0)) < 0.25);
        if (str.length() > 100 && countOfP > 0 && !lowTextRatio)
            return str;

        // no subelements
        if (str.isEmpty() || (!topNode.text().isEmpty()
                && str.length() <= topNode.ownText().length())
                || countOfP == 0 || lowTextRatio) {
            str = topNode.text();
        }

        // if jsoup failed to parse the whole html now parse this smaller 
        // snippet again to avoid html tags disturbing our text:
        return Jsoup.parse(str).text();
    }

    /**
     * If there are elements inside our top node that have a negative gravity
     * score remove them
     */
    private void removeNodesWithNegativeScores(Element topNode) {
        Elements gravityItems = topNode.select("*[gravityScore]");
        for (Element item : gravityItems) {
            int score = getScore(item);
            int paragraphIndex = getParagraphIndex(item);
            if (score < 0 || item.text().length() < getMinParagraph(paragraphIndex)) {
                item.remove();
            }
        }
    }

    private int append(Element node, StringBuilder sb, String tagName) {
        int countOfP = 0; // Number of P elements in the article
        int paragraphWithTextIndex = 0;
        // is select more costly then getElementsByTag?
        MAIN:
        for (Element e : node.select(tagName)) {
            Element tmpEl = e;
            // check all elements until 'node'
            while (tmpEl != null && !tmpEl.equals(node)) {
                if (unlikely(tmpEl))
                    continue MAIN;
                tmpEl = tmpEl.parent();
            }

            String text = node2Text(e);
            if (text.isEmpty() || text.length() < getMinParagraph(paragraphWithTextIndex)
                    || text.length() > SHelper.countLetters(text) * 2) {
                continue;
            }

            if (e.tagName().equals("p")) {
                countOfP++;
            }

            sb.append(text);
            sb.append("\n\n");
            paragraphWithTextIndex += 1;
        }

        return countOfP;
    }

    private static void setParagraphIndex(Element node, String tagName) {
        int paragraphIndex = 0;
        for (Element e : node.select(tagName)) {
            e.attr("paragraphIndex", Integer.toString(paragraphIndex++));
        }
    }

    private int getMinParagraph(int paragraphIndex) {
        if (paragraphIndex < 1) {
            return minFirstParagraphText;
        } else {
            return minParagraphText;
        }
    }

    private static int getParagraphIndex(Element el) {
        try {
            return Integer.parseInt(el.attr("paragraphIndex"));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static int getScore(Element el) {
        try {
            return Integer.parseInt(el.attr("gravityScore"));
        } catch (Exception ex) {
            return 0;
        }
    }

    private boolean unlikely(Node e) {
        if (e.attr("class") != null && e.attr("class").toLowerCase().contains("caption"))
            return true;

        String style = e.attr("style");
        String clazz = e.attr("class");
        return unlikelyPattern.matcher(style).find() || unlikelyPattern.matcher(clazz).find();
    }

    private void appendTextSkipHidden(Element e, StringBuilder accum, int indent) {
        for (Node child : e.childNodes()) {
            if (unlikely(child)) {
                continue;
            }
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                String txt = textNode.text();
                accum.append(txt);
            } else if (child instanceof Element) {
                Element element = (Element) child;
                if (accum.length() > 0 && element.isBlock()
                        && !lastCharIsWhitespace(accum))
                    accum.append(' ');
                else if (element.tagName().equals("br"))
                    accum.append(' ');
                appendTextSkipHidden(element, accum, indent + 1);
            }
        }
    }

    private static boolean lastCharIsWhitespace(StringBuilder accum) {
        return accum.length() != 0 && Character.isWhitespace(accum.charAt(accum.length() - 1));
    }

    private String node2Text(Element el) {
        StringBuilder sb = new StringBuilder(200);
        appendTextSkipHidden(el, sb, 0);
        return sb.toString();
    }

    private OutputFormatter setUnlikelyPattern(String unlikelyPattern) {
        this.unlikelyPattern = Pattern.compile(unlikelyPattern);
        return this;
    }

    public OutputFormatter appendUnlikelyPattern(String str) {
        return setUnlikelyPattern(unlikelyPattern.toString() + '|' + str);
    }
}