package spike.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import spike.sqlite.config.D1V1Config;
import spike.sqlite.config.D1V2Config;
import spike.sqlite.entity.d1v1.SampleRowV1;
import spike.sqlite.entity.d1v1.SampleRowV1Repository;
import spike.sqlite.entity.d1v2.SampleRowV2;
import spike.sqlite.entity.d1v2.SampleRowV2Repository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D1：ddl-auto=update 对既有表的真实行为（001-plan.md - 3 任务明细 T4；001-spec.md - 2.1 条款 D1（P0）：
 * ddl-auto=update 对既有表的真实行为）。
 *
 * <p>三步与通过判据的对应（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）D1 行方法清单逐字落实）：
 * 步骤① V1 基线（判据 1 前半：基线快照落档）——起上下文 A（D1V1Config + URL 指向 d1.db），插入 2 行样本，
 * 打印 PRAGMA table_info(sample_row) 快照与 sqlite_master 建表语句，关闭上下文 A；步骤② V2 演进（判据 1 后半
 * + 判据 2 + 判据 4：演进快照可逐列对照、样本行行数不变对照值留档、新列在旧行上的取值如实记录、结论行明确写
 * (a) 或 (b)）——起上下文 B（D1V2Config，同 yaml 同文件），打印 V2 阶段快照，查 V1 样本行行数与 remark 列在
 * 旧行上的取值；步骤③ V2 首建对照（判据 3）——换全新文件 d1-fresh.db 直接用 V2 首建，打印快照（预期 V2 全部
 * 列，如实记录）。
 *
 * <p>结论只允许两态、无第三种模糊态（001-spec.md - 2.1 通过判据 4）：(a) 新列自动出现且旧数据保留；
 * (b) 表结构不变（现象原文照录：启动静默跳过，或报错——报错时记录异常类型与消息原文）。V2 上下文启动报错
 * 属 (b) 分支，禁止改实体或配置救场后重测（001-plan.md - 5 分支与风险（每步失败观察点与下一步）R5）。
 *
 * <p>装配：D1V1Config / D1V2Config 各自 @EntityScan / @EnableJpaRepositories 只圈本侧包（同表名 sample_row
 * 的两组实体绝不能进同一 Spring 上下文，001-plan.md - 2.2 目录结构 骨架设计说明 2）；SpringApplicationBuilder
 * 程序化起停、上下文用后即 close；同一 application.yaml 照常加载（001-plan.md - 2.3 application.yaml（T0-3））。
 */
@TestMethodOrder(OrderAnnotation.class)
class D1SchemaEvolutionTest {

    private static final Logger log = LoggerFactory.getLogger(D1SchemaEvolutionTest.class);

    /** 本类库文件的共同目录（001-plan.md - 2.2 目录结构 骨架设计说明 3：统一放 target/spike-data/）。 */
    private static final Path DATA_DIR = Path.of("target", "spike-data");

    /**
     * V1/V2 两上下文共用的演进库文件连接串（001-plan.md - 2.4 测试类清单 D1 行：V1/V2 共用 d1.db）。
     * 参数段与 application.yaml 原样保留（生产同款装配；001-plan.md - 2.3 application.yaml（T0-3））。
     */
    static final String D1_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d1.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL";

    /** 全新文件首建对照连接串（001-plan.md - 2.4 测试类清单 D1 行：全新文件 d1-fresh.db 做首建对照）。 */
    static final String D1_FRESH_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d1-fresh.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL";

    /** 步骤①插入的 2 行样本（id 与对照值），步骤②逐行核对行数与取值不变（判据 2）。 */
    private static final List<V1SampleRow> baselineRows = new ArrayList<>();

    /** 演进路径结论行："(a) …" 或 "(b) …"，步骤②裁决、步骤③终局复述（判据 4）。 */
    private static String evolutionConclusion;

