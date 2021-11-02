package com.zxfh.blereader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 国密SM4分组密码算法工具类（对称加密）
 * <p>GB/T 32907-2016 信息安全技术 SM4分组密码算法</p>
 */
public class Sm4Util {

    private static final String ALGORITHM_NAME = "SM4";
    private static final String ALGORITHM_ECB_NOPADDING = "SM4/ECB/NoPadding";

    /**
     * SM4算法目前只支持128位（即密钥16字节）
     */
    private static final int DEFAULT_KEY_SIZE = 128;

    static {
        // 防止内存中出现多次BouncyCastleProvider的实例
        if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm4Util() {
    }

    /**
     * 生成密钥
     * <p>建议使用org.bouncycastle.util.encoders.Hex将二进制转成HEX字符串</p>
     *
     * @return 密钥16位
     * @throws Exception 生成密钥异常
     */
    private static byte[] generateKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
        kg.init(DEFAULT_KEY_SIZE, new SecureRandom());
        return kg.generateKey().getEncoded();
    }

    /**
     * 加密，SM4-ECB-NoPadding
     *
     * @param data 要加密的明文
     * @param key  密钥16字节，使用Sm4Util.generateKey()生成
     * @return 加密后的密文
     * @throws Exception 加密异常
     */
    private static byte[] encryptEcbNoPadding(byte[] data, byte[] key) throws Exception {
        return sm4(data, key, ALGORITHM_ECB_NOPADDING, null, Cipher.ENCRYPT_MODE);
    }

    /**
     * 解密，SM4-ECB-NoPadding
     *
     * @param data 要解密的密文
     * @param key  密钥16字节，使用Sm4Util.generateKey()生成
     * @return 解密后的明文
     * @throws Exception 解密异常
     */
    private static byte[] decryptEcbNoPadding(byte[] data, byte[] key) throws Exception {
        return sm4(data, key, ALGORITHM_ECB_NOPADDING, null, Cipher.DECRYPT_MODE);
    }

    /**
     * SM4对称加解密
     *
     * @param input   明文（加密模式）或密文（解密模式）
     * @param key     密钥
     * @param sm4mode sm4加密模式
     * @param iv      初始向量(ECB模式下传NULL)
     * @param mode    Cipher.ENCRYPT_MODE - 加密；Cipher.DECRYPT_MODE - 解密
     * @return 密文（加密模式）或明文（解密模式）
     * @throws Exception 加解密异常
     */
    private static byte[] sm4(byte[] input, byte[] key, String sm4mode, byte[] iv, int mode)
            throws Exception {
        IvParameterSpec ivParameterSpec = null;
        if (null != iv) {
            ivParameterSpec = new IvParameterSpec(iv);
        }
        SecretKeySpec sm4Key = new SecretKeySpec(key, ALGORITHM_NAME);
        Cipher cipher = Cipher.getInstance(sm4mode, new BouncyCastleProvider());// BouncyCastleProvider.PROVIDER_NAME);
        if (null == ivParameterSpec) {
            cipher.init(mode, sm4Key);
        } else {
            cipher.init(mode, sm4Key, ivParameterSpec);
        }
        return cipher.doFinal(input);
    }

    /**
     * 加密，内部会做补位对齐
     * @param data 要加密的明文
     * @param hashFactor 散列因子（蓝牙名称）
     * @return 加密后的 byte[]
     */
    public static byte[] encryptData(byte[] data, String hashFactor) {
        byte[] content = formatContent(data);
        byte[] result = null;
        try {
            byte[] key = generateSecondaryKey(hashFactor);
            result = Sm4Util.encryptEcbNoPadding(content, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 解密
     * @param data 秘文
     * @param hashFactor 散列因子 （蓝牙名称）
     * @return 去除补位后的解密 byte[]
     */
    public static byte[] decryptData(byte[] data, String hashFactor) {
        byte[] result = null;
        try {
            byte[] key = generateSecondaryKey(hashFactor);
            result = decryptEcbNoPadding(data, key);
            // 解析得到原二进制字符串
            result = parseResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    /**
     * 正文格式化 16字节整数倍，补位的值为余数值
     * @param content 正文
     * @return 16字节整数倍
     */
    private static byte[] formatContent(@NonNull byte[] content) {
        int contentLength = content.length;
        int paddingLength = MODE - contentLength % MODE;
        byte[] result = new byte[contentLength + paddingLength];
        System.arraycopy(content, 0, result, 0, contentLength);
        for (int i = 0; i < paddingLength; i++) {
            result[contentLength + i] = (byte)paddingLength;
        }
        return result;
    }

    /**
     * 解析返回值，去除补位数据
     * @param content 解密后的返回值
     * @return 原解密内容 byte[]
     */
    private static byte[] parseResponse(@NonNull byte[] content) {
        int paddingLength = content[content.length - 1];
        byte[] result = new byte[content.length - paddingLength];
        System.arraycopy(content, 0, result, 0, result.length);
        return result;
    }

    /**
     * 获取散列后的key
     * @param bluetoothName 蓝牙名称
     * @return byte[]
     */
    @Nullable
    public static byte[] generateSecondaryKey(@NonNull String bluetoothName) {
        String bluetoothHexName = toHex(bluetoothName);
        byte[] hashFactor = Hex.decode(bluetoothHexName);
        // 散列因子是从 bluetooth name 得到的。长度可能会 == 16，< 16 or > 16;
        // 暂定规则为，== 16 不做处理；< 16 按照正文补位规则来；> 16 截断到 16 位
        byte[] formattedFactor = new byte[16];
        int remainder = 16 - hashFactor.length;
        if (remainder > 0) {
            System.arraycopy(hashFactor, 0, formattedFactor, 0, hashFactor.length);
            for (int i = 0; i < remainder; i++) {
                formattedFactor[hashFactor.length + i] = (byte) remainder;
            }
        } else {
            System.arraycopy(hashFactor, 0, formattedFactor, 0, 16);
        }
        byte[] primeKey = Hex.decode(PRIME_KEY);
        try {
            return Sm4Util.encryptEcbNoPadding(formattedFactor, primeKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 字符串转十六进制字符串
     * @param arg utf-8 编码的字符串
     * @return
     */
    public static String toHex(@NonNull String arg) {
        return String.format("%x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }

    /**
     * 翻转 byte array 内每个 byte 的高低位
     * @param data byte[]
     * @return
     */
    public static byte[] revertEveryByte(@NonNull byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            // 以 EA 举例 1110 1010, 高位 1010 0000 160 A0
            byte upperByte = (byte) ((data[i] << 4) & 0xF0);
            // 0000 1110 14 0E
            byte lowerByte = (byte) ((data[i] >> 4) & 0x0F);
            result[i] = (byte) (upperByte | lowerByte);
        }
        return result;
    }

    /** 主密钥 */
    private static final String PRIME_KEY = "5348414E58495A484958494E46454948";
    /** 十六取模 */
    private static final int MODE = 16;
}