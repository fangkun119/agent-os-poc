# AI 指令元规范（预打包判例）

> 本文件职责：汇总「怎么写 CLAUDE.md」的 AI 指令元规范六条口径，并附爬取原件索引与打包说明。skill 打包为 `references/meta-norms.md` 后，由运行 agent 直接 Read，不触发 firecrawl。
> 上游：skill-design.md——本文件的规则落点与门控边界取自其 §6 红线与 §3.2 门控（skill-design.md 文件更名记录见其附录 A）。
> 下游：skill 运行时在步骤②（判例落盘的恒定路）与步骤⑦⑧（行数红线、回潜 grep、条款自足性评审）消费本文件。

## 0. 门控地位（本文件为何不走运行时爬取）

- 恒定路定位：AI 指令元规范路是第②步判例落盘的恒定路。它与「代码怎么写」的判例正交；用户的现成编程规范文档（SRC-6）替代不了它，除非用户显式提供 AI 指令类文档（SRC-5）。
- 门控边界：
  - 本文件在创建 skill 时一次性爬取、预打包进 skill。
  - 运行时 Read 本地文件：不触发 firecrawl，不受门控。
  - 门控仅作用于支路一②的全量调研爬取。SRC-6 本地判例拷入、元规范路读本地，均不在门控之内。
- 理由：元规范更新频率极低（季度级）。预打包比运行时反复爬取更稳——零网络依赖、零失败面、版本随 skill 升级同步更新。
- 判例引用纪律照常适用：抽取必通读本文件与随包原件全文；评审必回查原文。禁止凭记忆引用。
- 本文件不受判例来源二分分叉（含 SRC-6 与否）影响，恒定保留。monorepo 按子栈拆路时，共享本元规范路。

## 1. 元规范核心口径（六条）

每条含四项：规则（祈使句）、可检验判据、设计文档引用处、原件出处。

### 元-1 单文件长度红线：正文 ≤200 行

- 规则：单份指令文件（CLAUDE.md、SKILL.md）正文不超过 200 行。
- 可检验判据：200 行口径 = 注入后对 agent 可见行数 = 全文行数减去块级 HTML 注释行。落盘时记录三个数：200、估算、实况。超行按牺牲顺序裁撤。
- 机制依据：超长指令文件消耗更多上下文并降低遵从率（官方原文口径）。故 200 行是遵从率阈值，不是任意配额。
- 设计文档引用处：§1（草稿正文 ≤200 行）；§6 红线 1；§4 步骤⑦（≤200 行红线与三数记录）。§7 另定 SKILL.md ≤200 行与切分原则：数千行素材直接进 SKILL.md，会触发「超长降低遵从率」问题。
- 原件出处：`claude-code-memory.md`（"target under 200 lines per CLAUDE.md file"、"over 200 lines consume more context and may reduce adherence"）。

### 元-2 无矛盾条款：矛盾规则会被随机执行其一

- 规则：同一文件内不得出现互相矛盾的指令。挂起决策未定期间，同主题不得写出矛盾条款。
- 可检验判据：同主题下不存在结论相反的两条条款。定期清查过期或冲突的条款，范围含嵌套子目录文件与规则文件之间。
- 机制依据：两条规则互相矛盾时，agent 可能随机执行其中一条，行为不可预测。这也是「能合并就合并」的官方依据。
- 设计文档引用处：§6 红线 6（未决期间同主题不得写出矛盾条款）；§6 红线 8（无矛盾表述）。
- 原件出处：`claude-code-memory.md`（"if two rules contradict each other, Claude may pick one arbitrarily"）。

### 元-3 增条门槛：同类错误重复出现才增条

- 规则：自描述演进纪律写进提示区。可对照 diff 检验才增条；同类错误重复出现才增条，单次偶发不增条；优先并入既有条目，而非新开条目；同时维持 ≤200 行与无矛盾表述。
- 可检验判据：每条新增条款能给出可对照 diff 的违反判据。新增前先检索既有条目，可并入的不新开。
- 设计文档引用处：§6 红线 8（自描述演进纪律五要素全文）；§1（自描述演进纪律属本 skill 质量机制一轴）。
- 原件出处：`claude-code-memory.md`（编写有效指令的保持性要求：定期审查、合并精简）；同款表述实证见最终模板 `20260829-claudeme-draft.md` 维护者注。

### 元-4 块级 HTML 注释：注入前被剥离——加载器实现行为，非格式通性

- 规则：人类专用元信息只写入块级 HTML 注释（`<!-- ... -->`），正文不承载。人类专用元信息包括：勿再加清单、参数清单、复用方法、元规范出处、状态块、遗留规范对照等。
- 机制边界：块级 HTML 注释在内容注入 agent 上下文前被剥离。这是 Claude Code 加载器的实现行为，不是 Markdown 或指令文件格式的通性。跨工具消费需另议，不得假设其他工具同样剥离。
- 可检验判据：注释整块置于 `<!-- -->` 内，且不位于代码块内——代码块内注释会被保留。行数预算按「全文减 HTML 注释行」计。评审回潜 grep 的唯一白名单是 HTML 注释区；正文区命中即删除，或显式改判。
- 设计文档引用处：§6 红线 9（含「注明这是 Claude Code 加载器实现行为而非格式通性」）；§4 步骤⑦（五段预算结构末段为「块级 HTML 豁免注记」；≤200 行口径 = 注入后对 agent 可见行数）；§4 步骤⑧（回潜 grep，HTML 注释区唯一白名单）；MF-4（skill-design.md 附录 B）。
- 原件出处：`claude-code-memory.md`（"Block-level HTML comments (`<!-- maintainer notes -->`) ... are stripped before the content is injected"、"Comments inside code blocks are preserved"；用 Read 工具直读文件时注释仍可见）。

