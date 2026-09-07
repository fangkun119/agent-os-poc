# Spike 实验流程：自研 ReAct 循环 + Spring AI 版本锁定验证

> 位置：`spike/007-react-loop/spec/001-expirement.md`
> 创建：2026-09-06 · 状态：待执行
> 下游文档：`002-spec.md`（实验代码的规格说明，下称 002）、`003-plan.md`（实施计划，下称 003）。两份文档都从本文档派生，派生所需信息见第 10 节索引。

## 0. 文档定位与术语

### 0.1 本文档回答三件事

1. **验证什么**（第 2、5 节）
2. **怎么验证**（第 5、7 节）
3. **结果怎么判定**（第 8 节）

分工边界：本文档不定义代码结构（002 的事）、不排任务（003 的事）。但本文档提供 002 和 003 需要的全部原材料，做到"只读本文档加官方文档，就能写出 002 和 003"。

这个 spike 的产出主要是 4 项技术决议（见第 2 节）。实验代码只是拿到证据的手段，实验做完就封存，不再维护。

### 0.2 术语表（本文档中每个术语按此含义使用）

| 术语 | 含义 |
|---|---|
| ReAct 循环 | "推理→行动"循环：模型先判断要不要用工具；要用就发起工具调用请求，程序执行工具、把结果还给模型；模型继续推理，直到给出不含工具调用的最终回答 |
| 自动执行（框架自动执行工具） | Spring AI 框架的默认行为：模型发起工具调用后，框架在模型调用内部自动执行工具并自动回灌结果，不经过调用方代码 |
| 双执行 | 同一次工具调用被执行两次：框架自动执行一遍，自研循环又执行一遍。本项目红线，必须避免 |
| BOM | 依赖版本清单（Maven 的一种 pom 文件），引入后统一管理一组依赖的版本号，避免逐个写版本 |
| parent（父 pom） | Maven 工程的父级配置，统一管理子工程的插件与依赖版本。本工程根 pom 的 parent 是 spring-boot-starter-parent，决定全工程 Spring Boot 版本 |
| starter | Spring Boot 的开箱即用依赖包：引入一个 starter 就带齐某项能力所需的全部依赖和默认配置 |
| Spring 上下文（Spring 容器） | Spring 程序的运行时环境。"上下文能启动"= 依赖组合能一起正常工作 |
| token 用量（usage） | 模型调用消耗的文字计量数（输入/输出各多少 token）。token 是模型计量与计费的最小文字单位 |
| 工具调用（function calling） | 模型不直接回答，而是返回一个结构化请求："请帮我执行某工具，参数如下"，由程序执行后再把结果给模型 |
| 审计对齐 | 每次 LLM 调用能取到一份独立的记录（provider、模型、token 用量、耗时），做到"一次模型调用 ↔ 一条审计记录"一一对应（纪要 8.1(2) 的设计论据） |
| 主线 / 支线 | 主线 = 必做实验路径（E1-E8）；支线 = 条件触发的备选路径（E9 仅当主线实测失败时触发） |

## 1. 背景与动机

评审纪要（docs/review/001-tech-review-based-on-requirement.md）以风险 R1 锁定主决议（**2026-09-06 晚决议变更**，此前为 1.0.0.2 线）：**SAA 1.1.2.0 + Spring AI 1.1.2 + Spring Boot 3.5.x**（Spring AI Alibaba 下称 SAA）。关键事实：`internalToolExecutionEnabled` 开关在 1.0.x 与 1.1.x 都存在且默认开（Spring AI v1.1.8 官方文档核验），2.0 才删除——所以真正不可用的只有 2.0 线，1.0.x / 1.1.x 都是可用线，选择依据是配套证据与工程适配（见 4.1 与纪要 2.6 节）。注意：锁的是实现版本线，不是"设计在 2.0 不可行"——自研循环在 2.0 仍是官方支持的形态，只是换实现路径。

这个论断已经在 2026-09-06 用 Context7 官方文档逐条核验坐实（见纪要 2.5 节和验证报告 chat/temp/20260906-spring-ai-alibaba-api-validation-check.md）。

文档核验只能回答"官方文档是怎么写的"。这个 spike 用真实代码验证同一批结论，并回答文档核验回答不了的两个问题：

