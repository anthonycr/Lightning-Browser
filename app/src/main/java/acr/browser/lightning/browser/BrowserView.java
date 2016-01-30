package acr.browser.lightning.browser;

import android.support.annotation.NonNull;
import android.view.View;

public interface BrowserView {

    void setTabView(@NonNull View view);

    void removeTabView();

    void updateUrl(String url, boolean shortUrl);

    void updateProgress(int progress);

}
