package com.kevin.lib.quickble;

import com.clj.fastble.data.BleDevice;

/**
 * @author HeQinfu
 */

public class BleRequest {


    public enum RequestType {
        CHARACTERISTIC_NOTIFICATION, CHARACTERISTIC_READ,
        CHARACTERISTIC_STOP_NOTIFICATION, CHARACTERISTIC_WRITE
    }

    public enum FailReason {
        START_FAIL, TIMEOUT, RESULT_FAIL
    }

    RequestType type;
    String serviceUUID;
    String characteristicUUID;
    BleDevice bleDevice;
    byte[] writeData;

    BleRequest(RequestType type, BleDevice bleDevice, String serviceUUID, String characteristicUUID) {
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.type = type;
        this.bleDevice = bleDevice;
    }

    BleRequest(RequestType type, BleDevice bleDevice, String serviceUUID, String characteristicUUID, byte[] writeData) {
        this.type = type;
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.bleDevice = bleDevice;
        this.writeData = writeData;
    }
}
