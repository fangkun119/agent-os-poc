# docs/design/ 目录约定

本目录四份文档（DemandAnalysis / TechnicalSolution / AiProgrammingGuide / IndustryResearch）互有章节号锚点引用，修订时遵守以下项目约定：

- **不得修改任何文档的标题**（章节标题是跨文档引用的锚点，如 "TechnicalSolution.md - 7.4 关键设计点"；改标题会连锁失效）——项目约定，docs 未明文
- **冲突仲裁**（完整表述，根 CLAUDE.md 有一行压缩版）：技术实现细节冲突以 TechnicalSolution 为准；需求范围与验收标准冲突以 DemandAnalysis 为准（防止技术方案的实现裁剪覆盖验收标准；注意 spec/tasks 从 docs 派生时会把冲突继承下去，实施期在 specs/ 工作同样适用此仲裁）；API 线上契约（请求/响应体、参数约束、错误码）与 TS 重叠或冲突处，以 docs/design/detail/api.md 为准（TS 保留设计考虑与端点语义概要）

## 标题编号规范（2026-10-02 起）

适用范围：本目录全部文档。规范化基线（2026-10-02 完成）：TechnicalSolution（新增 70 个四级标题）、DemandAnalysis（40 处补序号）、AiProgrammingGuide（5 处补序号）；api.md / IndustryResearch 为扁平结构，暂无编号需求（model-config.md 已有三级编号标题并被跨文档引用）。

| 级别 | 格式 | 说明 |
|------|------|------|
| 一级 | `# 第<M>部分` | M 用汉字（第一 / 第二 / 第三）；文档标题（`# 文档名`）豁免本格式 |
| 二级 | `## <N>. <题名>` | N 用阿拉伯数字，序号后带点号 |
| 三级 | `### <N.P> <题名>` | 章内节号 |
| 四级 | `#### (Q) <题名>` | 每个三级节（或无三级小节的章）内从 (1) 重新编号 |
| 五级 | `##### ① <题名>` | 每个四级内从 ① 重新编号，仅为真实嵌套的平行子结构而设 |

- **调整编号只加前缀、标题原文一字不动**——与上方"不得修改任何文档的标题"同源：现有标题是跨文档引用锚点（如 "TechnicalSolution.md - 7.4 关键设计点" 引用的是章节号）
- **编号在每个父节内独立重启**，不做全文连续编号
- **新标题不得与同文档现有标题重名**
- 四级标题优先由既有"粗体领句段"（`**XXX。**` 开头的段落）升格产生；无真实嵌套平行结构时不设五级（2026-10-02 全文核查：TechnicalSolution / DemandAnalysis / AiProgrammingGuide 三份文档的五级结论均为 0）
