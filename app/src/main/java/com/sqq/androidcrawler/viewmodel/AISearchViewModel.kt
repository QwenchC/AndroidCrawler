package com.sqq.androidcrawler.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqq.androidcrawler.crawler.WebCrawlerService
import com.sqq.androidcrawler.search.AIService
import com.sqq.androidcrawler.search.ContentAggregatorService
import com.sqq.androidcrawler.search.SearchEngineType
import com.sqq.androidcrawler.search.SearchResult
import com.sqq.androidcrawler.search.SearchService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AISearchViewModel(private val context: Context) : ViewModel() {
    private val searchService = SearchService(context)
    private val crawlerService = WebCrawlerService()
    private val contentAggregator = ContentAggregatorService(crawlerService)
    private val aiService = AIService()
    
    // 状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching
    
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults
    
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse
    
    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _currentSearchEngine = MutableStateFlow(searchService.getCurrentSearchEngineType())
    val currentSearchEngine: StateFlow<SearchEngineType> = _currentSearchEngine
    
    // 缓存聚合内容，避免重复爬取
    private var aggregatedContent = ""
    
    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 搜索引擎切换方法
     */
    fun switchSearchEngine(engineType: SearchEngineType) {
        searchService.switchSearchEngine(engineType)
        _currentSearchEngine.value = engineType
    }
    
    /**
     * 获取搜索引擎名称
     */
    fun getSearchEngineName(engineType: SearchEngineType): String {
        return searchService.getSearchEngineName(engineType)
    }
    
    /**
     * 执行搜索
     */
    fun search() {
        val query = searchQuery.value
        if (query.isBlank()) {
            _errorMessage.value = "请输入搜索内容"
            return
        }
        
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _errorMessage.value = null
                
                // 重置缓存内容
                aggregatedContent = ""
                
                // 执行搜索，现在使用当前选择的搜索引擎
                val results = searchService.search(query)
                _searchResults.value = results
                
                if (results.isEmpty()) {
                    _errorMessage.value = "没有找到相关搜索结果"
                }
            } catch (e: Exception) {
                Log.e("AISearchViewModel", "搜索出错", e)
                _errorMessage.value = "搜索失败: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    /**
     * 生成AI回答
     */
    fun generateAIResponse() {
        if (_searchResults.value.isEmpty()) {
            _errorMessage.value = "请先搜索相关内容"
            return
        }
        
        viewModelScope.launch {
            try {
                _isGeneratingResponse.value = true
                _errorMessage.value = null
                _aiResponse.value = ""
                
                // 如果没有缓存内容，才进行爬取
                if (aggregatedContent.isBlank()) {
                    // 聚合内容
                    aggregatedContent = contentAggregator.aggregateContent(_searchResults.value)
                    
                    if (aggregatedContent.isBlank()) {
                        _errorMessage.value = "无法提取足够的内容来生成回答"
                        _isGeneratingResponse.value = false
                        return@launch
                    }
                }
                
                // 流式生成AI回答
                aiService.generateStreamingResponse(
                    question = _searchQuery.value,
                    searchResults = aggregatedContent,
                    onTextReceived = { text ->
                        _aiResponse.value += text
                    },
                    onComplete = {
                        _isGeneratingResponse.value = false
                    },
                    onError = { error ->
                        _errorMessage.value = error
                        _isGeneratingResponse.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("AISearchViewModel", "生成回答出错", e)
                _errorMessage.value = "生成回答失败: ${e.message}"
                _isGeneratingResponse.value = false
            }
        }
    }
    
    /**
     * 爬取特定搜索结果的内容
     */
    fun crawlSpecificResult(result: SearchResult) {
        viewModelScope.launch {
            try {
                _isGeneratingResponse.value = true
                _errorMessage.value = null
                
                val document = crawlerService.crawlPage(result.url)
                if (document != null) {
                    val content = crawlerService.extractArticleContent(document)
                    _aiResponse.value = "# ${result.title}\n\n$content\n\n来源: ${result.url}"
                } else {
                    _errorMessage.value = "无法爬取该内容，请检查URL是否可访问"
                }
            } catch (e: Exception) {
                _errorMessage.value = "爬取过程出错: ${e.message}"
            } finally {
                _isGeneratingResponse.value = false
            }
        }
    }
}