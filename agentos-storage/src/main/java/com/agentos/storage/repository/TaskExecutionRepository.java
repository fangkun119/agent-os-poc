package com.agentos.storage.repository;

import com.agentos.storage.entity.TaskExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * task_executions 表仓储（TS 9.2，收尾阶段补齐，见 8.5）。骨架空接口。
 */
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, Long> {
}
