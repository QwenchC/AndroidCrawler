package com.sqq.androidcrawler.search

import android.util.Log
import com.sqq.androidcrawler.crawler.WebCrawlerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 内容聚合服务 - 从多个来源爬取内容
 */
class ContentAggregatorService(private val crawlerService: WebCrawlerService) {
    
    /**
     * 聚合多个搜索结果的内容
     */
    suspend fun aggregateContent(searchResults: List<SearchResult>, maxResults: Int = 5): String {
        return withContext(Dispatchers.IO) {
            val contentBuilder = StringBuilder()
            
            // 增加结果处理数量
            val processedUrls = mutableSetOf<String>() // 避免处理重复URL
            var successCount = 0
            
            // 先添加一个简短的摘要指示这是关于什么问题的搜索结果
            contentBuilder.append("以下是关于搜索结果的摘要，请基于这些信息回答问题：\n\n")
            
            for (result in searchResults) {
                if (processedUrls.contains(result.url) || successCount >= maxResults) {
                    continue
                }
                
                try {
                    Log.d("ContentAggregator", "开始爬取URL: ${result.url}")
                    val document = crawlerService.crawlPage(result.url)
                    
                    if (document != null) {
                        // 添加标题和链接
                        contentBuilder.append("## 来源: ${result.title}\n")
                        contentBuilder.append("URL: ${result.url}\n\n")
                        
                        // 使用现有摘要作为快速预览
                        if (result.snippet.isNotEmpty()) {
                            contentBuilder.append("摘要: ${result.snippet}\n\n")
                        }
                        
                        // 提取结构化内容
                        val contentSections = crawlerService.extractStructuredContent(document)
                        
                        if (contentSections.isNotEmpty()) {
                            for (section in contentSections.take(3)) {
                                contentBuilder.append("### ${section.title}\n")
                                // 增加内容截取长度
                                contentBuilder.append("${section.content.take(2500)}\n\n")
                            }
                        } else {
                            // 如果没有结构化内容，使用一般方法提取
                            val content = crawlerService.extractArticleContent(document)
                            // 增加内容截取长度
                            contentBuilder.append("${content.take(3000)}\n\n")
                        }
                        
                        contentBuilder.append("---\n\n")
                        processedUrls.add(result.url)
                        successCount++
                    }
                } catch (e: Exception) {
                    Log.e("ContentAggregator", "处理URL时出错 ${result.url}: ${e.message}")
                }
            }
            
            if (contentBuilder.length > 100000) {
                // 如果内容总量超过10万字符，进行适当截断
                Log.w("ContentAggregator", "内容过长(${contentBuilder.length}字符)，进行截断")
                return@withContext contentBuilder.substring(0, 100000) + "\n\n[内容过多，已截断]"
            }
            
            if (contentBuilder.length <= 100) {
                return@withContext "未能提取到相关内容。请尝试其他关键词。"
            }
            
            // 添加最终指示
            contentBuilder.append("\n请基于以上搜索结果回答用户问题，如果搜索结果中包含确切信息，请直接提供；如果没有确切答案，请说明信息不足。")
            
            contentBuilder.toString()
        }
    }
}