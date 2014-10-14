package info.guardianproject.onionkit.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.OperatedClientConnection;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.impl.conn.DefaultClientConnectionOperator;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

public class SocksProxyClientConnOperator extends DefaultClientConnectionOperator {

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;
    private static final int READ_TIMEOUT_MILLISECONDS = 60000;

    private String mProxyHost;
    private int mProxyPort;

    public SocksProxyClientConnOperator(SchemeRegistry registry, String proxyHost, int proxyPort) {
        super(registry);

        mProxyHost = proxyHost;
        mProxyPort = proxyPort;
    }

    // Derived from the original DefaultClientConnectionOperator.java in Apache HttpClient 4.2
    @Override
    public void openConnection(
            final OperatedClientConnection conn,
            final HttpHost target,
            final InetAddress local,
            final HttpContext context,
            final HttpParams params) throws IOException {
        Socket socket = null;
        Socket sslSocket = null;
        try {
            if (conn == null || target == null || params == null) {
                throw new IllegalArgumentException("Required argument may not be null");
            }
            if (conn.isOpen()) {
                throw new IllegalStateException("Connection must not be open");
            }

            Scheme scheme = schemeRegistry.getScheme(target.getSchemeName());
            SchemeSocketFactory schemeSocketFactory = scheme.getSchemeSocketFactory();

            int port = scheme.resolvePort(target.getPort());
            String host = target.getHostName();

            // Perform explicit SOCKS4a connection request. SOCKS4a supports remote host name resolution
            // (i.e., Tor resolves the hostname, which may be an onion address).
            // The Android (Apache Harmony) Socket class appears to support only SOCKS4 and throws an
            // exception on an address created using INetAddress.createUnresolved() -- so the typical
            // technique for using Java SOCKS4a/5 doesn't appear to work on Android:
            // https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/java/net/PlainSocketImpl.java
            // See also: http://www.mit.edu/~foley/TinFoil/src/tinfoil/TorLib.java, for a similar implementation

            // From http://en.wikipedia.org/wiki/SOCKS#SOCKS4a:
            //
            // field 1: SOCKS version number, 1 byte, must be 0x04 for this version
            // field 2: command code, 1 byte:
            //     0x01 = establish a TCP/IP stream connection
            //     0x02 = establish a TCP/IP port binding
            // field 3: network byte order port number, 2 bytes
            // field 4: deliberate invalid IP address, 4 bytes, first three must be 0x00 and the last one must not be 0x00
            // field 5: the user ID string, variable length, terminated with a null (0x00)
            // field 6: the domain name of the host we want to contact, variable length, terminated with a null (0x00)


            socket = new Socket();
            conn.opening(socket, target);
            socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
            socket.connect(new InetSocketAddress(mProxyHost, mProxyPort), CONNECT_TIMEOUT_MILLISECONDS);

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write((byte) 0x04);
            outputStream.write((byte) 0x01);
            outputStream.writeShort((short) port);
            outputStream.writeInt(0x01);
            outputStream.write((byte) 0x00);
            outputStream.write(host.getBytes());
            outputStream.write((byte) 0x00);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            if (inputStream.readByte() != (byte) 0x00 || inputStream.readByte() != (byte) 0x5a) {
                throw new IOException("SOCKS4a connect failed");
            }
            inputStream.readShort();
            inputStream.readInt();

            if (schemeSocketFactory instanceof SSLSocketFactory) {
                sslSocket = ((SSLSocketFactory) schemeSocketFactory).createLayeredSocket(socket, host, port, params);
                conn.opening(sslSocket, target);
                sslSocket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
                prepareSocket(sslSocket, context, params);
                conn.openCompleted(schemeSocketFactory.isSecure(sslSocket), params);
            } else {
                conn.opening(socket, target);
                socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
                prepareSocket(socket, context, params);
                conn.openCompleted(schemeSocketFactory.isSecure(socket), params);
            }
            // TODO: clarify which connection throws java.net.SocketTimeoutException?
        } catch (IOException e) {
            try {
                if (sslSocket != null) {
                    sslSocket.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioe) {
            }
            throw e;
        }
    }

    @Override
    public void updateSecureConnection(
            final OperatedClientConnection conn,
            final HttpHost target,
            final HttpContext context,
            final HttpParams params) throws IOException {
        throw new RuntimeException("operation not supported");
    }

    @Override
    protected InetAddress[] resolveHostname(final String host) throws UnknownHostException {
        throw new RuntimeException("operation not supported");
    }
}
