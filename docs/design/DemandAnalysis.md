# AgentOS 需求文档

> 本文档定义 AgentOS 项目的功能需求和非功能需求，作为后续技术方案设计、研发实施、测试验收的依据。本文档回答 **What**，不回答 **How**，How 在后续的技术方案中展开。前置阅读《项目篇 AgentOS 业界调研》，本文档基于调研得出的领域判断，不重复论证企业 Agent OS 领域的现状。

---

## 1. 项目概述

### 1.1 AgentOS 是什么

AgentOS 是基于 Java 实现的面向企业场景的 **Agent OS**。它装在企业自己的 K8s 或服务器上，作为统一底座，在底座上跑各种业务 Agent（运维助手、客服助手、HR 助手、销售助手、知识管理助手等），共享一套渠道接入、模型路由、工具调用、记忆系统、沙箱执行、安全审计能力。数据完全留在企业自己的基础设施，不锁任何云生态。

业界已经有开源 Agent 项目把这套设计验证过（OpenClaw 用 Node.js，Hermes Agent 用 Python），但 Java 生态没有任何项目把"Agent OS"作为定位。Java 是大量企业现有后端的事实标准技术栈，Spring AI 已经把底层 LLM 调用解决了（本项目用 Spring AI 官方 starter 接入，SAA 只以 BOM 管版本——spike/007-react-loop/README.md 第二组 D5），缺的就是上面那一层"Agent OS"。AgentOS 填这个位置。

#### (1) Agent OS vs agent runtime 的分层

**Agent OS** 跟 **agent runtime**（Agent 运行时）不是一回事：

- **agent runtime**：让单个 Agent 跑起来的执行内核，负责 LLM 调用、工具执行、上下文管理、循环控制
- **Agent OS**：内核包含一个 agent runtime，但在 runtime 之上还要管多个 Agent 的生命周期、统一的对外对内接入、统一记忆、多租户、审计这些 OS 级治理能力

借操作系统类比，runtime 像单个进程的执行环境，Agent OS 像管理一群进程、调度资源、提供共享服务和治理的那层。一句话：runtime 让一个 Agent 跑起来，**Agent OS 让一群 Agent 在企业里被管起来**。

#### (2) 交付分两段

理解这个分层，才能看懂 AgentOS 的交付节奏：

1. **核心阶段**：先把 AgentOS 的运行时内核用 Java 做扎实，这一层在能力上对齐业界开源 Agent OS 的基础层
2. **扩展阶段**：AgentOS 真正的差异化治理层（多租户、SSO、完整审计、Tool 治理），在核心内核之上由扩展阶段和社区共建陆续补齐

核心阶段交付的是 AgentOS 的**内核底座**，而不是一个治理能力完备的企业级 AgentOS，后者是终局，核心阶段是地基。

---

### 1.2 AgentOS 能干什么

AgentOS 优先做五个核心能力，基于这五个能力可以扩展出企业里大量真实需求。这五个能力都属于"让单个 Agent 跑得好"的运行时内核层；让 AgentOS 成为真正"OS"的多 Agent 治理能力（多租户、Tool Policy、审计、SSO），在扩展和社区阶段补齐。

#### (1) 能力一：对接 LLM

AgentOS 通过 Provider 抽象层对接主流大模型，Agent 不感知具体调的是哪家模型，运行时切换无 lock-in（现行接入 openai/anthropic/minimax/zhipu 四家，deepseek/kimi 预留，见 5.3 Provider 抽象；无官方 starter 的厂商经 OpenAI 兼容腿兜底扩展）。

**基于这个能力可以做的事：**
- 任意业务场景的自然语言对话助手，Agent 通过 LLM 理解用户意图、给出回复
- 同一个 Agent 在不同任务用不同模型，简单任务走便宜模型、复杂任务走强模型
- 接入企业自有的本地推理服务（Ollama、vLLM），数据完全不出企业
- 多 Provider 编排，做一份报告可以让规划用便宜模型、综合用强模型

#### (2) 能力二：ReAct 循环

ReAct（Reason + Act）是 Agent 的核心工作机制：Agent 接到一个任务后，LLM 思考要不要调工具、调哪个工具，调用之后看结果，再决定下一步，直到给出最终响应。

**基于这个能力可以做的事：**
- Agent 能自主决定何时调用哪个工具，不需要业务方写死流程
- 多步骤任务可以一次对话内连续完成（先读文件、再分析、再调 API、再生成报告）
- Agent 出错时能自己回滚、重试、换工具
- 复杂业务流程不需要预先编排，Agent 在运行时动态决定执行路径

#### (3) 能力三：Memory 三层记忆

Agent 记得住用户的偏好、项目、决策、对话历史。三层记忆设计，核心阶段先实现会话和长期两层，情景记忆放扩展阶段补齐：

| 层次 | 说明 | 核心阶段 |
|------|------|---------|
| 会话记忆 | 当前对话的完整历史（存储全量永久保留；prompt 注入按 `max_history_turns` 截断保留近期，两口径见 5.9 Session 管理；总结压缩扩展阶段） | ✅ 实现 |
| 长期记忆 | 用户偏好、项目背景、关键事实，经 `LongTermMemoryStore` 后端存储（核心阶段交付 Markdown 默认档 MEMORY.md，按 `<Agent, 用户>` 二元组分档；接口预留 `memory.backend` 切换，SQLite/Mem0 档随后补齐），同一 `<Agent, 用户>` 内跨对话保留 | ✅ 实现 |
| 情景记忆 | 每个任务过程中学到的东西，修改了什么文件、做了什么决策 | ⏳ 扩展阶段 |

**基于这个能力可以做的事：**
- Agent 跨多次对话记住用户偏好（"我一般用 Spring Boot 不用 Spring MVC"）
- 长任务过程中状态保持，对话中断后能恢复继续做
- 同一个用户使用同一个 Agent 的偏好记忆跨会话保留（长期记忆按 `<Agent, 用户>` 二元组分档）；跨 Agent、跨团队的共享信息不通过 Memory 承载——共享需要受控的发布/审核机制，将来单独立项设计
- 历史决策可追溯（"上次为什么选 DeepSeek 不选 Kimi"在记忆里能查到）

#### (4) 能力四：Plugin 自定义工具 + 内置工具集

Agent 能调用工具实际操作系统。AgentOS 提供两类 Tool：

- **内置 Tool**：AgentOS 自带的基础工具（读写文件、执行 Shell、发起 HTTP 请求、记忆读写、通知推送）
- **Plugin Tool**：业务方自己扩展的工具，按门槛从低到高有三种方式

| 方式 | 门槛 | 做法 | 适用场景 |
|------|------|------|---------|
| 零代码 | 最低 | 写 Agent 目录（AGENT.md）+ 复用社区现成 MCP server | 业务方只描述意图，LLM 自己组合调用 |
| 轻代码 | 中等 | 用任何语言写 MCP server | 接入企业自有系统（ERP、CRM） |
| 重代码 | 最高 | 用 `@Tool` 注解写 Java Spring Bean | 深度集成，性能最好 |

**基于这个能力可以做的事：**
- 给 Agent 接入企业自己的 ERP、CRM、CMDB，让 Agent 真正能干企业的活
- 接 GitHub、Jira、Confluence，做研发助手
- 接 Prometheus、Grafana、SSH，做运维自愈
- 业务方零代码扩展，写 Agent 目录 + 复用 MCP，纯 markdown 就能上线新场景

#### (5) 能力五：Web Service

AgentOS 通过完整的 REST API 把所有能力对外暴露，业务系统用 HTTP 调一下就能用上 Agent，不用关心内部怎么实现。Web Service 是 AgentOS 的对外门面，是企业把 AI 能力嵌入已有业务系统的唯一通道。

API 覆盖八类操作：

| 类别 | 端点功能 |
|------|---------|
| 会话管理 | 创建会话、发消息、会话单查、会话列表（会话删除/清理属扩展阶段） |
| Agent 调用 | 无状态调用一次 Agent（每次调用独立创建单轮 Session，channel=`invoke`；流式响应扩展阶段补） |
| Profile 管理 | 列 Profile、看详情、重载 |
| Memory 操作 | 查长期记忆、手动写入、清理 |
| Tool 信息 | 列可用 Tool、看元信息 |
| 系统状态 | 健康检查、运行指标、Provider 状态 |
| 通知渠道管理 | notify-channels 列表/注册/更新/删除（webhook 推送目标，CRUD 4 个） |
| 定时任务管理 | 查任务状态、执行历史、立即执行、启停 |

> 注：类别覆盖全周期；核心阶段交付基础 10 + 第四周收尾 8 共 18 个端点（见 5.8 Web Service），系统状态类核心阶段交付 health/info 两个只读状态端点；运行指标（Prometheus metrics）、Provider 状态属扩展阶段，各类中的其余端点（如 Memory 写入/清理、Profile 详情/重载）亦属扩展阶段。

#### (6) 关于 Channel

核心阶段还有一个基础模块是 Channel（消息接入渠道）。Channel 主要解决"消息进来、响应出去"，核心阶段只内置 CLI 一种，企业微信、飞书、钉钉等 IM Channel 放扩展阶段。Channel 是核心功能模块，但它不算"五大核心能力"之一。

#### (7) 五个能力组合可以解决的场景

