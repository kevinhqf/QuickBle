package com.kevin.lib.quickbledemo;

import android.app.Application;
import android.util.Log;

import com.kevin.lib.quickble.Ble;

/**
 * @author HeQinfu
 */

public class MyApplication extends Application {
private static final String TAG = "MyApplication";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: ");
        Ble.bindService(this);
    }
}
