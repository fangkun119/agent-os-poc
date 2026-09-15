# Spike 结论：模型接入路径重验（007 第二组）+ 第一组结论存档

> 位置：`spike/007-react-loop/README.md`
> 说明：本 README 于 2026-09-15 **全新创建**（用户指示删除旧版，避免第一组残留影响第二轮结论）。第一节为第一组结论的**存档原文**（自 git 历史恢复，引用链"`spike/007-react-loop/README.md` 的 D1-D4"保持有效）；第二节为第二组结论（本轮产物）。
> 引用方式：结论请实名引用，如"`spike/007-react-loop/README.md` 的 D5 决议"。

## 0. 本文件是什么、为什么做这个实验（零背景读者从这里读）

**本 spike 回答的问题**：AgentOS 项目（Java 21 + Spring Boot 3.5.16 的企业级 Agent 运行时）接大模型时，连接层代码用谁的实现？2026-09-13 设计评审发现问题：设计文档写的是"基于 Spring AI Alibaba（下称 SAA，阿里巴巴对 Spring AI 框架的扩展）做模型调用"，但代码事实是"用的全是 Spring AI 官方自己的 starter（开箱即用依赖包），SAA 只提供了一个版本对齐清单"。为回答"要不要换成 SAA 的连接器"，做了本轮实验，实测三条候选路径：

| 路径 | 含义 | 裁决 |
|---|---|---|
| A | 维持现状：Spring AI 官方 openai / anthropic 两个 starter，借道 MiniMax 的协议兼容端点（第一组已验证） | 可行（现状） |
| B | Spring AI 官方 MiniMax 专用 starter（`spring-ai-starter-model-minimax`，走 MiniMax 原生协议） | **可行（本轮实测，见 D5）** |
| C | SAA 自家的 MiniMax 连接器 | **不存在**（V0 三路证据查实，出局） |

**执行基线**：2026-09-15 执行；JDK 21；Maven 3.9.10（全路径 `/opt/homebrew/bin/mvn`，在 spike 目录内执行）；Spring AI 1.1.2 + Spring Boot 3.5.16 + SAA BOM 1.1.2.0；模型供应商 MiniMax（国内站 `api.minimax.cn`）与智谱 GLM（Coding Plan 端点）。全部命令与证据文件在 `logs/r2/`（9 个日志）。

### 术语与文件指代（本 README 按此含义使用）

| 术语/指代 | 含义 |
|---|---|
| starter | Spring Boot 的开箱即用依赖包：引入一个坐标就带齐该能力的依赖与默认配置 |
| BOM | 依赖版本清单（Maven 的一种 pom 文件）：引入后统一管理一组依赖的版本号，自身不含代码 |
| SAA | Spring AI Alibaba（`com.alibaba.cloud.ai` groupId，阿里巴巴维护的 Spring AI 扩展） |
| 腿 | 一条可用的模型接入通道。本项目四条腿：腿 1 = openai starter → MiniMax 的 OpenAI 兼容端点；腿 2 = anthropic starter → MiniMax 的 Anthropic 兼容端点；腿 3 = minimax starter → MiniMax 原生协议端点；腿 4 = openai starter → 智谱 GLM 的 OpenAI 兼容端点 |
| 兼容端点 | 大模型厂商提供的"说别家协议"的接口：说 OpenAI 协议的 OpenAI 兼容端点、说 Anthropic 协议的 Anthropic 兼容端点 |
| 原生协议端点 | MiniMax 自家协议格式的接口，路径 `/v1/text/chatcompletion_v2`（与 OpenAI 兼容的 `/chat/completions` 是两套） |
| 四元组 | 每个接入通道一组四个环境变量：`*_API_KEY` / `*_BASE_URL` / `*_DEFAULT_MODEL` / `*_MODEL_LIST`（维护于仓库外脚本 `~/.agent-os-poc/script/agent-os-env.sh`） |
| E3 计数法 | 判"无双执行"的方法：给工具加执行计数器，计数器读数 = 模型发起工具调用的总个数即通过 |
| `<think>` 标签 | MiniMax-M3 模型把思考过程混在回答正文里，包在 `<think>...</think>` 中；断言前要先剥离 |
| V0-V9 | 第二组验证项编号（定义见 `spec/002-req.md` 3.1 节）；E1-E9 为第一组验证项编号 |
| D1-D4 / D5-D7 | 决议编号：D1-D4 = 第一组（第一节存档）；D5-D7 = 第二组（第二节） |
| req / spec / plan | 本 spike 的三份规格文档：`spec/002-req.md`（需求与验证项）/ `spec/002-spec.md`（代码规格）/ `spec/002-plan.md`（任务计划） |
| 定稿 | `docs/design/detail-supplement/001-model-config-export.md`（模型接入环境变量定稿） |
| 根 CLAUDE.md | 仓库根 `/CLAUDE.md` |
| agentos/CLAUDE.md | `agentos/CLAUDE.md`（Java 编码规范，spike 代码同样遵守，见 spike/CLAUDE.md「编码规范」节） |
| `llm_calls` | AgentOS 设计中的 SQLite 审计表：记录每次大模型调用（供应商、模型、token 数、耗时）；V6 验证的就是"能不能取到这些数" |
| surefire 180 秒兜底 | Maven 测试插件配置 `forkedProcessTimeoutInSeconds=180`：测试进程超 180 秒强杀，防挂死（spike 的超时替代方案） |