| 场景 | LLM | ReAct | Memory | Tool | Web Service |
|------|-----|-------|--------|------|-------------|
| 全渠道客服 | 理解用户问题 | 循环调知识库 | 记住客户历史 | 接 CRM | HTTP 接入客服系统 |
| 运维助手 | 分析告警 | 调日志查询+重启 | 记住历史故障 | 接 Prometheus/SSH | Webhook 触发 |
| 研发助手 | 理解需求 | 读代码改代码 | 记住项目惯例 | 接 GitHub/CI | IDE 插件接入 |
| 知识管理 | 理解问题 | 检索文档 | 记住团队约定 | 接 Confluence | 内网门户嵌入 |
| 销售助手 | 拼装客户画像 | 调 CRM+企查查 | 记住客户偏好 | 接销售系统 | 销售 App 调用 |
| 数据分析 | 生成 SQL | 执行查询+出图 | 记住业务表结构 | 接 BI 系统 | BI 工具集成 |

---

### 1.3 文档定位

本文档按三档分级定义 AgentOS 的功能需求：

1. **核心功能**：最短链路，跑通"配置一个 Agent、跟它对话、它能调用工具"这件事，对应 AgentOS 的运行时内核
2. **扩展功能**：生产级使用必需但不在核心链路上的能力，包含企业级治理层（多租户、SSO、审计、Tool Policy）
3. **社区共建功能**：长期方向，开放给社区贡献

核心阶段按 **4 周节奏**组织，每周 3 小时实践，合计 12 小时。这是极强的时间约束，核心功能范围必须收得很紧，只覆盖运行时内核的最短跑通链路。

---

## 2. 术语和概念

为避免歧义，统一核心术语定义（对齐业界开源 Agent OS 事实标准）：

| 术语 | 定义 |
|------|------|
| **Agent（智能体）** | 一个具象的业务智能体，定义本体是一个目录 `.agentos/agents/<name>/`：`AGENT.md`（frontmatter 是运行配置、正文是任务指令）加可选 `skills/` 公共 Skill 软连接、`scripts/` 脚本、`REFERENCE.md` 参考。一个目录就是一个完整可用的业务 Agent，不是写代码写出来的 |
| **Profile（配置）** | 底座内部的运行时宿主配置对象，决定一个 Agent"怎么跑"：绑定的 LLM Provider、可用 Tool 列表、Channel、Tool Policy（扩展阶段）和定时规则。它不再是一份单独手写的 YAML——`AgentLoader.deriveProfile()` 把 `AGENT.md` 的 frontmatter 派生成 `Profile`；Skill 绑定由 Agent 本地 `skills/` 软连接集合独立表达 |
| **Provider（供应商）** | LLM API 服务的抽象，实现统一接口让 Agent 不感知具体调的是哪家模型 |
| **ReAct 循环** | Agent 的核心工作机制，Reason + Act。LLM 思考是否调用工具，调用后看结果，再决定下一步，直到给出最终响应 |
| **Tool（工具）** | Agent 可以调用的外部能力。内置 Tool 是 AgentOS 自带的（文件、Shell、HTTP、记忆、通知推送）；Plugin Tool 是业务方自己写的 |
| **Memory（记忆）** | Agent 的记忆体系，分三层：会话记忆（当前对话）、长期记忆（按 `<Agent, 用户>` 二元组分档，见 TechnicalSolution.md - 5.1 模块组成；同一二元组内跨对话保留，经 `LongTermMemoryStore` 存储，核心阶段 Markdown 默认档；接口预留 `memory.backend` 切换，SQLite/Mem0 档随后补齐）、情景记忆（扩展阶段） |
| **Channel（渠道）** | Agent 对外接入的消息入口，包括 CLI、企业微信、飞书、钉钉、Slack 等 |
| **Web Service** | AgentOS 对外暴露的完整 REST API，是业务系统集成 AgentOS 的唯一通道 |
| **Session（会话）** | 用户和 Agent 一次对话的上下文容器，包含对话历史、当前上下文、临时变量；`session_id` 为四元组 `<channel>-<user>-<profile>-<uuid>`（uuid 会话创建时生成，对调用方是不透明串，见 5.9 Session 管理） |
| **User（用户标识）** | 调用方身份标识，格式 [a-z0-9-_]{1,32}；CLI 经 --user、Web 与 invoke 经 X-User-Id 头、钟推经 schedules.user 传入，缺省 default；决定会话身份（session_id 四元组的 user 段，sessions 表 user_id 独立列）与长期记忆档的隔离维度（见 TechnicalSolution.md - 5.1 模块组成 / 9.2 SQLite 关系型数据） |
| **Sandbox（沙箱）** | 工具执行前的策略校验层（核心阶段应用层白名单；受控执行的容器/microVM 隔离是扩展阶段另立的 `execute_code` Runner） |
| **Tool Policy（工具策略）** | 控制 Agent 可用工具的允许或拒绝规则，在 Profile 级别配置（扩展阶段能力；核心阶段由 frontmatter 的 `tools` 字段充当雏形，见 6.3 工具和安全层 与 TechnicalSolution.md - 6.7 Sandbox 检查） |
| **Skill（技能）** | 公共实体存 `.agentos/skills/<name>/`，Agent 通过自身 `skills/<name>` 相对软连接选择可见集合。`ContextLoader` 每迭代只注入已绑定 Skill 的 name、description 和本地读取路径；正文与附属资源经 `read_file`/`shell` 按需进入上下文。Skill 不是 Tool，不进 `ToolRegistry` |
| **Bootstrap（引导文件）** | 加载到系统提示词中的上下文文件：AGENTS.md（项目级 agent 行为说明）、SOUL.md（agent 人格定义）、USER.md（用户偏好） |
| **Workspace（工作区）** | AgentOS 实例的工作目录，默认是 `.agentos/`，包含 Agent 目录、全局 Skill 库、Bootstrap 文件、记忆、日志等子目录，以及 `mcp_servers.yaml` 与会话数据（agentos.db） |

---

## 3. 设计目标

AgentOS 的核心目标可以用四个词概括：**统一、私有、易接入、可观测**。

| 目标 | 说明 |
|------|------|
| **统一** | 企业内多个业务 Agent 共享同一套底座。Channel、Provider、Tool、Memory、Sandbox 这些公共能力下沉到 AgentOS，企业上一个新 Agent 放一个 Agent 目录（一份 `AGENT.md`）就能跑起来 |
| **私有** | 数据完全留在企业自己的基础设施上，部署在企业自己的 K8s、虚拟机或物理机上。AgentOS 本身不收集任何企业数据 |
| **易接入** | 基于 Spring Boot 的标准 Java 工程结构，跟企业现有的 ERP、CRM、CMDB、SSO、监控系统直接对接，运维工具链复用现有 Java 生态 |
| **可观测** | 标准的 Prometheus 指标、结构化 JSON 日志、健康检查接口、Web 仪表板，适配企业现有监控告警体系 |

---

## 4. 典型场景

以下三个典型场景描述 AgentOS 完整形态（含扩展阶段能力）下的目标用法，核心阶段先具备其运行时内核。

### 4.1 场景一：运维助手

某中型 SaaS 公司的运维团队基于 AgentOS 搭一个运维助手，接入企业微信。Agent 配了几个 Tool（告警分诊、日志查询、服务重启、变更审批）。凌晨告警通过 webhook 进 AgentOS，Agent 收到告警后调用日志查询 Tool 拉错误堆栈，跟历史故障库交叉引用发现是已知 bug，自动应用 mitigation Skill 重启服务，在企业微信运维群里汇报"已自愈，详情见附件"，值班工程师早晨起来看下记录就行。

**AgentOS 在此场景的角色**：Channel 接入（企业微信）、模型路由（主备 LLM Provider）、Tool 调用（SSH、Prometheus、Slack 通知）、Memory（历史故障库）、Skill（自愈 runbook）。

### 4.2 场景二：知识管理助手

某金融企业的法务团队基于 AgentOS 搭一个知识管理 Agent，接入飞书。Agent 索引了内部的合同模板、法规文档、历史案例、咨询记录。员工在飞书里问"上次签 SaaS 服务协议是怎么处理数据出境条款的"，Agent 检索 Memory 拉出历史案例，综合相关法规给出建议草稿，标注引用来源。

**关键点**：Memory 检索准确度和引用追溯（合规要求所有 Agent 回复必须可追溯到引用源）。

### 4.3 场景三：销售助手

某制造业企业的销售部门基于 AgentOS 搭一个客户洞察 Agent，接入企业微信和 CRM。销售跑客户前问 Agent"明天去拜访 A 公司，有什么我需要知道的"，Agent 调用 CRM connector 拉客户历史交易记录，调用企查查 MCP 工具查最新工商信息，调用知识库 Tool 提取关键决策人和采购习惯，综合输出客户简报。

**AgentOS 在此场景的核心能力**：MCP 集成（外部数据）、企业 IT 系统 connector（自家 CRM）、Tool 编排。

---

## 5. 核心功能

> 核心功能是核心阶段 4 周（合计 12 小时）内必须完成的最短链路，对应 AgentOS 的运行时内核。目标是跑通一个完整链路：用 Profile 配置一个 Agent，通过 CLI 跟它对话，它能调用 LLM 和工具完成任务，并能通过 REST API 对外暴露。

### 5.1 工作区初始化

AgentOS 的工作目录是 `.agentos/`，通过 `agentos init` 命令初始化。

```bash
agentos init   # 在当前目录下创建 .agentos/ 工作区
```

