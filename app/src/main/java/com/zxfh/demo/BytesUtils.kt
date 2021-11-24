package com.zxfh.demo

/**
 * 该类是 byte array 工具类
 */
class BytesUtils {

    companion object{
        /**
         * 获取十六进制字符串
         * @param bytes ByteArray
         */
        fun getHexStr(bytes: ByteArray?): String {
            var hexStr = StringBuilder()
            bytes?.forEach {
                hexStr.append(String.format("%02X ", it))
            }
            return hexStr.toString()
        }

        /**
         * 翻转 DATA 区域
         */
        private fun revertDataArray(bytes: ByteArray?) {
            bytes?.let { it ->
                // LEN 标志位，表数据长度，包括 DATA, SW1, SW2
                val dataLen = it[2].toInt() - 2
                revertByteArray(it, 2 + 1, 2 + dataLen)
            }
        }

        /**
         * 翻转 byteArray
         * @param bytes 要翻转的byte数组
         * @param start 第一个翻转元素索引
         * @param end 最后一个翻转元素索引
         */
        private fun revertByteArray(bytes: ByteArray, start: Int, end: Int) {
            try {
                for (index in 0 until (end - start + 1) / 2) {
                    val temp = bytes[start + index]
                    bytes[start + index] = bytes[end - index]
                    bytes[end - index] = temp
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}