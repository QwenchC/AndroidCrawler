package com.sqq.androidcrawler.utils

import android.util.Log
import java.net.URLDecoder

/**
 * URL处理工具类
 */
object UrlUtils {
    /**
     * 从DuckDuckGo重定向URL中提取真实URL
     */
    fun extractRealUrl(url: String): String {
        try {
            Log.d("UrlUtils", "正在解析URL: $url")
            
            // 特殊情况：修复带有双斜杠的URL
            val normalizedUrl = url.replace("//duck", "/duck")
            
            // 处理相对路径
            if (normalizedUrl.startsWith("/")) {
                return "https://html.duckduckgo.com$normalizedUrl"
            }
            
            // 方法1: 通过正则表达式提取uddg参数（更宽松的匹配）
            val uddgPattern = "[?&]uddg=([^&]+)".toRegex()
            val uddgMatch = uddgPattern.find(normalizedUrl)
            
            if (uddgMatch != null) {
                val encoded = uddgMatch.groupValues[1]
                try {
                    val decoded = URLDecoder.decode(encoded, "UTF-8")
                    Log.d("UrlUtils", "成功解析URL: $decoded (方法1)")
                    return decoded
                } catch (e: Exception) {
                    Log.e("UrlUtils", "URL解码失败(方法1): ${e.message}")
                }
            }
            
            // 方法2: 直接字符串处理
            if (normalizedUrl.contains("duckduckgo.com") && normalizedUrl.contains("uddg=")) {
                val startIndex = normalizedUrl.indexOf("uddg=")
                if (startIndex >= 0) {
                    val valueStartIndex = startIndex + 5
                    val valueEndIndex = normalizedUrl.indexOf('&', valueStartIndex).takeIf { it > 0 } ?: normalizedUrl.length
                    val encoded = normalizedUrl.substring(valueStartIndex, valueEndIndex)
                    
                    try {
                        val decoded = URLDecoder.decode(encoded, "UTF-8")
                        Log.d("UrlUtils", "成功解析URL: $decoded (方法2)")
                        return decoded
                    } catch (e: Exception) {
                        Log.e("UrlUtils", "URL解码失败(方法2): ${e.message}")
                    }
                }
            }
            
            // 方法3: 应对更复杂的URL格式
            if (normalizedUrl.contains("uddg=")) {
                // 尝试从URL的任何位置提取uddg参数
                val startIndex = normalizedUrl.indexOf("uddg=")
                if (startIndex >= 0) {
                    val valueStartIndex = startIndex + 5
                    var valueEndIndex = normalizedUrl.length
                    
                    // 查找下一个可能的分隔符
                    val possibleEndIndex = normalizedUrl.indexOf('&', valueStartIndex)
                    if (possibleEndIndex > 0) {
                        valueEndIndex = possibleEndIndex
                    }
                    
                    val encoded = normalizedUrl.substring(valueStartIndex, valueEndIndex)
                    try {
                        val decoded = URLDecoder.decode(encoded, "UTF-8")
                        Log.d("UrlUtils", "成功解析URL: $decoded (方法3)")
                        return decoded
                    } catch (e: Exception) {
                        Log.e("UrlUtils", "URL解码失败(方法3): ${e.message}, 编码值: $encoded")
                    }
                }
            }
            
            // 如果是普通URL，直接返回
            if (normalizedUrl.startsWith("http")) {
                return normalizedUrl
            }
            
            // 无法解析，返回原始URL
            Log.w("UrlUtils", "无法解析URL，返回原始URL: $url")
            return url
        } catch (e: Exception) {
            Log.e("UrlUtils", "URL处理异常: ${e.message}", e)
            return url
        }
    }
}