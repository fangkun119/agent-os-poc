# AgentOS 官网主页实施计划

> 状态标记：`[ ]` 未开始 · `[x]` 已完成 · `[!]` 受阻/待定
> 本文是 website 子项目的实施计划与事实基线，内容事实一律以 docs/ 权威文档为最终依据
> （需求范围/验收标准以 DemandAnalysis 第 13 章为准，实现细节以 TechnicalSolution 为准，见仓库 CLAUDE.md 仲裁约定）。

---

## 1. 目标与范围

**目标**：为 AgentOS 交付一个单页官网主页（中英双语），静态构建、零运行时依赖（仅 VitePress），
页面上的每一个数字与能力表述都有 DA/TS 出处，不提前承诺扩展阶段能力。

**范围内**：

- 主页八个板块（Hero / Stats / 运行原理 / 核心能力 / 使用场景 / 路线图 / CTA / Footer），中英双语
- 站点骨架：VitePress 配置、自定义主题（主页组件 + 全局换皮样式）
- 品牌资产：文字版 logo.svg、favicon.svg、AgentOS 架构图 architecture.svg（自绘 SVG）
- 本地 dev 预览验证（桌面/移动/双语截图）+ 生产构建通过

**范围外（本期不做）**：

- 文档页（what / why / quick-start / api / cli 等）——导航与侧边栏不放置死链
- 部署流水线 / 域名 / base path 调整（本期 base 用 `/`）
- 暗色模式切换（全站恒黑，隐藏切换按钮）、全文搜索、站点统计
- 管理台等扩展阶段产品功能页面

**已锁定的决策（2026-08-30）**：

| 决策项 | 结论 |
| --- | --- |
| 范围 | 只做主页 |
| 配色 | 黑底 `#000` + 橙色强调 `#f97316` |
| 双语 | 中英双语，单组件 `t(zh, en)` 模式，URL 前缀区分（root=EN，`/zh/`=中文） |
| 站名 | AgentOS，定位语「基于 Java 的企业级 Agent OS 运行时内核」（DA 1.1） |

---

## 2. 内容事实基线（主页可用数字与表述，全部带出处）

> 红线：主页不得出现下表之外的能力声明或数字。性能数字必须标注「验收目标」。

### 2.1 定位与口号

| 事实 | 出处 |
| --- | --- |
| 定位语：基于 Java 的企业场景 Agent OS，装在企业自己的 K8s/服务器上，统一底座跑业务 Agent，共享渠道接入/模型路由/工具调用/记忆/沙箱/安全审计，数据完全留在企业基础设施，不锁云生态 | DA 1.1 |
| 金句：runtime 让一个 Agent 跑起来，Agent OS 让一群 Agent 在企业里被管起来 | DA 1.1 |
| 设计目标四词：统一、私有、易接入、可观测 | DA 3 |
| Java 生态空白：业界已验证 Agent OS 定位，Java 生态尚无同定位项目 | DA 1.1 / DA 12 |
| 「一个目录就是一个完整可用的业务 Agent，不是写代码写出来的」 | DA 2 / DA 5.2 |

### 2.2 硬数字（Stats 条与能力卡可用）

| 数字 | 内容 | 出处 |
| --- | --- | --- |
| 9 | Maven 模块（core/provider/memory/tool/channel-cli/web/storage/cli/boot） | TS 10 |
| 9 | 内置 Tool，5 组：FileTools×3（read_file/write_file/list_dir）、ShellTools×1（shell）、HttpTools×2（http_get/http_post）、MemoryTools×2（save_memory/recall_memory）、NotifyTools×1（notify） | TS 6.2 / DA 5.6 |
| 18 | REST 端点 = 基础 10（sessions×4、agents/{name}/invoke×1、profiles/memory/tools 查询×3、health/info×2）+ 收尾 8（notify-channels CRUD×4、schedules×4） | TS 7.2 / DA 5.8 |
| 12 | CLI 子命令：init/status/chat/serve/gateway/profile list\|create\|show\|delete/provider list/tool list/session list | TS 8.7 / DA 5.11 |
| 3 | 触发源（CLI / Web API / AgentScheduler 定时）汇入同一 AgentService.process | DA 9 / TS 2 |
| 2 | 审计表（tool_invocations / llm_calls）核心阶段 day one 落库 | TS 1.1 决策七 |
| 3 档 | Plugin Tool 接入：零代码（AGENT.md 目录+现成 MCP，主推）→ 轻代码（任意语言 MCP server）→ 重代码（@Tool Bean）；原则「能用一不用二，能用二不用三」 | TS 6 / DA 5.6 |
| 3 档 | 记忆后端：Markdown 默认 → SQLite → 自托管 Mem0，`memory.backend` 一行切换 | TS 5.1 / 5.5 |
| ≥100 / P99<200ms | 单节点并发 Session / Session 创建 P99 —— **验收目标，非承诺值**（TS 中「千级并发」为 10 倍余量论证） | DA 8.1 / DA 13 / TS 14 |
| 30 分钟 | 新手单节点部署（验收项） | DA 13 |
| 60s/30s/300s | LLM / Tool / 总 超时三档分步预算，配置化不硬编码 | TS 7.4 |

