package info.guardianproject.onionkit.trust;

/**
 * $RCSfile$ $Revision: $ $Date: $
 *
 * Copyright 2003-2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1String;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.asn1.x509.X509Extensions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import info.guardianproject.onionkit.R;
import info.guardianproject.onionkit.ui.CertDisplayActivity;

/**
 * Updated multifaceted StrongTrustManager Based on TrustManager from Jive:
 * Trust manager that checks all certificates presented by the server. This
 * class is used during TLS negotiation. It is possible to disable/enable some
 * or all checkings by configuring the {@link ConnectionConfiguration}. The
 * truststore file that contains knows and trusted CA root certificates can also
 * be configure in {@link ConnectionConfiguration}.
 *
 * @author Gaston Dombiak
 * @autor n8fr8
 */
public abstract class StrongTrustManager implements X509TrustManager {

    private static final String TAG = "ONIONKIT";
    public static boolean SHOW_DEBUG_OUTPUT = true;

    private final static Pattern cnPattern = Pattern.compile("(?i)(cn=)([^,]*)");

    private final static String TRUSTSTORE_TYPE = "BKS";
    private final static String TRUSTSTORE_PASSWORD = "changeit";

    private int DEFAULT_NOTIFY_ID = 10;

    /**
     * Holds the domain of the remote server we are trying to connect
     */
    private String mServer;
    private String mDomain;

    private KeyStore mTrustStore; // root CAs
    private Context mContext;

    private int mAppIcon = R.drawable.ic_menu_key;
    private String mAppName = null;

    boolean mExpiredCheck = true;
    boolean mVerifyChain = true;
    boolean mVerifyRoot = true;
    boolean mSelfSignedAllowed = false;
    boolean mCheckMatchingDomain = true;
    boolean mCheckChainCrypto = true;

    boolean mNotifyVerificationSuccess = false;
    boolean mNotifyVerificationFail = true;