初始化后的目录结构：

```text
.agentos/
├── agents/            # 每个子目录 = 一个 Agent（AGENT.md + skills/软连接 + scripts/ REFERENCE.md）
├── skills/            # 公共 Skill 实体库（每个子目录一个 SKILL.md + 可选附属资源）
├── memory/            # 长期记忆根目录（按 <agent>/<user>/ 分档，首次 save_memory 自动创建；见 TechnicalSolution.md - 5.1 模块组成）
├── logs/              # 结构化日志
├── mcp_servers.yaml   # MCP server 全局配置
├── agentos.db         # SQLite（核心阶段 7 张表：sessions、session_messages、tool_invocations、llm_calls 随第三周 SQLite 落地，notify_channels、scheduled_tasks、task_executions 随第四周收尾交付；另预留 memory_entries 条件表，扩展阶段 SQLite 记忆档启用时才建。见 10 数据模型与 TechnicalSolution.md - 9.2 SQLite 关系型数据）
├── AGENTS.md          # Bootstrap：项目级 agent 行为说明
├── SOUL.md            # Bootstrap：默认 agent 人格定义
└── USER.md            # Bootstrap：用户偏好
```

- 三个 Bootstrap 文件在 Agent 启动时被自动加载到系统提示词，让 Agent 知道项目背景、自己的身份、用户偏好
- init 同时生成空的 `mcp_servers.yaml` 模板；`agentos.db` 由 JPA 在首次启用 SQLite 持久化（第三周 serve）时自动创建
- `agentos init` 幂等：已存在的目录和文件一律不覆盖；缺失的目录补建，缺失的文件跳过不补（删掉的模板文件 init 不恢复；2026-10-01 Q8（init 幂等边界）裁决，细则见 TechnicalSolution.md - 8.1 工作区初始化）。四个子目录建好后，用 `agentos profile create <name>` 生成第一个 Agent 目录

---

### 5.2 定义一个 Agent：AGENT.md

一个目录 = 一个 Agent。`.agentos/agents/<name>/AGENT.md` 由两部分组成：**frontmatter** 是这个 Agent 自己的 profile（用哪个 Provider/模型、能用哪些 Tool、绑定哪个 Channel、要不要定时），**正文**是任务指令。`AgentLoader.deriveProfile()` 把 frontmatter 派生成底座认识的 `Profile`。Agent 的 Skill 绑定不写进 frontmatter，而由同目录 `skills/` 下的相对软连接表达。

**AGENT.md frontmatter 结构：**

```yaml
name: string                    # Agent 名（= 目录名）
description: string             # 描述

identity:
  agent_name: string            # Agent 名称
  prompt: string                # 人格/系统提示词（或引用 SOUL.md）

provider:
  name: string                  # Provider 名称（现行 openai/anthropic/minimax/zhipu，deepseek/kimi 为预留）
  temperature: float            # 温度参数（可选）

tools:
  - string                      # 可用 Tool 名称列表（只认内置与方式三裸名；缺省=全部内置与方式三可见，显式列举即收窄；MCP 工具不在此列，见 TechnicalSolution.md - 6.6 ToolRegistry）

mcp_servers:
  - string                      # 引用的 MCP server 列表（声明的 server 其全部工具整组可用，暴露名 <server 名>__<工具名>，见 TechnicalSolution.md - 6.4 Plugin Tool 方式二 / 6.6 ToolRegistry）

channels:
  - name: string                # Channel 名称（核心阶段仅校验合法性，触发不按此过滤，见 TechnicalSolution.md - 8.2 Profile 配置；2026-09-29 Q2（channels）裁决）
    config: {}                  # Channel 配置

schedules:                      # 定时任务（可选）
  - id: string                  # 任务 id，Agent 内唯一（同 Agent 内同 id 后条跳过并记日志，见 TechnicalSolution.md - 8.2 Profile 配置）；全局寻址键为组合 task_id = <agent>__<id>（2026-09-29 E1-A（修订）裁决，表定义见 TechnicalSolution.md - 9.2 SQLite 关系型数据，契约见 docs/design/detail/api.md）
    cron: string                # cron 表达式（Spring CronTrigger 六段式：秒 分 时 日 月 周，见 TechnicalSolution.md - 8.5 定时任务）
    timezone: string            # 时区，可选（缺省 = 部署机器系统时区，见 TechnicalSolution.md - 8.5 定时任务；如 Asia/Shanghai）
    message: string             # 触发时注入的消息
    user: string                # 可选，定时触发的会话与记忆身份，缺省 default（见 TechnicalSolution.md - 8.5 定时任务 / 5.1 模块组成）

bootstrap:
  - string                      # Bootstrap 文件列表

settings:
  max_iterations: 10            # 最大 ReAct 迭代次数
  max_history_turns: 20         # 最大对话历史轮数
  model: string                 # 可选，Agent 级模型覆盖（三级选择：Provider 缺省来自环境变量 *_DEFAULT_MODEL → 此处 settings.model → 运行时经每次调用的 options 动态路由；运行时切换模型不走环境变量，详见 docs/design/detail/model-config.md）
  timeout: {tool: 30s, total: 300s}  # 可选，分步超时预算，仅 tool/total 两档按 Agent 覆盖（LLM 单次调用超时仅全局）；默认值见 TechnicalSolution.md - 7.4 关键设计点
```

**Agent 管理命令**（命令组名沿用 `profile`，操作的是 `.agentos/agents/` 下的 Agent 目录）：

```bash
agentos profile create <name>    # 创建 Agent（生成最小 AGENT.md 模板）
agentos profile list             # 列出全部 Agent
agentos profile show <name>      # 查看某个 Agent 的 AGENT.md
agentos profile delete <name>    # 删除 Agent（整个目录；记忆档 memory/<name>/ 不删、保留并输出提示——记忆属用户数据；归档策略扩展阶段定）
```

核心阶段支持创建并管理多个 Agent，多个 Agent 可以在同一个 AgentOS 实例上并存，这是"OS"在核心阶段的最小体现。

---

### 5.3 Provider 抽象（核心能力一：对接 LLM）

Provider 是 LLM 调用的统一抽象。所有 LLM 调用通过 Provider 接口走，Agent 不感知具体调的是哪家。

核心阶段直接基于 Spring AI 官方 starter 的 `ChatModel` 实现（调用侧经 `ChatClient` 封装；MiniMax 走原生 starter `spring-ai-starter-model-minimax`，spike/007-react-loop/README.md 第二组 D5）。Spring AI 官方 starter 已覆盖主流 LLM（MiniMax、OpenAI、Anthropic、智谱等），AgentOS 把它们包装成 Provider，不重复造轮子；Spring AI 没有官方 starter 的 Provider 经 OpenAI 兼容腿显式构造接入（兜底模式，spike/007 第二组 V9/D8）。

每个 Provider 实例配置：
- `provider 名`（现行 openai / anthropic / minimax / zhipu；deepseek / kimi 为将来新增预留）
- `模型名`
- `API key`
- `可选的 base URL`

> 注：API key 一律经环境变量注入，密钥只放仓库外脚本 `~/.agent-os-poc/script/agent-os-env.sh`，变量名按 Provider 命名——`OPENAI_*` / `ANTHROPIC_*` / `MINIMAX_*` / `ZHIPU_*` 各一组四元组、彼此并列；base URL 非敏感、可直接写配置文件，但 Spring AI 的 OpenAI 腿 base-url 不带 `/v1`（框架会自动追加 `/v1/chat/completions`，带了会 `/v1/v1` 双写 404）。变量 schema 与 Spring AI 属性对照详见 docs/design/detail/model-config.md

**核心阶段不做**：fallback 和 hedge racing。Provider 故障时直接报错给 Agent；成本透明只做基础版（每次 LLM 调用记录 token 使用量、Provider、模型落到 SQLite 审计表 llm_calls，日志仅辅助）。

---

### 5.4 ReAct 循环（核心能力二：Agent 大脑）

ReAct 循环是 Agent 的核心工作机制，也是 AgentOS 最关键的一段代码。

**核心算法（Reason + Act）：**

```text
接到用户消息
  └─ 追加到 Session 对话历史
     └─ 组装 Prompt（system prompt[含已绑定 Skill 元数据] + Bootstrap + Memory 注入[仅长期记忆] + 对话历史 + 可用 Tool 列表，五部分见 TechnicalSolution.md - 4.2 模块组成）
        └─ 调用 LLM Provider 获取响应
           ├─ [无 Tool 调用] → 返回最终响应
           └─ [有 Tool 调用] → 执行 Tool，把结果追加到对话历史 → 继续循环
```

达到最大迭代次数（默认 10 次）强制结束。

**实现要点：**
- 核心循环约数十行 Java 代码，自己实现而不依赖 Spring AI 的 Agent 抽象
- 最大迭代次数可在 Profile 里覆盖
- 每次 LLM 调用和 Tool 调用都记录结构化日志

**核心阶段不做**：Tool 调用并行、上下文动态压缩、Agent 间任务委托。

---

### 5.5 Memory 三层记忆（核心能力三：让 Agent 记得住）

**核心阶段实现会话 + 长期两层（情景记忆放扩展阶段）：**

#### (1) 会话记忆

