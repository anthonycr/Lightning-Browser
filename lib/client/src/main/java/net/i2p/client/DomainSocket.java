package net.i2p.client;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * Bridge to LocalSocket.
 * <p/>
 * DomainSockets are always bound, and always start out connected.
 * You may not create an unbound DomainSocket.
 * Create this through the DomainSocketManager.
 *
 * @author str4d
 * @since 0.9.14
 */
class DomainSocket extends Socket {
    private final LocalSocket mLocalSocket;

    /**
     * @throws IOException
     * @throws UnsupportedOperationException always
     */
    DomainSocket(String name) throws IOException {
        mLocalSocket = new LocalSocket();
        mLocalSocket.connect(new LocalSocketAddress(name));
    }

    /**
     * @param localSocket the LocalSocket to wrap.
     */
    DomainSocket(LocalSocket localSocket) {
        mLocalSocket = localSocket;
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void bind(SocketAddress bindpoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        mLocalSocket.close();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void connect(SocketAddress endpoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void connect(SocketAddress endpoint, int timeout) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return null always, unimplemented
     */
    @Override
    public SocketChannel getChannel() {
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
     * @throws IOException
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return mLocalSocket.getInputStream();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean getKeepAlive() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return null always
     */
    @Override
    public InetAddress getLocalAddress() {
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
     * @return false always
     */
    @Override
    public boolean getOOBInline() {
        return false;
    }

    /**
     * @throws IOException
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return mLocalSocket.getOutputStream();
    }

    /**
     * @return -1 always
     */
    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        try {
            return mLocalSocket.getReceiveBufferSize();
        } catch (IOException e) {
            throw new SocketException(e.getLocalizedMessage());
        }
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public SocketAddress getRemoteSocketAddress() {
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
     * @throws SocketException
     */
    @Override
    public int getSendBufferSize() throws SocketException {
        try {
            return mLocalSocket.getSendBufferSize();
        } catch (IOException e) {
            throw new SocketException(e.getLocalizedMessage());
        }
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public int getSoLinger() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws SocketException
     */
    @Override
    public int getSoTimeout() throws SocketException {
        try {
            return mLocalSocket.getSoTimeout();
        } catch (IOException e) {
            throw new SocketException(e.getLocalizedMessage());
        }
    }

    /**
     * @return false always
     */
    @Override
    public boolean getTcpNoDelay() {
        return false;
    }

    /**
     * @return 0 always
     */
    @Override
    public int getTrafficClass() {
        return 0;
    }

    @Override
    public boolean isBound() {
        return mLocalSocket.isBound();
    }

    @Override
    public boolean isClosed() {
        return mLocalSocket.isClosed();
    }

    @Override
    public boolean isConnected() {
        return mLocalSocket.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return mLocalSocket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return mLocalSocket.isOutputShutdown();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void sendUrgentData(int data) {
        throw new UnsupportedOperationException();
    }

    /**
     * Does nothing.
     */
    @Override
    public void setKeepAlive(boolean on) {
    }

    /**
     * @throws UnsupportedOperationException if on is true
     */
    @Override
    public void setOOBInline(boolean on) {
        if (on)
            throw new UnsupportedOperationException();
    }

    /**
     * Does nothing.
     */
    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * @throws SocketException
     */
    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        try {
            mLocalSocket.setReceiveBufferSize(size);
        } catch (IOException e) {
            throw new SocketException(e.getLocalizedMessage());
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public void setReuseAddress(boolean on) {
    }

    /**
     * @throws SocketException
     */
    @Override
    public void setSendBufferSize(int size) throws SocketException {
        try {
            mLocalSocket.setSendBufferSize(size);
        } catch (IOException e) {
            throw new SocketException(e.getLocalizedMessage());
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public void setSoLinger(boolean on, int linger) {
    }

    /**
     * @throws SocketException
     */
    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        try {
            mLocalSocket.setSoTimeout(timeout);
        } catch (IOException e) {
            throw new SocketException(e.getLocalizedMessage());
        }
    }

    /**
     * Does nothing.
     */
    @Override
    public void setTcpNoDelay(boolean on) {
    }

    /**
     * Does nothing.
     */
    @Override
    public void setTrafficClass(int tc) {
    }

    @Override
    public void shutdownInput() throws IOException {
        mLocalSocket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        mLocalSocket.shutdownOutput();
    }

    @Override
    public String toString() {
        return mLocalSocket.toString();
    }
}
