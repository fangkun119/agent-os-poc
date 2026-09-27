package spike.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import spike.sqlite.entity.d3.SpikeMessage;
import spike.sqlite.entity.d3.SpikeMessageRepository;
import spike.sqlite.service.RoundCommitService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D3：轮原子提交事务与并发 BUSY 面（001-plan.md - 3 任务明细 T3；001-spec.md - 2.3 条款 D3（P0）：
 * 轮原子提交事务与并发 BUSY 面）。
 *
 * <p>四个方法与通过判据的对应（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）D3 行方法清单逐字落实，
 * 四段日志前缀 [D3-正常路径] / [D3-异常路径] / [D3-并发组一] / [D3-并发组二] 即判据 5 的四条证据段）：
 * 方法① 正常路径（判据 1：行数=5、事务内 id 排序后逐个 +1 连续，id 清单打印）；方法② 异常路径（判据 2：
 * commitThenThrow 抛 RuntimeException 回滚后行数=0）；方法③ 并发组一（判据 3：A 持锁 1000ms &lt;
 * busy_timeout 5000ms，B 的等待时长 System.nanoTime 实测打印；预登记两分支——B 等待后成功 = 预期、
 * 直接异常 = 偏差，命中哪个都登记到明确一栏，不判失败不重跑改结果，001-plan.md - 5 分支与风险
 * （每步失败观察点与下一步）R3）；方法④ 并发组二（判据 4：A 持锁 6500ms &gt; busy_timeout 5000ms，
 * 捕获 B 的异常类型名与完整消息原文；未拿到 BUSY 时按 R4 加大占用到 8000ms 重跑一次、两轮结果都落档）。
 *
 * <p>装配：JPA 路径走 T0 已建的 RoundCommitService（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）
 * 末尾说明），内嵌 config 只圈 spike.sqlite.entity.d3 包（001-plan.md - 2.2 目录结构 骨架设计说明 2），
 * URL 覆盖为 d3.db、连接串三参数与 application.yaml 原样保留（001-plan.md - 2.3 application.yaml（T0-3））。
 * 并发两组用裸 JDBC（DriverManager 直连、不经连接池，隔离连接池语义），连接串三参数与 D2 已验证的正向组
 * 写法一致（001-spec.md - 2.3 行为规约第 3 条；D2 实测三参数生效见 logs/d2-pragma.txt）。事务纪律：
 * 事务体内只做写库与计时等待，不仿真 LLM 调用等行为（agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）
 * "物理事务尽可能短"条款）。
 */