- 当前对话的完整历史，按 `session_id` 标识（四元组 `<channel>-<user>-<profile>-<uuid>`，见 5.9 Session 管理）
- Session 数据持久化到本地 SQLite，会话数据永久保留，重启后可查询（`agentos session show` / `GET /api/v1/sessions/{id}`）、Web 凭 session_id 可续聊
- prompt 注入按 `max_history_turns`（默认 20 轮）截断保留近期（消息逐行存 `session_messages` 表、全量永久保留——存储口径与注入口径分离，见 5.9 Session 管理；不探测 context window，以轮数预算兜底；总结压缩放扩展阶段）

#### (2) 长期记忆

- 经 `LongTermMemoryStore` 统一存取，按 `<Agent, 用户>` 二元组分档、同一 `<Agent, 用户>` 内跨对话保留；核心阶段交付 Markdown 默认档（MEMORY.md），接口预留 `memory.backend` 切换，SQLite/Mem0 档随后补齐
- Agent 通过两个内置 Tool 主动读写：
  - `save_memory(content, scope)`：把要长期记住的事写入存储，带 scope 参数（CORE 核心区/ARCHIVAL 归档区，默认 ARCHIVAL）
  - `recall_memory(query)`：检索相关内容
- Markdown 档下每份档的 MEMORY.md 按"## 核心记忆/## 归档记忆"两分区组织：核心区永不被截断，截断只作用归档区
- 每次组装 prompt 时经 `MemoryService` 注入长期记忆（核心区全量 + 归档区截断）（预算键 `agentos.memory.archival-max-chars`，定义见 TechnicalSolution.md - 5.1 模块组成）

**核心阶段不做**：自动从对话中抽取事实、语义检索（Markdown/SQLite 档用关键词匹配；Mem0 档自带语义检索，用不用取决于后端选择）、情景记忆、Memory Wiki、矛盾检测。

**用户核心体验**：用 AgentOS 一段时间后，Agent 自然会记住用户的偏好、项目信息、关键决策，下一次对话不需要重新解释。这是 AgentOS 区别于 chatbot 的核心体验。

---

### 5.6 Tool 体系（核心能力四：让 Agent 能干事）

Tool 是 Agent 可以调用的外部能力。Agent 通过 LLM Function Calling 决定何时调哪个 Tool，AgentOS 负责 Tool 的注册、查找、调用、结果回传。

#### (1) 内置 Tool（核心阶段 9 个）

| Tool | 类型 | 说明 |
|------|------|------|
| `read_file` | 文件 | 读取文件内容，受路径白名单限制 |
| `write_file` | 文件 | 写入文件内容，受路径白名单限制 |
| `list_dir` | 文件 | 列出目录，受路径白名单限制 |
| `shell` | Shell | 直接执行白名单内可执行文件与参数数组（不经 Shell 解释），带超时；解释器仅在管理员显式列入白名单时可用（见 TechnicalSolution.md - 6.7 Sandbox 检查） |
| `http_get` | HTTP | 发起 HTTP 请求，有域名白名单限制 |
| `http_post` | HTTP | 发起 HTTP 请求，有域名白名单限制 |
| `save_memory` | Memory | 把内容经 `LongTermMemoryStore` 写入长期记忆（`scope` 指定 CORE/ARCHIVAL 分区，写入当前 `<Agent, 用户>` 档（默认 Markdown 档）） |
| `recall_memory` | Memory | 按关键词检索当前 `<Agent, 用户>` 档的归档区 |
| `notify` | 通知 | 通过 NotifyChannelAdapter 推送消息，随第四周定时任务与通知收尾一并交付 |

#### (2) Plugin Tool（业务方扩展）

| 方式 | 门槛 | 推荐度 | 场景 |
|------|------|--------|------|
| **方式一**：写 Agent 目录（AGENT.md）+ 复用 MCP server | 零代码 | ⭐⭐⭐ 主推 | 描述意图，LLM 自己组合现成能力 |
| **方式二**：自己写 MCP server | 轻代码 | ⭐⭐ | 接入企业自有系统，任何语言皆可 |
| **方式三**：写 Java @Tool Bean | 重代码 | ⭐ | 深度集成，性能最好 |

> **选择原则**：能用方式一就不用方式二，能用方式二就不用方式三。

**零代码示例**：想做"每天早上推送昨日 GitHub PR 评审进度到 Slack"，只需：
1. 建 `.agentos/agents/daily-pr-digest/`，写一份 `AGENT.md`：frontmatter 声明 provider、`mcp_servers`、`schedules`，正文写任务指令
2. 复用社区现成的 `github-mcp` 和 `slack-mcp`，配置在 `mcp_servers.yaml`
3. 需要固定产出格式就在 Agent 的 `skills/` 下绑定对应公共 Skill；下一迭代 prompt 自动出现其名称和描述，正文按需读取

整个过程不写一行代码。

#### (3) Sandbox 安全隔离

核心阶段用应用层白名单校验实现：
- 文件操作：路径白名单
- Shell：命令白名单
- HTTP：域名白名单
- 通知：独立域名白名单（`notify.allowed_domains`，与 HTTP 白名单分离——webhook URL 含 token 等凭证，不暴露给 `http_post`）
- 执行超时（Tool 单次 30s，见 TechnicalSolution.md - 7.4 关键设计点）；资源占用限制随扩展阶段 execute_code Runner 引入
- 覆盖边界：白名单校验只覆盖内置 Tool；MCP 工具与 `@Tool` Bean 工具不经此校验（治理边界见 TechnicalSolution.md - 6.7 Sandbox 检查）

> 注：不使用 Java SecurityManager，它在 JDK 17 起已废弃、JDK 21 已不可用。完整的 Docker/K8s 容器级沙箱放在扩展功能。

---

### 5.7 Channel 接入

Channel 是 Agent 对外的消息接入入口，主要解决"消息进来、响应出去"这件事。HTTP 接入归 Web Service，不在 Channel 范畴内。

核心阶段只内置一种 Channel：**CLI Channel**，通过 `agentos chat` 命令启动，支持多轮对话、查看上下文、查看 Tool 调用记录。

企业微信、飞书、钉钉、Slack 等 IM Channel 放在扩展功能（实现复杂度高，需要 OAuth 和企业资质，不在 12 小时核心阶段能完成的范围）。

---

### 5.8 Web Service（核心能力五：对外接口暴露）

Web Service 是 AgentOS 的对外完整门面，业务系统通过 REST API 接入 AgentOS 的所有能力。这是 AgentOS 区别于偏个人定位的 OpenClaw、Hermes 的关键能力。

#### (1) 核心阶段端点（基础 10 个）

| 类别 | 端点 | 说明 |
|------|------|------|
| 会话管理 | `POST /api/v1/sessions` | 创建会话 |
| 会话管理 | `POST /api/v1/sessions/{id}/messages` | 发消息 |
| 会话管理 | `GET /api/v1/sessions/{id}` | 查会话详情：7 项元数据（session_id、profile_name、channel、user_id、created_at、last_active_at、last_termination）+ messages 全量 |
| 会话管理 | `GET /api/v1/sessions?cnt=<N>` | 会话列表：按 last_active_at 倒序取前 N（缺省 100），各触发途径不区分；每项 3 字段（session_id、last_active_at、messages 首条预览） |
| Agent 调用 | `POST /api/v1/agents/{name}/invoke` | 无状态调用（每次调用独立创建单轮 Session，channel=`invoke`；响应体返回本次 session_id 及最终回复） |
| Profile 信息 | `GET /api/v1/profiles` | 列 Profile |
| Memory 操作 | `GET /api/v1/memory?agent=<name>` | 查长期记忆（`X-User-Id` 头定用户，缺省 `default`；见 TechnicalSolution.md - 7.2 核心阶段端点） |
| Tool 信息 | `GET /api/v1/tools` | 列可用 Tool |
| 系统状态 | `GET /api/v1/health` | 健康检查 |
| 系统状态 | `GET /api/v1/info` | 运行信息 |

收尾阶段（第四周定时任务与通知）追加 8 个（notify-channels CRUD 4 个：GET 列表/POST 注册/PUT 更新/DELETE 删除；schedules 管理 4 个），核心阶段合计 18 个，详见 TechnicalSolution.md - 7.2 核心阶段端点。API 线上契约（请求/响应体、参数约束、错误码全集）见 docs/design/detail/api.md。

#### (2) 扩展阶段补齐的端点

Agent 目录的上传/查看/更新/删除（含一句话生成 AGENT.md）、AgentScheduler 调度定义的增删改、Memory 的 append/clear/search、Tool describe 和调用历史查询、LLM call 历史、token 统计、Webhook 触发、流式 SSE 响应、Prometheus metrics、会话删除（原核心阶段 `DELETE /api/v1/sessions/{id}` 归档端点移此，届时语义=真删除/清理）、messages 分页（单 session 查询核心阶段不分页）。

**核心阶段不做**：认证机制（无认证假设内网）、流式响应 SSE、WebSocket、RBAC 权限。

#### (3) 业务系统集成场景

| 模式 | 方式 | 适用 |
|------|------|------|
| 同步调用 | `POST /agents/{name}/invoke` 等返回 | Stateless 短任务 |
| 会话保持 | 先创建 Session，后续多次发消息 | 连续对话 |
| Webhook 触发 | 告警系统、CI/CD 通过 Webhook 调 Agent（端点属扩展阶段，见本节"扩展阶段补齐的端点"；核心阶段可由外部系统定时调 invoke 代替） | 事件驱动 |
| 跨语言集成 | 任何能发 HTTP 请求的语言都能接入 | 通用集成 |

