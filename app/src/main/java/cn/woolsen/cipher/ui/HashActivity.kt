package cn.woolsen.cipher.ui

import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityHashBinding
import cn.woolsen.cipher.enums.Charset
import cn.woolsen.cipher.util.Base64Utils.base64EncodeToString
import cn.woolsen.cipher.util.ClipUtils
import cn.woolsen.cipher.util.HexUtils
import cn.woolsen.cipher.util.SnackUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import java.security.MessageDigest


/**
 * @author woolsen
 * @date 2021/03/16 13:17
 */
class HashActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityHashBinding

    private lateinit var charsetMenu: PopupMenu
    private lateinit var encryptedFormatMenu: PopupMenu
    private var charset = Charset.UTF_8
    private var encryptedFormat = Charset.BASE64

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.title_hash)

        charset = Charset.UTF_8
        binding.charset.setText(R.string.utf8)
        encryptedFormat = Charset.BASE64
        binding.encryptedFormat.setText(R.string.base64)

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
        when (v?.id ?: return) {
            R.id.encrypted_format -> {
                encryptedFormatMenu.show()
                return
            }
            R.id.charset -> {
                charsetMenu.show()
                return
            }
            R.id.encrypt -> {
                encrypt()
                return
            }
            R.id.clip_md5 -> ClipUtils.clip(this, binding.md5.text.toString())
            R.id.clip_sha1 -> ClipUtils.clip(this, binding.sha1.text.toString())
            R.id.clip_sha256 -> ClipUtils.clip(this, binding.sha256.text.toString())
            R.id.clip_sha384 -> ClipUtils.clip(this, binding.sha384.text.toString())
            R.id.clip_sha512 -> ClipUtils.clip(this, binding.sha512.text.toString())
        }
        showSnackbar("已复制到剪切版", Snackbar.LENGTH_SHORT)
    }

    private fun encrypt() {
        val text = if (charset == Charset.HEX) {
            HexUtils.decode(binding.text.text.toString())
        } else {
            binding.text.text.toString().toByteArray(charset(charset))
        }
        encodeWithPrint(text, "MD5", binding.md5)
        encodeWithPrint(text, "SHA1", binding.sha1)
        encodeWithPrint(text, "SHA256", binding.sha256)
        encodeWithPrint(text, "SHA384", binding.sha384)
        encodeWithPrint(text, "SHA512", binding.sha512)
    }

    private fun encodeWithPrint(text: ByteArray, algorithm: String, output: TextView) {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            val encrypted = digest.digest(text)
            output.text = when (encryptedFormat) {
                Charset.BASE64 -> encrypted.base64EncodeToString().dropLastWhile { it == '\n' }
                Charset.HEX -> HexUtils.encode(encrypted)
                else -> {
                    showSnackbar("${algorithm}加密出错", Snackbar.LENGTH_SHORT)
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar("${algorithm}加密出错", Snackbar.LENGTH_SHORT)
        }
    }
}