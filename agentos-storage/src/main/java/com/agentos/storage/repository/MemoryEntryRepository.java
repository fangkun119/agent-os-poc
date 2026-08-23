package com.agentos.storage.repository;

import com.agentos.storage.entity.MemoryEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * memory_entries 表仓储（TS 9.2 条件表）：扩展阶段 SqliteMemoryStore 档引入时启用，
 * 核心阶段 Markdown 档不用。骨架空接口。
 */
public interface MemoryEntryRepository extends JpaRepository<MemoryEntryEntity, Long> {
}
