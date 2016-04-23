/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.util.Patterns.GOOD_IRI_CHAR;

/**
 * Web Address Parser
 * <p/>
 * This is called WebAddress, rather than URL or URI, because it attempts to
 * parse the stuff that a user will actually type into a browser address widget.
 * <p/>
 * Unlike java.net.uri, this parser will not choke on URIs missing schemes. It
 * will only throw a ParseException if the input is really hosed.
 * <p/>
 * If given an https scheme but no port, fills in port
 */
class WebAddress {

    private String mScheme;
    private String mHost;
    private int mPort;
    private String mPath;
    private String mAuthInfo;
    private static final int MATCH_GROUP_SCHEME = 1;
    private static final int MATCH_GROUP_AUTHORITY = 2;
    private static final int MATCH_GROUP_HOST = 3;
    private static final int MATCH_GROUP_PORT = 4;
    private static final int MATCH_GROUP_PATH = 5;
    private static final Pattern sAddressPattern = Pattern.compile(
    /* scheme */"(?:(http|https|file)://)?" +
    /* authority */"(?:([-A-Za-z0-9$_.+!*'(),;?&=]+(?::[-A-Za-z0-9$_.+!*'(),;?&=]+)?)@)?" +
    /* host */"([" + GOOD_IRI_CHAR + "%_-][" + GOOD_IRI_CHAR + "%_\\.-]*|\\[[0-9a-fA-F:\\.]+\\])?" +
    /* port */"(?::([0-9]*))?" +
    /* path */"(/?[^#]*)?" +
    /* anchor */".*", Pattern.CASE_INSENSITIVE);

    /**
     * Parses given URI-like string.
     */
    public WebAddress(@Nullable String address) throws IllegalArgumentException {

        if (address == null) {
            throw new IllegalArgumentException("address can't be null");
        }

        mScheme = "";
        mHost = "";
        mPort = -1;
        mPath = "/";
        mAuthInfo = "";

        Matcher m = sAddressPattern.matcher(address);
        String t;
        if (!m.matches()) {
            throw new IllegalArgumentException("Parsing of address '" + address + "' failed");
        }

        t = m.group(MATCH_GROUP_SCHEME);
        if (t != null) {
            mScheme = t.toLowerCase(Locale.ROOT);
        }
        t = m.group(MATCH_GROUP_AUTHORITY);
        if (t != null) {
            mAuthInfo = t;
        }
        t = m.group(MATCH_GROUP_HOST);
        if (t != null) {
            mHost = t;
        }
        t = m.group(MATCH_GROUP_PORT);
        if (t != null && !t.isEmpty()) {
            // The ':' character is not returned by the regex.
            try {
                mPort = Integer.parseInt(t);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Parsing of port number failed", ex);
            }
        }
        t = m.group(MATCH_GROUP_PATH);
        if (t != null && !t.isEmpty()) {
            /*
             * handle busted myspace frontpage redirect with missing initial "/"
             */
            if (t.charAt(0) == '/') {
                mPath = t;
            } else {
                mPath = '/' + t;
            }
        }

        /*
         * Get port from scheme or scheme from port, if necessary and possible
         */
        if (mPort == 443 && mScheme != null && mScheme.isEmpty()) {
            mScheme = "https";
        } else if (mPort == -1) {
            if ("https".equals(mScheme)) {
                mPort = 443;
            } else {
                mPort = 80; // default
            }
        }
        if (mScheme != null && mScheme.isEmpty()) {
            mScheme = "http";
        }
    }

    @NonNull
    @Override
    public String toString() {

        String port = "";
        if ((mPort != 443 && "https".equals(mScheme)) || (mPort != 80 && "http".equals(mScheme))) {
            port = ':' + Integer.toString(mPort);
        }
        String authInfo = "";
        if (!mAuthInfo.isEmpty()) {
            authInfo = mAuthInfo + '@';
        }

        return mScheme + "://" + authInfo + mHost + port + mPath;
    }

    public void setScheme(String scheme) {
        mScheme = scheme;
    }

    public String getScheme() {
        return mScheme;
    }

    public void setHost(@NonNull String host) {
        mHost = host;
    }

    public String getHost() {
        return mHost;
    }

    public void setPort(int port) {
        mPort = port;
    }

    public int getPort() {
        return mPort;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public void setAuthInfo(String authInfo) {
        mAuthInfo = authInfo;
    }

    public String getAuthInfo() {
        return mAuthInfo;
    }
}
