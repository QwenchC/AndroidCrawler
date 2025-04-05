# Android Crawler - 网络抓取与AI分析工具

![版本](https://img.shields.io/badge/版本-1.0-blue)
![Android SDK](https://img.shields.io/badge/Android%20SDK-35-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-orange)
![License](https://img.shields.io/badge/许可-MIT-lightgrey)

Android Crawler 是一款功能强大的移动端网络内容抓取与分析应用，专为学习和研究目的设计。它允许用户提取、分析和存储来自互联网的公开内容，同时集成了多搜索引擎支持和AI分析能力,文本生成api来源于https://github.com/pollinations/pollinations。

## 📱 功能特点

### 🕸️ 网页爬虫
- **多类型内容提取**：支持抓取网页标题、段落文本、图片等内容
- **智能内容分析**：自动识别并提取网页主要内容区域
- **特定网站优化**：针对常见网站（如知乎、CSDN等）提供特别的内容提取规则
- **重定向链接处理**：智能处理各种搜索引擎的重定向链接
- **Robots.txt 合规性**：尊重并遵守网站爬虫规则

### 🔍 多搜索引擎支持
- **搜索引擎切换**：支持多种搜索引擎（DuckDuckGo、搜狗）
- **搜索结果处理**：能够正确解析搜索引擎重定向链接
- **简洁用户界面**：直观的搜索结果展示

### 🤖 AI联网搜索
- **基于搜索结果的AI回答**：整合搜索结果，生成连贯的AI回答
- **内容聚合**：从多个来源提取信息，提供全面的回答
- **实时流式响应**：采用流式输出，提高用户体验

### 📊 数据处理和展示
- **结构化内容展示**：将爬取的内容以清晰的方式呈现
- **图片预览**：支持图片显示和链接复制
- **文本内容复制**：方便用户复制和分享爬取的文本内容

## 🛠️ 技术栈

- **开发语言**：Kotlin
- **UI框架**：Jetpack Compose
- **网络请求**：OkHttp, Retrofit
- **HTML解析**：Jsoup
- **图片加载**：Coil
- **架构模式**：MVVM (使用ViewModel和Compose状态管理)
- **并发处理**：Kotlin Coroutines

## 📲 安装使用

### 系统要求
- Android 8.0 (API 24) 或更高版本
- 网络连接

### 安装方法
1. 从Release页面下载最新的APK文件
2. 在Android设备上启用"未知来源"应用安装
3. 安装并运行应用

## 🚀 核心功能使用说明

### 网页爬虫功能
1. 在主界面点击"网页爬虫"选项卡
2. 在输入框中输入要爬取的URL
3. 选择需要爬取的内容类型（标题、内容、图片）
4. 点击"开始爬取"按钮
5. 查看爬取结果并可选择复制内容

### 搜索功能
1. 点击"AI联网搜索"选项卡
2. 在下拉菜单中选择搜索引擎
3. 在搜索框中输入查询内容
4. 查看搜索结果列表
5. 点击任一结果可跳转到爬虫界面进行内容提取

### AI联网搜索
1. 在"AI联网搜索"界面进行搜索
2. 选择感兴趣的搜索结果
3. 点击"生成AI回答"按钮
4. 等待AI基于搜索结果生成连贯的回答
5. 可复制AI回答内容

## 📝 代码结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/sqq/androidcrawler/
│   │   │   ├── crawler/             # 爬虫核心功能
│   │   │   │   ├── WebCrawlerService.kt
│   │   │   │   └── CrawlerConfig.kt
│   │   │   ├── data/                # 数据模型
│   │   │   │   └── CrawledItem.kt
│   │   │   ├── search/              # 搜索功能
│   │   │   │   ├── AIService.kt
│   │   │   │   ├── SearchService.kt
│   │   │   │   ├── SearchEngineProvider.kt
│   │   │   │   ├── DuckDuckGoSearchEngine.kt
│   │   │   │   └── SogouSearchEngine.kt
│   │   │   ├── screen/              # UI界面
│   │   │   │   └── AISearchScreen.kt
│   │   │   ├── utils/               # 工具类
│   │   │   │   ├── UrlUtils.kt
│   │   │   │   ├── ComplianceChecker.kt
│   │   │   │   └── ClipboardUtil.kt
│   │   │   ├── viewmodel/           # 视图模型
│   │   │   │   ├── CrawlerViewModel.kt
│   │   │   │   └── AISearchViewModel.kt
│   │   │   └── MainActivity.kt      # 主活动
│   │   ├── res/                     # 资源文件
│   │   └── AndroidManifest.xml      # 应用清单
│   └── test/                        # 测试代码
└── build.gradle.kts                 # 构建配置
```

## 🔒 隐私与合规性

Android Crawler 严格遵守以下原则：
- 仅爬取公开数据，尊重并遵循网站的 robots.txt 协议
- 所有爬取的数据仅用于个人研究和学习用途
- 尊重网站服务器带宽，控制请求频率
- 不收集任何个人敏感信息
- 符合《网络安全法》和《数据安全法》的相关规定

## 🔄 更新计划

- [ ] 添加更多搜索引擎支持（百度、必应等）
- [ ] 实现爬取历史记录功能
- [ ] 增加数据导出功能（PDF、Markdown等格式）
- [ ] 优化内容提取算法，提高准确性
- [ ] 加入更多网站的专门优化规则
- [ ] 添加内容分类和标签功能

## 🤝 贡献指南

欢迎为项目做出贡献！您可以通过以下方式参与：
1. 报告错误或提出功能建议
2. 提交代码改进
3. 改进文档
4. 分享使用经验

## 📃 许可证

本项目采用 MIT 许可证。详情请参阅 LICENSE 文件。

## ⚠️ 免责声明

本应用仅供学习和研究使用，请合理使用并遵守相关法律法规。使用者应当：
- 遵守网站的使用条款和robots.txt规则
- 尊重内容创作者的版权
- 不使用本工具进行任何违法或侵权活动

开发者不对使用本工具产生的任何后果负责。

---

**Android Crawler** - 用于学习和研究目的的移动端网络内容提取工具，让信息获取更便捷，让学习更高效。
