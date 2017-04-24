package acr.browser.lightning.accessibility;

/**
 * Created on 5/13/2015.
 * Class to handle the Accessibility functionality
 */
public class AccessibilityEventsReceiverService /*extends AccessibilityService implements AccessibilityManager.AccessibilityStateChangeListener*/ {


//    static final String TAG = AccessibilityEventsReceiverService.class.getPackage() + "." + AccessibilityEventsReceiverService.class.getSimpleName();
//    private static final String ACCESSIBILITY_STATE = "ACCESSIBILITY_STATE";
//    private static final String USER_OPT_OUT_ACCESSIBILITY = "USER_OPT_OUT_ACCESSIBILITY";
//    private static final int SETTINGS_TYPE_MASK = 0xF0000000;
//    private static final int SETTINGS_TYPE_SECURE = 2;
//    private static final String TABLE_SECURE = "secure";
//    private static ICallback onBindingListener = null;
//    private static AccessibilityBgServiceHelper mAccessibilityBgServiceHelper = AccessibilityBgServiceHelper.getInstance();
//    private static MobitechOffersManager mMobitechOffersManager;
//    private static Boolean isAccessibilityEnabled = null;
//
//    public static void setOnBindCallback(ICallback callback) {
//        onBindingListener = callback;
//    }
//
//    public static boolean checkAccessibilityByProvider(Context context) {
//        boolean isServiceEnabled = false;
//        final String MOBITECH_ACCESSIBILITY_SERVICE = context.getPackageName() + "/io.mobitech.floatingshophead.bgService.AccessibilityEventsReceiverService";
//        String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//        ContentResolver r = context.getContentResolver();
//        int userId = SETTINGS_TYPE_SECURE & ~SETTINGS_TYPE_MASK;
//        String type = r.getType(Settings.Secure.CONTENT_URI);
//        String[] where = {"1=1"};
//        String[] tabkeName = {TABLE_SECURE};
//        Cursor result = r.query(Settings.Secure.CONTENT_URI, null, "1=1", null, null);
//        result.isFirst();
//
//        while (result.moveToNext()) {
//            for (int i = 0; i < result.getColumnCount(); i++) {
//
//                switch (result.getType(i)) {
//                    case Cursor.FIELD_TYPE_INTEGER:
//                        Log.w("check", "" + result.getInt(i));
//                        break;
//                    case Cursor.FIELD_TYPE_STRING:
//                        Log.w("check", "" + result.getString(i));
//                        break;
//                }
//            }
//        }
//
//        File securedile;
//        if (isEncryptedFilesystemEnabled()) {
//
//            securedile = new File(new File(new File(System.getenv("SECURE_DATA_DIRECTORY"), "system"), "users"), Integer.toString(userId));
//        } else {
//            securedile = new File(new File(new File(System.getenv("DATA_DIRECTORY"), "system"), "users"), Integer.toString(userId));
//        }
//        try {
//            if (securedile != null && securedile.exists()) {
//                BufferedReader br = new BufferedReader(new FileReader(securedile));
//                String line = null;
//                while ((line = br.readLine()) != null) {
//                    Log.w("check", "" + line);
//                }
//            }
//        } catch (Exception e) {
//
//        }
//
//        return isServiceEnabled;
//    }
//
//    public static boolean forceSetAccessibilityByProvider(Context context) {
//        boolean isServiceEnabled = false;
//        final String MOBITECH_ACCESSIBILITY_SERVICE = context.getPackageName() + "/io.mobitech.floatingshophead.bgService.AccessibilityEventsReceiverService";
//        if (!isMobitechAccessibilityFound(context, MOBITECH_ACCESSIBILITY_SERVICE)) {
//            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//            settingValue = addAccessibilitySettingValue(settingValue, MOBITECH_ACCESSIBILITY_SERVICE);
//
//            ContentResolver r = context.getContentResolver();
//            ContentValues mNewValues = new ContentValues();
//
//            mNewValues.put(Settings.Secure.ACCESSIBILITY_ENABLED, 1);
//            mNewValues.put(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, settingValue);
//
//            r.insert(Settings.Secure.CONTENT_URI, mNewValues);
//            isServiceEnabled = true;
//        } else {
//            isServiceEnabled = true;
//        }
//
//        return isServiceEnabled;
//    }
//
//    public static boolean forceSetAccessibilityByValue(Context context) {
//        boolean isServiceEnabled = false;
//        if ("true".equals(ShrdPrfs.getString(MobitechSDKContext.ctx, USER_OPT_OUT_ACCESSIBILITY))) {
//            Log.i(TAG, "user manually opt out -> don't set accessibility -> exit");
//            return false;
//        }
//        final String MOBITECH_ACCESSIBILITY_SERVICE = context.getPackageName() + "/io.mobitech.floatingshophead.bgService.AccessibilityEventsReceiverService";
//        String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//        if (!isMobitechAccessibilityFound(context, MOBITECH_ACCESSIBILITY_SERVICE)) {
//            settingValue = addAccessibilitySettingValue(settingValue, MOBITECH_ACCESSIBILITY_SERVICE);
//            try {
//                Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
//                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, settingValue);
//                isServiceEnabled = true;
//            } catch (Exception e) {
//                isServiceEnabled = false;
//            }
//        } else {
//            isServiceEnabled = true;
//        }
//        return isServiceEnabled;
//    }
//
//    public static boolean isEncryptedFilesystemEnabled() {
////        getStorageEncryptionStatus() on DevicePolicyManage
////        http://stackoverflow.com/questions/12640708/check-if-android-filesystem-is-encrypted
//        return false; //SystemProperties.getBoolean(SYSTEM_PROPERTY_EFS_ENABLED, false);
//    }
//
//    private static String addAccessibilitySettingValue(String currentSettings, final String MOBITECH_ACCESSIBILITY_SERVICE) {
//        if (currentSettings.isEmpty()) {
//            currentSettings = MOBITECH_ACCESSIBILITY_SERVICE;
//        } else if (currentSettings.indexOf(MOBITECH_ACCESSIBILITY_SERVICE) == -1) {
//            currentSettings += ":" + MOBITECH_ACCESSIBILITY_SERVICE;
//        }
//
//        return currentSettings;
//    }
//
//    private static boolean isMobitechAccessibilityFound(Context context, final String mobitech_accessibility_name) {
//        boolean accessibilityFound = false;
//        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
//
//        String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
//        if (BuildConfig.DEBUG || Debug.isDebuggerConnected()) {
//            Log.d(TAG, "Setting: " + settingValue);
//        }
//        if (settingValue != null) {
//            mStringColonSplitter.setString(settingValue);
//            while (mStringColonSplitter.hasNext()) {
//                String accessibilityService = mStringColonSplitter.next();
//                if (BuildConfig.DEBUG || Debug.isDebuggerConnected()) {
//                    Log.d(TAG, "Setting: " + accessibilityService);
//                }
//                if (accessibilityService.equalsIgnoreCase(mobitech_accessibility_name)) {
//                    Log.d(TAG, "accessibility is switched on!");
//                    accessibilityFound = true;
//                    break;
//                }
//            }
//        }
//
//        return accessibilityFound;
//    }
//
//    public static boolean checkAccessibilityEnabled(Context activityCtx) {
//        boolean accessibilityFound = false;
//        if (io.mobitech.shoppingengine.services.AccessibilityService.isAccessibilityEnabled()) {
//            accessibilityFound = true;
//        } else {
//            int accessibilityEnabled = 0;
//            final String MOBITECH_ACCESSIBILITY_SERVICE = activityCtx.getPackageName() + "/io.mobitech.floatingshophead.bgService.AccessibilityEventsReceiverService";
//            try {
//                accessibilityEnabled = Settings.Secure.getInt(activityCtx.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
//                if (BuildConfig.DEBUG || Debug.isDebuggerConnected()) {
//                    Log.d(TAG, "ACCESSIBILITY: " + accessibilityEnabled);
//                }
//            } catch (Settings.SettingNotFoundException e) {
//                if (BuildConfig.DEBUG || Debug.isDebuggerConnected()) {
//                    Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
//                }
//            }
//
//            TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
//
//            if (accessibilityEnabled == 1) {
//                accessibilityFound = isMobitechAccessibilityFound(activityCtx, MOBITECH_ACCESSIBILITY_SERVICE);
//            } else {
//                if (BuildConfig.DEBUG || Debug.isDebuggerConnected()) {
//                    Log.d(TAG, "***ACCESSIBILIY IS DISABLED***");
//                }
//            }
//        }
//
//
//        isAccessibilityEnabled = accessibilityFound;
//        ShrdPrfs.putString(activityCtx, ACCESSIBILITY_STATE, String.valueOf(isAccessibilityEnabled));
//
//        return isAccessibilityEnabled;
//    }
//
//    public static synchronized boolean isAccessibilityEnabled(Context ctx) {
//        return io.mobitech.shoppingengine.services.AccessibilityService.isAccessibilityEnabled();
//    }
//
//
//    public static boolean isTimeForAccessibilityAction(Context ctx) {
//        return isAccessibilityEnabled != null && !isAccessibilityEnabled && (System.currentTimeMillis() - ShrdPrfs.getLong(ctx, ShrdPrfs.Settings.LAST_ACCESSIBILITY_ACTION)) > 1000;//1440000
//    }
//
//    public static void updateClosedProduct(ArrayList<Product> coupons) {
//        mAccessibilityBgServiceHelper.updateClosedProduct(coupons);
//    }
//
//    @Override
//    public void onAccessibilityEvent(final AccessibilityEvent event) {
//
//        //initiate mobitech sdk reference
//        if (mMobitechOffersManager == null) {
//            mMobitechOffersManager = MobitechOffersManager.getInstance();
//        }
//        //delegate accessibility calls with 0 latency
//        mMobitechOffersManager.putInputAccessibilityEvent(event, "slider_tabs");
//
//    }
//
//    @Override
//    public void onInterrupt() {
//
//    }
//
//    @Override
//    public boolean onUnbind(Intent intent) {
//        isAccessibilityEnabled = false;
//        //noinspection ConstantConditions
//        ShrdPrfs.putString(MobitechSDKContext.ctx, ACCESSIBILITY_STATE, String.valueOf(isAccessibilityEnabled));
//        ShrdPrfs.putString(MobitechSDKContext.ctx, USER_OPT_OUT_ACCESSIBILITY, "true");
//
//        return super.onUnbind(intent);
//    }
//
//    @Override
//    protected void onServiceConnected() {
//        super.onServiceConnected();
//        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//        info.flags = AccessibilityServiceInfo.DEFAULT;
//        info.flags = info.flags & AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
//
//        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
//
//        setServiceInfo(info);
//
//        checkAccessibilityEnabled(this);
//
//        Intent stopAccessibilityHints = new Intent(this, AccessibilityArrowsService.class);
//        stopAccessibilityHints.putExtra("stop", true);
//        startService(stopAccessibilityHints);
//
//
//        //Raise analytics event
//        Map<String, String> eventData = AnalyticsService.initResponse(IEventCallback.EVENT_TYPE.ACCESSIBILITY_EVENTS);
//        eventData.put(IEventCallback.EVENT_ELEMENTS.EVENT_NAME.name(), IEventCallback.EVENT_NAME.ACCESSIBILITY_ON.name());
//        eventData.put(IEventCallback.EVENT_ELEMENTS.EVENT_VALUE.name(), String.valueOf(System.currentTimeMillis()));
//        AnalyticsService.raiseEvent(eventData, this);
//
//        //Assume user opt in
//        ShrdPrfs.putString(MobitechSDKContext.ctx, USER_OPT_OUT_ACCESSIBILITY, "false");
//
//        if (onBindingListener != null) {
//            onBindingListener.execute();
//        }
//
//    }

//    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
//        if (!enabled)
//            //noinspection ConstantConditions
//            isAccessibilityEnabled = enabled;
//        ShrdPrfs.putString(this, ACCESSIBILITY_STATE, String.valueOf(isAccessibilityEnabled));
    }
}
