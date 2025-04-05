package com.sqq.androidcrawler.search

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * AI服务 - 与Pollinations.ai API通信
 */
class AIService {
    // 使用lazy初始化完全独立的OkHttpClient
    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            // 重要: 使用独立的连接池
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            // 重要: 使用独立的分发器
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = 5 })
            // 明确标记为AIService的请求
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "AndroidCrawler/1.0 AIService")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    private val gson = Gson()
    
    /**
     * 生成流式AI回答
     */
    fun generateStreamingResponse(
        question: String,
        searchResults: String,
        onTextReceived: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val random = Random.nextInt(10000)
        
        try {
            // 系统参数过长可能是问题所在，限制一下长度
            val trimmedResults = if (searchResults.length > 50000) {
                Log.w("AIService", "系统参数过长，进行截断")
                searchResults.substring(0, 50000) + "\n\n[内容已截断]"
            } else {
                searchResults
            }
            
            // 分开编码所有参数
            val prompt = URLEncoder.encode("综合网页搜索结果对${question}进行回答", "UTF-8")
            val systemParam = URLEncoder.encode(trimmedResults, "UTF-8")
            
            // 记录系统参数长度，便于调试
            Log.d("AIService", "系统参数长度: ${trimmedResults.length}字符")
            
            // 构建URL
            val url = "https://text.pollinations.ai/$prompt" +
                    "?model=openai&seed=$random&stream=true&system=$systemParam"
            
            Log.d("AIService", "请求URL长度: ${url.length}")
            
            val request = Request.Builder()
                .url(url)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("AIService", "API请求失败", e)
                    Handler(Looper.getMainLooper()).post {
                        onError("请求失败: ${e.message}")
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    // 详细记录响应情况
                    Log.d("AIService", "收到API响应: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        Log.e("AIService", "API响应错误: ${response.code}, ${response.message}")
                        Handler(Looper.getMainLooper()).post {
                            onError("请求失败: ${response.code}")
                        }
                        return
                    }
                    
                    response.body?.source()?.let { source -> 
                        try {
                            // 处理流式响应
                            while (!source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                
                                // 判断是否是数据行
                                if (line.startsWith("data: ")) {
                                    val jsonContent = line.substring(6).trim()
                                    if (jsonContent == "[DONE]") continue
                                    
                                    // 解析JSON内容，提取实际文本
                                    try {
                                        val content = extractTextFromJson(jsonContent)
                                        if (content.isNotEmpty()) {
                                            // 解析内容并发送
                                            Handler(Looper.getMainLooper()).post {
                                                onTextReceived(content)
                                            }
                                        }
                                    } catch (e: JsonParseException) {
                                        Log.e("AIService", "JSON解析错误: $jsonContent", e)
                                    }
                                }
                            }
                            
                            Handler(Looper.getMainLooper()).post {
                                onComplete()
                            }
                        } catch (e: Exception) {
                            Log.e("AIService", "处理响应数据时出错", e)
                            Handler(Looper.getMainLooper()).post {
                                onError("读取响应失败: ${e.message}")
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("AIService", "构建请求时出错", e)
            onError("构建请求失败: ${e.message}")
        }
    }
    
    /**
     * 从OpenAI API返回的JSON中提取文本内容
     */
    private fun extractTextFromJson(json: String): String {
        try {
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            
            // 检查是否有choices数组
            if (jsonObject.has("choices") && jsonObject.get("choices").isJsonArray) {
                val choices = jsonObject.getAsJsonArray("choices")
                if (choices.size() > 0) {
                    val firstChoice = choices.get(0).asJsonObject
                    
                    // 提取delta中的content字段
                    if (firstChoice.has("delta") && firstChoice.get("delta").isJsonObject) {
                        val delta = firstChoice.getAsJsonObject("delta")
                        if (delta.has("content") && !delta.get("content").isJsonNull) {
                            return delta.get("content").asString
                        }
                    }
                }
            }
            return ""
        } catch (e: Exception) {
            Log.e("AIService", "JSON解析异常", e)
            return ""
        }
    }
}