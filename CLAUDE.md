# AgentOS

基于 Java 的企业级 Agent OS 运行时内核。本仓库只覆盖核心阶段：交付运行时内核；
多租户 / SSO / 审计查询 / Tool Policy 等治理层属扩展阶段，本仓库不做。

## 仓库地图

- docs/design/ 权威文档：DemandAnalysis（需求 What，验收标准以 DemandAnalysis.md - 13「验收标准」为唯一依据）、
  TechnicalSolution（技术方案 How，模块/接口/数据模型权威定义）、
  AiProgrammingGuide（实施方法）、IndustryResearch（背景，不做实施依据）
- 文档仲裁：实现细节以 TechnicalSolution 为准；需求范围/验收标准以 DemandAnalysis.md - 13 验收标准为准
  （完整表述与"不改标题"等目录约定见 docs/design/CLAUDE.md）
- .specify/memory/constitution.md 宪章；`specs/<feature>/` 存 spec·plan·tasks
- agentos/ 九个 Maven 模块源码目录（聚合 pom 与 mvnw 在仓库根，mvn 命令仍在仓库根执行）
- 运行时工作区 .agentos/（agentos init 生成）：agents/ skills/ memory/（长期记忆根目录，init 只建
  空目录；记忆档按 <agent>/<user>/ 分档、首次 save_memory 时自动创建，见 TechnicalSolution.md - 5.1 模块组成 / 8.1 工作区初始化） logs/
  mcp_servers.yaml agentos.db + 三个 Bootstrap（AGENTS.md/SOUL.md/USER.md）
- draft/ 过程与评审产物（仓库现状，不进构建）
- 快查指针：REST 端点清单见 TechnicalSolution.md - 7.2 核心阶段端点、线上契约见 docs/design/detail/api.md；Provider 变量 schema 与 Spring AI 属性对照见 docs/design/detail/model-config.md；CLI 命令见 TechnicalSolution.md - 8.7 命令行工具
- Spike 结论指针：W1 实现 Provider / ReAct 前，先读 `spike/007-react-loop/README.md`（第一组 D1-D4：parent 保持 3.5.16、依赖坐标清单、手动循环标准路径、Provider 显式映射样例；第二组 D5-D8：MiniMax 走原生 starter、显式构造必传 RetryTemplate、SAA BOM 维持 1.1.2.0、ZHIPU 转正用官方 starter）
- Spike 结论指针：W2 实现 McpClientService 前，先读 `spike/008-mcp/README.md`（D1-D9 实测结论：mcp SDK 0.17.0 随 spring-ai-mcp 引入、stdio-only、starter 不引入、适配走 AgentOSTool 自适配（候选二）、失败可重试不自动重连、子进程最小 env）
- Spike 结论指针：W3 实现 SQLite 持久化前，先读 `spike/009-sqlite/README.md`（D1 update 自动加列成立、列变更只走手工迁移脚本；D2 连接串三 pragma 生效、拼错参数静默忽略；D3 轮原子提交成立 + BUSY 错误面样本；D4 两坑——AUTOINCREMENT 方言不生成、LocalDateTime 亚毫秒截断，条款已落 agentos/CLAUDE.md 第 3 章；D5 FULL 单次提交均值 0.102 ms；D6 版本组合 Boot 3.5.16 / hibernate-community-dialects 6.6.53.Final / sqlite-jdbc 3.49.1.0）
- 阅读注记：TS 中"千级/几千并发"均为按 DemandAnalysis.md - 8.1 性能（100 并发 Session）的 10 倍余量论证，非承诺值

## 非协商原则（.specify/memory/constitution.md 的压缩复述，冲突时以 constitution.md 为准）

1. JDK 21 + Spring Boot 3.x 单体应用，Maven 多模块、单二进制部署（9 模块，源码在 agentos/ 目录下：agentos-core/
   provider/memory/tool/channel-cli/web/storage/cli/boot，模块清单以 TechnicalSolution.md - 10 项目工程结构为准）
2. 五大核心能力优先：核心阶段交付运行时内核，企业级治理层放扩展阶段
3. 自实现 ReAct 循环，不依赖 Spring AI 的 Agent 抽象
4. **Spring AI 只用一半**：只用 Provider 抽象 + 协议转换 + @Tool schema 生成；**禁用自动
   tool 执行**（启用会导致 tool 被调两次——发现 tool 重复执行先查这里）。最易被违反的一条
