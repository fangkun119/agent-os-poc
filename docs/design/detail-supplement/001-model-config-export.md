# 模型接入环境变量：导出脚本设计与使用（定稿）

> 位置：`docs/design/detail-supplement/001-model-config-export.md`
> 创建：2026-09-06 · 状态：定稿
> 实现体：`~/.agent-os-poc/script/agent-os-env.sh`（git 仓库外，权限 600）
> 过程稿：`chat/temp/20260906-model-config-export-design.md`（保留作过程记录；本文是入库存档，两处不一致以本文为准）

## 0. 先看结论（4 条）

1. **密钥只放一个地方**：仓库外的脚本 `~/.agent-os-poc/script/agent-os-env.sh`（权限 600）。仓库内任何文件只允许写 `${环境变量名}` 占位符。
2. **环境变量按 Provider（供应商）命名**：一个 Provider 一组四元组（`*_API_KEY` / `*_BASE_URL` / `*_DEFAULT_MODEL` / `*_MODEL_LIST`）。现有三个 Provider 并列：`OPENAI`、`ANTHROPIC`、`MINIMAX`；`OPENAI_*` / `ANTHROPIC_*` 的取值当前来自 MiniMax 兼容端点（因为只有 MiniMax 账号），用户可整体替换为原生账号或其它兼容服务。
3. **Spring AI 接 OpenAI 兼容端点，base-url 不带 `/v1`**：Spring AI 会自动追加 `/v1/chat/completions`，base-url 写 `https://api.minimax.cn` 即可；base-url 非敏感、直接写 yaml，只有密钥走环境变量。
4. **运行时切换模型不靠环境变量**：靠每次调用的 options 参数覆盖缺省值；环境变量只提供"缺省模型 + 可用模型清单"。

## 0.x 术语表

| 术语 | 含义 |
|---|---|
| source | shell 命令：在当前 shell 里执行脚本内容，让 `export` 的变量留在当前 shell、传给后续命令。必须 source、不能直接执行（直接执行时变量只存在于子进程，一退出就没了） |
| vendor | 模型供应商（如 MiniMax、DeepSeek、Kimi） |
| 兼容腿 | 口语简称：某 Provider 的配置值指向别家供应商的协议兼容端点（如当前 OPENAI Provider 的配置值 = MiniMax 的 OpenAI 兼容端点） |
| Provider 四元组 | 一个 Provider 的一组配置值：端点（`BASE_URL`）、密钥（`API_KEY`）、缺省模型（`DEFAULT_MODEL`）、可用模型清单（`MODEL_LIST`） |
| 协议兼容端点 | 某家供应商提供的、说另一种协议的服务（如 MiniMax 的 OpenAI 兼容端点：说 OpenAI 协议，后端是 MiniMax） |
| Spring AI 属性 | Spring AI 框架从配置文件读的配置键，如 `spring.ai.openai.api-key`（写在 application.yaml） |
| starter | Spring Boot 的开箱即用依赖包：引入一个 starter 就带齐某项能力所需的依赖和默认配置 |
| ChatModel | Spring AI 里代表"一个模型连接"的接口实例；同一个实例可以对不同模型发请求 |
| options | 每次调用模型时附带的参数对象（指定模型名、工具、开关等），可覆盖实例缺省值 |
| `/v1` 双写 | URL 拼接错误：base-url 已带 `/v1`，框架又自动追加 `/v1/chat/completions`，产生 `/v1/v1/...`，请求 404 |

## 1. 背景与密钥红线

**接入现实**：项目没有原生 OpenAI、Anthropic 模型账号。MiniMax 提供两个协议兼容端点，共用同一把 key：

| 兼容协议 | MiniMax 端点 | 可用模型 |
|---|---|---|
| OpenAI 兼容 | `https://api.minimax.cn/v1` | MiniMax-M3、MiniMax-M2.7、MiniMax-M2.7-highspeed |
| Anthropic 兼容 | `https://api.minimaxi.com/anthropic` | 同上 |

