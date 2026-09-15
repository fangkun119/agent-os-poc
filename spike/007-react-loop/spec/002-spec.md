# Spike 实验代码规格说明（第二组：模型接入路径重验）

> 位置：`spike/007-react-loop/spec/002-spec.md`
> 创建：2026-09-14 · 状态：待评审
> 上游：`spike/007-react-loop/spec/002-req.md`（本文下称 **req**——验证项 V0-V8、硬约束 4.1、规程 4.2、产出物 4.4 的唯一源头，本文只做代码层细化、不弱化其中任何标准）
> 参照件（只参照、不拷贝代码）：`spike/007-react-loop/README.md`（下称 **007 README**，第一节为第一组结论存档——D1-D4 决议与 E1-E9 打勾表）。第一组规格三件套（001-expirement / 001-spec / 001-plan）已于 2026-09-15 删除（已被 002- 替代）；其中 001-expirement 可自 git 历史（2a01bee / edfadf3）恢复，001-spec / 001-plan 从未提交、删除后不可恢复
> 下游：实施计划（建议命名 `spec/002-plan.md`，本文第 7.3 节给输入）

## 0. 文档定位、术语与文件指代

### 0.1 文档定位

- 本文回答两件事：**第二组实验代码长什么样**（工程改动、组件、配置），**V0-V8 每项怎么判**（代码层判定，第 4 节）。
- 与第一组（E1-E9，已采纳封存）的关系，四条（2026-09-14 方案 B 修订）：
  1. 不拷贝第一组代码——组件按 007 README D3 的循环路径、第五节样例与 ThinkStripper 思路**重写**（req 4.2 第 3 条）；
  2. 第一组代码处置（方案 B）：`src/` 已归档迁移至 `git 历史（提交 2a01bee）`（git 历史另有全量），工作区零残留；`logs/` 与 `README.md` 原地保留；本轮代码 100% 新写、**自含启动类与全量配置**（见 1.3 / 2.1）；
  3. `pom.xml` 是工程级共享文件——本组对 pom **只增不改**（增一条依赖，见 1.2）；
  4. 本组工程因此完全自含：无旧测试可回归，原 §2.3 的"minimax 自动配置波及第一组测试"风险随归档**消除**；本组测试命令只挑本组的类（见 7.3）。

### 0.2 术语表（本文中每个术语按此含义使用）

| 术语 | 含义 |
|---|---|
| 第二组 / 本轮 | spike/007-react-loop 的第二轮实验：模型接入路径重验（req 定义，V0-V8） |
| 路径 A / B / C | 三条候选接入路径（req 3.2）：A=维持双腿 starter；B=Spring AI MiniMax 原生 starter（`spring-ai-starter-model-minimax` + `spring.ai.minimax.*`）；C=SAA 自家 MiniMax connector（是否存在待 V0 检索） |
| V0-V8 | 本轮验证项编号（req 3.1 矩阵）。V8 是决议收口项 |
| D1-D4 / D5+ | D1-D4=第一组四项决议（007 README 第二节）；本轮新决议建议从 D5 起编号（req 4.2 第 5 条） |
| 工程包 | 本轮新代码的 Java 包 `spike.reactloop`（方案 B 后工作区唯一包；第一组代码已从工作区移除，保留于 git 历史） |
| 占位符 | 配置文件里的 `${环境变量名:默认值}` 写法：运行时从环境变量取值，取不到用默认值；密钥真实值永不进仓库（req 4.1 第 1 条） |
| 四元组 | 一个 Provider 一组四个环境变量：`*_API_KEY` / `*_BASE_URL` / `*_DEFAULT_MODEL` / `*_MODEL_LIST`（定稿 §2） |
| 双腿 starter | 第一组现行接法：Spring AI 官方 openai 与 anthropic 两个 starter，各自指向 MiniMax 的协议兼容端点（007 README D2） |
| 手动循环 | `internalToolExecutionEnabled(false)` 关掉框架自动执行后，由自研循环自己执行工具、自己组装下一轮请求（007 README D3） |
| 双执行 | 同一次工具调用被执行两次（框架一遍 + 自研循环一遍）。本项目红线（req 4.1 第 3 条） |
| E3 计数法 | 第一组 E3 的判定方法：给工具加执行计数器，"工具执行次数 = 日志中模型发起工具调用的轮数"即无双执行 |
| 显式映射 / 类型扫描 | 显式映射=`Map<provider 名, ChatModel>` 按名取用；类型扫描=扫描容器里所有同类型 Bean 找实现。本项目禁类型扫描（TS 3.2；req 4.1 第 2 条） |
| usage | 模型调用消耗的 token 计量数（输入/输出各多少） |
| `<think>` 标签 | MiniMax-M3 把思考过程混在返回正文里、包在 `<think>...</think>` 中；断言前先剥离（定稿 §5.4） |
| ThinkStripper | 第一组的 `<think>` 剥离工具类（`spike/reactloop/util/ThinkStripper.java`）；本轮参照其正则思路重写为 ThinkStripper |
| live 测试 | 需要真实 API key、产生真实模型调用的测试，标 JUnit `@Tag("live")`（第一组同款约定） |
| `@Qualifier` | Spring 按名字注入指定 Bean 的注解——第一组用它实现显式映射（007 README 第五节样例） |
| 路径 D / 腿 4（zhipu） | 第四条腿：以智谱 GLM **模拟**"无 Spring AI Starter、仅提供 OpenAI 兼容 API 的厂商"——经智谱兼容 API + OpenAI starter 接入（req 3.1 V9）。注：智谱在 1.1 线其实有官方 starter（`spring-ai-starter-model-zhipuai`），本腿刻意不用（场景要求走 OpenAI starter）；该 starter 与 minimax 同在 2.0 被移除（两份仓库树核查），模拟的恰是长期更稳的接入形态 |
| mutate() | Spring AI 官方多端点模式：从既有 OpenAiApi / OpenAiChatModel 派生改了 base-url / api-key / 默认模型的新实例（v1.1.8 chatclient.adoc "Multiple OpenAI-Compatible API Endpoints"节，官方示例即接 Groq） |

