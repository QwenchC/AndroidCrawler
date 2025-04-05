package com.sqq.androidcrawler.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * 将文本复制到剪贴板
 */
fun copyTextToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    
    // Android 13及以上系统不再显示复制成功的Toast提示
    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}