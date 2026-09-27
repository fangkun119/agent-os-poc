# AgentOS

## 仓库地图

- .specify/memory/constitution.md 宪章；`specs/<feature>/` 存 spec·plan·tasks
- draft/ 过程与评审产物（仓库现状，不进构建）
- website/ 官网（VitePress 双语站点；README 顶部 logo 取自 website/public/logo.svg）
- docs/tutorial/ 教程（叙事文档，非规格）

## 构建验证与 LSP 边界

- Java 源码有两条编译路径：`./mvnw`（javac）和 jdtls（Eclipse JDT 语言服务器，编辑器/LSP
  智能的来源；导入 Maven 工程时自动生成 Eclipse 工程文件，已 gitignore）。两者编译产物写
  同一个 target/classes。
- 症状识别：代码确认已改、`./mvnw test` 仍是旧行为，或 LSP 提示与新代码不一致——先怀疑
  target/classes 留有陈旧 .class 或 jdtls 索引滞后，再排查代码本身。
- 处置顺序：仓库根 `./mvnw clean` 后重跑；编辑器侧仍异常，再清 jdtls 工作区（VS Code 命令
  面板执行 "Java: Clean Java Language Server Workspace"）。
- 判定口径：代码正确性只认 `./mvnw compile` / `./mvnw test` 结果；LSP 查询（跳转、找引用）
  只作导航参考，刚写入的符号查不到时以 Grep 磁盘结果为准。

## 工具使用指南

- 搜索和网页访问、爬取，只有提示词中显式指定时才使用 firecrawl ，以避免消耗 firecrawl 数量有限的 credits
- git 写操作不自动执行（add / commit / restore / checkout / reset / stash 等一切改变仓库或工作区状态的命令），除非用户指令显式要求；读操作（status / diff / log 等只读查询）可自行执行——用户对 git 写操作有自己的编排（部分暂存、分批提交等）

## 沟通要点

- 在聊天框中回复时：采用清晰的、短句为主的表达方式；要清楚，用户并没有深入参与你所执行的具体操作，对细节不像你那样清楚；你的介绍必须清晰易懂；特别是一些描述某些处理步骤或指代某些设计的名词，要使用容易理解的词语（或者括号简短的备注在后面）以便对方能懂。
- 写面向 AI 的规则文档（CLAUDE.md、AGENTS.md、skill 文件、.agentos/ 下的规则文件等），
  以及新撰或更新 docs/design/ 下的文档时：落盘前按"零背景读者"自检
  一遍——零背景读者指没有本次会话记录、只能读到文档本身的读者。检验标准：这份文档不靠
  任何会话记忆，就要能被读懂并照做。查三处：
  ① 关键信息只写在读者一定能看到的正文里，不放只有作者会话里才有的地方；
  ② 生僻术语首次出现带白话解释，指代一律实名（写文件名、章节号、参数名，禁写"上述/该/它"）；
  ③ 写明内容所依据的环境与前提（哪个进程、哪个 PATH、哪个版本号、哪个日期的实测结论）
- 仓库长期文档（CLAUDE.md、docs/、spike/ 下的一切）中不得写指向临时草稿目录（chat/draft/、
  chat/temp/、chat/consolidate/ 等）的引用指针——这类目录内容随时可删，指向它的指针必然悬空。
  要引用其中的结论时，替换成能独立成文的文字说明：写清结论本身、依据（文档章节号、spike
  编号、决议编号）与日期，让读者不依赖草稿文件即可读懂。
- **跨文件引用**（引用其他文档的章节）统一 "<文档名> - <标题原文>" 形态：<标题原文> 涵盖标题编号 + 标题文字
  （例：TechnicalSolution.md - 4.3 关键设计点）；文档改名、标题改名、标题序号更改后，
  全库 grep 旧名同步更正，防止死链。本条对全部仓库文档生效（含代码注释）
- **文件内引用**（引用本文档的章节）统一 "<标题原文>" 形态（跨文件形态去掉文件名与分隔符）：
  编号 + 标题文字（例：4.2 模块组成、第 13 章验收标准）；api.md 的 § 编号保留 § 前缀（例：§4 失败路径契约）。
  "第 N 章"式编号后直接接标题文字、不加空格。同名编号在其他文档也存在且上下文不足以绑定本文档时，
  升级用跨文件形态消歧；标题文字与句中既有词重叠时，可用「」包裹标题文字消歧（例：第 13 章「验收标准」）；标题文字取语义主干——丢弃括号限定词、保留冒号后的说明（例：11.2 核心阶段：一个目录定义一个 Agent，不写「（文件系统）」）
- **表格行定位**：文件内引用模块表某一行，用「10. 项目工程结构（X 行）」形态；紧邻表格处的「见 X 行」简写合法。
- **术语用标准专业词汇**（禁口语缩写）：框架写 `Spring Boot` 不写 `boot`，指模块时用模块全称（如 `agentos-boot`）；契约写 `interface`/`接口` 不写 `port`/`端口`（网络端口本义如默认 `8080` 端口除外）。本条对全部仓库文档生效（含代码注释），存量违规随触碰修正。

---

本文件现只保留与 `docs/design/` 无关的通用规则（仓库导航、构建验证、工具使用、沟通与写作纪律）。
设计决策类内容不在本文件维护，一律以 `docs/design/` 下的权威文档为准——文档清单、各文档定位与
冲突仲裁见 `docs/design/AGENTS.md`。

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
