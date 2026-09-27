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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D5：synchronous=FULL 的提交耗时量级（001-plan.md - 3 任务明细 T5；001-spec.md - 2.5 条款 D5（P1）：
 * synchronous=FULL 的提交耗时量级）。
 *
 * <p>两个方法与通过判据的对应（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）D5 行方法清单逐字落实）：
 * 方法① groupFull_thousandCommits——裸 JDBC 打开 d5-full.db（URL 带 journal_mode=WAL&amp;synchronous=FULL），
 * 先回读 journal_mode 与 synchronous 打印（守门：确认参数就位再计时），建表，10 次预热提交（不计时），
 * System.nanoTime 计时连续 1000 次"开事务 → INSERT 1 行 → 提交"，打印总耗时与单次均值（ms）；
 * 方法② groupNormal_thousandCommits——同流程开 d5-normal.db（仅 synchronous 换 NORMAL），计时后一并打印
 * FULL/NORMAL 倍数（组 1 总耗时 ÷ 组 2 总耗时）。两组同构、只差 synchronous（001-spec.md - 2.5 行为规约：
 * 同机、同库结构、WAL 开、每次提交事务写 1 行、连续 1000 次提交，文件库而非 :memory:）。
 *
 * <p>判据落点（001-spec.md - 2.5 通过判据 1-3）：判据 1 两组总耗时/单次均值/倍数落 logs/（方法② 统一打印）；
 * 判据 2 明确回答单次 FULL 提交均值是否 &lt; 10ms——成立与否都是合法结论（001-spec.md - 0.1 总口径：
 * 符合预期或推翻预期都算完成），不做硬断言，只落结论行；判据 3 数字全部 System.nanoTime 实测，禁估算。
 * 量级换算（README 结论用，公式出自 001-spec.md - 2.5 行为规约第 3 条）一并由实测均值计算落档。
 *
 * <p>实验控制与风险分支：机器空闲控制归 T5-1（执行 agent 无法替用户关闭重 IO 应用时，按
 * 001-plan.md - 5 分支与风险（每步失败观察点与下一步）R7 口径如实记录局限）；数字接近或倍数异常
 * （如 FULL 反而更快）时 R7——回读两组 synchronous 确认 2/1 各就位（两方法守门段已内置）、确认文件库
 * （方法内断言非 :memory:）、排除后重跑一次且两轮日志都留档、结论取实测。
 */
@TestMethodOrder(OrderAnnotation.class)
class D5FsyncTimingTest {

    private static final Logger log = LoggerFactory.getLogger(D5FsyncTimingTest.class);

    /** 本类两个库文件的共同目录（001-plan.md - 2.2 目录结构 骨架设计说明 3：统一放 target/spike-data/）。 */
    private static final Path DATA_DIR = Path.of("target", "spike-data");

    /**
     * 组 1（FULL）连接串：库文件 d5-full.db，journal_mode=WAL 与 busy_timeout=5000 与 D2 正向组同款
     * （D2 实测三参数生效见 logs/d2-pragma.txt），synchronous=FULL。
     */
    static final String D5_FULL_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d5-full.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL";

    /**
     * 组 2（NORMAL）连接串：与组 1 逐字符同构、仅 synchronous 一处换 NORMAL
     * （001-spec.md - 2.5 行为规约：其余条件全同）。
     */
    static final String D5_NORMAL_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d5-normal.db?journal_mode=WAL&busy_timeout=5000&synchronous=NORMAL";

    /** 探针表名（各自组库文件内裸 JDBC 建表；两组同构，表结构一致）。 */
    private static final String PROBE_TABLE = "d5_commit_probe";

    /** 预热提交次数（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）D5 行：10 次预热提交，不计时）。 */
    private static final int WARMUP_COMMITS = 10;

    /** 计时提交次数（同上 D5 行：连续 1000 次）。 */
    private static final int TIMED_COMMITS = 1000;

