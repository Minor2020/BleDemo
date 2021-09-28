package com.zxfh.demo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

import com.ble.zxfh.sdk.blereader.BLEReader;

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
}
