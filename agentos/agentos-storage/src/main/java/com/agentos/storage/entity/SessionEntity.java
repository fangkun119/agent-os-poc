package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Session 元数据行，对应 SQLite 表 sessions（TechnicalSolution.md - 9.2 SQLite 关系型数据的核心表之一）。
 *
 * <p>session_id 为四元组 {@code <channel>-<user>-<profile>-<uuid>}（uuid 会话创建时生成、
 * 对调用方不透明；channel/user/profile 三元组在本表有独立列，无需解析 id——Session 重构裁决 S1，
 * 2026-09-21）。钟推每次触发新建单轮会话。对话历史不在本表：消息逐行存
 * {@link SessionMessageEntity}（session_messages 行表，按轮原子提交——整轮一次事务、异常零提交；消息行全量永久保留，
 * prompt 注入按 max_history_turns 截断——存储口径与注入口径分离，Session 重构裁决 S8，2026-09-20；同会话并发裁决 2026-09-25）。
 * 字段全集见 TechnicalSolution.md - 9.2 SQLite 关系型数据。首建走 ddl-auto=update。
 */
@Entity
@Table(name = "sessions")
public class SessionEntity {

    /** 主键：四元组 {@code <channel>-<user>-<profile>-<uuid>}，对调用方不透明（TechnicalSolution.md - 9.2 SQLite 关系型数据；Session 重构裁决 S1，2026-09-21）。 */
    @Id
    @Column(name = "session_id")
    private String sessionId;

    /** 关联 Profile 名（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "profile_name")
    private String profileName;

    /** 接入 Channel（TechnicalSolution.md - 9.2 SQLite 关系型数据；取值五常量 cli/web/invoke/scheduler/generate，generate 为扩展阶段生成会话专用）。 */
    @Column(name = "channel")
    private String channel;

    /** 用户标识（会话身份独立列，格式 [a-z0-9-_]{1,32}；TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "user_id")
    private String userId;

    /** 最后活跃时间（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "last_active_at")
    private Instant lastActiveAt;
}
