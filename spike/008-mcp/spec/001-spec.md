# MCP Client 集成 Spike 代码规格说明（001-spec）

> 位置：`spike/008-mcp/spec/001-spec.md`
> 创建：2026-09-14 · 状态：已执行（2026-09-14，配套实验全部完成，结论见 README）
> 上游：`001-req.md`（下称 001；M1-M8 问题清单、方法、结论记录要求、一致性清单的唯一源头）
> 平行输入：`spike/007-react-loop/README.md`（下称 007 README；D1-D4 决议与基线坐标）、`docs/design/detail/model-config.md`（密钥与 yaml 接线规则；引用用 model-config.md - 编号 标题主干 全名形，2026-10-03 起"定稿"别名停用）
> 下游：`001-plan.md`（待生成的实施计划，命名沿 007 先例）
> 读者：零背景读者——没有 2026-09-13 设计评审会话记录、只读本文件的人。术语沿用 001 术语表，本文新增术语见 0.1 节

## 0. 文档定位

- 002 回答两件事：**实验代码长什么样**（工程形态、组件、配置），**M1-M8 每项怎么验、验收怎么判**。
- 验收源头是 001 第三章（M1-M8 的问题与出口形态）与第五章（README 必须记录的 9 条结论）。002 只做代码层细化，**不得弱化** 001 的出口条件，也不增设 001 没有的范围。
- 文档系列对照：007 的三件套是 `001-expirement.md` / `001-spec.md` / `001-plan.md`；本 spike 是 001-req（需求）→ 001-spec（本文）→ 001-plan（后续）。角色一一对应，007 的 001-expirement（实验流程规格）对应本 spike 的 001-req（需求）。

### 0.1 本文新增术语（001 术语表之外）

| 术语 | 白话解释 |
|---|---|
| everything server / filesystem server | MCP 官方仓库（modelcontextprotocol/servers）维护的两个现成 server，均为 stdio 传输的本地子进程，用 npx 拉起。spike 的被连对象（001 四章方法 1） |
| ToolCallback | Spring AI 里"一个可被模型调用的工具"的统一封装对象。`@Tool` 方法和 MCP 工具都能转成它、进入循环选项的 toolCallbacks 参数 |
| SyncMcpToolCallback / McpToolUtils | spring-ai-mcp 模块提供的现成适配类与工具方法：把 MCP 工具直接转成 ToolCallback（Spring AI v1.1.8 文档核验，2026-09-14 Context7）。M5 候选一的依托 |
| spring-ai-starter-mcp-client | Spring AI 的 MCP Client 开箱依赖包，按 `spring.ai.mcp.client.*` 配置自动建连、注册工具。M2 附带验证项（001 M2 附带要定） |
| starter 自动装配 | 引入 starter 后 Spring Boot 自动创建并配置相关 Bean 的机制。与"AgentOS 自持 McpClientService"的取向是否相容，要实测 |
| 探针法 | 007 E1 沿用的依赖树验证手法：引入构件、看依赖树解析出的版本，验证版本仲裁是否生效 |
| 组合 A / B / C / D | 本文 1.2 节定义的四个 MCP 依赖实验组合 |

## 1. 工程形态

### 1.1 目录结构

