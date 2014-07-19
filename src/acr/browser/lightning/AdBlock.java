package acr.browser.lightning;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.TreeMap;

public class AdBlock {

	private static TreeMap<String, Integer> mAdBlockMap = null;

	private SharedPreferences mPreferences;

	private boolean mBlockAds = false;

	public AdBlock(Context context) {
		if (mAdBlockMap == null) {
			mAdBlockMap = new TreeMap<String, Integer>(
					String.CASE_INSENSITIVE_ORDER);
		}
		if (mAdBlockMap.isEmpty()) {
			fillSearchTree(context);
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

	public void fillSearchTree(final Context context) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				AssetManager asset = context.getAssets();
				try {
					InputStream input = asset.open("hosts.txt");
					InputStreamReader read = new InputStreamReader(input);
					BufferedReader reader = new BufferedReader(read);
					String line = reader.readLine();
					while (line != null) {
						mAdBlockMap.put(line, 1);
						line = reader.readLine();
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
		String domain = "";
		try {
			domain = getDomainName(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		}
		return mAdBlockMap.containsKey(domain);
	}

	private static String getDomainName(String url) throws URISyntaxException {
		int index = url.indexOf("/", 8);
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