### 0.3 文件指代（防编号混淆，务必先读）

| 简称 | 实体 |
|---|---|
| req | `spike/007-react-loop/spec/002-req.md`（本轮需求） |
| 本文 / 002-spec | `spike/007-react-loop/spec/002-spec.md`（本文件） |
| 第一组规格三件套 | `spec/001-expirement.md`（git 历史 2a01bee / edfadf3 可恢复）；`001-spec.md` / `001-plan.md`（从未提交，2026-09-15 删除；已被 002- 替代，确认不再保留） |
| 007 README | `spike/007-react-loop/README.md` |
| 定稿 | `docs/design/detail-supplement/001-model-config-export.md` |
| 评审 001 | `docs/review/001-tech-review-based-on-requirement.md` |
| TS / DA / AG（此式引用） | `docs/design/TechnicalSolution.md` / `DemandAnalysis.md` / `AiProgrammingGuide.md` 的对应章节号 |
| 根 CLAUDE.md / spike/CLAUDE.md | 仓库根 `/CLAUDE.md` / `spike/CLAUDE.md` |

## 1. 工程形态：改什么、不改什么

### 1.1 改动总览

| 对象 | 动作 | 内容 |
|---|---|---|
| `pom.xml` | **只增不改** | 新增 1 条依赖：`org.springframework.ai:spring-ai-starter-model-minimax`（不写版本号，交给 spring-ai-bom:1.1.2 仲裁），见 1.2 |
| `src/main/java/spike/reactloop/` | 新增包 | 本轮全部新代码（6 个类，含启动类，见第 3 节） |
| `src/main/resources/application.yaml` | 新增文件 | 工程唯一配置：三腿全量 + retry 关闭（见第 2 节） |
| `src/test/java/spike/reactloop/` | 新增包 | 本轮 7 个测试类（见 3.5） |
| `logs/r2/` | 新增子目录 | 本轮证据日志（文件名表见 7.1） |
| 第一组 `src/` | **归档迁移**（方案 B） | 压缩包 `git 历史（提交 2a01bee）`（git 历史另有全量）；第一组 `logs/`（8 个日志）与 `README.md` 原地保留 |

### 1.2 pom.xml 变更与 V1 的失败信号

在 `<dependencies>` 现有 4 条依赖之后追加：

```xml
<!-- 第二组 V1：Spring AI MiniMax 原生 starter（路径 B）。
     不写版本号：能否被 spring-ai-bom:1.1.2 仲裁出 1.1.2 正是 V1 的验证点。
     解析失败（报 dependencies.dependency.version is missing 或找不到构件）
     即 V1 判"该 starter 在 spring-ai-bom:1.1.2 中不存在"，报错原文落盘
     logs/r2/v1-resolve.txt，然后按 req 3.3 决议规则收口，V3-V7 不再执行。 -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-minimax</artifactId>
</dependency>
```

其余一切（parent 3.5.16、双 BOM import、两个双腿 starter、graph-core 探针、surefire 180 秒兜底）保持现状——parent 与 JDK 21 不动是 req 4.1 第 4 条硬约束（007 README D1）。

