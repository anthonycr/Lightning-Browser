package acr.browser.lightning.favicon;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import com.anthonycr.bonsai.Completable;
import com.anthonycr.bonsai.CompletableAction;
import com.anthonycr.bonsai.CompletableSubscriber;
import com.anthonycr.bonsai.Single;
import com.anthonycr.bonsai.SingleAction;
import com.anthonycr.bonsai.SingleSubscriber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.utils.Preconditions;
import acr.browser.lightning.utils.Utils;

/**
 * Reactive model that can fetch favicons
 * from URLs and also cache them.
 */
@Singleton
public class FaviconModel {

    private static final String TAG = "FaviconModel";

    @NonNull private final ImageFetcher mImageFetcher;
    @NonNull private final Application mApplication;
    @NonNull private final LruCache<String, Bitmap> mFaviconCache = new LruCache<String, Bitmap>((int) FileUtils.megabytesToBytes(1)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    @Inject
    public FaviconModel(@NonNull Application application) {
        mImageFetcher = new ImageFetcher();
        mApplication = application;
    }

    @Nullable
    private Bitmap getFaviconFromMemCache(@NonNull String url) {
        Preconditions.checkNonNull(url);
        synchronized (mFaviconCache) {
            return mFaviconCache.get(url);
        }
    }

    private void addFaviconToMemCache(@NonNull String url, @NonNull Bitmap bitmap) {
        Preconditions.checkNonNull(url);
        Preconditions.checkNonNull(bitmap);
        synchronized (mFaviconCache) {
            mFaviconCache.put(url, bitmap);
        }
    }

    @NonNull
    private static File createFaviconCacheFile(@NonNull Application app, @NonNull Uri uri) {
        FaviconUtils.assertUriSafe(uri);

        String hash = String.valueOf(uri.getHost().hashCode());

        return new File(app.getCacheDir(), hash + ".png");
    }


    @NonNull
    public Single<Bitmap> faviconForUrl(@NonNull final String url,
                                        @NonNull final Bitmap defaultFavicon,
                                        final boolean allowGoogleService) {
        return Single.create(new SingleAction<Bitmap>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Bitmap> subscriber) {
                Uri uri = FaviconUtils.safeUri(url);

                if (uri == null) {

                    Bitmap newFavicon = Utils.padFavicon(defaultFavicon);

                    subscriber.onItem(newFavicon);
                    subscriber.onComplete();

                    return;
                }

                File faviconCacheFile = createFaviconCacheFile(mApplication, uri);

                Bitmap favicon = getFaviconFromMemCache(url);

                if (faviconCacheFile.exists() && favicon != null) {
                    favicon = mImageFetcher.retrieveFaviconFromCache(faviconCacheFile);
                }

                if (favicon == null) {
                    favicon = mImageFetcher.retrieveBitmapFromDomain(uri);
                } else {
                    Bitmap newFavicon = Utils.padFavicon(favicon);

                    subscriber.onItem(newFavicon);
                    subscriber.onComplete();

                    return;
                }

                if (favicon == null && allowGoogleService) {
                    favicon = mImageFetcher.retrieveBitmapFromGoogle(uri);
                }

                if (favicon != null) {
                    addFaviconToMemCache(url, favicon);
                    cacheFaviconForUrl(favicon, url).subscribe();
                }

                if (favicon == null) {
                    favicon = defaultFavicon;
                }

                Bitmap newFavicon = Utils.padFavicon(favicon);

                subscriber.onItem(newFavicon);
                subscriber.onComplete();
            }
        });
    }

    @NonNull
    public Completable cacheFaviconForUrl(@NonNull final Bitmap favicon,
                                          @NonNull final String url) {
        return Completable.create(new CompletableAction() {
            @Override
            public void onSubscribe(@NonNull CompletableSubscriber subscriber) {
                Uri uri = FaviconUtils.safeUri(url);

                if (uri == null) {
                    subscriber.onComplete();
                    return;
                }

                Log.d(TAG, "Caching icon for " + uri.getHost());
                FileOutputStream fos = null;

                try {
                    File image = createFaviconCacheFile(mApplication, uri);
                    fos = new FileOutputStream(image);
                    favicon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to cache favicon", e);
                } finally {
                    Utils.close(fos);
                }
            }
        });
    }

}