    /**
     * 统一清理重建本类两个库文件（含 -wal/-shm 伴生），保证"V1 首建"与"V2 全新文件首建"语义可复现
     * （001-plan.md - 2.2 目录结构 骨架设计说明 3）。此刻各 Spring 上下文尚未启动，删除不与任何存活连接冲突。
     */
    @BeforeAll
    static void cleanSpikeDataFiles() throws Exception {
        Files.createDirectories(DATA_DIR);
        for (String fileName : List.of("d1.db", "d1-fresh.db")) {
            for (String suffix : List.of("", "-wal", "-shm")) {
                Files.deleteIfExists(DATA_DIR.resolve(fileName + suffix));
            }
        }
        log.info("[D1-前置] 已清理重建 {}（d1.db 与 d1-fresh.db 及各自 -wal/-shm）", DATA_DIR);
    }

    /**
     * 步骤①（判据 1 前半）：起上下文 A（D1V1Config + d1.db），插入 2 行样本并留档对照值，打印
     * PRAGMA table_info(sample_row) 快照与 sqlite_master 建表语句，随后关闭上下文 A（上下文用后即 close）。
     */
    @Test
    @Order(1)
    void step1_baselineV1_createAndInsert() throws SQLException {
        log.info("[D1-V1基线] 起上下文 A（D1V1Config），目标库文件 = d1.db");
        try (ConfigurableApplicationContext context =
                startContext(D1V1Config.class, D1_JDBC_URL, "[D1-V1基线]")) {
            SampleRowV1Repository repository = context.getBean(SampleRowV1Repository.class);

            // 插入 2 行样本（对照值留档，供步骤②核对行数与取值不变——判据 2）
            insertBaselineSample(repository, "d1-基线样本-1", 11L);
            insertBaselineSample(repository, "d1-基线样本-2", 22L);
            assertThat(baselineRows).as("步骤①应插入 2 行样本（001-plan.md - 2.4 测试类清单 D1 行步骤①）").hasSize(2);

            try (Connection conn = DriverManager.getConnection(D1_JDBC_URL)) {
                Map<String, String> columns = logTableInfo(conn, "sample_row", "[D1-V1基线]");
                logCreateSql(conn, "sample_row", "[D1-V1基线]");
                assertThat(columns).as("V1 基线快照应含 V1 全部三列").containsKeys("id", "name", "quantity");
                assertThat(columns).as("V1 基线快照不应含 remark 列（实验前提：V1 实体只映射三列）")
                        .doesNotContainKey("remark");
            }
        }
        log.info("[D1-V1基线] 上下文 A 已关闭（演进库文件 d1.db 上 sample_row 为 V1 三列形态 + 2 行样本）");
    }

