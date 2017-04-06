package acr.browser.lightning.app;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.annotation.ReportsCrashes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import acr.browser.lightning.BuildConfig;
import acr.browser.lightning.R;
import acr.browser.lightning.preference.PreferenceManager;
import acr.browser.lightning.utils.FileUtils;
import acr.browser.lightning.utils.MemoryLeakUtils;
import io.mobitech.commonlibrary.analytics.AnalyticsService;
import io.mobitech.commonlibrary.analytics.IEventCallback;
import io.mobitech.commonlibrary.model.HttpResponse;
import io.mobitech.commonlibrary.utils.NetworkUtil;
import io.mobitech.commonlibrary.utils.contentParsers.StringParser;
import io.mobitech.reporting.HockeySender;

import static acr.browser.lightning.constant.Constants.MOBITECH_APP_KEY;

@ReportsCrashes(formUri = "")
public class BrowserApp extends Application {

    private static final String TAG = BrowserApp.class.getSimpleName();

    private static AppComponent mAppComponent;
    private static final Executor mIOThread = Executors.newSingleThreadExecutor();
    private static final Executor mTaskThread = Executors.newCachedThreadPool();

    @Inject Bus mBus;
    @Inject PreferenceManager mPreferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        InitiateOnAppStartupTask initiateOnAppStartupTask = new InitiateOnAppStartupTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            initiateOnAppStartupTask
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            initiateOnAppStartupTask.execute();
        }
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        }

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {

                if (BuildConfig.DEBUG) {
                    FileUtils.writeCrashToStorage(ex);
                }

                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                } else {
                    System.exit(2);
                }
            }
        });

        mAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        mAppComponent.inject(this);

        if (mPreferenceManager.getUseLeakCanary() && !isRelease()) {
            LeakCanary.install(this);
        }
        if (!isRelease() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        registerActivityLifecycleCallbacks(new MemoryLeakUtils.LifecycleAdapter() {
            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "Cleaning up after the Android framework");
                MemoryLeakUtils.clearNextServedView(activity, BrowserApp.this);
            }
        });
    }

    @NonNull
    public static BrowserApp get(@NonNull Context context) {
        return (BrowserApp) context.getApplicationContext();
    }

    public static AppComponent getAppComponent() {
        return mAppComponent;
    }

    @NonNull
    public static Executor getIOThread() {
        return mIOThread;
    }

    @NonNull
    public static Executor getTaskThread() {
        return mTaskThread;
    }

    public static Bus getBus(@NonNull Context context) {
        return get(context).mBus;
    }

    /**
     * Determines whether this is a release build.
     *
     * @return true if this is a release build, false otherwise.
     */
    public static boolean isRelease() {
        return !BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.toLowerCase().equals("release");
    }

    public static void copyToClipboard(@NonNull Context context, @NonNull String string) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URL", string);
        clipboard.setPrimaryClip(clip);
    }

    private class InitiateOnAppStartupTask extends
            AsyncTask<String, Void, String> {


        public InitiateOnAppStartupTask() {
        }

        @Override
        protected String doInBackground(String... args) {

            //init bug and error reporting
            ACRA.init(BrowserApp.this);
            ACRAConfiguration conf = new ACRAConfiguration();
            conf.setBuildConfigClass(BuildConfig.class);
            ACRA.setConfig(conf);
            ACRA.getErrorReporter().setReportSender(new HockeySender("2955c756b2a44bdf9eafc02a848930d9", " 9f03c103e83eab675f0c6ff239e36d79",BrowserApp.this));

//            AnalyticsService.addEventListener(IEventCallback.EVENT_TYPE.ALL, new SegmentAnalyticsTracking(BrowserApp.this, getString(R.string.analytics_write_key)));
//
//            AccessibilityEventsReceiverService.setOnBindCallback(new ICallback() {
//                @Override
//                public void execute() {
//                    Intent reOpenShoppingBuddy = new Intent(ShoppingBuddyAppContext.this, MainWithSliderActivity.class);
//                    reOpenShoppingBuddy.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//                    startActivity(reOpenShoppingBuddy);
//                }
//            });


                if (mPreferenceManager!=null && !mPreferenceManager.isInstalled()) {
                    String referrer = mPreferenceManager.getReferrer();
                    String appId = mPreferenceManager.getReferrerAppId();
                    if(TextUtils.isEmpty(appId)){
                        appId = getString(R.string.app_name);
                    }
                    if(TextUtils.isEmpty(referrer)){
                        referrer = BuildConfig.VERSION_NAME;
                    }

                    try {
                        String url = "http://dashboard.mobitech.io/v1/tracking/install";
                        Map<String, String> contentValue   = new HashMap<>();
                        contentValue.put("p_key", MOBITECH_APP_KEY);
                        contentValue.put("referrer_id", URLEncoder.encode(referrer,"UTF-8"));
                        contentValue.put("app_id",  URLEncoder.encode(appId,"UTF-8"));
                        HttpResponse response = NetworkUtil.getContentFromURLPOST(url, new StringParser(String.class),new HashMap<String, String>(), contentValue, false);
                        mPreferenceManager.setInstalled(response.responseCode<400);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

            //Track daily usage
            Map<String, String> eventData = AnalyticsService.initResponse(IEventCallback.EVENT_TYPE.SYSTEM);
            eventData.put(IEventCallback.EVENT_ELEMENTS.EVENT_NAME.name(), "BROWSER_OPEN");
            eventData.put(IEventCallback.EVENT_ELEMENTS.EVENT_VALUE.name(), MOBITECH_APP_KEY);
            AnalyticsService.raiseEvent(eventData, BrowserApp.this);
//

            return null;
        }


    }
}
