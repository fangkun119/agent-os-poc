package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 定时任务每次执行的历史，成功失败都记，对应 SQLite 表 task_executions
 * （TechnicalSolution.md - 9.2 SQLite 关系型数据，第四周收尾补齐，见 TechnicalSolution.md - 8.5 定时任务）。字段全集见 TechnicalSolution.md - 9.2 SQLite 关系型数据。
 */
@Entity
@Table(name = "task_executions")
public class TaskExecutionEntity {

    /** 主键，自增（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Id
    @Column(name = "id")
    private Long id;

    /** 关联 scheduled_tasks 的 task_id（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "task_id")
    private String taskId;

    /** 本次触发所用的钟推 Session（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "session_id")
    private String sessionId;

    /** 开始时间（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "started_at")
    private Instant startedAt;
}
