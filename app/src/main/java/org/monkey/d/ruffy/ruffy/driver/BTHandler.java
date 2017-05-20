package org.monkey.d.ruffy.ruffy.driver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by fishermen21 on 15.05.17.
 */

public interface BTHandler {
    public void deviceConnected();

    void log(String s);

    void fail(String s);

    void deviceFound(BluetoothDevice bd);

    void handleRawData(byte[] buffer, int bytes);

    void requestBlueTooth();
}
