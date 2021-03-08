package cn.woolsen.cipher

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * @author woolsen
 * @date 2021/03/08 09:07
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Security.addProvider(BouncyCastleProvider())
    }

}