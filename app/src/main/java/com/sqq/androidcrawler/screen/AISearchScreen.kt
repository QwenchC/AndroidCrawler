package com.sqq.androidcrawler.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sqq.androidcrawler.LocalAppState
import com.sqq.androidcrawler.utils.copyTextToClipboard
import com.sqq.androidcrawler.viewmodel.AISearchViewModel
import com.sqq.androidcrawler.search.SearchResult
import androidx.compose.material3.CardDefaults
import com.sqq.androidcrawler.utils.UrlUtils
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.setValue
import com.sqq.androidcrawler.search.SearchEngineType

@Composable
fun AISearchScreen(
    modifier: Modifier = Modifier
) {
    // 获取当前Context
    val context = LocalContext.current
    // 创建ViewModel时传入context
    val viewModel: AISearchViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AISearchViewModel(context) as T
            }
        }
    )
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState() 
    val searchResults by viewModel.searchResults.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isGeneratingResponse by viewModel.isGeneratingResponse.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentSearchEngine by viewModel.currentSearchEngine.collectAsState()
    
    // 获取应用状态以便导航
    val appState = LocalAppState.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 搜索区域 - 固定在顶部
        Text(
            text = "AI联网搜索（pollinations.ai）",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 添加搜索引擎选择器 - 置于搜索框的左侧
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 搜索引擎选择下拉菜单
            SearchEngineSelector(
                currentEngine = currentSearchEngine,
                onEngineSelected = { viewModel.switchSearchEngine(it) },
                modifier = Modifier.weight(0.3f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("请输入您的问题") },
                modifier = Modifier.weight(0.7f),
                trailingIcon = {
                    IconButton(onClick = { viewModel.search() }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 搜索状态和错误信息 - 固定在搜索框下方
        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        errorMessage?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        // 搜索结果和AI回答区域 - 使用LazyColumn让两者能一起滚动
        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 搜索结果计数和生成按钮 - 固定在滚动区域上方
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("找到 ${searchResults.size} 个相关结果", 
                    style = MaterialTheme.typography.titleMedium)
                
                Button(
                    onClick = { viewModel.generateAIResponse() },
                    enabled = !isGeneratingResponse && searchResults.isNotEmpty()
                ) {
                    Text("生成AI回答")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 滚动内容区域 - AI回答和搜索结果一起滚动
            LazyColumn(
                modifier = Modifier.weight(1f)  // 使LazyColumn填充剩余空间
            ) {
                // AI回答部分
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "AI回答:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (isGeneratingResponse) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("正在生成回答...")
                                }
                            }
                            
                            // 显示流式回答
                            SelectionContainer {
                                Text(
                                    // 修改这里：如果正在生成回答，则显示空字符串
                                    text = when {
                                        isGeneratingResponse -> ""  // 生成中显示空字符串
                                        aiResponse.isNotBlank() -> aiResponse  // 有回答则显示回答
                                        else -> "请手动点击\"生成AI回答\""  // 无回答且非生成中显示提示
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            // 复制按钮
                            if (aiResponse.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            copyTextToClipboard(
                                                context = context,
                                                text = aiResponse,
                                                label = "AI回答"
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "复制内容",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("复制回答")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 搜索结果标题
                item {
                    Text(
                        text = "搜索结果:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // 搜索结果列表
                items(searchResults) { result ->
                    SearchResultItem(
                        result = result, 
                        onItemClick = { originalUrl ->
                            // 先对URL进行解析，再导航到爬虫界面
                            val realUrl = UrlUtils.extractRealUrl(originalUrl)
                            Log.d("AISearchScreen", "点击搜索结果 - 原始URL: $originalUrl")
                            Log.d("AISearchScreen", "点击搜索结果 - 处理后URL: $realUrl")
                            appState.navigateToCrawler(realUrl)
                        }
                    )
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (!isSearching) {
            // 初始状态提示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请输入问题进行搜索",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(result: SearchResult, onItemClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                // 传递原始URL，让调用方处理
                onItemClick(result.url)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.snippet,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 显示解析后的URL，方便用户查看
            Text(
                text = UrlUtils.extractRealUrl(result.url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchEngineSelector(
    currentEngine: SearchEngineType,
    onEngineSelected: (SearchEngineType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (currentEngine) {
                    SearchEngineType.DUCKDUCKGO -> "DuckDuckGo"
                    SearchEngineType.SOGOU -> "搜狗"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "选择搜索引擎"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.3f)
        ) {
            DropdownMenuItem(
                text = { Text("DuckDuckGo") },
                onClick = { 
                    onEngineSelected(SearchEngineType.DUCKDUCKGO)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = { Text("搜狗") },
                onClick = { 
                    onEngineSelected(SearchEngineType.SOGOU)
                    expanded = false
                }
            )
        }
    }
}