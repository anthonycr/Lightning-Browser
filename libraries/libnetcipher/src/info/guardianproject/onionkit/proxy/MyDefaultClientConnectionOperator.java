package info.guardianproject.onionkit.proxy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.HttpHostConnectException;
import ch.boye.httpclientandroidlib.conn.OperatedClientConnection;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.scheme.SocketFactory;
import ch.boye.httpclientandroidlib.impl.conn.DefaultClientConnectionOperator;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

public class MyDefaultClientConnectionOperator extends
        DefaultClientConnectionOperator {

    public MyDefaultClientConnectionOperator(SchemeRegistry schemes) {
        super(schemes);

    }

    @Override
    public void openConnection(OperatedClientConnection conn, HttpHost target,
                               InetAddress local, HttpContext context, HttpParams params)
            throws IOException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target host must not be null.");
        }
        // local address may be null
        // @@@ is context allowed to be null?
        if (params == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }
        if (conn.isOpen()) {
            throw new IllegalArgumentException("Connection must not be open.");
        }

        final Scheme schm = schemeRegistry.getScheme(target.getSchemeName());
        final SocketFactory sf = schm.getSocketFactory();

        Socket sock = sf.createSocket();
        conn.opening(sock, target);

        try {
            Socket connsock = sf.connectSocket(sock, target.getHostName(),
                    schm.resolvePort(target.getPort()),
                    local, 0, params);

            if (sock != connsock) {
                sock = connsock;
                conn.opening(sock, target);
            }
        } catch (ConnectException ex) {
            throw new HttpHostConnectException(target, ex);
        }
        prepareSocket(sock, context, params);
        conn.openCompleted(sf.isSecure(sock), params);
    }

}
