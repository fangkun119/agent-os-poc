package com.agentos.storage.repository;

import com.agentos.storage.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * sessions 表仓储（TechnicalSolution.md - 9.2 SQLite 关系型数据）。骨架空接口，查询方法随实施阶段补齐。
 */
public interface SessionRepository extends JpaRepository<SessionEntity, String> {
}
