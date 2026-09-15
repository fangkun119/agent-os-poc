# Spike 结论：008-mcp（MCP Client 集成技术风险实测）

> 位置：`spike/008-mcp/README.md`
> 执行日期：2026-09-14 · 状态：已评审采纳（2026-09-14 用户裁决：D5 定候选二、安全发现最小 env 方案同意、docs 联动与根 CLAUDE.md 指针已执行）
> 规格三件套：`spec/001-req.md`、`spec/001-spec.md`、`spec/001-plan.md`
> 引用方式：结论请实名引用为"spike/008-mcp/README.md 的 D1 决议"这类形式
> 术语与编号体系：沿用 001-req 术语表与 001-spec §0.1；TS=docs/design/TechnicalSolution.md、DA=docs/design/DemandAnalysis.md（全文"TS 6.4"式引用均指这两个文件的对应章节）；"001"单独出现时指 001-req.md；"候选一 / 候选二"定义见 001-spec §3.6；M 编号=验证项（001-req 第三章）、T 编号=任务（001-plan §3）、D 编号=决议（本文第二节）；MCP=Model Context Protocol（模型连接外部工具服务器的开放协议，工具跑在独立进程"server"里，客户端经协议发现并调用）
> 主线组合：Boot 3.5.16 + JDK 21（openjdk 21.0.10）+ Spring AI 1.1.2（spring-ai-bom）+ SAA BOM 1.1.2.0（Spring AI Alibaba 的版本清单，仅管版本）+ mcp SDK 0.17.0（经 spring-ai-mcp 传递引入）+ node v25.9.0（npx 拉起 2 个 MCP 官方测试 server，npm 包版本 2026.8.31）+ 模型 MiniMax-M2.7（走 OpenAI 协议的一条模型连接线，当前指向 MiniMax 的 OpenAI 兼容端点）

**总体结论：MCP Java SDK（MCP 协议的官方 Java 开发包）路径成立，采纳组合 A（随框架走；"组合"= 四种 MCP 依赖引入方式的实验编号——A 随框架 / B 直引升级 / C 纯 SDK / D starter，定义见 001-spec §1.2）。** M1-M7（八个验证项的前七项，编号与名称对照见下节表）全部有实测结论（M8 降级路径未触发）。证据留档于 `logs/`（依赖树与各测试输出，文件名清单见 001-plan §5）。

## 一、验证项打勾表

| 项 | 结果 | 关键证据 |
|---|---|---|
| M1 传输层范围 | ✅ | 全程仅 stdio 实测通过（T2-T5 全绿；T 编号=001-plan 的任务序号：T2 连接与工具发现、T3 往返、T4 循环闭环、T5 失败模式）；Loader（配置加载器 McpServersYamlLoader）对非 stdio 的 transport 值跳过并留痕（logs/m7-config.txt）；M3-M6 无一项需要 SSE |
| M2 版本锁定 | ✅ | 组合 A 依赖树零冲突，mcp SDK 构件全落 0.17.0（logs/m2-tree-a.txt）；starter 1.1.2 在架（Maven Central 已可下载） |
| M3 工具发现 | ✅ | 27 个工具批量注册（everything 13 个 + filesystem 14 个——两者是 MCP 官方仓库维护的现成测试 server，以本地子进程方式运行），四方法非空、schema 全部可解析（logs/m3-discovery.txt） |
| M4 往返映射 | ✅ | 成功 / server 业务错（MCP 协议的 isError 标记）/ 超时三形态到统一结果结构（ToolResult，定义见第二节 D4）的映射齐备（logs/m4-roundtrip.txt） |
| M5 手动循环闭环 | ✅ | 两候选各自闭环，计数=轮数（无双执行——"同一次工具调用被框架与自研循环各执行一次"的事故模式，本项目红线）；开关生效（禁框架自动执行工具的开关，见第二节 D5 附）；usage/耗时顺带验证——一次模型调用 ↔ 一组 (in/out token, 毫秒耗时)，样本如 in=1336,out=60（logs/m5-loop.txt） |
| M6 失败模式 | ✅ | 坏命令跳过不阻断、kill 后调用悬挂至超时、SDK 无自动重连、双档超时预算（requestTimeout / initializationTimeout 各默认 20s）（logs/m6-failure.txt） |
| M7 配置四字段 + env 通路 | ✅ | 四字段（name / transport / command / env，每条 server 配置的四个字段）够用；占位符→子进程通路强证据（get-env 工具回显含探测值 MCP_SPIKE_PROBE——实验自造的非密钥环境变量，logs/m4-roundtrip.txt） |
| M8 降级路径 | 未触发 | 组合 A 全绿，SDK 路径未被证伪，001 M8 的触发前提不成立 |

