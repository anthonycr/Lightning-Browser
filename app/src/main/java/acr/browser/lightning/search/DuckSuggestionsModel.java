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

final class DuckSuggestionsModel extends BaseSuggestionsModel {

    @NonNull private static final String ENCODING = "UTF-8";
    @NonNull private final String mSearchSubtitle;

    DuckSuggestionsModel(@NonNull Application application) {
        super(application);
        mSearchSubtitle = application.getString(R.string.suggestion);
    }

    @NonNull
    @Override
    protected String createQueryUrl(@NonNull String query, @NonNull String language) {
        return "https://duckduckgo.com/ac/?q=" + query;
    }

    @Override
    protected void parseResults(@NonNull FileInputStream inputStream, @NonNull List<HistoryItem> results) throws Exception {
        String content = FileUtils.readStringFromFile(inputStream, ENCODING);
        JSONArray jsonArray = new JSONArray(content);
        int counter = 0;
        for (int n = 0, size = jsonArray.length(); n < size; n++) {
            JSONObject object = jsonArray.getJSONObject(n);
            String suggestion = object.getString("phrase");
            results.add(new HistoryItem(mSearchSubtitle + " \"" + suggestion + '"',
                suggestion, R.drawable.ic_search));
            counter++;
            if (counter >= MAX_RESULTS) {
                break;
            }
        }
    }

    @NonNull
    @Override
    protected String getEncoding() {
        return ENCODING;
    }

}