域名说明：`.cn` 是国内站，`.minimaxi.com` 是国际站；实测哪个通用哪个，互为备选。MINIMAX 自己也是一个 Provider（`MINIMAX_*`），与 OPENAI、ANTHROPIC 并列（见第 2 节）。

**密钥安全红线（目的：防泄密——不进代码库、不进日志）**：

1. Auth Token / API Key 目前**只能从环境变量读取**（未来可能迁移到专门存储秘钥的基础设施）
2. **绝对不可以从配置文件读取**
3. **绝对不可以完整写到命令行或日志**——debug 最多输出前 5 位前缀（如 `sk-cp***`）

推论：密钥在磁盘上的唯一落点 = 仓库外脚本（600 权限）；git 仓库内只允许 `${环境变量名}` 占位符。

## 2. 环境变量命名规则：一个 Provider 一组四元组（schema）

### 2.1 Provider 清单与当前取值

| Provider | 变量前缀 | 当前取值来源 | 用途 |
|---|---|---|---|
| OPENAI | `OPENAI_*` | MiniMax 的 OpenAI 兼容端点（当前唯一账号） | OpenAI 协议接入（Spring AI openai 连接器栈） |
| ANTHROPIC | `ANTHROPIC_*` | MiniMax 的 Anthropic 兼容端点 | Anthropic 协议接入（Spring AI anthropic 连接器栈） |
| MINIMAX | `MINIMAX_*` | MiniMax 自己 | 以供应商名字注册的 Provider |

- 三个 Provider 并列、互不影响；将来的 DEEPSEEK、KIMI 各占一组前缀，与现有 Provider 并列
- **OPENAI / ANTHROPIC 的四元组是"可整体替换的配置值"**：用户有原生账号（或想换其它兼容服务）时，直接改注册区的四个值即可，变量名与代码零改动；MINIMAX 不受影响

### 2.2 脚本注册区的命名模式

每个 Provider 一组，按这个模式起名：

```bash
<PROVIDER>_API_KEY          # 密钥
<PROVIDER>_BASE_URL         # 端点地址（非敏感）
<PROVIDER>_DEFAULT_MODEL    # 缺省模型：调用方不指定 model 时用它；必须是 MODEL_LIST 的成员
<PROVIDER>_MODEL_LIST       # 可用模型清单，逗号分隔（非敏感）
```

`MODEL_LIST` 的用途边界：只做**校验**（防止写错模型名）和**发现**（看这家能跑什么）。运行时选择模型不靠它，靠每次调用的 options。

### 2.3 导出的环境变量 → Spring AI 属性对照

| 导出的变量 | 对应 Spring AI 属性（yaml 只写占位符） | 说明 |
|---|---|---|
| `OPENAI_API_KEY` | `spring.ai.openai.api-key: ${OPENAI_API_KEY}` | SDK 标准名（OpenAI 生态通用） |
| `OPENAI_BASE_URL` | ⚠️ **不直接映射**（SDK 惯例值带 `/v1`，原因见 5.3 节）。base-url 非敏感，Spring AI 侧直接写 yaml：`spring.ai.openai.base-url: https://api.minimax.cn` | OpenAI SDK 语境的端点（带 /v1） |
| `OPENAI_DEFAULT_MODEL` | `spring.ai.openai.chat.options.model: ${OPENAI_DEFAULT_MODEL}` | 缺省模型 |
| `OPENAI_MODEL_LIST` | （无对应属性） | 可用模型清单，逗号分隔 |
| `ANTHROPIC_API_KEY` | `spring.ai.anthropic.api-key: ${ANTHROPIC_API_KEY}`（属性名已经 E8 实测生效） | SDK 标准名 |
| `ANTHROPIC_BASE_URL` | ⚠️ **不直接映射**（与 OpenAI 腿同规则）：base-url 非敏感，Spring AI 侧直接写 yaml——`spring.ai.anthropic.base-url: https://api.minimaxi.com/anthropic`（属性生效已经 E8 实测确认） | MiniMax Anthropic 兼容端点 |
| `ANTHROPIC_DEFAULT_MODEL` | `spring.ai.anthropic.chat.options.model: ${ANTHROPIC_DEFAULT_MODEL}` | 缺省模型 |
| `ANTHROPIC_MODEL_LIST` | （无对应属性） | 可用模型清单 |
| `MINIMAX_API_KEY` / `MINIMAX_BASE_URL` / `MINIMAX_DEFAULT_MODEL` / `MINIMAX_MODEL_LIST` | 无自动映射（正式实现由 `ProviderService` 显式构造实例时读取） | MINIMAX Provider |

