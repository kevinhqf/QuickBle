package com.kevin.lib.quickbledemo;

import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;
import com.kevin.lib.quickble.QuickBle;
import com.kevin.lib.quickble.callback.BleReadCallback;

import java.util.List;

public class ScanActivity extends AppCompatActivity implements DevicesAdapter.OnBleDeviceClick {


    private static final String TAG = "ScanActivity";
    Button btnScan;
    ProgressBar pb;
    RecyclerView rv;
    DevicesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "onCreate: ");
        initView();

    }

    private void initView() {
        btnScan = findViewById(R.id.btnScan);
        pb = findViewById(R.id.pb);
        rv = findViewById(R.id.rvDevices);
        mAdapter = new DevicesAdapter(this);
        mAdapter.setOnBleDeviceClick(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mAdapter);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
    }

    private void scan() {
        if (BleManager.getInstance().getScanSate() != BleScanState.STATE_SCANNING) {
            BleManager.getInstance().scan(new BleScanCallback() {
                @Override
                public void onScanStarted(boolean success) {
                    pb.setVisibility(View.VISIBLE);
                    mAdapter.clear();
                }

                @Override
                public void onScanning(final BleDevice result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.addDevice(result);
                        }
                    });
                }

                @Override
                public void onScanFinished(List<BleDevice> scanResultList) {
                    pb.setVisibility(View.INVISIBLE);
                }
            });
        }
    }


    @Override
    public void onDeviceClick(BleDevice bleDevice) {
        QuickBle.handler().requestConnect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.e(TAG, "onStartConnect: ----------------------");
            }

            @Override
            public void onConnectFail(BleException exception) {
                Log.e(TAG, "onConnectFail: ------------------------");
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.e(TAG, "onConnectSuccess: ---------------------");
                Intent intent = new Intent(ScanActivity.this, DeviceOPActivity.class);
                intent.putExtra("device", bleDevice);
                startActivity(intent);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                Log.e(TAG, "onDisConnected: --------------------------");
            }
        });
    }
}
