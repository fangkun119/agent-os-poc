# Spike 实验代码规格说明（002-spec）

> 位置：`spike/007-react-loop/spec/002-spec.md`
> 创建：2026-09-07 · 状态：待评审（基于 2026-09-06 决议变更后的 1.1.x 主线重新生成；初版已删除）
> 上游：`001-expirement.md`（下称 001）；术语沿用 001 §0.2 术语表
> 平行输入：`docs/design/detail-supplement/001-model-config-export.md`（下称"定稿"）
> 下游：`003-plan.md`

## 0. 文档定位

- 002 回答两件事：**实验代码长什么样**（工程形态、组件、配置），**每项验收怎么判**（E1-E9 逐条代码层判定）。
- 验收标准以 001 §5 实验矩阵为源头，逐条编号追溯；002 只做代码层细化，**不得弱化** 001 的通过标准。

## 1. 工程形态

### 1.1 目录结构

```text
spike/007-react-loop/
├── pom.xml                                # 独立 Maven 工程（spike/CLAUDE.md 纪律：不进根 pom modules）
├── spec/                                  # 001 / 002 / 003
├── src/main/java/spike/reactloop/
│   ├── SpikeApp.java                      # @SpringBootApplication 最小启动类
│   ├── tool/CountingTools.java            # 带计数器、固定返回的工具类（E3 对照 / E4 / E6）
│   ├── tool/ChainTools.java               # 链式工具类（E5 多轮工具链）
│   ├── loop/ManualLoop.java               # 手动 ReAct 循环封装（E4 / E5 / E7 共用）
│   ├── loop/LoopResult.java               # 循环结果载体
│   ├── provider/ProviderRegistry.java     # Map<String, ChatModel> 显式映射（E8）
│   └── util/ThinkStripper.java            # <think> 标签剥离（断言前处理）
├── src/main/resources/application.yaml
└── src/test/java/spike/reactloop/
    ├── E1E2BootstrapTest.java             # E1 依赖解析 + 容器启动；E2 开关引用编译
    ├── E3AutoExecComparisonTest.java      # E3 开/关两组对照
    ├── E4SingleToolLoopTest.java
    ├── E5MultiToolChainTest.java
    ├── E6ToolSchemaTest.java
    ├── E7UsageCaptureTest.java
    └── E8ProviderRegistryTest.java
```

### 1.2 Maven 坐标与依赖（1.1.x 主线，全部经官方推荐核验，2026-09-07）

| 项 | 值 | 说明 |
|---|---|---|
| groupId / artifactId | `com.agentos.spike : react-loop-spike` | 独立工程，永不进根 pom `<modules>` |
| parent | `spring-boot-starter-parent:3.5.16` | 3.5 线最新补丁（Central 核验）；Spring AI 官方兼容声明"Spring AI 1.1.x is compatible with Spring Boot 3.5.x"（v1.1.8 README） |
| java.version | 21 | 环境实测 JDK 21.0.10；SAA 要求 JDK 17+、Spring AI 目标 Java 17+ |
| BOM | `org.springframework.ai:spring-ai-bom:1.1.2`（import） | 1.1 线最新；官网推荐行"Spring AI 1.1.2" |
| BOM | `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.1.2.0`（import） | 官网版本页逐字"当前推荐"；官方 quickstart 即此双 BOM 结构 |
| 依赖 | `org.springframework.ai:spring-ai-starter-model-openai`（版本由 spring-ai-bom 管理 → 1.1.2） | OPENAI Provider 连接器栈 |
| 依赖 | `org.springframework.ai:spring-ai-starter-model-anthropic`（同上 → 1.1.2） | ANTHROPIC Provider 连接器栈 |
| 依赖（探针，test scope，可选） | `com.alibaba.cloud.ai:spring-ai-alibaba-graph-core`（版本由 SAA BOM 管理 → 1.1.2.0） | **仅作"同线解析探针"**：让 SAA 构件出现在依赖树里验证 BOM 协作，不 import 其任何类（Graph 属 001 非范围）；见 §4 E1 |
| 依赖 | `org.springframework.boot:spring-boot-starter-test`（test） | JUnit 5 + Spring Test |
| 插件 | surefire `forkedProcessTimeoutInSeconds=180` | 防挂死兜底（001 §6），非三档超时预算 |

SAA 的 DashScope 连接器、agent-framework、studio、sandbox 等 SAA 构件不引入、不使用。

### 1.3 双 BOM 决策记录

- **双 BOM 现在是官方 quickstart 同款**：官网版本页"依赖管理（推荐使用 BOM）"给出的就是 `spring-ai-alibaba-bom:1.1.2.0` + `spring-ai-bom:1.1.2`（另列 extensions-bom 1.1.2.1——spike 不用 extensions，不引）。初版 002 曾把双 BOM 当"无法预核验的权宜"，1.1.x 核验后升级为"官方推荐模式"，E1 只需实测确认。
- **版本决议拆两个子问题**（沿用 001 E1 的判定）：① Spring AI↔Boot 共存靠 spring-ai-bom 1.1.2 + starter；② SAA 构件同线解析靠 SAA BOM + graph-core 探针。
- 技术注意：BOM import 本身**不会**把 SAA 构件拉进依赖树（BOM 只管版本、不加依赖）——这就是需要探针构件的原因；没有探针时依赖树不出现 `com.alibaba.cloud.ai` 属正常。

