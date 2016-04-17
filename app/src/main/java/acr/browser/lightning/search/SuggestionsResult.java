package acr.browser.lightning.search;

import android.support.annotation.NonNull;

import java.util.List;

import acr.browser.lightning.database.HistoryItem;

public interface SuggestionsResult {

    /**
     * Called when the search suggestions have
     * been retrieved from the server.
     *
     * @param searchResults the results, a valid
     *                      list of results. May
     *                      be empty.
     */
    void resultReceived(@NonNull List<HistoryItem> searchResults);

}