---

### 5.9 Session 管理

Session 是用户和 Agent 一次对话的上下文容器，包含用户身份、Agent 标识、对话历史、当前上下文、临时变量。`session_id` 为四元组 `<channel>-<user>-<profile>-<uuid>`：uuid（v4 全串，格式属实现细节）在会话创建时生成；session_id 对调用方是不透明串，channel/user/profile 三元组在 sessions 表有独立列，无需解析 id。

创建时机（按触发源）：① CLI：`agentos chat --profile <name> --user <id>` 启动时创建，session_id 保存在 CLI 进程内，进程退出即失联、下次启动是新会话（`--message` 单发同此）；② Web：`POST /api/v1/sessions` 显式创建，session_id 随响应体返回；③ 钟推：每次 cron 触发创建一个新 Session（单轮）；④ invoke：每次调用创建一个新 Session（单轮）；⑤ generate（扩展阶段）：`POST /api/v1/agents/generate` 每次调用创建一个新 Session（单轮），channel=`generate`、profile_name=`nan`、user 取 `X-User-Id` 头值（缺省 `default`，与 Web 入口同规则）（2026-10-09 Q5（generate 会话化）裁决）。复用语义：CLI 与 Web 凭同一 session_id 多轮持续；钟推、invoke 与 generate 每次执行只有一轮会话。

核心阶段 Session 数据持久化到 `.agentos/agentos.db`（SQLite，启用 WAL），会话数据永久保留：sessions 表不设 `status`/`archived_at` 列、无归档流程、无会话超时结束、无隐式 getOrCreate（创建一律由上述五途径显式发起）；对话历史逐行存 `session_messages` 消息行表、全量永久保留（同会话并发裁决，2026-09-25：对话历史由 sessions 单列改为消息行表），prompt 注入按 `max_history_turns`（默认 20 轮）截断——存储口径与注入口径分离（见 TechnicalSolution.md - 4.3 关键设计点）。会话可查：`agentos session show --session-id=<SID>` 与 `GET /api/v1/sessions/{id}` 返回 7 项元数据加 messages 全量；Web 凭 session_id 可续聊。

同一会话的并发发消息：平台层不设闸（无锁、不排队、不拒绝）。并发请求各自基于起点已提交快照展开、消息按轮原子提交落库（轮级原子穿插——单请求整轮原子入库，穿插只发生在轮与轮之间、单请求内部顺序完整）；客户端超时重试在两次尝试都完成时产生重复完整轮，业务方自律避免向同一会话并发发送；终态取最后完成请求，失败追溯以审计表与定时任务执行历史为准（行为细节见 TechnicalSolution.md - 7.2 核心阶段端点；同会话并发裁决，2026-09-25、2026-10-05 轮原子提交裁决）。

---

### 5.10 三种运行模式

| 模式 | 命令 | 说明 |
|------|------|------|
| 交互对话 | `agentos chat` | 交互式多轮对话，开发调试和日常使用的主要方式。`--message "xxx"` 可发单条消息后退出 |
| HTTP API | `agentos serve` | 启动后在指定端口（默认 8080）开放 RESTful 接口，业务系统通过 HTTP 调用 |
| 守护进程 | `agentos gateway` | 常驻守护进程，同时服务多个 Channel（核心阶段 = serve + 预挂 CLI Channel，HTTP 行为与 serve 完全一致；多 Channel 扩展阶段启用） |

三种模式共享同一份 Profile 配置和 Session 存储。

---

### 5.11 命令行工具

核心阶段实现 **13 个命令**：

| 类别 | 命令 | 说明 |
|------|------|------|
| 启动和状态 | `agentos init` | 初始化工作区 |
| 启动和状态 | `agentos status` | 查看配置和运行状态 |
| 启动和状态 | `agentos chat [--profile <name>] [--message <msg>] [--user <id>]` | 交互对话；--message 发单条消息后退出；--user 缺省 `default`；--profile 缺省：单 Agent 自动选用、多/零个报错列名单 |
| 启动和状态 | `agentos serve` | 启动 HTTP API 服务 |
| 启动和状态 | `agentos gateway` | 启动多渠道守护进程（核心阶段 = serve + CLI） |
| Profile 管理 | `agentos profile list` | 列出所有 Profile |
| Profile 管理 | `agentos profile create <name>` | 创建新 Profile |
| Profile 管理 | `agentos profile show <name>` | 查看 Profile 详情 |
| Profile 管理 | `agentos profile delete <name>` | 删除 Profile（记忆档 `memory/<name>/` 不删，见 5.2 定义一个 Agent） |
| 查询 | `agentos provider list` | 列出已配置的 Provider |
| 查询 | `agentos tool list` | 列出已注册的 Tool |
| 查询 | `agentos session list` | 按 last_active_at 倒序列出最近 N 个会话（缺省 100），每项 3 字段：session_id、last_active_at、messages 首条预览 |
| 查询 | `agentos session show --session-id=<SID>` | 查看单个会话：7 项元数据（session_id、profile_name、channel、user_id、created_at、last_active_at、last_termination）+ messages 全量 |

---

### 5.12 配置与密钥加载

核心阶段做基础版：

- 敏感配置（LLM API key 等密钥）只通过**环境变量**注入，禁止从配置文件读取，也不明文写死在 `AGENT.md` frontmatter 或 application.yaml 里
- 密钥统一放仓库外脚本 `~/.agent-os-poc/script/agent-os-env.sh`（source 加载、权限 600），git 仓库内任何文件只允许写 `${环境变量名}` 占位符；日志与命令行最多输出前 5 位前缀（环境变量命名与加载方法详见 docs/design/detail/model-config.md）
- Profile 里用 `${ENV_VAR}` 占位，加载时从环境变量解析
- 配置加载时做基础校验（必填项、格式），缺失或非法时给出清晰报错

完整的加密存储、密钥轮转、对接企业密钥管理系统（KMS、Vault）放在扩展阶段。

---

### 5.13 项目主页

AgentOS 作为开源项目，需要一个独立的主页作为对外门面，讲清楚 AgentOS 是什么、能干嘛、怎么用，引导开发者快速上手。

主页在核心阶段做出来，与核心代码同期发布，作为 AgentOS 1.0 对外亮相的一部分。技术栈推荐使用 VitePress、Astro 或 Docusaurus 等静态站点生成器。

---

## 6. 扩展功能

扩展功能在核心功能完成后推进，补齐生产级使用必需但不在最短链路上的能力，以开源社区方式陆续补齐。

### 6.1 渠道和模型层

- **多 Channel 接入**：企业微信、飞书、钉钉、Slack、邮件，通过 Channel Adapter 插件机制扩展
- **Provider Fallback 和可靠性**：三层 failover（hedge racing、circuit breaker、自动切换），Provider 故障时自动切换备用
- **Adaptive Routing**：模型路由从静态配置升级为动态决策，根据任务类型、历史调用质量、当前 Provider 负载自动选择

### 6.2 记忆和能力层

- **Memory 自动抽取**：LLM 在对话结束时自动提取值得长期保留的事实写入对应档
- **Memory 语义检索**：集成向量数据库（Milvus、Qdrant、Weaviate、PostgreSQL pgvector），按语义相似度匹配
- **情景记忆**：补齐 Memory 第三层，记录任务过程中修改的文件、决策、成果
- **Memory Wiki**：结构化 claim/evidence、矛盾检测、新鲜度管理
- **Skill 库增强**：公共 Skill 实体 + Agent 本地软连接绑定 + 元数据发现/正文按需加载在核心阶段落地；扩展阶段补语义推荐、版本管理和企业审查流程

### 6.3 工具和安全层

- **MCP server 暴露**：AgentOS 自己作为 MCP server，把内部 Agent 能力暴露给其他系统
- **Tool Policy**：Profile 级别的 Tool 允许或拒绝规则，这是 AgentOS 治理能力里最轻、最能体现 OS 管控的一项，扩展阶段优先做
- **Tool LRU 加载**：工具数量多时，动态加载，避免把所有工具塞进 LLM context
- **受控执行隔离**：容器/microVM 级经扩展阶段另立的 `execute_code` Runner 接口承载（Docker/K8s pod/WASM），`Sandbox.check` 继续作为前置校验

### 6.4 治理和运维层

> 这一层是 AgentOS 区别于个人级 Agent OS 的核心差异化所在

- **Web 仪表板**：提供 Web 仪表板做 Profile 管理、Session 查看、监控看板、审计日志查询
- **SSO 和多租户**：SAML/OIDC 接入，对接企业 AD/Okta/Entra ID/阿里云 IDaaS。三级租户模型（组织、部门、项目），RBAC 权限粒度到 Agent、Tool、Skill 级别
- **审计与可追溯**：完整审计事件记录、JSON 结构化输出、trace ID 串联、敏感信息脱敏、SIEM 导出
- **可观测性**：Prometheus 指标、结构化日志、健康检查接口、Grafana Dashboard 模板
- **集群化部署与高可用**：多节点协同通过 Nacos 或 ETCD 完成，节点故障自动迁移负载

### 6.5 企业集成层

- **企业 IT 系统 connector**：ERP（用友、金蝶、SAP）、CRM（销售易、纷享销客、Salesforce）、CMDB、监控系统、内网知识库的现成 connector

---

## 7. 社区共建功能

