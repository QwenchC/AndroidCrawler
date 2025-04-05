package com.sqq.androidcrawler.crawler

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class WebCrawlerService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    // 添加User-Agent模拟浏览器行为
    private val userAgent = "Mozilla/5.0 (Android) WebCrawler/1.0 (Legal Research Bot; respects robots.txt)"
    
    /**
     * 检查robots.txt并遵循其规则
     */
    suspend fun checkRobotsTxt(baseUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val robotsUrl = "$baseUrl/robots.txt"
                val request = Request.Builder()
                    .url(robotsUrl)
                    .header("User-Agent", userAgent)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext true // 如果无法获取robots.txt，假设允许爬取
                    
                    val robotsTxt = response.body?.string() ?: return@withContext true
                    // 简单解析robots.txt
                    // 实际应用中应使用专门的解析器如robotstxt-parser库
                    !robotsTxt.contains("Disallow: /")
                }
            } catch (e: IOException) {
                true // 发生错误时假设允许爬取
            }
        }
    }
    
    /**
     * 获取网页并解析HTML
     */
    suspend fun crawlPage(url: String): Document? {
        return withContext(Dispatchers.IO) {
            try {
                // 添加日志
                Log.d("WebCrawler", "开始爬取URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    // 添加响应日志
                    Log.d("WebCrawler", "收到响应: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        Log.e("WebCrawler", "请求失败: ${response.code}")
                        return@withContext null
                    }
                    
                    val html = response.body?.string() ?: return@withContext null
                    Log.d("WebCrawler", "HTML长度: ${html.length}")
                    Jsoup.parse(html, url)
                }
            } catch (e: IOException) {
                Log.e("WebCrawler", "爬取异常", e)
                null
            }
        }
    }
    
    /**
     * 解析特定内容(例如提取新闻标题)
     */
    fun extractNewsHeadlines(document: Document): List<String> {
        // 扩展选择器匹配更多可能的标题元素
        val headlineElements = document.select("h1, h2, h3, .headline, .title, .article-title, .post-title, article h1")
        val headlines = headlineElements.map { it.text().trim() }
            .filter { it.isNotEmpty() }
        
        // 添加日志
        Log.d("WebCrawler", "找到 ${headlines.size} 个标题")
        if (headlines.isEmpty()) {
            // 如果没找到标题，尝试获取页面中的所有文本内容作为备选
            val bodyText = document.body()?.text()?.take(500)
            Log.d("WebCrawler", "页面内容预览: $bodyText")
        }
        
        return headlines
    }
    
    /**
     * 提取文章内容
     */
    fun extractArticleContent(document: Document, headlineText: String? = null): String {
        // 1. 尝试找到与标题相关联的内容区域
        if (headlineText != null) {
            val headlineElement = document.select("h1, h2, h3").firstOrNull { it.text().contains(headlineText) }
            
            headlineElement?.let { element ->
                // 尝试找到标题后的内容
                val nextElements = mutableListOf<org.jsoup.nodes.Element>()
                var currentElement = element.nextElementSibling()
                
                while (currentElement != null && currentElement.tagName() != "h1" && currentElement.tagName() != "h2") {
                    if (currentElement.tagName() == "p" || currentElement.tagName() == "div") {
                        nextElements.add(currentElement)
                    }
                    currentElement = currentElement.nextElementSibling()
                }
                
                if (nextElements.isNotEmpty()) {
                    val content = nextElements.joinToString("\n\n") { it.text() }
                    if (content.length > 50) {
                        return content
                    }
                }
            }
        }
        // 2. 尝试通过常见内容选择器提取
        val contentSelectors = listOf(
            "article", ".article-content", ".post-content", ".entry-content", 
            ".content", "#content", ".main-content", "article p", ".story-body",
            ".news-content", "[itemprop=articleBody]", ".article-body"
        )
        
        for (selector in contentSelectors) {
            val contentElements = document.select(selector)
            if (contentElements.isNotEmpty()) {
                // 优先使用p标签提取段落
                val paragraphs = contentElements.select("p")
                if (paragraphs.size > 1) {
                    val content = paragraphs.joinToString("\n\n") { it.text() }
                    if (content.length > 50) {
                        Log.d("WebCrawler", "使用选择器 '$selector' 提取到内容，长度: ${content.length}")
                        return content
                    }
                } else {
                    // 如果没有多个p标签，直接使用元素文本
                    val content = contentElements.text()
                    if (content.length > 50) {
                        Log.d("WebCrawler", "使用选择器 '$selector' 提取到内容，长度: ${content.length}")
                        return content
                    }
                }
            }
        }
        
        // 3. 后备方案：尝试提取所有p标签内容
        val allParagraphs = document.select("p")
        if (allParagraphs.size > 1) {
            val content = allParagraphs.toList()  // 先转换为Kotlin List
                .filter { it.text().length > 20 }
                .joinToString("\n\n") { it.text() }
            if (content.length > 50) {
                Log.d("WebCrawler", "从所有p标签提取到内容，长度: ${content.length}")
                return content
            }
        }
        
        // 4. 最后的后备：获取body的前500个字符作为摘要
        val bodyText = document.body()?.text()?.take(500)
        Log.d("WebCrawler", "无法找到具体内容，返回body摘要")
        return bodyText ?: "无法提取内容"
    }
    
    /**
     * 增强标题-内容匹配的算法
     */
    private fun findContentForHeadline(document: Document, headline: String): String {
        // 策略1: 直接使用标题查找元素
        val headlineElement = document.select("h1, h2, h3").firstOrNull { 
            it.text().trim().equals(headline, ignoreCase = true) 
        }
        
        // 策略2: 使用部分文本匹配
        val partialMatchElement = if (headlineElement == null && headline.length > 10) {
            document.select("h1, h2, h3").firstOrNull { 
                it.text().contains(headline.substring(0, 10), ignoreCase = true)
            }
        } else null
        
        // 使用找到的元素提取内容
        val targetElement = headlineElement ?: partialMatchElement
        if (targetElement != null) {
            // 提取标题后的内容
            val nextElements = mutableListOf<org.jsoup.nodes.Element>()
            var currentElement = targetElement.nextElementSibling()
            
            // 获取直到下一个标题的所有内容
            while (currentElement != null && !currentElement.tagName().matches(Regex("h[1-3]"))) {
                if (currentElement.tagName() == "p" || currentElement.tagName() == "div") {
                    nextElements.add(currentElement)
                }
                currentElement = currentElement.nextElementSibling()
            }
            
            if (nextElements.isNotEmpty()) {
                val content = nextElements.joinToString("\n\n") { it.text() }
                if (content.length > 30) {
                    return content
                }
            }
        }
        
        // 后备策略: 返回一个通用的内容提取结果
        return extractGeneralContent(document)
    }
    
    /**
     * 提取网页的通用内容，作为标题内容匹配的后备策略
     */
    private fun extractGeneralContent(document: Document): String {
        // 首先尝试常见内容区域
        val contentSelectors = listOf(
            "article", ".article-content", ".post-content", ".entry-content", 
            ".content", "#content", ".main-content", "article p", ".story-body"
        )
        
        for (selector in contentSelectors) {
            val contentElements = document.select(selector)
            if (contentElements.isNotEmpty()) {
                // 获取段落文本
                val paragraphs = contentElements.select("p")
                if (paragraphs.size > 0) {
                    val content = paragraphs.joinToString("\n\n") { it.text() }
                    if (content.length > 50) {
                        return content
                    }
                }
                
                // 如果没有段落，返回整个内容区域的文本
                val text = contentElements.text()
                if (text.length > 50) {
                    return text
                }
            }
        }
        
        // 如果没有找到任何内容区域，提取所有有意义的段落
        val allParagraphs = document.select("p").toList()
            .filter { it.text().length > 30 } // 过滤掉太短的段落
        
        if (allParagraphs.isNotEmpty()) {
            return allParagraphs.joinToString("\n\n") { it.text() }
        }
        
        // 最后的后备方案：返回body的前500个字符
        return document.body()?.text()?.take(500) ?: "无法提取内容"
    }
    
    /**
     * 提取特定网站内容
     */
    private fun extractSpecificSiteContent(document: Document, url: String): String? {
        val domain = extractDomain(url)
        
        return when {
            domain.contains("zhihu.com") -> document.select(".RichText").text()
            domain.contains("jianshu.com") -> document.select(".article .show-content").text()
            domain.contains("csdn.net") -> document.select("#article_content").text()
            domain.contains("cnblogs.com") -> document.select("#cnblogs_post_body").text()
            domain.contains("news.sina.com.cn") -> document.select(".article-content").text()
            domain.contains("weixin.qq.com") -> document.select("#js_content").text()
            else -> null
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 提取图片
     */
    fun extractImages(document: Document): List<String> {
        // 记录已添加的图片URL，避免重复
        val uniqueUrls = mutableSetOf<String>()
        
        // 查找所有img标签
        val images = document.select("img[src]")
            .mapNotNull { img -> 
                try {
                    // 尝试获取原图属性
                    val srcAttrs = listOf("data-original", "data-src", "data-lazy-src", "data-original-src", "src")
                    
                    // 尝试多种可能的属性名称
                    var imageUrl: String? = null
                    for (attr in srcAttrs) {
                        val url = img.attr(attr)
                        if (url.isNotEmpty() && !url.contains("data:image")) {
                            imageUrl = url
                            break
                        }
                    }
                    
                    // 如果找到URL，转换为绝对URL
                    if (imageUrl != null && imageUrl.isNotEmpty()) {
                        if (imageUrl.startsWith("http")) imageUrl else img.absUrl(srcAttrs.first { img.hasAttr(it) })
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("WebCrawler", "处理图片URL时出错", e)
                    null
                }
            }
            .filter { url ->
                // 过滤掉数据URI和小图标
                url.isNotEmpty() && 
                !url.startsWith("data:") &&
                !url.contains("icon", ignoreCase = true) && // 过滤图标
                !url.contains("logo", ignoreCase = true) && // 过滤Logo
                !uniqueUrls.contains(url) && // 去重
                uniqueUrls.add(url) // 添加到已处理集合
            }
        
        Log.d("WebCrawler", "找到 ${images.size} 张图片")
        return images
    }
    
    /**
     * 查找所有段落
     */
    fun findAllParagraphs(document: Document): List<org.jsoup.nodes.Element> {
        return document.select("p").toList()
    }
    
    /**
     * 查找特定元素
     */
    fun findSpecificElements(document: Document): List<org.jsoup.nodes.Element> {
        return document.select("div, p").filter { element ->
            // 在这里进行自定义过滤逻辑
            element.text().length > 10 && !element.hasAttr("hidden")
        }
    }
    
    /**
     * 提取结构化的标题和内容
     */
    fun extractStructuredContent(document: Document): List<ContentSection> {
        // 所有标题元素
        val headingElements = document.select("h1, h2, h3, h4, h5, h6")
        
        // 结果列表
        val contentSections = mutableListOf<ContentSection>()
        
        // 处理每个标题
        for (i in 0 until headingElements.size) {
            val currentHeading = headingElements[i]
            val headingLevel = getHeadingLevel(currentHeading.tagName())
            val headingText = currentHeading.text().trim()
            
            if (headingText.isEmpty()) continue
            
            // 查找下一个同级或更高级的标题
            var nextHeadingIndex = -1
            for (j in i + 1 until headingElements.size) {
                val nextHeading = headingElements[j]
                val nextLevel = getHeadingLevel(nextHeading.tagName())
                
                if (nextLevel <= headingLevel) {
                    nextHeadingIndex = j
                    break
                }
            }
            
            // 提取当前标题和下一个边界标题之间的内容
            val contentElements = mutableListOf<org.jsoup.nodes.Element>()
            var currentElement = currentHeading.nextElementSibling()
            
            val endElement = if (nextHeadingIndex != -1) headingElements[nextHeadingIndex] else null
            
            while (currentElement != null && currentElement != endElement) {
                // 只收集段落、div和列表，排除嵌套标题
                if ((currentElement.tagName() == "p" || 
                     currentElement.tagName() == "div" || 
                     currentElement.tagName() == "ul" || 
                     currentElement.tagName() == "ol") && 
                    !currentElement.tagName().matches(Regex("h[1-6]"))) {
                    
                    contentElements.add(currentElement)
                }
                currentElement = currentElement.nextElementSibling()
            }
            
            // 将内容合并
            val content = contentElements.joinToString("\n\n") { it.text() }
            
            // 如果内容不为空，添加到结果
            if (content.isNotEmpty()) {
                contentSections.add(ContentSection(
                    title = headingText,
                    content = content,
                    level = headingLevel
                ))
            }
        }
        
        return contentSections
    }

    // 辅助函数：获取标题级别
    private fun getHeadingLevel(tagName: String): Int {
        return when (tagName.toLowerCase()) {
            "h1" -> 1
            "h2" -> 2
            "h3" -> 3
            "h4" -> 4
            "h5" -> 5
            "h6" -> 6
            else -> 0
        }
    }

    // 内容节结构
    data class ContentSection(
        val title: String,
        val content: String,
        val level: Int
    )
}