@SpringBootTest(classes = D3RoundAtomicCommitTest.D3Context.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=" + D3RoundAtomicCommitTest.D3_JDBC_URL)
@TestMethodOrder(OrderAnnotation.class)
class D3RoundAtomicCommitTest {

    private static final Logger log = LoggerFactory.getLogger(D3RoundAtomicCommitTest.class);

    /** 本类两个库文件的共同目录（001-plan.md - 2.2 目录结构 骨架设计说明 3：统一放 target/spike-data/）。 */
    private static final Path DATA_DIR = Path.of("target", "spike-data");

    /**
     * D3 专用连接串（JPA 路径）：库文件换 d3.db，参数段与 application.yaml 原样保留
     * （journal_mode=WAL &amp; busy_timeout=5000 &amp; synchronous=FULL；001-plan.md - 2.3 application.yaml（T0-3））。
     * 同时供 @SpringBootTest 属性覆盖使用（编译期常量保证与生效装配一字不差）。
     */
    static final String D3_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d3.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL";

    /**
     * 并发两组连接串：库文件 d3-conc.db（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）D3 行），
     * 三参数与 D2 正向组同款（001-spec.md - 2.3 行为规约第 3 条：连接串写法与 D2 已验证的写法一致）。
     */
    static final String D3_CONC_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d3-conc.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL";

    /** 一轮的角色序列（与 RoundCommitService 的插入序列一致；此处从落库侧断言同一序列）。 */
    private static final List<String> ROUND_ROLES = List.of("user", "assistant", "assistant", "tool", "tool");

    /** 并发两组共用的探针表名（d3-conc.db 内，裸 JDBC 建表，不进 JPA 持久化单元）。 */
    private static final String CONC_TABLE = "d3_conc_probe";

    @Autowired
    private RoundCommitService roundCommitService;

    @Autowired
    private SpikeMessageRepository spikeMessageRepository;

    /** 生效装配 URL 留档用（应等于 D3_JDBC_URL，证明覆盖到位）。 */
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    /**
     * D3 实验上下文（001-plan.md - 2.2 目录结构 骨架设计说明 2：内嵌 @SpringBootConfiguration + 显式圈定）。
     *
     * <p>@EntityScan / @EnableJpaRepositories 只圈 spike.sqlite.entity.d3，阻断其他实验（d1v1/d1v2/d4）
     * 的实体与仓库混进本上下文——尤其 d1v1/d1v2 两组实体同表名 sample_row 绝不能同上下文共存。
     * RoundCommitService（@Service）经 @Import 显式注册（@SpringBootConfiguration 不含组件扫描，
     * 裸注解起不了 @Service；显式单 Bean 注册与"显式圈定、不开开放式扫描"同一取向）。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("spike.sqlite.entity.d3")
    @EnableJpaRepositories("spike.sqlite.entity.d3")
    @Import(RoundCommitService.class)
    static class D3Context {
    }

    /**
     * 统一清理重建本类两个库文件（含 -wal/-shm 伴生），保证正常路径"全新库计数从 0 起"的语义可复现
     * （001-plan.md - 2.2 目录结构 骨架设计说明 3）。此刻 Spring 上下文尚未启动，删除不与任何存活连接冲突
     * （与 T1/T2 已验证的 D2/D4 同款时序）；spike_message 表由本上下文启动时 ddl-auto=update 首建。
     */
    @BeforeAll
    static void cleanSpikeDataFiles() throws Exception {
        Files.createDirectories(DATA_DIR);
        for (String dbFile : List.of("d3.db", "d3-conc.db")) {
            Files.deleteIfExists(DATA_DIR.resolve(dbFile));
            Files.deleteIfExists(DATA_DIR.resolve(dbFile + "-wal"));
            Files.deleteIfExists(DATA_DIR.resolve(dbFile + "-shm"));
        }
        log.info("[D3-前置] 已清理重建 {}（d3.db / d3-conc.db 及各自 -wal/-shm）", DATA_DIR);
    }

    /**
     * 方法①（判据 1）：调 RoundCommitService.commitRound（@Transactional，单事务 5 行），断言行数=5、
     * 事务内分配的 id 排序后逐个 +1 连续，id 清单打印；并从落库侧核对 5 行的角色序列（1 user + 2 assistant
     * + 2 tool）与 id 集合。"事务内 id 连续"对照 TechnicalSolution.md - 4.3 关键设计点 (2) 的单写者语义。
     */
    @Test
    @Order(1)
    void normalPath_roundOfFive_idsContinuous() {
        log.info("[D3-正常路径] 生效 spring.datasource.url = {}", datasourceUrl);
        List<Long> ids = roundCommitService.commitRound("d3-normal-round");
        List<Long> sortedIds = ids.stream().sorted().toList();
        log.info("[D3-正常路径] commitRound 返回 id 清单（插入序）= {}，排序后 = {}", ids, sortedIds);
        assertThat(ids).as("一轮返回的 id 清单").hasSize(5);
        for (int i = 1; i < sortedIds.size(); i++) {
            assertThat(sortedIds.get(i)).as("事务内分配 id 排序后第 %d 对相邻应逐个 +1 连续", i)
                    .isEqualTo(sortedIds.get(i - 1) + 1);
        }

        List<SpikeMessage> rows = spikeMessageRepository.findAll(Sort.by("id"));
        for (SpikeMessage row : rows) {
            log.info("[D3-正常路径] 落库行：id = {}，session_id = {}，role = {}，payload = {}，created_at = {}",
                    row.getId(), row.getSessionId(), row.getRole(), row.getPayload(), row.getCreatedAt());
        }
        long rowCount = spikeMessageRepository.count();
        log.info("[D3-正常路径] 提交后查询行数 = {}（判据：5），落库 id 序列 = {}，角色序列 = {}",
                rowCount, rows.stream().map(SpikeMessage::getId).toList(),
                rows.stream().map(SpikeMessage::getRole).toList());
        assertThat(rowCount).as("正常路径提交后行数=5").isEqualTo(5);
        assertThat(rows).as("落库行清单").hasSize(5);
        assertThat(rows.stream().map(SpikeMessage::getId).toList())
                .as("落库 id 序列应与事务内返回的 id 排序序列一致").containsExactlyElementsOf(sortedIds);
        assertThat(rows.stream().map(SpikeMessage::getRole).toList())
                .as("一轮角色序列应为 1 user + 2 assistant + 2 tool").containsExactlyElementsOf(ROUND_ROLES);
    }

    /**
     * 方法②（判据 2）：调 commitThenThrow（同结构插入 5 行后抛 RuntimeException 触发回滚），回滚后查询
     * 行数=0（001-spec.md - 2.3 通过判据 2 字面口径）。方法①已提交 5 行，故先清空再取零行基线，使
     * "回滚后行数=0"落在全表计数上、证据无歧义。
     */
    @Test
    @Order(2)
    void exceptionPath_rollbackZeroRows() {
        spikeMessageRepository.deleteAll();
        long baseline = spikeMessageRepository.count();
        log.info("[D3-异常路径] 已清空方法①样本，异常轮前基线行数 = {}", baseline);
        assertThat(baseline).as("异常轮前基线应清零").isZero();

        RuntimeException probe = null;
        try {
            roundCommitService.commitThenThrow("d3-exception-round");
        } catch (RuntimeException caught) {
            probe = caught;
        }
        if (probe != null) {
            log.info("[D3-异常路径] 捕获探针异常：类型 = {}，消息原文 = {}",
                    probe.getClass().getName(), probe.getMessage());
        } else {
            log.warn("[D3-异常路径] commitThenThrow 未抛出异常（回滚前提不成立，实验代码缺陷嫌疑）");
        }
        assertThat(probe).as("commitThenThrow 应抛出 RuntimeException 触发回滚").isInstanceOf(RuntimeException.class);
        assertThat(probe).as("探针异常消息原文").hasMessageContaining("D3 异常路径探针");

        long afterRollback = spikeMessageRepository.count();
        log.info("[D3-异常路径] 回滚后查询行数 = {}（判据：0，异常轮零行落库）", afterRollback);
        assertThat(afterRollback).as("回滚后查询行数=0").isZero();
    }

    /**
     * 方法③（判据 3）：并发组一（等待成功面）——两个裸 JDBC 连接交叠写 d3-conc.db（连接串三参数与 D2
     * 正向组同款）：A 关自动提交、INSERT、持锁等待 1000ms（&lt;busy_timeout 5000ms）后提交；同窗口 B 发起
     * INSERT，等待时长 System.nanoTime 实测打印。预登记两分支：B 等待后成功 = 预期；直接异常 = 偏差
     * （001-plan.md - 5 分支与风险（每步失败观察点与下一步）R3：属预登记分支之一，照实登记明确一栏，
     * 不判失败、不重跑改结果——本方法对两分支都不做成败断言，只登记 + 机械一致性核验）。
     */
    @Test
    @Order(3)
    void concurrentGroup1_holdShort_waitThenSuccess() throws Exception {
        log.info("[D3-并发组一] === 判据 3：占用 < busy_timeout（A 持锁目标 1000ms），预登记预期 = B 等待后成功 ===");
        OverlapScenarioResult result = runOverlapScenario("并发组一", 1000L, "g1-A", "g1-B");
        WriterOutcome b = result.bOutcome;
        assertThat(b.unexpectedError).as("并发组一 B 线程不应有非预期异常").isNull();

        if (b.insertSucceeded) {
            log.info("[D3-并发组一] 【分支登记】命中：预登记预期（等待后成功）——B 从发起 INSERT 到写入成功实测 {} ms"
                    + "（A 持锁实测 {} ms，busy_timeout 回读 = {}）",
                    toMillisText(b.waitNanos), toMillisText(result.aHoldNanos), b.busyTimeoutReadback);
        } else {
            log.info("[D3-并发组一] 【分支登记】命中：偏差（B 未等待成功而抛异常，R3 预登记偏差分支）");
            log.info("[D3-并发组一] 偏差证据（R3 三观察点）：异常类型 = {}，消息原文 = {}，SQLite 结果码 = {}；"
                    + "B 连接 busy_timeout 回读 = {}（应 5000）；A 持锁实测 {} ms（关自动提交、INSERT 已执行未提交）",
                    b.exceptionTypeName, b.exceptionMessage, b.resultCodeName,
                    b.busyTimeoutReadback, toMillisText(result.aHoldNanos));
        }

        long aRows = countConcRows("g1-A");
        long bRows = countConcRows("g1-B");
        log.info("[D3-并发组一] 机械一致性核验：g1-A 行数 = {}（A 提交成功应为 1），g1-B 行数 = {}（B {} 应为 {}）",
                aRows, bRows, b.insertSucceeded ? "写入成功" : "写入异常", b.insertSucceeded ? 1 : 0);
        assertThat(aRows).as("并发组一：A 提交行 g1-A 应为 1 行").isEqualTo(1L);
        assertThat(bRows).as("并发组一机械一致性：B 行落库数应与 B 写入结果一致")
                .isEqualTo(b.insertSucceeded ? 1L : 0L);
    }

    /**
     * 方法④（判据 4）：并发组二（超时错误面）——同交叠结构但 A 持锁 6500ms（&gt;busy_timeout 5000ms），
     * 捕获 B 的异常类型名与完整消息原文打印（BUSY 错误在 JDBC 层的面貌，供正式实现"报错与有限重试出口"
     * 设计引用）。若 B 等待后成功（未拿到 BUSY），按 001-plan.md - 5 分支与风险（每步失败观察点与下一步）
     * R4 加大 A 占用到 8000ms 重跑一次，两轮结果都落档、结论取实测。
     */
    @Test
    @Order(4)
    void concurrentGroup2_holdBeyondTimeout_busySample() throws Exception {
        log.info("[D3-并发组二] === 判据 4：占用 > busy_timeout（A 持锁目标 6500ms），预期 B 超时抛 BUSY ===");
        OverlapScenarioResult round1 = runOverlapScenario("组二第1轮", 6500L, "g2-r1-A", "g2-r1-B");
        WriterOutcome b1 = round1.bOutcome;
        assertThat(b1.unexpectedError).as("并发组二第 1 轮 B 线程不应有非预期异常").isNull();
        logRound2Evidence("组二第1轮", round1, b1);
        logBusyVerdict(b1);

        WriterOutcome busySampleOutcome = b1;
        boolean busySampleObtained = isBusySample(b1);
        long lastRoundBWaitNanos = b1.waitNanos;
        long lastRoundAHoldNanos = round1.aHoldNanos;
        if (!busySampleObtained && b1.insertSucceeded) {
            log.info("[D3-并发组二] 【R4 分支】第 1 轮未拿到 BUSY（B 等待后成功，等待 {} ms；A 持锁实测 {} ms）"
                    + "→ 加大 A 占用到 8000ms 重跑第 2 轮，两轮结果都落档",
                    toMillisText(b1.waitNanos), toMillisText(round1.aHoldNanos));
            OverlapScenarioResult round2 = runOverlapScenario("组二第2轮", 8000L, "g2-r2-A", "g2-r2-B");
            WriterOutcome b2 = round2.bOutcome;
            assertThat(b2.unexpectedError).as("并发组二第 2 轮 B 线程不应有非预期异常").isNull();
            logRound2Evidence("组二第2轮", round2, b2);
            logBusyVerdict(b2);
            busySampleObtained = isBusySample(b2);
            busySampleOutcome = b2;
            lastRoundBWaitNanos = b2.waitNanos;
            lastRoundAHoldNanos = round2.aHoldNanos;
            long b2Rows = countConcRows("g2-r2-B");
            assertThat(b2Rows).as("并发组二第 2 轮机械一致性：B 行落库数应与 B 写入结果一致")
                    .isEqualTo(b2.insertSucceeded ? 1L : 0L);
        }
        if (busySampleObtained) {
            log.info("[D3-并发组二] 【BUSY 样本落档】异常类型名 = {}；消息原文 = {}；SQLite 结果码 = {}；"
                    + "SQLState = {}；vendorErrorCode = {}；B 等待实测 {} ms（对照 busy_timeout=5000ms）；"
                    + "A 持锁实测 {} ms（须 > 5000ms）",
                    busySampleOutcome.exceptionTypeName, busySampleOutcome.exceptionMessage,
                    busySampleOutcome.resultCodeName, busySampleOutcome.sqlState,
                    busySampleOutcome.vendorErrorCode, toMillisText(lastRoundBWaitNanos),
                    toMillisText(lastRoundAHoldNanos));
        }
        assertThat(busySampleObtained)
                .as("并发组二应拿到 BUSY 异常样本（类型名与消息原文；未拿到时核查两轮 A 持锁与 B 等待实测值，按 R4 处置）")
                .isTrue();
    }

    /** 并发组二单轮证据行：A 持锁实测、B 成败与等待实测（R4 观察点的两个计时数字）。 */
    private void logRound2Evidence(String label, OverlapScenarioResult round, WriterOutcome b) {
        log.info("[D3-并发组二] {} 实测：A 持锁 {} ms；B 插入成功 = {}；B 从发起 INSERT 到返回/异常实测 {} ms",
                label, toMillisText(round.aHoldNanos), b.insertSucceeded, toMillisText(b.waitNanos));
    }

    /** BUSY 判定材料落档：写入失败 + 结果码/消息/类型名任一含 BUSY（判定依据逐项打印，不黑箱）。 */
    private void logBusyVerdict(WriterOutcome b) {
        log.info("[D3-并发组二] BUSY 判定材料：写入失败 = {}；异常类型 = {}；SQLite 结果码 = {}；"
                + "消息原文含 BUSY = {}；类型名含 BUSY = {}",
                !b.insertSucceeded, b.exceptionTypeName, b.resultCodeName,
                b.exceptionMessage.contains("BUSY"), b.exceptionTypeName.contains("BUSY"));
    }

    /** BUSY 样本判定：B 写入失败，且结果码名 / 消息原文 / 异常类型名任一含 BUSY。 */
    private boolean isBusySample(WriterOutcome b) {
        return !b.insertSucceeded
                && (b.resultCodeName.startsWith("SQLITE_BUSY")
                        || b.exceptionMessage.contains("BUSY")
                        || b.exceptionTypeName.contains("BUSY"));
    }

    /**
     * 交叠写场景统一编排（并发两组共用）：A 连接先做 journal_mode=WAL 前提核验与建表守门（自动提交态），
     * 再关自动提交、INSERT 占住写锁；同窗口启动 B 线程发起竞争 INSERT；A 按 System.nanoTime 口径持锁至
     * 目标时长后提交，join B 后回传双方实测值。事务体内只做写库与计时等待（001-spec.md - 2.3 行为规约末段
     * 短事务纪律）。
     */
    private OverlapScenarioResult runOverlapScenario(String label, long holdMillis, String aTag, String bTag)
            throws Exception {
        log.info("[D3-{}] 场景开始：A 关自动提交并 INSERT 占写锁，持锁目标 {} ms（busy_timeout=5000ms），"
                + "同窗口 B 发起竞争 INSERT", label, holdMillis);
        WriterOutcome bOutcome = new WriterOutcome();
        long aHoldNanos;
        try (Connection occupier = DriverManager.getConnection(D3_CONC_JDBC_URL)) {
            String journalMode = readPragma(occupier, "journal_mode");
            log.info("[D3-{}] A 连接 PRAGMA journal_mode = {}（场景前提：WAL 单写者）", label, journalMode);
            assertThat(journalMode).as("%s 场景前提 journal_mode 应为 wal（连接串三参数，D2 已验证写法）", label)
                    .isEqualTo("wal");
            createConcTableIfAbsent(occupier);
            occupier.setAutoCommit(false);
            try (Statement st = occupier.createStatement()) {
                st.executeUpdate("INSERT INTO " + CONC_TABLE + "(tag) VALUES ('" + aTag + "')");
            }
            log.info("[D3-{}] A 已关自动提交并执行 INSERT（tag = {}），写锁持有开始", label, aTag);
            long aHoldStart = System.nanoTime();
            Thread contender = startContendingInsert(label, bTag, bOutcome);
            sleepForRemainingHold(aHoldStart, holdMillis);
            aHoldNanos = System.nanoTime() - aHoldStart;
            occupier.commit();
            log.info("[D3-{}] A 提交完成：持锁实测 {} ms（目标 {} ms）", label, toMillisText(aHoldNanos), holdMillis);
            contender.join(60_000L);
            assertThat(contender.isAlive()).as("%s：B 线程应在 60s 内结束", label).isFalse();
        }
        return new OverlapScenarioResult(aHoldNanos, bOutcome);
    }

    /**
     * 竞争写入方（B）线程：独立连接直连 d3-conc.db，先回读 busy_timeout（R3 观察点），再对 INSERT 前后
     * System.nanoTime 计时——等待时长即锁等待面（成功与异常两路都实测）。结果写入调用方的 outcome 载体，
     * 主线程 join 后读取（join 建立 happens-before，无需同步原语）。
     */
    private Thread startContendingInsert(String label, String tag, WriterOutcome outcome) {
        Thread contender = new Thread(() -> {
            try (Connection contending = DriverManager.getConnection(D3_CONC_JDBC_URL)) {
                outcome.busyTimeoutReadback = readPragma(contending, "busy_timeout");
                log.info("[D3-{}] B 连接已建立，PRAGMA busy_timeout 回读 = {}（连接串同款三参数应生效为 5000）",
                        label, outcome.busyTimeoutReadback);
                long start = System.nanoTime();
                try (Statement st = contending.createStatement()) {
                    st.executeUpdate("INSERT INTO " + CONC_TABLE + "(tag) VALUES ('" + tag + "')");
                    outcome.insertSucceeded = true;
                } catch (SQLException writeError) {
                    outcome.insertSucceeded = false;
                    outcome.exceptionTypeName = writeError.getClass().getName();
                    outcome.exceptionMessage = writeError.getMessage();
                    outcome.sqlState = writeError.getSQLState();
                    outcome.vendorErrorCode = String.valueOf(writeError.getErrorCode());
                    if (writeError instanceof org.sqlite.SQLiteException sqliteError) {
                        outcome.resultCodeName = sqliteError.getResultCode().name();
                    }
                } finally {
                    outcome.waitNanos = System.nanoTime() - start;
                }
                if (outcome.insertSucceeded) {
                    log.info("[D3-{}] B 写入成功：从发起 INSERT 到返回实测 {} ms（该时长即 B 的锁等待面）",
                            label, toMillisText(outcome.waitNanos));
                } else {
                    log.info("[D3-{}] B 写入抛异常：类型 = {}，消息原文 = {}，SQLState = {}，vendorErrorCode = {}，"
                            + "SQLite 结果码 = {}，从发起 INSERT 到异常实测 {} ms",
                            label, outcome.exceptionTypeName, outcome.exceptionMessage, outcome.sqlState,
                            outcome.vendorErrorCode, outcome.resultCodeName, toMillisText(outcome.waitNanos));
                }
            } catch (Exception threadError) {
                outcome.unexpectedError = threadError.getClass().getName() + ": " + threadError.getMessage();
                log.error("[D3-{}] B 线程出现非预期异常（实验程序缺陷嫌疑）：{}", label, outcome.unexpectedError,
                        threadError);
            }
        }, "d3-contending-writer-" + tag);
        contender.start();
        return contender;
    }

    /** A 持锁等待出口：从持锁起点补足到目标持锁时长（System.nanoTime 口径；实测值以打印留档为准，R4 据实判）。 */
    private void sleepForRemainingHold(long aHoldStartNanos, long holdMillis) throws InterruptedException {
        long targetNanos = holdMillis * 1_000_000L;
        long remainingNanos = targetNanos - (System.nanoTime() - aHoldStartNanos);
        if (remainingNanos > 0) {
            Thread.sleep(remainingNanos / 1_000_000L, (int) (remainingNanos % 1_000_000L));
        }
    }

    /** d3-conc.db 探针表守门：已存在则跳过（建表在 A 占用事务之前的自动提交态完成，不参与锁占用）。 */
    private void createConcTableIfAbsent(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + CONC_TABLE
                    + " (id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL)");
        }
    }

    /** 按标签查 d3_conc_probe 行数（机械一致性核验：B 行落库数必须与 B 写入结果一致）。 */
    private long countConcRows(String tag) throws SQLException {
        try (Connection conn = DriverManager.getConnection(D3_CONC_JDBC_URL);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM " + CONC_TABLE + " WHERE tag = '" + tag + "'")) {
            assertThat(rs.next()).as("COUNT(*) 查询应返回一行").isTrue();
            return rs.getLong(1);
        }
    }

    /** PRAGMA 回读统一出口：执行 "PRAGMA &lt;名&gt;"，取第一行第一列的字符串原值（B 线程内不做断言，断言由主线程完成）。 */
    private String readPragma(Connection conn, String pragma) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA " + pragma)) {
            return rs.next() ? rs.getString(1) : "（无返回行）";
        }
    }

    /** 纳秒转毫秒文本（三位小数）：计时一律 System.nanoTime 实测后的统一打印出口。 */
    private String toMillisText(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }

    /** 一次交叠场景的结果载体：A 持锁实测值 + B 竞争写入结果（主线程 join 后读取，无并发可见性问题）。 */
    private static final class OverlapScenarioResult {

        private final long aHoldNanos;

        private final WriterOutcome bOutcome;

        private OverlapScenarioResult(long aHoldNanos, WriterOutcome bOutcome) {
            this.aHoldNanos = aHoldNanos;
            this.bOutcome = bOutcome;
        }
    }

    /**
     * 竞争写入方（B）的一次尝试结果（跨线程可变载体）。exceptionTypeName / exceptionMessage / resultCodeName
     * 三件即并发组二的"BUSY 异常类型名与消息原文样本"取证字段。
     */
    private static final class WriterOutcome {

        private boolean insertSucceeded;

        /** 从发起 INSERT 到返回/异常的实测时长（System.nanoTime；成功与异常两路都在 finally 落值）。 */
        private long waitNanos = -1L;

        private String busyTimeoutReadback = "（未回读）";

        private String exceptionTypeName = "（无异常）";

        private String exceptionMessage = "（无异常）";

        private String sqlState = "（无异常）";

        private String vendorErrorCode = "（无异常）";

        private String resultCodeName = "（非 org.sqlite.SQLiteException，无结果码）";

        /** 非预期异常（实验程序缺陷嫌疑）时置值；正常路径保持 null，主线程断言其恒 null。 */
        private String unexpectedError;
    }
}