### 1.3 新增部分的目录结构

```text
spike/007-react-loop/
├── pom.xml                                    # 只增 1 条依赖（1.2）
├── src/main/resources/application.yaml        # 工程唯一配置：三腿全量 + retry 关闭
├── src/main/java/spike/reactloop/
│   ├── App.java                             # 自含启动类（第一组 SpikeApp 已随归档迁出）
│   ├── CountingTools.java                   # 带计数器、固定返回的工具（V3/V4/V5/V6）
│   ├── ManualLoop.java                      # 手动 ReAct 循环（参照 D3 路径重写）
│   ├── LoopResult.java                      # 循环结果载体
│   ├── ProviderRegistry.java                # Map<provider 名, ChatModel>（V5/V6）
│   └── ThinkStripper.java                   # <think> 剥离（V7，参照思路重写）
├── src/test/java/spike/reactloop/
│   ├── V1BootstrapTest.java                 # V1 容器启动（离线）
│   ├── V2MinimaxPropertiesTest.java         # V2 属性面（离线断言 + live 探针）
│   ├── V3SingleToolLoopTest.java            # V3 单工具闭环（live）
│   ├── V4ManualLoopCountTest.java           # V4 手动循环 + 计数对照（live）
│   ├── V5ProviderRegistryTest.java          # V5 显式映射复验（live）
│   ├── V6UsageCaptureTest.java              # V6 usage/耗时采集（live）
│   └── V9DualOpenAIInstanceTest.java        # V9 双 OpenAI 协议实例并存（live）
└── logs/r2/                                   # 本轮证据（7.1 文件名表）
```

## 2. 配置：application.yaml（工程唯一配置）

### 2.1 全文草稿（src/main/resources/application.yaml，工程唯一配置）

```yaml
# 工程唯一配置（方案 B：第一组 application.yaml 已随 src/ 归档）——三腿全量自含，腿 4 走显式构造。
spring:
  ai:
    # 关掉 Spring AI 默认重试链（默认 max-attempts=10、退避最长 3min）——
    # 否则 V2 负向探针会拖满 surefire 180 秒被强杀；V6 的"一次调用 ↔ 一组计时"
    # 也要求无隐式重试污染。正式实现按 agentos/CLAUDE.md 第 36 条显式设计重试与三档预算的关系
    retry:
      max-attempts: 1
    openai:                                      # 腿 1：MiniMax 国内站 OpenAI 兼容端点（配置自第一组平移，值不变）
      api-key: ${OPENAI_API_KEY:placeholder}
      base-url: https://api.minimax.cn
      chat:
        options:
          model: ${OPENAI_DEFAULT_MODEL:MiniMax-M2.7}
    anthropic:                                   # 腿 2：MiniMax 国内站 Anthropic 兼容端点（2026-09-14 国内站决议）
      api-key: ${ANTHROPIC_API_KEY:placeholder}
      base-url: https://api.minimax.cn/anthropic # 可达性未经实测，由 V6 顺带验证；不通则改回国际站值并另报评审
      chat:
        options:
          model: ${ANTHROPIC_DEFAULT_MODEL:MiniMax-M3}
    minimax:
      api-key: ${MINIMAX_API_KEY:placeholder}   # 密钥：环境变量占位；placeholder 默认值只服务离线测试
      base-url: https://api.minimax.cn           # V2 实测项：候选 A（不带 /v1）。
                                                  # 失败（404 / 路径错）则改候选 B
                                                  # https://api.minimax.cn/v1 重测；
                                                  # 两候选结果都落盘 logs/r2/v2-properties.txt。
                                                  # 实测前不得照搬任何一条现成结论（req 4.1 第 5 条注意事项）。
                                                  # 源码口径（2026-09-14 核验，v1.1.8 MiniMaxApi 144/164 行）：
                                                  # 客户端自动追加的是 MiniMax 原生协议路径
                                                  # /v1/text/chatcompletion_v2（非 OpenAI 兼容路径）——
                                                  # 即 minimax 腿是第三种协议形态，预期候选 A 正确；
                                                  # 运行时候选实测照做，不因此跳过
      chat:
        options:
          model: ${MINIMAX_DEFAULT_MODEL:MiniMax-M2.7}
```

说明：腿 4（zhipu）不走 yaml——其配置（`ZHIPU_*` 环境变量）由 ProviderRegistry 构造时显式读取（§3.3），这正是"无 Starter 厂商"接入形态的一部分。

### 2.2 设计说明