社区共建功能不在 AgentOS 主线开发计划内，作为长期方向开放给社区贡献，不规定时间表。

- **剩余项目文档**：API 参考文档、部署运维手册、贡献者指南 CONTRIBUTING.md、典型场景使用手册
- **Skills Marketplace**：社区贡献的 Skill 共享平台，兼容 agentskills.io 开放标准，跟 OpenClaw 和 Hermes Agent 兼容
- **SDK 多语言支持**：优先级 Java → Python → TypeScript → Go
- **可视化 Profile 编辑器**：让非工程师也能配置 Agent，接近 Dify 的 Agent 配置界面
- **Native 文件生成**：不依赖 LibreOffice 直接生成 pptx、docx、xlsx（Apache POI）
- **多区域部署**：跨地域 AgentOS 集群，Agent、Memory、Session 可以跨区域协同
- **Kubernetes Operator**：一键部署、声明式配置、GitOps 工作流
- **移动端管理台**：手机随时查集群状态、处理告警
- **Voice Channel**：语音唤醒和连续语音对话
- **RISC-V 和边缘部署**：跑在 Raspberry Pi、边缘网关（GraalVM Native Image）

---

## 8. 非功能需求

### 8.1 性能

| 指标 | 目标 |
|------|------|
| 单节点 Agent 数 | ≥ 10 个 |
| 单节点并发 Session 数 | ≥ 100 个 |
| Session 创建 P99 延迟 | ≤ 200ms |
| AgentOS 内部转发开销 | ≤ 50ms |

（LLM 调用本身的延迟取决于 Provider，不在 AgentOS 控制范围内）

### 8.2 可靠性

- 已注册的 Profile 配置和已写入的 Session 数据保证不丢
- LLM Provider 故障时核心阶段直接报错，完整 failover 在扩展阶段实现
- Tool 调用失败时返回可重试标识，由 Agent 自行决定是否再次调用；框架级自动重试（指数退避）放扩展阶段

### 8.3 可运维性

- 配置变更通过修改 `AGENT.md` frontmatter；AGENT.md 正文与 Skill 绑定修改后下一迭代 prompt 即生效（ContextLoader 每迭代现读），frontmatter 派生字段（provider/tools/schedules）变更需重启或触发重新加载生效（核心阶段新增 Agent 仍需重启）
- 支持物理机、虚拟机、Docker、Kubernetes 部署

### 8.4 兼容性

- **JDK**：21 及以上（Spring Boot 3.x 要求）
- **操作系统**：Linux 主流发行版（Ubuntu 22.04+、CentOS 8+、Debian 11+、Alibaba Cloud Linux 3、Rocky Linux）
- **LLM 协议**：OpenAI 兼容协议是事实标准，只要 Provider 实现这套协议，AgentOS 就能直接接

### 8.5 安全

- API 调用支持 HTTPS（经企业侧反向代理终结 TLS；应用内只出 HTTP、不处理证书，见 TechnicalSolution.md - 7.4 关键设计点）
- 敏感配置（LLM API key、数据库密码、Tool 凭证）支持加密存储，不能明文写在配置文件里
- Tool 调用通过应用层白名单校验做基础隔离（只覆盖内置 Tool；MCP 工具与 `@Tool` Bean 工具不经此校验，治理边界见 TechnicalSolution.md - 6.7 Sandbox 检查）
- 完整的鉴权机制、容器级受控执行（`execute_code` Runner）、SSO 集成放在扩展阶段

### 8.6 合规

- **数据驻留**：AgentOS 不主动外发任何数据，所有数据留在企业自己的基础设施上
- 完整的审计日志覆盖、SIEM 导出、SOC 2、GDPR、HIPAA、等保三级的对接放在扩展阶段

---

## 9. 关键流程

### 9.1 流程一：工作区初始化

```text
用户执行 agentos init
  → AgentOS 创建 .agentos/ 目录及四个子目录
    （agents / skills / memory / logs）
  → 创建三个 Bootstrap 文件（AGENTS.md、SOUL.md、USER.md）
  → 幂等：已存在的目录和文件一律不覆盖
用户编辑 Bootstrap 文件填入项目背景、Agent 人格、用户偏好
```

### 9.2 流程二：Agent 创建和启动

```text
用户执行 agentos profile create <name>
  → AgentOS 在 .agentos/agents/<name>/ 下创建 AGENT.md（最小模板）
用户编辑 AGENT.md：frontmatter 配 Provider、Tool 列表、Channel，正文写任务指令；需要 Skill 时在 Agent 目录 `skills/` 下建相对软连接
用户执行 agentos chat --profile <name>
  → AgentLoader.deriveProfile() 把 frontmatter 派生成 Profile
  → 初始化 Provider 连接
  → 注册 Tool 到 Agent 工具池
  → ContextLoader 把 AGENT.md 正文、Bootstrap 文件加载到系统提示词；已绑定 Skill 每迭代只注入 name/description/读取路径，正文经 read_file 按需进入上下文
  → Agent 进入待对话状态
```

### 9.3 流程三：消息处理（最高频链路）

```text
消息从三个入口进来（CLI Channel / Web Service HTTP API / AgentScheduler 钟推），均调 AgentService.process
  → Channel 触发经 Channel Adapter 转换成内部统一格式
  → Agent 查询 Session 上下文
  → 组装 LLM Prompt（system prompt（AGENT.md 正文，含已绑定 Skill 元数据）+ Bootstrap + Memory 注入（仅长期记忆）+ 对话历史 + 可用 Tool 列表，五部分见 TechnicalSolution.md - 4.2 模块组成）
  → 调用 LLM Provider 获取响应
  → [有 Tool 调用] → AgentOS 执行 Tool → 结果回传给 LLM 继续生成
  → 最终响应通过 Channel Adapter 发回给用户
  → 所有动作落结构化日志
```

### 9.4 流程四：Tool 调用

```text
LLM 通过 Function Calling 指明 Tool 名称和参数
  → AgentOS 从 Agent 工具池找到对应 Tool
  → 做 Tool 入参的 JSON schema 校验；白名单校验收口在 ToolExecutor 执行入口、仅覆盖内置 Tool（各 Tool 经 sandboxActions 申报动作；MCP 工具与 @Tool Bean 工具豁免——管理员配置信任边界，见 5.6 Tool 体系与 TechnicalSolution.md - 6.7 Sandbox 检查；注：shell 白名单仅精确比对可执行文件、参数不校验）
  → 内置 Tool：在 AgentOS 进程内执行（白名单约束下）
  → MCP Tool：通过 MCP 协议转发给对应 MCP server 执行
  → 执行结果（成功/失败、错误信息、可重试标识）回传给 Agent
  → Agent 把 Tool 结果作为下一次迭代的 LLM 输入继续推理
```

### 9.5 流程五：Session 上下文管理

```text
用户发起对话（CLI 启动 / Web 创建会话 / 钟推到点 / invoke 调用；user 来源：CLI `--user` / Web `X-User-Id` / 钟推 `schedules.user`，缺省 `default`）
  → 创建新 Session：session_id = <channel>-<user>-<profile>-<uuid>，创建即交付（CLI 进程内存 / HTTP 响应体 / task_executions 行）
  → 消息按轮原子提交入库（session_messages 行表，正常完成整轮一次事务、异常零提交），终止收尾写终止标记 last_termination（无归档动作）
  → [超过轮数预算] → prompt 注入按 `max_history_turns`（默认 20 轮）截断保留近期（session_messages 消息行全量永久保留；不探测 context window，以轮数预算兜底；总结压缩放扩展阶段）
```

---

## 10. 数据模型

### 10.1 Profile（由 AGENT.md frontmatter 派生）