## 2. 配置

### 2.1 application.yaml 全文草稿（src/main/resources）

```yaml
spring:
  application:
    name: react-loop-spike
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:placeholder}        # 密钥：环境变量，E1/E2 离线项可用占位默认值
      base-url: https://api.minimax.cn              # 非敏感：明文 yaml，不带 /v1（定稿 5.3）
      chat:
        options:
          model: ${OPENAI_DEFAULT_MODEL:MiniMax-M2.7}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:placeholder}
      base-url: https://api.minimaxi.com/anthropic  # 属性名未获文档确认，E8 实测（见 §6 开放项）
      chat:
        options:
          model: ${ANTHROPIC_DEFAULT_MODEL:MiniMax-M3}
```

说明：

- 变量名与语义按定稿 §2（`OPENAI_*` / `ANTHROPIC_*` / `MINIMAX_*` 三组 Provider 四元组）。
- `placeholder` 默认值只服务 E1/E2；E3-E8 前先 `source ~/.agent-os-poc/script/agent-os-env.sh`。
- Anthropic base-url 属性名未获文档确认：先按同构惯例写 yaml，E8 实测请求是否打到 MiniMax 端点；不生效则切手动构造 `AnthropicApi(baseUrl, apiKey)`（001 §4.2-7③）。

### 2.2 模型组合（与 001 §6 一致）

| Provider | 模型 | 说明 |
|---|---|---|
| OPENAI | MiniMax-M2.7（备选 M3 / M2.7-highspeed） | E3-E8 主用 |
| ANTHROPIC | MiniMax-M3 | E8 第二腿 |

## 3. 组件规格

### 3.1 SpikeApp

- `@SpringBootApplication` 最小启动类，无业务逻辑；E1 的容器启动测试用。

### 3.2 CountingTools（E3 对照 / E4 / E6）

- 工具方法返回**固定内容**（确定性夹具，001 §10 生成约束）：
  - `@Tool getWeather(city, date)` → `"晴，12°C"`，参数 `@ToolParam` 说明；内置 `AtomicInteger` 计数器 +1，并记录最近一次入参
  - `@Tool getAdvice(weather)` → `"穿外套"`，同样计数与记录入参
- 暴露 `counts()` / `lastArgs()` 读取器供断言；`reset()` 供用例间隔离。

### 3.3 ChainTools（E5 多轮工具链）

- 三个工具构成依赖链，串行才走得通：
  - `getCity()` → `"北京"`
  - `getDate(city)` → city 非空返回 `"2026-09-07"`，否则返回 `"缺少城市参数，请先调用 getCity"`
  - `getWeather2(city, date)` → 两参齐返回 `"晴，12°C"`，否则返回 `"缺少参数，请先调用 getCity / getDate"`
- 设计意图：依赖关系迫使模型串行调用；配合提问"不要猜测，必须用工具查询"。

### 3.4 ManualLoop + LoopResult（E4 / E5 / E7 共用）

- 签名：`LoopResult run(String userText, ChatModel chatModel, int maxIterations)`。
- 循环体（官方标准路径，v1.1.8 文档同构核验）：
  1. `ToolCallingChatOptions.builder().toolCallbacks(ToolCallbacks.from(tools)).internalToolExecutionEnabled(false).build()`
  2. `Prompt prompt = new Prompt(userText, options)` → `chatModel.call(prompt)`
  3. `chatResponse.hasToolCalls()` 为真 → `toolCallingManager.executeToolCalls(prompt, chatResponse)` → 取 `conversationHistory()` 重建 `Prompt` → 再 `call`
  4. 假 → 循环结束
- `LoopResult` 字段：`finalText`、`iterations`、`toolCallRounds`、`usages`（每轮 token 用量）、`durationsMs`（每轮毫秒耗时）。
- 超过 `maxIterations`（默认 10）即终止并标记 `hitLimit=true`，不抛异常。

### 3.5 ProviderRegistry（E8）

- `@Configuration` 提供 `Map<String, ChatModel>` Bean：`"openai"` → 自动装配 `OpenAiChatModel`；`"anthropic"` → 自动装配 `AnthropicChatModel`。
- 显式按名取用，**禁止类型扫描**（001 §4.3 红线）。
- E8 实测点：`anthropic` 实例请求是否真打到 MiniMax Anthropic 端点——`spring.ai.anthropic.base-url` 不生效则切手动构造 `AnthropicApi(baseUrl, apiKey)`（001 §4.2-7③），对外形态不变。

### 3.6 ThinkStripper

