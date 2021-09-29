package com.zxfh.blereader;

import java.lang.reflect.Field;
import java.util.UUID;

import com.ble.zxfh.sdk.blereader.BLEReader;
import com.ble.zxfh.sdk.blereader.IBLEReader_Callback;
import com.ble.zxfh.sdk.blereader.LOG;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

public class BLEHandler {

    private volatile static BLEHandler mBleHandler;
    public static final int CARD_TYPE_AT88SC102 = 4;
    private Application mApplication;

    private BLEHandler() {

    }

    public static BLEHandler getInstance() {
        if (mBleHandler == null) {
            synchronized (BLEHandler.class) {
                if (mBleHandler == null) {
                    mBleHandler = new BLEHandler();
                }
            }
        }
        return mBleHandler;
    }

    public void setApplication(Application application) {
        mApplication = application;
        BLEReader.getInstance().setApplication(mApplication);
        BLEReader.getInstance().setDeviceModel(BLEReader.DEVICE_MODEL_W1981);
        modifyUuid();
    }

    public void setLogEnabled(boolean enabled) {
        LOG.setLogEnabled(true);
    }

    /**
     * 修改三个UUID的启动函数
     */
    private void modifyUuid() {
        modify("UUID_SERVICE_W1981", "0000FFF0-0000-1000-8000-00805F9B34FB");
        modify("UUID_WRITE_W1981", "0000FFF5-0000-1000-8000-00805F9B34FB");
        modify("UUID_NOTIFICATION_W1981", "0000FFF4-0000-1000-8000-00805F9B34FB");
    }

    /**
     * 修改 UUID
     * @param name 要修改的 uuid 名称
     * @param changed 改变值
     */
    private void modify(String name, String changed) {
        try {
            Field field = BLEReader.getInstance().getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(BLEReader.getInstance(), UUID.fromString(changed));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setListener(IBLEReader_Callback callback) {
        BLEReader.getInstance().set_callback(callback);
    }

    public String getUuid() {
        StringBuilder strB = new StringBuilder();
        strB.append("UUID - service ");
        strB.append(BLEReader.getInstance().UUID_SERVICE_W1981);
        strB.append(" write ");
        strB.append(BLEReader.getInstance().UUID_WRITE_W1981);
        strB.append(" notification ");
        strB.append(BLEReader.getInstance().UUID_NOTIFICATION_W1981);
        return strB.toString();
    }

    public int disconnectGatt() {
        return BLEReader.getInstance().disconnectGatt();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return BLEReader.getInstance().bluetoothAdapter;
    }

    public int connectGatt(String macAddress) {
        return BLEReader.getInstance().connectGatt(macAddress);
    }

    public boolean isBLEnabled() {
        return BLEReader.getInstance().isBLEnabled();
    }

    /**
     * 修改密码
     * @param zone for PosMemoryCardReader.AT88SC102_ZONE_TYPE_SC
     * @param pin pin length must be 2
     * @return 0 update ok
     */
    public int MC_UpdatePIN_AT88SC102(int zone, byte pin[]) {
        return BLEReader.getInstance().MC_UpdatePIN_AT88SC102(zone, pin);
    }

    /**
     * 模块复位
     * @param out_atr ATR of CPU card, or the character string of Memory card
     * @param out_atrlen out_atrlen[0] the length of card ATR
     * @return card type
     */
    public int ICC_Reset(byte[] out_atr, int out_atrlen[]) {
        return BLEReader.getInstance().ICC_Reset(out_atr, out_atrlen);
    }

    /**
     * 写卡数据
     * @param zone For PosMemoryCardReader.AT88SC102_ZONE_TYPE_MTZ
     * @param start_address start_address must be 0
     * @param write_data write_len must be 2
     * @param data_offset data offset
     * @param write_len length of writing
     * @return 0 successful
     */
    public int MC_Write_AT88SC102(int zone, int start_address, byte write_data[], int data_offset, int write_len) {
        return BLEReader.getInstance().MC_Write_AT88SC102(zone, start_address, write_data, data_offset, write_len);
    }

    /**
     * Verify the PIN of AT88SC102
     * <p>
     *      for PosMemoryCardReader.AT88SC102_ZONE_TYPE_SC, pin length must be 2
     *      for PosMemoryCardReader.AT88SC102_ZONE_TYPE_EZ1, pin length must be 6
     *      for PosMemoryCardReader.AT88SC102_ZONE_TYPE_EZ2, pin length must be 4
     * </p>
     * @param zone
     * @param pin
     * @param out_pin_retry_left
     * @return 0 verify OK
     */
    public int MC_VerifyPIN_AT88SC102(int zone, byte pin[], int out_pin_retry_left[]) {
        return BLEReader.getInstance().MC_VerifyPIN_AT88SC102(zone, pin, out_pin_retry_left);
    }

    /**
     * Read data of AT88SC102
     * <p>
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_FZ, start_address must be 0 and read_len must be 2,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_SC, start_address must be 0 and read_len must be 2,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_SCAC, start_address must be 0 and read_len must be 2,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_AZ1, start_address must be 0 and read_len must be 6,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_AZ2, start_address must be 0 and read_len must be 4,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_EC, start_address must be 0 and read_len must be 16,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_MTZ, start_address must be 0 and read_len must be 2,
     *      It means we have to read out all data one time
     *      For PosMemoryCardReader.AT88SC102_ZONE_TYPE_FUSE, start_address must be 0 and read_len must be 2,
     *      It means we have to read out all data one time
     * </p>
     * @param zone
     * @param start_address
     * @param read_len
     * @param out_data
     * @return
     */
    public int MC_Read_AT88SC102(int zone, int start_address, int read_len, byte out_data[]) {
        return BLEReader.getInstance().MC_Read_AT88SC102(zone, start_address, read_len, out_data);
    }

    /**
     * get the Card type of CPU / Memory Card
     * @param bFromCache true, get it from cache if visible or get it from reader. false, get it from reader.
     * @return card type
     */
    public int ICC_GetCardType(boolean bFromCache) {
        return BLEReader.getInstance().ICC_GetCardType(bFromCache);
    }

    /**
     * send raw data to reader asynchronous
     * <p>
     *      please try to get the response data of reader by callback
     *      IBLEReader_Callback.onCharacteristicChanged()
     *      It's not suggested to operate the reader by transmitting raw data unless you are familiar
     *      with the protocol of reader
     * </p>
     * @param data
     * @return
     */
    public synchronized int sendData(byte[] data) {
        return BLEReader.getInstance().sendData(data);
    }

    /**
     * get the PAC of AT88SC102
     * @param zone PosMemoryCardReader.AT88SC102_ZONE_TYPE_SCAC
     * @return return >=0: value of 'pin access counter'
     */
    public int MC_PAC_AT88SC102(int zone) {
        return BLEReader.getInstance().MC_PAC_AT88SC102(zone);
    }

}