---

## 一、第一组结论存档（E1-E9 / D1-D4，2026-09-07 执行，原文恢复）

> 以下为第一组 README 原文存档（对应 git 历史 2a01bee 提交；第一组 `src/` 已于 2026-09-14 从工作区移除、代码全量保留于 git 历史，恢复用 `git checkout 2a01bee -- spike/007-react-loop/src`；`logs/` 8 个日志原地保留）。

**总体结论：采纳。** 主线组合全部实验通过（E1-E8 全绿，E9 未触发），四项决议（D1-D4）全部落定。

### 验证项打勾表

| 项 | 结果 | 关键证据 |
|---|---|---|
| E1 依赖解析 + 容器启动 | ✅ | 依赖树零冲突；graph-core 探针落 1.1.2.0；容器启动；`logs/e1-dependency-tree.txt` |
| E2 开关存在 | ✅ | `internalToolExecutionEnabled(false)` 编译通过并构建出选项对象 |
| E3 自动执行对照 | ✅ | 组 1（默认开）计数=1 → 框架确实自动执行；组 2（开关关+手动）我方执行=2、计数增量=2，精确吻合无双执行；`logs/e3-autoexec.txt` |
| E4 单工具闭环 | ✅ | 2 轮迭代，`toolCallRounds=[getWeather]`，最终回答含"晴，12°C"；`logs/e4-e5-loop.txt` |
| E5 多轮工具链 | ✅ | `[getCity, getDate, getWeather2]` 三步严格串行，4 轮迭代，未触上限；`logs/e5-loop.txt` |
| E6 参数描述正确性 | ✅ | 第 1 次尝试即命中：`city=北京, date=2026-09-07`；`logs/e6-args.txt` |
| E7 用量/耗时采集 | ✅ | usages=[in=359,out=115, in=494,out=74]，durationsMs=[6063, 2750]；`logs/e7-usage.txt`（注：仅 openai 腿，anthropic 腿由第二组 V6 补齐） |
| E8 双 Provider 映射 | ✅ | openai + anthropic 两腿各自闭环，按名取用无类型扫描；anthropic 腿实测 `spring.ai.anthropic.base-url` 属性生效（当时值为国际站端点）；`logs/e8-providers.txt` |
| E9 对照线回归 | 未触发 | E1 通过，无需切 1.0.x 对照线 |

### 四项决议（D1-D4）

| # | 决议 | 依据 |
|---|---|---|
| D1 | parent 保持 3.5.16（1.1.x 官方配套即 Boot 3.5.x） | E1 实测 |
| D2 | 依赖坐标清单：parent 3.5.16 + 双 BOM（`spring-ai-alibaba-bom:1.1.2.0` + `spring-ai-bom:1.1.2`）+ `spring-ai-starter-model-openai` / `spring-ai-starter-model-anthropic`（版本由 spring-ai-bom 管理，实际 1.1.2）+ starter-test。正式实现第一周在 agentos-provider 照单引入 | E1 |
| D3 | 手动循环官方标准路径实证成立：`ToolCallingChatOptions.builder().toolCallbacks(...).internalToolExecutionEnabled(false).build()` → `call` → `hasToolCalls()` → `executeToolCalls` → `conversationHistory()` 重建 → 再 `call` | E3-E5 |
| D4 | Provider 显式映射模式成立：`Map<provider 名, ChatModel>` 按名取用，无类型扫描 | E8 |

### 附带实测结论（第一组）

- `spring.ai.anthropic.base-url` 属性生效（当时值为国际站端点）
- MiniMax-M3 思考内容混在 `<think>` 标签里，ThinkStripper 剥离有效
- 双 BOM 协作无冲突

---

## 二、第二组结论：模型接入路径重验（V0-V9 / D5-D7，2026-09-15 执行）

