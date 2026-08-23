package com.agentos.memory;

/**
 * 默认后端：底层操作 {@code .agentos/memory/MEMORY.md} 单个 Markdown 文件（TS 5.1 / 5.2）。
 *
 * <p>文件内部按 {@code ## 核心记忆} / {@code ## 归档记忆} 两个 header 分区，每条记忆带日期 header，
 * 格式不做更严格规定——Agent 写什么 LLM 自己理解。截断是字符串裁归档段，检索是 {@code String.contains} 行匹配。
 * 零依赖、人可读、git 可跟踪，记忆量不大时的首选。
 *
 * <p>{@code SqliteMemoryStore}（记忆按条入库 {@code memory_entries} 表）/ {@code Mem0MemoryStore}
 * （自托管 Mem0 记忆层，REST 集成）属扩展阶段，经 {@code memory.backend} 切换补齐（TS 5.1）。
 */
public class MarkdownMemoryStore implements LongTermMemoryStore {

    @Override
    public void append(String content, MemoryScope scope) {
        // TODO: 实施阶段补 MEMORY.md 分区追加（TS 5.1/5.2）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.append（TS 5.1/5.2）");
    }

    @Override
    public String load() {
        // TODO: 实施阶段补核心区全量 + 归档区截断读取（TS 5.1/5.2）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.load（TS 5.1/5.2）");
    }

    @Override
    public String recallByKeyword(String keyword) {
        // TODO: 实施阶段补归档区 String.contains 行匹配（TS 5.1）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.recallByKeyword（TS 5.1）");
    }

    @Override
    public void truncateIfNeeded() {
        // TODO: 实施阶段补归档段字符串裁剪（核心区永不截断）（TS 5.1）
        throw new UnsupportedOperationException("尚未实现：MarkdownMemoryStore.truncateIfNeeded（TS 5.1）");
    }
}
