package cn.woolsen.cipher.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.hutool.core.util.HexUtil
import cn.woolsen.cipher.Format
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityRsaGenBinding
import cn.woolsen.cipher.util.ClipUtils
import java.security.KeyPairGenerator

/**
 * @author woolsen
 */
class RSAGenActivity : AppCompatActivity(), View.OnClickListener {

    private var bit = 1024
    private lateinit var bitMenu: PopupMenu
    private lateinit var binding: ActivityRsaGenBinding

//    private lateinit var keyFormatMenu: PopupMenu
//    private var keyFormat = Format.HEX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsaGenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.title_rsa_gen)

//        setKeyFormat(Format.HEX)
//        keyFormatMenu = PopupMenu(this, binding.format, Gravity.BOTTOM).apply {
//            inflate(R.menu.format_key_iv)
//            setOnMenuItemClickListener {
//                when (it?.itemId) {
//                    R.id.hex -> setKeyFormat(Format.HEX)
//                    R.id.ascii -> setKeyFormat(Format.ASCII)
//                }
//                true
//            }
//        }

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
//        binding.format.setOnClickListener(this)
    }

    private fun generate() {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(bit)
        val keyPair = generator.genKeyPair()
        val privateKey = HexUtil.encodeHexStr(keyPair.private.encoded)
        binding.privateKey.text = privateKey
        val publicKey = HexUtil.encodeHexStr(keyPair.public.encoded)
        binding.publicKey.text = publicKey
    }

//    private fun setKeyFormat(format: Format) {
//        keyFormat = format
//        binding.format.text = format.str
//    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.bit -> bitMenu.show()
            R.id.generate -> generate()
//            R.id.format -> keyFormatMenu.show()
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