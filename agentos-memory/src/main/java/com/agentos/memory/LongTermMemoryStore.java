package com.agentos.memory;

/**
 * 长期记忆后端接口（可插拔，TS 5.1）。
 *
 * <p>把"长期记忆的读写契约"和"具体存哪、怎么存"解耦——早期方案评审定下的"接口墙"在实现层的落地。
 * 核心阶段交付 {@link MarkdownMemoryStore} 默认档；{@code SqliteMemoryStore} / {@code Mem0MemoryStore}
 * 属扩展阶段，经 {@code memory.backend} 一行配置切换补齐，{@code MemoryService} 以上一个字不动（TS 5.1）。
 *
 * <p>所有实现共同遵守四条行为契约（TS 5.1）：
 * <ol>
 *   <li><b>不缓存</b>：每次重新读文件 / 查库 / 调 API，保证 Agent 调 save_memory 后下一轮立刻可见（TS 5.3）；</li>
 *   <li><b>核心记忆区永不被截断</b>：截断只作用在归档区；</li>
 *   <li><b>scope 显式</b>：写核心还是写归档由 Agent 显式指定，系统不猜（核心阶段不做自动抽取）；</li>
 *   <li><b>关键词检索不复杂化</b>：recall 是关键词匹配，语义检索是 Mem0 档自带的升级空间。</li>
 * </ol>
 */
public interface LongTermMemoryStore {

    /**
     * 追加一条记忆到指定分区（TS 5.1）。
     *
     * <p>默认语义为 {@link MemoryScope#ARCHIVAL}：Agent 未显式选择分区时落归档区，
     * 只有明确标 {@link MemoryScope#CORE} 的内容才进永不截断的核心区（契约三：系统不猜）。
     *
     * @param content 记忆内容
     * @param scope   目标分区
     */
    void append(String content, MemoryScope scope);

    /**
     * 返回核心记忆区全量 + 归档记忆区截断后的内容（TS 5.1 / 5.3）。
     *
     * <p>内部调用 {@link #truncateIfNeeded()} 对归档区先行截断；每次重新读，不做缓存（契约一）。
     */
    String load();

    /**
     * 按关键词检索，只在归档记忆区做匹配（TS 5.1）。
     *
     * <p>核心区不参与检索——它本来就会被 load 全量注入。关键词匹配，不做复杂化（契约四）。
     */
    String recallByKeyword(String keyword);

    /**
     * 对归档记忆区执行超限截断，由 {@link #load()} 内部调用（TS 5.1）。
     *
     * <p>核心记忆区永不截断（契约二）。
     */
    void truncateIfNeeded();
}
