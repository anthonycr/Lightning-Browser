package info.guardianproject.onionkit.web;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProxySelector extends java.net.ProxySelector {

    private ArrayList<Proxy> listProxies;

    public ProxySelector() {
        super();

        listProxies = new ArrayList<Proxy>();


    }

    public void addProxy(Proxy.Type type, String host, int port) {
        Proxy proxy = new Proxy(type, new InetSocketAddress(host, port));
        listProxies.add(proxy);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress address,
                              IOException failure) {
        Log.w("ProxySelector", "could not connect to " + address.toString() + ": " + failure.getMessage());
    }

    @Override
    public List<Proxy> select(URI uri) {

        return listProxies;
    }

}