| # | 决定 | 理由 |
|---|---|---|
| 1 | `application.yaml` 为 工程唯一配置（方案 B：第一组 application.yaml 已随 src/ 归档） | 四腿全量自含，无 profile 叠加机制；文件名即标准名，测试无需 @ActiveProfiles |
| 2 | **不**把 `${MINIMAX_BASE_URL}` 映射给 `spring.ai.minimax.base-url` | 脚本里 `MINIMAX_BASE_URL='https://api.minimax.cn/v1'`（SDK 惯例值带 `/v1`，定稿 §7 附录脚本全文 282 行）——与 OPENAI/ANTHROPIC 两腿同一条 ⚠️ 规则（定稿 §2.3）：带 `/v1` 的值直映射有双写风险。base-url 非敏感，明文写 yaml，密钥才走环境变量 |
| 3 | 模型占位符用 `MINIMAX_DEFAULT_MODEL`（当前值 MiniMax-M2.7） | 三级选择的第 1 级（环境变量缺省模型，定稿 §5.2）；V3-V5 用 per-call options 覆盖成 MiniMax-M3（第 3 级），两级都覆盖到 |
| 4 | `MINIMAX_MODEL_LIST` 不进 yaml | 定稿 §2.3：MODEL_LIST 无对应 Spring AI 属性，只做校验与发现（req 4.1 第 7 条同口径） |
| 5 | anthropic 腿直接配置国内站 `https://api.minimax.cn/anthropic` | 落实用户 2026-09-14"全部只用国内站"决议；可达性未经实测，由 V6 顺带验证并落盘；若实测不通，yaml 改回国际站值并另报评审（spec §7.2 ⑥ 约定） |
| 6 | 记录路径 B 的长期维护风险（事实注记，2026-09-14 核验）：Spring AI 2.0 main 分支已移除 minimax 源码模块（完整树核查仅剩文档页）；starter 目标老代 MiniMax API——默认模型为 ABAB 6.5 代枚举（`abab6.5g-chat`）、文档链接为 V2 代 API（v1.1.8 tag 源码 `MiniMaxApiConstants` 17 行 / `MiniMaxApi` 58 行） | 2.0 线禁入的补充证据；"新模型名 M2.7/M3 能否经 options 传入、现行 key 是否被原生路径接受"即 V2/V3 的核心实测点；V8 决议若选路径 B，本风险随决议进 README |
| 7 | `spring.ai.retry.max-attempts: 1`（工程专用，关闭默认重试链） | Spring AI 默认 10 次退避重试会把 V2 负向探针拖满 surefire 180 秒；也保证 V6 的 duration 无隐式重试污染（一次调用 ↔ 一组计时）。出处口径：agentos/CLAUDE.md 第 36 条记载的默认值；正式实现按该条显式设计重试与三档预算的关系，spike 从简 |

### 2.3 已识别风险（2026-09-14 更新：原风险已消除）

原风险（方案 A 时代）：minimax starter 入 classpath 可能波及第一组不激活 profile 的离线测试。**方案 B 归档后第一组源码不在工作区，风险源消除**，本条仅存档。保留的观察点（移交 V1）：placeholder key 下 minimax 自动配置能否正常启动容器。

## 3. 组件规格（spike.reactloop 包，全部为重写、非拷贝）

### 3.0 App（启动类，方案 B 新增）

- `@SpringBootApplication` 最小启动类，承接第一组 SpikeApp 的角色（该类已随 src/ 归档）；无业务逻辑，供各测试类启动 Spring 容器。

### 3.1 CountingTools

- 与第一组 CountingTools 同款职责，独立重写：`@Tool getWeather(city, date)` 返回固定内容 `"晴，12°C"`，参数带 `@ToolParam` 说明；内置 `AtomicInteger` 计数器 + 最近入参记录，暴露 `counts()` / `lastArgs()` / `reset()`。
- 计数器是 V4 按 E3 计数法判"无双执行"的依据（req 3.1 V4 行）。

### 3.2 ManualLoop + LoopResult

- 循环路径照 007 README D3（= 评审 001 8.1(3) 官方标准路径）重写：`ToolCallingChatOptions.builder().toolCallbacks(...).internalToolExecutionEnabled(false).build()` → `new Prompt(text, options)` → `chatModel.call(prompt)` → `hasToolCalls()` 为真则 `toolCallingManager.executeToolCalls(prompt, chatResponse)` → `conversationHistory()` 重建 Prompt → 再 call；为假则结束。
- 每轮 call 外裹计时、读响应元数据 usage——V6 的取数路径在这里（一次模型调用 ↔ 一组 (usage, duration)）。
- `LoopResult` 字段：`finalText`、`iterations`、`toolCallRounds`、`usages`、`durationsMs`、`hitLimit`（迭代上限默认 10，防挂死，不抛异常）。
- 支持以 per-call options 覆盖模型名（`.model("MiniMax-M3")`）——V3-V5 跑 M3、V6 跑缺省模型，三级选择的第 3 级由此覆盖。