1. 依赖组合 **SAA 1.1.2.0 + Spring AI 1.1.2 + Spring Boot 3.5.16 + JDK 21** 能不能一起正常工作——1.1.x 官方配套就是 Boot 3.5.x，预期 parent 3.5.16 直接可用，由 E1 依赖树确认（原"降 parent 还是实测兼容"的 D1 决策随主线切换消失）；
2. "关闭自动执行 + 自研 ReAct 循环"按官方标准路径写出来，是否真的工作。

## 2. 决议清单（spike 必须回答的问题）

| # | 待决议问题 | 结论去向 |
|---|---|---|
| D1 | 确认 parent 3.5.16 与 1.1.x 组合直接可用（官方配套即 Boot 3.5.x）；若 E1 失败按 E9 切 1.0.x 对照线（SAA 优先 1.0.0.4，parent 降 3.4.x） | 根 pom.xml + 纪要 R1 风险表补实测结论 |
| D2 | 依赖坐标清单确认（两个 BOM 怎么引、starter 坐标是什么、关闭自动执行的开关可用） | agentos-provider/pom.xml（正式实现第一周引入依赖时照单引入） |
| D3 | 手动循环的标准代码形态实证（走 `ToolCallingManager` 的官方标准路径）；如与纪要 8.1(3) 的落地要点有偏差，回填修正 | 纪要 8.1(3) + README 代码片段 |
| D4 | Provider 显式映射模式实证：按"provider 名 → ChatModel 实例"建映射表取用（不做类型扫描），并验证两种模型接入路径：OpenAI 兼容接口配 base-url（接口地址）、Anthropic 兼容接口配 base-url——两条腿当前都由 MiniMax 兼容端点模拟，对应两个不同的 Spring AI 连接器栈 | README 样例代码，供正式实现 `ProviderService` 参考 |

## 3. 范围与非范围

**范围**：第 5 节实验矩阵。E1-E7 必做；E8 在有两条可用接入路径时必做（两条腿 = MiniMax OpenAI 兼容腿 + Anthropic 兼容腿，单把 key 两条腿都通，见第 6 节与 docs/design/detail-supplement/001-model-config-export.md），只有一条路径可用时 E8 记"部分执行"；E9 在 E1 失败时触发（切 1.0.x 对照线）。

**非范围**（出现以下内容即越界）：

- 不实现、不验证 Memory、Session、SQLite、Web 端点——这些分属正式实现第一周（内存 Session）、第二周（Memory）、第三周（SQLite 与 Web 端点），不在本 spike 范围
- 不碰 SAA 的 Graph 多智能体编排框架（纪要 8.1(3) 论断 5 已明确绕开）
- 不验证流式响应（stream）——设计文档的 ReAct 循环走同步 `call`，流式不在第一周范围
- 不做重试、超时预算的框架化实现——三档超时预算属正式实现红线；spike 只在循环里放一个迭代上限防死循环
- spike 代码不回灌主工程——只回灌结论与样例代码片段（spike/CLAUDE.md：实验结束在 README.md 写结论）

## 4. 技术背景与关键事实（写代码时引用）

### 4.1 版本配套表（2026-09-06 文档复核修正后，与纪要 8.2 选型表一致）

| SAA | Spring AI | Spring Boot | 本 spike 角色 |
|---|---|---|---|
| **1.1.2.0**（**主线**；官网版本页逐字"当前推荐"，2026-09-06 决议变更） | 1.1.2（spring-ai-bom 1.1.2，官方 quickstart 双 BOM 同款） | **3.5.x**（工程 parent 3.5.16 直接可用） | **主线**（E1-E8）。开关已在 Spring AI v1.1.8 文档同构核验：存在、默认开、手动循环官方标准路径与 1.0.x 一致 |
| 1.0.0.2（对照备选线；1.0 GA 首版） | 1.0.x（配套 Spring AI 1.0.0 有 POM 依赖清单硬证据；POM=Maven 的依赖描述文件） | 3.4.x（官方配套口径，精确补丁版本官方文档查无原文） | **支线 E9**（仅当主线 E1 失败时切换回归） |
| 2.0.0-M1.1（里程碑预发布版） | 2.0.0-M1 | 4.0.0 | **禁入**：开关已删除，且预发布版叠预发布版风险高 |

