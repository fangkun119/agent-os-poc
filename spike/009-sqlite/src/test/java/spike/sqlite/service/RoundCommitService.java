package spike.sqlite.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spike.sqlite.entity.d3.SpikeMessage;
import spike.sqlite.entity.d3.SpikeMessageRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * D3 JPA 路径服务：轮原子提交的两个事务方法（001-plan.md - 2.4 测试类清单 的 RoundCommitService 说明；
 * 001-spec.md - 2.3 条款 D3（P0）：轮原子提交事务与并发 BUSY 面）。
 *
 * <p>事务纪律：@Transactional 体内只做写库，不做任何阻塞等待或外部行为仿真
 * （agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）"物理事务尽可能短"条款）。
 * 一个业务动作（一整轮）的多次写库落同一事务边界，禁逐语句独立提交（同上条款）。
 */
@Service
public class RoundCommitService {

    /** 一轮消息的角色序列：1 条 user + 2 条 assistant + 2 条 tool（业务语义字面量先具名常量，agentos/CLAUDE.md - 9 命名、注释与组织）。 */
    private static final String[] ROUND_ROLES = {"user", "assistant", "assistant", "tool", "tool"};

    /** 一轮固定 5 行（判据：行数=5）。 */
    private static final int ROUND_ROW_COUNT = 5;

    private final SpikeMessageRepository repository;

    public RoundCommitService(SpikeMessageRepository repository) {
        this.repository = repository;
    }

    /**
     * 单事务内插入一整轮 5 行（1 user + 2 assistant + 2 tool）并提交，返回事务内分配的 5 个主键 id（插入序）。
     *
     * @param sessionId 会话标识（写入 session_id 列）
     * @return 事务内分配的主键 id 清单，按插入顺序排列（D3 据此断言 id 连续）
     */
    @Transactional
    public List<Long> commitRound(String sessionId) {
        List<SpikeMessage> savedRows = insertRound(sessionId);
        List<Long> ids = new ArrayList<>(ROUND_ROW_COUNT);
        for (SpikeMessage row : savedRows) {
            ids.add(row.getId());
        }
        return ids;
    }

    /**
     * 与 commitRound 同结构插入一整轮 5 行，插入完成后抛 RuntimeException 触发回滚
     * （D3 异常路径的实验条件：001-spec.md - 2.3 条款 D3（P0）行为规约第 2 条明定抛 RuntimeException，
     * 属实验探针、非生产异常选型惯例的例外）。
     *
     * @param sessionId 会话标识（写入 session_id 列）
     */
    @Transactional
    public void commitThenThrow(String sessionId) {
        insertRound(sessionId);
        throw new RuntimeException("D3 异常路径探针：事务内插入 5 行后主动抛出，预期整轮回滚");
    }

    /**
     * 一轮 5 行的插入实现：顺序 save 5 行并回填实体（IDENTITY 主键在 save 时即分配）。
     * 私有方法，仅由本类的 @Transactional 方法调用，不自开事务边界。
     */
    private List<SpikeMessage> insertRound(String sessionId) {
        List<SpikeMessage> savedRows = new ArrayList<>(ROUND_ROW_COUNT);
        for (String role : ROUND_ROLES) {
            SpikeMessage row = new SpikeMessage();
            row.setSessionId(sessionId);
            row.setRole(role);
            row.setPayload("spike-round:" + sessionId + ":" + role);
            row.setCreatedAt(Instant.now());
            savedRows.add(repository.save(row));
        }
        return savedRows;
    }
}
