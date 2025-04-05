package com.sqq.androidcrawler.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "crawled_items")
data class CrawledItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val sourceUrl: String,
    val crawlDate: Date = Date(),
    val category: String = "",
    val imageUrls: List<String> = emptyList()  // 新增字段：存储图片URL列表
) {
    // 添加此方法，提供格式化的内容
    fun getFormattedContent(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val dateStr = dateFormat.format(crawlDate)
        
        // 如果是图片类别，只返回图片链接
        if (category == "images") {
            return "图片链接:\n${imageUrls.joinToString("\n")}"
        }
        
        // 普通内容不包含图片链接
        return """
            标题: $title
            
            内容: $content
            
            来源: $sourceUrl
            爬取时间: $dateStr
            ${if(category.isNotEmpty() && category != "images") "分类: $category" else ""}
        """.trimIndent()
    }
}