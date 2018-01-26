package com.kevin.lib.quickble;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.clj.fastble.BleManager;

/**
 * @author HeQinfu
 *         Ble操作的调用类, 使用方法如下：
 *         1. 首先在应用的Application的onCreate()中调用{@link Ble#bindService(Application)}进行服务的绑定
 *         2. 在需要进行Ble操作的地方使用{@link Ble#handler()}获取{@link BleRequestHandler}对象进行ble请求调用
 * @see BleRequestHandler
 */

public  class Ble {

    private static final String TAG = "Ble";
    private static BleService sService;
    private static BleRequestHandler sBleRequestHandler;


    private static final ServiceConnection sServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected:" );
            sService = ((BleService.LocalBinder) iBinder).getService();
            sBleRequestHandler = sService.getBle();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            sService = null;
        }
    };


    public static void bindService(Application app) {
        BleManager.getInstance().init(app);
        BleManager.getInstance()
                .setMaxConnectCount(7)
                .setOperateTimeout(5000);
        Intent intent = new Intent(app, BleService.class);
        app.bindService(intent, sServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public static BleRequestHandler handler() {
        if (sBleRequestHandler == null)
            throw new NullPointerException("BleService is not bound yet! Please check BleService is already registered in your app manifest or not.");
        return sBleRequestHandler;
    }


    public interface BleCallback{
        void onCharacteristicChange();
        void onCharacteristicWrite();
        void onCharacteristicRead(String serviceUUID, String characteristicUUID, byte[] value, int status);
        void onCharacteristicNotify();
        void onRequestFailed();
        void onStateAbnormal();
    }

}