    /**
     * Construct a trust manager for XMPP connections. Certificates are
     * considered verified if:
     * <ul>
     * <li>The root certificate is in our trust store
     * <li>The chain is valid
     * <li>The leaf certificate contains the identity of the domain or the
     * requested server
     * </ul>
     *
     * @param mContext      - the Android mContext for presenting notifications
     * @param configuration - the XMPP configuration
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public StrongTrustManager(Context context) throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {

        mContext = context;

        InputStream in = null;

        mTrustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
        // load our bundled cacerts from raw assets
        in = mContext.getResources().openRawResource(R.raw.debiancacerts);
        mTrustStore.load(in, TRUSTSTORE_PASSWORD.toCharArray());

        mAppName = mContext.getApplicationInfo().name;
    }

    public StrongTrustManager(Context context, KeyStore keystore) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {

        mContext = context;
        mTrustStore = keystore;

        mAppName = mContext.getApplicationInfo().name;
    }

    public KeyStore getKeyStore() {
        return mTrustStore;
    }

    /**
     * Construct a trust manager for XMPP connections. Certificates are
     * considered verified if:
     * <ul>
     * <li>The root certificate is in our trust store
     * <li>The chain is valid
     * <li>The leaf certificate contains the identity of the domain or the
     * requested server
     * </ul>
     *
     * @param mContext      - the Android mContext for presenting notifications
     * @param appIcon       - optional icon to show in notifications
     * @param configuration - the XMPP configuration
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public StrongTrustManager(Context mContext, String appName, int appIcon)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        this(mContext);

        mAppIcon = appIcon;
        mAppName = appName;
    }

    public void setAppIcon(int appIcon) {
        mAppIcon = appIcon;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0]; // we accept anyone now, but this should
        // return the list from our trust Root CA
        // Store
    }

    /**
     * @Override public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
     * <p/>
     * debug("WARNING: Client Cert Trust NOT YET IMPLEMENTED");
     * }
     * @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String keyExchangeAlgo)
     * throws CertificateException {
     * <p/>
     * // first check the main cert
     * X509Certificate certSite = x509Certificates[0];
     * checkStrongCrypto(certSite);
     * <p/>
     * if (mExpiredCheck)
     * certSite.checkValidity();
     * <p/>
     * String fingerprint = null;
     * <p/>
     * try {
     * fingerprint = getFingerprint(certSite, "SHA-1");
     * } catch (Exception e)
     * {
     * debug("could not get cert fingperint: " + e.getMessage());
     * }
     * <p/>
     * // then go through the chain
     * if (mVerifyChain)
     * {
     * boolean verifiedRootCA = false;
     * <p/>
     * // for every certificate in the chain,
     * // verify its issuer exists in the chain, or our local root CA store
     * for (int i = 0; i < x509Certificates.length; i++)
     * {
     * X509Certificate x509certCurr = x509Certificates[i];
     * <p/>
     * debug(i + ": verifying cert issuer for: " + x509certCurr.getSubjectDN() + "; "
     * + x509certCurr.getSigAlgName());
     * <p/>
     * X509Certificate x509issuer = null;
     * boolean isLocalRootCA = false;
     * <p/>
     * for (X509Certificate x509search : x509Certificates)
     * {
     * if (checkSubjectMatchesIssuer(x509search.getSubjectX500Principal(),
     * x509certCurr.getIssuerX500Principal()))
     * {
     * x509issuer = x509search;
     * debug("found issuer for current cert in chain: "
     * + x509issuer.getSubjectDN() + "; " + x509certCurr.getSigAlgName());
     * <p/>
     * // now check if it is a root
     * X509Certificate x509root = findCertIssuerInStore(x509certCurr, mTrustStore);
     * if (x509root != null) {
     * debug("got root cert: " + x509root.getSubjectDN());
     * isLocalRootCA = true;
     * }
     * <p/>
     * break;
     * }
     * }
     * <p/>
     * // this is now verifying against the root store
     * // did not find signing cert in chain, so check our store
     * if (x509issuer == null)
     * {
     * x509issuer = findCertIssuerInStore(x509certCurr, mTrustStore);
     * isLocalRootCA = true;
     * }
     * <p/>
     * if (x509issuer != null) {
     * <p/>
     * try {
     * // check expiry
     * x509issuer.checkValidity();
     * <p/>
     * if (!isLocalRootCA)
     * {
     * boolean foundInChain = false;
     * <p/>
     * // make sure there isn't the same named cert in the
     * // chain, that is not meant for signing
     * for (X509Certificate x509search : x509Certificates)
     * {
     * if (x509issuer.getSubjectDN().equals(x509search.getSubjectDN()))
     * {
     * debug("found matching subject cert in chain: verifying it can act as CA: "
     * + x509issuer.getSubjectDN());
     * checkBasicConstraints(x509search);
     * checkKeyUsage(x509search);
     * foundInChain = true;
     * }
     * }
     * <p/>
     * if (!foundInChain)// this should not happen, but
     * // just in case
     * {
     * throw new GeneralSecurityException(
     * "Error verifying cert extension: "
     * + x509issuer.getSubjectDN());
     * }
     * }
     * <p/>
     * // isRootCA means we have it in our local store; can
     * // meet root CA or any chain we have imported like
     * // CACert's
     * // MD5 collision not a risk for the Root CA in our store
     * if ((!isLocalRootCA) && mCheckChainCrypto)
     * checkStrongCrypto(x509issuer);
     * <p/>
     * // verify cert with issuer public key
     * x509certCurr.verify(x509issuer.getPublicKey());
     * debug("SUCCESS: verified issuer: " + x509certCurr.getIssuerDN());
     * <p/>
     * if (isLocalRootCA)
     * verifiedRootCA = true;
     * }
     * <p/>
     * catch (GeneralSecurityException gse) {
     * <p/>
     * if (SHOW_DEBUG_OUTPUT)
     * Log.d(TAG, "cert general security exception", gse);
     * <p/>
     * debug("ERROR: invalid or unverifiable issuer: "
     * + x509certCurr.getIssuerDN());
     * <p/>
     * if (mNotifyVerificationFail)
     * showCertMessage(
     * mContext.getString(R.string.error_signature_chain_verification_failed)
     * + gse.getMessage(),
     * x509issuer.getIssuerDN().getName(), x509issuer, fingerprint);
     * <p/>
     * throw new CertificateException(
     * mContext.getString(R.string.error_signature_chain_verification_failed)
     * + x509issuer.getIssuerDN().getName()
     * + ": "
     * + gse.getMessage());
     * }
     * }
     * else {
     * <p/>
     * String errMsg = mContext
     * .getString(R.string.error_could_not_find_cert_issuer_certificate_in_chain)
     * + x509certCurr.getIssuerDN().getName();
     * <p/>
     * debug(errMsg);
     * <p/>
     * if (mNotifyVerificationFail)
     * showCertMessage(errMsg,
     * x509certCurr.getIssuerDN().getName(), x509certCurr, fingerprint);
     * <p/>
     * throw new CertificateException(errMsg);
     * }
     * <p/>
     * }
     * <p/>
     * if (mVerifyRoot && (!verifiedRootCA))
     * {
     * String errMsg = mContext
     * .getString(R.string.error_could_not_find_root_ca_issuer_certificate_in_chain);
     * <p/>
     * debug(errMsg);
     * <p/>
     * if (mNotifyVerificationFail)
     * showCertMessage(errMsg,
     * x509Certificates[0].getIssuerDN().getName(), x509Certificates[0],
     * fingerprint);
     * <p/>
     * throw new CertificateException(errMsg);
     * }
     * }
     * else if (mExpiredCheck)
     * {
     * // at least check the validity of the chain
     * for (X509Certificate x509cert : x509Certificates)
     * x509cert.checkValidity();
     * <p/>
     * }
     * <p/>
     * if (mSelfSignedAllowed)
     * {
     * boolean foundSelfSig = false;
     * <p/>
     * // for every certificate in the chain,
     * // verify its issuer exists in the chain, or our local root CA store
     * for (int i = 0; i < x509Certificates.length; i++)
     * {
     * X509Certificate x509certCurr = x509Certificates[i];
     * <p/>
     * debug(i + ": verifying cert issuer for: " + x509certCurr.getSubjectDN());
     * <p/>
     * X509Certificate x509issuer = null;
     * <p/>
     * for (X509Certificate x509search : x509Certificates)
     * {
     * if (checkSubjectMatchesIssuer(x509search.getSubjectX500Principal(),
     * x509certCurr.getIssuerX500Principal()))
     * {
     * x509issuer = x509search;
     * debug("found issuer for current cert in chain: "
     * + x509issuer.getSubjectDN());
     * <p/>
     * // check expiry
     * x509issuer.checkValidity();
     * <p/>
     * try {
     * x509certCurr.verify(x509issuer.getPublicKey());
     * foundSelfSig = true;
     * }
     * <p/>
     * catch (GeneralSecurityException gse) {
     * debug("ERROR: unverified issuer: " + x509certCurr.getIssuerDN());
     * <p/>
     * if (mNotifyVerificationFail)
     * showCertMessage(
     * mContext.getString(R.string.error_signature_chain_verification_failed)
     * + gse.getMessage(),
     * x509issuer.getIssuerDN().getName(), x509issuer, fingerprint);
     * <p/>
     * throw new CertificateException(
     * mContext.getString(R.string.error_signature_chain_verification_failed)
     * + x509issuer.getIssuerDN().getName()
     * + ": "
     * + gse.getMessage());
     * }
     * <p/>
     * debug("SUCCESS: verified issuer: " + x509certCurr.getIssuerDN());
     * <p/>
     * break;
     * }
     * }
     * }
     * <p/>
     * if (!foundSelfSig)
     * {
     * String errMsg = mContext
     * .getString(R.string.could_not_find_self_signed_certificate_in_chain);
     * <p/>
     * debug(errMsg);
     * <p/>
     * if (mNotifyVerificationFail)
     * showCertMessage(errMsg,
     * x509Certificates[0].getIssuerDN().getName(), x509Certificates[0],
     * fingerprint);
     * <p/>
     * throw new CertificateException(errMsg);
     * }
     * }
     * <p/>
     * if (mCheckMatchingDomain && mDomain != null && mServer != null)
     * {
     * // get peer identities available in the first cert in the chain
     * Collection<String> peerIdentities = getPeerIdentity(x509Certificates[0]);
     * <p/>
     * // Verify that the first certificate in the chain corresponds to
     * // the server we desire to authenticate.
     * boolean found = checkMatchingDomain(mDomain, mServer, peerIdentities);
     * <p/>
     * if (!found) {
     * <p/>
     * if (mNotifyVerificationFail)
     * showCertMessage(
     * mContext.getString(R.string.error_domain_check_failed),
     * join(peerIdentities)
     * + mContext.getString(R.string.error_does_not_contain_)
     * + "'" + mServer + "' or '" + mDomain + "'",
     * x509Certificates[0], fingerprint);
     * <p/>
     * throw new CertificateException("target verification failed of " + peerIdentities);
     * }
     * }
     * <p/>
     * if (mNotifyVerificationSuccess)
     * showCertMessage(mContext.getString(R.string.secure_connection_active_)
     * + certSite.getSubjectDN().getName(),
     * certSite.getSubjectDN().getName(), certSite, fingerprint);
     * <p/>
     * }
     */