### 元-5 按需加载 → 自足性：子目录指令文件被读到时才进上下文

- 规则：子目录级指令文件（子目录 CLAUDE.md、SKILL.md 的 references/ 文件）按需加载——agent 读到对应目录或文件时才注入。因此，每个被按需加载的文件必须自足。
- 可检验判据：文件开头自述适用范围与职责。运行所需的清单、规则、判据原文内联，只读该文件即可完成其声明的职责。引用外部材料时必须在本文复述规则本身，禁止「见某处」却不复述的悬空引用。
- 设计文档引用处：§2（SKILL.md 自足性硬要求：运行 skill 的 agent 无本设计文档上下文，只读 SKILL.md 就必须能独立完成来源识别与分叉判定）；§7 切分原则（references 细则按需 Read + 自足性硬要求）；§4 步骤⑧（评审项「条款自足性」）。
- 原件出处：`claude-code-memory.md`（"Files in subdirectories load on demand when Claude reads files in those directories"）。

### 元-6 可推导内容不写：能从代码推导的不进指令文件

- 规则：能从代码推导的内容不写，如目录布局、依赖清单、架构综述类可枚举事实。只写三类：坑、理由、与工具默认值不同的约定。
- 可检验判据：条款不得是 agent 读仓库即可自行得出的事实陈述。无客观违反判据、可从代码推导的候选直接出局并记否决清单。全文无不可判表述。
- 设计文档引用处：§6 红线 7（「全文无不可判表述；能从代码推导的不写」）；§4 步骤③（准入一票筛：可从代码推导的直接出局记否决清单）。
- 原件出处：`claude-code-memory.md`（/doctor 裁剪原则："cuts content Claude can derive from the codebase, such as directory layouts, dependency lists, and architecture overviews, and keeps pitfalls, rationale, and conventions that differ from tool defaults"）。

## 2. 爬取原件索引

- 来源：四份原件均为创建本 skill 时经 firecrawl 爬取落盘。每份头部带标注头两行：`> Source: URL` 与 `> Downloaded: 日期`。
- 存放：核心两份（`claude-code-memory.md`、`agentsmd-spec.md`）已随包搬入本文件所在 `references/`、与本文件同目录；OSS 实例两份不随包，仅存于设计底稿目录。见第 3 节。

| 原件（路径） | 对本 skill 的贡献 |
| ------ | ----------- |
| `claude-code-memory.md`（同目录，随包） | Claude Code 官方 memory 文档——第 1 节六条口径的官方机制依据（200 行遵从率阈值、矛盾条款任选其一执行、子目录按需加载、块级 HTML 注释注入前剥离、可推导内容裁剪、编写有效指令的结构/具体性/一致性要求）。 |
| `agentsmd-spec.md`（同目录，随包） | AGENTS.md 开放规范——「面向 agent 的 README」定位与推荐章节结构（Setup commands / Code style / Testing instructions / PR instructions 等），为草稿章节组织提供跨工具通用参照。 |
| `astro-agents-md.md`（不随包，仅存设计底稿） | Astro 仓库 AGENTS.md 爬取原件（知名 OSS 实例）——组织法参考：按主题分节陈述编码约束的写法（Think Before Coding / Simplicity First / Surgical Changes 等节）。 |
| `vite-copilot-instructions.md`（不随包，仅存设计底稿） | Vite 仓库 `.github/copilot-instructions.md` 爬取原件（知名 OSS 实例）——组织法参考：面向编码 agent 的约束与协作流程分节写法（Code Standards / Repository Structure / PR Guidelines 等节）。 |

阅读顺序：先读本文件第 1 节口径。引用机制依据时，回查 `claude-code-memory.md` 对应段落。起草章节结构时，参考两份 OSS 实例的组织法，不照抄其站点特定取值。

## 3. 打包说明

- 打包形态拍板：独立文件随包。核心原件两份（`claude-code-memory.md`、`agentsmd-spec.md`）以独立文件与 `meta-norms.md` 同置于 `references/` 随包携带，不并入本文件。目的：使运行 agent 的引用可回查原文全文，不依赖本文件第 1 节的转述。
- 原件各自头部保留标注头两行（`> Source:` / `> Downloaded:`），随包不删。
- OSS 实例两份（`astro-agents-md.md`、`vite-copilot-instructions.md`）不随包，仅保留第 2 节的索引说明。
- 路径口径：`../../code-style/` 是底稿阶段相对路径；打包后随包原件与本文件同目录（`references/`），第 2 节索引按打包后同目录路径引用，保证随包可解析。
- 保持「运行时零网络依赖」：运行期间禁止为元规范发起任何网络请求。本文件与随包原件的读取是本地 Read，不属于 §3.2 门控作用的爬取动作。
- 版本维护：随包原件随 skill 升级同步更新（更新标注头 `> Source:` / `> Downloaded:`），不设运行时刷新通道。
