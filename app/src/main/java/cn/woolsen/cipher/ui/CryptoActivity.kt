package cn.woolsen.cipher.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.PopupWindowCompat
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityCryptoBinding
import cn.woolsen.cipher.enums.Charset
import cn.woolsen.cipher.enums.KeyIvFormat
import cn.woolsen.cipher.util.Base64Utils.base64DecodeToBytes
import cn.woolsen.cipher.util.Base64Utils.base64EncodeToString
import cn.woolsen.cipher.util.ClipUtils
import cn.woolsen.cipher.util.HexUtils
import cn.woolsen.cipher.util.SnackUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.IvParameterSpec
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

    private lateinit var blockSizeMenu: PopupMenu
    private var blockSize: Int = 128

    private lateinit var binding: ActivityCryptoBinding

    private var algorithm = Crypto.DES
    private var keyFormat = KeyIvFormat.Hex
    private var ivFormat = KeyIvFormat.Hex

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
        setKeyFormat(KeyIvFormat.ASCII)
        setIvFormat(KeyIvFormat.ASCII)
        binding.iv.isEnabled = false
        binding.layoutIv.isVisible = false
        charset = Charset.UTF_8
        binding.charset.setText(R.string.utf8)
        encryptedFormat = Charset.BASE64
        binding.encryptedFormat.setText(R.string.base64)

        val res = intent.getIntExtra("title", 0)
        when (res) {
            R.string.title_aes -> {
                algorithm = Crypto.AES
                binding.blockSize.setOnClickListener(this)
                binding.layoutBlockSize.isVisible = true
                blockSizeMenu = PopupMenu(this, binding.blockSize, Gravity.BOTTOM).apply {
                    inflate(R.menu.block_size)
                    setOnMenuItemClickListener {
                        blockSize = when (it.itemId) {
                            R.id.bit128 -> 128
                            R.id.bit192 -> 192
                            R.id.bit256 -> 256
                            else -> {
                                showSnackbar("不支持此编码格式", Toast.LENGTH_SHORT)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        binding.blockSize.text = it.title
                        true
                    }
                }
            }
            R.string.title_des -> {
                algorithm = Crypto.DES
                binding.layoutBlockSize.isVisible = false
            }
            R.string.title_des_ede -> {
                algorithm = Crypto.DESede
                binding.layoutBlockSize.isVisible = false
            }
        }
        setTitle(res)

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
                    R.id.hex -> setKeyFormat(KeyIvFormat.Hex)
                    R.id.ascii -> setKeyFormat(KeyIvFormat.ASCII)
                }
                true
            }
        }
        ivFormatMenu = PopupMenu(this, binding.ivFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_key_iv)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.hex -> setIvFormat(KeyIvFormat.Hex)
                    R.id.ascii -> setIvFormat(KeyIvFormat.ASCII)
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
            R.id.block_size -> blockSizeMenu.show()
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
                binding.layoutIv.isVisible = false
                binding.iv.text = null
                binding.iv.isEnabled = false
            }
            R.id.cbc, R.id.cfb, R.id.ofb, R.id.ctr -> {
                binding.layoutIv.isVisible = true
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
        try {
            val text = if (charset == Charset.HEX) {
                HexUtils.decode(binding.text.text.toString())
            } else {
                binding.text.text.toString().toByteArray(charset(charset))
            }

            val encryptedBytes = doFinal(Cipher.ENCRYPT_MODE, text)

            val afterText = when (encryptedFormat) {
                Charset.BASE64 -> encryptedBytes.base64EncodeToString()
                Charset.HEX -> HexUtils.encode(encryptedBytes)
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

            val decryptedBytes = doFinal(Cipher.DECRYPT_MODE, text)

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

    private fun doFinal(opmode: Int, text: ByteArray): ByteArray {
        val mode = binding.mode.text.toString()
        var padding = binding.padding.text.toString()
        val isZeroPadding = padding == "ZeroPadding"
        if (isZeroPadding) {
            padding = "NoPadding"
        }
        val iv = getIv()
        val alg = when (algorithm) {
            Crypto.AES -> "AES"
            Crypto.DES -> "DES"
            Crypto.DESede -> "DESede"
        }
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("${alg}/${mode}/${padding}")
        if (iv == null) {
            cipher.init(opmode, secretKey)
        } else {
            cipher.init(opmode, secretKey, IvParameterSpec(iv))
        }

        var data = text
        if (isZeroPadding) {
            val blockSize = cipher.blockSize
            val length: Int = text.size
            // 按照块拆分后的数据中多余的数据
            val remainLength: Int = length % blockSize
            if (remainLength > 0) {
                // 新长度为blockSize的整数倍，多余部分填充0
                data = text.copyOf(length + blockSize - remainLength)
            }
        }

        return cipher.doFinal(data)
    }

    private fun getSecretKey(): SecretKey {
        var keyBytes = when (keyFormat) {
            KeyIvFormat.Hex -> HexUtils.decode(binding.key.text.toString())
            KeyIvFormat.ASCII -> binding.key.text.toString().toByteArray()
        }
        return when (algorithm) {
            Crypto.AES -> {
                keyBytes = keyBytes.copyOf(blockSize / 8)
                SecretKeySpec(keyBytes, "RSA")
            }
            Crypto.DES -> {
                val keyFactory = SecretKeyFactory.getInstance("DES")
                keyFactory.generateSecret(DESKeySpec(keyBytes))
            }
            Crypto.DESede -> {
                val keyFactory = SecretKeyFactory.getInstance("DESede")
                keyFactory.generateSecret(DESedeKeySpec(keyBytes))
            }
        }
    }

    private fun getIv(): ByteArray? {
        val mode = binding.mode.text.toString()
        val iv = binding.iv.text.toString()
        return if (mode == "ECB" || iv.isEmpty()) {
            null
        } else {
            when (ivFormat) {
                KeyIvFormat.Hex -> HexUtils.decode(binding.iv.text.toString())
                KeyIvFormat.ASCII -> binding.iv.text.toString().toByteArray()
            }
        }
    }

    private fun setIvFormat(keyIvFormat: KeyIvFormat) {
        ivFormat = keyIvFormat
        binding.ivFormat.text = keyIvFormat.name
    }

    private fun setKeyFormat(keyIvFormat: KeyIvFormat) {
        keyFormat = keyIvFormat
        binding.keyFormat.text = keyIvFormat.name
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

}