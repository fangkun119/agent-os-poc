package spike.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D2：WAL 三件套 pragma 经连接串的生效性（001-plan.md - 3 任务明细 T1；001-spec.md - 2.2 条款 D2（P0）：
 * WAL 三件套 pragma 经连接串的生效性）。
 *
 * <p>五个方法与通过判据的对应（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）方法清单逐字落实）：
 * 方法① 正向组三回读（判据 1：wal / 5000 / 2）；方法② 伴生文件取证（判据 3）；方法③④ 负向组缺省值
 * （判据 2：journal_mode 用全新库文件、busy_timeout 与 synchronous 用同文件无参重开）；方法⑤ 拼错探针
 * （判据 4）。判据 5 为分支条款：任一参数不生效时按 001-plan.md - 5 分支与风险（每步失败观察点与下一步）
 * R1 换 SQLiteConfig 备选路径复测、两路结论都落档。
 *
 * <p>正向组装配 = 001-plan.md - 2.3 application.yaml（T0-3）原样：单一入口约束（agentos/CLAUDE.md -
 * 3 存储与事务（SQLite/JPA）），DataSource 经 HikariCP 从 spring.datasource.url 三参数创建，
 * 不自建 DataSource Bean。判读手段 = PRAGMA 回读。
 */
@SpringBootTest(classes = D2PragmaEffectivenessTest.D2Context.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.data.jpa.repositories.enabled=false")
@TestMethodOrder(OrderAnnotation.class)
class D2PragmaEffectivenessTest {

    private static final Logger log = LoggerFactory.getLogger(D2PragmaEffectivenessTest.class);

    /** 本类三个库文件的共同目录（001-plan.md - 2.2 目录结构 骨架设计说明 3：统一放 target/spike-data/）。 */
    private static final Path DATA_DIR = Path.of("target", "spike-data");

    /** SQLite journal_mode 的合法枚举（PRAGMA journal_mode 回读的合法性兜底断言用）。 */
    private static final List<String> JOURNAL_MODES = List.of("delete", "truncate", "persist", "memory", "wal", "off");

    @Autowired
    private DataSource dataSource;

    /** 正向组装配 URL 留档用（= application.yaml 的 spring.datasource.url 原值）。 */
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    /**
     * D2 实验上下文（001-plan.md - 2.2 目录结构 骨架设计说明 2：内嵌 @SpringBootConfiguration + 显式圈定）。
     *
     * <p>D2 无自己的实体与仓库：@EntityScan / @EnableJpaRepositories 圈定专用空包 spike.sqlite.entity.d2，
     * 阻断默认包扫描把其他实验（d1v1/d1v2/d3/d4）的实体与仓库带进本上下文——尤其 d1v1/d1v2 两组实体同表名
     * sample_row 绝不能同上下文共存。spring.data.jpa.repositories.enabled=false 同理阻断 Spring Data
     * 按配置类主包（spike.sqlite）自动扫仓库（那些仓库的领域实体不在本上下文持久化单元，启动会炸出误导性失败）。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("spike.sqlite.entity.d2")
    @EnableJpaRepositories("spike.sqlite.entity.d2")
    static class D2Context {
    }

    /**
     * 统一清理重建本类三个库文件（含 -wal/-shm 伴生），保证"全新库文件"（从未被任何进程设过 WAL）语义可复现
     * （001-plan.md - 3 任务明细 T1 执行注记 ②；001-spec.md - 0.2 术语与文件指代（沿用 req，只补本文新增））。
     * 此刻 Spring 上下文尚未启动，删除不与任何存活连接冲突。
     */
    @BeforeAll
    static void cleanSpikeDataFiles() throws Exception {
        Files.createDirectories(DATA_DIR);
        for (String dbFile : List.of("d2.db", "d2-fresh.db", "d2-typo.db")) {
            Files.deleteIfExists(DATA_DIR.resolve(dbFile));
            Files.deleteIfExists(DATA_DIR.resolve(dbFile + "-wal"));
            Files.deleteIfExists(DATA_DIR.resolve(dbFile + "-shm"));
        }
        log.info("[D2-前置] 已清理重建 {}（d2.db / d2-fresh.db / d2-typo.db 及各自 -wal/-shm）", DATA_DIR);
    }

    /**
     * 方法①（判据 1）：起 Spring 上下文（application.yaml 原样），从 DataSource（HikariCP 连接池）取连接，
     * 依次执行并打印 PRAGMA journal_mode / busy_timeout / synchronous，断言 wal / 5000 / 2
     * （2 = FULL 的官方数值）。回读值先落日志再断言，任一不符时证据已留档（R1 观察点）。
     */
    @Test
    @Order(1)
    void positiveGroup_readbackThreePragmas() throws SQLException {
        log.info("[D2-正向组] spring.datasource.url = {}", datasourceUrl);
        try (Connection conn = dataSource.getConnection()) {
            String journalMode = readStringPragma(conn, "journal_mode");
            String busyTimeout = readStringPragma(conn, "busy_timeout");
            String synchronous = readStringPragma(conn, "synchronous");
            log.info("[D2-正向组] PRAGMA journal_mode = {}（预期 wal）", journalMode);
            log.info("[D2-正向组] PRAGMA busy_timeout = {}（预期 5000）", busyTimeout);
            log.info("[D2-正向组] PRAGMA synchronous = {}（预期 2，FULL 的官方数值）", synchronous);
            assertThat(journalMode).as("PRAGMA journal_mode 回读").isEqualTo("wal");
            assertThat(busyTimeout).as("PRAGMA busy_timeout 回读").isEqualTo("5000");
            assertThat(synchronous).as("PRAGMA synchronous 回读").isEqualTo("2");
        }
    }

    /**
     * 方法②（判据 3）：同一上下文建一张探针表并写入 1 行，在连接存活窗口内列 target/spike-data/ 目录，
     * 打印 d2.db-wal 与 d2.db-shm 存在性。SQLite 干净关闭最后一个连接会收走伴生文件，故取证必须在窗口内
     * （001-plan.md - 3 任务明细 T1 执行注记 ①）。探针表用普通表而非 SQLite TEMP 表：TEMP 表写在临时库、
     * 不产生主库 WAL 流量，取证对象是主库伴生文件。
     */
    @Test
    @Order(2)
    void walCompanionFiles_visibleWhileConnectionOpen() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS d2_probe_table (id INTEGER PRIMARY KEY AUTOINCREMENT, note TEXT)");
                int inserted = st.executeUpdate("INSERT INTO d2_probe_table(note) VALUES ('d2-wal-companion-probe')");
                log.info("[D2-伴生文件] 建探针表 d2_probe_table 并写入 1 行，实测写入行数 = {}", inserted);
                assertThat(inserted).as("探针写入行数").isEqualTo(1);
            }
            try (Stream<Path> entries = Files.list(DATA_DIR)) {
                List<String> names = entries.map(entry -> entry.getFileName().toString()).sorted().toList();
                log.info("[D2-伴生文件] 连接存活窗口内 {} 目录列表 = {}", DATA_DIR, names);
                boolean walExists = names.contains("d2.db-wal");
                boolean shmExists = names.contains("d2.db-shm");
                log.info("[D2-伴生文件] d2.db-wal 存在 = {}，d2.db-shm 存在 = {}", walExists, shmExists);
                assertThat(walExists).as("WAL 伴生文件 d2.db-wal 应在连接存活窗口内出现").isTrue();
                assertThat(shmExists).as("WAL 伴生文件 d2.db-shm 应在连接存活窗口内出现").isTrue();
            }
        }
    }

    /**
     * 方法③（判据 2 前半）：裸 JDBC（DriverManager，不经连接池）无参打开全新文件 d2-fresh.db，回读
     * journal_mode。journal_mode 是写入库文件的持久属性，负向证据必须用从未设过 WAL 的全新文件
     * （001-spec.md - 2.2 条款 D2（P0）：WAL 三件套 pragma 经连接串的生效性）；预期 delete，以实测为准。
     */
    @Test
    @Order(3)
    void negativeJournalMode_freshFileWithoutParams() throws SQLException {
        String freshUrl = "jdbc:sqlite:" + DATA_DIR.resolve("d2-fresh.db");
        try (Connection conn = DriverManager.getConnection(freshUrl)) {
            String journalMode = readStringPragma(conn, "journal_mode");
            log.info("[D2-负向组journal_mode] 裸 JDBC 无参打开全新文件，url = {}", freshUrl);
            log.info("[D2-负向组journal_mode] PRAGMA journal_mode = {}（预期 delete，以实测为准）", journalMode);
            assertThat(journalMode).as("journal_mode 回读应落在合法枚举内").isIn(JOURNAL_MODES);
        }
    }

    /**
     * 方法④（判据 2 后半）：裸 JDBC 无参重开 d2.db（同文件），回读 busy_timeout 与 synchronous 缺省值。
     * 两者是连接级参数（不落文件、随连接存在），同文件无参重开对照成立；缺省值以实测为准、不预设
     * （synchronous 缺省值随驱动构建与日志模式而变）。顺带回读 journal_mode 作佐证：journal_mode 写入
     * 库文件头，同文件无参重开必读 wal，故不作负向证据（规格原文的机制注记，此处实证它）。
     */
    @Test
    @Order(4)
    void negativeBusyTimeoutSync_reopenSameFileWithoutParams() throws SQLException {
        String sameFileUrl = "jdbc:sqlite:" + DATA_DIR.resolve("d2.db");
        try (Connection conn = DriverManager.getConnection(sameFileUrl)) {
            String busyTimeout = readStringPragma(conn, "busy_timeout");
            String synchronous = readStringPragma(conn, "synchronous");
            String journalMode = readStringPragma(conn, "journal_mode");
            log.info("[D2-负向组缺省值] 裸 JDBC 无参重开同文件，url = {}", sameFileUrl);
            log.info("[D2-负向组缺省值] PRAGMA busy_timeout = {}（连接级缺省值实测，不预设）", busyTimeout);
            log.info("[D2-负向组缺省值] PRAGMA synchronous = {}（连接级缺省值实测，不预设）", synchronous);
            log.info("[D2-负向组缺省值] PRAGMA journal_mode = {}（佐证记录：同文件无参重开必读 wal，不作负向证据）", journalMode);
            assertThat(Integer.parseInt(busyTimeout)).as("busy_timeout 缺省值应为非负整数").isGreaterThanOrEqualTo(0);
            assertThat(Integer.parseInt(synchronous)).as("synchronous 缺省值应落在 0-3 官方枚举内").isBetween(0, 3);
            assertThat(journalMode).as("同文件无参重开 journal_mode 应仍读 wal（库文件头持久语义佐证）").isEqualTo("wal");
        }
    }

    /**
     * 方法⑤（判据 4）：URL 写 journalmode=WAL（少一个下划线）打开另一全新文件 d2-typo.db，回读
     * journal_mode，如实记录驱动有无报错或日志。预期拼错参数被静默忽略（回读值保持缺省、非 wal）、
     * 无异常抛出；实测为哪种都照实落档（001-spec.md - 2.2 条款 D2（P0）：WAL 三件套 pragma 经连接串的
     * 生效性 通过判据 4）。驱动自身的 SLF4J 输出与本文件 stdout 同流，其 warn/error 条目有无即"有无日志"的判读依据。
     */
    @Test
    @Order(5)
    void typoProbe_misspelledParamRecorded() throws SQLException {
        String typoUrl = "jdbc:sqlite:" + DATA_DIR.resolve("d2-typo.db") + "?journalmode=WAL";
        boolean driverThrew = false;
        String journalMode = "（连接未建立，无回读值）";
        try (Connection conn = DriverManager.getConnection(typoUrl)) {
            journalMode = readStringPragma(conn, "journal_mode");
        } catch (SQLException driverError) {
            driverThrew = true;
            log.warn("[D2-拼错探针] 驱动对拼错参数抛出异常：类型 = {}，消息原文 = {}",
                    driverError.getClass().getName(), driverError.getMessage());
        }
        log.info("[D2-拼错探针] 裸 JDBC 打开全新文件，URL 拼错参数名 journalmode=WAL（少一个下划线），url = {}", typoUrl);
        log.info("[D2-拼错探针] PRAGMA journal_mode = {}（回读值不变即未生效，预期非 wal）", journalMode);
        log.info("[D2-拼错探针] 驱动行为实录：抛异常 = {}；驱动有无日志以本文件中 org.sqlite 相关输出条目为准（无 warn/error 即坐实静默忽略）", driverThrew);
        if (!driverThrew) {
            assertThat(journalMode).as("拼错参数名后回读值应保持缺省（未切到 wal）").isNotEqualToIgnoringCase("wal");
        }
    }

    /** PRAGMA 回读统一出口：执行 "PRAGMA &lt;名&gt;"，取第一行第一列的字符串原值（整型值原样转字符串留档）。 */
    private String readStringPragma(Connection conn, String pragma) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA " + pragma)) {
            assertThat(rs.next()).as("PRAGMA %s 应返回一行", pragma).isTrue();
            return rs.getString(1);
        }
    }
}
