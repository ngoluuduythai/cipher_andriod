package cn.woolsen.cipher.ui

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityHmacBinding
import cn.woolsen.cipher.enums.Charset
import cn.woolsen.cipher.enums.KeyIvFormat
import cn.woolsen.cipher.util.Base64Utils.base64EncodeToString
import cn.woolsen.cipher.util.ClipUtils
import cn.woolsen.cipher.util.HexUtils
import cn.woolsen.cipher.util.SnackUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import java.lang.Exception
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


/**
 * @author woolsen
 * @date 2021/03/16 15:11
 */
class HMacActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityHmacBinding

    private lateinit var keyFormatMenu: PopupMenu
    private lateinit var algorithmMenu: PopupMenu
    private lateinit var charsetMenu: PopupMenu
    private lateinit var encryptedFormatMenu: PopupMenu

    private var keyFormat = KeyIvFormat.Hex
    private var algorithmId = R.id.hmac_md5
    private var charset = Charset.UTF_8
    private var encryptedFormat = Charset.BASE64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHmacBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.title_hmac)

        keyFormat = KeyIvFormat.ASCII
        binding.keyFormat.setText(R.string.ascii)
        algorithmId = R.id.hmac_md5
        binding.algorithm.setText(R.string.hmac_md5)
        charset = Charset.UTF_8
        binding.charset.setText(R.string.utf8)
        encryptedFormat = Charset.BASE64
        binding.encryptedFormat.setText(R.string.base64)


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
        algorithmMenu = PopupMenu(this, binding.algorithm, Gravity.BOTTOM).apply {
            inflate(R.menu.hmac_algorithm)
            setOnMenuItemClickListener {
                algorithmId = it.itemId
                binding.algorithm.text = it.title
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
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.algorithm -> algorithmMenu.show()
            binding.keyFormat -> keyFormatMenu.show()
            binding.encryptedFormat -> encryptedFormatMenu.show()
            binding.charset -> charsetMenu.show()
            binding.encrypt -> encrypt()
            binding.clip -> {
                ClipUtils.clip(this, binding.afterText.text.toString())
                showSnackbar("已复制到剪切版", Snackbar.LENGTH_SHORT)
            }
        }
    }

    private fun encrypt() {
        try {
            val text = if (charset == Charset.HEX) {
                HexUtils.decode(binding.text.text.toString())
            } else {
                binding.text.text.toString().toByteArray(charset(charset))
            }

            val algorithm = when (algorithmId) {
                R.id.hmac_md5 -> "HmacMD5"
                R.id.hmac_sha1 -> "HmacSHA1"
                R.id.hmac_sha256 -> "HmacSHA256"
                R.id.hmac_sha384 -> "HmacSHA384"
                R.id.hmac_sha512 -> "HmacSHA512"
                else -> {
                    showSnackbar("不支持此编码格式", Snackbar.LENGTH_SHORT)
                    return
                }
            }

            val mac = Mac.getInstance(algorithm)
            val key =  binding.key.text.toString()
            val secretKey = kotlin.run {
                val keyBytes = when (keyFormat) {
                    KeyIvFormat.Hex -> HexUtils.decode(key)
                    KeyIvFormat.ASCII -> key.toByteArray(Charsets.US_ASCII)
                }
                SecretKeySpec(keyBytes, algorithm)
            }
            mac.init(secretKey)
            val afterTextBytes = mac.doFinal(text)
            val afterText = if (encryptedFormat == Charset.HEX) {
                HexUtils.encode(afterTextBytes)
            } else {
                afterTextBytes.base64EncodeToString()
            }
            binding.afterText.text = afterText
            showSnackbar("加密成功", Toast.LENGTH_SHORT)
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(e.message ?: "加密出错", Toast.LENGTH_SHORT)
        }

    }

    private fun setKeyFormat(keyIvFormat: KeyIvFormat) {
        keyFormat = keyIvFormat
        binding.keyFormat.text = keyIvFormat.name
    }
}