# AgentOS

基于 Java 的企业级 Agent OS 运行时内核。本仓库只覆盖核心阶段：交付运行时内核；
多租户 / SSO / 审计查询 / Tool Policy 等治理层属扩展阶段，本仓库不做。

## 仓库地图

- docs/ 权威文档：DemandAnalysis（需求 What，验收标准以第 13 章为唯一依据）、
  TechnicalSolution（技术方案 How，模块/接口/数据模型权威定义）、
  AiProgrammingGuide（实施方法）、IndustryResearch（背景，不做实施依据）
- 文档仲裁：实现细节以 TechnicalSolution 为准；需求范围/验收标准以 DemandAnalysis（第 13 章）为准
  （完整表述与"不改标题"等 docs 目录约定见 docs/CLAUDE.md）
- .specify/memory/constitution.md 宪章；`specs/<feature>/` 存 spec·plan·tasks
- 运行时工作区 .agentos/（agentos init 生成）：agents/ skills/ memory/MEMORY.md logs/
  mcp_servers.yaml agentos.db + 三个 Bootstrap（AGENTS.md/SOUL.md/USER.md）
- draft/ 过程与评审产物（仓库现状，不进构建）
- 快查指针：REST 端点清单见 TS 7.2；CLI 命令见 TS 8.7
- 阅读注记：TS 中"千级/几千并发"均为按 DA 8.1 目标（100 并发 Session）的 10 倍余量论证，非承诺值

## 非协商原则（.specify/memory/constitution.md 的压缩复述，冲突时以 constitution.md 为准）

1. JDK 21 + Spring Boot 3.x 单体应用，Maven 多模块、单二进制部署（9 模块：agentos-core/
   provider/memory/tool/channel-cli/web/storage/cli/boot，模块清单以 TS 第 10 章为准）
2. 五大核心能力优先：核心阶段交付运行时内核，企业级治理层放扩展阶段
3. 自实现 ReAct 循环，不依赖 Spring AI 的 Agent 抽象
4. **Spring AI 只用一半**：只用 Provider 抽象 + 协议转换 + @Tool schema 生成；**禁用自动
   tool 执行**（启用会导致 tool 被调两次——发现 tool 重复执行先查这里）。最易被违反的一条
5. Plugin Tool 三档接入（零代码 AGENT.md 目录+MCP 主推 / 轻代码自写 MCP / 重代码 @Tool Bean）；
   DA 13 验收硬项含方式三 @Tool 示例跑通
6. SQLite 关系持久化 + LongTermMemoryStore 接口 + Markdown 默认档（预留 memory.backend 切换）；
   审计表 tool_invocations / llm_calls 核心阶段就写入落库，不是只放日志；进程内不自建向量层
7. 每个 user story 完成后有可验证成果（相邻 US 可合并演示，如 US-1+US-2 合跑 Demo 一）；
   跑通优先于完美

补充红线（AG 3.4 plan review 清单 + TS 7.4 + TS 8.8）：

- 超时禁硬编码：llm/tool/total 三档分步预算，application.yaml 默认 + Profile settings.timeout 覆盖
- Memory 经 MemoryService 三层统一门面，不得简化为与 Session 合并
- 敏感凭证经环境变量注入（${ENV_VAR} 占位），不明文写配置

## 架构关键事实

- 三种触发源（CLI / Web API / AgentScheduler 定时）汇入同一 AgentService.process，审计与 Session 语义一致（TS 2）
- 一个目录 = 一个 Agent：.agentos/agents/<name>/AGENT.md（frontmatter=配置，正文=指令），AgentLoader.deriveProfile() 派生 Profile（TS 11.1）
- Agent 目录不是 Tool：AGENT.md 正文经 ContextLoader 注入 system prompt（归 core，不进 ToolRegistry）（TS 6.3）
- Skill = .agentos/skills/ 实体 + Agent 本地 skills/ 软连接绑定；frontmatter 无 skills 字段，绑定只认软连接；每轮只注入元数据，正文经 read_file 按需读（TS 6.3/8.3/11.1）
- 通知渠道是 SQLite 全局注册表（notify_channels），AGENT.md frontmatter 无 notify_channels 字段（TS 6.8/8.2）
- Provider 用 provider name → ChatModel 显式映射，禁止类型扫描 Bean（TS 3.2）
- ProfileContext 是 ThreadLocal：ReAct 循环全程禁止切换执行线程（禁 @Async / 跨线程 CompletableFuture）（TS 4.2）
- PromptBuilder 五部分（TS 4.2）中两条结构性约束：system prompt 末尾附当前日期时间（定时场景的"今天"全靠它）；Memory 段只放长期记忆，会话历史由对话历史段独立注入一次
- SQLite：首建走 ddl-auto=update（其在 SQLite 上唯一可靠场景）；方言需显式引入 hibernate-community-dialects；WAL 在 JDBC 连接层设置（TS 9.2）

## 工具使用指南

- 搜索和网页访问、爬取，只有提示词中显式指定时才使用 firecrawl ，以避免消耗 firecrawl 数量有限的 credits

## 实施工作流

- 实施走 Spec-Kit（主体开发用 /speckit-* 命令），增量开发直接用 Claude Code，见 AG 第 6 章；
  流程约束由 speckit skills 与 AG 自身承载，此处不复述

## 沟通要点

- 在聊天框中回复时：采用清晰的、短句为主的表达方式；要清楚，用户并没有深入参与你所执行的具体操作，对细节不像你那样清楚；你的介绍必须清晰易懂；特别是一些描述某些处理步骤或指代某些设计的名词，要使用容易理解的词语（或者括号简短的备注在后面）以便对方能懂。

---

本文件只在权威文档结构性修订（模块数变化、constitution 原则增删）时同步更新，不随日常实施维护。

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
