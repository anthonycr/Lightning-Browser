package acr.browser.lightning.di;

import javax.inject.Singleton;

import acr.browser.lightning.adblock.AssetsAdBlocker;
import acr.browser.lightning.adblock.NoOpAdBlocker;
import acr.browser.lightning.browser.activity.BrowserActivity;
import acr.browser.lightning.network.NetworkObservable;
import acr.browser.lightning.reading.activity.ReadingActivity;
import acr.browser.lightning.browser.TabsManager;
import acr.browser.lightning.browser.activity.ThemableBrowserActivity;
import acr.browser.lightning.settings.activity.ThemableSettingsActivity;
import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.browser.BrowserPresenter;
import acr.browser.lightning.browser.SearchBoxModel;
import acr.browser.lightning.constant.BookmarkPage;
import acr.browser.lightning.constant.DownloadsPage;
import acr.browser.lightning.constant.HistoryPage;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.download.DownloadHandler;
import acr.browser.lightning.download.LightningDownloadListener;
import acr.browser.lightning.settings.fragment.BookmarkSettingsFragment;
import acr.browser.lightning.browser.fragment.BookmarksFragment;
import acr.browser.lightning.settings.fragment.DebugSettingsFragment;
import acr.browser.lightning.settings.fragment.GeneralSettingsFragment;
import acr.browser.lightning.settings.fragment.LightningPreferenceFragment;
import acr.browser.lightning.settings.fragment.PrivacySettingsFragment;
import acr.browser.lightning.browser.fragment.TabsFragment;
import acr.browser.lightning.search.SearchEngineProvider;
import acr.browser.lightning.search.SuggestionsAdapter;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.view.LightningChromeClient;
import acr.browser.lightning.view.LightningView;
import acr.browser.lightning.view.LightningWebClient;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(BrowserActivity activity);

    void inject(BookmarksFragment fragment);

    void inject(BookmarkSettingsFragment fragment);

    void inject(LightningDialogBuilder builder);

    void inject(TabsFragment fragment);

    void inject(LightningView lightningView);

    void inject(ThemableBrowserActivity activity);

    void inject(LightningPreferenceFragment fragment);

    void inject(BrowserApp app);

    void inject(ProxyUtils proxyUtils);

    void inject(ReadingActivity activity);

    void inject(LightningWebClient webClient);

    void inject(ThemableSettingsActivity activity);

    void inject(LightningDownloadListener listener);

    void inject(PrivacySettingsFragment fragment);

    void inject(StartPage startPage);

    void inject(HistoryPage historyPage);

    void inject(BookmarkPage bookmarkPage);

    void inject(DownloadsPage downloadsPage);

    void inject(BrowserPresenter presenter);

    void inject(TabsManager manager);

    void inject(DebugSettingsFragment fragment);

    void inject(SuggestionsAdapter suggestionsAdapter);

    void inject(LightningChromeClient chromeClient);

    void inject(DownloadHandler downloadHandler);

    void inject(SearchBoxModel searchBoxModel);

    void inject(SearchEngineProvider searchEngineProvider);

    void inject(GeneralSettingsFragment generalSettingsFragment);

    void inject(NetworkObservable networkObservable);

    AssetsAdBlocker provideAssetsAdBlocker();

    NoOpAdBlocker provideNoOpAdBlocker();

}