### 2.3 关键机制（能力卡文案素材）

- **一个目录 = 一个 Agent**：`.agentos/agents/<name>/`，AGENT.md = frontmatter 配置 + 正文指令，
  可选 skills/（软连接绑定）、scripts/；`agentos profile create` 生成第一个 Agent（DA 2 / TS 8.1 / TS 11.1）
- **ReAct**：自实现（数十行 Java、最大迭代默认 10），同步阻塞 + 虚拟线程，全程禁 @Async
  （ProfileContext ThreadLocal）（DA 5.4 / TS 1.1 决策一三 / TS 4.2）
- **Memory**：MemoryService 统一门面（会话=SessionManager/SQLite，长期=LongTermMemoryStore/MEMORY.md，
  情景=扩展阶段）；MEMORY.md 双分区「核心记忆（永不截断）/ 归档记忆（按需截断）」（TS 5 / 5.1）
- **Provider**：name → ChatModel 显式映射、禁止类型扫描；DeepSeek/通义/Kimi/智谱/Anthropic/OpenAI 等
  主流 connector；协议转换覆盖 OpenAI/Anthropic/Gemini 三家 tools 格式。注意：核心阶段 Provider 走
  application.yaml 配置，**没有**运行时 CRUD/fallback（TS 3 / DA 5.3）
- **Sandbox**：四类白名单（文件路径 / Shell 命令 / HTTP 域名 / 通知独立域名），应用层「劝阻级」防线，
  违规拦截并写审计；容器/microVM 隔离是扩展阶段升级路径（TS 6.7）
- **MCP**：原生 MCP Client，`mcp_servers.yaml` 声明，stdio/SSE 双传输，启动即发现工具注册进
  ToolRegistry（TS 6.4）
- **Notify**：notify 一个 Tool 推企微/飞书/钉钉群机器人；渠道是 SQLite 全局注册表 notify_channels，
  webhook 地址不进对话，「消息怎么进来有 Channel，结果怎么出去有 Notify」（TS 6.8）
- **调度**：AGENT.md frontmatter 写 schedules（id/cron/timezone/message）到点自动运行；状态与执行历史
  落 SQLite 重启不丢；同任务重叠触发自动跳过（TS 8.5）
- **Web Service**：默认 8080，springdoc OpenAPI 3.0 暴露在 /swagger-ui，任何能发 HTTP 的语言可接入
  （TS 7.1 / 7.6）
- **两个验收 Demo**：每日 8 点天气+穿搭建议推企业 IM 群；每日科技日报。钟推自动运行、覆盖五大能力，
  是发布硬条件（DA 13）
- **零代码示例**：每天早上推送昨日 GitHub PR 评审进度到 Slack——AGENT.md + 现成 github-mcp/slack-mcp，
  全程不写一行代码（DA 5.6）

### 2.4 诚实边界（必须以路线图/脚注表述，不得隐瞒不得提前承诺）

- 核心阶段 Web Service：无认证（假设内网）、无 SSE/WebSocket/RBAC/限流（TS 7.5）
- 管理台页面放扩展阶段；核心阶段通知渠道/定时任务经 API/Swagger 操作（TS 6.8 / 8.5）
- 无 Provider fallback、无 Tool 并行、无情景记忆、无进程内向量层（TS 3.3 / 5.5 / 14）
- 扩展阶段边界：多租户/SSO/完整审计查询/Tool Policy 等治理层（DA 1.1 / DA 6 五层明细）
- 社区共建/长期：移动端管理台等（DA 7）