> 执行依据：req（V0-V9 矩阵）→ spec → plan 三件套（见术语表）。
> 代码：100% 新写（Java 包 `spike.reactloop`：App、CountingTools、ManualLoop、LoopResult、ProviderRegistry、ThinkStripper 共 6 类；测试 7 个）；第一组 `src/` 已归档（方案 B）。
> **执行环境注记**：本机代理为 fake-IP 模式（任意假域名都会被"解析"，探针拿不到 DNS 异常），负向探针改用 `127.0.0.1:1`（本机必拒连、不走代理）。

### 验证项打勾表（V0-V9，9/9 通过）

| 项 | 测试类 | 结果 | 关键证据 |
|---|---|---|---|
| V0 SAA 有无 MiniMax connector | 无代码（检索） | ✅ C 出局 | 三路独立证据：java2ai.com（SAA 官网）MiniMax 页给的是 Spring AI 官方 starter；SAA 官方仓库文档自述模型支持只有 DashScope / OpenAI / DeepSeek；SAA 仓库完整树（无截断）"minimax"零命中——`logs/r2/v0-connector-search.txt` |
| V1 starter 存在性与版本仲裁 | V1BootstrapTest | ✅ | `spring-ai-starter-model-minimax:jar:1.1.2:compile`（被 spring-ai-bom:1.1.2 仲裁）；依赖树零冲突；容器启动，placeholder key 下 MiniMaxChatModel Bean 正常创建——`logs/r2/v1-resolve.txt`、`v1-dependency-tree.txt` |
| V2 属性面 | V2MinimaxPropertiesTest | ✅ | ① model / base-url / api-key 三属性绑定生效（缺省 MiniMax-M2.7、纯主机 base-url、真 key 注入）；② 负向探针铁证 `POST https://127.0.0.1:1/v1/text/chatcompletion_v2`——base-url 接线 + 原生路径拼接双实证；④ `spring.ai.minimax.chat.base-url` 属性存在、未设置时回落通用值——`logs/r2/v2-properties.txt` |
| V3 单工具闭环（路径 B 核心命题） | V3SingleToolLoopTest | ✅ | minimax 腿 2 轮闭环（`round=1:getWeather` → 最终回答含"晴，12°C"）；候选 A（纯主机）正向实证；新模型名 MiniMax-M3 经 options 传入被原生端点接受（老代 API 担忧排除）——`logs/r2/v3-tool-loop.txt` |
| V4 手动循环计数（E3 计数法） | V4ManualLoopCountTest | ✅ | 计数器 1 = 模型工具调用 1，无双执行；`internalToolExecutionEnabled(false)` 在 MiniMaxChatModel 上与 openai 腿行为一致——`logs/r2/v4-manual-count.txt` |
| V5 显式映射复验 | V5ProviderRegistryTest | ✅ | 四键（openai / anthropic / minimax / zhipu）并存；minimax 与 openai 两键各自闭环；Bean 名 `miniMaxChatModel` 确认——`logs/r2/v5-registry.txt` |
| V6 usage/耗时双腿 | V6UsageCaptureTest | ✅ | minimax：usages=[in=267,out=69, in=325,out=64]，durationsMs=[5238, 2580]；anthropic：usages=[in=477,out=41, in=282,out=48]，durationsMs=[4611, 3708]；一次调用 ↔ 一组样本，重试已关无污染；**anthropic 国内站 `api.minimax.cn/anthropic` 实测可达**（补齐第一组 E7 缺的第二协议样本）——`logs/r2/v6-usage.txt` |
| V7 `<think>` 剥离 | 内建于 V3/V4 | ✅（含差异记录） | 本轮 MiniMax-M3 经原生端点的回答**未混入** `<think>`（第一组 openai 腿 M3 混入）；ThinkStripper 保留（幂等无害）——`logs/r2/v3-tool-loop.txt` |
| V8 决议收口 | 无代码 | ✅ | 即本 README 第二节 |
| V9 双 OpenAI 协议实例并存 | V9DualOpenAIInstanceTest | ✅ | openai 腿（MiniMax 兼容端点）与 zhipu 腿（智谱 Coding Plan 端点，glm-5.3-flash）并存互不干扰、各自 2 轮闭环；tools/tool_calls 往返双端可用；mutate() 反射探测透出 completionsPath=true——`logs/r2/v9-dual-openai.txt` |

### 路径 B 属性表（V2 实测）

