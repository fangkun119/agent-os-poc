# docs/design/ 目录约定

本目录四份文档（DemandAnalysis / TechnicalSolution / AiProgrammingGuide / IndustryResearch）互有章节号锚点引用，修订时遵守以下项目约定：

- **不得修改任何文档的标题**（章节标题是跨文档引用的锚点，如"见技术方案 7.4"；改标题会连锁失效）——项目约定，docs 未明文
- **冲突仲裁**（完整表述，根 CLAUDE.md 有一行压缩版）：技术实现细节冲突以 TechnicalSolution 为准；需求范围与验收标准冲突以 DemandAnalysis 为准（防止技术方案的实现裁剪覆盖验收标准；注意 spec/tasks 从 docs 派生时会把冲突继承下去，实施期在 specs/ 工作同样适用此仲裁）