### 2.5 内容源文件说明

docs/ 权威文档（DA/TS/AG/IR）为官网内容唯一事实来源；其引用的配图已解耦至 `docs/imgs/`
（docs/TechnicalSolution.md 以相对路径 `imgs/*.svg` 引用）。`website/public/images/` 仅存站内资产。
`website/CLAUDE.md`（站点开发规范）与本文经 `srcExclude` 排除、不作为站点页面发布。

---

## 3. 设计规范

### 3.1 品牌与色彩 token（已定稿）

> 本色板经用户确认定稿（2026-08-30）。落地时全部定义为 `custom.css` `:root` CSS 变量，
> 组件样式一律 `var()` 消费、不在 scoped 内新增色值副本（website/CLAUDE.md「品牌色单一来源」）；
> 语义色（终端红黄绿灯、终端绿）不随色板联动。

| Token | 值 | 用途 |
| --- | --- | --- |
| 背景 | `#000000` | 全站底色 |
| 区块底 | `#0d0d0d` / `#111111` | stats 条 / 卡片 |
| 悬停底 | `#141414` / `#161616` | 卡片 hover |
| 代码块底 | `#0a0a0a` | 终端窗 / 代码卡 / CTA 终端 |
| 边框/缝隙 | `#1e1e1e`（分隔）、`#222222`（弱边）、`#333333`（强边）、`#444444`（仅示意图箭杆，SVG 经 img 引用吃不到 CSS 变量） | — |
| 强调色 | `#f97316`（hover `#fb923c`、深 `#ea6a00`、暗 `#c2550a`，透明度变体 rgba .12/.08） | 主色，全部 CTA/数字/高亮 |
| 文字 | `#fafafa`（主）/ `#888888`（次）/ `#7a7a7a`/`#8a8a8a`（弱，对黑底 ≥4.5:1 可读性校准） | — |
| 橙底反差段深色族 | rgba(0,0,0,.65/.5/.25/.15) | CTA 区文字/脚注/描边 |
| 代码字色 | `#d4d4d4`，注释 `#7a7a7a` | — |
| 终端绿 | `#22c55e` | 终端用户标识 |
| 终端窗点 | `#ff5f57` `#febc2e` `#28c840` | macOS 三点（语义色，不随色板联动） |
| --vp 映射 | `#1a1a1a`（--vp-c-bg-mute） | 文档区将来沿用 |

> 弱文字两档原为 `#555555`/`#666666`，2026-08-30 按可访问性校准提亮（小字对黑底需 ≥4.5:1），
> 装饰性元素（分隔符、编号）不受影响。

### 3.2 字体与版式

- 不引入外部字体（内网/离线可渲染、无第三方请求）：
  正文 `-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`；
  代码/标签 `'JetBrains Mono', 'Fira Code', ui-monospace, monospace`
- 版心：hero 800px、stats 900px、section 1040px；板块纵向节奏 88px（移动 64px）
- 断点：900px（网格降列）/ 768px（单列、stats 折行）/ 480px（CTA 纵排）
- 卡片网格统一「1px 缝隙」技法：`gap: 1px` + 缝隙色当网格底 + 卡片底色
- 代码块：`#0a0a0a` 底 + `#1e1e1e` 边 + 6px 圆角
- 视觉全大写（板块标签、Stats 数字标签等）一律 `text-transform: uppercase` 实现，
  源码字符串不硬编码大写

### 3.3 页面板块与内容要点

1. **Hero**：eyebrow（`// 私有部署 · 数据不出域 · 不锁云生态`）→ 标签「企业级 Agent OS」→
   主标题金句（白/橙两行）→ 副文案（DA 1.1 压缩）→ 双 CTA（快速开始=页内锚点 `#get-started`、
   GitHub=`github.com/fangkun119/agent-os-poc`）→ 仿终端演示
   （`agentos init` → `profile create` → `chat --profile` 记忆对话演示，
   命令以 TS 8.1/8.7 为准；`chat` 的 `--profile` flag 出处为 DA 5.11）
