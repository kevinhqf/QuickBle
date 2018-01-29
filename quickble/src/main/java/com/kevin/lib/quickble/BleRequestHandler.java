package com.kevin.lib.quickble;

import android.bluetooth.BluetoothGatt;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.BleException;

/*
 * @author HeQinfu
 */

/**
 * 对外提供ble请求的相关方法
 */
public class BleRequestHandler {
    private BleService mService;
    public static final int STATE_CHARACTERISTIC_CHANGE = 123;

    BleRequestHandler(BleService service) {
        mService = service;
    }

    /**
     * 进行Ble扫描 （不加入请求队列）
     *
     * @param callback 对BLE扫描的回调进行处理的callback
     */
    public void scan(BleScanCallback callback) {
        if (BleScanState.STATE_SCANNING != BleManager.getInstance().getScanSate()) {
            BleManager.getInstance().scan(callback);
        }
    }

    /**
     * 进行连接操作 （不加入请求队列）
     *
     * @param callback 对蓝牙连接的回调进行处理的callback
     */
    public void connect(BleDevice bleDevice, BleGattCallback callback) {
        if (!isConnected(bleDevice)) {
            BleManager.getInstance().connect(bleDevice, callback);
        }
    }

    /**
     * 断开连接
     */
    public void requestDisconnect(BleDevice bleDevice) {
        disconnect(bleDevice);
    }

    /**
     * 进行读特性的请求
     *
     * @param bleDevice           对应的设备
     * @param service_uuid        相应服务的UUID
     * @param characteristic_uuid 相应特性的UUID
     */
    public boolean requestCharacteristicRead(BleDevice bleDevice,
                                             String service_uuid,
                                             String characteristic_uuid) {
        if (!isConnected(bleDevice)) {
            return false;
        }
        mService.addBleRequest(new BleRequest(BleRequest.RequestType.CHARACTERISTIC_READ,
                bleDevice, service_uuid, characteristic_uuid));
        return true;
    }

    /**
     * 进行写特性的请求
     *
     * @param bleDevice           对应的设备
     * @param service_uuid        相应服务的UUID
     * @param characteristic_uuid 相应特性的UUID
     * @param writeData           需要写入的数据
     */
    public boolean requestCharacteristicWrite(BleDevice bleDevice,
                                              String service_uuid,
                                              String characteristic_uuid,
                                              byte[] writeData) {
        if (!isConnected(bleDevice)) {
            return false;
        }
        mService.addBleRequest(new BleRequest(BleRequest.RequestType.CHARACTERISTIC_WRITE,
                bleDevice, service_uuid, characteristic_uuid, writeData));
        return true;
    }

    /**
     * 进行notify特性的请求
     *
     * @param bleDevice           对应的设备
     * @param service_uuid        相应服务的UUID
     * @param characteristic_uuid 相应特性的UUID
     * @param notify_type         notify的类别 {@link BleRequest.RequestType#CHARACTERISTIC_NOTIFICATION} 表示监听notify特性的变化，
     *                            {@link BleRequest.RequestType#CHARACTERISTIC_STOP_NOTIFICATION} 表示取消监听
     */
    public boolean requestCharacteristicNotification(BleDevice bleDevice,
                                                     String service_uuid,
                                                     String characteristic_uuid,
                                                     BleRequest.RequestType notify_type) {
        if (!isConnected(bleDevice)) {
            return false;
        }
        mService.addBleRequest(new BleRequest(notify_type, bleDevice,
                service_uuid, characteristic_uuid));
        return true;
    }