结构见 [5.2 定义一个 Agent：AGENT.md](#52-定义一个-agentagentmd)。

### 10.2 Session（持久化到 SQLite）

| 字段 | 类型 | 说明 |
|------|------|------|
| `session_id` | VARCHAR | 主键，四元组 `<channel>-<user>-<profile>-<uuid>`（uuid v4 全串、会话创建时生成；对调用方不透明，channel/user/profile 有独立列、无需解析 id） |
| `profile_name` | VARCHAR | 关联的 Profile 名称 |
| `channel` | VARCHAR | 接入渠道 |
| `user_id` | VARCHAR | 用户标识 |
| `last_termination` | VARCHAR（可空） | 上次结束方式（`normal`/`exhausted`/`interrupted`/`failed`，语义见 TechnicalSolution.md - 4.3 关键设计点） |
| `created_at` | TIMESTAMP | 创建时间 |
| `last_active_at` | TIMESTAMP | 最后活跃时间 |

**Session Message（会话消息行表，同会话并发裁决 2026-09-25）**：对话历史逐行存 `session_messages` 表——`id`（自增，插入序）、`session_id`、`payload_json`（一条消息 JSON 原文）、`role`（消息角色，取值 `user` / `assistant` / `tool`，投影列）、`created_at`；多轮与单轮会话的消息行均永久完整保留（存储口径），prompt 注入另按 `max_history_turns` 截断（注入口径，两口径分离见 5.9 Session 管理）。字段全集、提交纪律与并发行为见 TechnicalSolution.md - 9.2 SQLite 关系型数据 / 7.2 核心阶段端点。

### 10.3 Memory（文件形态，非数据库表）

长期记忆按 `<Agent, 用户>` 二元组分档，每档是 `.agentos/memory/<agent>/<user>/MEMORY.md` 一个 Markdown 文件，按追加方式写入，无结构化 schema。切到 SqliteMemoryStore 档时落 `memory_entries` 表（scope 列区分 CORE/ARCHIVAL，隔离键 `agent_id`/`user_id` 两列（或按二元组分表）；扩展阶段 SqliteMemoryStore 档引入时建表），结构见 TechnicalSolution.md - 5.1 模块组成。扩展阶段引入向量库后，Memory 才有结构化的 embedding 存储。

### 10.4 Tool Invocation（记录每次 Tool 调用）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `session_id` | VARCHAR | 关联 Session |
| `tool_name` | VARCHAR | Tool 名称 |
| `input_json` | TEXT | 调用参数（JSON） |
| `result_json` | TEXT | 执行结果（JSON） |
| `success` | BOOLEAN | 是否成功 |
| `retryable` | BOOLEAN | 是否可重试（瞬态失败 true、确定性失败 false，取值口径见 TechnicalSolution.md - 4.2 模块组成 的 (4) ToolExecutor 模块 小节；成功调用统一落 false） |
| `error_message` | TEXT | 错误信息（可空） |
| `duration_ms` | BIGINT | 执行耗时（毫秒） |
| `created_at` | TIMESTAMP | 调用时间 |

### 10.5 LLM Call（记录每次 LLM 调用）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键 |
| `session_id` | VARCHAR | 关联 Session |
| `provider` | VARCHAR | Provider 名称 |
| `model` | VARCHAR | 模型名 |
| `prompt_tokens` | INT（可空） | 输入 token 数 |
| `completion_tokens` | INT（可空） | 输出 token 数 |
| `total_tokens` | INT（可空） | 总 token 数 |
| `duration_ms` | BIGINT | 调用耗时（毫秒） |
| `created_at` | TIMESTAMP | 调用时间 |

> 注：三个 token 列拿不到用量数据时落 NULL 而非 0——0 是实测为零，NULL 是拿不到，审计表不造数（spike/007 第一组 E7 实测 openai 腿取数路径，第二组 V6 补齐 anthropic 腿样本；设计评审 Q8 决议，2026-09-14）。

### 10.6 Scheduled Task（定时任务定义）

AgentScheduler 调度定义表（`scheduled_tasks`），字段见 TechnicalSolution.md - 9.2 SQLite 关系型数据。

### 10.7 Task Execution（定时任务执行历史）

定时任务每次触发的执行历史表（`task_executions`），字段见 TechnicalSolution.md - 9.2 SQLite 关系型数据。

### 10.8 Notify Channel（通知渠道）

webhook 推送目标注册表（`notify_channels`），字段见 TechnicalSolution.md - 6.8 通知推送。

---

## 11. 里程碑规划

AgentOS 核心功能的实施按 **4 周节奏**组织，每周 3 小时，合计 12 小时。

| 周次 | 时间投入 | 能力主线 | 可演示成果 |
|------|---------|---------|-----------|
| **第一周** | 3 小时 | 对接 LLM + ReAct 循环（核心能力一+二） | `agentos chat` 多轮对话，Agent 通过 ReAct 调 HTTP Tool 完成天气查询 |
| **第二周** | 3 小时 | Memory + Tool 体系（核心能力三+四） | Agent 记住用户偏好并在后续对话用到，能调本地文件和外部 MCP server |
| **第三周** | 3 小时 | Web Service（核心能力五） | 外部系统通过基础 10 个 REST 端点完整调用 AgentOS，会话数据跨重启保留、可查询与续聊（Web 凭 session_id） |
| **第四周** | 3 小时 | 多 Agent 演示 + 工程化收尾 | 多 Agent 并存可用，CLI 完整，两个验收 Demo 以钟推自动运行（含 notify 推送），scripts/ 最小链路演示通过，项目主页可访问 |

### 11.1 各周详细实施内容

**第一周**（3 小时）：对接 LLM + ReAct 循环
- `agentos init` 工作区初始化、`AGENT.md` frontmatter 解析
- Provider 抽象（基于 Spring AI 官方 starter，先跑通 MiniMax：当前唯一已配密钥的供应商，MiniMax 原生 starter 主用（spike/007 第二组 D5），环境变量与 Spring AI 接线详见 docs/design/detail/model-config.md；DeepSeek/Kimi 待密钥就绪后按该文档的新增 Provider 流程扩展。接入已由 spike 实测验证，结论见 spike/007-react-loop/README.md）
- ReAct 循环（核心循环约数十行 Java，含 LLM 调用、Tool 调用解析、消息累积）
- 一个基础内置 Tool（HTTP）、CLI Channel
- Session 管理（内存版，第三周 Web Service 阶段加 SQLite 持久化）

**第二周**（3 小时）：Memory + Tool 体系
- Memory 长期记忆（Markdown 默认档交付、按 `<Agent, 用户>` 二元组分档（`LongTermMemoryStore` 接口预留三档切换，SQLite/Mem0 档随后补齐），`save_memory`/`recall_memory` 两个内置 Tool，每迭代注入长期记忆（核心区全量 + 归档区截断），经 `MemoryService` 进入 PromptBuilder 的 Memory 段）
- 文件操作 Tool（read_file、write_file、list_dir）、Shell Tool（带白名单校验）
- MCP Client 集成（连接外部 MCP server）

**第三周**（3 小时）：Web Service + API 端点
- Web Service 基础 10 个 REST 端点（会话管理 4 个：创建、发消息、单查、列表；Agent 调用 1 个、Profile/Memory/Tool 列表 3 个、health/info 2 个）
- 通过 `agentos serve` 启动 Spring MVC 服务
- 配置与密钥加载（环境变量注入 + 基础校验）
- Session 持久化到 SQLite（会话数据跨重启保留、可查询，Web 凭 session_id 续聊）、命令行工具补齐至 13 个命令（含 `session show`）
- Bootstrap 文件机制补齐（AGENTS.md、SOUL.md、USER.md 加载到系统提示词）

**第四周**（3 小时）：多 Agent 演示 + 工程化收尾
- 多 Agent 演示（配置两个不同 Profile 的 Agent 在同一实例并存）
- AgentScheduler 定时任务（Profile `schedules` 字段驱动，第三触发源）
- NotifyTools 通知推送（`notify` 内置 Tool + WebhookNotifyAdapter + `notify_channels` 注册，两个 Demo 的推送依赖它）
- scripts/ 最小链路手工演示（`AGENT.md + scripts/` 形态：目录加载→脚本调起→产出进上下文→`tool_invocations` 有记录，见 TechnicalSolution.md - 12.3 关于 scripts/ 脚本的说明，不设独立 Demo）
- 结构化日志、项目主页（VitePress 或类似静态站点工具）

---

## 12. 风险与未决事项

### 12.1 已识别风险

| 风险 | 描述 | 应对措施 |
|------|------|---------|
| **核心功能范围风险** | 4 周 12 小时是极紧的时间约束，某些功能可能比预期复杂 | 核心功能范围卡紧；某周完不成时立刻把末段功能挪到扩展功能，保证每周有可演示成果 |
| **Spring AI 兼容性风险** | Function Calling、Stream、Token 计数、错误码细节不一致 | 核心阶段先把 OpenAI 协议跑稳，其他 Provider 在扩展阶段做完整回归测试 |
| **Tool 执行安全风险** | 应用层白名单不是完整 Sandbox，可能影响 AgentOS 进程 | 严格限制内置 Tool 能力范围，核心阶段不建议在生产环境跑高敏感场景 |
| **Java 启动速度和内存占用** | Java 应用启动慢、内存占用大，影响体验 | 核心阶段先验证功能完整，扩展阶段引入 GraalVM Native Image |
| **社区接力的不确定性** | 扩展功能依赖社区贡献者，可能某些功能长期没人推进 | 项目维护方对核心扩展功能保持基本投入，社区共建功能靠社区 |
| **定位被误读的风险** | 社区可能问"核心阶段跟 OpenClaw、Hermes 有什么区别" | 文档明确说明核心阶段是地基，差异化是终局，不包装成完整企业级 Agent OS |
| **生态关系风险** | AgentOS 和 OpenClaw、Hermes 的关系 | 通过 markdown + frontmatter 的目录形态互通，生态互补不竞争；OpenClaw 偏个人、Hermes 偏小团队、AgentOS 定位企业 |

### 12.2 未决事项

| 事项 | 说明 | 决议时间 |
|------|------|---------|
| GraalVM Native Image 引入时机 | 核心阶段还是扩展阶段 | 核心阶段结束后 |
| 各触发源下的 Prompt 组装策略 | 受 LLM 上下文窗口限制，人推（`agentos chat`、`POST /agents/{name}/invoke`）、钟推（`AgentScheduler` 定时触发）及其他后续触发方式（如 Webhook）下，组装进 LLM 请求的内容（Bootstrap、长期记忆注入、对话历史轮数）是否按触发源差异化调整 | 核心阶段结束后 |
| 工具返回结果的裁剪策略 | 当前设计中 Tool 返回结果不论体积、内容全部进入 Session messages 并随每次迭代的 prompt 进入 LLM 请求，无体积上限、裁剪、淘汰、压缩、截断机制；后续需增加工具返回结果裁剪能力（截断、过滤、摘要等），具体策略结合实测决议 | 核心阶段结束后 |
| 模型思考标签的用户可见处理 | 部分模型（已实测：MiniMax-M3 走 OpenAI 兼容端点）会把思考内容以 `<think>` / `</think>` 标签混在返回正文里，随最终响应进入 CLI 输出、会话历史与 Demo 正文。核心阶段决定不做后处理（原样透传，见 TechnicalSolution.md - 3.1"响应文本后处理边界"），W1 实施期用当前默认模型组合实测输出形态后，再裁决是否引入剥离及落点 | 第一周实施期实测后 |
| Anthropic 兼容腿对合并后报文的真实行为 | 组装侧合并相邻同角色消息后（拼接 user、assistant 内容，连续 tool_result 块），MiniMax /anthropic 兼容层的真实行为（合并/报错/其他）未经实测；官方 Anthropic Messages API 文档口径为连续同角色合并为单轮、不报错（原句与合并机制均见 TechnicalSolution.md - 4.2 模块组成；原句经 2026-09-25 查证，合并机制出自 2026-09-25 同会话并发裁决） | 第一周实施期实测后 |

> 注：原未决项 Provider 抽象接口设计、Bootstrap 文件加载顺序和优先级已决，见正文 5.3 Provider 抽象 与 TechnicalSolution.md - 4.2 模块组成；原挂账项"openai starter 多实例接入"已决（2026-09-15）——spike/007 第二组 V9 实证可行（显式构造 + `OpenAiApi.builder().completionsPath()` 覆盖，双 OpenAI 协议实例并存互不干扰），接入优先级为"官方 starter 优先、无 starter 才经 OpenAI 兼容腿显式构造兜底"，见 model-config.md - 2.1 Provider 清单与当前取值 与 spike/007-react-loop/README.md V9/D8；"LLM 超时按 Agent 覆盖"已于 2026-09-17 设计评审裁决从需求移除（裁决编号 Q5）——LLM 单次调用超时在 Spring AI 底层 HTTP 客户端构建时定死（全局值），调用 options 参数无超时字段可携带，且该项无验收场景支撑（"重推理 Agent 放宽预算"的真实诉求由总超时按 Agent 覆盖与 settings.model 换模型承接），扩展阶段同样不列，重新引入须有新需求场景并实测框架能力；5.2 定义一个 Agent 的 `settings.timeout` 由此只保留 `tool`/`total` 两键，机制定义见 TechnicalSolution.md - 7.4 关键设计点。

---

## 13. 验收标准

### 13.1 功能验收

核心功能（第 5 章核心功能）全部完成，每个功能模块至少有一个端到端测试用例覆盖：

- [ ] `agentos init` 工作区初始化
- [ ] Profile 配置和管理（支持多 Profile 并存）
- [ ] Provider 抽象（至少跑通一个 Provider：当前以 MiniMax 跑通，唯一已配密钥的供应商，经 MiniMax 原生 starter 接入（spike/007 第二组 D5）；密钥加载与环境变量命名详见 docs/design/detail/model-config.md；DeepSeek/Kimi 待密钥就绪后替换）
- [ ] ReAct 循环（多轮 Tool 调用、正确累积消息历史、达到最大迭代次数时正确终止）
- [ ] Memory 长期记忆（save_memory 写入、recall_memory 关键词检索、每迭代注入（核心区全量 + 归档区截断）；按 `<Agent, 用户>` 二元组分档）
- [ ] 内置 Tool（文件、HTTP、Shell、save_memory、recall_memory、notify）
- [ ] Plugin Tool 接入（方式一零代码 Agent 目录 + MCP 跑通；方式三 @Tool 注解示例跑通）
- [ ] MCP Client 集成、CLI Channel
- [ ] 定时任务 `AgentScheduler`（第三触发源，cron 到点自动触发，跟 CLI/Web Service 走同一条 `AgentService` 链路——复用的是处理链路、不是 Session，钟推每次触发新建会话）
- [ ] Web Service 端点全部跑通（基础 10 个 + 第四周收尾 8 个，见 5.8 Web Service）
- [ ] Session 持久化（SQLite，会话数据跨重启保留；凭 session_id 可查询（`agentos session show` / `GET /api/v1/sessions/{id}`）并可续聊（Web））
- [ ] 13 个命令行工具（含 `agentos session show`）
- [ ] 配置与密钥加载

### 13.2 性能验收

通过压力测试验证：
- 单节点 10 个 Agent 稳定运行 4 小时
- 单节点 100 个并发 Session
- Session 创建 P99 延迟 < 200ms
- 内部转发开销 < 50ms

### 13.3 可运维性验收

- 完整的部署文档（新手 30 分钟内完成单节点部署）
- 命令行工具有清晰的帮助和错误提示
- 项目主页可访问，讲清楚 AgentOS 是什么、怎么快速开始

### 13.4 场景验收（两个 Demo）

早期按"一个 Demo 验证一个能力"拆了五个 Demo，但真实场景从来不是单一能力独立跑的——一个能打动人的 Agent，一定是多个能力叠在一起、自己到点跑起来的。改成两个**每日自动运行**的端到端 Demo，每个 Demo 横向串起多个核心能力，两个 Demo 加起来覆盖全部五大核心能力加定时任务这个第三触发源。两个 Demo 跑通是核心功能发布的**硬条件**：

| Demo | 验证能力 | 场景描述 | 验收标准 |
|------|---------|---------|---------|
| **Demo 一：每日天气** | 能力一+二（LLM + ReAct）、能力四（内置 HTTP Tool）、能力五（Session 查询兜底）、定时任务（`AgentScheduler`） | 每天早上到点自动查天气、生成穿搭建议，推送到企业 IM 群 | 不需要人工触发，到点自动跑完整 ReAct 循环；查天气和推送各一次 HTTP 调用，分别过 HTTP 与 notify 各自的域名白名单且都写入 `tool_invocations`；本次触发的 session_id 落在 `task_executions.session_id`，凭它 `GET /api/v1/sessions/{id}` 能查到这次自动触发的对话记录（钟推单轮会话全量落盘，完整审计链路在 `tool_invocations`/`llm_calls`） |
| **Demo 二：每日科技日报** | 能力四（Plugin Tool 方式一 Agent 目录零代码 + 方式二 MCP）、能力三（Memory）、定时任务（`AgentScheduler`） | 每天到点自动汇总当日科技新闻并推送，且日报内容会体现用户之前说过的关注方向（比如"更关注 AI 和芯片"） | 业务方全程不写 Java 代码，只写 `AGENT.md`（含 `schedules` 字段）并在 Agent `skills/` 绑定公共 Skill，再配置 `mcp_servers.yaml`；prompt 只出现 Skill 元数据，模型按需读取正文并完成日报；示例 AGENT.md 的 schedules 配置 user 与偏好用户一致（钟推与人推读写同一档） |

两个 Demo 都是"钟推"（`AgentScheduler` 到点自动触发），但都要能同时支持"人推"手动补跑一次做验证（`agentos chat` 或 `POST /agents/{name}/invoke`），验证同一个 Agent 不管从哪个入口触发，走的都是同一条 `AgentService` 链路。

`AGENT.md + scripts/`（Agent 目录第三档丰富度）不设独立 Demo，以第四周"scripts/ 最小链路手工演示"验证：目录加载、脚本经专用 Tool/MCP 封装或白名单 shell 调起、产出进上下文、`tool_invocations` 有记录。推荐将脚本封装为专用 Tool 或 MCP server，通用 shell + 白名单解释器仅限可信单机部署。

---

## 14. 总结

AgentOS 是基于 Java 实现的面向企业场景的 Agent OS，装在企业自己的 K8s 或服务器上，作为统一底座跑各种业务 Agent，共享一套渠道接入、模型路由、工具调用、记忆系统、沙箱执行、安全审计能力。

**AgentOS 的交付分两段：**

1. 核心阶段先用 Java 把 AgentOS 的运行时内核做扎实，这一层在能力上对齐业界开源 Agent OS 的基础层
2. AgentOS 真正的差异化治理层（多租户、SSO、完整审计、Tool 治理），在核心内核之上由扩展阶段和社区共建陆续补齐。**核心阶段是地基，企业级治理是终局。**

**核心阶段五大核心能力：**

- **对接 LLM**：Provider 抽象，让 Agent 能调任意主流大模型，运行时切换无 lock-in
- **ReAct 循环**：Agent 大脑，LLM 思考 + 工具执行，多步骤任务自主完成
- **Memory 三层记忆**：核心阶段会话 + 长期记忆（核心阶段 Markdown 默认档，按 `<Agent, 用户>` 二元组分档；`LongTermMemoryStore` 接口预留 `memory.backend` 三档切换，SQLite/Mem0 档随后补齐），同一 `<Agent, 用户>` 内跨对话记住用户偏好和项目背景
- **Plugin 自定义工具 + 内置工具集**：内置文件/Shell/HTTP，业务方通过 Agent 目录 + MCP 零代码扩展、MCP server 轻代码扩展、@Tool 注解重代码扩展
- **Web Service**：REST API 覆盖八类操作（会话管理、Agent 调用、Profile、Memory、Tool 信息、系统状态，第四周收尾补齐通知渠道管理、定时任务管理后两类）

**核心理念**：AgentOS 五大能力扎实落地，业务方组合 Agent 目录 + MCP server 就能解决业务问题，通过 Web Service 接入已有系统，不需要写 Agent 后端代码。AgentOS 不绑定具体业务，业务方按自己的需求组合。
