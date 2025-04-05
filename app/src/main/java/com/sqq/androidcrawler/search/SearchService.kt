package com.sqq.androidcrawler.search

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 搜索服务 - 使用多种搜索引擎
 */
class SearchService(private val context: Context) {
    init {
        // 初始化搜索引擎提供者
        SearchEngineProvider.init(context)
    }
    
    /**
     * 获取当前选择的搜索引擎类型
     */
    fun getCurrentSearchEngineType(): SearchEngineType {
        return SearchEngineProvider.getCurrentSearchEngineType()
    }
    
    /**
     * 获取搜索引擎显示名称
     */
    fun getSearchEngineName(engineType: SearchEngineType): String {
        return SearchEngineProvider.getSearchEngineName(engineType)
    }
    
    /**
     * 切换搜索引擎
     */
    fun switchSearchEngine(engineType: SearchEngineType) {
        SearchEngineProvider.setCurrentSearchEngine(engineType)
    }
    
    /**
     * 执行搜索
     */
    suspend fun search(query: String): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val searchEngine = SearchEngineProvider.getCurrentSearchEngine()
                Log.d("SearchService", "使用搜索引擎: ${getCurrentSearchEngineType()}")
                
                searchEngine.search(query)
            } catch (e: Exception) {
                Log.e("SearchService", "搜索时出错: ${e.message}", e)
                emptyList()
            }
        }
    }
}

/**
 * 搜索结果数据类
 */
data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val engineType: SearchEngineType = SearchEngineType.DUCKDUCKGO
)