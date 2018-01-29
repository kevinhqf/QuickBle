package com.kevin.lib.quickble;

import android.util.Log;

import com.clj.fastble.utils.HexUtil;
import com.kevin.lib.quickble.callback.BleCallback;

import java.util.ArrayList;
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
public class BleService {
    private static final String TAG = "BleService";
    /**
     * 对Ble操作请求，注册了回调的列表
     */
    private static final ArrayList<BleCallback> mBleCallbacks = new ArrayList<>();

    /**
     * 标志是否在characteristicChange的时候对相同的数据进行返回
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
    private Thread mRequestTimeoutThread;
    private boolean mCheckTimeout = false;
    private int mRequestTimeout = 10 * 10;// total timeout = *100ms
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

                    if (elapsed > mRequestTimeout && mCurrentRequest != null) {
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
     * 对异常状态进行回调
     *
     * @param reason 异常状态的原因
     */
    private void bleStatusAbnormal(String reason) {
        for (BleCallback callback : mBleCallbacks)
            callback.onStateAbnormal(reason);
    }


    BleService() {
        mBleHandler = new BleRequestHandler(this);
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
     * 对请求失败的情况进行通知
     *
     * @param type   请求的类型
     * @param reason 失败原因
     */
    private void bleRequestFailed(BleRequest.RequestType type, BleRequest.FailReason reason) {
        for (BleCallback callback : mBleCallbacks)
            callback.onRequestFailed(type, reason);
    }

    /**
     * 清除超时轮询线程
     */
    private void clearTimeoutThread() {
        if (mRequestTimeoutThread.isAlive()) {
            try {
                mCheckTimeout = false;
                mRequestTimeoutThread.join();
                mRequestTimeoutThread = null;
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
        mRequestTimeoutThread = new Thread(mTimeoutRunnable);
        mRequestTimeoutThread.start();
    }

    /**
     * 对特性读取完成的状态进行通知
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param value              ble操作完成后回调返回的数据
     * @param status             ble操作完成后返回的状态结果
     *                           （{@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} 或 {@link android.bluetooth.BluetoothGatt#GATT_FAILURE}）
     */
    void bleCharacteristicRead(String serviceUUID, String characteristicUUID,
                               byte[] value, int status) {
        for (BleCallback callback : mBleCallbacks)
            callback.onCharacteristicRead(serviceUUID, characteristicUUID, value, status);
        requestProcessed(BleRequest.RequestType.CHARACTERISTIC_READ, true);
    }

    /**
     * 对特性变化的状态进行通知
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param value              特性变化后的数据
     * @param status             状态值为{@link BleRequestHandler#STATE_CHARACTERISTIC_CHANGE}
     */
    void bleCharacteristicChange(String serviceUUID, String characteristicUUID,
                                 byte[] value, int status) {
        if (isNotifyDataChange(value)) {
            for (BleCallback callback : mBleCallbacks)
                callback.onCharacteristicChange(serviceUUID, characteristicUUID, value, status);
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
     * 对通知特性的情况进行通知
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param status             ble操作完成后返回的状态结果
     *                           （{@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} 或 {@link android.bluetooth.BluetoothGatt#GATT_FAILURE}）
     */
    void bleCharacteristicNotify(String serviceUUID, String characteristicUUID, int status) {
        for (BleCallback callback : mBleCallbacks)
            callback.onCharacteristicNotify(serviceUUID, characteristicUUID, status);
        requestProcessed(BleRequest.RequestType.CHARACTERISTIC_NOTIFICATION, true);
    }

    /**
     * 对特性写入完成的状态进行通知
     *
     * @param serviceUUID        相应服务的UUID
     * @param characteristicUUID 相应特性的UUID
     * @param writeData          写入特性的数据
     * @param status             ble操作完成后返回的状态结果
     *                           （{@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} 或 {@link android.bluetooth.BluetoothGatt#GATT_FAILURE}）
     */
    void bleCharacteristicWrite(String serviceUUID, String characteristicUUID,
                                byte[] writeData, int status) {
        for (BleCallback callback : mBleCallbacks)
            callback.onCharacteristicWrite(serviceUUID, characteristicUUID, writeData, status);
        requestProcessed(BleRequest.RequestType.CHARACTERISTIC_WRITE, true);
    }

    public void setFilterDuplicate(boolean filterDuplicate) {
        mIsFilterDuplicate = filterDuplicate;
    }

    public void setRequestTimeout(int millis) {
        mRequestTimeout = millis / 100;
    }

    void addBleCallback(BleCallback callback) {
        mBleCallbacks.add(callback);
    }

    void removeCallback(BleCallback callback) {
        mBleCallbacks.remove(callback);
    }

    BleRequestHandler handler() {
        return mBleHandler;
    }


}
