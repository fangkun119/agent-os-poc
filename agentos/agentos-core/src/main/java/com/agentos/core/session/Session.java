package com.agentos.core.session;

import java.time.Instant;

/**
 * 一次会话（TechnicalSolution.md - 4.2 模块组成 / 9.2 SQLite 关系型数据）。
 *
 * <p>session_id 为四元组 {@code <channel>-<user>-<profile>-<uuid>}，uuid 会话创建时生成、
 * 对调用方不透明（Session 重构裁决 S1，2026-09-21）；钟推与 invoke 每次触发新建单轮会话。
 * 完整对话历史经 session_messages 消息行表落 SQLite（按轮原子提交——整轮一次事务、异常零提交，见 TechnicalSolution.md - 9.2 SQLite 关系型数据；
 * 同会话并发裁决 2026-09-25），消息行全量永久保留，prompt 注入按 max_history_turns 截断
 * （存储口径与注入口径分离，Session 重构裁决 S8，2026-09-20），审计链路完整保留在 tool_invocations / llm_calls（TechnicalSolution.md - 8.5 定时任务 / 9.2 SQLite 关系型数据）。
 *
 * <p>骨架仅保留标识字段，完整字段见 TechnicalSolution.md - 9.2 SQLite 关系型数据的 sessions 表。
 */
public class Session {

    private String sessionId;
    private String profileName;
    private String channel;
    private String userId;
    private Instant lastActiveAt;

    // TODO(项目方/2026-09-25): 实施阶段补消息历史关联（经 session_messages 行表读取，见 TechnicalSolution.md - 9.2 SQLite 关系型数据）；会话无 status/归档概念（Session 重构裁决 S5，2026-09-20）

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
