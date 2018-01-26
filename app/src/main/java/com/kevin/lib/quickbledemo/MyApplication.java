package com.kevin.lib.quickbledemo;

import android.app.Application;
import android.util.Log;

import com.kevin.lib.quickble.QuickBle;

/**
 * @author HeQinfu
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: ");
        QuickBle.get().init(new QuickBle.Config(this)
                .isFilter(true)
                .maxConnection(7)
                .timeout(5000));
    }
}
