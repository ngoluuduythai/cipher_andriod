package cn.woolsen.cipher.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.woolsen.cipher.R
import cn.woolsen.cipher.crypto.Mode
import cn.woolsen.cipher.crypto.symmetric.AES
import cn.woolsen.cipher.crypto.symmetric.DES
import cn.woolsen.cipher.crypto.symmetric.DESede
import cn.woolsen.cipher.databinding.ActivityCryptoBinding
import cn.woolsen.cipher.enums.Charset
import cn.woolsen.cipher.enums.KeyIvtFormat
import cn.woolsen.cipher.util.Base64Utils.base64DecodeToBytes
import cn.woolsen.cipher.util.Base64Utils.base64EncodeToString
import cn.woolsen.cipher.util.ClipUtils
import cn.woolsen.cipher.util.HexUtils
import cn.woolsen.cipher.util.SnackUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author woolsen
 */
class CryptoActivity : AppCompatActivity(), View.OnClickListener,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var modeMenu: PopupMenu
    private lateinit var paddingMenu: PopupMenu
    private lateinit var keyFormatMenu: PopupMenu
    private lateinit var ivFormatMenu: PopupMenu
    private lateinit var charsetMenu: PopupMenu
    private lateinit var encryptedFormatMenu: PopupMenu

    private lateinit var binding: ActivityCryptoBinding

    private var algorithm = Crypto.DES
    private var keyFormat = KeyIvtFormat.Hex
    private var ivFormat = KeyIvtFormat.Hex

    private var charset = Charset.UTF_8
    private var encryptedFormat = Charset.BASE64

    private enum class Crypto {
        AES, DES, DESede
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化选项
        setKeyFormat(KeyIvtFormat.ASCII)
        binding.iv.isEnabled = false

        charset = Charset.UTF_8
        binding.charset.setText(R.string.utf8)
        encryptedFormat = Charset.BASE64
        binding.encryptedFormat.setText(R.string.base64)

        when (val res = intent.getIntExtra("title", 0)) {
            R.string.title_aes -> {
                algorithm = Crypto.AES
                title = getString(res)
            }
            R.string.title_des -> {
                algorithm = Crypto.DES
                title = getString(res)
            }
            R.string.title_des_ede -> {
                algorithm = Crypto.DESede
                title = getString(res)
            }
        }

        binding.mode.setOnClickListener(this)
        binding.padding.setOnClickListener(this)
        binding.encrypt.setOnClickListener(this)
        binding.decrypt.setOnClickListener(this)
        binding.clip.setOnClickListener(this)
        binding.ivFormat.setOnClickListener(this)
        binding.keyFormat.setOnClickListener(this)
        binding.charset.setOnClickListener(this)
        binding.encryptedFormat.setOnClickListener(this)


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
                        showSnackbar("不支持此编码格式", Toast.LENGTH_SHORT)
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
                        showSnackbar("不支持此编码格式", Toast.LENGTH_SHORT)
                        return@setOnMenuItemClickListener true
                    }
                }
                binding.encryptedFormat.text = it.title
                true
            }
        }

        keyFormatMenu = PopupMenu(this, binding.keyFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_key_iv)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.hex -> setKeyFormat(KeyIvtFormat.Hex)
                    R.id.ascii -> setKeyFormat(KeyIvtFormat.ASCII)
                }
                true
            }
        }
        ivFormatMenu = PopupMenu(this, binding.ivFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_key_iv)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.hex -> setIvFormat(KeyIvtFormat.Hex)
                    R.id.ascii -> setIvFormat(KeyIvtFormat.ASCII)
                }
                true
            }
        }

        modeMenu = PopupMenu(this, binding.mode, Gravity.BOTTOM).apply {
            inflate(R.menu.mode)
            setOnMenuItemClickListener(this@CryptoActivity)
        }
        paddingMenu = PopupMenu(this, binding.padding, Gravity.BOTTOM).apply {
            inflate(R.menu.padding)
            setOnMenuItemClickListener(this@CryptoActivity)
        }
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
            R.id.mode -> modeMenu.show()
            R.id.padding -> paddingMenu.show()
            R.id.iv_format -> ivFormatMenu.show()
            R.id.key_format -> keyFormatMenu.show()
            R.id.charset -> charsetMenu.show()
            R.id.encrypted_format -> encryptedFormatMenu.show()
            R.id.encrypt -> encrypt()
            R.id.decrypt -> decrypt()
            R.id.clip -> {
                ClipUtils.clip(this, binding.afterText.text.toString())
                showSnackbar("已复制到剪切版", Snackbar.LENGTH_SHORT)
            }
            else -> return
        }
        hideKeyboard()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.ecb -> {
                binding.mode.text = item.title
                binding.iv.setText("")
                binding.iv.isEnabled = false
            }
            R.id.cbc, R.id.cfb, R.id.ofb, R.id.ctr -> {
                binding.mode.text = item.title
                binding.iv.isEnabled = true
            }
            R.id.pkcs5, R.id.no, R.id.zero -> {
                binding.padding.text = item.title
            }
            else -> return false
        }
        return true
    }

    private fun encrypt() {
        val mode = binding.mode.text.toString()
        val padding = binding.padding.text.toString()
        try {
            val text = if (charset == Charset.HEX) {
                HexUtils.decode(binding.text.text.toString())
            } else {
                binding.text.text.toString().toByteArray(charset(charset))
            }
            val key = getKey()
            val iv = getIv()
            val crypto = when (algorithm) {
                Crypto.AES -> AES(mode, padding, key, iv)
                Crypto.DES -> DES(mode, padding, key, iv)
                Crypto.DESede -> DESede(mode, padding, key, iv)
            }
            val encrypted = crypto.encrypt(text)
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
        val mode = binding.mode.text.toString()
        val padding = binding.padding.text.toString()
        try {
            val text = when (encryptedFormat) {
                Charset.BASE64 -> binding.text.text.toString().base64DecodeToBytes()
                Charset.HEX -> HexUtils.decode(binding.text.text.toString())
                else -> {
                    showSnackbar("不支持此编码", Snackbar.LENGTH_SHORT)
                    return
                }
            }
            val key = getKey()
            val iv = getIv()
            val crypto = when (algorithm) {
                Crypto.AES -> AES(mode, padding, key, iv)
                Crypto.DES -> DES(mode, padding, key, iv)
                Crypto.DESede -> DESede(mode, padding, key, iv)
            }
            val decryptedBytes = crypto.decrypt(text)
            val afterText = if (charset == Charset.HEX) {
                HexUtils.encode(decryptedBytes)
            } else {
                String(decryptedBytes, charset(charset))
            }
            binding.afterText.text = afterText
            showSnackbar("解密成功", Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(e.message ?: "解密出错", Snackbar.LENGTH_SHORT)
        }
    }

    private fun getKey(): ByteArray {
        return when (keyFormat) {
            KeyIvtFormat.Hex -> HexUtils.decode(binding.key.text.toString())
            KeyIvtFormat.ASCII -> binding.key.text.toString().toByteArray()
        }
    }

    private fun getIv(): ByteArray? {
        val mode = binding.mode.text.toString()
        val iv = binding.iv.text.toString()
        return if (mode == Mode.ECB.name || iv.isEmpty()) {
            null
        } else {
            when (ivFormat) {
                KeyIvtFormat.Hex -> HexUtils.decode(binding.iv.text.toString())
                KeyIvtFormat.ASCII -> binding.iv.text.toString().toByteArray()
            }
        }
    }

    private fun setIvFormat(keyIvtFormat: KeyIvtFormat) {
        ivFormat = keyIvtFormat
        binding.ivFormat.text = keyIvtFormat.name
    }

    private fun setKeyFormat(keyIvtFormat: KeyIvtFormat) {
        keyFormat = keyIvtFormat
        binding.keyFormat.text = keyIvtFormat.name
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

}