```text
spike/008-mcp/
├── pom.xml                                # 独立 Maven 工程（spike/CLAUDE.md：不进根 pom modules）
├── .gitignore                             # 忽略 logs/
├── spec/                                  # 001-req / 001-spec（本文）/ 001-plan（待生成）
├── src/main/java/spike/mcp/
│   ├── SpikeApp.java                      # @SpringBootApplication 最小启动类
│   ├── config/McpServerEntry.java         # mcp_servers.yaml 单条配置（四字段）
│   ├── config/McpServersYamlLoader.java   # 读 yaml + ${环境变量名} 占位符解析（M7）
│   ├── client/SpikeMcpClients.java        # 连接维护（M3/M6；正式实现 McpClientService 的 spike 形态）
│   ├── tool/AgentOSTool.java              # 四方法接口最小副本（TechnicalSolution.md - 6.1 AgentOSTool 抽象）
│   ├── tool/ToolResult.java               # 四要素载体（TechnicalSolution.md - 6.1 AgentOSTool 抽象 的四要素）
│   ├── tool/ToolRegistry.java             # 内存注册表（TechnicalSolution.md - 6.6 ToolRegistry 的 spike 形态）
│   ├── tool/McpToolAdapter.java           # MCP 工具 → AgentOSTool 适配（M3/M4；对应 TechnicalSolution.md - 6.4 Plugin Tool 方式二 的 McpToolAdapter）
│   ├── tool/McpToolCallbacks.java         # M5 候选一：spring-ai-mcp 现成 ToolCallback 适配
│   ├── loop/ManualLoop.java               # 手动 ReAct 循环（按 007 README D3 重写，不拷代码）
│   ├── loop/LoopResult.java               # 循环结果载体
│   └── util/ThinkStripper.java            # <think> 标签剥离（断言前处理）
├── src/main/resources/
│   ├── application.yaml                   # 模型腿（照 007 spec §2.1 同款、只留 OPENAI 腿）
│   └── mcp-servers.yaml                   # 实验用 MCP server 配置（M7）
├── src/test/java/spike/mcp/               # 第 4 节测试类（M2-M7 各一 + 条件 M8）
└── logs/                                  # 证据留档（git 忽略）
```

### 1.2 Maven 坐标与依赖

基线（照 007 README D1/D2。parent / JDK / 双 BOM 三项一字不动；模型 starter 相对 007 D2 清单收窄为单腿——007 D2 含 openai / anthropic 两条，本 spike 无双腿需求、减变量，版本仍由同一 spring-ai-bom 管理。此为对 001 四章方法 2"LLM 依赖照 007 D2 坐标清单引入"的收窄解读，已经用户裁决确认（2026-09-14）：按单腿 OPENAI 执行）：

| 项 | 值 | 说明 |
|---|---|---|
| groupId / artifactId | `com.agentos.spike : mcp-spike` | 独立工程，永不进根 pom `<modules>` |
| parent | `spring-boot-starter-parent:3.5.16` | 007 D1 |
| java.version | 21 | 同上 |
| BOM | `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.1.2.0` + `org.springframework.ai:spring-ai-bom:1.1.2`（双 import） | 007 D2 |
| 依赖 | `org.springframework.ai:spring-ai-starter-model-openai`（版本由 BOM 管理 → 1.1.2） | M5 循环的模型腿。ANTHROPIC 腿本 spike 不引入（无双腿需求，减变量） |
| 依赖 | `org.springframework.boot:spring-boot-starter-test`（test） | JUnit 5 + Spring Test |
| 插件 | surefire `forkedProcessTimeoutInSeconds=180` | 防挂死兜底（007 同款，非三档超时预算） |

MCP 相关依赖是 **M2 的实验对象，不在此预选**。四个实验组合（去留按 1.3 节决议规则）：

| 组合 | 新增坐标 | SDK 版本落点 | 验证点 |
|---|---|---|---|
| A 随框架走 | `org.springframework.ai:spring-ai-mcp`（版本由 spring-ai-bom 管理 → 1.1.2） | 传递 `io.modelcontextprotocol.sdk:mcp:0.17.0`（repo1 POM 实查 2026-09-14，另传递 mcp-spring-webflux / mcp-spring-webmvc 同为 0.17.0） | 框架托管的默认位；M5 候选一（SyncMcpToolCallback）可用 |
| B 直引升级 | A 基础上 `dependencyManagement` 显式钉 `io.modelcontextprotocol.sdk:mcp` 更高版本 | 0.18.4 → 1.1.4 → 2.0.1 从低到高试（Maven Central metadata 实查 2026-09-14：全线 40 版、0.x 线 25 版止于 0.18.4、1.x 线止于 1.1.4、当前 release 2.0.1） | 版本提升后 spring-ai-mcp 适配是否仍工作；钉版本时三构件（mcp / mcp-spring-webflux / mcp-spring-webmvc）须同钉，混合版本态如实记录 |
| C 纯 SDK 直引 | 只引 `io.modelcontextprotocol.sdk:mcp`（不经 spring-ai-mcp） | 同 B 各档 | 摆脱 spring-ai-mcp 的裸 SDK 路径；此组合下 M5 只能走候选二（自适配） |
| D starter | `org.springframework.ai:spring-ai-starter-mcp-client`（repo1 已确认 1.1.2 在架，2026-09-14 实查） | 随 spring-ai-mcp 同线 | 自动装配与自持 mcp_servers.yaml / 连接管理的取向是否相容（M2 附带） |

