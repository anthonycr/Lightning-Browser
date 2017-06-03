package acr.browser.lightning.search;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.FileUtils;

// http://unionsug.baidu.com/su?wd=encodeURIComponent(U)
// http://suggestion.baidu.com/s?wd=encodeURIComponent(U)&action=opensearch


class BaiduSuggestionsModel extends BaseSuggestionsModel {

    @NonNull private static final String ENCODING = "UTF-8";
    @Nullable private static XmlPullParser sXpp;
    @NonNull private final String mSearchSubtitle;

    BaiduSuggestionsModel(@NonNull Application application) {
        super(application, ENCODING);
        mSearchSubtitle = application.getString(R.string.suggestion);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query, @NonNull String language) {
        return "http://suggestion.baidu.com/s?wd=" + query + "&action=opensearch";
    }

    @Override
    protected void parseResults(@NonNull InputStream inputStream, @NonNull List<HistoryItem> results) throws Exception {
        String content = FileUtils.readStringFromStream(inputStream, "GBK");
        JSONArray respArray = new JSONArray(content);
        JSONArray jsonArray = respArray.getJSONArray(1);
        int counter = 0;
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            String suggestion = jsonArray.getString(n);
            results.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                    suggestion, R.drawable.ic_search));
            counter++;
            if (counter >= MAX_RESULTS) {
                break;
            }
        }
    }
}