    /**
     * 组 1（FULL）的总耗时（System.nanoTime 实测值，方法① 落值、方法② 取用算倍数）。
     * JUnit 同线程顺序执行（@Order），静态字段跨方法传递；方法② 先断言其已落值。
     */
    private static long fullGroupTotalNanos = -1L;

    /**
     * 统一清理重建本类两个库文件（含 -wal/-shm 伴生），保证两组各自"全新库文件"语义可复现
     * （001-plan.md - 2.2 目录结构 骨架设计说明 3）；spike 全程无跨类共用 d5 库。
     */
    @BeforeAll
    static void cleanSpikeDataFiles() throws Exception {
        Files.createDirectories(DATA_DIR);
        for (String dbFile : List.of("d5-full.db", "d5-normal.db")) {
            Files.deleteIfExists(DATA_DIR.resolve(dbFile));
            Files.deleteIfExists(DATA_DIR.resolve(dbFile + "-wal"));
            Files.deleteIfExists(DATA_DIR.resolve(dbFile + "-shm"));
        }
        log.info("[D5-前置] 已清理重建 {}（d5-full.db / d5-normal.db 及各自 -wal/-shm）", DATA_DIR);
    }

    /**
     * 方法①（001-spec.md - 2.5 通过判据 1 的 FULL 组数字来源）：裸 JDBC 打开 d5-full.db，守门回读
     * journal_mode（预期 wal）与 synchronous（预期 2，FULL 官方数值）后计时；10 次预热提交不计时，
     * 连续 1000 次"开事务 → INSERT 1 行 → 提交"计时，总耗时落静态字段供方法② 算倍数。
     */
    @Test
    @Order(1)
    void groupFull_thousandCommits() throws Exception {
        log.info("[D5-组FULL] === 组 1：synchronous=FULL，预热 {} 次（不计时）+ 计时 {} 次提交 ===",
                WARMUP_COMMITS, TIMED_COMMITS);
        GroupTiming timing = runThousandCommitGroup("FULL", D5_FULL_JDBC_URL, "2");
        fullGroupTotalNanos = timing.totalNanos();
        logGroupTiming("FULL", timing);
    }

