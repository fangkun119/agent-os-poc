package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 定时任务每次执行的历史，成功失败都记，对应 SQLite 表 task_executions
 * （TS 9.2，收尾阶段补齐，见 8.5）。字段全集见 TS 9.2。
 */
@Entity
@Table(name = "task_executions")
public class TaskExecutionEntity {

    /** 主键，自增（TS 9.2）。 */
    @Id
    @Column(name = "id")
    private Long id;

    /** 关联 scheduled_tasks 的 task_id（TS 9.2）。 */
    @Column(name = "task_id")
    private String taskId;

    /** 本次触发所用的钟推 Session（TS 9.2）。 */
    @Column(name = "session_id")
    private String sessionId;

    /** 开始时间（TS 9.2）。 */
    @Column(name = "started_at")
    private Instant startedAt;
}
