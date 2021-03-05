package cn.woolsen.cipher.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.hutool.core.util.HexUtil
import cn.woolsen.cipher.Format
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityRsaCryptoBinding
import cn.woolsen.cipher.util.Base64Utils.base64DecodeToBytes
import cn.woolsen.cipher.util.ClipUtils
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * @author woolsen
 */
class RSACryptoActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityRsaCryptoBinding

    private var algorithm = RSA.PRIVATE

    private lateinit var keyFormatMenu: PopupMenu
    private var keyFormat = Format.HEX

    enum class RSA {
        PRIVATE, PUBLIC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsaCryptoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        when (val res = intent.getIntExtra("title", 0)) {
            R.string.title_rsa_private -> {
                title = getString(res)
                algorithm = RSA.PRIVATE
                binding.keyHint.setText(R.string.private_key)
                binding.encrypt.setText(R.string.rsa_private_encrypt)
                binding.decrypt.setText(R.string.rsa_private_decrypt)
            }
            R.string.title_rsa_public -> {
                title = getString(res)
                algorithm = RSA.PUBLIC
                binding.keyHint.setText(R.string.public_key)
                binding.encrypt.setText(R.string.rsa_public_encrypt)
                binding.decrypt.setText(R.string.rsa_public_decrypt)
            }
            else -> throw RuntimeException()
        }
        binding.keyFormat.setOnClickListener(this)
        binding.encrypt.setOnClickListener(this)
        binding.decrypt.setOnClickListener(this)
        binding.clip.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.encrypt -> encrypt()
            R.id.decrypt -> decrypt()
            R.id.key_format -> keyFormatMenu.show()
            R.id.clip -> {
                ClipUtils.clip(this, binding.afterText.text.toString())
                Toast.makeText(this, "已复制到剪切版", Toast.LENGTH_SHORT).show()
            }
            else -> return
        }
        hideKeyboard()
    }

    private fun encrypt() {
        try {
            val text = HexUtil.decodeHex(binding.text.text.toString())
            val keyPublic = getKey()
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, keyPublic)
            val encrypted = cipher.doFinal(text)
            val afterText = HexUtil.encodeHexStr(encrypted)
            binding.afterText.setText(afterText)
            Toast.makeText(this, "加密成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message ?: "加密出错", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decrypt() {
        try {
            val text = HexUtil.decodeHex(binding.text.text.toString())
            val keyPublic = getKey()
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.DECRYPT_MODE, keyPublic)
            val decrypted = cipher.doFinal(text)
            val afterText = HexUtil.encodeHexStr(decrypted)
            binding.afterText.setText(afterText)
            Toast.makeText(this, "解密成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message ?: "解密出错", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getKey(): Key {
        val key = when (keyFormat) {
            Format.ASCII -> binding.key.text.toString().toByteArray()
            Format.HEX -> HexUtil.decodeHex(binding.key.text.toString())
        }
        binding.key.text.toString().base64DecodeToBytes()
        return when (algorithm) {
            RSA.PUBLIC -> {
                val keySpec = X509EncodedKeySpec(key)
                val keyFactory = KeyFactory.getInstance("RSA")
                keyFactory.generatePublic(keySpec)
            }
            RSA.PRIVATE -> {
                val keySpec = PKCS8EncodedKeySpec(key)
                val keyFactory = KeyFactory.getInstance("RSA")
                keyFactory.generatePrivate(keySpec)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

}