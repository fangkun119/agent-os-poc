# AgentOS

## 仓库地图

- `agentos/` 九模块 Java 源码；写或改 Java 前读 `agentos/AGENTS.md`。
- `docs/design/` 权威设计文档；IndustryResearch 为背景、不做实施依据；写或改设计文档前读 `docs/design/AGENTS.md`。
- `.specify/memory/constitution.md` 宪章（未填模板、非仲裁源；2026-11-12 核实）；`specs/<feature>/` 存 spec·plan·tasks。
- `draft/` 过程与评审产物（仓库现状，不进构建）；`chat/` 会话工作区（随时可清理）；两者都是临时目录，引用规则见 `沟通要点`。
- `website/` 官网（VitePress 双语站点；README 顶部 logo 取自 `website/public/logo.svg`）；站点规范见 `website/AGENTS.md`。
- `docs/tutorial/` 教程（叙事文档，非规格；教程内引用仓库文件用历史 commit 固定链接）。

## 构建验证与 LSP 边界

Java 源码有两条编译路径，产物都写同一个 target/classes：`./mvnw`（javac）与 jdtls（Eclipse JDT 语言服务器，编辑器/LSP 智能的来源；导入 Maven 工程时自动生成 Eclipse 工程文件，已 gitignore）。

症状识别：代码确认已改、`./mvnw test` 仍是旧行为，或 LSP 提示与新代码不一致——先怀疑 target/classes 留有陈旧 .class 或 jdtls 索引滞后。按症状分流：

- 只有 `./mvnw test` 仍旧行为 → 走 1。
- 只有 LSP 提示与新代码不一致 → 走 2。
- 两者都有 → 1 → 2 → 3。

处置顺序：

1. 构建侧：仓库根 `./mvnw clean` 后重跑 `./mvnw compile` / `./mvnw test`。完成标准：命令退出码 0 且 `./mvnw test` 反映新行为；仍见旧行为 → 3。
2. 编辑器侧：清 jdtls 工作区——VS Code 命令面板执行 "Java: Clean Java Language Server Workspace"。完成标准：LSP 提示与新代码一致；仍不一致 → 3。
3. 排查代码本身。完成标准：修复后 1 与 2 的完成标准同时满足。

判定口径：代码正确性只认 `./mvnw compile` / `./mvnw test` 的结果；LSP 查询（跳转、找引用）只作导航参考，刚写入的符号查不到时以 Grep 磁盘结果为准。

## 工具使用指南

- firecrawl 只在提示词显式指定时使用（网页搜索、访问、爬取）——credits 数量有限。
- git 写操作只在用户指令显式要求时执行（`add / commit / restore / checkout / reset / stash` 等一切改变仓库或工作区状态的命令）；读操作（`status / diff / log` 等只读查询）可自行执行。理由：用户对 git 写操作有自己的编排（部分暂存、分批提交等）。

## 沟通要点

- 在聊天框中回复时：使用 wait-what skill——先给一点上下文，写简化技术表达（短句、常用词、主动语态；ASD-STE100 口径），术语取自 `GLOSSARY.md`（多个术语表时按 `GLOSSARY-MAP.md` 找对应文件）；默认用中文，用户显式要求其他语言时除外。
- 写或改面向 AI 的规则文档（`AGENTS.md`、`CLAUDE.md`、skill 文件、`.agentos/` 下的规则文件等），以及新撰或更新 `docs/design/` 下的文档时：读 writing-for-agents skill 文件，按它的要求写；默认用中文，用户显式要求其他语言时除外。
- 仓库长期文档（各子树 AGENTS.md/CLAUDE.md、docs/、spike/、website/ 等）中不得写指向临时草稿目录（仓库根 draft/、整个 chat/，含 chat/draft/、chat/temp/、chat/consolidate/ 等）的引用指针——这类目录内容随时可删，指向它的指针必然悬空。要引用其中的结论时，替换成能独立成文的文字说明：写清结论本身、依据（文档章节号、spike 编号、决议编号）与日期，让读者不依赖草稿文件即可读懂。
- **引用向标题看齐**：引用里的标题文字以目标文件的标题行为唯一准绳，逐字照录标题文字原文（含编号、括号限定词、冒号后的说明）；引用与标题行不一致时，按标题行改正引用。标题文字本身含反引号时，外层改用双反引号包裹、内层反引号原样保留（例：``6.8 通知推送（`NotifyTools`）``）。本条对全部仓库文档生效（含代码注释）。
- **跨文件引用**（引用其他文档的章节）统一 "<文档名> - `整行标题文字`" 形态：英文反引号包裹整行标题文字，原文有编号时含编号，照抄原文（例：TechnicalSolution.md - `4.3 关键设计点`）；文档改名、标题改名、标题序号更改后，全库 grep 旧名同步更正，防止死链。引用正文部分时，按文件内引用的定位写法接在标题原文后。本条对全部仓库文档生效（含代码注释）；写在反引号（行内代码）中的引用同样适用。
- **文件内引用**（引用本文档的章节）统一用英文反引号包裹整行标题文字，原文有编号时含编号：标题文字照录原文，不删括号限定词、不改冒号后的说明（例：`4.2 模块组成`、`11.2 核心阶段：一个目录定义一个 Agent（文件系统）`）；api.md 的 § 编号保留 § 前缀（例：`§4 失败路径契约（errorCode 权威归属表）`）。引用正文部分时，在标题文字后接定位：有子条目的写子条目编号（例：`4.3 关键设计点` (5)），没有子条目的写段落号（例：`4.3 关键设计点` 第 2 段），引用原句时用「」包住原句（例：`4.3 关键设计点`：「失败的调用与总超时打断前已完成的调用照常落库」）。同名编号在其他文档也存在且上下文不足以绑定本文档时，升级用跨文件形态消歧；写在反引号（行内代码）中的引用同样适用。
- **表格行定位**：文件内引用模块表某一行，用 `10. 项目工程结构`（X 行）形态——标题文字同样用英文反引号包裹、原文有编号时含编号；紧邻表格处的「见 X 行」简写合法；写在反引号（行内代码）中的引用同样适用。
- **术语用标准专业词汇**（禁口语缩写）：框架写 `Spring Boot` 不写 `boot`，指模块时用模块全称（如 `agentos-boot`）；契约写 `interface`/`接口` 不写 `port`/`端口`（网络端口本义如默认 `8080` 端口除外）。本条对全部仓库文档生效（含代码注释），存量违规随触碰修正。

---

本文件只保留与 `docs/design/` 无关的通用规则（仓库导航、构建验证、工具使用、沟通与写作纪律）。设计决策类内容不在本文件维护，一律以 `docs/design/` 下的权威文档为准——文档清单、各文档定位与冲突仲裁见 `docs/design/AGENTS.md`。

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
