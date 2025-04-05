package com.sqq.androidcrawler.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqq.androidcrawler.crawler.WebCrawlerService
import com.sqq.androidcrawler.data.CrawledItem
import com.sqq.androidcrawler.utils.ComplianceChecker
import kotlinx.coroutines.launch

class CrawlerViewModel : ViewModel() {
    private val crawlerService = WebCrawlerService()
    private val complianceChecker = ComplianceChecker()
    
    var crawledItems by mutableStateOf<List<CrawledItem>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // 爬取偏好设置 - 将默认值从true改为false
    var crawlTitles by mutableStateOf(false)
        private set
        
    var crawlContent by mutableStateOf(false)
        private set
        
    var crawlImages by mutableStateOf(false)
        private set
    
    // 切换偏好状态的方法
    fun toggleCrawlTitles() {
        crawlTitles = !crawlTitles
    }
    
    fun toggleCrawlContent() {
        crawlContent = !crawlContent
    }
    
    fun toggleCrawlImages() {
        crawlImages = !crawlImages
    }
    
    fun startCrawling(url: String) {
        // 确保URL格式正确
        var finalUrl = url.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "https://$finalUrl"
        }
        
        // 确保至少一种爬取类型被启用
        if (!crawlTitles && !crawlContent && !crawlImages) {
            errorMessage = "请至少选择一种要爬取的内容类型"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // 检查robots.txt
                val allowedByRobots = crawlerService.checkRobotsTxt(finalUrl)
                if (!allowedByRobots) {
                    errorMessage = "该网站的robots.txt禁止爬取，根据合规要求已停止"
                    isLoading = false
                    return@launch
                }
                
                // 爬取页面
                val document = crawlerService.crawlPage(finalUrl)
                if (document != null) {
                    // 使用新的结构化内容提取方法
                    val contentSections = crawlerService.extractStructuredContent(document)
                    
                    // 单独提取图片
                    val imageUrls = if (crawlImages) {
                        crawlerService.extractImages(document)
                    } else {
                        emptyList()
                    }
                    
                    // 创建文本内容项
                    val textItems = contentSections.map { section ->
                        CrawledItem(
                            title = section.title,
                            content = section.content,
                            sourceUrl = finalUrl,
                            imageUrls = emptyList() // 文本项不包含图片
                        )
                    }
                    
                    val resultItems = mutableListOf<CrawledItem>()
                    
                    // 添加文本内容
                    resultItems.addAll(textItems)
                    
                    // 如果有图片，添加一个专门的图片项
                    if (imageUrls.isNotEmpty()) {
                        resultItems.add(
                            CrawledItem(
                                title = "爬取到的图片 (${imageUrls.size}张)",
                                content = "网页中包含 ${imageUrls.size} 张图片",
                                sourceUrl = finalUrl,
                                imageUrls = imageUrls,
                                category = "images" // 使用category标记这是图片项
                            )
                        )
                    }
                    
                    // 如果没有找到任何内容，回退方案
                    if (resultItems.isEmpty()) {
                        val pageTitle = document.title() ?: "未知标题"
                        val pageContent = if (crawlContent) {
                            crawlerService.extractArticleContent(document)
                        } else {
                            "未爬取正文内容"
                        }
                        
                        crawledItems = listOf(CrawledItem(
                            title = pageTitle,
                            content = pageContent,
                            sourceUrl = finalUrl,
                            imageUrls = imageUrls
                        ))
                    } else {
                        crawledItems = resultItems
                    }
                } else {
                    errorMessage = "无法爬取页面，请检查URL是否正确"
                }
            } catch (e: Exception) {
                errorMessage = "爬取过程出错: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 添加测试方法
    fun testCrawling() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // 使用百度作为测试
                val testUrl = "https://www.baidu.com"
                val document = crawlerService.crawlPage(testUrl)
                
                if (document != null) {
                    // 尝试提取多种内容
                    val testItems = mutableListOf<CrawledItem>()
                    
                    // 提取页面标题
                    val pageTitle = document.title()
                    testItems.add(CrawledItem(
                        title = "页面标题",
                        content = pageTitle ?: "无标题",
                        sourceUrl = testUrl
                    ))
                    
                    // 提取图片
                    val imageUrls = crawlerService.extractImages(document)
                    if (imageUrls.isNotEmpty()) {
                        testItems.add(CrawledItem(
                            title = "找到的图片",
                            content = "共爬取到 ${imageUrls.size} 张图片",
                            sourceUrl = testUrl,
                            imageUrls = imageUrls
                        ))
                    }
                    
                    // 提取页面内容
                    val pageContent = crawlerService.extractArticleContent(document)
                    if (pageContent.length > 50) {
                        testItems.add(CrawledItem(
                            title = "页面内容",
                            content = pageContent,
                            sourceUrl = testUrl
                        ))
                    }
                    
                    // 提取搜索框和热门搜索
                    val searchInputs = document.select("input[type=text]").map { it.attr("name") }
                    val hotSearches = document.select(".s-hotsearch-content .hotsearch-item").map { it.text() }
                    
                    if (hotSearches.isNotEmpty()) {
                        testItems.add(CrawledItem(
                            title = "百度热搜",
                            content = hotSearches.joinToString("\n"),
                            sourceUrl = testUrl
                        ))
                    }
                    
                    // 添加链接
                    val links = document.select("a[href]").take(10)
                        .map { "${it.text()} -> ${it.attr("href")}" }
                        .filter { it.isNotEmpty() && it != " -> " }
                    
                    if (links.isNotEmpty()) {
                        testItems.add(CrawledItem(
                            title = "找到的链接",
                            content = links.joinToString("\n"),
                            sourceUrl = testUrl
                        ))
                    }
                    
                    crawledItems = testItems
                } else {
                    errorMessage = "无法连接到百度，请检查网络连接"
                }
            } catch (e: Exception) {
                errorMessage = "测试爬取过程出错: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}