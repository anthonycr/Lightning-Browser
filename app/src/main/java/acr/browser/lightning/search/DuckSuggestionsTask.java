package acr.browser.lightning.search;

import android.app.Application;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.util.List;

import acr.browser.lightning.R;
import acr.browser.lightning.database.HistoryItem;
import acr.browser.lightning.utils.FileUtils;

public final class DuckSuggestionsTask extends BaseSuggestionsTask {

    private static final String ENCODING = "UTF-8";
    @NonNull private final String mSearchSubtitle;

    DuckSuggestionsTask(@NonNull String query,
                        @NonNull Application application,
                        @NonNull SuggestionsResult callback) {
        super(query, application, callback);
        mSearchSubtitle = application.getString(R.string.suggestion);
    }

    @Override
    protected String getQueryUrl(@NonNull String query, @NonNull String language) {
        return "https://duckduckgo.com/ac/?q=" + query;
    }

    @Override
    protected void parseResults(FileInputStream inputStream, List<HistoryItem> results) throws Exception {
        String content = FileUtils.readStringFromFile(inputStream, ENCODING);
        JSONArray jsonArray = new JSONArray(content);
        int counter = 0;
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            JSONObject object = jsonArray.getJSONObject(n);
            String suggestion = object.getString("phrase");
            results.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                suggestion, R.drawable.ic_search));
            counter++;
            if (counter >= 5) {
                break;
            }
        }
    }

    @Override
    protected String getEncoding() {
        return ENCODING;
    }

}
