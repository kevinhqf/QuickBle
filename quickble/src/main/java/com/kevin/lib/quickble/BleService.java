package com.kevin.lib.quickble;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.clj.fastble.utils.HexUtil;

import java.util.LinkedList;
import java.util.Queue;

/*
 * @author HeQinfu
 */

/**
 * 使用Service和队列的方法将蓝牙的异步请求转换成同步请求
 * 具体流程为：
 * 1. UI 通过调用{@link BleRequestHandler}的 requestXXX({@link BleRequest}) 等方法对设备发起蓝牙请求
 * 2. {@link BleRequestHandler} 将请求 ({@link BleRequest}) 添加到 {@link BleService} 的队列中等待执行，
 * 3. {@link BleService}通过{@link BleService#processNextRequest()}对请求进行顺序执行，调用相应的蓝牙请求
 * 4. 等待蓝牙请求的回调，回调里通过{@link BleService}的 bleXXX()方法把回调的数据对外进行广播
 * 5. UI 接收广播数据并做相应的操作
 */
public class BleService extends Service {
    private static final String TAG = "BleService";
    // ble请求完成并回调后 会对外发送的广播action
    /**
     * Ble请求失败广播的action
     *
     * @see BleService#bleRequestFailed(BleRequest.RequestType, BleRequest.FailReason)
     */
    public static final String BLE_REQUEST_FAILED = "action.ble.request_failed";
    /**
     * Ble读特性请求的广播action
     *
     * @see BleService#bleCharacteristicRead(String, String, byte[], int)
     */
    public static final String BLE_CHARACTERISTIC_READ = "action.ble.characteristic_read";
    /**
     * Ble写特性请求的广播action
     *
     * @see BleService#bleCharacteristicWrite(String, String, byte[], int)
     */
    public static final String BLE_CHARACTERISTIC_WRITE = "action.ble.characteristic_write";
    /**
     * Ble特性notify的广播action
     *
     * @see BleService#bleCharacteristicNotify(String, String, int)
     */
    public static final String BLE_CHARACTERISTIC_NOTIFY = "action.ble.characteristic_notify";
    /**
     * Ble特性变化的广播action
     *
     * @see BleService#bleCharacteristicChange(String, String, byte[], int)
     */
    public static final String BLE_CHARACTERISTIC_CHANGE = "action.ble.characteristic_change";
    /**
     * Ble状态异常广播的action，
     *
     * @see BleService#bleStatusAbnormal(String)
     */
    public static final String BLE_STATUS_ABNORMAL = "action.ble.status_abnormal";

    private Ble.BleCallback mBleCallback;

    public static final String BLE_THREAD = "ble-th";

    // extra
    public static final String EXTRA_TYPE = "TYPE";
    public static final String EXTRA_REASON = "REASON";
    public static final String EXTRA_CHARACTERISTIC_UUID = "CHARACTERISTIC_UUID";
    public static final String EXTRA_SERVICE_UUID = "SERVICE_UUID";
    public static final String EXTRA_VALUE = "VALUE";
    public static final String EXTRA_STATUS = "STATUS";
//    public static final String EXTRA_DEVICE = "DEVICE";


    private final IBinder mBinder = new LocalBinder();

    /**
     * 标志是否在发送 {@link BleService#BLE_CHARACTERISTIC_CHANGE} 广播时过滤重复的数据
     * 默认为 false 不过滤，在{@link BleRequestHandler}中进行设置
     */
    private boolean mIsFilterDuplicate = false;
    private String mLastNotifyData = "";//上次characteristic数据变化的值

