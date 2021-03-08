package cn.woolsen.cipher.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityRsaCheckBinding
import cn.woolsen.cipher.enums.RSAKeyFormat
import cn.woolsen.cipher.util.HexUtils
import cn.woolsen.cipher.util.SnackUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * @author woolsen
 * @date 2021/03/08 13:21
 */
class RSACheckActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityRsaCheckBinding
    private lateinit var keyFormatMenu: PopupMenu
    private var keyFormat = RSAKeyFormat.PEM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRsaCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTitle(R.string.title_rsa_check)

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

        binding.check.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id ?: return) {
            R.id.check -> check()
            R.id.key_format -> keyFormatMenu.show()
        }
    }

    private fun check() {
        try {
            val keyFactory = KeyFactory.getInstance("RSA")
            //从私钥中提取公钥
            val publicKeyByPrivate = kotlin.run {
                val keyText = binding.privateKey.text.toString()
                val privateKeyBytes = when (keyFormat) {
                    RSAKeyFormat.Hex -> HexUtils.decode(keyText)
                    RSAKeyFormat.PEM -> {
                        val reader = StringReader(keyText)
                        val pemReader = PemReader(reader)
                        val pemObject = pemReader.readPemObject()
                        pemObject.content
                    }
                }
                val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
                val privateKey = keyFactory.generatePrivate(privateKeySpec)
                val private = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec::class.java)
                val keySpec = RSAPublicKeySpec(private.modulus, BigInteger.valueOf(65537))
                keyFactory.generatePublic(keySpec).encoded
            }
            //获取输入的公钥
            val publicKey = kotlin.run {
                val keyText = binding.publicKey.text.toString()
                val keyBytes = when (keyFormat) {
                    RSAKeyFormat.Hex -> HexUtils.decode(keyText)
                    RSAKeyFormat.PEM -> {
                        val reader = StringReader(keyText)
                        val pemReader = PemReader(reader)
                        val pemObject = pemReader.readPemObject()
                        pemObject.content
                    }
                }
                val keySpec = X509EncodedKeySpec(keyBytes)
                keyFactory.generatePublic(keySpec).encoded
            }
            if (publicKeyByPrivate.contentEquals(publicKey)) {
                showSnackbar("恭喜你，公私钥匹配的！", Snackbar.LENGTH_SHORT)
            } else {
                showSnackbar("公私钥不匹配！", Snackbar.LENGTH_SHORT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(e.message ?: "检查出错", Snackbar.LENGTH_SHORT)
        }
    }

    private fun setKeyFormat(keyFormat: RSAKeyFormat) {
        this.keyFormat = keyFormat
        binding.format.text = keyFormat.name
    }
}