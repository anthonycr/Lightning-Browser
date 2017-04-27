package acr.browser.lightning.utils;

import android.content.Context;

import com.segment.analytics.Analytics;

import java.util.Map;

import io.mobitech.commonlibrary.analytics.IEventCallback;

import static io.mobitech.commonlibrary.analytics.AnalyticsService.optOutEnabled;

/**
 * Created on 24-Apr-17.
 */

public class AnalyticsUtil {

    private static final String TAG = AnalyticsUtil.class.getName();
    private static Analytics analytics = null;
    static boolean isIdentified = false;

    public static void initiate(Context context){
// Create an analytics client with the given context and Segment write key.
        analytics = new Analytics.Builder(context, "4zWrl8ocziBp2lTLeGOFzfcD9Hp1oLGS")
                // Enable this to record certain application events automatically!
                .trackApplicationLifecycleEvents()
                // Enable this to record screen views automatically!
                .recordScreenViews()
                .build();

// Set the initialized instance as a globally accessible instance.
        Analytics.setSingletonInstance(analytics);
    }

    public static void raiseEvent(String event, Context context){
        if (analytics==null){
            initiate(context);
        }
        Analytics.with(context).track("Application Started");
    }

    public static Map<String,String> addID(Map<String,String> eventValues, String id){
        if (!optOutEnabled){
            eventValues.put(IEventCallback.EVENT_ELEMENTS.ID.name(), id);
        }
        return eventValues;
    }

    private void identifyUser(Map<String, String> eventData, Context context) {
        if (isIdentified){
            return;
        }

        if (eventData.containsKey(IEventCallback.EVENT_ELEMENTS.ID.name())){
            String id = eventData.get(IEventCallback.EVENT_ELEMENTS.ID.name());
            if (id!=null && !id.isEmpty()){
                isIdentified = true;
                Analytics.with(context).alias(id);
                Analytics.with(context).identify(id);
                Analytics.with(context).track("identify-via-sdk");
            }
        }

    }

}
