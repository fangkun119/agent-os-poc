# AgentOS API 线上契约

> 位置：`docs/design/detail/api.md`
> 创建：2026-09-26 · 状态：定稿（端点随第三周/第四周交付；契约测试 T1-T3 随实现交付，见附录 A 契约测试）
> 仲裁：本文件与 TechnicalSolution.md（下称 TS）重叠或冲突处，**以本文件为准**；TS 保留设计考虑与端点语义概要（docs/design/CLAUDE.md 冲突仲裁条，2026-09-26 批准）

## 0. 先看结论（5 条）

1. **信封统一**：全部端点返回 `{code, message, data, timestamp}`，成功与错误共用一个信封；本契约只定义 data。
2. **失败 data 三字段恒在**：所有错误响应（400/404/500/503/504）的 data 恒为 `{errorCode, sessionId, lastTermination}` 同一对象，不适用填 null、不省略键。
3. **errorCode 封闭 11 值**：机读分类字段 errorCode 取值是封闭常量集（§4 失败路径契约的 11 值），集外值不允许出现。
4. **本文件与 TS 重叠处以本文件为准**；端点语义概要与设计考虑仍看 TS。
5. 机制为什么长这样（并发、终止、超时、渠道删除）看 TS 对应章节（§5 行为语义指针给出指针）。

## 1. 术语表

| 术语 | 含义 |
|------|------|
| 信封 | 统一响应包裹结构 `{code, message, data, timestamp}`，全部端点共用（TechnicalSolution.md - 7.1 模块组成） |
| 轮 | 一次"user 发消息 → Agent 回复完成"的完整过程，可含多次工具调用 |
| 迭代 | 单次 LLM 调用加其工具结果，是 MAX_ITERATIONS 与审计计数的粒度单元（见 TechnicalSolution.md - 4.3 关键设计点 的 (2) 消息累积与提交纪律 小节）；一轮含一至多次迭代 |
| 轮级原子穿插 | 同会话并发时，两请求的轮按提交先后整轮原子混排落库；进行中单查只见已完成轮（TechnicalSolution.md - 7.2 核心阶段端点的 2026-09-25 段、2026-10-05 轮原子提交裁决） |
| invoke | 无状态调用端点 `POST /api/v1/agents/{name}/invoke`，每次调用新建单轮会话 |
| generate | 扩展阶段端点 `POST /api/v1/agents/generate`：一句话经 LLM 生成 AGENT.md 草稿；每次调用创建单轮会话（channel=`generate`、profile_name=`nan`），草稿不写 Agent 目录、不注册（TechnicalSolution.md - 11.3 扩展阶段：`/api/v1/agents` + 一句话生成 + 实时监听 + 文件浏览器） |
| 钟推 | 定时触发（AgentScheduler 按 cron 自动发起），对应"人推"（CLI/HTTP 人工发起） |
| session_messages | 会话消息行表（一条消息一行），2026-09-25 裁决引入，替代原 sessions.messages_json 单列（TechnicalSolution.md - 9.2 SQLite 关系型数据） |

## 2. 通用规则

| 规则 | 内容 |
|------|------|
| 响应信封 | 全部端点返回 `{code, message, data, timestamp}`；code 为 HTTP 风格字符串（"200"/"404"…）；本契约只定义 data |
| 请求格式 | POST/PUT 的 Content-Type 统一 `application/json` |
| JSON 键名 | 一律小驼峰（sessionId、lastTermination、nextRunAt…）；唯一例外见 §3 端点契约 注 3（messages 元素同构原样） |
| 可空字段策略 | 失败与成功响应的可空字段一律以 null 填充、键不省略（例：#1 新会话 lastTermination=null、#8 builtin 的 sourceName=null、#12/#13 未填的 description=null） |
| 时间字段 | 统一 ISO-8601 字符串 |
| 认证 | 无（核心阶段内网假设，TechnicalSolution.md - 7.5 核心阶段不做的部分） |
| 用户身份 | 请求头 `X-User-Id`：格式 `[a-z0-9-_]{1,32}`、缺省 `default`、非法→400；适用端点 #1/#5/#7；#2 可省（user 以会话为准） |
| 列表参数 | `cnt` 必须 ≥1；缺省 100；上限 1000（超限截断）；非数字/负数/0 → 400 |
| 空列表 | 返回 `[]`，禁 null |
| 列表口径 | 会话列表为实例级、不分用户（无认证假设下的一致选择，N1'） |
| 不收编声明 | 未映射路径的 404、方法不匹配的 405/415 为框架行为、不带信封，不在错误码集内 |