补丁现状（Maven Central，2026-09-06 查）：1.1.x 补丁已到 1.1.2.3；1.0.x 有 1.0.0.3 / 1.0.0.4，另存在 CVE 补丁 1.0.0.3-20260305-cve（提示 1.0.0.2 附近存在已知漏洞修复）——E9 对照线触发时**优先用 1.0.0.4**（同线更高补丁、含修复），1.0.0.2 为已深度核验的备选。

### 4.2 关键 API 事实（已经文档核验；v1.0.3 与 v1.1.8 双版本线同构核验，2026-09-06；出处：纪要 8.1(3) 复核修正后 + 验证报告 §3 + 纪要 2.6 节）

1. **关闭自动执行**：`ToolCallingChatOptions.builder().toolCallbacks(...).internalToolExecutionEnabled(false).build()` 构造调用选项对象，作为参数传给 `chatModel.call(...)`。默认行为是自动执行（官方升级说明原文："In Spring AI 1.x, every ChatModel implementation contained its own internal tool-execution loop"）。
2. **判断模型是否请求了工具**：`chatResponse.hasToolCalls()`。
3. **执行工具的官方标准路径**：调用 `toolCallingManager.executeToolCalls(prompt, chatResponse)` 执行模型请求的工具；从返回结果取 `conversationHistory()`（已追加工具结果的完整消息列表），用它重建下一轮发给模型的完整输入，再次调用模型。手工构造 `ToolResponseMessage` 属"官方文档允许但没有示例"的路径，spike 不采用（A3 收紧项）。
4. **工具注册**：Java 方法标 `@Tool(description = "...")` 注解（参数可加 `@ToolParam` 说明），经 `ToolCallbacks.from(new XxxTools())` 生成工具数组，放进调用选项。参数格式描述（schema，即告诉模型该工具需要什么参数、什么类型）由注解自动生成；关闭自动执行不影响参数描述的下发。
5. **双执行红线**：不关自动执行、又自建循环，工具会被执行两次（CLAUDE.md 第一红线）。E3 用执行计数器实证。
6. **token 用量可得**：`ChatResponse` 元数据含 token 用量。E7 实证审计对齐可行。
7. **已核验与待确认细节**。已核验（2026-09-06 Context7，v1.0.3 官方文档）：两个 starter 的精确坐标 `org.springframework.ai:spring-ai-starter-model-openai`、`org.springframework.ai:spring-ai-starter-model-anthropic`；OpenAI 属性名 `spring.ai.openai.api-key / .base-url / .chat.completions-path / .chat.options.model`；Anthropic 属性名 `spring.ai.anthropic.api-key / .chat.options.model`（手动构造路径 `new AnthropicApi(System.getenv("ANTHROPIC_API_KEY"))`）。已全部解决（2026-09-06 Context7，v1.0.3 官方文档）：
① 调用选项传入 `call` 的方法签名——`Prompt` 类持有 `List<Message> messages` + `ChatOptions chatOptions` 两个字段；官方示例：`Prompt prompt = new Prompt("...", chatOptions); chatModel.call(prompt);`（api/prompt.adoc、api/tools.adoc；Groq / Vertex 示例证明 per-call options 覆盖实例默认值，即三级模型选择的机制坐实）；
② OpenAI 腿 URL 拼接——Spring AI 的 OpenAiApi 自动追加 `/v1/chat/completions`，`spring.ai.openai.base-url` 必须写 `https://api.minimax.cn`（**不带** `/v1`，否则 `/v1/v1` 双写 404）；环境变量 `OPENAI_BASE_URL` 是 OpenAI SDK 惯例值（带 `/v1`），不映射给 Spring AI，base-url 非敏感直接写 yaml，仅密钥走环境变量；
③ `spring.ai.anthropic.base-url`——3 次 Context7 查询均未在 v1.0.3 文档页命中该属性行（api-key 与 chat.options.* 已命中）。决策：E8 第一轮按同构惯例写 yaml `spring.ai.anthropic.base-url: https://api.minimaxi.com/anthropic` 实测；若 starter 不生效（请求仍打到 api.anthropic.com），切换备选方案——手动构造 `AnthropicApi(baseUrl, apiKey)` + `AnthropicChatModel`，只用 `ANTHROPIC_API_KEY` 环境变量。**2026-09-07 E8 实测：属性生效**（anthropic 腿闭环到 MiniMax 端点，备选方案未启用）。

