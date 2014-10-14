package info.guardianproject.onionkit.web;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Proxy;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;

import org.apache.http.HttpHost;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

public class WebkitProxy {

    private final static String DEFAULT_HOST = "localhost";//"127.0.0.1";
    private final static int DEFAULT_PORT = 8118;
    private final static int DEFAULT_SOCKS_PORT = 9050;

    private final static int REQUEST_CODE = 0;

    private final static String TAG = "OrbotHelpher";

    public static boolean setProxy(String appClass, Context ctx, String host, int port) throws Exception {

        //setSystemProperties(host, port);

        boolean worked = false;

        if (Build.VERSION.SDK_INT < 14) {
            worked = setWebkitProxyGingerbread(ctx, host, port);
        } else if (Build.VERSION.SDK_INT < 19) {
            worked = setWebkitProxyICS(ctx, host, port);
        } else {
            worked = setKitKatProxy(appClass, ctx, host, port);

            if (!worked) //some kitkat's still use ICS browser component (like Cyanogen 11)
                worked = setWebkitProxyICS(ctx, host, port);

        }

        return worked;
    }

    private static void setSystemProperties(String host, int port) {

        System.setProperty("proxyHost", host);
        System.setProperty("proxyPort", port + "");

        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port + "");

        System.setProperty("https.proxyHost", host);
        System.setProperty("https.proxyPort", port + "");


        System.setProperty("socks.proxyHost", host);
        System.setProperty("socks.proxyPort", DEFAULT_SOCKS_PORT + "");