    /**
     * 步骤②（判据 1 后半 + 判据 2 + 判据 4）：起上下文 B（D1V2Config，同 yaml 同文件 d1.db），打印 V2 阶段
     * PRAGMA table_info 快照，核对 V1 样本行行数与取值不变、新列在旧行上的取值如实记录；结论行裁决为 (a) 或 (b)。
     * V2 上下文启动报错属 (b) 分支：记录异常类型与消息原文后照常取证（裸 JDBC 不依赖上下文），不改实体救场
     * （001-plan.md - 5 分支与风险（每步失败观察点与下一步）R5）。
     */
    @Test
    @Order(2)
    void step2_evolveV2_sameFile() throws SQLException {
        log.info("[D1-V2演进] 起上下文 B（D1V2Config），同 yaml 同库文件 = d1.db");
        ConfigurableApplicationContext context = null;
        Throwable startupFailure = null;
        try {
            context = startContext(D1V2Config.class, D1_JDBC_URL, "[D1-V2演进]");
        } catch (Throwable t) {
            startupFailure = t;
            logExceptionChain("[D1-V2演进] V2 上下文启动报错（R5：(b) 分支，禁止改实体或配置救场后重测）", t);
        }

        try {
            boolean remarkPresent = false;
            try (Connection conn = DriverManager.getConnection(D1_JDBC_URL)) {
                Map<String, String> columns = logTableInfo(conn, "sample_row", "[D1-V2演进]");
                remarkPresent = columns.containsKey("remark");

                long rowCount = readRowCount(conn, "[D1-V2演进]");
                log.info("[D1-V2演进] V1 样本行行数核对：实测 = {}，步骤①插入 = {}（判据 2：行数不变）",
                        rowCount, baselineRows.size());
                assertThat(rowCount).as("V1 样本行在 V2 阶段行数应不变（判据 2）").isEqualTo(baselineRows.size());
                readAndCompareOldRows(conn, remarkPresent);
            }

            // 生产同款查询路径（V2 仓库 findAll）的结果如实记录：加列成功则应读回旧行；未加列则本查询
            // 大概率报错（SELECT 带 remark 列）——报错原文属 (b) 现象的一部分，照录不掩盖。
            Throwable jpaReadFailure = null;
            if (context != null) {
                SampleRowV2Repository repository = context.getBean(SampleRowV2Repository.class);
                try {
                    List<SampleRowV2> rows = repository.findAll();
                    log.info("[D1-V2演进] V2 仓库 findAll 读回 {} 行（生产同款查询路径可用）", rows.size());
                    for (SampleRowV2 row : rows) {
                        log.info("[D1-V2演进] JPA 读回行：id = {}，name = {}，quantity = {}，remark = {}",
                                row.getId(), row.getName(), row.getQuantity(), row.getRemark());
                    }
                } catch (RuntimeException e) {
                    jpaReadFailure = e;
                    logExceptionChain("[D1-V2演进] V2 仓库 findAll 读回报错（如实记录）", e);
                }
            } else {
                log.info("[D1-V2演进] V2 上下文未启动成功，生产同款查询路径（JPA 仓库）本次不可用，属 (b) 现象之一");
            }

            evolutionConclusion = resolveConclusion(startupFailure, remarkPresent, jpaReadFailure);
            log.info("[D1-结论] 演进路径结论行：{}", evolutionConclusion);
        } finally {
            if (context != null) {
                context.close();
                log.info("[D1-V2演进] 上下文 B 已关闭");
            }
        }
    }

    /**
     * 步骤③（判据 3）：换全新文件 d1-fresh.db 直接用 V2 首建，打印 PRAGMA table_info 快照与建表语句
     * （预期 V2 全部列，如实记录），复述 D1 终局结论。首建与演进两条路径分开下结论
     * （001-spec.md - 2.1 条款 D1（P0）：ddl-auto=update 对既有表的真实行为）。
     */
    @Test
    @Order(3)
    void step3_freshFile_firstBuildV2() throws SQLException {
        log.info("[D1-V2首建] 起上下文 C（D1V2Config），全新库文件 = d1-fresh.db");
        try (ConfigurableApplicationContext context =
                startContext(D1V2Config.class, D1_FRESH_JDBC_URL, "[D1-V2首建]")) {
            try (Connection conn = DriverManager.getConnection(D1_FRESH_JDBC_URL)) {
                Map<String, String> columns = logTableInfo(conn, "sample_row", "[D1-V2首建]");
                logCreateSql(conn, "sample_row", "[D1-V2首建]");
                assertThat(columns)
                        .as("全新库文件首建快照应显示 V2 全部列（判据 3；不符则按实测照录并定位）")
                        .containsKeys("id", "name", "quantity", "remark");
            }
        }
        log.info("[D1-V2首建] 上下文 C 已关闭");

        assertThat(evolutionConclusion)
                .as("D1 结论行必须明确写 (a) 或 (b)，无第三种模糊态（判据 4）")
                .matches("^\\((a|b)\\) .*");
        log.info("[D1-结论] ==================== D1 终局结论 ====================");
        log.info("[D1-结论] 演进路径（同文件 V1→V2）：{}", evolutionConclusion);
        log.info("[D1-结论] 首建路径（全新文件直接 V2）：建表正常，PRAGMA table_info 含 V2 全部列（id/name/quantity/remark）");
        log.info("[D1-结论] 三份快照（V1 基线 / V2 演进 / V2 首建）均已落档 logs/d1-schema-evolution.txt，可逐列对照");
        log.info("[D1-结论] ===================================================");
    }