    /**
     * 需要处理的请求队列
     */
    private final Queue<BleRequest> mRequestQueue = new LinkedList<>();
    /**
     * 具体完成请求处理的对象
     */
    private BleRequestHandler mBleHandler;
    /**
     * 检查请求超时的轮询线程
     */
    private Thread mRequestTimeout;
    private boolean mCheckTimeout = false;
    private static final int REQUEST_TIMEOUT = 10 * 10;// total timeout = *100ms
    /**
     * 当前正在处理的请求
     */
    private BleRequest mCurrentRequest = null;
    /**
     * 超时轮询线程的任务
     */
    private Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "monitoring thread start ");
            int elapsed = 0;
            try {
                while (mCheckTimeout) {
                    Thread.sleep(100);
                    elapsed++;

                    if (elapsed > REQUEST_TIMEOUT && mCurrentRequest != null) {
                        bleRequestFailed(mCurrentRequest.type, BleRequest.FailReason.TIMEOUT);
                        bleStatusAbnormal("-processrequest type "
                                + mCurrentRequest.type + " address "
                                + mCurrentRequest.bleDevice.getMac() + " [timeout]");
                        if (mBleHandler != null) {
                            mBleHandler.disconnect(mCurrentRequest.bleDevice);
                        }
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
                        mCurrentRequest = null;
                        processNextRequest();
//                            }
//                        }, BLE_THREAD).start();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 发送状态异常广播
     *
     * @param reason 异常状态的原因
     */
    private void bleStatusAbnormal(String reason) {
        Intent intent = new Intent(BLE_STATUS_ABNORMAL);
        intent.putExtra(EXTRA_VALUE, reason);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.e(TAG, "onCreate: ");
        mBleHandler = new BleRequestHandler(this);
    }

    /**
     * 包含Ble请求后广播的action的IntentFilter
     */
    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_REQUEST_FAILED);
        intentFilter.addAction(BLE_CHARACTERISTIC_READ);
        intentFilter.addAction(BLE_CHARACTERISTIC_WRITE);
        intentFilter.addAction(BLE_CHARACTERISTIC_NOTIFY);
        intentFilter.addAction(BLE_STATUS_ABNORMAL);
        intentFilter.addAction(BLE_CHARACTERISTIC_CHANGE);
        return intentFilter;
    }

    /**
     * 添加Ble请求到请求队列中
     *
     * @param request 添加到队列的请求对象
     */
    public void addBleRequest(BleRequest request) {
        Log.i("add request", "Thread---" + Thread.currentThread().getName() + "---id---" + Thread.currentThread().getId() + "---activeCount: " + Thread.activeCount());
        synchronized (mRequestQueue) {
            mRequestQueue.add(request);
            processNextRequest();
        }
    }


    /**
     * 处理队列中的下一个请求
     */
    private void processNextRequest() {

        Log.i(TAG, "Thread: " + Thread.currentThread().getName() + "---id: " + Thread.currentThread().getId() + "---activeCount: " + Thread.activeCount());
        if (mCurrentRequest != null) {
            return;
        }
        if (mRequestQueue.isEmpty()) {
            return;
        }
        mCurrentRequest = mRequestQueue.remove();
        startTimeoutThread();
        boolean result = false;
        switch (mCurrentRequest.type) {
            case CHARACTERISTIC_READ:
                result = mBleHandler.readCharacteristic(mCurrentRequest);
                break;
            case CHARACTERISTIC_WRITE:
                result = mBleHandler.writeCharacteristic(mCurrentRequest);
                break;
            case CHARACTERISTIC_NOTIFICATION:
            case CHARACTERISTIC_STOP_NOTIFICATION:
                result = mBleHandler.characteristicNotification(mCurrentRequest);
                break;

            default:
                break;
        }

        if (!result) {
            clearTimeoutThread();
            bleRequestFailed(mCurrentRequest.type, BleRequest.FailReason.START_FAIL);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
            mCurrentRequest = null;
            processNextRequest();
//                }
//            }, BLE_THREAD).start();
        }
    }

    /**
     * 处理当前的请求任务，若超时或失败则进行下一请求的处理
     *
     * @param requestType 请求类型
     * @param success     当前请求是否成功
     */
    void requestProcessed(BleRequest.RequestType requestType, boolean success) {

        if (mCurrentRequest != null && mCurrentRequest.type == requestType) {
//            Log.e("requestProcessd", mCurrentRequest.type + "");
            clearTimeoutThread();
            if (!success) {
                bleRequestFailed(requestType, BleRequest.FailReason.RESULT_FAIL);
            }
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
            mCurrentRequest = null;
            processNextRequest();
//                }
//            }, BLE_THREAD).start();
        }
    }

    /**
     * 发送请求失败广播
     *
     * @param type   请求的类型
     * @param reason 失败原因
     */
    private void bleRequestFailed(BleRequest.RequestType type, BleRequest.FailReason reason) {
        Intent intent = new Intent(BLE_REQUEST_FAILED);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_REASON, reason.ordinal());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 清除超时轮询线程
     */
    private void clearTimeoutThread() {
        if (mRequestTimeout.isAlive()) {
            try {
                mCheckTimeout = false;
                mRequestTimeout.join();
                mRequestTimeout = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动超时的轮询线程
     */
    private void startTimeoutThread() {
        mCheckTimeout = true;
        mRequestTimeout = new Thread(mTimeoutRunnable);
        mRequestTimeout.start();
    }

    /**
     * 发送特性读取完成的广播
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param value              ble操作完成后回调返回的数据
     * @param status             ble操作完成后返回的状态结果
     *                           （{@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} 或 {@link android.bluetooth.BluetoothGatt#GATT_FAILURE}）
     */
    void bleCharacteristicRead(String serviceUUID, String characteristicUUID,
                               byte[] value, int status) {
        Intent intent = new Intent(BLE_CHARACTERISTIC_READ);
        intent.putExtra(EXTRA_SERVICE_UUID, serviceUUID);
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristicUUID);
        intent.putExtra(EXTRA_VALUE, value);
        intent.putExtra(EXTRA_STATUS, status);
        if (mBleCallback!=null)
            mBleCallback.onCharacteristicRead(serviceUUID,characteristicUUID,value,status);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//        Log.e("broadcast read", "send" + characteristic_uuid);
        requestProcessed(BleRequest.RequestType.CHARACTERISTIC_READ, true);
    }

    /**
     * 发送特性变化的广播
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param value              特性变化后的数据
     * @param status             状态值为{@link BleRequestHandler#STATE_CHARACTERISTIC_CHANGE}
     */
    void bleCharacteristicChange(String serviceUUID, String characteristicUUID,
                                 byte[] value, int status) {
        if (isNotifyDataChange(value)) {
            Intent intent = new Intent(BLE_CHARACTERISTIC_CHANGE);
            intent.putExtra(EXTRA_SERVICE_UUID, serviceUUID);
            intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristicUUID);
            intent.putExtra(EXTRA_VALUE, value);
            intent.putExtra(EXTRA_STATUS, status);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }


    /**
     * 判断两次characteristicChange回调返回的数据是否发生改变
     *
     * @param data characteristicChange回调返回的数据
     * @return 数据内容是否与上次的相同
     */
    private boolean isNotifyDataChange(byte[] data) {
        // 判断过滤重复数据的标志是否开启
        if (!mIsFilterDuplicate)
            return true;
        if (mLastNotifyData.equals(HexUtil.formatHexString(data))) {
            return false;
        } else {
            mLastNotifyData = HexUtil.formatHexString(data);
            return true;
        }
    }

    /**
     * 发送通知特性的广播
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param status             ble操作完成后返回的状态结果
     *                           （{@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} 或 {@link android.bluetooth.BluetoothGatt#GATT_FAILURE}）
     */
    void bleCharacteristicNotify(String serviceUUID, String characteristicUUID, int status) {
        Intent intent = new Intent(BLE_CHARACTERISTIC_NOTIFY);
        intent.putExtra(EXTRA_SERVICE_UUID, serviceUUID);
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristicUUID);
        intent.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        requestProcessed(BleRequest.RequestType.CHARACTERISTIC_NOTIFICATION, true);
    }

    /**
     * 发送特性写入完成的广播
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param writeData          写入特性的数据
     * @param status             ble操作完成后返回的状态结果
     *                           （{@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} 或 {@link android.bluetooth.BluetoothGatt#GATT_FAILURE}）
     */
    void bleCharacteristicWrite(String serviceUUID, String characteristicUUID,
                                byte[] writeData, int status) {
        Intent intent = new Intent(BLE_CHARACTERISTIC_WRITE);
        intent.putExtra(EXTRA_SERVICE_UUID, serviceUUID);
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristicUUID);
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_VALUE, writeData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        requestProcessed(BleRequest.RequestType.CHARACTERISTIC_WRITE, true);
    }

    public void setFilterDuplicate(boolean filterDuplicate) {
        mIsFilterDuplicate = filterDuplicate;
    }

    public BleRequestHandler getBle() {
        return mBleHandler;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        Log.e(TAG, "onBind: ");
        return mBinder;
    }


    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }


}
