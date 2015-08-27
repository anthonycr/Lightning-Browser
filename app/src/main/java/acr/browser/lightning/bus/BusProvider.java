package acr.browser.lightning.bus;

import com.squareup.otto.Bus;

/**
 * Created by Stefano Pacifici on 25/08/15.
 */
public class BusProvider {

    private static final Bus bus = new Bus();

    public static Bus getInstance() {
        return bus;
    }

    private BusProvider() {
        // No instances
    }
}