## 二、决议（D1-D9）

| # | 决议 | 依据 |
|---|---|---|
| D1 | **组合 A（随框架走）：spring-ai-mcp 1.1.2（版本由 spring-ai-bom 管理），mcp SDK 0.17.0 传递引入**。版本一律以 Maven Central 为准，SDK 官方文档站快照（0.17.2）不可用作坐标；Spring AI 全程锁 1.1.x，2.0 线禁入（T7/T8 未触发，组合 B/C 未启用）。注：D5 定候选二后，候选二代码不 import spring-ai-mcp 的任何类（SyncMcpToolCallback 属候选一），spring-ai-mcp 构件仅作 mcp SDK 的版本载体；亦可直接引 `io.modelcontextprotocol.sdk:mcp:0.17.0`（组合 C 路径，未实测，如需切换另行验证） | M2 |
| D2 | **传输层结论：stdio-only 成立**。TS 6.4 收窄措辞建议："核心阶段仅 stdio，SSE 传输放扩展阶段" | M1 |
| D3 | **工具发现直映射成立**：tools/list → AgentOSTool 四方法直取 + schema JSON 直传 + ToolRegistry 批量注册 + subset 过滤，27 工具零适配障碍 | M3 |
| D4 | **ToolResult 映射定案**：成功 → success=true 拼接全部 text 段；server 业务错（isError）→ retryable=false；超时 → retryable=true；不存在工具 / 坏参数 → retryable=false。多段 content 拼接策略够用（M4 实测样本均为单段） | M4 |
| D5 | **M5 接线两候选均可行，定候选二**（2026-09-14 用户裁决；McpToolAdapter 包装 AgentOSTool + 自适配 ToolCallback）：与 TS 6.1 统一抽象同源（AgentOSTool=AgentOS 的统一工具接口，内置工具与 MCP 工具都包装成它）、审计口径不特殊化；候选一（SyncMcpToolCallback）可行但两参构造在 1.1.2 已 deprecated（builder 路径可用） | M5 |
| D5 附 | 双执行排除证据：执行计数 == 模型发起工具调用轮数（007 E3 计数法），`internalToolExecutionEnabled(false)` 生效 | M5 |
| D6 | **starter 不引入**（spring-ai-starter-mcp-client）：读自身属性体系（spring.ai.mcp.client.*）不读 mcp_servers.yaml，与自持（AgentOS 自己解析 yaml、自己管理连接）是"谁拥有连接"的二选一；双连接冲突实证（同一 server 子进程数=2）；默认不激活、未配置时不拉进程 | M2 附带 |
| D7 | **失联 / 超时最小行为**：①坏命令连接失败 → 记日志跳过、不阻断（对齐 TS 8.2）；②server 被杀后调用悬挂至 requestTimeout 超时，SDK 无自动重连——核心阶段建议不做自动重连，失败返回可重试标识、由 LLM 决定重试（对齐 TS 4.2 / DA 8.2）；③SDK 0.17.0 有两个独立超时预算 requestTimeout（默认 20s）与 initializationTimeout（默认 20s），正式实现映射到 TS 7.4 的 Tool 档（配置化不硬编码） | M6 |
| D7 附 | retryable 判定必须遍历 cause 链找 TimeoutException——SDK 把超时包成 ReactiveException（message 不含 timeout 字样），只查消息判不出 | M4/M6 |
| D8 | **mcp_servers.yaml 四字段够用、语义成立**；外层 `servers:` 列表结构已定案（2026-09-14 随 docs 联动回填 TS 6.4）；env 占位符 `${VAR}` → 子进程通路实测成立。**重大安全发现见第五节** | M7 |
| D9 | M8 降级路径未触发，手写 JSON-RPC 客户端未实现；触发前提（SDK 路径证伪）不成立 | 001 M8 |

