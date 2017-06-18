package acr.browser.lightning.search.engine;

import acr.browser.lightning.constant.Constants;

/**
 * The Yandex search engine.
 * <p>
 * See http://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Yandex.svg/600px-Yandex.svg.png
 * for the icon.
 */
public class YandexSearch extends BaseSearchEngine {

    public YandexSearch() {
        super("file:///android_asset/yandex.png", Constants.YANDEX_SEARCH);
    }

}