### 3.3 ProviderRegistry

- `@Configuration` 提供 `Map<String, ChatModel>` Bean，四个键：`"minimax"` → `@Qualifier` 按名注入 MiniMaxChatModel、`"openai"` → openAiChatModel、`"anthropic"` → anthropicChatModel、`"zhipu"` → 方法体内局部构造的第二 OpenAI 协议实例（req 3.1 V9，路径 D，指向智谱）。
- 取用方式 = 007 README 第五节样例（`@Qualifier` 显式按名），**禁止类型扫描**（req 4.1 第 2 条）。
- **zhipu 构造避雷条款（硬性）**：OpenAiChatAutoConfiguration 的 `openAiApi` 与 `openAiChatModel` 两个 `@Bean` 方法都带 `@ConditionalOnMissingBean`（Spring AI v1.1.8 源码 66 / 87 行）——容器里一旦出现自注册的 `OpenAiApi` / `OpenAiChatModel` 类型 Bean，自动配置实例退位不建，腿 1（openai 腿）断裂。因此 zhipu 实例只能在 registry 的 Map Bean 方法体内构造为局部变量（Map 类型不触发该条件注解），不得注册为独立 Bean。
- zhipu 构造写法：`OpenAiApi.builder().baseUrl(<读 ZHIPU_BASE_URL，去尾斜杠>).apiKey(...).completionsPath("/chat/completions").build()` → `new OpenAiChatModel(zhipuApi, OpenAiChatOptions.builder().model(<读 ZHIPU_DEFAULT_MODEL>).build())`。**completions-path 覆盖是本腿关键技术点**：智谱完整路径为 `<base>/api/paas/v4/chat/completions`（Z.AI 官方文档，2026-09-14 Context7 核验），非标准 `/v1/chat/completions` 布局，不改 completionsPath 必然 404；builder 带 completionsPath 有源码实证（OpenAiChatAutoConfiguration 78 行即此用法），`mutate()` 是否透出该字段 V9 现场确认，不透出则全手工 builder。
- zhipu 凭证与端点：key 读 `ZHIPU_API_KEY`（四元组已于 2026-09-14 入脚本注册区，值已由用户填入，智谱标准格式 49 字符、已核验）；base-url 读 `ZHIPU_BASE_URL`，实际值 `https://open.bigmodel.cn/api/coding/paas/v4/`（智谱 GLM Coding Plan 端点，带尾斜杠——代码去尾斜杠后作 builder baseUrl）；认证为标准 `Authorization: Bearer`（OpenAI starter 原生形态，无需适配）；模型名读 `ZHIPU_DEFAULT_MODEL`，实际值 `glm-5.3-flash`。与 MiniMax 腿不同：智谱的 SDK 惯例 base **不带 `/v1`**，故 `ZHIPU_BASE_URL` 可直接作 builder 的 baseUrl，无 `/v1` 双写问题。
- MiniMax 腿的 Bean 名未经文档核验（自动配置的方法名决定），**以容器实测为准**：V5 测试先打印 `context.getBeanNamesForType(ChatModel.class)` 留档、再按实测名 `@Qualifier` 注入。打印属测试内诊断动作，不进任何运行时取用路径——"禁类型扫描"约束的是 Provider 取用方式，不约束测试里的诊断打印（此区分在测试类 Javadoc 里写明，防评审误读）。

### 3.4 ThinkStripper

- `static String strip(String text)`：删除所有 `<think>...</think>` 片段（含跨行）后 trim。正则思路同第一组（`(?s)<think>.*?</think>`），代码重写。

### 3.5 测试类清单

| 测试类 | 覆盖 | live | 模型 |
|---|---|---|---|
| V1BootstrapTest | V1：starter 在 classpath 上容器可启动（全量 yaml、placeholder key） | 否 | 不调用 |
| V2MinimaxPropertiesTest | V2：api-key / base-url / chat.options.model 三属性绑定断言（离线）；base-url 属性生效的负向探针（live，见 4.2） | 部分 | 候选判定用 |
| V3SingleToolLoopTest | V3：MiniMax 腿单工具闭环；V7 的 M3 观察内建于本测试与 V4（见 4.2） | 是 | MiniMax-M3（options 覆盖） |
| V4ManualLoopCountTest | V4：手动循环 + 计数器，按 E3 计数法判无双执行 | 是 | MiniMax-M3（options 覆盖） |
| V5ProviderRegistryTest | V5：minimax 与 openai 两实例并存映射、按名取用各跑闭环（anthropic 键供 V6 取用） | 是 | minimax=M3、openai=腿缺省 |
| V6UsageCaptureTest | V6：minimax 腿（缺省模型 M2.7）+ anthropic 腿（M3，配置即国内站）各一组 usage/耗时样本 | 是 | 见左列 |
| V9DualOpenAIInstanceTest | V9：openai 与 zhipu 两 OpenAI 协议实例各跑单工具闭环、互不干扰；zhipu 腿 completions-path 覆盖实测 | 是 | openai=腿缺省（M2.7）、zhipu=ZHIPU_DEFAULT_MODEL |