    /**
     * 方法②（001-spec.md - 2.5 通过判据 1 的 NORMAL 组数字来源与判据 2 结论行）：同流程开 d5-normal.db
     * （守门 synchronous 预期 1，NORMAL 官方数值），计时后一并打印 FULL/NORMAL 倍数（组 1 总耗时 ÷
     * 组 2 总耗时）、单次 FULL 均值是否 &lt; 10ms 的明确结论行、21 次提交/轮的量级换算。
     * FULL/NORMAL 倍数 &lt; 1（FULL 反而更快）时按 R7 打异常征兆标记（处置：排除干扰后重跑一次、
     * 两轮日志留档、结论取实测——重跑动作在 mvn 命令层做，不在本测试内自循环）。
     */
    @Test
    @Order(2)
    void groupNormal_thousandCommits() throws Exception {
        assertThat(fullGroupTotalNanos).as("组 1（FULL）应先于组 2 执行并留下实测总耗时（@Order 顺序前提）")
                .isPositive();
        log.info("[D5-组NORMAL] === 组 2：synchronous=NORMAL，其余条件与组 1 全同（同机、同表结构、WAL 开、"
                + "每提交写 1 行、连续 {} 次）===", TIMED_COMMITS);
        GroupTiming normalTiming = runThousandCommitGroup("NORMAL", D5_NORMAL_JDBC_URL, "1");
        logGroupTiming("NORMAL", normalTiming);

        double fullTotalMs = fullGroupTotalNanos / 1_000_000.0;
        double normalTotalMs = normalTiming.totalNanos() / 1_000_000.0;
        double fullMeanMs = fullTotalMs / TIMED_COMMITS;
        double normalMeanMs = normalTotalMs / TIMED_COMMITS;
        double ratio = (double) fullGroupTotalNanos / normalTiming.totalNanos();

        log.info("[D5-结论] 判据 1 两组数字：FULL 总耗时 {} ms（{} 次提交），单次均值 {} ms；"
                        + "NORMAL 总耗时 {} ms（{} 次提交），单次均值 {} ms",
                toMillisText(fullTotalMs), TIMED_COMMITS, toMillisText(fullMeanMs),
                toMillisText(normalTotalMs), TIMED_COMMITS, toMillisText(normalMeanMs));
        log.info("[D5-结论] 判据 1 FULL/NORMAL 倍数（FULL 总耗时 ÷ NORMAL 总耗时）= {}",
                String.format("%.3f", ratio));

        boolean fullUnder10ms = fullMeanMs < 10.0;
        log.info("[D5-结论] 判据 2 明确回答：单次 FULL 提交均值 {} ms，是否 < 10ms → {}（{}）",
                toMillisText(fullMeanMs), fullUnder10ms ? "是" : "否",
                fullUnder10ms ? "成立，维持 TechnicalSolution.md - 9.2 SQLite 关系型数据 现行表述"
                        : "不成立，触发 req 第 7 章联动 3 改写");

        double roundCommitCount = 1 + 2 * 10;
        double roundOverheadMs = roundCommitCount * fullMeanMs;
        log.info("[D5-结论] 量级换算（001-spec.md - 2.5 行为规约第 3 条公式，代入实测均值计算）：一轮 = 1 次消息事务"
                        + " + 每迭代 2 次审计事务，10 次迭代一轮 = {} 次提交事务；单轮提交开销 = {} × {} ms = {} ms，"
                        + "对照 60s 级 LLM 调用量级",
                String.format("%.0f", roundCommitCount), String.format("%.0f", roundCommitCount),
                toMillisText(fullMeanMs), toMillisText(roundOverheadMs));
        log.info("[D5-结论] 判据 3：以上数字全部 System.nanoTime 实测（每组预热 {} 次不计时、计时 {} 次），"
                + "无估算补数；执行日期/机型/磁盘类型与机器负载局限见 logs/d5-fsync-timing.txt 末尾执行环境注记",
                WARMUP_COMMITS, TIMED_COMMITS);

        if (ratio < 1.0) {
            log.warn("[D5-R7] 异常征兆：FULL 总耗时反而更短（倍数 {} < 1.0）——按 001-plan.md - 5 分支与风险"
                    + "（每步失败观察点与下一步）R7：核对两组守门回读（synchronous=2/1）与文件库路径后，"
                    + "机器空闲重跑一次，两轮日志都留档，结论取实测", String.format("%.3f", ratio));
        } else {
            log.info("[D5-R7] 无异常征兆：FULL/NORMAL 倍数 {} ≥ 1.0（FULL 更慢或持平属方向正常），R7 重跑分支未触发",
                    String.format("%.3f", ratio));
        }
    }

    /**
     * 单组 1000 次提交计时（两组共用的同构出口）：裸 JDBC 打开指定 URL → 守门回读 journal_mode 与
     * synchronous（断言预期值，参数未就位直接失败、不产生计时数字）→ 建表（自动提交态）→ 关自动提交后
     * 10 次预热提交（不计时）→ System.nanoTime 计时连续 1000 次"INSERT 1 行 → commit"→ 行数机械一致性
     * 核验（10 + 1000 = 1010 行）。计时口径：nanoTime 只包住 1000 次"INSERT + commit"循环体本身，
     * 建表与预热在计时窗外。每次提交事务写 1 行（001-spec.md - 2.5 行为规约）。
     */
    private GroupTiming runThousandCommitGroup(String label, String jdbcUrl, String expectedSynchronous)
            throws SQLException {
        Path dbFile = DATA_DIR.resolve("d5-" + label.toLowerCase() + ".db");
        assertThat(jdbcUrl)
                .as("R7 干扰排除：组 %s 必须用文件库 %s（001-spec.md - 2.5 行为规约：内存库没有真实 fsync）",
                        label, dbFile)
                .contains(String.valueOf(dbFile))
                .doesNotContain(":memory:");
        log.info("[D5-组{}] 库文件 = {}（文件库已核验，非 :memory:）；连接串 = {}", label, dbFile, jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            String journalMode = readPragma(conn, "journal_mode");
            String synchronous = readPragma(conn, "synchronous");
            log.info("[D5-组{}] 守门回读（计时前）：PRAGMA journal_mode = {}（预期 wal）；"
                    + "PRAGMA synchronous = {}（预期 {}）", label, journalMode, synchronous, expectedSynchronous);
            assertThat(journalMode).as("组 %s 守门：journal_mode 应为 wal（WAL 开，001-spec.md - 2.5）", label)
                    .isEqualTo("wal");
            assertThat(synchronous).as("组 %s 守门：synchronous 应为 %s（参数就位才计时，R7 观察点）", label,
                    expectedSynchronous).isEqualTo(expectedSynchronous);

            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS " + PROBE_TABLE
                        + " (id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL)");
            }