## 3. 端点契约（基础 10 + 收尾 8）

> **errorCode 列总注**：下列两表的 errorCode 列只列**端点特有值**。INVALID_ARGUMENT（参数类兜底：缺字段/空值/body 不可解析/cnt 非法/X-User-Id 非法等）与 INTERNAL_ERROR（未预期异常兜底）对**全部端点**适用、不逐行列出；各端点完整全集 = 本列 + 两个全局值。权威归属表见 §4 失败路径契约。

### 3.1 基础 10 个（第三周交付）

| # | 方法与路径 | 参数/Header | 请求体 | 成功 data | 数组上限 | errorCode（仅端点特有值） |
|---|-----------|------------|--------|-----------|---------|--------------------------|
| 1 | POST /api/v1/sessions | X-User-Id 可选 | `{profile}` 必填 | 7 项会话元数据 + `messages: []` | messages 恒空 | AGENT_NOT_FOUND |
| 2 | POST /api/v1/sessions/{id}/messages | 路径 {id}=sessionId；X-User-Id 可省 | `{content}` 必填 ≤32KB | `{reply, lastTermination}` | — | MESSAGE_TOO_LARGE、SESSION_NOT_FOUND、TOTAL_TIMEOUT、PROVIDER_FAILURE、PERSIST_FAILURE |
| 3 | GET /api/v1/sessions/{id} | 路径 {id} | 无 | 7 项元数据 + messages 全量 | messages 无上限不分页（分页在扩展阶段） | SESSION_NOT_FOUND |
| 4 | GET /api/v1/sessions?cnt=N | 查询 cnt | 无 | `[{sessionId, lastActiveAt, preview}]` | 前 N 条按 lastActiveAt 倒序；preview ≤100 字符 | — |
| 5 | POST /api/v1/agents/{name}/invoke | 路径 {name}=Agent 名；X-User-Id 可选 | `{content}` ≤32KB | `{reply, sessionId, lastTermination}` | — | AGENT_NOT_FOUND、MESSAGE_TOO_LARGE、TOTAL_TIMEOUT、PROVIDER_FAILURE、PERSIST_FAILURE |
| 6 | GET /api/v1/profiles | 无 | 无 | `[{name, description, provider, model}]` | 全量无分页 | — |
| 7 | GET /api/v1/memory?agent=X | 查询 agent 必选；X-User-Id 可选 | 无 | `{agent, user, content}` | content = MEMORY.md 档内容一段字符串（两分区 header 随文保留；核心区全量在前、永不截断；归档区视图级截断 ≤ archival-max-chars，默认 20000 字符；TechnicalSolution.md - 5.1 模块组成） | AGENT_NOT_FOUND |
| 8 | GET /api/v1/tools | 无 | 无 | `[{name, description, source, sourceName}]`；source ∈ {builtin, bean, mcp}；sourceName 仅 mcp 时非空（= MCP server 名） | 全量（内置 9 + 方式三 + MCP 已注册全部） | — |
| 9 | GET /api/v1/health | 无 | 无 | `{status: "UP"}` | — | — |
| 10 | GET /api/v1/info | 无 | 无 | `{version, providers: [{name, defaultModel, models[]}]}` | providers = 已启用清单（静态）；**不含 api-key**（密钥红线） | — |

7 项会话元数据（JSON 键，小驼峰）：sessionId、profileName、channel、userId、createdAt、lastActiveAt、lastTermination。

### 3.2 收尾 8 个（第四周交付）