## 3. 脚本：生成方法与要领

### 3.1 基本盘

- 位置：`~/.agent-os-poc/script/agent-os-env.sh`（git 仓库**外**，不在任何仓库内）
- 权限：600（只有本人可读写）
- 语言：纯 bash，无外部依赖
- 内容三段：**Provider 注册区 → 加载函数 → 状态函数**，末尾一个入口判断

### 3.2 三段结构与职责

| 段 | 名字 | 职责 |
|---|---|---|
| 1 | Provider 注册区 | 每个 Provider 一组 `<PROVIDER>_*` 四元组（按 2.2 模式命名），密钥的真实值只出现在这里 |
| 2 | 加载函数 `agentos_env_load()` | `case` 按 vendor 分支：把注册区变量映射成第 2.3 节的标准名并 `export`；未知 vendor 报错返回 1 |
| 3 | 状态函数 `agentos_env_status()` | 打印各槽位状态；**key 只输出前 5 位**（红线第 3 条） |

入口逻辑：无参数 = 加载全部 vendor；有参数 = 只加载指定的（如 `minimax`）。

### 3.3 密钥占位示例（注册区一块的样子；真实值以 `<你的密钥>` 占位）

```bash
OPENAI_API_KEY='<你的 API Key>'   # 当前值 = MiniMax 的 key（取值来源见 2.1 节）
OPENAI_BASE_URL='https://api.minimax.cn/v1'
OPENAI_DEFAULT_MODEL='MiniMax-M2.7'
OPENAI_MODEL_LIST='MiniMax-M3,MiniMax-M2.7,MiniMax-M2.7-highspeed'
```

### 3.4 新增 Provider 的操作清单（3 步，改的都是这个脚本）

1. 注册区加一组 `<PROVIDER>_*` 四元组（按 2.2 模式）
2. `agentos_env_load()` 的 `case` 加一个分支：export 自己的 `<PROVIDER>_*` 四件套
3. `agentos_env_status()` 加一行状态输出（key 保持 5 位前缀）

脚本内已留 deepseek、kimi 两个注释分支作样例。

## 4. 使用说明

### 4.1 加载（必须 source，不能直接执行）

```bash
source ~/.agent-os-poc/script/agent-os-env.sh           # 加载全部 Provider
source ~/.agent-os-poc/script/agent-os-env.sh openai    # 只加载指定 Provider
agentos_env_status                                       # 查看状态（key 只显示前 5 位）
```

### 4.2 Spring AI 项目接线（application.yaml 写法）

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}            # 密钥：只有它走环境变量
      base-url: https://api.minimax.cn      # 非敏感：明文写 yaml，注意不带 /v1（原因见 5.3）
      chat:
        options:
          model: ${OPENAI_DEFAULT_MODEL}    # 缺省模型
