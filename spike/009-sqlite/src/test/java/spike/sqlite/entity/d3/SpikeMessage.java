package spike.sqlite.entity.d3;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * D3 实体：表 spike_message，一轮对话消息行（001-spec.md - 2.3 条款 D3（P0）：轮原子提交事务与并发 BUSY 面）。
 *
 * <p>结构取生产 session_messages 的最小面（TechnicalSolution.md - 9.2 SQLite 关系型数据的消息行形态）：
 * 一轮 = 1 条 role=user + 2 条 role=assistant + 2 条 role=tool 共 5 行，由 RoundCommitService
 * 在单个事务内写入；"事务内分配的主键 id 连续"是 D3 正常路径的观察点。
 */
@Entity
@Table(name = "spike_message")
public class SpikeMessage {

    /** 主键：数据库自增（GenerationType.IDENTITY；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）SQLite 主键条款）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属会话标识（D3 实验入参，无逻辑外键）。 */
    @Column(name = "session_id")
    private String sessionId;

    /** 消息角色：user / assistant / tool（一轮 1+2+2，字面量收敛在 RoundCommitService 常量）。 */
    @Column(name = "role")
    private String role;

    /** 消息内容占位串（D3 只验写入原子性与 id 面，不验内容格式）。 */
    @Column(name = "payload")
    private String payload;

    /** 写入时刻。 */
    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
