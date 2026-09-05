package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Session 元数据加 JSON 序列化的对话历史，对应 SQLite 表 sessions（TS 9.2 核心表之一）。
 *
 * <p>session_id 由 channel + user + profile 联合生成；钟推 Session（channel/user 固定 scheduler）
 * 历次触发复用同一 session_id，messages_json 每次落盘时按 max_history_turns 物理裁剪。
 * 字段全集见 TS 9.2。首建走 ddl-auto=update。
 */
@Entity
@Table(name = "sessions")
public class SessionEntity {

    /** 主键：channel 加 user 加 profile 联合生成（TS 9.2）。 */
    @Id
    @Column(name = "session_id")
    private String sessionId;

    /** 关联 Profile 名（TS 9.2）。 */
    @Column(name = "profile_name")
    private String profileName;

    /** 接入 Channel（TS 9.2）。 */
    @Column(name = "channel")
    private String channel;

    /** 最后活跃时间（TS 9.2）。 */
    @Column(name = "last_active_at")
    private Instant lastActiveAt;
}