两个注意：

- **MCP Java SDK 的 1.x / 2.x 版本线与"Spring AI 2.0 禁入"是两码事**。禁入 Spring AI 2.0 线禁的是框架本身（`internalToolExecutionEnabled` 开关已删；锁 1.1.x 见 007 D2 与 agentos/CLAUDE.md 第 1 章）；MCP Java SDK 是另一个项目的版本演进，不连带禁入。
- **Jackson 线是组合 B / C 的已知风险点**：SDK 0.17.x 走 Jackson 2.x（与 Boot 3.5 同线）；SDK 新版默认 Jackson 3.x，官方另发 `mcp-json-jackson2` 兼容模块（SDK 官方文档 quickstart，2026-09-14 Context7 核验）。依赖树见冲突时按此对治，实测记录进 M2。

### 1.3 决议规则（实验结果 → 组合去留）

| 实验结果 | 决定 |
|---|---|
| 组合 A 全绿（M3-M5 通过） | 主选 A：兼容性由 Spring AI 官方保证、与 007 主线同源。B / C 仅留依赖树记录，不深跑 |
| A 缺能力或有硬伤（工具调用失败、适配类缺失） | 升 B：从 0.18.4 起，M3-M5 复验通过即停（不追新）；B 各档全败再试 C |
| A / B / C 都无法支撑 M5 候选一 | M5 走候选二（AgentOSTool 自适配），组合可选 C 纯 SDK |
| D 的自动装配与自持 yaml / 连接管理冲突且不可关 | starter 不引入，README 记录冲突形态 |
| 任何组合要求违反第 5 节红线才能跑（如必须开自动 tool 执行） | 该组合否决，不进入候选 |

## 2. 配置

### 2.1 application.yaml 全文草稿（src/main/resources）

```yaml
spring:
  application:
    name: mcp-spike
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:placeholder}        # 密钥：环境变量；离线项可用占位默认值
      base-url: https://api.minimax.cn              # 非敏感：明文 yaml，不带 /v1（model-config.md - 5.3 MiniMax 两份文档是同一个端点）
      chat:
        options:
          model: ${OPENAI_DEFAULT_MODEL:MiniMax-M2.7}
```

- 变量语义按 model-config.md - 2 环境变量命名规则（`OPENAI_*` 四元组）。
- 默认**不写** `spring.ai.mcp.*`：自持路线（组合 A / B / C）不依赖 starter 属性。组合 D 验证时用独立测试配置（`src/test/resources/application-starter.yaml`）开启，避免污染主线。
- M2-M4、M7 的离线部分不起模型调用，占位默认值即可；M5 前先 `source ~/.agent-os-poc/script/agent-os-env.sh`。

### 2.2 mcp-servers.yaml 实验稿（src/main/resources；模拟 .agentos/mcp_servers.yaml 形态）

```yaml
servers:
  - name: everything
    transport: stdio
    command: "npx -y @modelcontextprotocol/server-everything"
    env:
      MCP_SPIKE_PROBE: ${MCP_SPIKE_PROBE}    # 占位符通路验证用，非真实凭证（M7）；不设缺省值——缺失即报错正是要验证的行为
  - name: filesystem
    transport: stdio
    command: "npx -y @modelcontextprotocol/server-filesystem ./sandbox-dir"
    env: {}
```

说明：

