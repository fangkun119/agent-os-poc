package com.agentos.storage.repository;

import com.agentos.storage.entity.TaskExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * task_executions 表仓储（TechnicalSolution.md - 9.2 SQLite 关系型数据，第四周收尾补齐，见 TechnicalSolution.md - 8.5 定时任务）。骨架空接口。
 */
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, Long> {
}
