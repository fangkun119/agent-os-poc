package com.agentos.storage.repository;

import com.agentos.storage.entity.ScheduledTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * scheduled_tasks 表仓储（TS 9.2，收尾阶段补齐，见 8.5）。骨架空接口。
 */
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTaskEntity, String> {
}