```

### 4.3 非 Spring AI 工具（OpenAI SDK、curl、Python）

直接用 Provider 标准名，和 MiniMax 官方文档写法一致：

```bash
export OPENAI_BASE_URL=https://api.minimax.cn/v1   # 已由脚本 export；OpenAI SDK 语境带 /v1
export OPENAI_API_KEY=...                          # 已由脚本 export
```

### 4.4 常见错误（都踩过的坑，别再踩）

| 错误 | 后果 | 正确做法 |
|---|---|---|
| 直接执行脚本（`./agent-os-env.sh`） | export 的变量随子进程消失 | 必须 `source` |
| 把 `OPENAI_BASE_URL`（带 /v1）映射给 `spring.ai.openai.base-url` | `/v1/v1` 双写，请求 404 | base-url 明文写 yaml 且不带 /v1 |
| 把密钥写进 yaml / 提交进仓库 | 违反红线第 1、2 条 | yaml 只写 `${OPENAI_API_KEY}` 占位符 |
| 日志打印完整密钥 | 违反红线第 3 条 | 最多输出前 5 位前缀 |
| 换取值来源时只改四元组中的一个值（如换了 endpoint 没换 key） | 配置指向不一致，请求失败或选错模型 | 换来源时整组四元组一起换（见 2.1） |

## 5. 定稿的设计决策（从过程稿收编）

### 5.1 为什么 OPENAI / ANTHROPIC Provider 用 SDK 标准名（而不是 `MINIMAX_OPENAI_*` 之类的自造名）

1. MiniMax 官方接入文档就是 `OPENAI_BASE_URL` / `OPENAI_API_KEY`、`ANTHROPIC_BASE_URL` / `ANTHROPIC_API_KEY`——导出名与官方一致，按官方文档写的任何工具直接复用
2. Spring AI 手动构造路径读的也是同名变量（官方示例：`new AnthropicApi(System.getenv("ANTHROPIC_API_KEY"))`）
3. 与 Spring AI 属性模式同构（`spring.ai.<provider>.api-key / .base-url / .chat.options.model`），新增 Provider 照模式扩展、不改结构
4. **配置值可整体替换**：Provider 名字（`OPENAI_*`）表达的是"接入哪个协议"，与背后是哪家供应商解耦——今天填 MiniMax 兼容端点，明天有原生账号直接改四个值，变量名与代码零改动

### 5.2 多 model 支持：provider 与 model 是两个维度

同一个 ChatModel 实例可以服务多个 model。三级选择（从静到动）：

| 级 | 谁决定 | 配置位置 |
|---|---|---|
| 1. Provider 缺省 | 部署方 | 环境变量 `*_DEFAULT_MODEL` → ChatModel 实例缺省 options |
| 2. Agent 覆盖 | Agent 作者 | AGENT.md frontmatter `settings.model` → 派生进 Profile → 每次调用带上 |
| 3. 运行时动态 | 独立的路由组件 | 按任务特征 / 成本 / 可用性输出 `(provider, model)`，写入调用 options |

### 5.3 MiniMax 两份文档是同一个端点（2026-09-06 核验）

MiniMax 提供的两份文档——"OpenAI SDK 方式"（base url `https://api.minimax.cn/v1`）与"OpenAI Chat Completion 方式"（请求 URL `https://api.minimax.cn/v1/chat/completions`）——是**同一个后端端点**的两种写法：前者是客户端用法指南，后者是原始 API 参考。差异在客户端的 URL 拼接惯例：OpenAI SDK 的 base_url 带 `/v1`（SDK 只追加 `/chat/completions`）；Spring AI 的 OpenAiApi 自己追加 `/v1/chat/completions`。证据：SAA 官方测试代码注释原话 "OpenAiApi appends /v1/chat/completions itself; do NOT include /v1 here"。结论（已采入 4.2 节 yaml 示例）：**Spring AI 侧 base-url 写 `https://api.minimax.cn`，不带 `/v1`**。

### 5.4 MiniMax 工具调用与思考标签（写实验代码要用）

- OpenAI 兼容端点支持工具调用：`tools` 请求 → `tool_calls` 响应 → 回灌 `tool` 消息 → 二次调用（官方 function calling 示例）
- Anthropic 兼容端点支持工具调用：`tools` + `tool_choice`（官方 OpenAPI 定义含 Tool Use 示例）
- 注意：MiniMax-M3 的思考内容默认混在返回文本的 `<think>` 标签里——实验断言要先剥掉 `<think>...</think>` 再比对；Anthropic 兼容端点用 `thinking: adaptive` 参数控制思考

### 5.5 已核验事实与出处

