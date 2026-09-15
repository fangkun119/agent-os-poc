# MCP Client 集成 Spike 实施计划（001-plan）

> 位置：`spike/008-mcp/spec/001-plan.md`
> 创建：2026-09-14 · 状态：已执行（2026-09-14，任务全部完成，结论见 README）
> 上游：`001-req.md`（下称 001；M1-M8 问题清单与出口形态）、`001-spec.md`（下称 002；工程形态、组件规格、验收判定——本文步骤里的"002 §x"均指它的节号）
> 约定：① 所有命令在 `spike/008-mcp/` 目录内执行 `mvn`（spike/CLAUDE.md）；② live 任务前先 `source ~/.agent-os-poc/script/agent-os-env.sh`；③ 术语沿用 001 术语表 + 002 §0.1；④ spike 执行期间不改 docs/design/ 与根 CLAUDE.md（001 §6.3 文档冻结令）

## 0. 文档定位

- 本文回答三件事：**按什么顺序做**（任务依赖）、**每步敲什么命令**（可执行）、**怎么算完成**（预期输出）。
- 完成定义（整体 DoD）：T0-T6 全部完成 + T7/T8 按条件执行或写明未触发理由 + T9 的 README.md 落盘 + 001 §6.1 八条不变项逐条自查通过。

## 1. 任务总览（顺序与依赖）

```text
T0 骨架与依赖树（组合 A，离线）       ← 唯一入口，不需要 key、不需要 server
 └→ T1 配置加载（M7 离线）           ← 不需要 server
     └→ T2 连接与工具发现（M3）      ← 需要 npx 拉起 server（live）
         └→ T3 工具调用往返（M4）    ← live
             └→ T4 手动循环闭环（M5）← 需要 key（live）
                 └→ T5 失败模式（M6）← live
                     └→ T9 结论落盘（README）
T6 starter 相容性（M2 附带）          ← 依赖 T2，独立于主线成败，结论只记录
T7 条件支线：组合升降级（A→B→C）     ← 仅当 M3/M4/M5 在当前组合失败
T8 条件支线：M8 手写 JSON-RPC 降级   ← 仅当 SDK 路径全被证伪
```

- T0 → T5 线性：前一任务完成判定未过，不进入下一任务（判定失败先走 T7 分支，不硬闯）。
- M1（传输层结论）无独立测试类：证据来自 T1（非 stdio transport 跳过记录）与 T2-T5（全程 stdio 实测日志），在 T9 汇总落盘。

### 1.1 代码目录结构与创建任务归属

目录结构源头是 002 §1.1，此处展开到文件级并标注每个文件的创建任务（T 编号见 §3）；`#` 注释即归属任务，未标注的目录是纯容器。

```text
spike/008-mcp/
├── pom.xml                                # T0-2（基线 + 组合 A；T7 触发时改钉 SDK 版本）
├── .gitignore                             # T0-1（忽略 logs/）
├── spec/                                  # 已有：001-req / 001-spec / 001-plan（本文）
├── src/main/java/spike/mcp/
│   ├── SpikeApp.java                      # T0-3（@SpringBootApplication 最小启动类）
│   ├── config/
│   │   ├── McpServerEntry.java            # T0-3（四字段 record，先建骨架供 Loader 用）
│   │   └── McpServersYamlLoader.java      # T1-2（yaml 解析 + ${VAR} 占位符 + env 注入式）
│   ├── client/
│   │   └── SpikeMcpClients.java           # T2-1（sync 客户端建立与维护）
│   ├── tool/
│   │   ├── AgentOSTool.java               # T0-3（四方法接口最小副本）
│   │   ├── ToolResult.java                # T0-3（四要素载体）
│   │   ├── ToolRegistry.java              # T0-3（内存注册表 + subset 过滤）
│   │   ├── McpToolAdapter.java            # T2-2（MCP 工具 → AgentOSTool；M5 候选二的地基）
│   │   └── McpToolCallbacks.java          # T4-2（spring-ai-mcp 现成适配；M5 候选一）
│   ├── loop/
│   │   ├── ManualLoop.java                # T4-3（五步路径重写，不拷 007 代码）
│   │   └── LoopResult.java                # T4-3
│   └── util/
│       └── ThinkStripper.java             # T4-3（<think> 剥离，同款重写）
├── src/main/resources/
│   ├── application.yaml                   # T0-3（002 §2.1 全文；不写 spring.ai.mcp.*）
│   └── mcp-servers.yaml                   # T1-1（002 §2.2 实验稿）
├── src/test/java/spike/mcp/               # 测试类与所属验证项（002 §4 判定）
│   ├── M2DependencyMatrixTest.java        # T0-4（组合断言；T6-4 加 starter.probe 分支）
│   ├── M7ConfigTest.java                  # T1-3（离线部分）
│   ├── M3DiscoveryTest.java               # T2-3（live）
│   ├── M4RoundtripTest.java               # T3-1（live）
│   ├── M5LoopClosedTest.java              # T4-4（live；两候选各一次）
│   ├── M6FailureModesTest.java            # T5-1（live）
│   └── M8FallbackProbeTest.java           # T8（条件触发才建）
├── src/test/resources/
│   └── application-starter.yaml           # T6-3（组合 D 专用配置，002 §2.1 说明）
└── logs/                                  # 各任务测试输出重定向（git 忽略，清单见 §5）
```

