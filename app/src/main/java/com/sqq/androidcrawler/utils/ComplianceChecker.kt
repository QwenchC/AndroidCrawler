package com.sqq.androidcrawler.utils

import java.net.URL

class ComplianceChecker {
    /**
     * 检查URL是否为公开数据(非登录区域)
     */
    fun isPublicData(url: String): Boolean {
        // 简单实现 - 检查URL是否包含登录相关关键词
        val loginKeywords = listOf("login", "signin", "account", "profile", "dashboard", "member")
        return !loginKeywords.any { url.contains(it, ignoreCase = true) }
    }
    
    /**
     * 检查爬取频率是否合理(避免DoS攻击)
     */
    fun isReasonableRate(requestsPerMinute: Int): Boolean {
        // 一般每分钟请求数不超过20次比较安全
        return requestsPerMinute <= 20
    }
    
    /**
     * 提供免责声明
     */
    fun getDisclaimerText(): String {
        return """
            本应用遵循以下原则:
            1. 仅爬取公开数据，遵循网站robots.txt协议
            2. 爬取数据仅用于个人研究和学习用途
            3. 尊重网站带宽，控制请求频率
            4. 不会收集个人敏感信息
            5. 符合《网络安全法》和《数据安全法》相关规定
        """.trimIndent()
    }
}