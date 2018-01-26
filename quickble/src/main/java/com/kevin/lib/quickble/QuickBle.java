package com.kevin.lib.quickble;

import android.app.Application;

import com.clj.fastble.BleManager;
import com.kevin.lib.quickble.callback.BleCallback;

/**
 * @author HeQinfu
 */

public class QuickBle {

    private static QuickBle singleton = SingletonHolder.instance;
    private static final String TAG = "QuickBle";
    private BleService mService = new BleService();
    private Config mConfig;

    private QuickBle() {
    }

    public void init(Config config) {
        if (this.mConfig != null)
            return;
        this.mConfig = config;
        mService.setFilterDuplicate(config.isFilterDuplicate);
        BleManager.getInstance().init(config.app);
        BleManager.getInstance()
                .setMaxConnectCount(config.maxConnectCount)
                .setOperateTimeout(config.operateTimeOut);
    }

    public static BleRequestHandler handler() {
        if (singleton.mConfig == null) {
            throw new NullPointerException("QuickBle is not init yet!");
        }
        return singleton.mService.handler();
    }


    public static void registerCallback(BleCallback callback) {
        singleton.mService.addBleCallback(callback);
    }

    public static void unregisterCallback(BleCallback callback) {
        singleton.mService.removeCallback(callback);
    }

    public static QuickBle get() {
        return singleton;
    }

    private static class SingletonHolder {
        private static final QuickBle instance = new QuickBle();
    }


    public static class Config {
        private int operateTimeOut = 5000;
        private int maxConnectCount = 7;
        private boolean isFilterDuplicate = false;
        private Application app;


        public Config maxConnection(int max) {
            this.maxConnectCount = max;
            return this;
        }

        public Config timeout(int millisecond) {
            this.operateTimeOut = millisecond;
            return this;
        }

        public Config isFilter(boolean isFilterDuplicate) {
            this.isFilterDuplicate = isFilterDuplicate;
            return this;
        }

        public Config(Application app) {
            this.app = app;
        }
    }


}

