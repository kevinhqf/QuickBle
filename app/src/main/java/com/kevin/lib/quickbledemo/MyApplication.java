package com.kevin.lib.quickbledemo;

import android.app.Application;

import com.kevin.lib.quickble.QuickBle;

/**
 * @author HeQinfu
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        QuickBle.instance().init(new QuickBle.Config(this)
                .isFilter(true)
                .maxConnection(7)
                .timeout(5000));
    }
}
