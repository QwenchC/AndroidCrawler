package com.sqq.androidcrawler.search

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 搜狗搜索引擎实现
 */
class SogouSearchEngine : ISearchEngine {
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
                val url = "https://www.sogou.com/web?query=$encodedQuery&ie=utf8"
                
                Log.d("SogouSearch", "开始搜索: $query")
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = searchClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val document = Jsoup.parse(body)
                    
                    // 搜狗搜索结果选择器
                    val results = document.select(".vrwrap, .rb")
                    
                    Log.d("SogouSearch", "找到 ${results.size} 条搜索结果")
                    
                    results.mapNotNull { result ->
                        try {
                            val titleElement = result.selectFirst("h3 a")
                            val linkElement = result.selectFirst("h3 a")
                            val snippetElement = result.selectFirst(".ft")
                            
                            if (titleElement != null && linkElement != null) {
                                // 搜狗链接需要处理，因为它用JavaScript重定向
                                val originalUrl = linkElement.attr("href")
                                
                                // 处理搜狗链接
                                val realUrl = if (originalUrl.startsWith("http")) {
                                    originalUrl
                                } else {
                                    "https://www.sogou.com$originalUrl"
                                }
                                
                                SearchResult(
                                    title = titleElement.text(),
                                    url = realUrl,
                                    snippet = snippetElement?.text() ?: "",
                                    engineType = SearchEngineType.SOGOU
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e("SogouSearch", "处理搜索结果项出错: ${e.message}")
                            null
                        }
                    }
                } else {
                    Log.e("SogouSearch", "搜索请求失败: ${response.code}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("SogouSearch", "搜索出错: ${e.message}", e)
                emptyList()
            }
        }
    }
}