live 约定沿用第一组：`@Tag("live")`；命令见 7.3。

## 4. 验收标准（V0-V8 代码层判定，追溯 req 3.1）

| 编号 | 验收条目（判定方式） | 追溯 |
|---|---|---|
| V0 | 无代码。检索 java2ai.com 全站与 SAA GitHub 仓库模块清单，过程与结论（查了哪些页面、检索日期、有无 MiniMax connector 及坐标）落 `logs/r2/v0-connector-search.txt` 并汇总进 README 增补节 | req 3.1 V0 |
| V1 | `mvn dependency:resolve` 成功且该 starter 构件版本落 1.1.2；`mvn dependency:tree -Dverbose` 留档 `logs/r2/v1-dependency-tree.txt`（无新增 "omitted for conflict"）；`V1BootstrapTest.contextStarts` 绿。解析失败即判"不存在"：报错原文落 `logs/r2/v1-resolve.txt`，V3-V7 停止，按 req 3.3 收口 | req 3.1 V1（对照 README 第一节存档 E1 行的解析法） |
| V2 | 属性表落盘 README（每个属性标"可配 / 不可配 / 默认值 / 实测表现"），证据进 `logs/r2/v2-properties.txt`：① model 绑定断言（离线：实例缺省 options 的 model = `${MINIMAX_DEFAULT_MODEL}` 解析值）；② base-url 负向探针（live，预期失败用例）；③ base-url 两候选（不带/带 `/v1`）的实测结果与自动追加路径结论；④ `spring.ai.minimax.chat.base-url` chat 级覆盖属性的绑定表现（文档记载存在，v1.1.8 属性表） | req 3.1 V2（对照 007 E8 属性生效实证法） |
| V3 | ManualLoop 在 MiniMax 腿跑 getWeather 单工具闭环：`ThinkStripper.strip(finalText)` 含工具结果要素（"12°C" 或 "晴"）；工具执行全部由我方代码触发 | req 3.1 V3（对照 007 E4） |
| V4 | 手动循环下计数器读数 = 该次运行日志中模型发起工具调用的轮数（E3 计数法），无双执行；对照记录进 `logs/r2/v4-manual-count.txt` | req 3.1 V4（对照 007 E3） |
| V5 | registry 含 `"minimax"` 与 `"openai"` 两键且互不混淆；两键按名取用各完成 V3 同款闭环；Bean 名诊断打印留档；无类型扫描取用 | req 3.1 V5（对照 007 E8） |
| V6 | minimax 腿与 anthropic 腿各采到一组 (usage, duration)：token 数 > 0、毫秒耗时 > 0、一次模型调用 ↔ 一组样本；进 `logs/r2/v6-usage.txt`——anthropic 腿样本即第一组 E7 缺的第二协议补齐；anthropic 腿配置即国内站 `api.minimax.cn/anthropic`（2026-09-14 域名口径），该路径可达性即 V6 的顺带实测产出（失败则如实落盘、application.yaml 改回国际站值并另报评审） | req 3.1 V6（对照 007 E7） |
| V7 | V3/V4 全部以 MiniMax-M3 跑（options 覆盖）：剥离后最终文本无 `<think>` 残留；与第一组表现（README 附带结论）的差异落 `logs/r2/v7-think-observations.txt` | req 3.1 V7 |
| V8 | 无代码。README 增补"第二组结论"节：V0-V8 打勾表（通过/失败/未执行 + 原因）+ 决议（D5 起）+ 正式实现落点 + req 4.4 第 4 项的联动清单命中勾选态 | req 3.1 V8、4.4 |
| V9 | openai 与 zhipu 两实例各自完成 V3 同款闭环且互不干扰（zhipu 来自不同厂商不同模型，可辨识）；按名取用无类型扫描；completions-path 覆盖结论与智谱端点表现（鉴权、模型接受、工具调用）落盘 `logs/r2/v9-dual-openai.txt`；自动配置 Bean 不退位（V5 的 Bean 名打印交叉印证） | req 3.1 V9 |