2. **Stats 条**：9 模块 / 9 内置 Tool / 18 REST 端点 / 12 CLI 命令 / 3 触发源；
   脚注小字：性能为验收目标（≥100 并发 Session · P99<200ms · 30 分钟部署，DA 8.1/13）
3. **运行原理**：标签 HOW IT WORKS，h2「人推钟推，同一条链路。」，配 `architecture.svg`
4. **核心能力 ×6**：五大核心能力（DA 1.2）+「一个目录 = 一个 Agent」形态卡；每卡编号+标题+描述+代码片段
5. **使用场景 ×8**：运维/客服/HR/知识管理/销售助手（DA 1.1 名单）+ PR 日报（零代码）+
   每日天气穿搭推送 + 每日科技日报（DA 13 Demo）
6. **路线图 ×3**：核心阶段·运行时内核［当前·地基］/ 扩展阶段·企业治理层［规划］/
   社区共建与长期方向［愿景］；内容按 §2.4 边界表述
7. **CTA（`#get-started`）**：橙底大区块「从一个 Agent 到一群 Agent 的底座。」+
   Quick Start 终端（init → export API key → profile create → chat / serve）+
   诚实脚注（核心阶段无认证假设内网等，以「路线图」口吻表述）
8. **Footer**：Agent**OS** 文字标 + tagline「基于 Java 的企业级 Agent OS 运行时内核 · 私有部署」+ GitHub 链接

### 3.4 架构图（architecture SVG）内容规格

透明底，配色随 §3.1；四层横向流（1040×560 viewBox）；**双语两份**（`architecture-en.svg` /
`architecture-zh.svg`），几何一致、文本互换，两图须同改：

```text
触发源列：CLI (agentos chat) · Web API (/api/v1/**) · AgentScheduler (cron)
   → 引擎列（高亮）：AgentService.process：PromptBuilder（五部分注入）
     → ProviderService（name → ChatModel 显式映射）→ ReActLoop（Reason→Act→Observe，≤10 轮）
     → ToolExecutor（超时 30s · 审计落库）
   → 能力列：ToolRegistry（9 内置 + MCP）· MemoryService（三层门面）· Sandbox（四类白名单）
     · Notify（Webhook 注册表）
   → 存储列：SQLite：sessions · scheduled_tasks · task_executions · tool_invocations ·
     llm_calls · notify_channels
```

---

## 4. 工程规范（规范条款映射 + 本站实例化参数）

### 4.1 仓库级条款适用映射（根 CLAUDE.md / constitution）

| 条款 | 在本计划中的落点 |
| --- | --- |
| 文档仲裁：实现以 TS 为准、需求/验收以 DA 13 为准 | §2 事实基线全部带出处；页面数字仅取自 §2 |
| 阅读注记：并发数字非承诺值 | Stats 脚注强制标注「验收目标」 |
| 红线：敏感凭证 `${ENV_VAR}` 注入、不明文 | 终端示例只用 `export DEEPSEEK_API_KEY=...` 占位 |
| 红线：跑通优先于完美 | Phase 4 验证不过不收尾；先桌面后移动逐项修 |
| 红线：超时/映射等机制表述 | 能力卡机制文案以 §2.3 为准，不得简写失真 |

### 4.2 规范遵循（website/CLAUDE.md 为权威，此处只写本站实例化参数）

按 website/CLAUDE.md 全量执行（make 单一入口、constants.ts 常量单一来源、色板变量化消费、
模板字符串 `\${VAR}` 转义、scoped 首 token 类、禁 nth-of-type、text-transform 大写、
`t(zh, en)`（zh 在前）、数据驱动段落三件套、零运行时外部请求等），本节只记录本站实例化取值：

- 构建入口：`Makefile`（install / dev / build / preview 四目标，npm scripts 同名对应）
- 常量单一来源：`.vitepress/constants.ts`（`SITE_NAME` / `REPO_URL` / `TAGLINE_EN` / `TAGLINE_ZH`，零依赖），
  config 与 `Home.vue` 一律 import 使用；md frontmatter 属人工同步副本
