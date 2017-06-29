package acr.browser.lightning.favicon;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
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

import acr.browser.lightning.R;
import acr.browser.lightning.utils.DrawableUtils;
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

    @NonNull private final BitmapFactory.Options mLoaderOptions = new BitmapFactory.Options();
    @NonNull private final Application mApplication;
    @NonNull private final LruCache<String, Bitmap> mFaviconCache = new LruCache<String, Bitmap>((int) FileUtils.megabytesToBytes(1)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private final int mBookmarkIconSize;

    @Inject
    FaviconModel(@NonNull Application application) {
        mApplication = application;
        mBookmarkIconSize = mApplication.getResources().getDimensionPixelSize(R.dimen.bookmark_item_icon_size);
    }

    /**
     * Retrieves a favicon from the memory cache.
     * Bitmap may not be present if no bitmap has
     * been added for the URL or if it has been
     * evicted from the memory cache.
     *
     * @param url the URL to retrieve the bitmap for.
     * @return the bitmap associated with the URL,
     * may be null.
     */
    @Nullable
    private Bitmap getFaviconFromMemCache(@NonNull String url) {
        Preconditions.checkNonNull(url);
        synchronized (mFaviconCache) {
            return mFaviconCache.get(url);
        }
    }

    @NonNull
    public Bitmap getDefaultBitmapForString(@Nullable String title) {
        Character firstTitleCharacter = !TextUtils.isEmpty(title) ? title.charAt(0) : '?';

        @ColorInt int defaultFaviconColor = DrawableUtils.characterToColorHash(firstTitleCharacter, mApplication);

        return DrawableUtils.getRoundedLetterImage(firstTitleCharacter,
            mBookmarkIconSize,
            mBookmarkIconSize,
            defaultFaviconColor);
    }

    /**
     * Adds a bitmap to the memory cache
     * for the given URL.
     *
     * @param url    the URL to map the bitmap to.
     * @param bitmap the bitmap to store.
     */
    private void addFaviconToMemCache(@NonNull String url, @NonNull Bitmap bitmap) {
        Preconditions.checkNonNull(url);
        Preconditions.checkNonNull(bitmap);
        synchronized (mFaviconCache) {
            mFaviconCache.put(url, bitmap);
        }
    }

    /**
     * Creates the cache file for the favicon
     * image. File name will be in the form of
     * [hash of URI host].png
     *
     * @param app the context needed to retrieve the
     *            cache directory.
     * @param uri the URI to use as a unique identifier.
     * @return a valid cache file.
     */
    @WorkerThread
    @NonNull
    public static File getFaviconCacheFile(@NonNull Application app, @NonNull Uri uri) {
        FaviconUtils.assertUriSafe(uri);

        String hash = String.valueOf(uri.getHost().hashCode());

        return new File(app.getCacheDir(), hash + ".png");
    }

    /**
     * Retrieves the favicon for a URL,
     * may be from network or cache.
     *
     * @param url   The URL that we should retrieve the
     *              favicon for.
     * @param title The title for the web page.
     */
    @NonNull
    public Single<Bitmap> faviconForUrl(@NonNull final String url,
                                        @NonNull final String title) {
        return Single.create(new SingleAction<Bitmap>() {
            @Override
            public void onSubscribe(@NonNull SingleSubscriber<Bitmap> subscriber) {
                Uri uri = FaviconUtils.safeUri(url);

                if (uri == null) {

                    Bitmap newFavicon = Utils.padFavicon(getDefaultBitmapForString(title));

                    subscriber.onItem(newFavicon);
                    subscriber.onComplete();

                    return;
                }

                File faviconCacheFile = getFaviconCacheFile(mApplication, uri);

                Bitmap favicon = getFaviconFromMemCache(url);

                if (faviconCacheFile.exists() && favicon == null) {
                    favicon = BitmapFactory.decodeFile(faviconCacheFile.getPath(), mLoaderOptions);

                    if (favicon != null) {
                        addFaviconToMemCache(url, favicon);
                    }
                }

                if (favicon != null) {
                    Bitmap newFavicon = Utils.padFavicon(favicon);

                    subscriber.onItem(newFavicon);
                    subscriber.onComplete();

                    return;
                }

                favicon = getDefaultBitmapForString(title);

                Bitmap newFavicon = Utils.padFavicon(favicon);

                subscriber.onItem(newFavicon);
                subscriber.onComplete();
            }
        });
    }

    /**
     * Caches a favicon for a particular URL.
     *
     * @param favicon the favicon to cache.
     * @param url     the URL to cache the favicon for.
     * @return an observable that notifies the consumer
     * when it is complete.
     */
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
                    File image = getFaviconCacheFile(mApplication, uri);
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
