package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 定时任务登记信息与运行状态，对应 SQLite 表 scheduled_tasks（TS 9.2，收尾阶段补齐，见 8.5）。
 *
 * <p>定义来源是 AGENT.md frontmatter 的 schedules——本表只存"状态 + 历史"，不作为定义源，
 * 重启时从文件重新注册。字段全集见 TS 9.2。
 */
@Entity
@Table(name = "scheduled_tasks")
public class ScheduledTaskEntity {

    /** 主键：schedule 的 id（AGENT.md frontmatter schedules 里声明，TS 9.2）。 */
    @Id
    @Column(name = "task_id")
    private String taskId;

    /** 归属 Profile（TS 9.2）。 */
    @Column(name = "profile_name")
    private String profileName;

    /** cron 表达式（TS 9.2）。 */
    @Column(name = "cron")
    private String cron;

    /** 下次触发时刻（TS 9.2）。 */
    @Column(name = "next_run_at")
    private Instant nextRunAt;
}