- 色板：§3.1 定稿色板 → `custom.css` `:root` 变量（`--aos-*` 前缀），组件 `var()` 消费
- 语言目录：en 为 root、zh 为 `/zh/` 子目录；双语范围仅页面文案 / nav 标签 / 访客 meta
- 组件：纯 JS `<script setup>`（无 `lang="ts"`）；组件文件名与注册名 `Home` 是硬依赖，禁重命名
- 排除项：`srcExclude: ['spec/**', 'CLAUDE.md']`——`website/spec/`（按「日期-序号-主题」分目录的
  实施计划）与站点规范文件不作为站点页面发布（漏排会被发布成公开页面）
- 资产引用：组件内图片一律 `withBase('/images/…')`；config head 与 markdown 用根绝对路径
- 主题覆盖只写 `custom.css`（最窄选择器 + 注明被压内部规则）；`siteTitle: false` 有意设置；
  `appearance: false` 使切换按钮原生不渲染（vitepress 1.6.4 **无** `'force-light'` 取值，勿改回）
- 不启用 `ignoreDeadLinks`（导航不放置死链，无需豁免）
- 占位符记法：`${VAR}`（组件模板字符串内写 `\${VAR}`）、用户填入值 `your-xxx-here`
- og:title / og:description 按 locale 分写（站点级只留 og:type/og:site_name/twitter:card；
  mergeHead 按 tag+attrs 去重，同名 og 标签不得站点级与 locale 级并存）
- 首页 frontmatter `markdownStyles: false`：关掉 vp-doc 容器包装与 markdown 内容样式，
  板块才能全宽铺满（vp-doc 的 1280px 容器/padding/h2 边线/a hover 色会级联进自绘组件）

---

## 5. 目标工程结构

```text
website/
├── CLAUDE.md                      # 已存在（站点开发规范，权威），不随主页日常维护
├── Makefile                       # 构建入口：install/dev/build/preview（CLAUDE.md「make 单一入口」）
├── .gitignore                     # node_modules、.vitepress/cache、.vitepress/dist、.DS_Store
├── package.json                   # vitepress 单依赖 + dev/build/preview 脚本
├── spec/
│   └── 20260830-001-create-website/
│       └── plan.md                # 本计划（按「日期-序号-主题」分目录，srcExclude 排除，不作为站点页面发布）
├── index.md                       # EN 主页壳（layout: home + <Home />）
├── zh/
│   └── index.md                   # 中文主页壳
├── public/
│   ├── favicon.svg
│   ├── logo.svg
│   └── images/
│       ├── architecture-en.svg    # 架构图双语两份，几何一致文本互换，两图须同改
│       └── architecture-zh.svg
└── .vitepress/
    ├── config.mts                 # locales(root=EN, zh)、nav、srcExclude、SEO head
    ├── constants.ts               # 站点常量单一来源（站名/仓库地址/tagline），零依赖
    └── theme/
        ├── index.ts               # theme-without-fonts 入口 + 注册 Home 组件
        ├── custom.css             # :root 色板变量 + 全局黑底 + 最窄覆盖（带注释）
        └── components/
            ├── Home.vue           # 主页全部内容（八板块，纯 JS script setup）
            └── Layout.vue         # 默认布局薄封装
```

---

## 6. 执行清单（Phases & Steps）

### Phase 1 站点骨架

- [x] 1.1 `package.json`（vitepress ^1.6.x，dev/build/preview 脚本）+ `Makefile`
      （install/dev/build/preview 四目标 + `##` 用途注释 + `.PHONY`）+ `.gitignore`，`make install`
- [x] 1.2 `.vitepress/constants.ts`（站名/仓库地址/tagline，零依赖）+ `config.mts`：
      title/description（定位语）、locales（root=EN + zh，中文 locale 覆盖默认主题文案）、
      nav（Home/GitHub，无死链）、`srcExclude`、SEO head、`base: '/'`
- [x] 1.3 theme 三件套：`index.ts`（theme-without-fonts 入口+注册 Home）、`Layout.vue`（薄封装）、
      `custom.css`（§3.1 色板 `:root` 变量、全站黑底）
- [x] 1.4 `index.md` / `zh/index.md` 主页壳 + `make dev` 可启动验证

### Phase 2 品牌资产

- [x] 2.1 `public/logo.svg` + `public/favicon.svg`（文字标方案：Agent 常规 + OS 加粗，橙色点缀）
- [x] 2.2 `public/images/architecture-en|zh.svg` 按 §3.4 规格自绘（黑底风格、文本手工布点）

