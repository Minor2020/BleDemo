package com.zxfh.blereader;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.ble.zxfh.sdk.blereader.LOG;
import com.ble.zxfh.sdk.blereader.WDBluetoothDevice;
import com.zxfh.util.encoders.Hex;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

public class BLEReader {

    private volatile static BLEReader sMBleReader;
    private static final String MOCK_BLUETOOTH_NAME = "TCHGAS_BTC800001";
    public static final int CARD_TYPE_AT88SC102 = 4;
    public static final int CARD_TYPE_AT24C02 = 2;
    /** 各区域长度 */
    private static final int SCAC_SIZE = 2;
    private static final int IZ_SIZE = 8;
    private static final int CPZ_SIZE = 8;
    private static final int AZ_SIZE = 64;
    /** 加密指令 */
    private static final int ENCRYPT = 0x80;
    /** 全流程写状态机制 */
    private int allWriteStatus = ALL_WRITE_END;
    private static final int ALL_WRITE_END = -1;
    private static final int READING_SCAC = 1;
    private static final int READING_IZ = 2;
    private static final int WRITING_IZ = 3;
    private static final int READING_CPZ = 4;
    private static final int WRITING_AZ1 = 5;
    private static final int WRITING_AZ2 = 6;
    private static final int UPDATE_PIN = 7;
    /** 全流程写入子区域 */
    private byte[] fz;
    private byte[] iz;
    private byte[] sc;
    private byte[] scac;
    private byte[] cpz;
    private byte[] az1;
    private byte[] az2;
    /** application 环境变量 */
    private Application mApplication;

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
                // NOTE: 如果开始全流程写，『迅速』点击其他指令，回调数据会有污染，由使用方保证。
                if (allWriteStatus != ALL_WRITE_END) {
                    processAllWriteCallback(i, o);
                } else {
                    mergeDataIfNeeded(i, o);
                }
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

