package com.agentos.core.session;

import java.time.Instant;

/**
 * 一次会话（TS 4.2 / 9.2）。
 *
 * <p>session_id 由 channel + user + profile 联合生成；完整对话历史经 messages_json 落 SQLite 的 sessions 表。
 * 钟推 Session（channel/user 固定 scheduler）历次触发复用同一 session_id，messages_json 每次落盘时
 * 按 max_history_turns 物理裁剪，审计链路完整保留在 tool_invocations / llm_calls（TS 8.5 / 9.2）。
 *
 * <p>骨架仅保留标识字段，完整字段见 TS 9.2 sessions 表。
 */
public class Session {

    private String sessionId;
    private String profileName;
    private String channel;
    private String userId;
    private Instant lastActiveAt;

    // TODO: 实施阶段补消息历史与状态（active/archived）、归档时间等字段（TS 9.2）

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