        System.setProperty("socksProxyHost", host);
        System.setProperty("socksProxyPort", DEFAULT_SOCKS_PORT + "");
        
        
        /*
        ProxySelector pSelect = new ProxySelector();
        pSelect.addProxy(Proxy.Type.HTTP, host, port);
        ProxySelector.setDefault(pSelect);
        */
        /*
        System.setProperty("http_proxy", "http://" + host + ":" + port);
        System.setProperty("proxy-server", "http://" + host + ":" + port);
        System.setProperty("host-resolver-rules","MAP * 0.0.0.0 , EXCLUDE myproxy");

        System.getProperty("networkaddress.cache.ttl", "-1");
        */

    }

    /**
     * Override WebKit Proxy settings
     *
     * @param ctx  Android ApplicationContext
     * @param host
     * @param port
     * @return true if Proxy was successfully set
     */
    private static boolean setWebkitProxyGingerbread(Context ctx, String host, int port)
            throws Exception {

        boolean ret = false;

        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null) {
            // Create Proxy config object and set it into request Q
            HttpHost httpHost = new HttpHost(host, port, "http");
            setDeclaredField(requestQueueObject, "mProxyHost", httpHost);
            return true;
        }
        return false;

    }

    private static boolean setWebkitProxyICS(Context ctx, String host, int port) {

        // PSIPHON: added support for Android 4.x WebView proxy
        try {
            Class webViewCoreClass = Class.forName("android.webkit.WebViewCore");

            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (webViewCoreClass != null && proxyPropertiesClass != null) {
                Method m = webViewCoreClass.getDeclaredMethod("sendStaticMessage", Integer.TYPE,
                        Object.class);
                Constructor c = proxyPropertiesClass.getConstructor(String.class, Integer.TYPE,
                        String.class);

                if (m != null && c != null) {
                    m.setAccessible(true);
                    c.setAccessible(true);
                    Object properties = c.newInstance(host, port, null);

                    // android.webkit.WebViewCore.EventHub.PROXY_CHANGED = 193;
                    m.invoke(null, 193, properties);


                    return true;
                }


            }
        } catch (Exception e) {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.net.ProxyProperties: "
                            + e.toString());
        } catch (Error e) {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.webkit.Network: "
                            + e.toString());
        }

        return false;

    }

    @TargetApi(19)
    public static boolean resetKitKatProxy(String appClass, Context appContext) {

        return setKitKatProxy(appClass, appContext, null, 0);
    }

    @TargetApi(19)
    private static boolean setKitKatProxy(String appClass, Context appContext, String host, int port) {
        //Context appContext = webView.getContext().getApplicationContext();

        if (host != null) {
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port + "");
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", port + "");
        }

        try {
            Class applictionCls = Class.forName(appClass);
            Field loadedApkField = applictionCls.getField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(appContext);
            Class loadedApkCls = Class.forName("android.app.LoadedApk");
            Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);

                        if (host != null) {
                            /*********** optional, may be need in future *************/
                            final String CLASS_NAME = "android.net.ProxyProperties";
                            Class cls = Class.forName(CLASS_NAME);
                            Constructor constructor = cls.getConstructor(String.class, Integer.TYPE, String.class);
                            constructor.setAccessible(true);
                            Object proxyProperties = constructor.newInstance(host, port, null);
                            intent.putExtra("proxy", (Parcelable) proxyProperties);
                            /*********** optional, may be need in future *************/
                        }

                        onReceiveMethod.invoke(rec, appContext, intent);
                    }
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        } catch (NoSuchFieldException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        } catch (IllegalAccessException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        } catch (IllegalArgumentException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        } catch (NoSuchMethodException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        } catch (InvocationTargetException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        } catch (InstantiationException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
        }
        return false;
    }

    private static boolean sendProxyChangedIntent(Context ctx, String host, int port) {

        try {
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (proxyPropertiesClass != null) {
                Constructor c = proxyPropertiesClass.getConstructor(String.class, Integer.TYPE,
                        String.class);

                if (c != null) {
                    c.setAccessible(true);
                    Object properties = c.newInstance(host, port, null);

                    Intent intent = new Intent(android.net.Proxy.PROXY_CHANGE_ACTION);
                    intent.putExtra("proxy", (Parcelable) properties);
                    ctx.sendBroadcast(intent);

                }

            }
        } catch (Exception e) {
            Log.e("ProxySettings",
                    "Exception sending Intent ", e);
        } catch (Error e) {
            Log.e("ProxySettings",
                    "Exception sending Intent ", e);
        }

        return false;

    }

    /**
     private static boolean setKitKatProxy0(Context ctx, String host, int port)
     {

     try
     {
     Class cmClass = Class.forName("android.net.ConnectivityManager");

     Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
     if (cmClass != null && proxyPropertiesClass != null)
     {
     Constructor c = proxyPropertiesClass.getConstructor(String.class, Integer.TYPE,
     String.class);

     if (c != null)
     {
     c.setAccessible(true);

     Object proxyProps = c.newInstance(host, port, null);
     ConnectivityManager cm =
     (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

     Method mSetGlobalProxy = cmClass.getDeclaredMethod("setGlobalProxy", proxyPropertiesClass);

     mSetGlobalProxy.invoke(cm, proxyProps);

     return true;
     }

     }
     } catch (Exception e)
     {
     Log.e("ProxySettings",
     "ConnectivityManager.setGlobalProxy ",e);
     }

     return false;

     }
     */
    //CommandLine.initFromFile(COMMAND_LINE_FILE);

    /**
     * private static boolean setKitKatProxy2 (Context ctx, String host, int port)
     * {
     * <p/>
     * String commandLinePath = "/data/local/tmp/orweb.conf";
     * try
     * {
     * Class webViewCoreClass = Class.forName("org.chromium.content.common.CommandLine");
     * <p/>
     * if (webViewCoreClass != null)
     * {
     * for (Method method : webViewCoreClass.getDeclaredMethods())
     * {
     * Log.d("Orweb","Proxy methods: " + method.getName());
     * }
     * <p/>
     * Method m = webViewCoreClass.getDeclaredMethod("initFromFile",
     * String.class);
     * <p/>
     * if (m != null)
     * {
     * m.setAccessible(true);
     * m.invoke(null, commandLinePath);
     * return true;
     * }
     * else
     * return false;
     * }
     * } catch (Exception e)
     * {
     * Log.e("ProxySettings",
     * "Exception setting WebKit proxy through android.net.ProxyProperties: "
     * + e.toString());
     * } catch (Error e)
     * {
     * Log.e("ProxySettings",
     * "Exception setting WebKit proxy through android.webkit.Network: "
     * + e.toString());
     * }
     * <p/>
     * return false;
     * }
     * <p/>
     * /**
     * private static boolean setKitKatProxy (Context ctx, String host, int port)
     * {
     * <p/>
     * try
     * {
     * Class webViewCoreClass = Class.forName("android.net.Proxy");
     * <p/>
     * Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
     * if (webViewCoreClass != null && proxyPropertiesClass != null)
     * {
     * for (Method method : webViewCoreClass.getDeclaredMethods())
     * {
     * Log.d("Orweb","Proxy methods: " + method.getName());
     * }
     * <p/>
     * Method m = webViewCoreClass.getDeclaredMethod("setHttpProxySystemProperty",
     * proxyPropertiesClass);
     * Constructor c = proxyPropertiesClass.getConstructor(String.class, Integer.TYPE,
     * String.class);
     * <p/>
     * if (m != null && c != null)
     * {
     * m.setAccessible(true);
     * c.setAccessible(true);
     * Object properties = c.newInstance(host, port, null);
     * <p/>
     * m.invoke(null, properties);
     * return true;
     * }
     * else
     * return false;
     * }
     * } catch (Exception e)
     * {
     * Log.e("ProxySettings",
     * "Exception setting WebKit proxy through android.net.ProxyProperties: "
     * + e.toString());
     * } catch (Error e)
     * {
     * Log.e("ProxySettings",
     * "Exception setting WebKit proxy through android.webkit.Network: "
     * + e.toString());
     * }
     * <p/>
     * return false;
     * }
     * <p/>
     * private static boolean resetProxyForKitKat ()
     * {
     * <p/>
     * try
     * {
     * Class webViewCoreClass = Class.forName("android.net.Proxy");
     * <p/>
     * Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
     * if (webViewCoreClass != null && proxyPropertiesClass != null)
     * {
     * for (Method method : webViewCoreClass.getDeclaredMethods())
     * {
     * Log.d("Orweb","Proxy methods: " + method.getName());
     * }
     * <p/>
     * Method m = webViewCoreClass.getDeclaredMethod("setHttpProxySystemProperty",
     * proxyPropertiesClass);
     * <p/>
     * if (m != null)
     * {
     * m.setAccessible(true);
     * <p/>
     * m.invoke(null, null);
     * return true;
     * }
     * else
     * return false;
     * }
     * } catch (Exception e)
     * {
     * Log.e("ProxySettings",
     * "Exception setting WebKit proxy through android.net.ProxyProperties: "
     * + e.toString());
     * } catch (Error e)
     * {
     * Log.e("ProxySettings",
     * "Exception setting WebKit proxy through android.webkit.Network: "
     * + e.toString());
     * }
     * <p/>
     * return false;
     * }*
     */

    public static void resetProxy(String appClass, Context ctx) throws Exception {


        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");


        if (Build.VERSION.SDK_INT < 14) {
            resetProxyForGingerBread(ctx);
        } else if (Build.VERSION.SDK_INT < 19) {
            resetProxyForICS();
        } else {
            resetKitKatProxy(appClass, ctx);
        }

    }

    private static void resetProxyForICS() throws Exception {
        try {
            Class webViewCoreClass = Class.forName("android.webkit.WebViewCore");
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (webViewCoreClass != null && proxyPropertiesClass != null) {
                Method m = webViewCoreClass.getDeclaredMethod("sendStaticMessage", Integer.TYPE,
                        Object.class);

                if (m != null) {
                    m.setAccessible(true);

                    // android.webkit.WebViewCore.EventHub.PROXY_CHANGED = 193;
                    m.invoke(null, 193, null);
                }
            }
        } catch (Exception e) {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.net.ProxyProperties: "
                            + e.toString());
            throw e;
        } catch (Error e) {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.webkit.Network: "
                            + e.toString());
            throw e;
        }
    }

    private static void resetProxyForGingerBread(Context ctx) throws Exception {
        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null) {
            setDeclaredField(requestQueueObject, "mProxyHost", null);
        }
    }

    public static Object getRequestQueue(Context ctx) throws Exception {
        Object ret = null;
        Class networkClass = Class.forName("android.webkit.Network");
        if (networkClass != null) {
            Object networkObj = invokeMethod(networkClass, "getInstance", new Object[]{
                    ctx
            }, Context.class);
            if (networkObj != null) {
                ret = getDeclaredField(networkObj, "mRequestQueue");
            }
        }
        return ret;
    }

    private static Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        // System.out.println(obj.getClass().getName() + "." + name + " = "+
        // out);
        return out;
    }

    private static void setDeclaredField(Object obj, String name, Object value)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object invokeMethod(Object object, String methodName, Object[] params,
                                       Class... types) throws Exception {
        Object out = null;
        Class c = object instanceof Class ? (Class) object : object.getClass();
        if (types != null) {
            Method method = c.getMethod(methodName, types);
            out = method.invoke(object, params);
        } else {
            Method method = c.getMethod(methodName);
            out = method.invoke(object);
        }
        // System.out.println(object.getClass().getName() + "." + methodName +
        // "() = "+ out);
        return out;
    }

    public static Socket getSocket(Context context, String proxyHost, int proxyPort)
            throws IOException {
        Socket sock = new Socket();

        sock.connect(new InetSocketAddress(proxyHost, proxyPort), 10000);

        return sock;
    }

    public static Socket getSocket(Context context) throws IOException {
        return getSocket(context, DEFAULT_HOST, DEFAULT_SOCKS_PORT);

    }

    public static AlertDialog initOrbot(Activity activity,
                                        CharSequence stringTitle,
                                        CharSequence stringMessage,
                                        CharSequence stringButtonYes,
                                        CharSequence stringButtonNo,
                                        CharSequence stringDesiredBarcodeFormats) {
        Intent intentScan = new Intent("org.torproject.android.START_TOR");
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);

        try {
            activity.startActivityForResult(intentScan, REQUEST_CODE);
            return null;
        } catch (ActivityNotFoundException e) {
            return showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes,
                    stringButtonNo);
        }
    }

    private static AlertDialog showDownloadDialog(final Activity activity,
                                                  CharSequence stringTitle,
                                                  CharSequence stringMessage,
                                                  CharSequence stringButtonYes,
                                                  CharSequence stringButtonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(stringTitle);
        downloadDialog.setMessage(stringMessage);
        downloadDialog.setPositiveButton(stringButtonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:org.torproject.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(intent);
            }
        });
        downloadDialog.setNegativeButton(stringButtonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }


}
