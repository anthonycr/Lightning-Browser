package acr.browser.lightning.view;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.utils.Utils;

/**
 * @author Anthony C. Restaino
 * @date 2015/09/29
 */
class IconCacheTask implements Runnable{
    private final Uri uri;
    private final Bitmap icon;

    public IconCacheTask(Uri uri, Bitmap icon) {
        this.uri = uri;
        this.icon = icon;
    }

    @Override
    public void run() {
        String hash = String.valueOf(uri.getHost().hashCode());
        Log.d(Constants.TAG, "Caching icon for " + uri.getHost());
        FileOutputStream fos = null;
        try {
            File image = new File(BrowserApp.getAppContext().getCacheDir(), hash + ".png");
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