### 4.3 设计红线（spike 代码同样遵守）

- 禁用自动执行——本项目第一红线；E3 是对这条红线的直接实证
- Provider 显式映射：按"provider 名 → ChatModel 实例"建映射表取用，禁止类型扫描 Bean（CLAUDE.md 架构关键事实；E8 实证）
- 敏感凭证只走环境变量：配置文件里写 `${环境变量名}` 占位，密钥本身不写进任何文件、不进 git
- 构建与测试只在 `spike/007-react-loop/` 目录内执行 `mvn`；spike 的 pom 永不加入根 pom 的 `<modules>`（spike/CLAUDE.md 规则）
- 不使用 SAA 的 Graph 等高层编排抽象；不把 ChatClient 用作循环承载（最小化：只面向 ChatModel 接口编程）

## 5. 实验矩阵

**前置条件**：至少一个**支持工具调用的模型**的 API key——已满足：MiniMax 双兼容端点均官方支持工具调用（环境变量经 `source ~/.agent-os-poc/script/agent-os-env.sh` 加载，见第 6 节）。

| 编号 | 验证点 | 方法 | 通过标准 | 证据留档 | 决议 |
|---|---|---|---|---|---|
| E1 | 依赖解析 + Spring 容器启动（主线组合：SAA 1.1.2.0 + Spring AI 1.1.2 + Boot 3.5.16 + JDK 21） | 独立 pom 引入两个 BOM 与 starter；写一个"Spring 容器能启动"的测试；执行 `mvn dependency:tree -Dverbose`（打印依赖树，含版本仲裁结果）留档 | 容器启动成功；依赖树中 spring-ai 与 Spring Boot 相关坐标无 "omitted for conflict"（因版本冲突被弃用）标记；其余第三方坐标的版本仲裁逐条列出并写明接受理由；`com.alibaba.cloud.ai`（SAA）构件版本全部落 **1.1.2.0**——"版本决议"拆两个子问题分别证明：Spring AI↔Boot 共存靠 spring-ai-bom + starter，SAA 同线解析靠 SAA BOM 拉入依赖树 | 依赖树输出 + 测试通过 | D1、D2 |
| E2 | 关闭自动执行的开关存在 | 按 4.2 第 1 条写代码引用该开关 | 编译通过（实证 1.1.x 线开关存在；v1.1.8 文档已同构核验） | 编译成功 | D2 |
| E3 | 关闭自动执行生效；无双执行 | 注册一个带执行计数器的工具（返回固定内容），跑两组对照。组 1：开关保持默认（开），代码不写任何工具执行逻辑——计数增加，证明框架确实自动执行。组 2：开关关，工具只由我方代码经 ToolCallingManager 执行——计数只由我方调用产生。提问尽量引导模型只发起一轮工具调用 | 执行次数与该组日志里模型发起工具调用的轮数一致，且不存在我方代码之外的执行；出现框架侧执行（双执行）即判定失败并上报。模型多发起轮次时允许固定次数重跑，README 记录实际轮数 | 测试通过 + 执行日志 | D2 |
| E4 | 手动单工具闭环（ReAct 最小回合） | 开关关；准备一个工具（如查询假想天气，返回固定内容）；循环：call → hasToolCalls 判断 → executeToolCalls 执行 → conversationHistory 重建输入 → 再 call | 得到基于工具结果的最终自然语言回答；工具执行全部由我方代码触发（无框架侧执行） | 测试通过 + 完整对话日志 | D3 |
| E5 | 多轮工具链 + 迭代上限 | 设计需要连续调用 2-3 个工具的任务（上一个工具的输出是下一个工具的输入）；循环设迭代上限（与设计文档一致，默认 10） | 在上限内推进并正确终止；最终答案正确；每轮重建的输入无消息丢失、无重复 | 测试通过 + 逐轮日志 | D3 |
| E6 | 工具参数描述正确性 | 准备多参数工具（如 getWeather(city, date)，返回固定内容）；提问里给定明确的参数值（如"查北京 2026-09-07 的天气"） | 日志中实际收到的入参与给定参数值一致；偶发偏差允许固定次数重跑并记录 | 测试通过 | D2 |
| E7 | token 用量与耗时采集（审计对齐） | 每次 call 外裹计时；读取响应元数据中的 token 用量 | 能取到输入/输出 token 数与毫秒耗时；结构上可做到一次模型调用对应一条审计记录 | 采集样本输出 | D3 |
| E8 | 双 Provider 显式映射 | 建一个映射表（`Map<String, ChatModel>`）放两个实例，对应两条接入路径（两个不同的 Spring AI 连接器栈）：OpenAI 兼容 starter（spring-ai-starter-model-openai，base-url 指向 MiniMax OpenAI 兼容端点）一条；Anthropic 兼容 starter（spring-ai-starter-model-anthropic，base-url 指向 MiniMax Anthropic 兼容端点）一条。两条腿用同一把 key，分别跑 E4 同款闭环 | 两个实例都完成闭环；实例按名称从映射表取用（无类型扫描） | 测试通过 ×2 | D4 |
| E9 | 支线（仅当 E1 失败时执行）：切 1.0.x 对照线回归（SAA 优先 1.0.0.4） | 对照组合 = SAA 1.0.0.4（1.0.x 最新补丁，含 CVE 修复；1.0.0.2 为已核验备选）+ spring-ai-bom 1.0.0 + parent 3.4.x（1.0.x 官方配套）；重跑 E1-E4，通过后**继续在该组合下把 E5-E8 跑完** | E1-E8 在对照组合下全部通过，才切换对照线并按 001 §8 上报评审确认 | 同 E1-E8 | D1 |

