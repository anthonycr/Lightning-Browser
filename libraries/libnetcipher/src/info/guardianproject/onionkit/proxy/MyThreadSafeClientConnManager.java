package info.guardianproject.onionkit.proxy;

import ch.boye.httpclientandroidlib.conn.ClientConnectionOperator;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;
import ch.boye.httpclientandroidlib.params.HttpParams;

public class MyThreadSafeClientConnManager extends ThreadSafeClientConnManager {

    public MyThreadSafeClientConnManager(HttpParams params, SchemeRegistry schreg) {
        super(params, schreg);
    }

    @Override
    protected ClientConnectionOperator createConnectionOperator(
            SchemeRegistry schreg) {
        return new MyDefaultClientConnectionOperator(schreg);
    }
}