    /** 插入一行 V1 样本并留档对照值（id 由 IDENTITY 回填）。 */
    private static void insertBaselineSample(SampleRowV1Repository repository, String name, long quantity) {
        SampleRowV1 row = new SampleRowV1();
        row.setName(name);
        row.setQuantity(quantity);
        SampleRowV1 saved = repository.save(row);
        baselineRows.add(new V1SampleRow(saved.getId(), saved.getName(), saved.getQuantity()));
        log.info("[D1-V1基线] 插入样本行：id = {}，name = {}，quantity = {}", saved.getId(), saved.getName(),
                saved.getQuantity());
    }

    /**
     * 程序化起上下文：SpringApplicationBuilder 起停 + URL 程序化覆盖。
     *
     * <p>接线说明（与 001-plan.md - 2.3 application.yaml（T0-3）"用 SpringApplicationBuilder.properties(...)
     * 覆盖 URL"的一处实现细节偏差，如实登记）：builder 的 properties(...) 落在 Boot 外部化配置次序的
     * defaultProperties（最低位），会被 application.yaml 的 spring.datasource.url 压制——yaml 写的是
     * d2.db，覆盖不生效会导致 V1/V2 串写 D2 的库文件。本实现改用 run 的命令行参数形态
     * （"--spring.datasource.url=…"，次序高于配置文件），覆盖语义不变；生效 URL 在运行时硬断言 +
     * 逐上下文落日志，杜绝静默串库。
     */
    private static ConfigurableApplicationContext startContext(Class<?> configClass, String jdbcUrl, String logTag) {
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(configClass).run("--spring.datasource.url=" + jdbcUrl);
        String effectiveUrl = context.getEnvironment().getProperty("spring.datasource.url");
        log.info("{} 程序化启动完成，生效 spring.datasource.url = {}", logTag, effectiveUrl);
        try {
            assertThat(effectiveUrl)
                    .as("%s 生效 URL 应为程序化覆盖值（否则 yaml 的 d2.db 串库）", logTag)
                    .isEqualTo(jdbcUrl);
        } catch (RuntimeException e) {
            context.close();
            throw e;
        }
        return context;
    }

