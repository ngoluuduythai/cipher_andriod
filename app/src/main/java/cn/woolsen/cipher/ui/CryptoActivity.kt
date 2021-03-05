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
import androidx.core.content.edit
import cn.hutool.core.util.HexUtil
import cn.woolsen.cipher.enums.KeyIvtFormat
import cn.woolsen.cipher.R
import cn.woolsen.cipher.crypto.Mode
import cn.woolsen.cipher.crypto.symmetric.AES
import cn.woolsen.cipher.crypto.symmetric.DES
import cn.woolsen.cipher.crypto.symmetric.DESede
import cn.woolsen.cipher.databinding.ActivityCryptoBinding
import cn.woolsen.cipher.util.ClipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author woolsen
 */
class CryptoActivity : AppCompatActivity(), View.OnClickListener,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var modeMenu: PopupMenu
    private lateinit var paddingMenu: PopupMenu
    private lateinit var keyFormatMenu: PopupMenu
    private lateinit var ivFormatMenu: PopupMenu
    private lateinit var binding: ActivityCryptoBinding

    private var algorithm = Crypto.DES
    private var keyFormat = KeyIvtFormat.Hex
    private var ivFormat = KeyIvtFormat.Hex

    private enum class Crypto {
        AES, DES, DESede
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setKeyFormat(KeyIvtFormat.ASCII)

        getFormatFromSP()

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

        binding.iv.isEnabled = false
        keyFormatMenu = PopupMenu(this, binding.keyFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_key_iv)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.hex -> setKeyFormat(KeyIvtFormat.Hex)
                    R.id.ascii -> setKeyFormat(KeyIvtFormat.ASCII)
                }
                saveFormatToSP()
                true
            }
        }

        ivFormatMenu = PopupMenu(this, binding.ivFormat, Gravity.BOTTOM).apply {
            inflate(R.menu.format_key_iv)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.hex -> setIvFormat(KeyIvtFormat.Hex)
                    R.id.ascii -> setIvFormat(KeyIvtFormat.ASCII)
                }
                saveFormatToSP()
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
            R.id.encrypt -> encrypt()
            R.id.decrypt -> decrypt()
            R.id.iv_format -> ivFormatMenu.show()
            R.id.key_format -> keyFormatMenu.show()
            R.id.clip -> {
                ClipUtils.clip(this, binding.afterText.text.toString())
                Toast.makeText(this, "已复制到剪切版", Toast.LENGTH_SHORT).show()
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
        val text = HexUtil.decodeHex(binding.text.text.toString())
        val mode = binding.mode.text.toString()
        val padding = binding.padding.text.toString()
        try {
            val key = getKey()
            val iv = getIv()
            val crypto = when (algorithm) {
                Crypto.AES -> AES(mode, padding, key, iv)
                Crypto.DES -> DES(mode, padding, key, iv)
                Crypto.DESede -> DESede(mode, padding, key, iv)
            }
            val encrypted = crypto.encrypt(text)
            val afterText = HexUtil.encodeHexStr(encrypted)
            binding.afterText.setText(afterText)
            Toast.makeText(this, "加密成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message ?: "加密出错", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decrypt() {
        val text = HexUtil.decodeHex(binding.text.text.toString())
        val mode = binding.mode.text.toString()
        val padding = binding.padding.text.toString()
        try {
            val key = getKey()
            val iv = getIv()
            val crypto = when (algorithm) {
                Crypto.AES -> AES(mode, padding, key, iv)
                Crypto.DES -> DES(mode, padding, key, iv)
                Crypto.DESede -> DESede(mode, padding, key, iv)
            }
            val decrypted = HexUtil.encodeHexStr(crypto.decrypt(text))
            binding.afterText.setText(decrypted)
            Toast.makeText(this, "解密成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message ?: "解密出错", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getKey(): ByteArray {
        return when (keyFormat) {
            KeyIvtFormat.Hex -> HexUtil.decodeHex(binding.key.text.toString())
            KeyIvtFormat.ASCII -> binding.key.text.toString().toByteArray()
        }
    }

    private fun getIv(): ByteArray? {
        val mode = binding.mode.text.toString()
        return if (mode == Mode.ECB.name) {
            null
        } else {
            when (ivFormat) {
                KeyIvtFormat.Hex -> HexUtil.decodeHex(binding.iv.text.toString())
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


    /**
     * 将当前使用的iv和key编码保存到SharedPreferences
     */
    private fun saveFormatToSP() {
        GlobalScope.launch(Dispatchers.IO) {
            getSharedPreferences("format", Context.MODE_PRIVATE).edit {
                putString("${title}_iv_format_name", ivFormat.name)
                putString("${title}_key_format_name", keyFormat.name)
            }
        }
    }

    /**
     * 从SharedPreferences中获取上一次使用的iv和key编码
     */
    private fun getFormatFromSP() {
        GlobalScope.launch(Dispatchers.IO) {
            val sp = getSharedPreferences("format", Context.MODE_PRIVATE)
            val ivFormat = try {
                KeyIvtFormat.valueOf(sp.getString("${title}_iv_format_name", null) ?: ivFormat.name)
            } catch (e: Exception) {
                KeyIvtFormat.Hex
            }
            setIvFormat(ivFormat)
            val keyFormat = try {
                KeyIvtFormat.valueOf(sp.getString("${title}_key_format_name", null) ?: keyFormat.name)
            } catch (e: Exception) {
                KeyIvtFormat.Hex
            }
            setKeyFormat(keyFormat)
        }

    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

}