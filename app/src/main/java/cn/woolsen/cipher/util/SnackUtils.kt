package cn.woolsen.cipher.util

import android.app.Activity
import com.google.android.material.snackbar.Snackbar

/**
 * @author woolsen
 * @date 2021/03/06 17:34
 */
object SnackUtils {

    fun Activity.showSnackbar(text: CharSequence, duration: Int) {
        Snackbar.make(this.window.decorView, text, duration).show()
    }

}