## 6. 环境与前置条件

- **构建环境**：JDK 21；Maven ≥ 3.6（优先用工程自带的 mvnw 包装脚本，免装 Maven；版本口径为 2026-09-06 复核修正后的官方要求）。不预设阿里云镜像（官方文档未点名需要；仅在依赖拉取失败时按需配置并记录）。
- **网络**：可访问 Maven 中央仓库与所选模型的 API。
- **API key 环境变量约定**：统一经 `source ~/.agent-os-poc/script/agent-os-env.sh` 加载（脚本说明与密钥红线见仓库根 CLAUDE.md「模型接入环境变量」一节）。导出并使用的变量：`OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_DEFAULT_MODEL`、`ANTHROPIC_API_KEY` / `ANTHROPIC_BASE_URL` / `ANTHROPIC_DEFAULT_MODEL`（另导出 `OPENAI_MODEL_LIST` / `ANTHROPIC_MODEL_LIST`，逗号分隔的可用模型清单，仅用于校验与发现；运行时切换模型不靠环境变量、靠每次调用的 options 参数。脚本按 Provider 命名导出 `*_API_KEY` / `*_BASE_URL` / `*_DEFAULT_MODEL` / `*_MODEL_LIST` 四件套，缺省模型即 `*_DEFAULT_MODEL`，不存在 `OPENAI_MODEL` / `ANTHROPIC_MODEL` 这类旧名。变量 schema 与 Provider 命名规则详见 docs/design/detail-supplement/001-model-config-export.md）。当前无原生 OpenAI/Anthropic 账号，两条腿都由 MiniMax 兼容端点模拟（单把 key 两条腿都通）。密钥在配置文件里一律写 `${环境变量名}` 占位；base-url 非敏感，直接明文写 yaml（OpenAI 腿写 `https://api.minimax.cn`，**不带** `/v1`——`${OPENAI_BASE_URL}` 是 OpenAI SDK 惯例值、自带 `/v1`，映射给 `spring.ai.openai.base-url` 会 `/v1/v1` 双写 404，详见 docs/design/detail-supplement/001-model-config-export.md）。E1、E2 不做真实模型调用，可用带默认值的占位写法（如 `${OPENAI_API_KEY:placeholder}`）让容器在没有真实 key 时也能启动；若某 starter 缺 key 时容器起不来，如实记录，D1 按"依赖解析完成、容器启动未验证"的口径落 README。密钥不落仓库、日志与命令行最多输出前 5 位前缀。
- **模型要求**：必须支持工具调用——已官方文档核验（MiniMax OpenAI 兼容端点有 function calling 官方示例；Anthropic 兼容端点 OpenAPI 含 tools + tool_choice）。实际组合：OpenAI 兼容腿 `MiniMax-M2.7`（备选 MiniMax-M3 / M2.7-highspeed）+ Anthropic 兼容腿 `MiniMax-M3`。写代码注意：MiniMax-M3 的思考内容默认混在返回文本的 `<think>` 标签里，最终回答断言前要先剥掉 `<think>...</think>` 再比对；Anthropic 兼容腿（E8 即用 MiniMax-M3）用 `thinking: adaptive` 参数控制思考输出，断言前同样先剥离 `<think>...</think>`（详见 docs/design/detail-supplement/001-model-config-export.md 5.4 节「MiniMax 工具调用与思考标签」）。
- **防挂死兜底**：实验代码对所有真实模型调用设宽松超时（读超时 60 秒量级），或给测试进程设全局超时（如 surefire 插件的 forkedProcessTimeoutInSeconds）。这只防实验挂死（调用卡住导致测试停不下来），不是三档超时预算的框架化实现——那是正式实现的红线范围。
- **目录纪律**：所有命令在 `spike/007-react-loop/` 目录内执行（spike/CLAUDE.md）。

