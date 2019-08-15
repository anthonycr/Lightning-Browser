package net.i2p.client;

import android.net.LocalSocket;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DomainSocketTest {
    LocalSocket mockSocket;
    DomainSocket domainSocket;

    @Before
    public void setUp() throws Exception {
        mockSocket = mock(LocalSocket.class);
        domainSocket = new DomainSocket(mockSocket);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBind() throws Exception {
        domainSocket.bind(null);
    }

    @Test
    public void testClose() throws Exception {
        domainSocket.close();
        verify(mockSocket).close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConnect() throws Exception {
        domainSocket.connect(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConnectWithTimeout() throws Exception {
        domainSocket.connect(null, 0);
    }

    @Test
    public void testGetChannel() throws Exception {
        assertEquals(null, domainSocket.getChannel());
    }

    @Test
    public void testGetInetAddress() throws Exception {
        assertEquals(null, domainSocket.getInetAddress());
    }

    @Test
    public void testGetInputStream() throws Exception {
        InputStream is = mock(InputStream.class);
        when(mockSocket.getInputStream()).thenReturn(is);
        assertSame(is, domainSocket.getInputStream());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetKeepAlive() throws Exception {
        domainSocket.getKeepAlive();
    }

    @Test
    public void testGetLocalAddress() throws Exception {
        assertEquals(null, domainSocket.getLocalAddress());
    }

    @Test
    public void testGetLocalPort() throws Exception {
        assertEquals(-1, domainSocket.getLocalPort());
    }

    @Test
    public void testGetLocalSocketAddress() throws Exception {
        assertEquals(null, domainSocket.getLocalSocketAddress());
    }

    @Test
    public void testGetOOBInline() throws Exception {
        assertEquals(false, domainSocket.getOOBInline());
    }

    @Test
    public void testGetOutputStream() throws Exception {
        OutputStream os = mock(OutputStream.class);
        when(mockSocket.getOutputStream()).thenReturn(os);
        assertSame(os, domainSocket.getOutputStream());
    }

    @Test
    public void testGetPort() throws Exception {
        assertEquals(-1, domainSocket.getPort());
    }

    @Test
    public void testGetReceiveBufferSize() throws Exception {
        when(mockSocket.getReceiveBufferSize()).thenReturn(0);
        assertEquals(0, domainSocket.getReceiveBufferSize());
    }

    @Test(expected = SocketException.class)
    public void testGetReceiveBufferSizeExceptionThrown() throws Exception {
        doThrow(IOException.class).when(mockSocket).getReceiveBufferSize();
        domainSocket.getReceiveBufferSize();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetRemoteSocketAddress() throws Exception {
        domainSocket.getRemoteSocketAddress();
    }

    @Test
    public void testGetReuseAddress() throws Exception {
        assertEquals(false, domainSocket.getReuseAddress());
    }

    @Test
    public void testGetSendBufferSize() throws Exception {
        when(mockSocket.getSendBufferSize()).thenReturn(0);
        assertEquals(0, domainSocket.getSendBufferSize());
    }

    @Test(expected = SocketException.class)
    public void testGetSendBufferSizeExceptionThrown() throws Exception {
        doThrow(IOException.class).when(mockSocket).getSendBufferSize();
        domainSocket.getSendBufferSize();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSoLinger() throws Exception {
        domainSocket.getSoLinger();
    }

    @Test
    public void testGetSoTimeout() throws Exception {
        when(mockSocket.getSoTimeout()).thenReturn(0);
        assertEquals(0, domainSocket.getSoTimeout());
    }

    @Test(expected = SocketException.class)
    public void testGetSoTimeoutExceptionThrown() throws Exception {
        doThrow(IOException.class).when(mockSocket).getSoTimeout();
        domainSocket.getSoTimeout();
    }

    @Test
    public void testGetTcpNoDelay() throws Exception {
        assertEquals(false, domainSocket.getTcpNoDelay());
    }

    @Test
    public void testGetTrafficClass() throws Exception {
        assertEquals(0, domainSocket.getTrafficClass());
    }

    @Test
    public void testIsBound() throws Exception {
        when(mockSocket.isBound()).thenReturn(true);
        assertEquals(true, domainSocket.isBound());
    }

    @Test
    public void testIsClosed() throws Exception {
        when(mockSocket.isClosed()).thenReturn(true);
        assertEquals(true, domainSocket.isClosed());
    }

    @Test
    public void testIsConnected() throws Exception {
        when(mockSocket.isConnected()).thenReturn(true);
        assertEquals(true, domainSocket.isConnected());
    }

    @Test
    public void testIsInputShutdown() throws Exception {
        when(mockSocket.isInputShutdown()).thenReturn(true);
        assertEquals(true, domainSocket.isInputShutdown());
    }

    @Test
    public void testIsOutputShutdown() throws Exception {
        when(mockSocket.isOutputShutdown()).thenReturn(true);
        assertEquals(true, domainSocket.isOutputShutdown());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSendUrgentData() throws Exception {
        domainSocket.sendUrgentData(0);
    }

    @Test
    public void testSetKeepAlive() throws Exception {
        domainSocket.setKeepAlive(true);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void testSetOOBInlineFalse() throws Exception {
        domainSocket.setOOBInline(false);
        verifyZeroInteractions(mockSocket);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetOOBInlineTrue() throws Exception {
        domainSocket.setOOBInline(true);
    }

    @Test
    public void testSetPerformancePreferences() throws Exception {
        domainSocket.setPerformancePreferences(0, 0, 0);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void testSetReceiveBufferSize() throws Exception {
        domainSocket.setReceiveBufferSize(0);
        verify(mockSocket).setReceiveBufferSize(0);
    }

    @Test(expected = SocketException.class)
    public void testSetReceiveBufferSizeExceptionThrown() throws Exception {
        doThrow(IOException.class).when(mockSocket).setReceiveBufferSize(0);
        domainSocket.setReceiveBufferSize(0);
    }

    @Test
    public void testSetReuseAddress() throws Exception {
        domainSocket.setReuseAddress(true);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void testSetSendBufferSize() throws Exception {
        domainSocket.setSendBufferSize(0);
        verify(mockSocket).setSendBufferSize(0);
    }

    @Test(expected = SocketException.class)
    public void testSetSendBufferSizeExceptionThrown() throws Exception {
        doThrow(IOException.class).when(mockSocket).setSendBufferSize(0);
        domainSocket.setSendBufferSize(0);
    }

    @Test
    public void testSetSoLinger() throws Exception {
        domainSocket.setSoLinger(true, 0);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void testSetSoTimeout() throws Exception {
        domainSocket.setSoTimeout(0);
        verify(mockSocket).setSoTimeout(0);
    }

    @Test(expected = SocketException.class)
    public void testSetSoTimeoutExceptionThrown() throws Exception {
        doThrow(IOException.class).when(mockSocket).setSoTimeout(0);
        domainSocket.setSoTimeout(0);
    }

    @Test
    public void testSetTcpNoDelay() throws Exception {
        domainSocket.setTcpNoDelay(true);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void testSetTrafficClass() throws Exception {
        domainSocket.setTrafficClass(0);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void testShutdownInput() throws Exception {
        domainSocket.shutdownInput();
        verify(mockSocket).shutdownInput();
    }

    @Test
    public void testShutdownOutput() throws Exception {
        domainSocket.shutdownOutput();
        verify(mockSocket).shutdownOutput();
    }

    @Test
    public void testToString() throws Exception {
        when(mockSocket.toString()).thenReturn("foo");
        assertEquals("foo", domainSocket.toString());
    }
}