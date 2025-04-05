package com.sqq.androidcrawler.search

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 搜索引擎类型
 */
enum class SearchEngineType {
    DUCKDUCKGO, SOGOU
}

/**
 * 搜索引擎提供者，管理不同的搜索引擎实现
 */
object SearchEngineProvider {
    private lateinit var prefs: SharedPreferences
    
    // 所有可用的搜索引擎
    private val searchEngines = mutableMapOf<SearchEngineType, ISearchEngine>()
    
    // 初始化搜索引擎提供者
    fun init(context: Context) {
        prefs = context.getSharedPreferences("search_settings", Context.MODE_PRIVATE)
        
        // 注册所有搜索引擎
        searchEngines[SearchEngineType.DUCKDUCKGO] = DuckDuckGoSearchEngine()
        searchEngines[SearchEngineType.SOGOU] = SogouSearchEngine()
        
        Log.d("SearchEngineProvider", "已初始化 ${searchEngines.size} 个搜索引擎")
    }
    
    // 获取当前选择的搜索引擎
    fun getCurrentSearchEngine(): ISearchEngine {
        val engineName = prefs.getString("current_search_engine", SearchEngineType.DUCKDUCKGO.name)
        val engineType = try {
            SearchEngineType.valueOf(engineName ?: SearchEngineType.DUCKDUCKGO.name)
        } catch (e: Exception) {
            SearchEngineType.DUCKDUCKGO
        }
        
        return searchEngines[engineType] ?: searchEngines[SearchEngineType.DUCKDUCKGO]!!
    }
    
    // 获取当前选择的搜索引擎类型
    fun getCurrentSearchEngineType(): SearchEngineType {
        val engineName = prefs.getString("current_search_engine", SearchEngineType.DUCKDUCKGO.name)
        return try {
            SearchEngineType.valueOf(engineName ?: SearchEngineType.DUCKDUCKGO.name)
        } catch (e: Exception) {
            SearchEngineType.DUCKDUCKGO
        }
    }
    
    // 设置当前搜索引擎
    fun setCurrentSearchEngine(engineType: SearchEngineType) {
        if (searchEngines.containsKey(engineType)) {
            prefs.edit().putString("current_search_engine", engineType.name).apply()
            Log.d("SearchEngineProvider", "已切换搜索引擎为: $engineType")
        }
    }
    
    // 获取搜索引擎名称 - 用于显示
    fun getSearchEngineName(engineType: SearchEngineType): String {
        return when (engineType) {
            SearchEngineType.DUCKDUCKGO -> "DuckDuckGo"
            SearchEngineType.SOGOU -> "搜狗"
        }
    }
}

/**
 * 搜索引擎接口
 */
interface ISearchEngine {
    suspend fun search(query: String): List<SearchResult>
}