- 四字段 `name` / `transport` / `command` / `env` 的语义按 TechnicalSolution.md - 6.4 Plugin Tool 方式二 与 001-req M7。顶层用 `servers:` 列表是本 spike 的自定（docs 只定了字段、没定外层结构）——M7 结论回填 TechnicalSolution.md - 6.4 Plugin Tool 方式二 时一并定案（001 的 6.2 节第 5 条）。
- `command` 是一条启动命令字符串；加载时拆成"可执行文件 + 参数数组"传给 SDK 的进程构造，不经 Shell 解释（与 TechnicalSolution.md - 6.7 Sandbox 检查 的 argv 直传同一哲学）。
- `env` 的值只允许 `${环境变量名}` 占位符或非敏感值（TechnicalSolution.md - 8.8 配置与密钥加载 的密钥红线延伸，001 的 6.1 节第 5 条）。`MCP_SPIKE_PROBE` 的值不是凭证，用于验证"占位符解析 → 子进程环境变量 → server 侧可见"这条通路。
- filesystem server 允许访问的目录以命令参数给定（`./sandbox-dir`，spike 目录内自建，预置一个可读文本文件）；两个 server 的具体命令形态以 spike 执行时官方仓库 modelcontextprotocol/servers 的当前形态为准（001 四章方法 1 同款口径）。

### 2.3 模型组合

| Provider | 模型 | 用途 |
|---|---|---|
| OPENAI | MiniMax-M2.7（缺省） | M5 循环闭环主用（007 spec §2.2 同款） |

## 3. 组件规格

### 3.1 SpikeApp

- `@SpringBootApplication` 最小启动类，无业务逻辑。

### 3.2 McpServerEntry + McpServersYamlLoader（M7）

- `McpServerEntry`：四字段 record（name / transport / command / env）。
- Loader 职责：读 `mcp-servers.yaml` → 逐条校验（`name` 唯一；`transport` 当前只认 `stdio`，其他值记录后跳过并给清晰报错，不抛异常终止整个加载——参照 TechnicalSolution.md - 8.2 Profile 配置 的"校验失败不阻断启动但记录错误日志"）→ `env` 值中的 `${环境变量名}` 从环境变量解析，缺失时给清晰报错。
- 占位符解析的注入式设计：Loader 收一个 `Map<String, String> env` 参数（生产传 `System.getenv()`，测试注入构造值）——避免测试里改 JVM 环境变量的麻烦，也让"缺失报错 / 存在解析"两个用例可控。
- command 拆分：空格切分为 argv。spike 规模够用；引号等复杂 shell 语法不支持，如实记录为限制。
- 日志纪律：env 值打印最多 5 位前缀（红线，001 的 6.1 节第 5 条）。

### 3.3 SpikeMcpClients（M3 / M6；正式实现 McpClientService 的 spike 形态）

- 按每条配置建立**同步**客户端（TechnicalSolution.md - 4.2 模块组成 的线程约束：ReAct 循环全程禁切换执行线程，异步客户端不引入——001 的 6.1 节第 8 条）。链路：进程参数构造（可执行文件 + args + env）→ stdio 传输 → `McpClient.sync(transport).requestTimeout(...).build()` → `initialize()` → `listTools()`。
- 具体构造 API 以 M2 选定组合的 SDK 版本为准（0.17 与 2.x 的 API 面不同，正是 M2 要实测的内容）；最新文档形态见 SDK 官方 quickstart / client 页（2026-09-14 Context7 核验：`ServerParameters.builder(...).args(...)` + `StdioClientTransport` + `McpClient.sync(...)` 三件套）。
- 单个 server 连接失败：记录异常、跳过该 server、不阻断其余 server 与整体启动（M6①素材）。
- `requestTimeout` 的设定值与 SDK 默认值实测记录（M6③）。正式实现的超时对齐 TechnicalSolution.md - 7.4 关键设计点 的 Tool 档预算（默认 30s、配置化不硬编码），不自造新的超时机制——实测结论写进 README 供 TechnicalSolution.md - 6.4 Plugin Tool 方式二 回填（001 M6 设计参照）。
- `closeGracefully()` 收尾；server 子进程句柄清理验证（防进程残留）。