## 三、结论 → 正式实现落点（W2=实施第二周；供该周实现 McpClientService 时借助）

| 结论 | 正式实现落点 | 怎么用 |
|---|---|---|
| D1 | TS 1.2 第 8 项 / agentos-tool 模块 pom（第二周） | 照 D1 锁定坐标；版本锚 Maven Central |
| D2 | TS 6.4 | 按建议收窄措辞 |
| D3 | TS 6.4 McpToolAdapter / McpClientService 职责句 | 直映射成立，照 001-spec §3.5 映射规则实现 |
| D4 / D7 附 | TS 6.4 / TS 6.1 | ToolResult 映射规则与 retryable cause 链判定照抄 |
| D5 | TS 6.4（已回填，候选二定案） | 候选二经 AgentOSTool 自适配，TS 1.1 决策二枚举不动（001-req 6.2 第 7 条不触发） |
| D6 | TS 1.2 / TS 6.4 | starter 不引入的决议与理由记录 |
| D7 | TS 6.4 McpClientService 职责句 | 最小行为三条照抄（跳过不阻断 / 无自动重连→可重试标识 / 双档超时映射 TS 7.4） |
| D8 | TS 6.4 | 外层 servers: 结构与四字段语义已回填；第五节安全发现已裁决同意（2026-09-14） |
| D9 | 无动作 | 未触发记录 |

## 四、失败与修复记录（诚实留档）

1. **`mvn -q` 吞掉依赖树输出**：tree 输出走 info 通道被 -q 抑制，三检查全空（"三检查"= 001-plan T0-4 对依赖树留档做 grep 核对的三项：无冲突弃用 / mcp SDK 版本落点 / Jackson 线记录）。修复：去掉 -q。
2. **Loader env 嵌套 Map 被扁平化**：castToStringMap 把 env 子 Map 也转成字符串，嵌套解析失效（IDE 诊断先于测试发现）。修复：env 从原始 map 取。
3. **SyncMcpToolCallback 两参构造 deprecated**：改 builder 链（`.mcpClient().tool().build()`）。
4. **超时 1s 卡死 initialize**：requestTimeout 覆盖 initialize，npx 冷启动 >1s 即挂。实测发现 SDK 有 requestTimeout / initializationTimeout 双档，正解是分开设。
5. **适配层 retryable 判定失效**：SDK 超时包成 ReactiveException（message 不含 timeout 字样），TimeoutException 在 cause 链上。修复：遍历 cause 链判定（已落 McpToolAdapter）。
6. **密钥红线事件（已整改）**：get-env 回显宿主全量 env（含一枚本机会话 token）被测试原样打印落盘。整改三步：打印改脱敏（只输出布尔结论）、含 token 的日志文件用重定向覆盖清理、复查 0 残留。这条是第五节安全发现的实测依据。
7. **M5 候选一首跑断言失败**：模型最终回答没带工具原文。归因断言 / 提示词设计，非框架问题；提示词硬化（007 E5 同款）后两候选全绿。
8. **T6 编排偏差**：starter 探测从 M2 类分支改为独立类 StarterProbeTest（starter 配置必须类级 properties，放 M2 类会激活离线测试的 starter 连接）；001-plan T6-4 已同步。

## 五、安全发现（已裁决：2026-09-14 用户同意最小 env 方案；docs 已联动）

**MCP server 子进程会继承 AgentOS 进程的全量环境变量。** 实测：get-env 回显了 mcp_servers.yaml
`env` 字段之外的大量宿主变量，证明 SDK 的 ServerParameters.env 是**追加而非替换**——yaml 的
env 配置只是追加项，父进程 env 全量继承。

风险链：AgentOS 进程 env 含模型密钥（OPENAI_API_KEY 等）→ 子进程全量继承 → server 侧任何
回显类工具 / 日志 / 崩溃转储都会把密钥带回工具结果 → 工具结果进 Session messages（对话历史）与
tool_invocations（工具调用审计表）→ 密钥进上下文与审计表。

**正式实现建议（随 D8 提交评审；2026-09-14 用户已裁决同意）**：McpClientService 构造子进程时使用最小 env——仅
mcp_servers.yaml 声明的 entry.env + 运行必需项（PATH 等），不用进程全量 env。落点候选：
TS 6.4 McpClientService 职责句 + TS 8.8 密钥红线延伸。执行纪律（本 spike 已执行）：回显类
输出打印脱敏、logs/ 目录 git 忽略、含疑似凭证的日志文件重定向覆盖清理。