## 2. 环境前置（2026-09-14 实测，全部就绪）

| # | 项 | 现状 | 缺失时 |
|---|---|---|---|
| 1 | JDK 21 | openjdk 21.0.10 | 按根 CLAUDE.md 技术栈安装 |
| 2 | Maven（/opt/homebrew/bin） | 3.9.10 | brew install maven |
| 3 | node + npx（/usr/local/bin） | node v25.9.0 | 按 nodejs.org 安装 |
| 4 | npm 网络 | registry=npmjs.org；@modelcontextprotocol/server-everything 与 server-filesystem 在架（版本 2026.8.31） | 检查网络 / 镜像 |
| 5 | 密钥脚本 `~/.agent-os-poc/script/agent-os-env.sh` | 在位、权限 600 | 找项目方要 OPENAI_* 四元组 |
| 6 | server 包预热（可选） | 首次 npx 需联网下载 | `npx -y @modelcontextprotocol/server-everything --help` 后 Ctrl-C |

## 3. 任务明细

### T0 骨架与依赖树（组合 A）——离线

| # | 步骤 |
|---|---|
| T0-1 | 按 002 §1.1 建目录结构与 `.gitignore`（忽略 logs/） |
| T0-2 | 写 `pom.xml`（002 §1.2 基线 + 组合 A：parent 3.5.16、Java 21、双 BOM、spring-ai-starter-model-openai、spring-ai-mcp、starter-test、surefire `forkedProcessTimeoutInSeconds=180`） |
| T0-3 | 写 `application.yaml`（002 §2.1 全文）与骨架类：`SpikeApp`（§3.1）、`AgentOSTool` / `ToolResult` / `ToolRegistry`（§3.4）、`McpServerEntry` |
| T0-4 | 依赖树留档 + 检查（命令见下）；写 `M2DependencyMatrixTest` 的组合 A 断言（mcp SDK 解析版本 = 0.17.0）并跑绿 |

```bash
mvn dependency:tree -Dverbose > logs/m2-tree-a.txt
# 检查 1：spring-ai / Boot 坐标无冲突弃用（预期无输出）
grep -E "org.springframework.ai|org.springframework.boot" logs/m2-tree-a.txt | grep "omitted for conflict"
# 检查 2：mcp SDK 三构件落 0.17.0（预期三行）
grep -E "io.modelcontextprotocol.sdk" logs/m2-tree-a.txt
# 检查 3：Jackson 线记录（002 §1.2 注意事项 + 开放项 3）——树中 jackson 相关行原样留档
grep -E "com.fasterxml.jackson|tools.jackson" logs/m2-tree-a.txt
```

T0 完成判定：检查 1 无异常；检查 2 三构件（mcp / mcp-spring-webflux / mcp-spring-webmvc）均 0.17.0；`M2DependencyMatrixTest` 组合 A 断言绿。证据：`logs/m2-tree-a.txt`。

### T1 配置加载（M7 离线部分）——离线

| # | 步骤 |
|---|---|
| T1-1 | 写 `mcp-servers.yaml` 实验稿（002 §2.2 全文：everything + filesystem 两条，env 占位符 `MCP_SPIKE_PROBE` 不设缺省值） |
| T1-2 | 写 `McpServersYamlLoader`（002 §3.2：env 参数注入式、`${VAR}` 解析、缺失清晰报错、非 stdio transport 跳过并记录、env 日志 ≤5 位前缀） |
| T1-3 | 写 `M7ConfigTest` 离线用例并跑绿：四字段解析、name 唯一校验、占位符存在→解析 / 缺失→清晰报错、非 stdio 值跳过留痕、日志前缀断言 |