### 3.4 AgentOSTool / ToolResult / ToolRegistry

本节为 TechnicalSolution.md - 6.1 AgentOSTool 抽象 / 6.6 ToolRegistry 的最小副本。

- `AgentOSTool` 四方法：`getName` / `getDescription` / `getInputSchema`（JSON Schema 字符串直传）/ `execute(jsonInput) → ToolResult`。
- `ToolResult` 四要素：成功标识、结果内容、错误信息、是否可重试。
- `ToolRegistry`：内存 Map；`register` / `lookup` / `list`；`subset(List<String> names)` 模拟 Profile `tools` 字段的白名单过滤（M3 对照 TechnicalSolution.md - 6.6 ToolRegistry）。

### 3.5 McpToolAdapter（M3 / M4 核心）

- 映射：`tools/list` 返回的每个工具 → `AgentOSTool`。`name` / `description` 直取；`inputSchema` 为 JSON Schema、直传（MCP 工具 schema 与 Spring AI `@Tool` 生成的 schema 同为 JSON Schema——直映射是否成立由 M3 实证，001 M3 对照点）；`execute` → `callTool(name, args)`。
- 一个 server 的多个工具批量注册（M3）。
- 结果映射（M4）：返回的 `content` 可能多段（文本 / 资源等），默认拼接全部 text 段为结果内容；实测到的结果形态如实落盘。裁剪策略属 DemandAnalysis.md - 12 风险与未决事项 的未决挂账，本 spike 不展开（001 M4 边界说明）。
- 出错映射（M4 / M6）：协议错误、server 报错（`isError`）、超时 → `ToolResult(success=false, error=..., retryable=...)`。`retryable` 的默认判定在 spike 定初值（建议：连接 / 超时类 true，参数 / 不存在类 false），结论回填 TechnicalSolution.md - 6.4 Plugin Tool 方式二（001 的 6.2 节第 5 条）。

### 3.6 McpToolCallbacks（M5 候选一）

- 复用 spring-ai-mcp 现成适配：`SyncMcpToolCallback(mcpClient, mcpTool)` / `McpToolUtils.getToolCallbacksFromSyncClients(clients)`（Spring AI v1.1.8 文档核验，2026-09-14 Context7）。
- 与候选二（McpToolAdapter 包装成 AgentOSTool 后自适配成 ToolCallback）在 M5 各跑一次同款闭环，对比两点：注入 toolCallbacks 后模型可见的 schema 是否等价；工具执行是否全部由我方循环触发。
- 实测定一条（001 M5"实测定一条"）。

### 3.7 ManualLoop + LoopResult（M5；按 007 README D3 重写、不拷代码）

- 五步路径：`ToolCallingChatOptions.builder().toolCallbacks(...).internalToolExecutionEnabled(false).build()` → `new Prompt(text, options)` → `chatModel.call(prompt)` → `hasToolCalls()` 为真 → `toolCallingManager.executeToolCalls(prompt, chatResponse)` → `conversationHistory()` 重建 Prompt 再调；为假 → 循环结束。
- 与 007 的差异仅一处：`toolCallbacks` 的来源是 MCP 工具（候选一经 3.6，候选二经 3.5 加自适配包装）。
- 循环内维护 MCP 工具执行计数，断言"执行次数 = 模型发起工具调用的轮数"（007 E3 计数法）——M5 的双执行排除证据。
- `LoopResult` 沿用 007 字段：`finalText` / `iterations` / `toolCallRounds` / `usages` / `durationsMs` / `hitLimit`。

### 3.8 ThinkStripper

- 删除所有 `<think>...</think>` 片段（含跨行），供断言前处理（MiniMax-M3 / M2.7 思考内容混正文，007 README 附带实测结论）。同款重写。

### 3.9 审计口径模拟（001 的 6.1 节第 3 条）

- spike 不建 SQLite。每次工具执行按 `tool_invocations` 的字段口径打一行结构化日志：tool_name / input_json / success / error_message / duration_ms。
- 目的：验证 MCP 工具与其他工具走同一条执行路径、留痕口径不特殊化（TechnicalSolution.md - 4.2 模块组成 的 ToolExecutor 统一写入，审计不自建）。README 记录正式实现里这行日志的落点（ToolExecutor）。