## 7. 实验步骤（阶段顺序）

```
阶段 0 骨架与依赖解析（E1、E2）
  → 阶段 1 自动执行对照实证（E3）
  → 阶段 2 手动循环（E4 单工具 → E5 多轮）
  → 阶段 3 工具参数描述正确性（E6）
  → 阶段 4 token 用量与耗时采集（E7）
  → 阶段 5 双 Provider 显式映射（E8）
  → 阶段 6 条件支线（E9 仅当 E1 失败：切 1.0.x 对照线）
  → 阶段 7 结论落盘（README.md + 回填动作）
```

- 阶段 0-5 线性推进：前一阶段的通过标准未满足，不进入下一阶段。
- E9 命中（E1 失败）时：阶段 1-5 的剩余实验（E5-E8）换到对照组合（SAA 1.0.x——优先 1.0.0.4 + Spring AI 1.0.x + parent 3.4.x）下继续执行，顺序不变。
- 每阶段的动作即第 5 节矩阵的"方法"列；具体命令（`mvn test`、`mvn dependency:tree` 等）由 003 展开为任务级步骤。
- 阶段 1 是全 spike 的枢纽：E3 不通过则后续全部无意义，直接按第 8 节的规则上报（区分开关问题与模型问题）。

## 8. 决议规则（实验结果 → 决定）

| 实验结果 | 决定 |
|---|---|
| E1 通过，E2-E8 无框架级失败 | D1 = parent 保持 3.5.16（1.1.x 官方配套即 3.5.x，实测确认）；纪要 R1 补记实测结论 |
| E1 失败，E9（含在对照组合下补跑的 E5-E8）全部通过 | 切 1.0.x 对照线（SAA 优先 1.0.0.4）：parent 降 3.4.x + spring-ai-bom 1.0.0；上报评审确认后更新 R1 |
| E1 失败，E9 也失败 | 上报。不得自行改用 2.0 版本线；也不得放弃"禁用自动执行"的设计——这是设计文档定死的根规则，要改它得走新一轮评审，不能在 spike 里自己拍板 |
| E2 编译不通过 / E3 出现框架侧执行（双执行） | 上报：与文档核验结论矛盾，需重查文档核验链；D2 记"未决" |
| E3 因模型侧原因失败（模型根本不发起工具调用，不是开关行为问题） | 换模型或改提问方式重试；重试仍失败则 D2 记"开关编译可用、行为未验证"，上报 |
| E4-E7 失败 | README 记最小复现。模型侧问题（不支持工具调用、输出不稳定）：换模型重试；框架侧问题：对应决议 D3 记"未决"，上报；不阻塞其他已通过项 |
| E8 部分执行（只有一条接入路径可用） | D4 记"部分成立"：映射模式已验证、双路径接入未验证，README 写明缺哪条路径 |
| E4-E8 全部通过 | D3、D4 成立，按第 9 节回填 |

## 9. 产出物与回填动作

**README.md**（spike/CLAUDE.md 要求的结论落点），内容至少含：

