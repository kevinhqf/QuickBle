package com.kevin.lib.quickbledemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.kevin.lib.quickble.BleRequest;
import com.kevin.lib.quickble.QuickBle;
import com.kevin.lib.quickble.callback.BleCallback;

public class DeviceOPActivity extends AppCompatActivity {
    private static final String TAG = "DeviceOPActivity";
    BleCallback mBleCallback = new BleCallback() {
        @Override
        public void onCharacteristicChange(String serviceUUID, String characteristicUUID, byte[] value, int status) {

        }

        @Override
        public void onCharacteristicWrite(String serviceUUID, String characteristicUUID, byte[] writeData, int status) {

        }

        @Override
        public void onCharacteristicRead(String serviceUUID, String characteristicUUID, byte[] value, int status) {
            Log.e(TAG, "onCharacteristicRead: ------" + characteristicUUID + new String(value));
        }

        @Override
        public void onCharacteristicNotify(String serviceUUID, String characteristicUUID, int status) {

        }

        @Override
        public void onRequestFailed(BleRequest.RequestType type, BleRequest.FailReason reason) {

        }

        @Override
        public void onStateAbnormal(String reason) {

        }
    };
    BleDevice bleDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_op);
        QuickBle.registerCallback(mBleCallback);
        bleDevice = getIntent().getParcelableExtra("device");
        if (BleManager.getInstance().isConnected(bleDevice))
            read();
    }

    private void read() {
        QuickBle.handler().requestCharacteristicRead(bleDevice, "0000180a-0000-1000-8000-00805f9b34fb", "00002a29-0000-1000-8000-00805f9b34fb");
        QuickBle.handler().requestCharacteristicRead(bleDevice, "0000180a-0000-1000-8000-00805f9b34fb", "00002a26-0000-1000-8000-00805f9b34fb");
        QuickBle.handler().requestCharacteristicRead(bleDevice, "0000180a-0000-1000-8000-00805f9b34fb", "00002a23-0000-1000-8000-00805f9b34fb");
        QuickBle.handler().requestCharacteristicRead(bleDevice, "0000180f-0000-1000-8000-00805f9b34fb", "00002a19-0000-1000-8000-00805f9b34fb");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        QuickBle.unregisterCallback(mBleCallback);
    }
}