## 4. 验证项与验收标准（M1-M8 → 测试类与判定）

| 编号 | 测试类 | live | 判定（通过标准） | 追溯 |
|---|---|---|---|---|
| M1 | 无独立测试类：传输层结论由 M2-M7 全程 stdio 实测汇总；Loader 对非 stdio transport 的跳过记录作辅助证据 | — | README 传输层结论节成立：stdio-only 是否覆盖核心阶段全部场景；若成立，附 TechnicalSolution.md - 6.4 Plugin Tool 方式二 的收窄措辞建议（001 的 6.2 节第 2 条） | 001 M1 |
| M2 | `M2DependencyMatrixTest`（离线）+ 组合切换复跑 M3-M5 | 否 | `mvn dependency:tree -Dverbose` 按组合留档：无 "omitted for conflict" 意外项；mcp SDK 解析版本 = 组合预期；选定组合下 M3-M5 全绿 | 001 M2 |
| M3 | `M3DiscoveryTest` | 是 | `tools/list` 返回的工具逐个映射成 AgentOSTool（四方法取值非空、schema 可解析）；一个 server 多工具批量注册数 = listTools 返回数；`subset(...)` 白名单过滤正确 | 001 M3 |
| M4 | `M4RoundtripTest` | 是 | 成功调用：`ToolResult.success=true`、内容含预期要素；失败调用（错误参数 / 不存在的工具）：`success=false` 且 error 非空；成功 / 失败 / 超时三种映射形态落盘 | 001 M4 |
| M5 | `M5LoopClosedTest`（两候选各一次） | 是 | LLM 经手动循环真调一个 MCP 工具完成闭环：`strip(finalText)` 含工具结果要素；`internalToolExecutionEnabled(false)` 生效；执行计数 = 模型发起轮数（无双执行）；两候选至少一条走通并定一条 | 001 M5 |
| M6 | `M6FailureModesTest` | 是 | ①坏命令（command 指向不存在的程序）：异常形态落盘、其余 server 不受影响；②kill server 子进程：调用异常形态 + SDK 重连行为（有无）落盘；③requestTimeout 调短：超时异常 → ToolResult 映射 | 001 M6 |
| M7 | `M7ConfigTest`（部分离线） | 混合 | 四字段解析成功；`${VAR}` 占位符解析后进入子进程环境且 server 侧可回显（回显手段以 everything server 实际工具 / 资源为准，tools/list 落盘即证据）；占位符缺失给清晰报错；日志中 env 值 ≤ 5 位前缀 | 001 M7 |
| M8 | `M8FallbackProbeTest`（条件触发） | 是 | 仅当 M2-M5 被证伪：手写最小 stdio JSON-RPC 客户端（initialize + tools/list + tools/call）对 everything server 跑通最小闭环；实现量（代码行数）落盘供降级决议 | 001 M8 |

验收基调：001 第五章 9 条"必须记录的结论"全部继承、只增不减；live 项偶发偏差允许固定次数重跑并记录（007 spec 验收基调同款）。

## 5. 约束与红线（继承 001 的 6.1 节八条，全文重列）