| # | 方法与路径 | 参数/Header | 请求体 | 成功 data | 数组上限 | errorCode（仅端点特有值） |
|---|-----------|------------|--------|-----------|---------|--------------------------|
| 11 | GET /api/v1/notify-channels | 无 | 无 | `[{name, type, url, description}]` | 全量 | — |
| 12 | POST /api/v1/notify-channels | 无 | `{name, type, url, description?}` | 创建后完整实体 | — | DUPLICATE_CHANNEL_NAME |
| 13 | PUT /api/v1/notify-channels/{name} | 路径 {name} | 同 #12（name 以路径为准不可改） | 更新后完整实体 | — | CHANNEL_NOT_FOUND |
| 14 | DELETE /api/v1/notify-channels/{name} | 路径 {name} | 无 | null | — | CHANNEL_NOT_FOUND |
| 15 | GET /api/v1/schedules | 无 | 无 | scheduled_tasks 全列 12 字段：`[{taskId, profileName, cron, zone, message, user, enabled, nextRunAt, lastRunAt, lastStatus, runCount, updatedAt}]` | 全量 | — |
| 16 | GET /api/v1/schedules/{id}/executions | 路径 {id}=taskId | 无（cnt 适用） | `[{id, sessionId, startedAt, success, errorMessage, durationMs}]`（taskId 在路径，响应不重复） | 最近 cnt 条按 startedAt 倒序（唯一无界增长列表，必须有上限） | SCHEDULE_NOT_FOUND |
| 17 | POST /api/v1/schedules/{id}/run | 路径 {id} | 无 | `{started}`；started=true=受理执行、false=任务正忙被跳过（E2 修订：accepted 已去除——404 之外恒受理） | — | SCHEDULE_NOT_FOUND |
| 18 | PUT /api/v1/schedules/{id} | 路径 {id} | 仅 `{enabled}` 一键（N2'） | 与 #15 单项同形的完整任务状态对象 | — | SCHEDULE_NOT_FOUND |

### 3.3 端点注

1. **（M1）** #5 对启动校验失败的 Agent 返回 404 AGENT_NOT_FOUND，message 点名校验失败原因（启动校验失败的 Agent 不注册，见 TechnicalSolution.md - 8.2 Profile 配置）。
2. **（E1-A（修订），2026-09-29）** #15-#18 的 {id} 即 taskId = `<agent>__<schedule-id>` 组合键，全局唯一由构造保证（替代 2026-09-26 原口径"裸 id 全局唯一、撞名后注册跳过"；表定义见 TechnicalSolution.md - 9.2 SQLite 关系型数据，校验见 TechnicalSolution.md - 8.2 Profile 配置）。
3. **（同构例外）** #3 的 messages 数组元素与存储原文（session_messages.payload_json）**同构原样返回**，键名随存储结构——存储格式由实现期消息序列化决定，本契约不另定存储格式；此为小驼峰规则的唯一例外。
4. **（M2）** #14 删除被 Agent 正文按名引用的渠道时不预警（引用检查归扩展阶段）；删除后 `notify` 调用报"渠道不存在"= 确定性失败、不可重试（TechnicalSolution.md - 4.2 模块组成 口径）。
5. **（选型记录）** 渠道撞名返回 400 + DUPLICATE_CHANNEL_NAME 而非 409——错误码封闭集既定、机读分类由 errorCode 承载，HTTP 层保持 {400, 404, 500, 503, 504} 五值不变；后人勿当缺陷“修”成 409。
6. **（Q6，2026-09-30）** #15/#18 响应中的 `zone` = register 时解析后的实际时区：frontmatter 声明了 `timezone` 即声明值，未声明即缺省值（进程系统时区）；定义见 TechnicalSolution.md - 9.2 SQLite 关系型数据。
7. **（Q5，2026-10-09）** #4 会话列表含 channel=`generate` 的会话、不滤（列表口径——实例级、不分用户、各触发途径不区分——对 generate 同样适用）；#3 单查此类会话时 `profileName` 字段值为 `nan`——不适用标记：执行时该 Agent 的 profile 尚不存在；`nan` 为保留字（Agent 目录名禁用，规则见 TechnicalSolution.md - 8.2 Profile 配置），构造上不与真实 Agent 名碰撞；语义声明、非占位（定义见 TechnicalSolution.md - 9.2 SQLite 关系型数据，2026-10-09 Q5（generate 会话化）裁决）

## 4. 失败路径契约（errorCode 权威归属表）

**统一形状**：所有错误响应（400/404/500/503/504）的 data 恒为同一对象、三字段恒在（不适用填 null，不省略键）：

```text
data = {
  errorCode:       string  必填，封闭常量集（下表 11 值）
  sessionId:       string|null  会话类失败：#2 回显路径值、#5 返回本次 id；其余 null
  lastTermination: string|null  仅 #2/#5 的 504/503/500 非空；可能为旧值（TechnicalSolution.md - 4.3 关键设计点：终止标记写失败时留旧值）
}
```

信封分工：`code` = HTTP 传输语义；`errorCode` = 业务机读分类；`message` = 人读细节（含现场，如"总预算 300s 已到"）。

