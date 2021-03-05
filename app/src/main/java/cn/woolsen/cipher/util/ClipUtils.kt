package cn.woolsen.cipher.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * @author woolsen
 * @date 2021/03/05 15:18
 */
object ClipUtils {

    fun clip(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val mClipData = ClipData.newPlainText("Label", text)
        cm.setPrimaryClip(mClipData)
    }
}