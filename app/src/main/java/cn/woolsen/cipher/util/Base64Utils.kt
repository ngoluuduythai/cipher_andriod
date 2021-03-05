package cn.woolsen.cipher.util

import android.util.Base64

/**
 * @author woolsen
 * @date 2021/03/05 15:18
 */
object Base64Utils {

    fun ByteArray.base64EncodeToString(flags: Int = Base64.DEFAULT): String {
        return Base64.encodeToString(this, flags)
    }

    fun String.base64DecodeToBytes(flags: Int = Base64.DEFAULT): ByteArray {
        return Base64.decode(this, flags)
    }
}