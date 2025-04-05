package com.sqq.androidcrawler.crawler

data class CrawlerConfig(
    val targetUrl: String,
    val crawlDepth: Int = 1,
    val delayBetweenRequests: Long = 1000, // 毫秒
    val respectRobotsTxt: Boolean = true,
    val maxPages: Int = 10,
    val followLinks: Boolean = false,
    val dataSelectors: Map<String, String> = mapOf() // CSS选择器映射
)