package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 每次 LLM 调用记录（token/Provider/模型，成本透明基础版），对应 SQLite 表 llm_calls
 * （TS 9.2 审计表，核心阶段就写入落库）。字段全集见 TS 9.2。
 */
@Entity
@Table(name = "llm_calls")
public class LlmCallEntity {

    /** 主键，自增（TS 9.2）。 */
    @Id
    @Column(name = "id")
    private Long id;

    /** Provider 名（provider name 到 ChatModel 显式映射的键，TS 3.2/9.2）。 */
    @Column(name = "provider")
    private String provider;

    /** 模型名（TS 9.2）。 */
    @Column(name = "model")
    private String model;

    /** 调用时刻（TS 9.2）。 */
    @Column(name = "called_at")
    private Instant calledAt;
}
