package cn.woolsen.cipher.ui

import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.hutool.core.util.HexUtil
import cn.woolsen.cipher.R
import cn.woolsen.cipher.enums.RSAKeyFormat
import cn.woolsen.cipher.databinding.ActivityRsaGenBinding
import cn.woolsen.cipher.util.ClipUtils
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.KeyPairGenerator


/**
 * @author woolsen
 */
class RSAGenActivity : AppCompatActivity(), View.OnClickListener {

    private var bit = 1024
    private lateinit var bitMenu: PopupMenu
    private lateinit var binding: ActivityRsaGenBinding

    private lateinit var keyFormatMenu: PopupMenu
    private var keyFormat = RSAKeyFormat.Hex

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsaGenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.title_rsa_gen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setKeyFormat(RSAKeyFormat.PEM)

        keyFormatMenu = PopupMenu(this, binding.format, Gravity.BOTTOM).apply {
            inflate(R.menu.format_rsa_key)
            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.hex -> setKeyFormat(RSAKeyFormat.Hex)
                    R.id.pem -> setKeyFormat(RSAKeyFormat.PEM)
                }
                true
            }
        }

        bitMenu = PopupMenu(this, binding.bit, Gravity.BOTTOM).apply {
            inflate(R.menu.rsa_gen_bit)
            setOnMenuItemClickListener {
                bit = when (it?.itemId) {
                    R.id.bit512 -> 512
                    R.id.bit1024 -> 1024
                    R.id.bit2048 -> 2048
                    R.id.bit4096 -> 4096
                    else -> return@setOnMenuItemClickListener false
                }
                binding.bit.text = it.title
                true
            }
        }
        binding.bit.setOnClickListener(this)
        binding.generate.setOnClickListener(this)
        binding.clipPrivate.setOnClickListener(this)
        binding.clipPublic.setOnClickListener(this)
        binding.format.setOnClickListener(this)
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

    private fun generate() {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(bit)
        val keyPair = generator.genKeyPair()
        val privateKey = when (keyFormat) {
            RSAKeyFormat.Hex -> HexUtil.encodeHexStr(keyPair.private.encoded)
            RSAKeyFormat.PEM -> {
                val writer = StringWriter()
                val pemWriter = PemWriter(writer)
                pemWriter.writeObject(PemObject("PRIVATE KEY", keyPair.private.encoded))
                pemWriter.flush()
                pemWriter.close()
                writer.toString()
            }
        }
        binding.privateKey.text = privateKey
        val publicKey = when (keyFormat) {
            RSAKeyFormat.Hex -> HexUtil.encodeHexStr(keyPair.public.encoded)
            RSAKeyFormat.PEM -> {
                val writer = StringWriter()
                val pemWriter = PemWriter(writer)
                pemWriter.writeObject(PemObject("PUBLIC KEY", keyPair.public.encoded))
                pemWriter.flush()
                pemWriter.close()
                writer.toString()
            }
        }
        binding.publicKey.text = publicKey
    }

    private fun setKeyFormat(RSAKeyFormat: RSAKeyFormat) {
        keyFormat = RSAKeyFormat
        binding.format.text = RSAKeyFormat.name
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.generate -> generate()
            R.id.bit -> bitMenu.show()
            R.id.format -> keyFormatMenu.show()
            R.id.clip_private -> {
                ClipUtils.clip(this, binding.privateKey.text.toString())
                Toast.makeText(this, "已复制到剪切版", Toast.LENGTH_SHORT).show()
            }
            R.id.clip_public -> {
                ClipUtils.clip(this, binding.publicKey.text.toString())
                Toast.makeText(this, "已复制到剪切版", Toast.LENGTH_SHORT).show()
            }
        }
    }


}