package acr.browser.lightning.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import acr.browser.lightning.utils.Utils;

public abstract class NetworkReceiver extends BroadcastReceiver {

    public abstract void onConnectivityChange(boolean isConnected);

    @Override
    public void onReceive(Context context, Intent intent) {
        onConnectivityChange(Utils.isNetworkConnected(context));
    }

}