package net.i2p.client;

import android.net.LocalServerSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

/**
 * Bridge to LocalServerSocket.
 * <p/>
 * accept() returns a real Socket (a DomainSocket).
 * <p/>
 * DomainServerSockets are always bound.
 * You may not create an unbound DomainServerSocket.
 * Create this through the DomainSocketFactory.
 *
 * @author str4d
 * @since 0.9.14
 */
class DomainServerSocket extends ServerSocket {
    private final LocalServerSocket mLocalServerSocket;
    private final DomainSocketFactory mDomainSocketFactory;
    private volatile boolean mClosed;

    /**
     * @throws IOException
     */
    public DomainServerSocket(String name, DomainSocketFactory domainSocketFactory) throws IOException {
        this(new LocalServerSocket(name), domainSocketFactory);
    }

    /**
     * Used for testing.
     *
     * @throws IOException
     */
    DomainServerSocket(LocalServerSocket localServerSocket, DomainSocketFactory domainSocketFactory) throws IOException {
        mLocalServerSocket = localServerSocket;
        mDomainSocketFactory = domainSocketFactory;
    }

    /**
     * @throws IOException
     */
    @Override
    public Socket accept() throws IOException {
        return mDomainSocketFactory.createSocket(mLocalServerSocket.accept());
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void bind(SocketAddress endpoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void bind(SocketAddress endpoint, int backlog) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        mLocalServerSocket.close();
        mClosed = true;
    }

    /**
     * @return null always
     */
    @Override
    public ServerSocketChannel getChannel() {
        return null;
    }

    /**
     * @return null always
     */
    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    /**
     * @return -1 always
     */
    @Override
    public int getLocalPort() {
        return -1;
    }

    /**
     * @return null always
     */
    @Override
    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public int getReceiveBufferSize() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return false always
     */
    @Override
    public boolean getReuseAddress() {
        return false;
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public int getSoTimeout() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return true always
     */
    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public boolean isClosed() {
        return mClosed;
    }

    /**
     * Does nothing.
     */
    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void setReceiveBufferSize(int size) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void setReuseAddress(boolean on) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void setSoTimeout(int timeout) throws SocketException {
    }

    @Override
    public String toString() {
        return mLocalServerSocket.toString();
    }
}
