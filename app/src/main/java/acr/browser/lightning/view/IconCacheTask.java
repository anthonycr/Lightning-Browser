package acr.browser.lightning.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.utils.Utils;

class IconCacheTask implements Runnable {
    private final Uri uri;
    private final Bitmap icon;
    private final Context context;

    public IconCacheTask(final Uri uri, final Bitmap icon, final Context context) {
        this.uri = uri;
        this.icon = icon;
        this.context = BrowserApp.get(context);
    }

    @Override
    public void run() {
        String hash = String.valueOf(uri.getHost().hashCode());
        Log.d(Constants.TAG, "Caching icon for " + uri.getHost());
        FileOutputStream fos = null;
        try {
            File image = new File(context.getCacheDir(), hash + ".png");
            fos = new FileOutputStream(image);
            icon.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.close(fos);
        }
    }
}
