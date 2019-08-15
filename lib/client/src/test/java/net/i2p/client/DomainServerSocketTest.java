package net.i2p.client;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DomainServerSocketTest {
    LocalServerSocket mockServerSocket;
    DomainSocketFactory mockFactory;
    DomainServerSocket domainServerSocket;

    @Before
    public void setUp() throws Exception {
        mockServerSocket = mock(LocalServerSocket.class);
        mockFactory = mock(DomainSocketFactory.class);
        domainServerSocket = new DomainServerSocket(mockServerSocket, mockFactory);
    }

    @Test
    public void testAccept() throws Exception {
        LocalSocket ls = mock(LocalSocket.class);
        DomainSocket ds = mock(DomainSocket.class);
        when(mockServerSocket.accept()).thenReturn(ls);
        when(mockFactory.createSocket(ls)).thenReturn(ds);
        assertSame(ds, domainServerSocket.accept());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBind() throws Exception {
        domainServerSocket.bind(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBindWithBacklog() throws Exception {
        domainServerSocket.bind(null, 0);
    }

    @Test
    public void testClose() throws Exception {
        domainServerSocket.close();
        verify(mockServerSocket).close();
    }

    @Test
    public void testGetChannel() throws Exception {
        assertEquals(null, domainServerSocket.getChannel());
    }

    @Test
    public void testGetInetAddress() throws Exception {
        assertEquals(null, domainServerSocket.getInetAddress());
    }

    @Test
    public void testGetLocalPort() throws Exception {
        assertEquals(-1, domainServerSocket.getLocalPort());
    }

    @Test
    public void testGetLocalSocketAddress() throws Exception {
        assertEquals(null, domainServerSocket.getLocalSocketAddress());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetReceiveBufferSize() throws Exception {
        domainServerSocket.getReceiveBufferSize();
    }

    @Test
    public void testGetReuseAddress() throws Exception {
        assertEquals(false, domainServerSocket.getReuseAddress());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSoTimeout() throws Exception {
        domainServerSocket.getSoTimeout();
    }

    @Test
    public void testIsBound() throws Exception {
        assertEquals(true, domainServerSocket.isBound());
    }

    @Test
    public void testIsClosed() throws Exception {
        assertEquals(false, domainServerSocket.isClosed());
        domainServerSocket.close();
        assertEquals(true, domainServerSocket.isClosed());
    }

    @Test
    public void testSetPerformancePreferences() throws Exception {
        domainServerSocket.setPerformancePreferences(0, 0, 0);
        verifyZeroInteractions(mockServerSocket);
    }

    @Test
    public void testSetReceiveBufferSize() throws Exception {
        domainServerSocket.setReceiveBufferSize(0);
        verifyZeroInteractions(mockServerSocket);
    }

    @Test
    public void testSetReuseAddress() throws Exception {
        domainServerSocket.setReuseAddress(true);
        verifyZeroInteractions(mockServerSocket);
    }

    @Test
    public void testSetSoTimeout() throws Exception {
        domainServerSocket.setSoTimeout(0);
        verifyZeroInteractions(mockServerSocket);
    }

    @Test
    public void testToString() throws Exception {
        when(mockServerSocket.toString()).thenReturn("foo");
        assertEquals("foo", domainServerSocket.toString());
    }
}