package acr.browser.lightning.search.suggestions;

import android.app.Application;
import android.support.annotation.NonNull;

import org.json.JSONArray;

import java.io.InputStream;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.FileUtils;

/**
 * The search suggestions provider for the Baidu search engine.
 */
public class BaiduSuggestionsModel extends BaseSuggestionsModel {

    @NonNull private static final String ENCODING = "UTF-8";
    @NonNull private final String mSearchSubtitle;

    public BaiduSuggestionsModel(@NonNull Application application) {
        super(application, ENCODING);
        mSearchSubtitle = application.getString(R.string.suggestion);
    }

    @NonNull
    protected String createQueryUrl(@NonNull String query, @NonNull String language) {
        // see http://unionsug.baidu.com/su?wd=encodeURIComponent(U)
        // see http://suggestion.baidu.com/s?wd=encodeURIComponent(U)&action=opensearch
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
