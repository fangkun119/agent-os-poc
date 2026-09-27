package com.agentos.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 会话消息行，对应 SQLite 表 session_messages（TechnicalSolution.md - 9.2 SQLite 关系型数据的核心表之一；同会话并发裁决 2026-09-25）。
 *
 * <p>对话历史逐行存储：id 全局自增=插入序（保留 AUTOINCREMENT：扩展阶段有会话删除端点，防 rowid 复用；
 * 该关键字社区方言建表不会生成——spike/009-sqlite D4 坑 1（2026-10-09 实测），且 SQLite 禁 ALTER 追加，
 * 首建后须按官方建表语法重建补齐，治理口径见 TechnicalSolution.md - 9.2 SQLite 关系型数据的「工程风险提示」段），
 * 同会话按 id 排序即对话顺序；提交纪律为轮原子提交——正常完成整轮一次事务写入（单写者下事务内行 id 连续），
 * 进程崩溃、总超时、LLM 调用报错、轮末提交失败四类异常零提交、本轮全部已执行迭代不留正文，库中只含完整轮。
 * 索引 (session_id, id)；SQLite 外键默认不启用，扩展阶段真删除会话时应用层同删本表行（TechnicalSolution.md - 7.3 扩展阶段补齐的端点）。
 * 字段全集与提交纪律见 TechnicalSolution.md - 9.2 SQLite 关系型数据。首建走 ddl-auto=update。
 */
@Entity
@Table(name = "session_messages",
       indexes = @Index(name = "idx_session_messages_session_id", columnList = "session_id, id"))
public class SessionMessageEntity {

    /** 主键：全局自增=插入序，同会话按 id 排序即对话顺序（TechnicalSolution.md - 9.2 SQLite 关系型数据；AUTOINCREMENT 防 rowid 复用——方言建表不生成、首建后手工补 DDL，见类注释）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属会话（逻辑外键 → sessions.session_id；外键约束未启用，见 TechnicalSolution.md - 9.2 SQLite 关系型数据/7.3 扩展阶段补齐的端点）。 */
    @Column(name = "session_id")
    private String sessionId;

    /** 一条消息的 JSON 原文整段存（消息内字段集不保证恒定、不拆列；TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "payload_json")
    private String payloadJson;

    /** 写入时刻（TechnicalSolution.md - 9.2 SQLite 关系型数据）。 */
    @Column(name = "created_at")
    private Instant createdAt;
}
