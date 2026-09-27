package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 定时任务登记信息与运行状态，对应 SQLite 表 scheduled_tasks（TechnicalSolution.md - 9.2 SQLite 关系型数据，第四周收尾补齐，见 TechnicalSolution.md - 8.5 定时任务）。
 *
 * <p>定义来源是 AGENT.md frontmatter 的 schedules——本表只存"状态 + 历史"，不作为定义源，
 * 重启时从文件重新注册。字段全集见 TechnicalSolution.md - 9.2 SQLite 关系型数据。
 */
@Entity
@Table(name = "scheduled_tasks")
public class ScheduledTaskEntity {

    /** 主键：组合键 &lt;agent&gt;__&lt;scheduleId&gt;（TechnicalSolution.md - 9.2 SQLite 关系型数据，2026-09-29 E1-A（修订））。 */
    @Id
    @Column(name = "task_id")
    private String taskId;

    /** 归属 Profile（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "profile_name")
    private String profileName;

    /** cron 表达式（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "cron")
    private String cron;

    /** 下次触发时刻（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "next_run_at")
    private Instant nextRunAt;
}