            conn.setAutoCommit(false);
            String warmupInsertSql = "INSERT INTO " + PROBE_TABLE + "(tag) VALUES ('d5-warmup-" + label + "')";
            String timedInsertSql = "INSERT INTO " + PROBE_TABLE + "(tag) VALUES ('d5-timed-" + label + "')";
            long totalNanos;
            try (Statement st = conn.createStatement()) {
                for (int i = 0; i < WARMUP_COMMITS; i++) {
                    st.executeUpdate(warmupInsertSql);
                    conn.commit();
                }
                log.info("[D5-组{}] 预热 {} 次提交完成（不计时），开始计时 {} 次提交", label, WARMUP_COMMITS,
                        TIMED_COMMITS);
                long startNanos = System.nanoTime();
                for (int i = 0; i < TIMED_COMMITS; i++) {
                    st.executeUpdate(timedInsertSql);
                    conn.commit();
                }
                totalNanos = System.nanoTime() - startNanos;
            }
            conn.setAutoCommit(true);

            long rowCount;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM " + PROBE_TABLE)) {
                assertThat(rs.next()).as("组 %s 行数查询应返回一行", label).isTrue();
                rowCount = rs.getLong(1);
            }
            log.info("[D5-组{}] 机械一致性核验：全表行数 = {}（预热 {} + 计时 {} = {}）",
                    label, rowCount, WARMUP_COMMITS, TIMED_COMMITS, WARMUP_COMMITS + TIMED_COMMITS);
            assertThat(rowCount).as("组 %s 全表行数应等于预热 + 计时提交次数", label)
                    .isEqualTo((long) WARMUP_COMMITS + TIMED_COMMITS);

            return new GroupTiming(totalNanos, journalMode, synchronous, rowCount);
        }
    }

    /** 单组计时结果落档行（总耗时/单次均值 ms；守门回读值与行数一并留档）。 */
    private void logGroupTiming(String label, GroupTiming timing) {
        double totalMs = timing.totalNanos() / 1_000_000.0;
        double meanMs = totalMs / TIMED_COMMITS;
        log.info("[D5-组{}] 判据数字：总耗时 {} ms；单次提交均值 {} ms（{} µs）；守门回读 journal_mode = {} / "
                + "synchronous = {}；提交后全表行数 = {}", label, toMillisText(totalMs), toMillisText(meanMs),
                String.format("%.1f", meanMs * 1000.0), timing.journalMode(), timing.synchronous(),
                timing.rowCount());
    }

    /** PRAGMA 回读统一出口：执行 "PRAGMA &lt;名&gt;"，取第一行第一列的字符串原值（与 D3 同款写法）。 */
    private String readPragma(Connection conn, String pragma) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA " + pragma)) {
            return rs.next() ? rs.getString(1) : "（无返回行）";
        }
    }

    /** 毫秒数值转文本（三位小数）：System.nanoTime 实测后的统一打印出口。 */
    private String toMillisText(double millis) {
        return String.format("%.3f", millis);
    }

    /** 单组计时结果：总耗时纳秒 + 守门回读值 + 行数（方法② 算倍数与结论行的全部输入，均为实测值）。 */
    private record GroupTiming(long totalNanos, String journalMode, String synchronous, long rowCount) {
    }
}
