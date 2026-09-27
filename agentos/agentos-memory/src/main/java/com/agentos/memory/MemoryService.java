package com.agentos.memory;

/**
 * Memory 三层记忆的统一门面（TechnicalSolution.md - 5.1 模块组成）。
 *
 * <p>对 ReAct 循环只暴露这一个记忆读写入口：内部把<b>会话记忆</b>委托给 SessionManager（底层是
 * SQLite 的 Session 存储，见 TechnicalSolution.md - 9 数据持久化），把<b>长期记忆</b>委托给 {@link LongTermMemoryStore}
 * 后端（默认 {@link MarkdownMemoryStore}）。ReAct 循环组装 prompt 时只调本门面拿完整上下文，
 * 不分别去问 Session 和 MEMORY.md 两个地方。情景记忆放扩展阶段（TechnicalSolution.md - 5.5 核心阶段不做的部分）。
 *
 * <p>结构性约束（TechnicalSolution.md - 4.2 模块组成 / 5.3 Memory 注入到 system prompt）：PromptBuilder 的 Memory 注入段<b>只放长期记忆</b>；
 * 会话历史由 PromptBuilder 按 maxHistoryTurns 截断后独立注入一次，不经本门面混入 Memory 段。
 */
public class MemoryService {

    // TODO: 实施阶段补统一门面方法（委托 SessionManager + LongTermMemoryStore，TechnicalSolution.md - 5.1 模块组成/5.3 Memory 注入到 system prompt）
}
