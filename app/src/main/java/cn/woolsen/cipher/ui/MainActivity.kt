package cn.woolsen.cipher.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import cn.woolsen.cipher.R
import cn.woolsen.cipher.databinding.ActivityMainBinding

/**
 * @author woolsen
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val list = mapOf(
            R.string.title_des to CryptoActivity::class.java,
            R.string.title_aes to CryptoActivity::class.java,
            R.string.title_des_ede to CryptoActivity::class.java,
            R.string.title_rsa_gen to RSAGenActivity::class.java,
            R.string.title_rsa_private to RSACryptoActivity::class.java,
            R.string.title_rsa_public to RSACryptoActivity::class.java
        ).entries.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list.map { getString(it.key) })
        binding.list.adapter = adapter
        binding.list.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, list[position].value).apply {
                putExtra("title", list[position].key)
            }
            startActivity(intent)
        }

    }
}