| # | 事实 | 出处 |
|---|---|---|
| 1 | Spring AI 1.0.x starter 坐标：`org.springframework.ai:spring-ai-starter-model-openai`、`org.springframework.ai:spring-ai-starter-model-anthropic` | Spring AI v1.0.3 官方文档（GitHub tag v1.0.3） |
| 2 | OpenAI 属性名：`spring.ai.openai.api-key / .base-url / .chat.completions-path / .chat.options.model`（官方 Perplexity 示例演示了 base-url 指向兼容端点 + completions-path 覆盖） | 同上 |
| 3 | Anthropic 属性名：`spring.ai.anthropic.api-key / .chat.options.model`；手动构造 `new AnthropicApi(System.getenv("ANTHROPIC_API_KEY"))` | 同上（anthropic-chat.adoc） |
| 4 | Spring AI 的 OpenAiApi 自动追加 `/v1/chat/completions`（base-url 不要带 /v1） | SAA 官方测试代码注释 + Spring AI 官方 Perplexity 示例（见 5.3） |
| 5 | MiniMax 双端点均支持工具调用 | MiniMax 官方文档 text-m3-function-call、text-chat-anthropic |
| 6 | Spring AI 1.x 自动执行工具默认开、2.0 全部移除 | Spring AI upgrade-notes（main 分支） |

补充核验（2026-09-06 晚，Context7 v1.0.3 官方文档）：

| # | 事实 | 出处 |
|---|---|---|
| 7 | per-call options 传入 `call` 的签名：`Prompt` 类持有 `List<Message> messages` + `ChatOptions chatOptions`；官方写法 `Prompt prompt = new Prompt("...", chatOptions); chatModel.call(prompt);`，per-call options 覆盖实例默认值 | Spring AI v1.0.3 api/prompt.adoc、api/tools.adoc（Groq / Vertex 运行时覆盖示例） |
| 8 | `spring.ai.anthropic.base-url`：3 次查询未在 v1.0.3 文档页命中该属性行（`api-key` 与 `chat.options.*` 已命中）。**2026-09-07 E8 实测：属性生效**——anthropic 腿按 yaml 配置闭环到 MiniMax 端点，"手动构造 `AnthropicApi(baseUrl, apiKey)`"备选方案无需启用 | Context7 三次检索记录 + spike E8 实测（`spike/007-react-loop/README.md`） |

## 6. 关联

| 对象 | 关系 |
|---|---|
| `spike/007-react-loop/spec/001-expirement.md` | 本脚本是 spike E3-E8（真实模型调用）的前置条件；E8 两条腿 = OpenAI 兼容腿 + Anthropic 兼容腿，单把 key 可跑满 |
| 正式实现第一周（Provider 抽象） | `ProviderService` 按 `Map<provider 名, ChatModel>` 显式构建（TS 3.2）；密钥按本文档的 Provider 变量名从环境变量读取 |
| `CLAUDE.md`「模型接入环境变量」节 | 使用规则的速查版；本文是完整定稿 |
| `chat/temp/20260906-model-config-export-design.md` | 过程稿（含决策演进记录）；与本文不一致处以本文为准 |

## 7. 附录：脚本全文（sample）

与真实文件 `~/.agent-os-poc/script/agent-os-env.sh` 的**唯一差异**：`MINIMAX_API_KEY` 的值替换为占位符 `'<你的 MiniMax API Key>'`（密钥红线：真实密钥只存在于仓库外脚本）。其余逐字一致；新增 Provider 时照第 3.4 节的三步操作改这份文件。

