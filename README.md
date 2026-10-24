# CET Focus 四六级学习助手
![Uploading CET-Modern-Pro-Preview.png…]()

主要功能：
1. 错题管理
2. 词汇管理
3. 模拟成绩管理

主窗口顶部会醒目显示用户自己的目标分数。用户可以修改目标，数据会保存在 SQLite 中。

首页会自动汇总今天到期的词汇和错题，以进度环显示今日完成比例，并展示连续学习、今日完成量和近七天掌握率。用户通过卡片完成复习，并选择“不会、模糊或掌握”，系统会自动计算下次复习日期。

AI 学习管家负责资料录入和辅助管理。用户可以一次选择多个 CSV、TXT 或 Markdown 文件，也可以直接粘贴混合资料；AI 会自动区分错题、词汇和模拟成绩，生成统一预览后一次写入数据库。原来的数据页面主要用于查询、核对和少量修正。

软件还提供可选的 AI 助手，内置 DeepSeek、Kimi、通义千问和智谱 GLM 预设，也可连接 OpenAI Responses API 或其他 OpenAI 兼容的 Chat Completions 接口。选择国产模型厂商后会自动填写协议、接口地址、鉴权方式和推荐模型，通常只需输入 API 密钥。所有字段仍可编辑，因此也能适配云端厂商和本地模型服务。AI 密钥不写入数据库，只保存在当前运行内存中。

项目使用 Java Swing 制作界面，使用 JDBC 操作 SQLite 数据库。

## 代码结构

```text
Main.java                 程序入口
Database.java             数据库连接和建表
MainFrame.java            主窗口
Theme.java                界面颜色和控件样式
DashboardPanel.java       学习数据概览与目标进度
TodayPanel.java           今日任务、学习连续天数与反馈
ReviewSessionDialog.java  单词和错题卡片复习
CsvFiles.java             CSV 读取与模板保存
AiConfig.java             多厂商接口配置
AiClient.java             通用大模型 HTTP 客户端
AiSettingsDialog.java     API 设置窗口
AiAssistantPanel.java     AI 智能录入与学习建议
WrongQuestionPanel.java   错题增删改查
VocabularyPanel.java      词汇增删改查
ScorePanel.java           模拟成绩增删改查
```

## 运行

使用 IntelliJ IDEA 打开 `simple-version` 文件夹，等待 Maven 下载依赖，然后运行 `Main.java`。

数据库会自动创建在：

```text
simple-version/data/cet-simple.db
```

## CSV 批量导入

进入错题、词汇或模拟成绩页面，先点击“下载模板”，用 Excel 打开并填写，然后另存为 CSV 文件，再点击“批量导入”。导入使用数据库事务，文件中有错误时不会只写入一部分数据。

## AI 助手

程序启动后进入“今日学习”，可从首页直接输入要求或导入资料。点击“AI 学习管家”中的“模型与密钥设置”，选择厂商预设并填写 API 密钥即可。当前预设如下：

| 厂商 | 默认模型 | 环境变量 |
| --- | --- | --- |
| DeepSeek | `deepseek-flash` | `DEEPSEEK_API_KEY` |
| Kimi | `kimi-k3` | `MOONSHOT_API_KEY` |
| 通义千问 | `qwen-plus` | `DASHSCOPE_API_KEY` |
| 智谱 GLM | `glm-5.3` | `ZHIPUAI_API_KEY` |

也可以选择“自定义兼容接口”手动填写参数。AI 学习管家可以：

1. 自动读取多个 CSV、TXT 或 Markdown 文件。
2. 从混合内容中同时识别错题、词汇和模拟成绩。
3. 显示分类结果和具体数据预览。
4. 经用户确认后使用同一个数据库事务一次写入全部数据。
5. 根据本地统计生成七天复习计划、分析薄弱项或回答学习问题。
6. 通过自然语言修改目标分数，或批量把已有单词标记为“已掌握”。

涉及数据库写入的结果仍需用户确认；删除操作继续在记录页面确认执行。

## 今日复习

程序会自动把从未复习或已经到期的词汇和错题加入今日任务。每次最多抽取 20 项，并优先保持词汇与错题之间的平衡。

- “不会”：降低熟练度，次日再次复习。
- “模糊”：保持当前熟练度，两天后再次复习。
- “掌握”：提高熟练度，并按 1、3、7、14、30、60 天逐步延长间隔。

词汇熟练度达到 3 级后会自动标记为“已掌握”。所有复习结果都会写入 `review_log`，用于计算连续学习天数、今日完成量和近七天掌握率。

## 学习顺序

先读 `Main.java`，再读 `Database.java` 和 `MainFrame.java`，最后分别阅读三个功能页面。
