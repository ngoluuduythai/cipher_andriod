package cn.woolsen.cipher

import android.util.Base64
import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.asymmetric.RSA
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert
import org.junit.Test
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * @author woolsen
 */
class RSATest {

    private val text: String = kotlin.run {
        val sb = StringBuilder()
        val range = ('a'..'z') + ('A'..'Z') + ('1'..'0')
        repeat(500) {
            sb.append(range.random())
        }
        sb.toString()
    }

    @Test
    fun rsaTest() {
        val algorithm = "RSA/ECB/PKCS1Padding"

        val bc = BouncyCastleProvider()
        val keyGenerator = KeyPairGenerator.getInstance("RSA", bc)
        keyGenerator.initialize(1024)
        val keyPair = keyGenerator.genKeyPair()

        val base64PrivateKey = keyPair.private.encoded.base64EncodeToString()
        println("私钥：$base64PrivateKey")
        val base64PublicKey = keyPair.public.encoded.base64EncodeToString()
        println("公钥：$base64PublicKey")

        val keyFactory = KeyFactory.getInstance("RSA", bc)
        val privateKey = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(base64PrivateKey.base64DecodeToBytes())
        )
        val publicKey = keyFactory.generatePublic(
            X509EncodedKeySpec(base64PublicKey.base64DecodeToBytes())
        )

        val cipher = Cipher.getInstance(algorithm, bc)

        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        val encryptedByPrivateKey = cipher.doFinal(text.toByteArray())
        val base64EncryptedByPrivateKey = encryptedByPrivateKey.base64EncodeToString()
        println("私钥加密：${base64EncryptedByPrivateKey}")
        cipher.init(Cipher.DECRYPT_MODE, publicKey)
        val decryptedByPublicKey = cipher.doFinal(encryptedByPrivateKey)
        println("公钥解密：${decryptedByPublicKey.decodeToString()}")
        Assert.assertEquals(text, decryptedByPublicKey.decodeToString())
    }

    @Test
    fun hutoolRsaTest() {
        println("原文本：$text")
        val genRsa = RSA()
        val base64PrivateKey = genRsa.privateKey.encoded.base64EncodeToString()
        println("私钥格式：${genRsa.privateKey.format}")
        println("私钥算法：${genRsa.privateKey.algorithm}")
        println("私钥：$base64PrivateKey")
        val base64PublicKey = genRsa.publicKey.encoded.base64EncodeToString()
        println("公钥格式：${genRsa.publicKey.format}")
        println("公钥算法：${genRsa.publicKey.algorithm}")
        println("公钥：$base64PublicKey")

        val rsa = RSA(base64PrivateKey, base64PublicKey)
        val encryptedByPrivateKey = rsa.encrypt(text, KeyType.PrivateKey)
        val base64EncryptedByPrivateKey = encryptedByPrivateKey.base64EncodeToString()
        println("私钥加密：${base64EncryptedByPrivateKey}")
        val decryptedByPublicKey = rsa.decrypt(encryptedByPrivateKey, KeyType.PublicKey)
        println("公钥解密：${decryptedByPublicKey.decodeToString()}")
        Assert.assertEquals(text, decryptedByPublicKey.decodeToString())

        val encryptedByPublicKey = rsa.encrypt(text, KeyType.PublicKey)
        val base64EncryptedByPublicKey = encryptedByPublicKey.base64EncodeToString()
        println("公钥加密：${base64EncryptedByPublicKey}")
        val decryptedByPrivateKey = rsa.decrypt(encryptedByPublicKey, KeyType.PrivateKey)
        println("私钥解密：${decryptedByPrivateKey.decodeToString()}")
        Assert.assertEquals(text, decryptedByPrivateKey.decodeToString())
    }

    private fun ByteArray.base64EncodeToString(flags: Int = Base64.DEFAULT): String {
        return java.util.Base64.getEncoder().encodeToString(this)
    }

    fun String.base64DecodeToBytes(flags: Int = Base64.DEFAULT): ByteArray {
        return java.util.Base64.getDecoder().decode(this)
    }
}