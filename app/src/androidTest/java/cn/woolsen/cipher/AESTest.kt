package cn.woolsen.cipher

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.woolsen.cipher.crypto.Mode
import cn.woolsen.cipher.crypto.Padding
import cn.woolsen.cipher.crypto.symmetric.AES
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author woolsen
 */
@RunWith(AndroidJUnit4::class)
class AESTest {

    private val paddings = Padding.values()
    private val modes = Mode.values()

    @Test
    fun addition_isCorrect() {
        val text = "hello, my friend"

        val key = "hellohello"
        val iv = "12345678"


        for (mode in modes) {
            for (padding in paddings) {
                println("密钥: ${key}, 偏移量: $iv")
                println("加密算法: DES/${mode}/${padding}")
                val des = if (mode == Mode.ECB) {
                    AES(mode, padding, key.toByteArray())
                } else {
                    AES(mode, padding, key.toByteArray(), iv.toByteArray())
                }
                val encryptedBytes = des.encrypt(text)
                val encryptedBase64 = Base64.encode(encryptedBytes, Base64.DEFAULT)
                println(encryptedBase64.decodeToString())

                val decryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
                val decrypted = des.decrypt(decryptedBytes)
                println(decrypted.decodeToString())
                Assert.assertEquals(text, decrypted.decodeToString())
            }
        }

    }

}