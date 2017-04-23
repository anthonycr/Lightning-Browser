package acr.browser.lightning.search.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;

import javax.inject.Inject;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.MainActivity;
import acr.browser.lightning.app.BrowserApp;
import acr.browser.lightning.constant.Constants;
import acr.browser.lightning.model.Article;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static acr.browser.lightning.search.notification.NotificationUtil.REQUEST_CODE_ARTICLE_NOTIFICATION;

public class ArticleNotificationService extends Service {

    private static final String TAG = ArticleNotificationService.class.getSimpleName();
    private static final String BASE_ARTICLE_URL = "http://api.mobitech-content.xyz/v1/" + Constants.MOBITECH_APP_KEY +
            "/document/get?&limit=1&p_id=daily_notification";
    private Uri.Builder mUriBuilder = Uri.parse(BASE_ARTICLE_URL).buildUpon();
    private Context context;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Inject
    PreferenceManager mPreferenceManager;

    public void onCreate() {
        super.onCreate();
        BrowserApp.getAppComponent().inject(this);
        context = this;
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        if (!mPreferenceManager.showArticleNotifications()) {
            return START_NOT_STICKY;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (Utils.isNetworkConnected(context)) {
                        checkArticle();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void checkArticle() throws IOException {
        mUriBuilder.appendQueryParameter("user_id", mPreferenceManager.getUserId());
        String url = mUriBuilder.build().toString();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            return;
        }
        final Article article = new Gson().fromJson(response.body().charStream(), Article.class);
        if (article == null || article.documents == null || article.documents.isEmpty()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                prepareNotification(article);
            }
        });
    }

    private void prepareNotification(final Article article) {
        Picasso.with(context).load(article.getImageUrl()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                showNotification(article, bitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                showNotification(article, null);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
    }

    private void showNotification(Article article, Bitmap bitmap) {
        Log.d(TAG, "showNotification: " + bitmap);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        nb.setSmallIcon(R.drawable.ic_mobitech_logo);
        nb.setContentTitle(article.getTitle());
        nb.setContentText(article.getText());
        nb.setTicker(article.getTitle());

        Intent intent = new Intent(context, ArticleNotifySettingActivity.class);
        nb.addAction(R.drawable.ic_settings, "Settings", PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        if(bitmap != null) {  //  big picture style
            NotificationCompat.BigPictureStyle s = new NotificationCompat.BigPictureStyle().bigPicture(bitmap);
            s.setSummaryText(article.getText());
            nb.setStyle(s);
        }

        Intent notifIntent = new Intent(this, MainActivity.class);
        notifIntent.setData(Uri.parse(article.getUrl()));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(pendingIntent);
        nb.setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(REQUEST_CODE_ARTICLE_NOTIFICATION, nb.build());
    }
}