T1 完成判定：002 §4 M7 行的离线判定全过；非 stdio 跳过记录已落日志（M1 辅助证据）。证据：`logs/m7-config.txt`（测试输出重定向）。

### T2 连接与工具发现（M3 + M7 通路）——live

| # | 步骤 |
|---|---|
| T2-1 | 写 `SpikeMcpClients`（002 §3.3：ServerParameters + StdioClientTransport + `McpClient.sync(...)` + initialize + requestTimeout 实测记录；单 server 失败跳过不阻断） |
| T2-2 | 写 `McpToolAdapter`（002 §3.5：tools/list → AgentOSTool 四方法映射、批量注册、subset 过滤对接） |
| T2-3 | export `MCP_SPIKE_PROBE=probe-value-2026` 后写并跑 `M3DiscoveryTest`：`mvn test -Dtest=M3DiscoveryTest > logs/m3-discovery.txt` |
| T2-4 | 从日志核对 M7 通路证据：加载后的 env 值进入子进程（everything server 的 tools/list 结果与探测相关的返回落盘；回显手段以该 server 实际工具为准，tools/list 落盘即最低证据） |

T2 完成判定：002 §4 M3 行全过——两 server 连接成功、工具逐个映射（四方法非空、schema 可解析）、批量注册数 = listTools 返回数、subset 过滤正确。证据：`logs/m3-discovery.txt`。

### T3 工具调用往返（M4）——live

| # | 步骤 |
|---|---|
| T3-1 | 写 `M4RoundtripTest` 并跑：`mvn test -Dtest=M4RoundtripTest > logs/m4-roundtrip.txt` |
| T3-2 | 三形态记录（002 §3.5）：成功调用（success=true + 内容要素）；失败调用（错误参数 / 不存在工具 → success=false + error 非空）；超时（requestTimeout 调短 → 超时异常映射 ToolResult）；多段 content 的拼接结果原样留档（开放项 5） |
| T3-3 | 按 002 §3.9 打审计口径日志行（tool_name / input_json / success / error_message / duration_ms） |

T3 完成判定：002 §4 M4 行全过——成功 / 失败 / 超时三种 ToolResult 映射齐备；retryable 初值判定有实据（开放项 6）。证据：`logs/m4-roundtrip.txt`。

### T4 手动循环闭环（M5）——live，需 key

| # | 步骤 |
|---|---|
| T4-1 | `source ~/.agent-os-poc/script/agent-os-env.sh` |
| T4-2 | 写 `McpToolCallbacks`（002 §3.6 候选一：`SyncMcpToolCallback` / `McpToolUtils.getToolCallbacksFromSyncClients`） |
| T4-3 | 写 `ManualLoop` + `LoopResult` + `ThinkStripper`（002 §3.7 / §3.8：按 007 README D3 五步路径重写、不拷代码；循环内维护 MCP 工具执行计数） |
| T4-4 | 写 `M5LoopClosedTest` 并跑（两候选各一次）：`mvn test -Dtest=M5LoopClosedTest > logs/m5-loop.txt` |
| T4-5 | 断言核对：`strip(finalText)` 含工具结果要素；`internalToolExecutionEnabled(false)` 生效；执行计数 = 模型发起轮数（无双执行）；候选一 / 候选二对比记录（schema 等价性、执行归属），定一条（002 §3.6） |

T4 完成判定：002 §4 M5 行全过，且至少一个候选闭环成功。证据：`logs/m5-loop.txt`（两候选分段记录）。

### T5 失败模式（M6）——live

| # | 步骤 |
|---|---|
| T5-1 | 写 `M6FailureModesTest` 并跑：`mvn test -Dtest=M6FailureModesTest > logs/m6-failure.txt` |
| T5-2 | ①坏命令（command 指向不存在程序）→ 连接阶段异常形态落盘，其余 server 不受影响（002 §3.3） |
| T5-3 | ②运行中 kill server 子进程 → 调用异常形态 + SDK 重连行为（有无自动重连）落盘 |
| T5-4 | ③requestTimeout 调短 → 超时表现与 SDK 默认值记录（开放项 8；正式实现超时对齐 TS 7.4 的说明一并写入） |

T5 完成判定：002 §4 M6 行三点齐备，核心阶段最小行为定义有成文素材（对齐 TS 8.2 / TS 4.2 / TS 7.4 的参照已在 001 M6）。证据：`logs/m6-failure.txt`。

### T6 starter 相容性（M2 附带项，组合 D）

