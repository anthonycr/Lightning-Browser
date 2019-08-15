package net.i2p.android.router.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Extracted from RouterService.
 *
 * @author str4d
 * @since 0.9.14
 */
public enum State implements Parcelable {
    // These states persist even if we died... Yuck, it causes issues.
    INIT, WAITING, STARTING, RUNNING, ACTIVE,
    // unplanned (router stopped itself), next: killSelf()
    STOPPING, STOPPED,
    // button, don't kill service when stopped, stay in MANUAL_STOPPED
    MANUAL_STOPPING, MANUAL_STOPPED,
    // button, DO kill service when stopped, next: killSelf()
    MANUAL_QUITTING, MANUAL_QUITTED,
    // Stopped by listener (no network), next: WAITING (spin waiting for network)
    NETWORK_STOPPING, NETWORK_STOPPED,
    /** @since 0.9.19 */
    GRACEFUL_SHUTDOWN;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(name());
    }

    public static final Creator<State> CREATOR = new Creator<State>() {
        @Override
        public State createFromParcel(final Parcel source) {
            try {
                String stateVal = source.readString();
                if (stateVal == null) {
                    // Somehow we got a null from the Parcel. Fail gracefully.
                    android.util.Log.e("I2P", "Received null from State Parcel.");
                    return null;
                }
                return State.valueOf(stateVal);
            } catch (IllegalArgumentException e) {
                // Parcel is from a newer version of State with new states.
                return null;
            }
        }

        @Override
        public State[] newArray(final int size) {
            return new State[size];
        }
    };
}