    public void setNotifyVerificationSuccess(boolean notifyVerificationSuccess) {
        mNotifyVerificationSuccess = notifyVerificationSuccess;
    }

    public void setNotifyVerificationFail(boolean notifyVerificationFail) {
        mNotifyVerificationFail = notifyVerificationFail;
    }

    static boolean checkMatchingDomain(String domain, String server,
                                       Collection<String> peerIdentities) {
        boolean found = false;

        for (String peerIdentity : peerIdentities) {
            // Check if the certificate uses a wildcard.
            // This indicates that immediate subdomains are valid.
            if (peerIdentity.startsWith("*.")) {
                // Remove wildcard: *.foo.info -> .foo.info
                String stem = peerIdentity.substring(1);

                // Remove a single label: baz.bar.foo.info -> .bar.foo.info and
                // compare
                if (server.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)
                        || domain.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)) {
                    found = true;
                    break;
                }
            } else if (server.equalsIgnoreCase(peerIdentity)
                    || domain.equalsIgnoreCase(peerIdentity)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private String join(Collection<String> strs) {
        boolean first = true;
        StringBuffer buf = new StringBuffer();
        for (String str : strs) {
            if (!first) {
                buf.append(':');
            }
            first = false;
            buf.append(str);
        }
        return buf.toString();
    }

    private X509Certificate findCertIssuerInStore(X509Certificate x509cert, KeyStore kStore)
            throws CertificateException {
        X509Certificate x509issuer = null;

        debug("searching store for issuer: " + x509cert.getIssuerDN());

        // check in our local root CA Store
        Enumeration<String> enumAliases;
        try {
            enumAliases = kStore.aliases();
            X509Certificate x509search = null;
            while (enumAliases.hasMoreElements()) {
                x509search = (X509Certificate) kStore
                        .getCertificate(enumAliases.nextElement());

                if (checkSubjectMatchesIssuer(x509search.getSubjectX500Principal(),
                        x509cert.getIssuerX500Principal())) {
                    x509issuer = x509search;
                    debug("found issuer for current cert in chain in ROOT CA store: "
                            + x509issuer.getSubjectDN());

                    break;
                }
            }
        } catch (KeyStoreException e) {

            String errMsg = mContext.getString(R.string.error_problem_access_local_root_ca_store);
            debug(errMsg);

            throw new CertificateException(errMsg);
        }

        return x509issuer;
    }

    private void showCertMessage(String title, String msg, X509Certificate cert, String fingerprint) {

        Intent nIntent = new Intent(mContext, CertDisplayActivity.class);

        nIntent.putExtra("issuer", cert.getIssuerDN().getName());
        nIntent.putExtra("subject", cert.getSubjectDN().getName());

        if (fingerprint != null)
            nIntent.putExtra("fingerprint", fingerprint);

        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        nIntent.putExtra("issued", df.format(cert.getNotBefore()) + " GMT");
        nIntent.putExtra("expires", df.format(cert.getNotAfter()) + " GMT");
        nIntent.putExtra("msg", title + ": " + msg);

        showToolbarNotification(title, msg, DEFAULT_NOTIFY_ID, mAppIcon,
                Notification.FLAG_AUTO_CANCEL, nIntent);

    }

    private void showToolbarNotification(String title, String notifyMsg, int notifyId, int icon,
                                         int flags, Intent nIntent) {

        NotificationManager mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancelAll();

        CharSequence tickerText = null;

        if (mAppName != null)
            tickerText = mAppName + ": " + title;
        else
            tickerText = title;

        long when = System.currentTimeMillis();
        CharSequence contentTitle = title;
        CharSequence contentText = notifyMsg;
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, nIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(mContext)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setWhen(when)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .build();

        if (flags > 0) {
            notification.flags |= flags;
        }

        mNotificationManager.notify(notifyId, notification);
    }

    /**
     * Returns the identity of the remote server as defined in the specified
     * certificate. The identity is defined in the subjectDN of the certificate
     * and it can also be defined in the subjectAltName extension of type
     * "xmpp". When the extension is being used then the identity defined in the
     * extension in going to be returned. Otherwise, the value stored in the
     * subjectDN is returned.
     *
     * @param x509Certificate the certificate the holds the identity of the
     *                        remote server.
     * @return the identity of the remote server as defined in the specified
     * certificate.
     */
    public static Collection<String> getPeerIdentity(X509Certificate x509Certificate) {
        // Look the identity in the subjectAltName extension if available
        Collection<String> names = getSubjectAlternativeNames(x509Certificate);
        if (names.isEmpty()) {
            String name = x509Certificate.getSubjectDN().getName();
            Matcher matcher = cnPattern.matcher(name);
            if (matcher.find()) {
                name = matcher.group(2);
            }
            // Create an array with the unique identity
            names = new ArrayList<String>();
            names.add(name);
        }
        return names;
    }

    /**
     * Returns the JID representation of an XMPP entity contained as a
     * SubjectAltName extension in the certificate. If none was found then
     * return <tt>null</tt>.
     *
     * @param certificate the certificate presented by the remote entity.
     * @return the JID representation of an XMPP entity contained as a
     * SubjectAltName extension in the certificate. If none was found
     * then return <tt>null</tt>.
     */
    static Collection<String> getSubjectAlternativeNames(X509Certificate certificate) {
        List<String> identities = new ArrayList<String>();
        try {
            byte[] extVal = certificate.getExtensionValue(X509Extensions.SubjectAlternativeName
                    .getId());
            // Check that the certificate includes the SubjectAltName extension
            if (extVal == null) {
                return Collections.emptyList();
            }

            ASN1OctetString octs = (ASN1OctetString) ASN1Primitive.fromByteArray(extVal);

            @SuppressWarnings("rawtypes")
            Enumeration it = DERSequence.getInstance(ASN1Primitive.fromByteArray(octs.getOctets()))
                    .getObjects();

            while (it.hasMoreElements()) {
                GeneralName genName = GeneralName.getInstance(it.nextElement());
                switch (genName.getTagNo()) {
                    case GeneralName.dNSName:
                        identities.add(((ASN1String) genName.getName()).getString());
                        break;
                }
            }
            return Collections.unmodifiableCollection(identities);

        } catch (Exception e) {
            Log.e(TAG, "getSubjectAlternativeNames()", e);
        }

        return identities;
    }

    public String getFingerprint(X509Certificate cert, String type)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance(type);
        byte[] publicKey = md.digest(cert.getEncoded());

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < publicKey.length; i++) {

            String appendString = Integer.toHexString(0xFF & publicKey[i]);

            if (appendString.length() == 1)
                hexString.append("0");
            hexString.append(appendString);
            hexString.append(' ');
        }

