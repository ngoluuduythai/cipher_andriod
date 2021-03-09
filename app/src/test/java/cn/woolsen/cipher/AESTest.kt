package cn.woolsen.cipher

import cn.woolsen.cipher.util.HexKt.decodeHexToBytes
import cn.woolsen.cipher.util.HexUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import java.security.Security
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author woolsen
 * @date 2021/03/08 15:13
 */
class AESTest {

    @Test
    fun test() {
        val key = "3132333435363738"
        val iv = "hello000hello000"
        val text = "hello world"


        val keyBytes = key.decodeHexToBytes().copyOf(24)
        println("Key[Hex]: " + HexUtils.encode(keyBytes))
        println("Key Length: ${keyBytes.size}")
        println("Key Bytes: ${keyBytes.joinToString()}")


        Security.addProvider(BouncyCastleProvider())


        //1.构造密钥生成器，指定为AES算法,不区分大小写
//        val keygen = KeyGenerator.getInstance("AES")
//        //2.根据ecnodeRules规则初始化密钥生成器
//        //生成一个128位的随机源,根据传入的字节数组
//        keygen.init(192, SecureRandom(key.toByteArray()))
//        //3.产生原始对称密钥
//        val secretKey = keygen.generateKey()

        val secretKey = SecretKeySpec(keyBytes, "AES")


        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv.toByteArray()))

        println(Base64.getEncoder().encodeToString(cipher.doFinal(text.toByteArray())))
    }

}