package com.agentos.memory;

/**
 * 长期记忆的分区标识（TS 5.1）。
 *
 * <p>写核心区还是写归档区由 Agent 经 scope 显式指定，系统不猜（行为契约三）。
 * 分区约定跨后端不变：Markdown 档落到 {@code ## 核心记忆} / {@code ## 归档记忆} 两个 header，
 * SQLite 档落到 {@code memory_entries.scope} 列，Mem0 档落到 metadata（TS 5.2）。
 */
public enum MemoryScope {

    /** 核心记忆区：每次 {@link LongTermMemoryStore#load()} 全量注入，永不截断，不参与关键词检索。 */
    CORE,

    /** 归档记忆区：load 时截断后注入，是 recallByKeyword 唯一的检索范围；append 的默认语义。 */
    ARCHIVAL
}
