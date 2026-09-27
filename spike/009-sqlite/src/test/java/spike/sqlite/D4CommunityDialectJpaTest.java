package spike.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
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

import spike.sqlite.entity.d4.TypeProbe;
import spike.sqlite.entity.d4.TypeProbeRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D4：社区方言常规 JPA 面够用性（001-plan.md - 3 任务明细 T2；001-spec.md - 2.4 条款 D4（P0）：
 * 社区方言常规 JPA 面够用性）。
 *
 * <p>三个方法与通过判据的对应（001-plan.md - 2.4 测试类清单（5 类 + 1 个命令收口项）D4 行方法清单逐字落实）：
 * 方法① DDL 快照（判据 3：建表语句落档、AUTOINCREMENT 可检索命中；逐列声明类型实录）；方法② CRUD 四步
 * （判据 1：save → findById → 修改再 save → 再查 → delete → 查空，AssertJ 全程断言，update 路径同步刷新
 * update_time）；方法③ 类型往返（判据 2：Boolean/long/String/LocalDateTime 边界样本逐字段比对；判据 4：
 * 裸连接 SELECT 原始值打印实际存储形态——文本串还是整数）。判据 5 为条件条款：若有坑，坑清单与三级归类
 * （阻断 / 有绕法 / 无碍）随实测结果如实登记，失败现象不掩盖、不改判据（001-plan.md - 5 分支与风险
 * （每步失败观察点与下一步）R6：DDL 检索不到 AUTOINCREMENT 时照实登记为坑 + req 第 7 章联动 5）。
 *
 * <p>装配：内嵌 config 只圈 spike.sqlite.entity.d4 包（001-plan.md - 2.2 目录结构 骨架设计说明 2），
 * URL 覆盖为 d4.db、连接串三参数与 2.3 yaml 原样保留（001-plan.md - 2.3 application.yaml（T0-3）
 * "D3/D4 测试类用 @SpringBootTest(properties) 覆盖为自己的文件"条款）；其余属性沿用 application.yaml。
 */
