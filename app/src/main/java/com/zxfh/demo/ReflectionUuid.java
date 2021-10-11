package com.zxfh.demo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import com.ble.zxfh.sdk.blereader.BLEReader;

import android.bluetooth.BluetoothAdapter;
import android.os.ParcelUuid;

/**
 * 反射修改 UUID
 */
class ReflectionUuid {

    /**
     * 修改三个UUID的启动函数
     */
    public static void trigger() {
        modify("UUID_SERVICE_W1981", "0000FFF0-0000-1000-8000-00805F9B34FB");
        modify("UUID_WRITE_W1981", "0000FFF5-0000-1000-8000-00805F9B34FB");
        modify("UUID_NOTIFICATION_W1981", "0000FFF4-0000-1000-8000-00805F9B34FB");
    }

    /**
     * 修改 UUID
     * @param name 要修改的 uuid 名称
     * @param changed 改变值
     */
    public static void modify(String name, String changed) {
        try {
            Field field = BLEReader.getInstance().getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(BLEReader.getInstance(), UUID.fromString(changed));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取蓝牙设备的 UUID
     */
    public static String getDeviceUuid() {
        try {
            BluetoothAdapter adapter = BLEReader.getInstance().bluetoothAdapter;
            Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
            ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(adapter, null);
            StringBuilder stringBuilder = new StringBuilder();
            for (ParcelUuid uuid : uuids) {
                stringBuilder.append(uuid.getUuid().toString());
                stringBuilder.append("\n");
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Cannot get uuid by reflection.";
    }
}