## 六、001 §6.2 联动清单命中情况

| 行号 | 001-req 6.2 条目 | 命中与执行情况（2026-09-14 已执行） |
|---|---|---|
| 1 | TS 1.2 第 8 项替换 | **命中，已执行**（D1：spring-ai-mcp → mcp 0.17.0，组合 A） |
| 2 | TS 6.4 收窄措辞 | **命中，已执行**（D2：stdio 收窄句随 McpToolAdapter 段落回填） |
| 3 | TS 13 第二周条目精化 | **命中，已执行**（D1 坐标 / D6 不引 starter / D7、D8 最小行为与最小 env 索引） |
| 4 | spike/CLAUDE.md 目录名 | 不适用（2026-09-14 已闭档） |
| 5 | TS 6.4 模块职责句回填 | **命中，已执行**（D3/D4/D5/D7/D8：候选二定案、stdio 收窄、ToolResult 映射、最小行为、最小 env） |
| 7 | TS 1.1 决策二枚举增列 | **不触发**（2026-09-14 用户裁决定候选二，枚举增列无必要） |
| 6 | 根 CLAUDE.md 指针行 | **已执行**（用户裁决加指针；「仓库地图」Spike 结论指针增 W2 行） |
| 新增 8 | 安全发现落 docs（本 spike 执行中新发现，经用户同意） | **已执行**：TS 6.4 McpClientService 职责句 + TS 8.8 红线延伸（子进程最小 env） |

## 七、001 §6.1 八条不变项自查

| # | 不变项 | 自查 |
|---|---|---|
| 1 | MCP 工具不经 SandboxChecker（工具执行前的白名单校验器，设计上仅覆盖内置工具） | ✅ 全程未写任何 Sandbox 代码 |
| 2 | AgentOSTool 包装 + Profile 过滤（Profile=Agent 的运行时配置对象，按其 tools 字段过滤可用工具子集） | ✅ 27 工具注册 + subset 过滤实测 |
| 3 | 审计统一口径（001-spec §3.9 日志行） | ✅ M4 起每次执行打 TOOL_INVOCATION 行（与正式审计表 tool_invocations 同字段的日志行，正式实现落点为统一执行器） |
| 4 | 禁自动 tool 执行 | ✅ internalToolExecutionEnabled(false) 全程生效，计数=轮数 |
| 5 | 密钥红线 | ✅（含一次事件整改，见第四节第 6 条） |
| 6 | 基线不动（parent / BOM） | ✅ T0 依赖树零冲突 |
| 7 | DA 13 验收不降 | ✅ M8 未触发，无降级 |
| 8 | 线程红线（只用 sync 客户端） | ✅ 循环用 sync 客户端 + 阻塞 call；SDK 内部 IO 线程仅做读写 |

## 八、遗留与偏差

- **连接时机策略待裁决（2026-09-14 发现）**：本 README D7 沿 TS 6.4 原文实测"启动时连接 + 失败跳过不阻断"；agentos/CLAUDE.md 编码规范另有一条"C-86：MCP server 连接懒加载（要用时才连接/列取），禁 @PostConstruct 全量初始化"（@PostConstruct=Spring Bean 初始化时自动执行的钩子注解）——两者对"不可达 server 不拖挂启动"给出了相反策略（启动连接+跳过 vs 干脆不启动连）。spike 未做连接时机的对照实验，D7 忠实记录的是 TS 现文方向。第二周 McpClientService 实现前需裁决：启动全量+跳过 / 纯懒加载 / 混合（启动只解析配置、首次使用才连接并缓存）；裁决结果回填 TS 6.4 与 agentos/CLAUDE.md 之一；
- 编排偏差仅 T6-4 一处（独立类 StarterProbeTest），001-plan 已同步；
- logs/ 已 git 忽略；含 token 的旧日志文件已被重定向覆盖，复查 0 残留；
- MiniMax-M2.7 的 `<think>` 剥离（ThinkStripper）有效，M5 两候选 finalText 剥后干净；
- 工程纪律：独立 pom、未进根 modules、spike/007 的 src 与 logs 封存未动。