        return hexString.toString();

    }

    private boolean checkSubjectMatchesIssuer(X500Principal subject, X500Principal issuer) {
        boolean result = false;

        // byte by byte check
        if (Arrays.equals(subject.getEncoded(), issuer.getEncoded()))
            // name check
            if (subject.getName("RFC1779").equals(issuer.getName("RFC1779")))
                result = true;

        return result;
    }

    private void debug(String msg) {
        if (SHOW_DEBUG_OUTPUT)
            Log.d(TAG, msg);
    }

    private void checkStrongCrypto(X509Certificate cert) throws CertificateException {
        String algo = cert.getSigAlgName().toLowerCase();

        if (algo.contains("md5")) {
            debug("cert uses weak crypto: " + algo);

            if (mNotifyVerificationFail)
                showCertMessage(mContext.getString(R.string.warning_weak_crypto),
                        cert.getIssuerDN().getName(), cert, null);

            throw new CertificateException("issuer uses weak crypto: " + algo);
        }

    }

    public KeyStore getTrustStore() {
        return mTrustStore;
    }

    public String getTrustStorePassword() {
        return TRUSTSTORE_PASSWORD;
    }

    public void setTrustStore(KeyStore mTrustStore) {
        this.mTrustStore = mTrustStore;
    }

    public boolean isExpiredCheck() {
        return mExpiredCheck;
    }

    public void setExpiredCheck(boolean mExpiredCheck) {
        this.mExpiredCheck = mExpiredCheck;
    }

    public boolean isVerifyChain() {
        return mVerifyChain;
    }

    public void setVerifyChain(boolean mVerifyChain) {
        this.mVerifyChain = mVerifyChain;
    }

    public boolean isVerifyRoot() {
        return mVerifyRoot;
    }

    public void setVerifyRoot(boolean mVerifyRoot) {
        this.mVerifyRoot = mVerifyRoot;
    }

    public boolean isSelfSignedAllowed() {
        return mSelfSignedAllowed;
    }

    public void setSelfSignedAllowed(boolean mSelfSignedAllowed) {
        this.mSelfSignedAllowed = mSelfSignedAllowed;
    }

    public boolean isCheckMatchingDomain() {
        return mCheckMatchingDomain;
    }

    public void setCheckMatchingDomain(boolean mCheckMatchingDomain) {
        this.mCheckMatchingDomain = mCheckMatchingDomain;
    }

    public String getServer() {
        return mServer;
    }

    public void setServer(String server) {
        this.mServer = server;
    }

    public String getDomain() {
        return mDomain;
    }

    public void setDomain(String domain) {
        this.mDomain = domain;
    }

    public boolean hasCheckChainCrypto() {
        return mCheckChainCrypto;
    }

    public void setCheckChainCrypto(boolean mCheckChainCrypto) {
        this.mCheckChainCrypto = mCheckChainCrypto;
    }

    /*
     * Ensure that a cert that is signing another cert, is actually allowed to
     * do so by checking the KeyUsage x509 certificate extension
     */
    private void checkKeyUsage(X509Certificate cert) throws GeneralSecurityException {
        try {
            Object bsVal = getExtensionValue(cert, X509Extensions.KeyUsage.getId(), KeyUsage.class);

            if (bsVal != null && bsVal instanceof KeyUsage) {
                KeyUsage keyUsage = (KeyUsage) bsVal;
                // SSLCA: CERT_SIGN; SSL_CA;+
//                debug("KeyUsage=" + keyUsage.intValue() + ";" + keyUsage.getString());

                if (keyUsage.hasUsages(KeyUsage.cRLSign)
                        && keyUsage.hasUsages(KeyUsage.keyCertSign)) {
                    // we okay
                } else
                    throw new GeneralSecurityException("KeyUsage = not set for signing");

            }
        } catch (IOException e) {
            throw new GeneralSecurityException("Basic Constraints CA = error reading extension");
        }
    }

    /*
     * ensure that a cert that is acting as an authority, either as a root or in
     * the chain, is allowed to act as a CA, by checking the BasicConstraints CA
     * extension
     */
    private void checkBasicConstraints(X509Certificate cert) throws GeneralSecurityException {
        // check basic constraints
        int bConLen = cert.getBasicConstraints();
        if (bConLen == -1) {
            throw new GeneralSecurityException("Basic Constraints CA not set for issuer in chain");
        } else {
            /*
             * basicConstraints=CA:TRUE basicConstraints=CA:FALSE
             * basicConstraints=critical,CA:TRUE, pathlen:0
             */
            // String OID_BASIC_CONSTRAINTS = "2.5.29.19";

            try {
                Object bsVal = getExtensionValue(cert, X509Extensions.BasicConstraints.getId(),
                        BasicConstraints.class);

                if (bsVal != null && bsVal instanceof BasicConstraints) {
                    BasicConstraints basicConstraints = (BasicConstraints) bsVal;
                    // BasicConstraints.getInstance(ASN1Object.fromByteArray(bsValBytes));

                    debug("Basic Constraints=CA:" + basicConstraints.isCA());

                    if (basicConstraints.getPathLenConstraint() != null)
                        debug("Basic Constraints: pathLen="
                                + basicConstraints.getPathLenConstraint().intValue());

                    if (!basicConstraints.isCA())
                        throw new GeneralSecurityException(
                                "Basic Constraints CA = true not set for issuer in chain");
                } else {
                    throw new GeneralSecurityException(
                            "Basic Constraints CA = true not set for issuer in chain");
                }
            } catch (IOException e) {
                throw new GeneralSecurityException("Basic Constraints CA = error reading extension");
            }

        }
    }

    /*
     * Turn DER encoded bytes from x509 cert extension into their matching
     * BouncyCastle classes
     */
    private Object getExtensionValue(X509Certificate X509Certificate, String oid, Object what)
            throws IOException {
        String decoded = null;
        byte[] extensionValue = X509Certificate.getExtensionValue(oid);

        if (extensionValue != null) {
            ASN1Primitive derObject = toASN1Primitive(extensionValue);
            if (derObject instanceof DEROctetString) {
                DEROctetString derOctetString = (DEROctetString) derObject;

                derObject = toASN1Primitive(derOctetString.getOctets());

                if (what == BasicConstraints.class) {
                    return BasicConstraints.getInstance(ASN1Primitive.fromByteArray(derOctetString
                            .getOctets()));
                } else if (what == KeyUsage.class) {
                    return KeyUsage.getInstance(ASN1Primitive.fromByteArray(derOctetString
                            .getOctets()));

                } else if (derObject instanceof ASN1String) {
                    ASN1String s = (ASN1String) derObject;
                    decoded = s.getString();
                }

            }
        }
        return decoded;
    }

    private ASN1Primitive toASN1Primitive(byte[] data) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(data);
        ASN1InputStream asnInputStream = new ASN1InputStream(inStream);
        ASN1Primitive obj = asnInputStream.readObject();
        asnInputStream.close();
        return obj;
    }

}
