package com.sqq.androidcrawler

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.sqq.androidcrawler.utils.copyTextToClipboard
import com.sqq.androidcrawler.utils.UrlUtils
import com.sqq.androidcrawler.viewmodel.CrawlerViewModel
import com.sqq.androidcrawler.screen.AISearchScreen
import com.sqq.androidcrawler.search.SearchEngineProvider
import java.text.SimpleDateFormat
import java.util.Locale

// 创建一个CompositionLocal用于在整个应用中共享数据
val LocalAppState = staticCompositionLocalOf { AppState() }

// 应用状态类，包含屏幕切换和URL共享功能
class AppState {
    val currentScreen = mutableStateOf(Screen.WebCrawler)
    val crawlerUrl = mutableStateOf("https://example.com")
    
    fun navigateToCrawler(url: String) {
        // 使用已经解析的URL
        val cleanUrl = UrlUtils.extractRealUrl(url) // 再次确保URL被正确解析
        crawlerUrl.value = cleanUrl
        currentScreen.value = Screen.WebCrawler
        
        // 添加日志以便调试
        Log.d("AppState", "导航到网页爬虫，URL: $cleanUrl")
    }
}

class MainActivity : ComponentActivity() {
    private val appState = AppState()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化搜索引擎提供者
        SearchEngineProvider.init(applicationContext)
        
        setContent {
            MaterialTheme {
                // 提供AppState给整个应用
                androidx.compose.runtime.CompositionLocalProvider(LocalAppState provides appState) {
                    AndroidCrawlerApp()
                }
            }
        }
        
        // 设置状态栏和导航栏为透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.insetsController?.let { controller ->
                // 设置状态栏图标为深色（在浅色主题上）
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                
                // 设置导航栏图标为深色（在浅色主题上）
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0-10 (API 26-29)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or 
                                                 View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-7.1 (API 23-25) - 只支持状态栏图标颜色设置
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

enum class Screen {
    WebCrawler,
    AISearch
}

@Composable
fun AndroidCrawlerApp() {
    // 获取应用状态
    val appState = LocalAppState.current
    val currentScreen by appState.currentScreen
    
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.WebCrawler,
                    onClick = { appState.currentScreen.value = Screen.WebCrawler },
                    icon = { Icon(Icons.Default.Language, contentDescription = "网页爬虫") },
                    label = { Text("网页爬虫") }
                )
                
                NavigationBarItem(
                    selected = currentScreen == Screen.AISearch,
                    onClick = { appState.currentScreen.value = Screen.AISearch },
                    icon = { Icon(Icons.Default.Search, contentDescription = "AI搜索") },
                    label = { Text("AI搜索") }
                )
            }
        }
    ) { padding ->
        when (currentScreen) {
            Screen.WebCrawler -> CrawlerScreen(
                Modifier.padding(padding),
                initialUrl = appState.crawlerUrl.value
            )
            Screen.AISearch -> AISearchScreen(Modifier.padding(padding))
        }
    }
}

@Composable
fun CrawlerScreen(
    modifier: Modifier = Modifier,
    viewModel: CrawlerViewModel = viewModel(),
    initialUrl: String = "https://example.com"
) {
    var urlInput by remember { mutableStateOf(initialUrl) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 免责声明
        DisclaimerSection()
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 将URL输入区和爬取按钮并排放置
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            // URL输入框
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("网站URL") },
                modifier = Modifier.weight(1f) // 让输入框占据大部分空间
            )
            
            Spacer(modifier = Modifier.width(8.dp)) // 在输入框和按钮之间添加间距
            
            // 爬取按钮
            Button(
                onClick = { 
                    if (urlInput.isNotEmpty()) {
                        viewModel.startCrawling(urlInput)
                    }
                }
            ) {
                Text("开始爬取")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        
        // 爬取选项
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题选项
                Row(
                    verticalAlignment = CenterVertically
                ) {
                    Text("标题", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(2.dp))
                    Switch(
                        checked = viewModel.crawlTitles,
                        onCheckedChange = { viewModel.toggleCrawlTitles() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
                
                // 正文选项
                Row(
                    verticalAlignment = CenterVertically
                ) {
                    Text("正文", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(2.dp))
                    Switch(
                        checked = viewModel.crawlContent,
                        onCheckedChange = { viewModel.toggleCrawlContent() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
                
                // 图片选项
                Row(
                    verticalAlignment = CenterVertically
                ) {
                    Text("图片", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(2.dp))
                    Switch(
                        checked = viewModel.crawlImages,
                        onCheckedChange = { viewModel.toggleCrawlImages() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // 状态指示
        if (viewModel.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 添加错误信息显示
        viewModel.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // 显示结果
        LazyColumn {
            items(viewModel.crawledItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 标题行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.title, 
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(item.crawlDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 根据不同类型显示不同内容
                        if (item.category == "images") {
                            // 图片展示部分
                            if (item.imageUrls.isNotEmpty()) {
                                // 使用LazyRow水平滚动显示图片
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(item.imageUrls.take(10)) { imageUrl ->
                                        // 使用SubcomposeAsyncImage加载网络图片
                                        SubcomposeAsyncImage(
                                            model = imageUrl,
                                            contentDescription = "爬取的图片",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .height(160.dp)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp)),
                                            loading = {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                            },
                                            error = {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text(
                                                        text = "加载失败",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                                
                                // 图片链接部分
                                Spacer(modifier = Modifier.height(8.dp))
                                val context = LocalContext.current
                                TextButton(
                                    onClick = {
                                        copyTextToClipboard(
                                            context = context,
                                            text = item.imageUrls.joinToString("\n"),
                                            label = "图片链接"
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "复制图片链接",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("复制所有图片链接")
                                }
                            }
                        } else {
                            // 文本内容展示部分
                            SelectionContainer {
                                Text(
                                    text = item.content, 
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 显示来源URL
                        Text(
                            text = "来源: ${item.sourceUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        // 底部操作栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val context = LocalContext.current  // 先获取 context

                            TextButton(
                                onClick = {
                                    // 使用已获取的 context 变量
                                    copyTextToClipboard(
                                        context = context,
                                        text = item.getFormattedContent(),
                                        label = "爬取内容"
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "复制完整内容",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复制完整内容")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisclaimerSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = CenterVertically
        ) {
            Text(
                "免责声明:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "仅爬取公开数据，遵守robots.txt协议，用于学习研究。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}