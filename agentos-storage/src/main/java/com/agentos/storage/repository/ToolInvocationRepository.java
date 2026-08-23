package com.agentos.storage.repository;

import com.agentos.storage.entity.ToolInvocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * tool_invocations 审计表仓储（TS 9.2）：核心阶段写入落库，查询接口放扩展阶段。骨架空接口。
 */
public interface ToolInvocationRepository extends JpaRepository<ToolInvocationEntity, Long> {
}