| errorCode | HTTP | 端点 | 触发条件 |
|-----------|------|------|---------|
| INVALID_ARGUMENT | 400 | 全部 | 兜底参数错：缺字段/空值/body 不可解析、cnt 非法（含 0）、渠道 type 非 webhook、#18 缺 enabled、X-User-Id 非法（message 点名具体项） |
| MESSAGE_TOO_LARGE | 400 | #2 #5 | content 超 32KB，Controller 层校验（与 X-User-Id 同层，容器层拦截会绕过信封与 errorCode，故不用；单列：可程序化截断，处置不同于普通参数错） |
| DUPLICATE_CHANNEL_NAME | 400 | #12 | 渠道名撞名（选型理由见 §3 端点契约 注 5） |
| AGENT_NOT_FOUND | 404 | #1 #5 #7 | Agent（含 #1 的 profile 引用）不存在，或启动校验失败未注册（M1） |
| SESSION_NOT_FOUND | 404 | #2 #3 | sessionId 不存在 |
| CHANNEL_NOT_FOUND | 404 | #13 #14 | 渠道名不存在 |
| SCHEDULE_NOT_FOUND | 404 | #16 #17 #18 | taskId 不存在 |
| TOTAL_TIMEOUT | 504 | #2 #5 | 总超时打断（默认 300s）；lastTermination=interrupted |
| PROVIDER_FAILURE | 503 | #2 #5 | LLM 调用报错；lastTermination=failed |
| PERSIST_FAILURE | 500 | #2 #5 | 消息行入库提交失败（busy 超时/磁盘满，2026-09-25 裁决第五类终止；2026-10-05 轮原子提交裁决：整轮不留、审计照留）；lastTermination=failed（可能旧值） |
| INTERNAL_ERROR | 500 | 全部 | 未预期异常兜底（GlobalExceptionHandler 收口） |

示例响应（两个代表性场景）：

```json
{"code":"504","message":"total budget 300s reached","data":{"errorCode":"TOTAL_TIMEOUT","sessionId":"invoke-default-ops-8f3a…","lastTermination":"interrupted"},"timestamp":"2026-09-26T08:00:00Z"}
{"code":"404","message":"channel not found: team-lark","data":{"errorCode":"CHANNEL_NOT_FOUND","sessionId":null,"lastTermination":null},"timestamp":"…"}
```

## 5. 行为语义指针（只放指针不复述）

- 并发行为（同会话并发发消息）→ TechnicalSolution.md - 7.2 核心阶段端点的 2026-09-25 段（轮级原子穿插、无闸、重试残留）
- 终止语义（四类结束 + 第五类触发）→ TechnicalSolution.md - 4.3 关键设计点
- 超时机制（三档计时与打断）→ TechnicalSolution.md - 7.4 关键设计点
- 渠道删除设计考虑 → TechnicalSolution.md - 6.8 通知推送

---

## 附录 A：契约测试（T1-T3，随实现交付）

| # | 名称 | 端点 | 前置 | 断言 |
|---|------|------|------|------|
| T1 | 成功响应契约（Q2） | #5 invoke 与 #2 messages | 已注册 Agent；stub Provider 返回无工具调用响应 | HTTP 200；data 含非空 reply、lastTermination="normal"；invoke 的 data.sessionId 非空。层级：@SpringBootTest + @AutoConfigureMockMvc + stub ChatModel |
| T2 | 总超时失败形状（E3） | #5 invoke | 测试配置把 total 预算压到秒级，stub Provider 延迟超过预算 | HTTP 504；data 三字段恒在且 errorCode="TOTAL_TIMEOUT"、sessionId 非空、lastTermination="interrupted"；message 含现场文字 |
| T3 | run 忙时跳过（E2） | #17 run | 注册一个 schedules 任务；并发触发两次（或先持任务锁再调） | 忙时 200 且 data.started=false；空闲时 started=true；taskId 不存在时 404 SCHEDULE_NOT_FOUND |

## 附录 B：裁决记录