```bash
#!/usr/bin/env bash
# =====================================================================
# AgentOS 模型接入环境变量导出脚本
#
# 位置：~/.agent-os-poc/script/agent-os-env.sh
#   本文件在 git 仓库之外、权限 600，是 API 密钥在磁盘上的唯一落点。
#   禁止把本文件（或其中的密钥）拷贝进任何 git 仓库。
#
# 概念：环境变量按 Provider（供应商）命名，一个 Provider 一组四元组：
#   <PROVIDER>_API_KEY / <PROVIDER>_BASE_URL / <PROVIDER>_DEFAULT_MODEL / <PROVIDER>_MODEL_LIST
#   现有 Provider：OPENAI、ANTHROPIC、MINIMAX（并列、互不覆盖）。
#   OPENAI / ANTHROPIC 的四元组是"可整体替换的配置值"——当前只有 MiniMax 账号，
#   所以取值来自 MiniMax 的协议兼容端点；换成原生账号或其它兼容服务时，直接改注册区的值即可。
#
# 用法（必须 source，不能直接执行——export 要留在当前 shell 才对子进程生效）：
#   source ~/.agent-os-poc/script/agent-os-env.sh          # 加载全部 Provider
#   source ~/.agent-os-poc/script/agent-os-env.sh openai   # 只加载指定 Provider
#   agentos_env_status                                      # 查看状态（key 只显示前 5 位）
#
# 导出的环境变量（与 Spring AI 属性一一对应，项目的 application.yaml 只写 ${环境变量名} 占位符）：
#   OPENAI Provider（spring-ai-starter-model-openai）：
#     OPENAI_API_KEY         <-> spring.ai.openai.api-key
#     OPENAI_BASE_URL        <-> spring.ai.openai.base-url
#     OPENAI_DEFAULT_MODEL   <-> spring.ai.openai.chat.options.model（缺省模型）
#     OPENAI_MODEL_LIST      <-> 可用模型清单（项目约定层，逗号分隔；用于校验与发现，不做运行时选择）
#   ANTHROPIC Provider（spring-ai-starter-model-anthropic）：
#     ANTHROPIC_API_KEY       <-> spring.ai.anthropic.api-key
#     ANTHROPIC_BASE_URL      <-> spring.ai.anthropic.base-url
#     ANTHROPIC_DEFAULT_MODEL <-> spring.ai.anthropic.chat.options.model（缺省模型）
#     ANTHROPIC_MODEL_LIST    <-> 可用模型清单（项目约定层）
#   MINIMAX Provider：无 Spring AI 自动映射，由 ProviderService 显式构造实例时读取
#
# 运行时切换模型：不靠环境变量，靠每次调用的 options（如
# ToolCallingChatOptions.builder().model("MiniMax-M3")...）覆盖缺省值——
# 一个 provider（一个连接实例）可服务多个 model。
#
# 安全红线（目的：防止密钥被提交进代码库、或写进日志外泄）：
#   1) 密钥只允许经环境变量进入程序（未来可能迁移到专门存储秘钥的基础设施）；
#   2) 绝不从配置文件读取密钥；
#   3) 绝不把密钥完整写到命令行或日志——debug 输出最多前 5 位前缀。
#
# 扩展新 Provider：在下方"Provider 注册区"加一组四元组，再在 agentos_env_load() 的
# case 里加一个分支（文件末尾有 deepseek 的注释示例）。
# 设计定稿（生成方法 / schema / 使用说明 / 设计决策记录）：
#   仓库 docs/design/detail-supplement/001-model-config-export.md
#   （过程稿：仓库 chat/temp/20260906-model-config-export-design.md）
# =====================================================================

# ---------------- Provider 注册区（每个 Provider 一组四元组，新增在这里加一块） ----------------
#
# MINIMAX：供应商自己的注册项（以供应商名字注册的 Provider）。
# 域名说明：.cn 为国内站，.io 与 .minimaxi.com 为国际站；以下取值来自
# ~/.agent-os-poc/doc/api-key-doc.md 的 JSON 配置块，实测不通时两个域名可互为备选。
MINIMAX_API_KEY='<你的 MiniMax API Key>'
MINIMAX_BASE_URL='https://api.minimax.cn/v1'
MINIMAX_DEFAULT_MODEL='MiniMax-M2.7'
MINIMAX_MODEL_LIST='MiniMax-M3,MiniMax-M2.7,MiniMax-M2.7-highspeed'
#
# OPENAI Provider：OpenAI 协议的接入配置。当前取值 = MiniMax 的 OpenAI 兼容端点
# （只有 MiniMax 账号）；换成原生 OpenAI 账号或其它 OpenAI 兼容服务时，直接改这四行。
OPENAI_API_KEY="$MINIMAX_API_KEY"
OPENAI_BASE_URL='https://api.minimax.cn/v1'
OPENAI_DEFAULT_MODEL='MiniMax-M2.7'
OPENAI_MODEL_LIST='MiniMax-M3,MiniMax-M2.7,MiniMax-M2.7-highspeed'
#
# ANTHROPIC Provider：Anthropic 协议的接入配置。当前取值 = MiniMax 的 Anthropic 兼容端点
# （只有 MiniMax 账号）；换成原生 Anthropic 账号或其它兼容服务时，直接改这四行。
ANTHROPIC_API_KEY="$MINIMAX_API_KEY"   # 当前与 OPENAI 共用同一把 MiniMax key
ANTHROPIC_BASE_URL='https://api.minimaxi.com/anthropic'
ANTHROPIC_DEFAULT_MODEL='MiniMax-M3'
ANTHROPIC_MODEL_LIST='MiniMax-M3,MiniMax-M2.7,MiniMax-M2.7-highspeed'

# ---------------- 加载函数（新增 Provider 在 case 里加一个分支） ----------------

agentos_env_load() {
  case "$1" in
    openai)
      export OPENAI_API_KEY OPENAI_BASE_URL OPENAI_DEFAULT_MODEL OPENAI_MODEL_LIST
      ;;
    anthropic)
      export ANTHROPIC_API_KEY ANTHROPIC_BASE_URL ANTHROPIC_DEFAULT_MODEL ANTHROPIC_MODEL_LIST
      ;;
    minimax)
      export MINIMAX_API_KEY MINIMAX_BASE_URL MINIMAX_DEFAULT_MODEL MINIMAX_MODEL_LIST
      ;;
    # -------- 未来新增 Provider 的示例（注册区加四元组 + 这里加一个分支） --------
    # deepseek)
    #   export DEEPSEEK_API_KEY DEEPSEEK_BASE_URL DEEPSEEK_DEFAULT_MODEL DEEPSEEK_MODEL_LIST
    #   ;;
    # kimi)
    #   export KIMI_API_KEY KIMI_BASE_URL KIMI_DEFAULT_MODEL KIMI_MODEL_LIST
    #   ;;
    *)
      echo "[agent-os-env] 未知 Provider: $1（当前可用：openai / anthropic / minimax）" >&2
      return 1
      ;;
  esac
}

# 状态查看：key 只输出前 5 位前缀（安全红线，禁止完整输出）
agentos_env_status() {
  echo "[agent-os-env] Provider OPENAI:    base=${OPENAI_BASE_URL:-未设置}  key=${OPENAI_API_KEY:0:5}***  default_model=${OPENAI_DEFAULT_MODEL:-未设置}  可用模型: ${OPENAI_MODEL_LIST:-未设置}"
  echo "[agent-os-env] Provider ANTHROPIC: base=${ANTHROPIC_BASE_URL:-未设置}  key=${ANTHROPIC_API_KEY:0:5}***  default_model=${ANTHROPIC_DEFAULT_MODEL:-未设置}  可用模型: ${ANTHROPIC_MODEL_LIST:-未设置}"
  echo "[agent-os-env] Provider MINIMAX:   base=${MINIMAX_BASE_URL:-未设置}  key=${MINIMAX_API_KEY:0:5}***  default_model=${MINIMAX_DEFAULT_MODEL:-未设置}  可用模型: ${MINIMAX_MODEL_LIST:-未设置}"
}

# ---------------- 入口：无参数 = 加载全部 Provider；有参数 = 只加载指定 Provider ----------------

if [ $# -eq 0 ]; then
  agentos_env_load openai
  agentos_env_load anthropic
  agentos_env_load minimax
else
  for _provider in "$@"; do
    agentos_env_load "$_provider"
  done
fi
```
