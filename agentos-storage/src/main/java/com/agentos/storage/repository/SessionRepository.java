package com.agentos.storage.repository;

import com.agentos.storage.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * sessions 表仓储（TS 9.2）。骨架空接口，查询方法随实施阶段补齐。
 */
public interface SessionRepository extends JpaRepository<SessionEntity, String> {
}