| # | 步骤 |
|---|---|
| T6-1 | test scope 引入 `org.springframework.ai:spring-ai-starter-mcp-client`（002 §1.2 组合 D；不污染主线 main 依赖） |
| T6-2 | **先测污染面**：不写任何 `spring.ai.mcp.*` 配置，跑一次既有离线测试——若 starter 自动装配默认激活（容器里出现 starter 的连接 Bean / 尝试建连），test scope 引入即污染主线，T6 改用独立测试类 + `@SpringBootTest(properties 显式关闭)` 隔离手法；若默认不激活（预期，Spring AI 该 starter 有显式 enabled 开关），按 T6-3 继续。实测结果落日志（开放项 4 素材） |
| T6-3 | 写 `src/test/resources/application-starter.yaml`（`spring.ai.mcp.client.enabled=true` + stdio connections 指向 everything server，002 §2.1 说明），按 T6-2 定的手法激活 |
| T6-4 | 跑 starter 上下文：自动装配的 ToolCallback 是否出现、与自持 `McpServersYamlLoader` + `SpikeMcpClients` 是否抢连接（同一 server 被拉起两次即冲突实证）：`mvn test -Dtest=StarterProbeTest -Dstarter.probe=true -Dgroups=live > logs/m2-starter.txt`（编排偏差备注：探测从 M2DependencyMatrixTest 分支改为独立类 StarterProbeTest——starter 配置必须类级 properties，放 M2 类会激活离线测试的 starter 连接） |

T6 完成判定：002 §4 M2 附带项——starter 的 enabled 默认值、自动装配与自持管理的相容 / 冲突形态记录成文（开放项 4）；冲突且不可关 → 按 002 §1.3 记"starter 不引入"。证据：`logs/m2-starter.txt`。本任务失败不阻断 T7 前主线。

### T7 条件支线：组合升降级（A→B→C）

| 触发 | 任务 |
|---|---|
| M3 / M4 / M5 在组合 A 下失败（且失败归因 SDK / spring-ai-mcp，非实验代码缺陷） | ①切组合 B：dependencyManagement 钉 `io.modelcontextprotocol.sdk:mcp` = 0.18.4（三构件同钉，002 §1.2），重跑 T0-4 → T1 → T2 → T3 → T4，证据文件加 `.b-0184` 后缀；②0.18.4 败 → 依次 1.1.4 → 2.0.1（同法，后缀 `.b-<ver>`）；③B 全败 → 组合 C：仅引 `io.modelcontextprotocol.sdk:mcp`、去 spring-ai-mcp，M5 只能走候选二（002 §1.3），重跑同序，后缀 `.c` |
| 全程判定 | 每档依赖树照 T0-4 三检查留档（`logs/m2-tree-<组合>.txt`）；Jackson 冲突出现时按 002 §1.2 记录对治（`mcp-json-jackson2`） |

### T8 条件支线：M8 手写 JSON-RPC 降级

| 触发 | 任务 |
|---|---|
| SDK 路径全被证伪（A/B/C 均无法支撑 M3-M5） | 写 `M8FallbackProbeTest`：对 everything server 手写最小 stdio JSON-RPC 客户端（initialize 握手 + tools/list + tools/call 三步，001 M8）；实现量（代码行数）落盘 `logs/m8-jsonrpc.txt`；跑通即按 001 M8 记技术债素材——DA 13 验收标准一字不动 |

### T9 结论落盘（README.md + 联动清单对账）

| # | 步骤 |
|---|---|
| T9-1 | 写 `spike/008-mcp/README.md`（格式照 007 README：D 决议编号**本文档内从 D1 起**，实名引用如"spike/008-mcp/README.md 的 D1 决议"，不与 007 的 D1-D4 混淆） |
| T9-2 | 按结论编号回填 001 §5 九条必须记录的结论（逐条对号：M2→结论 1、M1→结论 2、M3→3、M4→4、M5→5、M6→6、M7→7、M8→8、失败修复记录→9） |
| T9-3 | 附"结论 → 正式实现落点"映射表（正式落点候选：TS 1.2 第 8 项 / TS 6.4 / TS 13 第二周条目，见 001 §6.2） |
| T9-4 | 001 §6.2 联动清单命中情况逐条列明（哪条被触发、哪条不触发及原因）；**本 spike 不执行任何 docs 修改**——联动修改是结论经评审确认后的独立动作（001 §6.3） |
| T9-5 | 001 §6.1 八条不变项逐条自查，结果写入 README 尾节 |

