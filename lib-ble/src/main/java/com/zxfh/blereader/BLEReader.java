package com.zxfh.blereader;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import com.ble.zxfh.sdk.blereader.LOG;
import com.ble.zxfh.sdk.blereader.WDBluetoothDevice;
import com.zxfh.util.encoders.Hex;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;

public class BLEReader {

    private volatile static BLEReader sMBleReader;
    public static final int CARD_TYPE_AT88SC102 = 4;
    public static final int CARD_TYPE_AT24C02 = 2;
    private Application mApplication;
    private static final String MOCK_BLUETOOTH_NAME = "TCHGAS_BTC800001";
    /** 加密指令 */
    private static final int ENCRYPT = 0x80;

    private BLEReader() {

    }

    public static BLEReader getInstance() {
        if (sMBleReader == null) {
            synchronized (BLEReader.class) {
                if (sMBleReader == null) {
                    sMBleReader = new BLEReader();
                }
            }
        }
        return sMBleReader;
    }

    public void setApplication(Application application) {
        mApplication = application;
        com.ble.zxfh.sdk.blereader.BLEReader.getInstance().setApplication(mApplication);
        com.ble.zxfh.sdk.blereader.BLEReader.getInstance().setDeviceModel(com.ble.zxfh.sdk.blereader.BLEReader.DEVICE_MODEL_W1981);
        modifyUuid();
        setCLA(ENCRYPT);
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
     * 修改 CLA (public final int)
     * @param action 加密或者明文，默认是明文
     */
    public void setCLA(int action) {
        try {
            Field field = com.ble.zxfh.sdk.blereader.BLEReader.getInstance()
                    .getClass().getDeclaredField("CLA");
            field.setAccessible(true);
            field.set(com.ble.zxfh.sdk.blereader.BLEReader.getInstance(), action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 仅用于测试，返回sdk内部 CLA, 查看是否修改成功
     * @return
     */
    public int getCLA() {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().CLA;
    }

    /**
     * 修改 UUID
     * @param name 要修改的 uuid 名称
     * @param changed 改变值
     */
    private void modify(String name, String changed) {
        try {
            Field field = com.ble.zxfh.sdk.blereader.BLEReader.getInstance().getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(com.ble.zxfh.sdk.blereader.BLEReader.getInstance(), UUID.fromString(changed));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setListener(IBLEReader_Callback callback) {
        com.ble.zxfh.sdk.blereader.BLEReader.getInstance().set_callback(new com.ble.zxfh.sdk.blereader.IBLEReader_Callback() {

            byte[] mergedData;
            int index;

            @Override
            public void onLeScan(List<WDBluetoothDevice> list) {
                // Ignore.
            }

            @Override
            public void onConnectGatt(int i, Object o) {
                callback.onConnectGatt(i, o);
            }

            @Override
            public void onServicesDiscovered(int i, Object o) {
                callback.onServicesDiscovered(i, o);
            }

            @Override
            public void onCharacteristicChanged(int i, Object o) {
                if (o instanceof byte[]) {
                    byte[] data = (byte[]) o;
                    // 第一帧
                    if (index == 0) {
                        if (data.length > 3) {
                            // 原始数据长度
                            int originDataLen = (data[2] & 0xFF);
                            if (originDataLen > 16) { // 16 + 4 {21, 00, 10, ... CI}
                                // 合并数据包
                                mergedData = new byte[originDataLen + 4];
                                System.arraycopy(data, 0, mergedData, 0, data.length);
                                index = data.length;
                                return;
                            }
                        }
                        // 不满足分包要求
                        callback.onCharacteristicChanged(i, o);
                    } else if (index < mergedData.length) {
                        // 拼接后续帧
                        System.arraycopy(data, 0, mergedData, index, Math.min(data.length,
                                mergedData.length - index));
                        index += data.length;
                        // 最后一帧
                        if (index >= mergedData.length) {
                            callback.onCharacteristicChanged(i, mergedData);
                            clean();
                        }
                    }
                } else {
                    // 数据错误也返回，由用户处理
                    callback.onCharacteristicChanged(i, o);
                }
            }

            /**
             * 清除全局属性值
             */
            private void clean() {
                mergedData = null;
                index = 0;
            }

            @Override
            public void onReadRemoteRssi(int i) {
                callback.onReadRemoteRssi(i);
            }

            @Override
            public void onOTA(int i, Object o) {
                callback.onOTA(i, o);
            }

            @Override
            public int onChangeBLEParameter() {
                return callback.onChangeBLEParameter();
            }
        });
    }

    public String getUuid() {
        StringBuilder strB = new StringBuilder();
        strB.append("UUID - service ");
        strB.append(com.ble.zxfh.sdk.blereader.BLEReader.getInstance().UUID_SERVICE_W1981);
        strB.append(" write ");
        strB.append(com.ble.zxfh.sdk.blereader.BLEReader.getInstance().UUID_WRITE_W1981);
        strB.append(" notification ");
        strB.append(com.ble.zxfh.sdk.blereader.BLEReader.getInstance().UUID_NOTIFICATION_W1981);
        return strB.toString();
    }

    public int disconnectGatt() {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().disconnectGatt();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().bluetoothAdapter;
    }

    public int connectGatt(String macAddress) {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().connectGatt(macAddress);
    }

    public boolean isBLEnabled() {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().isBLEnabled();
    }

    /**
     * 修改密码
     * @param zone for PosMemoryCardReader.AT88SC102_ZONE_TYPE_SC
     * @param pin pin length must be 2
     * @return 0 update ok
     */
    public int MC_UpdatePIN_AT88SC102(int zone, byte[] pin) {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().MC_UpdatePIN_AT88SC102(zone, revertEveryByte(pin));
    }

    /**
     * 模块复位
     * @param out_atr ATR of CPU card, or the character string of Memory card
     * @param out_atrlen out_atrlen[0] the length of card ATR
     * @return card type
     */
    public int ICC_Reset(byte[] out_atr, int[] out_atrlen) {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().ICC_Reset(out_atr, out_atrlen);
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
    public int MC_Write_AT88SC102(int zone, int start_address, byte[] write_data, int data_offset, int write_len) {
        return com.ble.zxfh.sdk.blereader.BLEReader
                .getInstance().MC_Write_AT88SC102(zone, start_address, revertEveryByte(write_data), data_offset, write_len);
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
    public int MC_Read_AT88SC102(int zone, int start_address, int read_len, byte[] out_data) {
        return com.ble.zxfh.sdk.blereader.BLEReader
                .getInstance().MC_Read_AT88SC102(zone, start_address, read_len, out_data);
    }

    /**
     * Write data to AT24C02
     * valid space [0~255] of this card
     * @param start_address
     * @param write_data
     * @param data_offset
     * @param write_len
     * @return
     */
    public int MC_Write_AT24C02(int start_address, byte[] write_data, int data_offset, int write_len) {
        return com.ble.zxfh.sdk.blereader.BLEReader
                .getInstance().MC_Write_AT24C02(start_address, write_data, data_offset, write_len);
    }

    /**
     * Read data of AT24C02
     * valid space [0~255] of this card
     * @param start_address
     * @param read_len
     * @param out_data
     * @return return >0 length of data read
     */
    public int MC_Read_AT24C02(int start_address, int read_len, byte[] out_data) {
        return com.ble.zxfh.sdk.blereader.BLEReader
                .getInstance().MC_Read_AT24C02(start_address, read_len, out_data);
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
    public int MC_VerifyPIN_AT88SC102(int zone, byte[] pin, int[] out_pin_retry_left) {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().MC_VerifyPIN_AT88SC102(zone, revertEveryByte(pin),
                out_pin_retry_left);
    }

    /**
     * get the Card type of CPU / Memory Card
     * @param bFromCache true, get it from cache if visible or get it from reader. false, get it from reader.
     * @return card type
     */
    public int ICC_GetCardType(boolean bFromCache) {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().ICC_GetCardType(bFromCache);
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
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().sendData(data);
    }

    /**
     * get the PAC of AT88SC102
     * @param zone PosMemoryCardReader.AT88SC102_ZONE_TYPE_SCAC
     * @return return >=0: value of 'pin access counter'
     */
    public int MC_PAC_AT88SC102(int zone) {
        return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().MC_PAC_AT88SC102(zone);
    }

    /**
     * SM4 测试
     * @return
     */
    public String testSm4() {
        StringBuilder stringBuilder = new StringBuilder();
        // 1. 转为十六进制字符串; 如果已经是 hexStr 则无需这一步转化
        String hexContent = Sm4Util.toHex("sm4对称加密<pkCs5>演示←←");
        // 2. 加密
        byte[] encryptContent = encryptMsg(hexContent);
        // 3. 解密
        byte[] decryptContent = decryptMsg(encryptContent);
        try {
            // 4.解密得到的bytes转为string
            return new String(decryptContent, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 加密信息
     * @param hexStr 十六进制表示的字符串
     * @return 加密后的 byte[]
     */
    public byte[] encryptMsg(String hexStr) {
         if (hexStr == null || hexStr.length() % 2 != 0) {
             return null;
         }
         String bluetoothName = getConnectedBluetoothName();
         if (bluetoothName == null) {
             return null;
         }
         byte[] content = Hex.decode(hexStr);
         return Sm4Util.encryptData(content, bluetoothName);
    }

    public byte[] decryptMsg(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }
        String bluetoothName = getConnectedBluetoothName();
        if (bluetoothName == null) {
            return null;
        }
        return Sm4Util.decryptData(data, bluetoothName);
    }

    private String getConnectedBluetoothName() {
        if (com.ble.zxfh.sdk.blereader.BLEReader.getInstance().isServiceConnected()) {
            return com.ble.zxfh.sdk.blereader.BLEReader.getInstance().getCurDeviceName();
        }
        return null;
    }

    /**
     * 翻转 byte array 内每个 byte 的高低位
     * @param data byte[]
     * @return
     */
    private byte[] revertEveryByte(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            int rev = 0;
            byte item = data[i];
            for (int j = 0; j < 8; ++j) {
                rev = (rev << 1) + (item & 1);
                item >>= 1;
            }
            result[i] = (byte)rev;
        }
        return result;
    }
}
