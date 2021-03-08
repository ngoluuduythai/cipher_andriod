package cn.woolsen.cipher.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityRsaCryptoBinding
import cn.woolsen.cipher.enums.Charset
import cn.woolsen.cipher.enums.RSAKeyFormat
import cn.woolsen.cipher.util.Base64Utils.base64DecodeToBytes
import cn.woolsen.cipher.util.Base64Utils.base64EncodeToString
import cn.woolsen.cipher.util.ClipUtils
import cn.woolsen.cipher.util.HexUtils
import cn.woolsen.cipher.util.SnackUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
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
    private var keyFormat = RSAKeyFormat.Hex

    private lateinit var charsetMenu: PopupMenu
    private lateinit var encryptedFormatMenu: PopupMenu
    private var charset = Charset.UTF_8
    private var encryptedFormat = Charset.BASE64

    enum class RSA {
        PRIVATE, PUBLIC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsaCryptoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setKeyFormat(RSAKeyFormat.PEM)

        charset = Charset.UTF_8
        binding.charset.setText(R.string.utf8)
        encryptedFormat = Charset.BASE64
        binding.encryptedFormat.setText(R.string.base64)

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

        keyFormatMenu = PopupMenu(this, binding.keyFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_rsa_key)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.hex -> setKeyFormat(RSAKeyFormat.Hex)
                    R.id.pem -> setKeyFormat(RSAKeyFormat.PEM)
                }
                true
            }
        }
        charsetMenu = PopupMenu(this, binding.charset, Gravity.BOTTOM).apply {
            inflate(R.menu.charset)
            setOnMenuItemClickListener {
                charset = when (it.itemId) {
                    R.id.hex -> Charset.HEX
                    R.id.gb2312 -> Charset.GB2312
                    R.id.utf8 -> Charset.UTF_8
                    R.id.utf16 -> Charset.UTF_16
                    R.id.utf16be -> Charset.UTF_16BE
                    R.id.utf16le -> Charset.UTF_16LE
                    else -> {
                        showSnackbar("不支持此编码格式", Snackbar.LENGTH_SHORT)
                        return@setOnMenuItemClickListener true
                    }
                }
                binding.charset.text = it.title
                true
            }
        }
        encryptedFormatMenu = PopupMenu(this, binding.encryptedFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_encrypted)
            setOnMenuItemClickListener {
                encryptedFormat = when (it.itemId) {
                    R.id.hex -> Charset.HEX
                    R.id.base64 -> Charset.BASE64
                    else -> {
                        showSnackbar("不支持此编码格式", Snackbar.LENGTH_SHORT)
                        return@setOnMenuItemClickListener true
                    }
                }
                binding.encryptedFormat.text = it.title
                true
            }
        }


        binding.keyFormat.setOnClickListener(this)
        binding.encrypt.setOnClickListener(this)
        binding.decrypt.setOnClickListener(this)
        binding.clip.setOnClickListener(this)
        binding.charset.setOnClickListener(this)
        binding.encryptedFormat.setOnClickListener(this)


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.encrypt -> encrypt()
            R.id.decrypt -> decrypt()
            R.id.key_format -> keyFormatMenu.show()
            R.id.charset -> charsetMenu.show()
            R.id.encrypted_format -> encryptedFormatMenu.show()
            R.id.clip -> {
                ClipUtils.clip(this, binding.afterText.text.toString())
                showSnackbar("已复制到剪切版", Snackbar.LENGTH_SHORT)
            }
            else -> return
        }
        hideKeyboard()
    }

    private fun encrypt() {
        try {
            val text = if (charset == Charset.HEX) {
                HexUtils.decode(binding.text.text.toString())
            } else {
                binding.text.text.toString().toByteArray(charset(charset))
            }
            val keyPublic = getKey()
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keyPublic)
            val encrypted = cipher.doFinal(text)
            val afterText = when (encryptedFormat) {
                Charset.BASE64 -> encrypted.base64EncodeToString()
                Charset.HEX -> HexUtils.encode(encrypted)
                else -> {
                    showSnackbar("不支持此编码", Snackbar.LENGTH_SHORT)
                    return
                }
            }
            binding.afterText.text = afterText
            showSnackbar("加密成功", Snackbar.LENGTH_SHORT)
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(e.message ?: "加密出错", Snackbar.LENGTH_SHORT)
        }
    }

    private fun decrypt() {
        try {
            val text = when (encryptedFormat) {
                Charset.BASE64 -> binding.text.text.toString().base64DecodeToBytes()
                Charset.HEX -> HexUtils.decode(binding.text.text.toString())
                else -> {
                    showSnackbar("不支持此编码", Snackbar.LENGTH_SHORT)
                    return
                }
            }
            val keyPublic = getKey()
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, keyPublic)
            val decryptedBytes = cipher.doFinal(text)
            val afterText = if (charset == Charset.HEX) {
                HexUtils.encode(decryptedBytes)
            } else {
                String(decryptedBytes, charset(charset))
            }
            binding.afterText.text = afterText
            showSnackbar("解密成功", Snackbar.LENGTH_SHORT)
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(e.message ?: "解密出错", Snackbar.LENGTH_SHORT)
        }
    }

    private fun getKey(): Key {
        val key = when (keyFormat) {
            RSAKeyFormat.Hex -> HexUtils.decode(binding.key.text.toString())
            RSAKeyFormat.PEM -> {
                val reader = StringReader(binding.key.text.toString())
                val pemReader = PemReader(reader)
                val pemObject = pemReader.readPemObject()
                pemObject.content
            }
        }
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

    private fun setKeyFormat(format: RSAKeyFormat) {
        keyFormat = format
        binding.keyFormat.text = format.name
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

}