## 4. 命令速查

```bash
cd spike/008-mcp                                      # 所有命令的前提
source ~/.agent-os-poc/script/agent-os-env.sh         # live 任务前置（T4 起）
mvn test -DexcludedGroups=live                        # 离线快跑（T0/T1 及各组合 T0-4）
mvn test -Dgroups=live                                # 只跑 live 项
mvn test                                              # 全量（需 key + server）
mvn test -Dtest=M3DiscoveryTest                       # 单测一类
mvn dependency:tree -Dverbose > logs/m2-tree-a.txt    # 依赖树留档（按组合换文件名）
```

## 5. 证据留档约定

- 目录：`spike/008-mcp/logs/`（git 忽略）
- 清单：`m2-tree-a.txt`（及 `.b-0184` / `.b-1114` / `.b-201` / `.c` 各档）、`m2-starter.txt`、`m7-config.txt`、`m3-discovery.txt`、`m4-roundtrip.txt`、`m5-loop.txt`、`m6-failure.txt`、`m8-jsonrpc.txt`（条件触发时）
- 测试输出重定向即留档；组合切换的复跑证据用文件名后缀区分，不覆盖

## 6. 不可违背约束（违反即停）

> 盘点来源：根 CLAUDE.md（非协商原则 / 模型接入环境变量 / 工具使用指南）、TS 1.1 / 1.2 / 3.2 / 4.2 / 6.7 / 7.4 / 8.2 / 8.8、DA 13、定稿（docs/design/detail-supplement/001-model-config-export.md）、007 README D1-D4、001-req §6、002 §5。本节是执行期速查，仲裁以各出处原文为准。

### 6.1 类库与技术选型（锁死项）

| # | 项 | 锁定值 | 禁止 | 出处 |
|---|---|---|---|---|
| 1 | parent / JDK | `spring-boot-starter-parent:3.5.16` + Java 21 | 禁改 parent、禁升 Boot 4.x / 降 3.4.x | 007 README D1；根 CLAUDE.md 原则 1 |
| 2 | Spring AI 版本线 | 1.1.x（spring-ai-bom 1.1.2 import），全程不动 | **Spring AI 2.0 线禁入**——`internalToolExecutionEnabled` 是 Spring AI 框架"自动执行工具"的开关（本项目的红线是关掉它、由自研循环自己执行工具，见 6.2 第 1 条）；Spring AI 2.0 删除了该开关，`.internalToolExecutionEnabled(false)` 这行代码在 2.0 下直接编译不过。T7 升降级只允许动 MCP SDK 版本，**任何情况下不动 Spring AI 版本** | 007 D2；agentos/CLAUDE.md 第 1 章（锁定 Spring AI 1.1.x） |
| 3 | 模型连接器 | 仅 `spring-ai-starter-model-openai` 一条模型连接线（"腿"= 一条协议接入线；OPENAI 腿 = 经 OpenAI 协议接入，当前指向 MiniMax 的 OpenAI 兼容端点，缺省模型 MiniMax-M2.7；只引这一条已经 2026-09-14 用户裁决） | 禁再引第二条腿（anthropic 腿 = Anthropic 协议接入线）、禁引 SAA 自家 connector、禁引 `spring-ai-starter-model-minimax`（MiniMax 接入路径重验是 spike/007 第二组 002-req 的 V0-V8 另案，与本 spike 无关） | 002 §1.2 / §2.3；定稿 §2.1；007 002-req §3.2 |
| 4 | SAA 参与方式 | 仅 `spring-ai-alibaba-bom:1.1.2.0` 双 import 管版本 | 禁引 SAA 任何 connector、graph-core 等 SAA 构件（007 的 graph-core 是其 E1 探针专用，本 spike 无此需求） | 007 D2；002 §1.2 |
| 5 | MCP Java SDK 版本 | 组合 A 默认 0.17.0；T7 档位仅限 002 §1.2 所列 0.18.4 / 1.1.4 / 2.0.1 | 版本一律以 Maven Central（repo1.maven.org）为准，禁以 MCP Java SDK 官方文档站显示的版本号当坐标——该站文档 URL 固定挂在 latest-snapshot 快照路径下，版本号滞后于 Maven Central（2026-09-14 时点：文档快照显示 0.17.2，Central 当前 release 已是 2.0.1）。另注意：MCP SDK 的 2.x **不受**"Spring AI 2.0 禁入"约束——两者是不同项目的版本线，勿混淆 | 002 §1.2；001 §1.2；2026-09-14 Context7 与 repo1 对照实查 |
| 6 | YAML 解析 | SnakeYAML（Spring Boot 3.5 starter 传递自带，与正式技术栈同库） | 禁新引 Jackson YAML / snakeyaml-engine / 自写解析器 | TS 1.2 第 6 项（正式栈明文 SnakeYAML） |
| 7 | JSON 序列化 | 组合 A 用依赖树现成 Jackson 2（com.fasterxml.jackson，Boot 3.5 官方线）；若 T0-4 检查 3 发现树中无 Jackson 2，补引 `jackson-databind`（版本由 spring-ai-bom / Boot BOM 管理） | 禁自引 Gson / Jackson 3（tools.jackson）/ 手写字符串拼 JSON；T7 切组合 B/C 后依赖树若换 Jackson 线，JSON 库随选定组合走并如实记录（联动 002 开放项 3） | Boot 3.5 / Spring AI 1.1.x 依赖树实况；002 §1.2 注意事项 |
| 8 | 日志 | SLF4J + Logback（starter 自带） | 禁自引其他日志门面 / 实现 | TS 1.2 第 9 项 |
| 9 | 模型端点 | base-url 明文写 `https://api.minimax.cn`，**不带 /v1** | 禁把 `OPENAI_BASE_URL`（SDK 惯例带 /v1）直接映射给 `spring.ai.openai.base-url`——会 `/v1/v1` 双写 404 | 定稿 §4.4 / §5.3；007 实测坑 |
| 10 | Provider 映射 | 按 provider 名显式取用（本 spike 单模型，天然满足） | 禁类型扫描容器 Bean——T4 构造 ChatModel 时守住 | TS 3.2 |

