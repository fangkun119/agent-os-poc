package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 每次 LLM 调用记录（token/Provider/模型，成本透明基础版），对应 SQLite 表 llm_calls
 * （TechnicalSolution.md - 9.2 SQLite 关系型数据的审计表，核心阶段就写入落库）。字段全集见 DemandAnalysis.md - 10.5 LLM Call。
 */
@Entity
@Table(name = "llm_calls")
public class LlmCallEntity {

    /** 主键，自增（DemandAnalysis.md - 10.5 LLM Call）。 */
    @Id
    @Column(name = "id")
    private Long id;

    /** Provider 名（provider name 到 ChatModel 显式映射的键，TechnicalSolution.md - 3.2 Provider 名到 ChatModel 的显式映射/9.2 SQLite 关系型数据）。 */
    @Column(name = "provider")
    private String provider;

    /** 模型名（DemandAnalysis.md - 10.5 LLM Call）。 */
    @Column(name = "model")
    private String model;

    /** 调用时刻（DemandAnalysis.md - 10.5 LLM Call）。 */
    @Column(name = "created_at")
    private Instant createdAt;
}
