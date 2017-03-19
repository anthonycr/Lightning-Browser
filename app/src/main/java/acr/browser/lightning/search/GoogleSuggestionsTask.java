package acr.browser.lightning.search;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;

class GoogleSuggestionsTask extends BaseSuggestionsTask {

    private static final String ENCODING = "ISO-8859-1";
    @Nullable private static XmlPullParser sXpp;
    @NonNull private final String mSearchSubtitle;

    GoogleSuggestionsTask(@NonNull String query,
                          @NonNull Application application,
                          @NonNull SuggestionsResult callback) {
        super(query, application, callback);
        mSearchSubtitle = application.getString(R.string.suggestion);
    }

    @NonNull
    protected String getQueryUrl(@NonNull String query, @NonNull String language) {
        return "https://suggestqueries.google.com/complete/search?output=toolbar&hl="
            + language + "&q=" + query;
    }

    @Override
    protected void parseResults(FileInputStream inputStream, List<HistoryItem> results) throws Exception {
        BufferedInputStream fileInput = new BufferedInputStream(inputStream);
        XmlPullParser parser = getParser();
        parser.setInput(fileInput, ENCODING);
        int eventType = parser.getEventType();
        int counter = 0;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "suggestion".equals(parser.getName())) {
                String suggestion = parser.getAttributeValue(null, "data");
                results.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                    suggestion, R.drawable.ic_search));
                counter++;
                if (counter >= MAX_RESULTS) {
                    break;
                }
            }
            eventType = parser.next();
        }
    }

    @Override
    protected String getEncoding() {
        return ENCODING;
    }

    @NonNull
    private static synchronized XmlPullParser getParser() throws XmlPullParserException {
        if (sXpp == null) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            sXpp = factory.newPullParser();
        }
        return sXpp;
    }
}