5. Plugin Tool 三档接入（零代码 AGENT.md 目录+MCP 主推 / 轻代码自写 MCP / 重代码 @Tool Bean）；
   DemandAnalysis.md - 13「验收标准」验收硬项含方式三 @Tool 示例跑通
6. SQLite 关系持久化 + LongTermMemoryStore 接口 + Markdown 默认档（预留 memory.backend 切换）；
   审计表 tool_invocations / llm_calls 核心阶段就写入落库，不是只放日志；进程内不自建向量层
7. 每个 user story 完成后有可验证成果（相邻 US 可合并演示，如 US-1+US-2 合跑 Demo 一）；
   跑通优先于完美

补充红线（AiProgrammingGuide.md - 3.4 /speckit.plan：把技术方案转成实施 plan + TechnicalSolution.md - 7.4 关键设计点 / 8.8 配置与密钥加载）：

- 超时禁硬编码：llm/tool/total 三档分步预算（application.yaml 默认）；Profile settings.timeout 仅 tool/total 两档按 Agent 覆盖，llm 档仅全局（Spring AI 客户端构建时定死，2026-09-17 Q5 裁决从需求移除，见 TechnicalSolution.md - 7.4 关键设计点）
- Memory 经 MemoryService 三层统一门面，不得简化为与 Session 合并
- 敏感凭证经环境变量注入（${ENV_VAR} 占位），不明文写配置；API Key 仓库零落盘、日志与命令行最多 5 位前缀（详见模型接入环境变量）

## 模型接入环境变量（API Key 红线）

- 加载：`source ~/.agent-os-poc/script/agent-os-env.sh [vendor]`（无参=加载全部；必须 source 不能执行）。
  脚本在仓库外、权限 600，是密钥在磁盘上的唯一落点；新增 vendor 在脚本 vendor 注册区加一块配置 + case 加一个分支
- 导出变量与代码读取（application.yaml 只写 ${环境变量名} 占位符，禁止明文）：
  OpenAI 兼容腿：`OPENAI_API_KEY` / `OPENAI_DEFAULT_MODEL` / `OPENAI_MODEL_LIST` ↔ `spring.ai.openai.api-key` / `.chat.options.model`（DEFAULT=缺省模型；LIST=可用模型清单，逗号分隔，仅用于校验与发现——运行时切换模型走每次调用的 options 参数，不走环境变量）。
  ⚠️ `OPENAI_BASE_URL`（值带 `/v1`，OpenAI SDK 惯例）**不要**直接映射给 `spring.ai.openai.base-url`——Spring AI 的 OpenAiApi 会自己追加 `/v1/chat/completions`，直接映射会产生 `/v1/v1` 双写 404。base-url 非敏感：Spring AI 侧直接写 `https://api.minimax.cn`（不带 `/v1`），只有密钥必须走环境变量
  Anthropic 兼容腿：`ANTHROPIC_API_KEY` / `ANTHROPIC_DEFAULT_MODEL` / `ANTHROPIC_MODEL_LIST` ↔ `spring.ai.anthropic.api-key` / `.chat.options.model`。
  ⚠️ `ANTHROPIC_BASE_URL` 同样**不要**直接映射给 `spring.ai.anthropic.base-url`——base-url 非敏感：Spring AI 侧直接写 `https://api.minimax.cn/anthropic`（国内站；属性名生效经 spike E8 实测、国内站路径可达经 007 第二组 V6 实测，2026-09-15 确认），只有密钥必须走环境变量
  MiniMax 原生腿（主用）：`MINIMAX_API_KEY` / `MINIMAX_DEFAULT_MODEL` / `MINIMAX_MODEL_LIST` ↔ `spring.ai.minimax.api-key` / `.chat.options.model`；base-url 写纯主机 `https://api.minimax.cn`、不带路径——客户端自动追加原生路径 `/v1/text/chatcompletion_v2`（007 第二组 D5 实测，与 OpenAI 兼容路径是两套）
