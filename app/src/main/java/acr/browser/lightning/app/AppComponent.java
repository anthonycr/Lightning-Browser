package acr.browser.lightning.app;

import javax.inject.Singleton;

import acr.browser.lightning.activity.BrowserActivity;
import acr.browser.lightning.activity.ReadingActivity;
import acr.browser.lightning.activity.TabsManager;
import acr.browser.lightning.activity.ThemableBrowserActivity;
import acr.browser.lightning.activity.ThemableSettingsActivity;
import acr.browser.lightning.browser.BrowserPresenter;
import acr.browser.lightning.constant.StartPage;
import acr.browser.lightning.dialog.LightningDialogBuilder;
import acr.browser.lightning.download.LightningDownloadListener;
import acr.browser.lightning.fragment.BookmarkSettingsFragment;
import acr.browser.lightning.fragment.BookmarksFragment;
import acr.browser.lightning.fragment.LightningPreferenceFragment;
import acr.browser.lightning.fragment.PrivacySettingsFragment;
import acr.browser.lightning.fragment.TabsFragment;
import acr.browser.lightning.adapter.SearchAdapter;
import acr.browser.lightning.utils.AdBlock;
import acr.browser.lightning.utils.ProxyUtils;
import acr.browser.lightning.view.LightningView;
import acr.browser.lightning.view.LightningWebClient;
import dagger.Component;

@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

    void inject(BrowserActivity activity);

    void inject(BookmarksFragment fragment);

    void inject(BookmarkSettingsFragment fragment);

    void inject(SearchAdapter adapter);

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

    void inject(AdBlock adBlock);

    void inject(LightningDownloadListener listener);

    void inject(PrivacySettingsFragment fragment);

    void inject(StartPage startPage);

    void inject(BrowserPresenter presenter);

    void inject(TabsManager manager);

}