1. 验证项打勾表：E1-E9 逐项标注通过/失败/未执行（未执行的写明原因）
2. 四项决议（D1-D4）及依据
3. 失败项的最小复现与分析
4. Provider 显式映射样例代码片段（D4 结论，供正式实现 `ProviderService` 参考）

**回填动作清单**（spike 结束后执行）：

| # | 动作 | 目标 |
|---|---|---|
| 1 | 按 D1 调整或保持 parent 版本 | 根 pom.xml |
| 2 | 按 D2 落依赖坐标清单（正式实现第一周引入依赖时照单执行；spike 本身不改主工程） | agentos-provider/pom.xml |
| 3 | 按 D3 校对纪要 8.1(3) 落地要点，有偏差则更新 | 纪要 |
| 4 | 按 D1 补 R1 实测结论 | 纪要风险表 |

## 10. 为 002 / 003 准备的信息索引

**002（规格说明）需要什么 → 在本文档的位置**：

| 002 需要什么 | 取自 |
|---|---|
| 验收标准全集 | 第 5 节矩阵"通过标准"列（逐项转成 002 的验收条目，E 编号作追溯 ID） |
| 依赖与版本 | 4.1 配套表 + E1 行；4.2 第 7 条的坐标与签名已于 2026-09-06 用 Context7 落定（结论见 4.2-7 ①②③） |
| 组件与结构建议 | 第 7 节阶段划分（建议按阶段拆测试类；一个带计数器的工具类；一个双 provider 工厂。002 有权调整结构，但验收标准不得弱于第 5 节矩阵） |
| 配置项与凭证 | 第 6 节（环境变量名、占位写法、.gitignore 要求） |
| 约束红线 | 4.3 |

**003（实施计划）需要什么 → 在本文档的位置**：

| 003 需要什么 | 取自 |
|---|---|
| 任务顺序与依赖 | 第 7 节阶段流（阶段 0→7 线性；E9 为条件分支） |
| 每个任务的操作内容 | 第 5 节"方法"列（003 展开为带命令的任务步骤） |
| 每个任务的完成判定 | 第 5 节"通过标准"列（003 不得弱化） |
| 证据留档 | 第 5 节"证据留档"列 + 第 9 节 README 汇总 |
| 分支与升级路径 | 第 8 节决议规则 |

**对 002 / 003 的生成约束**：

- 002 的验收标准必须覆盖矩阵全部必做项（E1-E7 必做，E8 按第 3 节条件口径，E9 写成条件任务），只增不减；为 E3-E6 这类涉及模型输出的实验固定确定性夹具（工具返回固定内容、提问给定明确期望值），断言以"执行次数 = 日志中模型发起工具调用的轮数"为准，不假设模型只调一轮
- 003 的每个任务必须有独立的"怎么算完成"：可执行的命令 + 预期输出
- 两份文档保留与 001 的 E 编号追溯关系

## 11. 来源文档对照

| 来源 | 本文档使用位置 |
|---|---|
| 纪要 8.2 / R1 / 2.4 Action Item / 2.5 复核修正 | 第 1、2 节；4.1；第 8 节 |
| 纪要 8.1（2026-09-06 复核修正后） | 4.2；第 5 节 |
| 验证报告 chat/temp/20260906-spring-ai-alibaba-api-validation-check.md（§3 证据、§5 spike 输入） | 第 1 节；4.1、4.2；第 8 节 |
| 仓库根 CLAUDE.md 红线（禁自动 tool 执行、凭证环境变量注入、显式映射禁类型扫描） | 4.3 |
| 模型接入环境变量定稿 docs/design/detail-supplement/001-model-config-export.md（变量 schema 与 Provider 命名、密钥红线、base-url 不带 /v1、MiniMax 工具调用要点；过程稿 chat/temp/20260906-model-config-export-design.md 已被其收编，不一致处以定稿为准） | 4.2 第 7 条、第 5 节前置条件、第 6 节 |
| spike/CLAUDE.md（目录纪律、README 结论要求） | 3、6、9 节 |
| 外部证据（GitHub Release Notes、Maven Central POM 与 maven-metadata、Spring AI v1.0.3 / v1.1.8 官方接口文档与升级说明、java2ai 官网版本页） | 4.1（1.0.x 线出处见验证报告 §3-B3、§3-A5；1.1.x 主线证据见纪要 2.6 节） |