| 属性 | 实测表现 |
|---|---|
| `spring.ai.minimax.api-key` | ✅ 绑定生效（环境变量真值注入） |
| `spring.ai.minimax.base-url` | ✅ 纯主机即可（客户端自动追加原生路径 `/v1/text/chatcompletion_v2`）；负向探针证明请求确实打向所配 base-url |
| `spring.ai.minimax.chat.options.model` | ✅ 缺省 MiniMax-M2.7 生效；每次调用带 options 覆盖为 MiniMax-M3 成功 |
| `spring.ai.minimax.chat.base-url` | ✅ 属性面存在；未设置时回落通用 base-url |

### 决议（D5 起）

| # | 决议 | 依据 |
|---|---|---|
| D5 | **MiniMax 接入采路径 B**：`spring-ai-starter-model-minimax:1.1.2` + `spring.ai.minimax.*` 属性族 + base-url 写纯主机（客户端自动追加原生路径 `/v1/text/chatcompletion_v2`）；OPENAI / ANTHROPIC 两条腿保留，语义回归"真 OpenAI / Anthropic 协议腿、待原生账号"（req 3.3 第 1 行） | V1-V7 全绿 |
| D6 | **显式构造的 ChatModel 必须显式传 RetryTemplate**（Spring Retry 的重试模板，控制失败后重试几次、间隔多久）：手动 new 的 ChatModel 内置默认模板（10 次退避、最长 3min），不吃 `spring.ai.retry.*` 配置（那只注入自动配置创建的 Bean）——正式实现的 `ProviderService` 显式构造实例时照此办理（与 agentos/CLAUDE.md「配置纪律」章互为印证） | V2 首跑实证（探针被退避链拖满 surefire 180s 强杀） |
| D7 | SAA BOM 维持 1.1.2.0 不升级（1.1.2.2 / 1.1.2.3 差异仅记录，升级另行评审——req 1.3 口径） | 版本线现状核查 |

### 附带发现（供正式实现与后续排障）

1. 手动构造实例不吃 `spring.ai.retry.*`（见 D6）
2. 本机 fake-IP 代理会解析任意假域名——负向探针须用 `127.0.0.1:1` 这类确定性地址
3. MiniMax-M3 经**原生协议端点**的回答默认不含 `<think>` 思考正文（与 OpenAI 兼容腿不同）；剥离逻辑保留
4. `OpenAiChatModel` 在 1.1.2 **没有两参构造器**（v1.1.8 文档示例超前），显式构造需五参：api + options + toolCallingManager + retryTemplate + observationRegistry
5. `OpenAiApi.mutate()` 派生 builder 透出 completionsPath（反射探测 true）——多兼容端点场景可用官方 mutate 模式
6. 智谱 OpenAI 兼容端点路径布局非标准（`<base>/api/paas/v4/chat/completions`，无 `/v1` 段），必须用 `OpenAiApi.builder().completionsPath("/chat/completions")` 显式覆盖，否则 404

### 联动清单命中情况（req 5.2，路径 B 全绿触发；执行时机 = 评审确认后）

| req 5.2 条目 | 命中 | 状态 |
|---|---|---|
| 1 定稿 §2.3 MINIMAX 行（加 `spring.ai.minimax.*` 映射、删"无自动映射"句） | ✅ | 待执行 |
| 2 根 CLAUDE.md（MINIMAX 腿接线 + 是否结构性修订由用户裁决） | ✅ | 待裁决执行 |
| 3 007 README D2 注记 | ✅ | 本 README 已全新创建、两节结构，注记即本节 |
| 4/5 Q7 共 12 处表述（docs 四篇"基于 SAA 做调用"改为"MiniMax 原生 starter"口径） | ✅ | 待执行 |
| 6 AG §4.1 / §4.2 接线指引 | ✅ | 待执行 |
| 7 定稿 §2.1 + 根 CLAUDE.md 四元组语义 | ✅ | 待执行 |
| 8 DA 13 Provider 条目 | ✅ | 待执行 |
| 9 架构图 SVG 字样 | ✅ | 待执行 |
| 追加：V6 已实证国内站 /anthropic 可达 → spec §7.2 ⑥"待 V6 实测"解除 | ✅ | 定稿 / 根 CLAUDE.md 相应注记可随联动 wave 移除 |

### 执行记录

- 执行日期 2026-09-15；时间盒内完成（T0-T7 全部执行，未触发跳过分支）
- 环境实测备注：fake-IP 代理（负向探针改 127.0.0.1:1）；mvn 全路径 `/opt/homebrew/bin/mvn` 3.9.10；GitHub raw 经本机代理 127.0.0.1:15236 拉取源码核对
- 终验：更名清理（r2/R2 前缀移除）+ 编码规范改造（SLF4J 证据打印、Pattern 预编译、zhipu 受控重试模板）后全量 `mvn test -Dtest='V*'` → Tests run: 7, Failures: 0, Errors: 0