### Phase 3 主页内容（Home.vue）

- [x] 3.1 数据层：t() 双语助手 + capabilities×6 / scenarios×8 / roadmap×3 / stats×5 数据数组（§2/§3.3）
- [x] 3.2 Hero 板块（含终端演示，命令按 TS 8.1/8.7、DA 5.11）
- [x] 3.3 Stats 条 + 验收目标脚注
- [x] 3.4 运行原理板块（嵌双语 architecture SVG）
- [x] 3.5 核心能力六卡（含代码片段）
- [x] 3.6 使用场景八卡
- [x] 3.7 路线图三卡（按 §2.4 诚实边界）
- [x] 3.8 CTA + Footer（含 Quick Start 终端与诚实脚注）
- [x] 3.9 响应式规则（900/768/480 三断点）

### Phase 4 验证

- [x] 4.1 `make dev` + Playwright 截图：EN 桌面、EN 移动、ZH 桌面、ZH 移动（逐板块走查）
- [x] 4.2 事实复核：页面出现的每个数字/断言对照 §2 基线（三视角对抗校验 26 条 findings 全部处置）
- [x] 4.3 `make build` 生产构建通过 + `make preview` 目检（website/CLAUDE.md「验证定义」）

### Phase 5 收尾

- [x] 5.1 回填本清单勾选状态与变更记录
- [x] 5.2 向用户汇报：截图 + 文件清单 + 遗留开放问题（开放项移交 chat/temp/website-todo.md）

---

## 7. 开放问题（不阻塞实施，移交 chat/temp/website-todo.md 统一处理）

| 问题 | 当前处理 |
| --- | --- |
| 开源许可证（文档未载明） | eyebrow 不写许可证字样，License 确定后补 |
| 生产部署域名 / base path | 本期 `/`，部署时再调 config |
| 图形版 logo | 先用文字标 SVG，后续可替换 |
| 文档页何时补 | 主页上线后另行立项（导航暂只留 Home/GitHub） |

---

## 8. 变更记录

- 2026-08-30：创建计划；内容事实基线经 DA/TS 双文档提取并交叉核对（18 端点、12 命令、9 工具等
  数字均已对表核验）。
- 2026-08-30：对照新版 website/CLAUDE.md（站点开发规范）更正：构建入口改 make 单一入口、
  常量单一来源 constants.ts、色板 `:root` 变量化落点、模板字符串 `\${VAR}` 转义与占位符记法、
  scoped 纪律以 CLAUDE.md 为权威，§4.2 只保留本站实例化参数；工程结构补 Makefile/.gitignore/constants.ts。
- 2026-08-30：三视角对抗校验（事实基线 / 规范合规 / VitePress 正确性）后修复：
  `appearance: 'force-light'`（1.6.4 无此取值，退化为跟随系统深色）改 `appearance: false`；
  `srcExclude` 补 CLAUDE.md（内部规范曾被发布为公开页面）；og meta 按 locale 分写；
  首页 `markdownStyles: false` 解除 vp-doc 容器级联；弱文字色按可读性校准提亮；
  路线图阶段归属修正（容器隔离/情景记忆归扩展阶段，第三阶段只留 DA 7 社区共建清单）；
  能力卡删「不重启」过承诺、标题「无限 Agent」改「多 Agent 并存」；
  架构图 PromptBuilder 副标补齐五部分口径；theme 入口换 theme-without-fonts 去除死字体资产；
  `chat --profile` 经 DA 5.11 核实为有出处（DA 484/638），保留。
- 2026-08-30：docs 与 websites 图片解耦后（配图移 docs/imgs/），本文曾被移入 docs/docs/ 并随该
  误创建目录一并丢失；现按会话内完整编辑历史在 website/docs/plan.md 重建为最终状态，
  §2.5 / §5 / §6 中已失效的「website/docs 快照」描述同步更新为解耦后现状。
- 2026-08-31：视觉刷新立项并实施完成（v3「机房 LED」色板——初选琥珀荧光同日改选——+
  工作台结构 + 自托管字体 + 唯一编排时刻），独立文档见 chat/consolidate/20260830-website-visual-refresh-{spec,plan,
  claudemd-change}.md；本计划的事实基线（§2）继续作为内容红线有效。