@SpringBootTest(classes = D4CommunityDialectJpaTest.D4Context.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=" + D4CommunityDialectJpaTest.D4_JDBC_URL)
@TestMethodOrder(OrderAnnotation.class)
class D4CommunityDialectJpaTest {

    private static final Logger log = LoggerFactory.getLogger(D4CommunityDialectJpaTest.class);

    /** 本类库文件的共同目录（001-plan.md - 2.2 目录结构 骨架设计说明 3：统一放 target/spike-data/）。 */
    private static final Path DATA_DIR = Path.of("target", "spike-data");

    /**
     * D4 专用连接串：库文件换 d4.db，参数段与 application.yaml 原样保留（journal_mode=WAL &
     * busy_timeout=5000 & synchronous=FULL；001-plan.md - 2.3 application.yaml（T0-3））。同时供
     * @SpringBootTest 属性覆盖与裸 JDBC 探针使用，两处一字不差（编译期常量保证）。
     */
    static final String D4_JDBC_URL =
            "jdbc:sqlite:target/spike-data/d4.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL";

    /**
     * CRUD 样本的基准时刻（update 路径刷新 update_time 的对照起点，取固定值避免时钟分辨率竞态）。
     * 取毫秒精度（.123）：方法②只验 CRUD 链路机制，纳秒精度边界由方法③专门承载——2026-10-09 首跑实测
     * 社区方言路径下 LocalDateTime 亚毫秒位往返截断（坑位，方法③取证），方法②样本若混入纳秒，链路后段
     * （update / delete 观察）会被同一断言提前截停、无法取证（R8 实验代码缺陷修复重跑，机制断言未动）。
     */
    private static final LocalDateTime CRUD_BASE_TIME = LocalDateTime.of(2026, 10, 9, 8, 0, 0, 123000000);

    @Autowired
    private TypeProbeRepository typeProbeRepository;

    /** 生效装配 URL 留档用（应等于 D4_JDBC_URL，证明覆盖到位）。 */
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    /**
     * D4 实验上下文（001-plan.md - 2.2 目录结构 骨架设计说明 2：内嵌 @SpringBootConfiguration + 显式圈定）。
     *
     * <p>@EntityScan / @EnableJpaRepositories 只圈 spike.sqlite.entity.d4，阻断其他实验（d1v1/d1v2/d3）
     * 的实体与仓库混进本上下文——尤其 d1v1/d1v2 两组实体同表名 sample_row 绝不能同上下文共存。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("spike.sqlite.entity.d4")
    @EnableJpaRepositories("spike.sqlite.entity.d4")
    static class D4Context {
    }

    /**
     * 统一清理重建本类库文件（含 -wal/-shm 伴生），保证首建 DDL 快照语义可复现
     * （001-plan.md - 2.2 目录结构 骨架设计说明 3）。此刻 Spring 上下文尚未启动，删除不与任何
     * 存活连接冲突（与 T1 已验证的 D2 同款时序）；type_probe 表由本上下文启动时 ddl-auto=update 首建。
     */
    @BeforeAll
    static void cleanSpikeDataFiles() throws Exception {
        Files.createDirectories(DATA_DIR);
        for (String suffix : List.of("", "-wal", "-shm")) {
            Files.deleteIfExists(DATA_DIR.resolve("d4.db" + suffix));
        }
        log.info("[D4-前置] 已清理重建 {}（d4.db 及其 -wal/-shm）", DATA_DIR);
    }

    /**
     * 方法①（判据 3）：裸连接查 PRAGMA table_info(type_probe) 逐列打印 + SELECT sql FROM sqlite_master
     * WHERE name='type_probe' 打印建表语句，断言小写化后含 autoincrement，逐列声明类型落日志。
     * SQLite 列类型是建议性的（001-spec.md - 2.4 条款 D4（P0）：社区方言常规 JPA 面够用性），声明类型
     * 只实录不断言具体值；AUTOINCREMENT 必须真出现（TechnicalSolution.md - 9.2 SQLite 关系型数据 (2) 的
     * session_messages.id 依赖此语义），检索不到即 R6 分支。
     */
    @Test
    @Order(1)
    void ddlSnapshot_autoincrementAndColumnTypes() throws SQLException {
        log.info("[D4-DDL快照] 生效 spring.datasource.url = {}（应与裸连接一致）", datasourceUrl);
        try (Connection conn = DriverManager.getConnection(D4_JDBC_URL)) {
            Map<String, String> columnDeclarations = new LinkedHashMap<>();
            String primaryKeyColumn = "（未读到）";
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("PRAGMA table_info(type_probe)")) {
                while (rs.next()) {
                    int cid = rs.getInt("cid");
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    int notNull = rs.getInt("notnull");
                    String defaultValue = rs.getString("dflt_value");
                    int pk = rs.getInt("pk");
                    columnDeclarations.put(name, type);
                    if (pk > 0) {
                        primaryKeyColumn = name;
                    }
                    log.info("[D4-DDL快照] table_info 第 {} 列：name = {}，声明类型 = {}，notnull = {}，dflt_value = {}，pk = {}",
                            cid, name, type, notNull, defaultValue, pk);
                }
            }
            log.info("[D4-DDL快照] 逐列声明类型汇总 = {}（SQLite 列类型建议性，仅实录）", columnDeclarations);
            log.info("[D4-DDL快照] 主键列（pk=1）= {}", primaryKeyColumn);
            assertThat(columnDeclarations)
                    .as("type_probe 表应存在且含主键列 id（上下文启动 ddl-auto=update 首建）")
                    .containsKey("id");

            String createSql;
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT sql FROM sqlite_master WHERE name = 'type_probe'")) {
                assertThat(rs.next()).as("sqlite_master 应能检索到 type_probe 的建表语句").isTrue();
                createSql = rs.getString(1);
            }
            log.info("[D4-DDL快照] 建表语句原文 = {}", createSql);
            log.info("[D4-DDL快照] 关键字检索：小写化含 autoincrement = {}，含 integer = {}",
                    createSql.toLowerCase().contains("autoincrement"), createSql.toLowerCase().contains("integer"));
            assertThat(createSql.toLowerCase())
                    .as("建表 DDL 主键列声明应含 AUTOINCREMENT 关键字（判据 3；检索不到按 R6 登记为坑，不降级）")
                    .contains("autoincrement");
        }
    }

    /**
     * 方法②（判据 1）：CRUD 四步全链——save → findById → 字段修改再 save → 再查 → delete → 查空，
     * 全程 AssertJ 断言；update 路径同步刷新 update_time（agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）
     * 新表条款：任何更新路径必须同步刷新本列）。对照值取固定基准时刻，避免时钟分辨率竞态。
     */
    @Test
    @Order(2)
    void crudFullChain_fourSteps() {
        // 步骤 1：save
        TypeProbe draft = new TypeProbe();
        draft.setFlagValue(true);
        draft.setCountValue(42L);
        draft.setNameValue("crud-初始样本");
        draft.setEventTime(CRUD_BASE_TIME.plusHours(1));
        draft.setCreateTime(CRUD_BASE_TIME);
        draft.setUpdateTime(CRUD_BASE_TIME);
        TypeProbe inserted = typeProbeRepository.save(draft);
        log.info("[D4-CRUD] 步骤 1 save 完成，IDENTITY 回填 id = {}", inserted.getId());
        assertThat(inserted.getId()).as("IDENTITY 主键应在插入后回填").isNotNull();

        // 步骤 2：findById 逐字段断言
        TypeProbe loaded = typeProbeRepository.findById(inserted.getId())
                .orElseThrow(() -> new AssertionError("步骤 2 findById 应能查回刚插入的行 id=" + inserted.getId()));
        log.info("[D4-CRUD] 步骤 2 findById 查回 id = {}", loaded.getId());
        assertRowFields(loaded, true, 42L, "crud-初始样本", CRUD_BASE_TIME.plusHours(1),
                CRUD_BASE_TIME, CRUD_BASE_TIME, "步骤2 findById");

        // 步骤 3：字段修改再 save（update_time 同步刷新）→ 再查
        loaded.setFlagValue(false);
        loaded.setCountValue(4242L);
        loaded.setNameValue("crud-修改后样本");
        loaded.setEventTime(CRUD_BASE_TIME.plusHours(2));
        LocalDateTime refreshedUpdateTime = CRUD_BASE_TIME.plusMinutes(1);
        loaded.setUpdateTime(refreshedUpdateTime);
        typeProbeRepository.saveAndFlush(loaded);
        TypeProbe reloaded = typeProbeRepository.findById(inserted.getId())
                .orElseThrow(() -> new AssertionError("步骤 3 再查应能查回 id=" + inserted.getId()));
        log.info("[D4-CRUD] 步骤 3 修改再 save 后再查完成，update_time 刷新为 {}", reloaded.getUpdateTime());
        assertRowFields(reloaded, false, 4242L, "crud-修改后样本", CRUD_BASE_TIME.plusHours(2),
                CRUD_BASE_TIME, refreshedUpdateTime, "步骤3 修改后再查");
        log.info("[D4-CRUD] update 路径同步刷新校验：update_time {} 在原值 {} 之后",
                reloaded.getUpdateTime(), CRUD_BASE_TIME);
        assertThat(reloaded.getUpdateTime()).as("update_time 应同步刷新且晚于初始值").isAfter(CRUD_BASE_TIME);

        // 步骤 4：delete → 查空
        typeProbeRepository.deleteById(inserted.getId());
        boolean gone = typeProbeRepository.findById(inserted.getId()).isEmpty();
        long remaining = typeProbeRepository.count();
        log.info("[D4-CRUD] 步骤 4 delete 后：findById 查空 = {}，全表剩余行数 = {}", gone, remaining);
        assertThat(gone).as("delete 后 findById 应查空").isTrue();
        assertThat(remaining).as("delete 后全表应零行（此时仅本方法写过数据）").isZero();
    }

    /**
     * 方法③（判据 2 与判据 4）：写入 Boolean/long/String/LocalDateTime 边界样本（纳秒精度
     * 2026-10-09T12:34:56.123456789、跨日时刻 2026-10-09T23:59:59.999999999），经 JPA 读出逐字段比对；
     * 再用裸连接 SELECT 原始值打印实际存储形态（文本串还是整数）。存储形态只实录不预设
     * （001-spec.md - 2.4 条款 D4（P0）：社区方言常规 JPA 面够用性：实际落什么存储形态以实测为准）。
     */
    @Test
    @Order(3)
    void typeRoundTrip_boundarySamples() throws SQLException {
        LocalDateTime nanos = LocalDateTime.of(2026, 10, 9, 12, 34, 56, 123456789);
        LocalDateTime lastInstantOfDay = LocalDateTime.of(2026, 10, 9, 23, 59, 59, 999999999);
        LocalDateTime startOfDay = LocalDateTime.of(2026, 10, 9, 0, 0, 0, 0);

        TypeProbe nanoSample = new TypeProbe();
        nanoSample.setFlagValue(false);
        nanoSample.setCountValue(0L);
        nanoSample.setNameValue("边界-纳秒精度：中文「引号」'单引'\"双引\"+emoji🚀");
        nanoSample.setEventTime(nanos);
        nanoSample.setCreateTime(startOfDay);
        nanoSample.setUpdateTime(startOfDay);

        TypeProbe lastInstantSample = new TypeProbe();
        lastInstantSample.setFlagValue(true);
        lastInstantSample.setCountValue(Long.MAX_VALUE);
        lastInstantSample.setNameValue("边界-跨日时刻-" + "x".repeat(64));
        lastInstantSample.setEventTime(lastInstantOfDay);
        lastInstantSample.setCreateTime(lastInstantOfDay);
        lastInstantSample.setUpdateTime(lastInstantOfDay);

        Long nanoId = typeProbeRepository.save(nanoSample).getId();
        Long lastInstantId = typeProbeRepository.save(lastInstantSample).getId();
        log.info("[D4-类型往返] 边界样本已写入：纳秒精度样本 id = {}，跨日时刻样本 id = {}（共写 2 行）", nanoId, lastInstantId);
        log.info("[D4-类型往返] 边界取值：纳秒精度 event_time = {}；跨日时刻 event_time = {}（当日最后一纳秒）；"
                + "count_value 取 0 与 Long.MAX_VALUE = {} 两端", nanos, lastInstantOfDay, Long.MAX_VALUE);

        // 存储形态探针前置（判据 4）：先取证原始存储值，再做逐字段比对——2026-10-09 首跑实证，比对断言
        // 失败会中断方法，探针放在断言之后会导致存储形态证据丢失（R8 实验代码缺陷修复，探针内容未动）。
        log.info("[D4-存储形态] 裸连接 SELECT 原始值实录（typeof() 为 SQLite 存储类别：text/integer/real/blob/null）");
        try (Connection conn = DriverManager.getConnection(D4_JDBC_URL);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id, typeof(id), flag_value, typeof(flag_value), "
                        + "count_value, typeof(count_value), name_value, typeof(name_value), "
                        + "event_time, typeof(event_time), create_time, typeof(create_time), "
                        + "update_time, typeof(update_time) FROM type_probe ORDER BY id")) {
            while (rs.next()) {
                log.info("[D4-存储形态] 行 id={}：id={}（{}），flag_value={}（{}），count_value={}（{}），"
                        + "name_value={}（{}），event_time={}（{}），create_time={}（{}），update_time={}（{}）",
                        rs.getLong(1), rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getString(10),
                        rs.getString(11), rs.getString(12),
                        rs.getString(13), rs.getString(14));
            }
        }

        TypeProbe nanoReadBack = typeProbeRepository.findById(nanoId)
                .orElseThrow(() -> new AssertionError("纳秒精度样本 id=" + nanoId + " 应可查回"));
        TypeProbe lastInstantReadBack = typeProbeRepository.findById(lastInstantId)
                .orElseThrow(() -> new AssertionError("跨日时刻样本 id=" + lastInstantId + " 应可查回"));
        // SoftAssertions 收集两行全部字段的比对结果后一次裁决（2026-10-09 首跑实证 fail-fast 会让
        // 后一样本的字段比对没有证据落档）；断言预期与写入值不变，只改收集方式，失败照常判失败。
        SoftAssertions softly = new SoftAssertions();
        compareRoundTripFields(softly, nanoReadBack, nanoSample, "纳秒精度样本");
        compareRoundTripFields(softly, lastInstantReadBack, lastInstantSample, "跨日时刻样本");
        softly.assertAll();
    }

    /** CRUD 路径的逐字段断言出口：先落日志再断言，任一字段不符时证据已留档。 */
    private void assertRowFields(TypeProbe actual, boolean expectedFlag, long expectedCount, String expectedName,
            LocalDateTime expectedEventTime, LocalDateTime expectedCreateTime, LocalDateTime expectedUpdateTime,
            String step) {
        log.info("[D4-CRUD] {} 逐字段：flag_value = {}，count_value = {}，name_value = {}，"
                + "event_time = {}，create_time = {}，update_time = {}",
                step, actual.getFlagValue(), actual.getCountValue(), actual.getNameValue(),
                actual.getEventTime(), actual.getCreateTime(), actual.getUpdateTime());
        assertThat(actual.getFlagValue()).as("%s flag_value", step).isEqualTo(expectedFlag);
        assertThat(actual.getCountValue()).as("%s count_value", step).isEqualTo(expectedCount);
        assertThat(actual.getNameValue()).as("%s name_value", step).isEqualTo(expectedName);
        assertThat(actual.getEventTime()).as("%s event_time", step).isEqualTo(expectedEventTime);
        assertThat(actual.getCreateTime()).as("%s create_time", step).isEqualTo(expectedCreateTime);
        assertThat(actual.getUpdateTime()).as("%s update_time", step).isEqualTo(expectedUpdateTime);
    }

    /**
     * 类型往返的逐字段比对出口（判据 2）：逐字段先落日志（写入值 vs 读回值 vs 一致与否），再经
     * SoftAssertions 收集（调用方统一 assertAll 裁决）。断言预期与写入值不变，收集方式保证两行样本
     * 全部字段的比对证据都能落档（2026-10-09 首跑实证 fail-fast 会截停后一样本的取证）。
     */
    private void compareRoundTripFields(SoftAssertions softly, TypeProbe readBack, TypeProbe written,
            String sampleLabel) {
        boolean flagMatch = written.getFlagValue().equals(readBack.getFlagValue());
        boolean countMatch = written.getCountValue() == readBack.getCountValue();
        boolean nameMatch = written.getNameValue().equals(readBack.getNameValue());
        boolean eventTimeMatch = written.getEventTime().equals(readBack.getEventTime());
        boolean createTimeMatch = written.getCreateTime().equals(readBack.getCreateTime());
        boolean updateTimeMatch = written.getUpdateTime().equals(readBack.getUpdateTime());
        log.info("[D4-类型往返] {}（id = {}）逐字段比对：flag_value 写入 {} 读回 {} 一致 = {}；"
                + "count_value 写入 {} 读回 {} 一致 = {}；name_value 写入长度 {} 读回长度 {} 一致 = {}；"
                + "event_time 写入 {} 读回 {} 一致 = {}；create_time 写入 {} 读回 {} 一致 = {}；"
                + "update_time 写入 {} 读回 {} 一致 = {}",
                sampleLabel, readBack.getId(),
                written.getFlagValue(), readBack.getFlagValue(), flagMatch,
                written.getCountValue(), readBack.getCountValue(), countMatch,
                written.getNameValue().length(), readBack.getNameValue().length(), nameMatch,
                written.getEventTime(), readBack.getEventTime(), eventTimeMatch,
                written.getCreateTime(), readBack.getCreateTime(), createTimeMatch,
                written.getUpdateTime(), readBack.getUpdateTime(), updateTimeMatch);
        softly.assertThat(readBack.getFlagValue()).as("%s flag_value 往返", sampleLabel).isEqualTo(written.getFlagValue());
        softly.assertThat(readBack.getCountValue()).as("%s count_value 往返", sampleLabel).isEqualTo(written.getCountValue());
        softly.assertThat(readBack.getNameValue()).as("%s name_value 往返", sampleLabel).isEqualTo(written.getNameValue());
        softly.assertThat(readBack.getEventTime()).as("%s event_time 往返", sampleLabel).isEqualTo(written.getEventTime());
        softly.assertThat(readBack.getCreateTime()).as("%s create_time 往返", sampleLabel).isEqualTo(written.getCreateTime());
        softly.assertThat(readBack.getUpdateTime()).as("%s update_time 往返", sampleLabel).isEqualTo(written.getUpdateTime());
    }
}