| 编号 | 日期 | 内容一句话 |
|------|------|-----------|
| 并发裁决 | 2026-09-25 | 同会话并发发消息平台不设闸（轮级原子穿插）；已落 TechnicalSolution.md - 7.2 核心阶段端点 / DemandAnalysis.md - 5.9 Session 管理，本文件只引用不复述 |
| Q2（reply 字段） | 2026-09-25 | 发消息与 invoke 两端点响应体必须含最终回复（reply）——原状只定义了 session_id，与 DA"同步调用"场景矛盾 |
| E1-A | 2026-09-26 | 定时任务 id（task_id）升级为全局唯一（原 DemandAnalysis.md - 5.2 定义一个 Agent"Agent 内唯一"作废；启动校验撞名跳过并记日志） |
| E1-A（修订） | 2026-09-29 | task_id 改为 `<agent>__<schedule-id>` 组合键（`__` 分隔符与 TechnicalSolution.md - 6.4 Plugin Tool 方式二 MCP 工具全名同款）；schedule id 回归 Agent 内唯一。起因：裸 id 全局唯一迫使跨 Agent 撞名时按注册顺序"牺牲"一个合法任务，牺牲者随目录扫描顺序漂移、不可复现；组合键由构造消灭撞名。替代上条口径 |
| Q6（zone 语义） | 2026-09-30 | schedules.timezone 可选、缺省 = 进程系统时区；#15/#18 的 zone 存解析后实际值（定义 TechnicalSolution.md - 8.5 定时任务 / 9.2 SQLite 关系型数据） |
| E2（修订） | 2026-09-26 | run 端点忙时跳过不得谎报；响应为 `{started}` 单字段（404 之外恒受理，accepted 冗余已去除） |
| E3 | 2026-09-26 | 失败路径统一：所有错误响应 data 用同一形状（三字段恒在） |
| errorCode 集 | 2026-09-26 | 机读分类字段 errorCode，取值 = 预定义字符串常量封闭集（11 值，§4 失败路径契约） |
| M1 | 2026-09-26 | 启动校验失败的 Agent 不注册，invoke 返回 404 点名校验原因 |
| M2 | 2026-09-26 | 通知渠道删除无引用检查：核心阶段允许删 + 写明 notify 将确定性失败；引用检查归扩展阶段 |
| M3 | 2026-09-26 | task_executions 永久增长无清理：TechnicalSolution.md - 7.3 扩展阶段补齐的端点 扩展清单补一行 |
| M4 / M5 | 2026-09-26 | cnt 必须 ≥1（0 非法）；会话列表预览定名 preview、≤100 字符 |
| N1' / N2' | 2026-09-26 | 会话列表实例级不分用户，写明；PUT /schedules/{id} 请求体仅 enabled 一键，写明 |
| 驼峰修正 | 2026-09-26 | API JSON 键一律小驼峰；唯一例外=messages 元素同构原样（§3 端点契约 注 3） |
| R1/R2 批准 | 2026-09-26 | 仲裁条款入 docs/design/CLAUDE.md、导航指针入仓库根 CLAUDE.md |
| 附录 A 定稿 | 2026-09-26 | 三条契约测试 T1-T3 |
| 骨架同步 | 2026-09-26 | 3 个骨架文件契约级修正（agentos-web：ApiResponse / SessionApiController / MemoryApiController） |
| Q8①（memory content 形状） | 2026-09-26 | #7 memory 响应 content = MEMORY.md 档内容一段字符串（header 随文、归档区视图级截断 ≤ archival-max-chars）——已批准 #7 结构的显式化、非新裁决；与 MEMORY.md 一一对应，不拆 {core, archival} |
| Q5（generate 会话化） | 2026-10-09 | generate 会话化——每次调用创建单轮会话（channel=generate、profile_name=nan，nan 为 Agent 目录名保留字）；llm_calls.session_id 保持必填，DemandAnalysis.md - 10.5 零改动 |

## 附录 C：变更记录与骨架同步挂账

- 2026-09-26 初版：全量 18 端点契约 + 失败路径契约 + 通用规则 + 契约测试 T1-T3。
- **骨架同步挂账**：第三周实现前，对 agentos-web 全部 Controller 逐条对照本文件全量核对（端点构成、请求/响应 data、错误映射）；本批已修 3 个骨架文件（ApiResponse / SessionApiController / MemoryApiController，2026-09-26），其余以实现期核对为准。
- 2026-09-28 扩面：agentos-cli 七处注释旧口径（命令数 12→13、init 生成物清单、密钥红线表述等）与 agentos-core 的 AgentLoader 启动校验清单注释（4 项→8 项）纳入同批核对；原挂账范围（agentos-web 全部 Controller）不变。
- 2026-09-29：E1-A（修订）——task_id 改组合键 `<agent>__<schedule-id>`，§3.3 端点注 注 2 同步；#15-#18 的 taskId 字段与 {id} 取值口径随注 2，端点构成与响应结构不变。
- 2026-09-30：Q6——#15/#18 zone 语义 = 解析后实际时区（缺省 = 进程系统时区），§3.3 端点注 注 6、附录 B 裁决记录。
