package info.guardianproject.onionkit.trust;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ClientConnectionOperator;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;
import info.guardianproject.onionkit.R;
import info.guardianproject.onionkit.proxy.MyThreadSafeClientConnManager;
import info.guardianproject.onionkit.proxy.SocksProxyClientConnOperator;

public class StrongHttpsClient extends DefaultHttpClient {

    final Context context;
    private HttpHost proxyHost;
    private String proxyType;

    private StrongSSLSocketFactory sFactory;
    private TrustManager mTrustManager;
    private SchemeRegistry mRegistry;

    private final static String TRUSTSTORE_TYPE = "BKS";
    private final static String TRUSTSTORE_PASSWORD = "changeit";

    public StrongHttpsClient(Context context) {
        this.context = context;

        mRegistry = new SchemeRegistry();
        mRegistry.register(
                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));


        try {

            KeyStore keyStore = loadKeyStore();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    mTrustManager = trustManagerFactory.getTrustManagers()[0];
                }
            }

            sFactory = new StrongSSLSocketFactory(context, mTrustManager, keyStore, TRUSTSTORE_PASSWORD);
            mRegistry.register(new Scheme("https", 443, sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private KeyStore loadKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        KeyStore trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
        // load our bundled cacerts from raw assets
        InputStream in = context.getResources().openRawResource(R.raw.debiancacerts);
        trustStore.load(in, TRUSTSTORE_PASSWORD.toCharArray());

        return trustStore;
    }

    public StrongHttpsClient(Context context, KeyStore keystore) {
        this.context = context;

        mRegistry = new SchemeRegistry();
        mRegistry.register(
                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

        try {
            //mTrustManager = new StrongTrustManager(context, keystore);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    mTrustManager = trustManagerFactory.getTrustManagers()[0];
                }
            }

            sFactory = new StrongSSLSocketFactory(context, mTrustManager, keystore, TRUSTSTORE_PASSWORD);
            mRegistry.register(new Scheme("https", 443, sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected ThreadSafeClientConnManager createClientConnectionManager() {

        if (proxyHost == null && proxyType == null) {
            Log.d("StrongHTTPS", "not proxying");

            return new MyThreadSafeClientConnManager(getParams(), mRegistry);

        } else if (proxyHost != null && proxyType.equalsIgnoreCase("socks")) {
            Log.d("StrongHTTPS", "proxying using: " + proxyType);

            return new MyThreadSafeClientConnManager(getParams(), mRegistry) {

                @Override
                protected ClientConnectionOperator createConnectionOperator(
                        SchemeRegistry schreg) {

                    return new SocksProxyClientConnOperator(schreg, proxyHost.getHostName(),
                            proxyHost.getPort());
                }

            };
        } else {
            Log.d("StrongHTTPS", "proxying with: " + proxyType);

            return new MyThreadSafeClientConnManager(getParams(), mRegistry);
        }
    }

    public TrustManager getTrustManager() {
        return mTrustManager;
    }

    public void useProxy(boolean enableTor, String type, String host, int port) {

        if (proxyType != null) {
            getParams().removeParameter(proxyType);
            proxyHost = null;
        }

        if (enableTor) {
            this.proxyType = type;

            HttpHost proxyHost = new HttpHost(host, port);
            getParams().setParameter(type, proxyHost);

            if (type.equalsIgnoreCase("socks")) {
                this.proxyHost = proxyHost;
            }
        }

    }


}
