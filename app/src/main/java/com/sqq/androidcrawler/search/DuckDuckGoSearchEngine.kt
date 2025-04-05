package com.sqq.androidcrawler.search

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * DuckDuckGo搜索引擎实现
 */
class DuckDuckGoSearchEngine : ISearchEngine {
    // 为搜索创建独立的客户端实例
    private val searchClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = 5 })
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    override suspend fun search(query: String): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
                
                Log.d("DuckDuckGoSearch", "开始搜索: $query")
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = searchClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val document = Jsoup.parse(body)
                    val results = document.select(".result")
                    
                    Log.d("DuckDuckGoSearch", "找到 ${results.size} 条搜索结果")
                    
                    results.mapNotNull { result ->
                        try {
                            val titleElement = result.selectFirst(".result__title")
                            val linkElement = result.selectFirst(".result__a") 
                                ?: result.selectFirst(".result__title a[href]")
                                ?: result.selectFirst("a[href]")
                            
                            val snippetElement = result.selectFirst(".result__snippet")
                            
                            if (titleElement != null && linkElement != null) {
                                val originalUrl = linkElement.attr("href")
                                val normalizedUrl = extractDuckDuckGoUrl(originalUrl)
                                
                                SearchResult(
                                    title = titleElement.text(),
                                    url = normalizedUrl,
                                    snippet = snippetElement?.text() ?: "",
                                    engineType = SearchEngineType.DUCKDUCKGO
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e("DuckDuckGoSearch", "处理结果项失败: ${e.message}")
                            null
                        }
                    }
                } else {
                    Log.e("DuckDuckGoSearch", "搜索请求失败: ${response.code}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("DuckDuckGoSearch", "搜索出错: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * 从DuckDuckGo URL中提取真实URL
     */
    private fun extractDuckDuckGoUrl(url: String): String {
        try {
            // 处理相对路径
            if (url.startsWith("/")) {
                return "https://html.duckduckgo.com$url"
            }
            
            // 提取uddg参数
            val uddgPattern = "uddg=([^&]+)".toRegex()
            val uddgMatch = uddgPattern.find(url)
            
            if (uddgMatch != null) {
                val encoded = uddgMatch.groupValues[1]
                try {
                    return URLDecoder.decode(encoded, "UTF-8")
                } catch (e: Exception) {
                    Log.e("DuckDuckGoSearch", "URL解码失败: ${e.message}")
                }
            }
            
            // 尝试其他提取方法
            if (url.contains("duckduckgo.com/l/") || url.contains("/l/?")) {
                val startIndex = url.indexOf("uddg=")
                if (startIndex >= 0) {
                    val valueStartIndex = startIndex + 5
                    val valueEndIndex = url.indexOf('&', valueStartIndex).takeIf { it > 0 } ?: url.length
                    val encoded = url.substring(valueStartIndex, valueEndIndex)
                    
                    try {
                        return URLDecoder.decode(encoded, "UTF-8")
                    } catch (e: Exception) {
                        Log.e("DuckDuckGoSearch", "URL解码失败(方法2): ${e.message}")
                    }
                }
            }
            
            // 如果是普通URL，直接返回
            if (url.startsWith("http")) {
                return url
            }
            
            return url
        } catch (e: Exception) {
            Log.e("DuckDuckGoSearch", "URL处理异常: ${e.message}")
            return url
        }
    }
}