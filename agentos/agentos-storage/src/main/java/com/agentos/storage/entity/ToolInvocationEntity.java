package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 每次 Tool 调用记录（含 Sandbox 拒绝，success=false），对应 SQLite 表 tool_invocations
 * （TechnicalSolution.md - 9.2 SQLite 关系型数据的审计表，核心阶段就写入落库，不是只放日志）。字段全集见 DemandAnalysis.md - 10.4 Tool Invocation。
 */
@Entity
@Table(name = "tool_invocations")
public class ToolInvocationEntity {

    /** 主键，自增（DemandAnalysis.md - 10.4 Tool Invocation）。 */
    @Id
    @Column(name = "id")
    private Long id;

    /** 归属 Session（DemandAnalysis.md - 10.4 Tool Invocation）。 */
    @Column(name = "session_id")
    private String sessionId;

    /** 被调用的 Tool 名（DemandAnalysis.md - 10.4 Tool Invocation）。 */
    @Column(name = "tool_name")
    private String toolName;

    /** 调用时刻（DemandAnalysis.md - 10.4 Tool Invocation）。 */
    @Column(name = "created_at")
    private Instant createdAt;
}