### 6.2 行为红线（执行禁令）

| # | 禁令 | 出处 |
|---|---|---|
| 1 | 禁自动 tool 执行：`internalToolExecutionEnabled(false)`，任何组合不得为跑通而放开 | 根 CLAUDE.md 原则 4；TS 1.1 决策二；002 §5 红线 4。M5 若定候选一（SyncMcpToolCallback，定义见 002 §0.1 / §3.6），涉及 TS 1.1 决策二与根 CLAUDE.md 原则 4 的 Spring AI 使用边界枚举增列——已按 001 §6.2 第 7 条预登记，属结论后的文档联动，不构成执行违规 |
| 2 | 线程红线：全链路只用同步 API（`McpClient.sync` + 阻塞 `chatModel.call`）；禁 `@Async`、禁跨线程 `CompletableFuture`、禁把 SDK 异步客户端引入循环——SDK 内部做输入输出读写的线程不执行循环步骤；ProfileContext（正式实现中存放"当前是哪个 Agent"的 ThreadLocal 变量，见 TS 4.2）要求循环全程不换线程 | TS 4.2；002 §5 红线 8 |
| 3 | 密钥红线：密钥只从环境变量读；仓库内任何文件只写 `${环境变量名}` 占位符；日志与命令行最多前 5 位前缀；`MCP_SPIKE_PROBE` 探测值非密钥、不受限 | TS 8.8；根 CLAUDE.md「模型接入环境变量」；002 §5 红线 5 |
| 4 | MCP 工具不挂 SandboxChecker 校验（spike 不写任何 Sandbox 代码）；审计走 002 §3.9 统一口径日志行、不自建第二套留痕 | TS 6.6 / 6.7；001 §6.1 第 1/3 条 |
| 5 | 超时参数纪律：spike 里 requestTimeout 的各取值都是**实验参数**（测默认值、调短测超时），不是预算定案——正式实现必须走 TS 7.4 三档分步预算（application.yaml 默认 + Profile 覆盖），README 记录时写明这层区别 | TS 7.4；AG 3.4 |
| 6 | spike 执行期间不改 docs/design/ 与根 CLAUDE.md（001 §6.3 冻结令）；README 里的"措辞建议"是建议，不是修改动作 | 001 §6.3 |
| 7 | T7 升降级命中后，剩余任务在新组合下跑完再下结论，不半途混档 | 007 plan 第 5 节同构 |
| 8 | pom 永不进根 `<modules>`；构建只在 spike/008-mcp/ 目录内执行；007 的 src 与 logs 封存不改不拷 | spike/CLAUDE.md；002 §5 红线 9 |
| 9 | 查 SDK / Spring AI / server 文档用 Context7 或只读浏览；firecrawl 仅在提示词显式指定时使用 | 根 CLAUDE.md「工具使用指南」 |