| # | 约束 | 出处 |
|---|---|---|
| 1 | MCP 工具不经 SandboxChecker 校验；spike 实验不给 MCP 工具挂 Sandbox 校验 | 001 的 6.1 节第 1 条（TechnicalSolution.md - 6.6 ToolRegistry / TechnicalSolution.md - 6.7 Sandbox 检查、评审 Q5③） |
| 2 | MCP 工具包装成 AgentOSTool 进 ToolRegistry，白名单过滤模拟 Profile `tools` 字段 | 001 的 6.1 节第 2 条（TechnicalSolution.md - 6.6 ToolRegistry） |
| 3 | 审计统一路径：MCP 工具执行与其他工具同一条留痕口径（3.9 节），不自建 | 001 的 6.1 节第 3 条（TechnicalSolution.md - 4.2 模块组成 / TechnicalSolution.md - 6.1 AgentOSTool 抽象 / TechnicalSolution.md - 9.2 SQLite 关系型数据） |
| 4 | 禁自动 tool 执行：`internalToolExecutionEnabled(false)`，M5 接线必须走手动循环 | 001 的 6.1 节第 4 条（根 CLAUDE.md 非协商原则 4、TechnicalSolution.md - 1.1 关键技术决策 的决策二） |
| 5 | 密钥红线：env 只写 `${环境变量名}` 占位符，不明文写配置；日志 / 命令行最多 5 位前缀 | 001 的 6.1 节第 5 条（TechnicalSolution.md - 8.8 配置与密钥加载） |
| 6 | 基线不动：parent 3.5.16 + JDK 21 + 双 BOM（SAA 1.1.2.0 + spring-ai-bom 1.1.2） | 001 的 6.1 节第 6 条（007 README D1/D2） |
| 7 | DemandAnalysis.md - 13 验收标准 的验收不降：M8 降级路径也不降验收标准 | 001 的 6.1 节第 7 条（DemandAnalysis.md - 13 验收标准） |
| 8 | 线程红线：只用同步（sync）客户端或经阻塞封装使用；不引入与 ThreadLocal ProfileContext 相抵的接线 | 001 的 6.1 节第 8 条（TechnicalSolution.md - 4.2 模块组成、根 CLAUDE.md「架构关键事实」） |
| 9（工程纪律） | 构建与测试只在 `spike/008-mcp/` 目录内执行；pom 永不进根 `<modules>`；007 的 src 与 logs 视为封存证据，不改不拷 | spike/CLAUDE.md；007 的 spec/002-req.md 4.2 节 |

## 6. 开放项（实验落定，不阻塞 001-plan）

| # | 开放项 | 落定时机 |
|---|---|---|
| 1 | mcp_servers.yaml 外层结构（`servers:` 列表 vs 其他）——docs 只定四字段、未定外层 | M7；随结论回填 TechnicalSolution.md - 6.4 Plugin Tool 方式二 |
| 2 | 组合 B / C 各 SDK 档（0.18.4 / 1.1.4 / 2.0.1）与 Spring AI 1.1.2 的兼容边界 | M2 |
| 3 | Jackson 线冲突是否出现；`mcp-json-jackson2` 兼容模块是否需要 | M2 依赖树 |
| 4 | starter（组合 D）自动装配与自持 yaml / 连接管理的相容性 | M2 附带 |
| 5 | 多段 content 的默认拼接策略是否够用 | M4 |
| 6 | `retryable` 默认判定（连接 / 超时类 vs 参数类） | M4 / M6 |
| 7 | everything server 是否有环境回显工具（M7 通路验证手段） | M3 tools/list |
| 8 | `requestTimeout` 默认值与单位 | M6③ |

## 7. 对 001-plan 的输入

1. 任务顺序建议：骨架 + M2 组合矩阵（离线）→ M7 配置 → M3 发现 → M4 往返 → M5 闭环 → M6 失败模式 →（条件）M8 → README 落盘。理由：M2 定组合后 M3-M5 才有稳定的 SDK 面可跑；M7 前置到 M3 之前，因为连接配置是 M3 的输入。
2. 环境前置：JDK 21、Maven 3.6+、Node.js 与 npx（拉起 everything / filesystem server 两个子进程）、`source ~/.agent-os-poc/script/agent-os-env.sh`（M5 前置）。
3. 命令：`mvn test`（全量）、`mvn test -DexcludedGroups=live`（离线）、`mvn dependency:tree -Dverbose > logs/m2-tree-<组合名>.txt`。
4. 证据留档：`logs/` 下按 M 编号命名（如 `m2-tree-a.txt`、`m5-loop.txt`）。
5. README 格式（001 第五章）："D 决议"编号在 `spike/008-mcp/README.md` 内从 D1 起自有编号；引用一律实名带文件名（如"spike/008-mcp/README.md 的 D1 决议"），不与 007 README 的 D1-D4 混淆。