### 4.1 前置依赖（与 req 3.1 一致）

V0、V1 是入口项；V2 依赖 V1；V3-V7 依赖 V1+V2；V8 收口。V1 失败则 V3-V7 不执行，直接按 req 3.3 决议规则进入 V8。

### 4.2 两处判定细则

1. **base-url 属性生效的负向探针**（V2 的"请求真实打到配置端点"判据，方法同 007 E8 的反证法）：把 `spring.ai.minimax.base-url` 临时指向一个必拒连地址（执行后修订：原定假域名不可用——本机 fake-IP 代理会解析任意假域名，改用 `https://127.0.0.1:1`，本机必拒连且 localhost 不走代理）发一次最小 call——预期抛连接异常且堆栈含该地址，即证明属性真的接线生效。注意：手动构造的 ChatModel 内置默认重试模板（10 次退避），必须显式传 `maxAttempts=1` 的模板，否则探针被退避链拖满 surefire 180 秒。探针结果进 v2 日志。正向端点证据复用 V3 闭环日志（闭环成功 = 请求打到了配置端点）；候选 A 失败时 V2 自做候选 B 的最小连通验证，胜出候选供 V3-V6 使用。
2. **V7 内建于 V3/V4**：V3/V4 的测试断言本来就要先 strip 再比对，M3 跑 V3/V4 时把"剥离前后文本、有无残留、与第一组差异"额外打印进日志即为 V7 证据，不单设测试类、不重复烧调用。

## 5. 硬约束映射（req 4.1 八条 → 本规格的落实）

| # | 约束（req 4.1） | 本规格落实 |
|---|---|---|
| 1 | 密钥只从环境变量读；仓库只写占位符；日志/命令行最多 5 位前缀 | 2.1 yaml 只写 `${MINIMAX_API_KEY:placeholder}`；全部组件与测试不打印配置值；跑前 `source ~/.agent-os-poc/script/agent-os-env.sh`（7.3 命令前置） |
| 2 | 显式映射 `Map<provider 名, ChatModel>`，禁类型扫描 | 3.3 ProviderRegistry 按名注入构造；诊断打印与取用的区分见 3.3 |
| 3 | 禁用自动 tool 执行 | 3.2 ManualLoop 全程 `internalToolExecutionEnabled(false)`；V4 计数法直接实证 |
| 4 | parent 3.5.16 + JDK 21 | 1.2 pom 只增一条依赖，其余零改动 |
| 5 | OpenAI 兼容腿 base-url 不带 `/v1`；MiniMax 腿拼接惯例未定 | openai 腿配置沿用第一组 yaml（不动）；minimax 腿两候选实测（2.1、4.2），实测前不照搬结论 |
| 6 | usage 落 llm_calls 审计表 | spike 不建表（SQLite 落库属正式实现范围）；本规格验证取数路径（V6），"一次调用 ↔ 一组 (usage, duration)"即审计对齐的结构证据 |
| 7 | 模型三级选择机制不变 | 第 1 级：yaml 占位符绑 `MINIMAX_DEFAULT_MODEL`（V2 离线断言 + V6 真调用）；第 3 级：V3-V5 per-call options 覆盖 M3；第 2 级（AGENT.md frontmatter）属正式实现，spike 不涉及 |
| 8 | 四元组语义：三组并列互不覆盖 | 本规格只新增接线 MINIMAX 一组，OPENAI/ANTHROPIC 两腿配置零改动；不新增 Provider、不改脚本 |

## 6. 开放项（全部由实测落定，不阻塞下游 plan）

| # | 开放项 | 落定时机 |
|---|---|---|
| 1 | `spring-ai-starter-model-minimax` 在 spring-ai-bom:1.1.2 中是否存在、解析版本是否 1.1.2 | V1 |
| 2 | `spring.ai.minimax.base-url` 可配与否、自动追加路径的拼接惯例（候选 A/B 谁胜出） | V2 |
| 3 | MiniMax 腿 Bean 的准确名称（`@Qualifier` 用） | V5 容器实测 |
| 4 | （风险源已消除）原"minimax 自动配置波及第一组离线测试"随方案 B 归档消失；保留观察点：placeholder key 下容器能否启动 | V1 |
| 5 | MiniMaxChatModel 上 `internalToolExecutionEnabled(false)` 行为是否与 openai 腿一致 | V4 |
| 6 | 路径 C 的 SAA connector 坐标有无——已裁决：SAA 无 MiniMax connector，路径 C 出局（2026-09-14 V0 检索，证据 `logs/r2/v0-connector-search.txt`） | 已结 |
| 7 | 国内站 `https://api.minimax.cn/anthropic` 路径可达性（E8 实测值为国际站；application.yaml 已直配国内站，见 2.1） | V6 |
| 8 | zhipu 腿 completions-path 覆盖（base 去 `/v1` 布局 + `/chat/completions` 组合）与 mutate() 是否透出 completionsPath（不透出退全手工 builder，见 3.3） | V9 |
| 9 | 智谱 OpenAI 兼容端点的工具调用支持度（tools / tool_calls 往返）——V9 闭环即顺带实测 | V9 |

