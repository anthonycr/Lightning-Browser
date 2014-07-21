package acr.browser.lightning;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class AdBlock {

	private static final String BLOCKED_DOMAINS_LIST_FILE_NAME = "hosts.txt";

	private static final Set<String> mBlockedDomainsList = new HashSet<String>();

	private SharedPreferences mPreferences;

	private boolean mBlockAds;

	public AdBlock(Context context) {
		if (mBlockedDomainsList.isEmpty()) {
			loadBlockedDomainsList(context);
		}
		mPreferences = context.getSharedPreferences(
				PreferenceConstants.PREFERENCES, 0);
		mBlockAds = mPreferences.getBoolean(PreferenceConstants.BLOCK_ADS,
				false);
	}

	public void updatePreference() {
		mBlockAds = mPreferences.getBoolean(PreferenceConstants.BLOCK_ADS,
				false);
	}

	private void loadBlockedDomainsList(final Context context) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				AssetManager asset = context.getAssets();
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(asset.open(BLOCKED_DOMAINS_LIST_FILE_NAME)));
					String line;
					while ((line = reader.readLine()) != null) {
						mBlockedDomainsList.add(line.trim().toLowerCase());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	public boolean isAd(String url) {
		if (!mBlockAds) {
			return false;
		}

		String domain;
		try {
			domain = getDomainName(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		}

		return mBlockedDomainsList.contains(domain.toLowerCase());
	}

	private static String getDomainName(String url) throws URISyntaxException {
		int index = url.indexOf('/', 8);
		if (index != -1) {
			url = url.substring(0, index);
		}

		URI uri = new URI(url);
		String domain = uri.getHost();
		if (domain == null) {
			return url;
		}

		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}
}
