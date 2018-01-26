package com.kevin.lib.quickble.callback;

import com.kevin.lib.quickble.BleRequest;

/**
 * @author HeQinfu
 * @copyright 康佳集团股份有限公司保留所有版权。
 * @created on 2018-1-26。
 * @description
 */

public  abstract class BleReadCallback implements BleCallback{
    @Override
    public void onCharacteristicChange(String serviceUUID, String characteristicUUID, byte[] value, int status) {

    }

    @Override
    public void onCharacteristicWrite(String serviceUUID, String characteristicUUID, byte[] writeData, int status) {

    }

    @Override
    public void onCharacteristicNotify(String serviceUUID, String characteristicUUID, int status) {

    }

    @Override
    public void onRequestFailed(BleRequest.RequestType type, BleRequest.FailReason reason) {

    }

    @Override
    public void onStateAbnormal(String reason) {

    }
}
