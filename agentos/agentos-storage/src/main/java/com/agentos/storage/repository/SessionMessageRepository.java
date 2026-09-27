package com.agentos.storage.repository;

import com.agentos.storage.entity.SessionMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * session_messages 表仓储（TechnicalSolution.md - 9.2 SQLite 关系型数据的核心表之一；同会话并发裁决 2026-09-25）：按 session_id 范围取消息行
 * 组装对话历史、按轮边界截取注入 prompt（见 TechnicalSolution.md - 9.2 SQLite 关系型数据的轮边界截取）。骨架空接口。
 */
public interface SessionMessageRepository extends JpaRepository<SessionMessageEntity, Long> {
}