- `static String strip(String text)`：删除所有 `<think>...</think>` 片段（含跨行），供断言前处理（定稿 5.4）。

### 3.7 测试类清单

| 测试类 | 覆盖 | live 标注 |
|---|---|---|
| E1E2BootstrapTest | E1 依赖解析 + 容器启动、E2 开关引用 | 否（离线） |
| E3AutoExecComparisonTest | E3 组 1（默认开）+ 组 2（手动） | 是 |
| E4SingleToolLoopTest | E4 单工具闭环 | 是 |
| E5MultiToolChainTest | E5 链式任务 | 是 |
| E6ToolSchemaTest | E6 参数填充断言 | 是 |
| E7UsageCaptureTest | E7 usage 与耗时采集 | 是 |
| E8ProviderRegistryTest | E8 双 Provider 闭环 | 是 |

live 约定：E3-E8 加 JUnit `@Tag("live")`；命令由 003 排。

## 4. 验收标准（E1-E9，代码层判定）

| 编号 | 验收条目（判定方式） | 追溯 |
|---|---|---|
| E1 | `mvn dependency:tree -Dverbose` 留档：spring-ai / Spring Boot 相关坐标无 "omitted for conflict"；graph-core 探针解析为 `1.1.2.0` 且无冲突标记；`E1E2BootstrapTest.contextStarts` 绿 | 001 §5 E1、E2 |
| E2 | 含 `internalToolExecutionEnabled(false)` 的代码编译通过（并入 E1 测试） | 001 §5 E2 |
| E3 | 组 1：默认开关、不写自研执行代码——`getWeather` 计数器增加（框架自动执行实证）；组 2：开关关 + ManualLoop——计数只由我方代码产生。两组"执行次数 = 该组日志中模型发起工具调用的轮数" | 001 §5 E3 |
| E4 | ManualLoop 跑 getWeather 单工具任务：`strip(finalText)` 含工具结果要素（"12°C" 或 "晴"）；工具执行全部由我方代码触发 | 001 §5 E4 |
| E5 | ChainTools 任务上限 10 轮内正确终止；`toolCallRounds` ≥ 3 轮；`strip(finalText)` 汇总"北京"、"2026-09-07"、"晴"三要素；每轮历史长度单调增 | 001 §5 E5 |
| E6 | `lastArgs` = 提问给定值（city=北京、date=2026-09-07）；偶发偏差允许固定次数重跑并记录 | 001 §5 E6 |
| E7 | `usages` 非空（token 数 > 0）、`durationsMs` 非空（> 0）；一次模型调用对应一组 (usage, duration) | 001 §5 E7 |
| E8 | `registry.get("openai")` / `registry.get("anthropic")` 各自完成 E4 同款闭环；Anthropic 腿日志确认请求打到 MiniMax 端点；按名取用无类型扫描 | 001 §5 E8 |
| E9 | 条件任务（E1 失败时）：切对照组合（SAA 1.0.0.4 优先 + spring-ai-bom 1.0.0 + parent 3.4.x），重跑 E1-E4 后继续跑完 E5-E8，全绿才按 001 §8 上报评审确认 | 001 §5 E9 |

验收基调：001 §5 通过标准全部继承、只增不减；E3-E6 断言以"执行次数 = 日志中模型发起工具调用的轮数"为准；偶发偏差允许固定次数重跑并记录。

## 5. 约束与红线（继承 001 §4.3）

- 禁用自动执行（E3 实证）；Provider 显式映射、禁类型扫描（E8 实证）
- 密钥只走环境变量：yaml 只写 `${环境变量名:默认值}` 占位符；日志/命令行最多 5 位前缀
- 构建与测试只在 spike 目录内执行；pom 永不进根 modules
- 不使用 SAA Graph（graph-core 仅作依赖树探针，不 import）；不把 ChatClient 用作循环承载；流式、重试、三档超时不做

## 6. 开放项（E1/E8 实测落定，不阻塞 003）

| # | 开放项 | 落定时机 |
|---|---|---|
| 1 | 双 BOM 协作实测：graph-core 探针解析结果（是否 1.1.2.0、有无冲突） | E1 依赖树 |
| 2 | `spring.ai.anthropic.base-url` 是否生效；不生效切手动构造 `AnthropicApi(baseUrl, apiKey)` | E8 |
| 3 | spring-ai-bom 1.1.2 与 SAA BOM 1.1.2.0 的管理重叠度（SAA BOM 是否已管理 spring-ai 各构件——若是，spring-ai-bom 可省；以依赖树为准） | E1 依赖树 |

## 7. 对 003 的输入

1. 任务顺序：按 001 §7 阶段流（T0→T7；E9 条件分支）
2. 每任务 = 操作内容（001 §5"方法"列 + 本文 §3）+ 完成判定（§4 对应条目）
3. 命令：`mvn test`（全量）、`mvn test -DexcludedGroups=live`（离线）、`mvn dependency:tree -Dverbose`
4. 证据留档：依赖树、测试输出、LoopResult 日志；README 汇总（001 §9）