- Provider 命名规则：环境变量按 Provider 命名，一个 Provider 一组四元组（`*_API_KEY` / `*_BASE_URL` / `*_DEFAULT_MODEL` / `*_MODEL_LIST`）。现有 Provider：`OPENAI`、`ANTHROPIC`、`MINIMAX`、`ZHIPU` 并列、互不覆盖。接入优先级：有 Spring AI 官方 starter 就用官方 starter（MiniMax→minimax starter、智谱→zhipuai starter）；官方没有的才走 OpenAI 兼容腿显式构造兜底（模式见 spike/007-react-loop/README.md V9/D8）。ZHIPU 已于 2026-09-15 裁决转正、接线用官方 starter，spike V9 手法仅作"无官方 starter 厂商"兜底。`OPENAI_*` / `ANTHROPIC_*` 的取值可整体替换——当前填的是 MiniMax 兼容端点（语义为真 OpenAI/Anthropic 协议腿、待原生账号），有原生账号后直接改脚本注册区的值即可
- 当前只有 MiniMax 账号：OPENAI / ANTHROPIC 两个 Provider 的配置值取自 MiniMax 双协议兼容端点；模型 MiniMax-M2.7（OPENAI Provider）/ MiniMax-M3（ANTHROPIC Provider），均已官方核验支持工具调用
- **密钥红线（目的：防泄密——防止密钥被提交进代码库、或写进日志外漏）**：
  API Key 只能从环境变量读取（未来可能迁移到专门存储秘钥的基础设施）；
  不可以写入代码仓库——代码、配置、各类文本都不可以；
  程序运行时不可以完整或大段打印到日志，也不可以完整写到命令行——最多打印前 5 位前缀用于 debug

## 架构关键事实

- 三种触发源（CLI / Web API / AgentScheduler 定时）汇入同一 AgentService.process，审计与 Session 语义一致（TechnicalSolution.md - 2 整体架构）
- 一个目录 = 一个 Agent：.agentos/agents/<name>/AGENT.md（frontmatter=配置，正文=指令），AgentLoader.deriveProfile() 派生 Profile（TechnicalSolution.md - 11.1 术语：一个目录 = 一个 Agent）
- Agent 目录不是 Tool：AGENT.md 正文经 ContextLoader 注入 system prompt（归 core，不进 ToolRegistry）（TechnicalSolution.md - 6.3 Plugin Tool 方式一）
- Skill = .agentos/skills/ 实体 + Agent 本地 skills/ 软连接绑定；frontmatter 无 skills 字段，绑定只认软连接；每迭代只注入元数据，正文经 read_file 按需读（TechnicalSolution.md - 6.3 Plugin Tool 方式一 / 8.3 上下文加载 / 11.1 术语：一个目录 = 一个 Agent）
- 通知渠道是 SQLite 全局注册表（notify_channels），AGENT.md frontmatter 无 notify_channels 字段（TechnicalSolution.md - 6.8 通知推送 / 8.2 Profile 配置）
- Provider 用 provider name → ChatModel 显式映射，禁止类型扫描 Bean（TechnicalSolution.md - 3.2 Provider 名到 ChatModel 的显式映射）
- ProfileContext 是 ThreadLocal：ReAct 循环全程禁止切换执行线程（禁 @Async / 跨线程 CompletableFuture）（TechnicalSolution.md - 4.2 模块组成）
- PromptBuilder 五部分（TechnicalSolution.md - 4.2 模块组成）中两条结构性约束：system prompt 末尾附当前日期时间（定时场景的"今天"全靠它）；Memory 段只放长期记忆，会话历史由对话历史段独立注入一次
- SQLite：首建走 ddl-auto=update（建新表唯一放行场景；实测亦能自动加列，治理口径 = 列变更只走手工迁移脚本、禁依赖自动加列）；方言需显式引入 hibernate-community-dialects；WAL 在 JDBC 连接层设置（TechnicalSolution.md - 9.2 SQLite 关系型数据）
- 会话消息逐行存 session_messages：轮原子提交——正常完成整轮一次事务、进程崩溃 / 总超时 / LLM 报错 / 轮末提交失败四类异常零提交；审计表 tool_invocations / llm_calls 逐次即时落库、与消息解耦（TechnicalSolution.md - 4.3 关键设计点 / 9.2 SQLite 关系型数据）

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

## 实施工作流

- 实施走 Spec-Kit（主体开发用 /speckit-* 命令），增量开发直接用 Claude Code，见 AiProgrammingGuide.md - 6 增量阶段：手动提示词模式；
  流程约束由 speckit skills 与 AiProgrammingGuide.md 自身承载，此处不复述

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
  升级用跨文件形态消歧；标题文字与句中既有词重叠时，可用「」包裹标题文字消歧（例：第 13 章「验收标准」）

---

本文件各条款是权威文档的压缩复述，条款末尾的括注指针即核对索引。docs/design/ 与 constitution.md 变更时，按指针定位受影响的条款并检查一致性，不一致则更新同步（含事实性断言被实测推翻）；其余情况不随日常实施维护。

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
