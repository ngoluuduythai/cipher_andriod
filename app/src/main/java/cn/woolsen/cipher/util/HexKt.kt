package cn.woolsen.cipher.util

object HexKt {

    fun ByteArray.encodeToHexString(): String {
        return HexUtils.encode(this)
    }

    fun String.decodeHexToBytes(): ByteArray {
        return HexUtils.decode(this)
    }

}