    /** 打印一张表的 PRAGMA table_info 逐列快照并返回 name→声明类型 汇总（判据 1：快照可逐列对照）。 */
    private static Map<String, String> logTableInfo(Connection conn, String table, String logTag) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                columns.put(name, rs.getString("type"));
                log.info("{} table_info 快照第 {} 列：name = {}，声明类型 = {}，notnull = {}，dflt_value = {}，pk = {}",
                        logTag, rs.getInt("cid"), name, rs.getString("type"), rs.getInt("notnull"),
                        rs.getString("dflt_value"), rs.getInt("pk"));
            }
        }
        log.info("{} {} 表快照汇总（name→声明类型）= {}", logTag, table, columns);
        return columns;
    }

    /** 打印 sqlite_master 中的建表语句原文（快照的对照面）。 */
    private static void logCreateSql(Connection conn, String table, String logTag) throws SQLException {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT sql FROM sqlite_master WHERE name = '" + table + "'")) {
            assertThat(rs.next()).as("sqlite_master 应能检索到 %s 的建表语句", table).isTrue();
            log.info("{} 建表语句原文 = {}", logTag, rs.getString(1));
        }
    }

    /** 读全表行数并落日志。 */
    private static long readRowCount(Connection conn, String logTag) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sample_row")) {
            assertThat(rs.next()).as("COUNT(*) 查询应返回一行").isTrue();
            long count = rs.getLong(1);
            log.info("{} SELECT COUNT(*) FROM sample_row = {}", logTag, count);
            return count;
        }
    }

    /**
     * 读旧行全值并与步骤①留档的对照值逐行核对（判据 2：行数不变 + 对照值留档；新列 remark 在旧行上的
     * 取值如实记录，通常为 null——照录不断言非空）。
     */
    private static void readAndCompareOldRows(Connection conn, boolean remarkPresent) throws SQLException {
        String selectSql = remarkPresent
                ? "SELECT id, name, quantity, remark FROM sample_row ORDER BY id"
                : "SELECT id, name, quantity FROM sample_row ORDER BY id";
        log.info("[D1-V2演进] 旧行取值核对 SQL = {}", selectSql);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(selectSql)) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                Long quantity = rs.getObject("quantity") == null ? null : rs.getLong("quantity");
                String remark = remarkPresent ? rs.getString("remark") : null;
                V1SampleRow expected = baselineRows.stream()
                        .filter(row -> row.id() == id)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("步骤②出现步骤①未插入的行 id=" + id + "（数据面异常，需定位）"));
                log.info("[D1-V2演进] 旧行对照：id = {}，name = {}（对照 {}），quantity = {}（对照 {}），remark = {}（如实记录）",
                        id, name, expected.name(), quantity, expected.quantity(), remark);
                assertThat(name).as("旧行 id=%s name 应与步骤①对照值一致", id).isEqualTo(expected.name());
                assertThat(quantity).as("旧行 id=%s quantity 应与步骤①对照值一致", id).isEqualTo(expected.quantity());
            }
        }
    }

    /**
     * 结论裁决（判据 4：只允许两态，无第三种模糊态）：(a) 当且仅当 V2 上下文启动成功且表结构出现 remark 列
     * （旧行数与取值不变已在前置断言核过）；其余一切情形归 (b)，现象原文（静默跳过 / 启动报错 / JPA 读回报错）
     * 全部照录进结论行。
     */
    private static String resolveConclusion(Throwable startupFailure, boolean remarkPresent, Throwable jpaReadFailure) {
        if (startupFailure == null && remarkPresent) {
            return "(a) 新列自动出现（ALTER TABLE ADD COLUMN 发生）且旧数据保留（行数与对照值逐行核过）";
        }
        StringBuilder phenomenon = new StringBuilder();
        if (startupFailure != null) {
            Throwable root = rootCause(startupFailure);
            phenomenon.append("V2 上下文启动报错，根因异常类型 ").append(root.getClass().getName())
                    .append("，消息原文「").append(root.getMessage()).append("」");
        } else {
            phenomenon.append("V2 上下文启动成功，但 PRAGMA table_info(sample_row) 快照无 remark 列（启动静默跳过加列）");
        }
        if (jpaReadFailure != null) {
            Throwable root = rootCause(jpaReadFailure);
            phenomenon.append("；V2 仓库 findAll 读回报错，根因异常类型 ").append(root.getClass().getName())
                    .append("，消息原文「").append(root.getMessage()).append("」");
        }
        return "(b) 表结构不变（现象原文：" + phenomenon + "）";
    }

    /** 异常 cause 链逐层落日志（R5：报错时记录异常类型与消息原文）。 */
    private static void logExceptionChain(String header, Throwable failure) {
        Throwable current = failure;
        int depth = 0;
        while (current != null && depth < 10) {
            log.info("{} 第 {} 层：{}：{}", header, depth, current.getClass().getName(),
                    String.valueOf(current.getMessage()));
            current = current.getCause();
            depth++;
        }
    }

    /** 取 cause 链根因（结论行 phenomenon 引用其类型名与消息原文）。 */
    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /** 步骤①样本行的对照值载体（id + name + quantity）。 */
    private record V1SampleRow(long id, String name, Long quantity) {
    }
}
