package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 扩展阶段 SqliteMemoryStore 档引入时建表，核心阶段 Markdown 档不建（TS 9.2 条件表，见 5.1）。
 *
 * <p>记忆按条入库：截断变成归档查询的 LIMIT N、检索变成 SQL LIKE、
 * 核心区用 WHERE scope='CORE' 全量取。字段全集见 TS 9.2。
 */
@Entity
@Table(name = "memory_entries")
public class MemoryEntryEntity {

    /** 主键，自增（TS 9.2）。 */
    @Id
    @Column(name = "id")
    private Long id;

    /** 区分 CORE / ARCHIVAL 两区（TS 5.1/9.2）。 */
    @Column(name = "scope")
    private String scope;

    /** 记忆条目内容（TS 5.1）。 */
    @Column(name = "content")
    private String content;

    /** 写入时刻（TS 9.2）。 */
    @Column(name = "created_at")
    private Instant createdAt;
}
