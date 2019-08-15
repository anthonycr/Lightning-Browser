package net.i2p.client;

import android.net.LocalSocket;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Bridge to Android implementation of Unix domain sockets.
 *
 * @author str4d
 * @since 0.9.14
 */
public class DomainSocketFactory {
    public static String I2CP_SOCKET_ADDRESS = "net.i2p.android.client.i2cp";

    public final Log _log;

    public DomainSocketFactory(I2PAppContext context) {
        _log = context.logManager().getLog(getClass());
    }

    public Socket createSocket(String name) throws IOException {
        if (_log.shouldDebug())
            _log.debug("Connecting to domain socket " + name);
        return new DomainSocket(name);
    }

    public Socket createSocket(LocalSocket localSocket) {
        return new DomainSocket(localSocket);
    }

    public ServerSocket createServerSocket(String name) throws IOException {
        if (_log.shouldDebug())
            _log.debug("Listening on domain socket " + name);
        return new DomainServerSocket(name, this);
    }
}