    /**
     * 具体的写特性操作,由BleService进行调用
     */
    boolean writeCharacteristic(BleRequest request) {
        if (!isConnected(request.bleDevice))
            return false;
        final String characteristicUUID = request.characteristicUUID;
        final String serviceUUID = request.serviceUUID;
        final byte[] writeData = request.writeData;
        BleManager.getInstance().write(request.bleDevice,
                request.serviceUUID, request.characteristicUUID,
                request.writeData, new BleRequestWriteCallback() {
                    @Override
                    void onCharacteristicWrite(int status) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            mService.requestProcessed(BleRequest.RequestType.CHARACTERISTIC_WRITE, false);
                            return;
                        }
                        mService.bleCharacteristicWrite(serviceUUID, characteristicUUID, writeData, status);
                    }
                });
        return true;
    }

    /**
     * 具体的读特性操作,由BleService进行调用
     */
    boolean readCharacteristic(BleRequest request) {
        BleDevice bleDevice = request.bleDevice;
        if (!isConnected(bleDevice))
            return false;
        final String characteristicUUID = request.characteristicUUID;
        final String serviceUUID = request.serviceUUID;
        BleManager.getInstance().read(bleDevice,
                serviceUUID, characteristicUUID,
                new BleRequestReadCallback() {
                    @Override
                    void onCharacteristicRead(byte[] data, int status) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            mService.requestProcessed(BleRequest.RequestType.CHARACTERISTIC_READ, false);
                            return;
                        }
                        mService.bleCharacteristicRead(serviceUUID, characteristicUUID, data, status);
                    }
                });
        return true;
    }


    /**
     * 具体的notify特性操作,由BleService进行调用
     */
    boolean characteristicNotification(BleRequest request) {
        if (!isConnected(request.bleDevice))
            return false;
        final String serviceUUID = request.serviceUUID;
        final String characteristicUUID = request.characteristicUUID;
        if (BleRequest.RequestType.CHARACTERISTIC_STOP_NOTIFICATION == request.type) {
            return BleManager.getInstance().stopNotify(request.bleDevice,
                    serviceUUID, characteristicUUID);
        }
        BleManager.getInstance().notify(request.bleDevice,
                serviceUUID, characteristicUUID,
                new BleRequestNotifyCallback() {
                    @Override
                    public void onNotification(byte[] data, int status) {
                        if (status == BluetoothGatt.GATT_FAILURE) {
                            mService.requestProcessed(BleRequest.RequestType.CHARACTERISTIC_NOTIFICATION, false);
                            return;
                        } else if (status == BluetoothGatt.GATT_SUCCESS) {
                            mService.bleCharacteristicNotify(serviceUUID, characteristicUUID, status);
                            return;
                        }
                        mService.bleCharacteristicChange(serviceUUID, characteristicUUID, data, status);
                    }
                });
        return true;
    }

    void disconnect(BleDevice bleDevice) {
        if (isConnected(bleDevice)) {
            BleManager.getInstance().disconnect(bleDevice);
        }
    }

    private boolean isConnected(BleDevice bleDevice) {
        return BleManager.getInstance().isConnected(bleDevice);
    }


    //TODO 返回exception
    private class BleRequestNotifyCallback extends BleNotifyCallback {


        @Override
        public void onNotifySuccess() {
            onNotification(null, BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onNotifyFailure(BleException exception) {
            onNotification(null, BluetoothGatt.GATT_FAILURE);

        }

        @Override
        public void onCharacteristicChanged(byte[] data) {
            onNotification(data, STATE_CHARACTERISTIC_CHANGE);

        }

        public void onNotification(byte[] data, int status) {

        }

    }

    private class BleRequestWriteCallback extends BleWriteCallback {

        @Override
        public void onWriteSuccess() {
            onCharacteristicWrite(BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onWriteFailure(BleException exception) {
            onCharacteristicWrite(BluetoothGatt.GATT_FAILURE);
        }

        void onCharacteristicWrite(int status) {

        }
    }

    private class BleRequestReadCallback extends BleReadCallback {

        @Override
        public void onReadSuccess(byte[] data) {
            onCharacteristicRead(data, BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onReadFailure(BleException exception) {
            onCharacteristicRead(null, BluetoothGatt.GATT_FAILURE);
        }

        void onCharacteristicRead(byte[] data, int status) {

        }
    }
}
