package info.guardianproject.onionkit;

public class OnionKitHelper {

    /**
     * Ordered to prefer the stronger cipher suites as noted
     * http://op-co.de/blog/posts/android_ssl_downgrade/
     */
    public static final String ENABLED_CIPHERS[] = {
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_RC4_128_MD5"};

    /**
     * Ordered to prefer the stronger/newer TLS versions as noted
     * http://op-co.de/blog/posts/android_ssl_downgrade/
     */
    public static final String ENABLED_PROTOCOLS[] = {"TLSv1.2", "TLSv1.1",
            "TLSv1"};
}