            /**
             * 处理全写流程中的回调
             * @param status
             * @param data
             */
            private void processAllWriteCallback(int status, Object data) {
                int preAllWriteStatus = allWriteStatus;
                if (data instanceof byte[]) {
                    byte[] response = (byte[]) data;
                    // TODO: 若 status 不满足，应终止流程。但手头没有每个阶段的特征值, 故未做处理。eg: && status == 0
                    if (response.length > 3) {
                        int length = (response[2] & 0xFF);
                        switch (allWriteStatus) {
                            case READING_SCAC:
                                if (length >= SCAC_SIZE) {
                                    byte[] curScac = new byte[SCAC_SIZE];
                                    // 读取 scac data 区域
                                    System.arraycopy(response, 2, curScac, 0, SCAC_SIZE);
                                    // scac == FFFFF
                                    if (Arrays.equals(curScac, new byte[]{(byte) 0xFF, (byte) 0xFF})) {
                                        MC_Read_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_IZ, 0, IZ_SIZE,
                                                new byte[8]);
                                        // 下一流程，继续读取 IZ 区域
                                        allWriteStatus = READING_IZ;
                                    } else {
                                        // 终止读写，返回 2100026985
                                        callback.onCharacteristicChanged(status,
                                                new byte[]{(byte) 0x21, (byte) 0, (byte) 2, (byte) 0x69, (byte) 0x85});
                                        allWriteStatus = ALL_WRITE_END;
                                        return;
                                    }
                                }
                                break;
                            case READING_IZ:
                                if (length >= IZ_SIZE) {
                                    byte[] curIz = new byte[IZ_SIZE];
                                    // 读取 iz data 区域
                                    System.arraycopy(response, 2, curIz, 0, IZ_SIZE);
                                    if (Arrays.equals(curIz, iz)) {
                                        // iz相同，跳过写入，继续读取 CPZ 区域
                                        MC_Read_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_CPZ, 0, IZ_SIZE,
                                                new byte[8]);
                                        // 下一流程，读取 cpz
                                        allWriteStatus = READING_CPZ;
                                    } else {
                                        // 写入 IZ
                                        MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_IZ, 0, iz, 0,
                                                IZ_SIZE);
                                        allWriteStatus = WRITING_IZ;
                                    }
                                }
                                break;
                            case WRITING_IZ:
                                // 继续读取 CPZ 区域
                                MC_Read_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_CPZ, 0, CPZ_SIZE, new byte[8]);
                                // 下一流程，读取 cpz
                                allWriteStatus = READING_CPZ;
                            case READING_CPZ:
                                if (length >= CPZ_SIZE) {
                                    byte[] curCpz = new byte[CPZ_SIZE];
                                    System.arraycopy(response, 2, curCpz, 0, CPZ_SIZE);
                                    if (Arrays.equals(curCpz, cpz)) {
                                        // cpz 相同，跳过写入，直接写入 az1
                                        MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_AZ1, 0, az1, 0, AZ_SIZE);
                                        // 下一流程，写入 az2
                                        allWriteStatus = WRITING_AZ2;
                                    } else {
                                        MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_CPZ, 0, cpz, 0,
                                                CPZ_SIZE);
                                        // 下一流程，写入 az1
                                        allWriteStatus = WRITING_AZ1;
                                    }
                                }
                                break;
                            case WRITING_AZ1:
                                // 写入 az1
                                MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_AZ1, 0, az1, 0, AZ_SIZE);
                                // 下一流程，写入 az2
                                allWriteStatus = WRITING_AZ2;
                                break;
                            case WRITING_AZ2:
                                // 写入 az2
                                MC_Write_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_AZ2, 0, az2, 0, AZ_SIZE);
                                // 下一流程，修改密码
                                allWriteStatus = UPDATE_PIN;
                                break;
                            case UPDATE_PIN:
                                // 修改密码
                                MC_UpdatePIN_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SC, sc);
                                // 下一流程，全流程写结束，update pin 回调会进入普通逻辑
                                allWriteStatus = ALL_WRITE_END;
                                return;
                            default:
                                break;
                        }
                    }
                }
                // 并未进入下一流程，终止全流程写
                if (preAllWriteStatus == allWriteStatus) {
                    // 抛出回调信息
                    callback.onCharacteristicChanged(status, data);
                    allWriteStatus = ALL_WRITE_END;
                }
            }

            /**
             * 根据需要合并数据
             * @param i
             * @param o
             */
            private void mergeDataIfNeeded(int i, Object o) {
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

        });
    }


    /**
     * AT88SC102 流程读写. 调用方处理相关异常
     * @param start_address
     * @param write_data
     * @param write_len
     * @return int 返回值没有任何意义，需要根据 onCharacteristicChanged 回调具体判断
     */
    public int MC_All_Write_AT88SC102(int start_address, byte[] write_data, int write_len) throws Exception {
        // 0. 初始化
        fz = new byte[2];
        iz = new byte[8];
        sc = new byte[2];
        scac = new byte[SCAC_SIZE];
        cpz = new byte[CPZ_SIZE];
        az1 = new byte[AZ_SIZE];
        az2 = new byte[AZ_SIZE];
        // 1. 截取相关区域
        System.arraycopy(write_data, 0, fz, 0, fz.length);
        System.arraycopy(write_data, 2, iz, 0, iz.length);
        System.arraycopy(write_data, 10, sc, 0, sc.length);
        System.arraycopy(write_data, 12, scac, 0, scac.length);
        System.arraycopy(write_data, 14, cpz, 0, cpz.length);
        System.arraycopy(write_data, 22, az1, 0, az1.length);
        System.arraycopy(write_data, 92, az2, 0, az2.length);
        // 2. 读取 scac 区域数据
        allWriteStatus = READING_SCAC;
        MC_Read_AT88SC102(PosMemoryConstants.AT88SC102_ZONE_TYPE_SCAC, 0, 2, new byte[10]);
        return 0;
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