## 7. 证据落点与对下游 plan 的输入

### 7.1 logs/r2/ 文件名表（按 V 编号命名，req 4.4 第 5 项）

| 文件 | 内容 |
|---|---|
| `v0-connector-search.txt` | V0 检索过程与结论 |
| `v1-resolve.txt` / `v1-dependency-tree.txt` | V1 解析输出与依赖树留档 |
| `v2-properties.txt` | V2 属性表证据：绑定断言、负向探针、两候选实测 |
| `v3-tool-loop.txt` | V3 闭环逐轮日志（含 base-url 正向端点证据） |
| `v4-manual-count.txt` | V4 计数对照 |
| `v5-registry.txt` | V5 Bean 名诊断打印 + 双键闭环 |
| `v6-usage.txt` | V6 双腿 usage/耗时样本 |
| `v7-think-observations.txt` | V7 剥离表现与第一组差异 |
| `v9-dual-openai.txt` | V9 双 OpenAI 协议实例并存 + zhipu 腿 completions-path 实测 |

### 7.2 README 全新创建的必含内容（req 4.4 对齐；2026-09-15 用户删除旧 README 后修订）

README.md 全新创建，分两节：**第一节"第一组结论存档"**——D1-D4 决议与 E1-E9 打勾表自 git 历史原文恢复（引用链"007 README 的 D1-D4"保持有效，根 CLAUDE.md 的 W1 指针不断链，且明确标注为存档节、非本轮产物）；**第二节"第二组结论（模型接入路径重验）"**：① V0-V8 打勾表（通过/失败/未执行 + 原因）；② 决议（D5 起）+ 正式实现落点；③ 路径 B 属性表（V2）；④ 路径 C 检索过程（V0 已判出局）；⑤ req 5.2 / 5.3 / 5.4 联动清单命中情况逐条勾选；⑥ 域名口径变更：anthropic 兼容端点由国际站（第一组 E8 实测值 `api.minimaxi.com/anthropic`）迁至国内站 `api.minimax.cn/anthropic`（2026-09-14 用户决议）。定稿 §1 / §2.3 / §7 与根 CLAUDE.md「模型接入环境变量」节已经用户指示于 2026-09-14 同步为国内站；国内站 /anthropic 路径可达性由 V6 实测确认，若实测不通需回改这三处并另报评审。
⑦ 归档注记：第一组 `src/` 已于 2026-09-14 归档至 `git 历史（提交 2a01bee）`（方案 B，用户拍板），`logs/` 与本 README 原地保留；工作区代码自本日起 100% 为第二组新写（App 起）。结论引用格式照 req 4.2 第 5 条实名引用（如"spike/007-react-loop/README.md 的 D5 决议"）。

### 7.3 对下游 plan（建议名 `spec/002-plan.md`）的输入

1. **任务顺序建议**（V 依赖链）：T0 pom 增依赖 + App + 全量 yaml + V1 → T1 V2（离线断言 → live 探针）→ T2 组件（3.0-3.4）→ T3 V3+V7 → T4 V4 → T5 V5 → T6 V6 → T6.5 V9（复用 T2 / T5 产物）→ T7 V8 README 收口。V0 检索（无代码）已于 2026-09-14 完成：路径 C 出局（`logs/r2/v0-connector-search.txt`）。
2. **命令**（全部在 `spike/007-react-loop/` 目录内执行，spike/CLAUDE.md）：解析 `mvn dependency:resolve`、`mvn dependency:tree -Dverbose`；离线项 `mvn test -Dtest='V1*' -DexcludedGroups=live`；全量 `mvn test -Dtest='V*'`；跑 live 前置 `source ~/.agent-os-poc/script/agent-os-env.sh`（必须 source 不能执行）。
3. **时间盒**：2-3 小时（req 4.3）。到盒即停：已完成项落 README，未完成项写明状态上报评审，不自动延期。
4. **每任务完成判定**：第 4 节对应 V 条目，只增不减。
