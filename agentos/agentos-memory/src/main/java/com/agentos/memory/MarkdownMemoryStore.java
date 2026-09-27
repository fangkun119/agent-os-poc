package com.agentos.memory;

/**
 * 默认后端：底层操作 {@code .agentos/memory/<agent>/<user>/MEMORY.md}，按 &lt;Agent, 用户&gt;
 * 二元组分档、每二元组一份（TechnicalSolution.md - 5.1 模块组成 / 5.2 MEMORY.md 文件设计）。
 *
 * <p>文件内部按 {@code ## 核心记忆} / {@code ## 归档记忆} 两个 header 分区，每条记忆带日期 header，
 * 格式不做更严格规定——Agent 写什么 LLM 自己理解。截断是视图级裁剪——只裁注入 prompt 的归档段视图、文件本体不动（视图级截断决议，2026-09-18），
 * 检索是 {@code String.contains} 行匹配。
 * 零依赖、人可读、git 可跟踪，记忆量不大时的首选。
 *
 * <p>{@code SqliteMemoryStore}（记忆按条入库 {@code memory_entries} 表）/ {@code Mem0MemoryStore}
 * （自托管 Mem0 记忆层，REST 集成）属扩展阶段，经 {@code memory.backend} 切换补齐（TechnicalSolution.md - 5.1 模块组成）。
 */
public class MarkdownMemoryStore implements LongTermMemoryStore {

    @Override
    public void append(String content, MemoryScope scope) {
        // TODO: 实施阶段补 MEMORY.md 分区追加（TechnicalSolution.md - 5.1 模块组成/5.2 MEMORY.md 文件设计）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.append（TechnicalSolution.md - 5.1 模块组成 / 5.2 MEMORY.md 文件设计）");
    }

    @Override
    public String load() {
        // TODO: 实施阶段补核心区全量 + 归档区截断读取（TechnicalSolution.md - 5.1 模块组成/5.2 MEMORY.md 文件设计）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.load（TechnicalSolution.md - 5.1 模块组成 / 5.2 MEMORY.md 文件设计）");
    }

    @Override
    public String recallByKeyword(String keyword) {
        // TODO: 实施阶段补归档区 String.contains 行匹配（TechnicalSolution.md - 5.1 模块组成）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.recallByKeyword（TechnicalSolution.md - 5.1 模块组成）");
    }

    @Override
    public void truncateIfNeeded() {
        // TODO: 实施阶段补归档段字符串裁剪（核心区永不截断）（TechnicalSolution.md - 5.1 模块组成）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.truncateIfNeeded（TechnicalSolution.md - 5.1 模块组成）");
    }
}
