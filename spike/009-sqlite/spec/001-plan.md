# Spike 实施计划（001-plan）

> 位置：`spike/009-sqlite/spec/001-plan.md`
> 创建：2026-10-09 · 状态：待执行
> 上游：`spec/001-req.md`（下称 req，背景、验收源头与结论回填联动的唯一源头）、`spec/001-spec.md`（下称 spec，条款 D1-D6 与通过判据的唯一源头）。本文只做任务化：按什么顺序做（第 1 章）、工程长什么样（第 2 章）、每步敲什么命令（第 3 章）、怎么算完成（第 4 章矩阵，判据逐条转录 spec 第 2 章，不另立口径）。
> 约定：① 所有命令在 `spike/009-sqlite/` 目录内执行（spike/CLAUDE.md - 规则：独立 Maven 工程）；② mvn 一律用全路径 `/opt/homebrew/bin/mvn`（2026-09-14 定案，沿用 007/008 两轮 spike 执行记录）；③ mvn 前置 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`（2026-10-09 实测：不设时该 mvn 报 Java 26.0.2.1，设后报 21.0.10，与 req 1.3 执行环境 JDK 21 对齐）；④ 术语与文件指代沿用 req 0.1 术语表与 spec 0.2 文件指代（含"req 2.1"式章节简写、"TechnicalSolution.md - 4.3 关键设计点 (2)"式四级条目简写）；⑤ 本文完成判定不弱于 spec 第 2 章任何一条。

## 0. 文档定位与前置状态

- 本文回答三件事：按什么顺序做（第 1 章）、每步敲什么命令（第 3 章）、怎么算完成（第 4 章）。
- **整体 DoD**：req 第 6 章「验收标准」五条全满足——六问全答、五账全销、证据齐备、README 齐备、联动明确。本文不弱化任何一条。
- **前置状态（2026-10-09 核验）**：
  - spec 已通过检查：`spec/validate/002-validate-spec.md`（2026-10-09，第 1 轮判定 pass、阻断 0 项）。
  - 工程骨架未建：`spike/009-sqlite/` 现只有 `spec/`（req、spec、validate 报告），无 pom、无 src。
  - 全部验证项离线可跑：不调模型 API、不需要凭证（req 1.1）。无需加载任何环境变量脚本。
- **环境基线（2026-10-09 实测）**：JDK 21（`java -version` 报 21.0.10，Microsoft OpenJDK）；Maven 3.9.10（`/opt/homebrew/bin/mvn`，默认 JAVA_HOME 指向 JDK 26，须按约定 ③ 先切 21）；sqlite3 命令行 3.51.0（仅排障备手，实验本体全在 Java 内跑）；版本组合基线 Spring Boot 3.5.16 / hibernate-community-dialects 6.6.53.Final / sqlite-jdbc 3.49.1.0（req 1.3，BOM 托管）。
- 参考耗时合计约 3.5 小时（各任务标在标题行）。参考值非硬时间盒：req/spec 未设时间盒条款，执行时只用于节奏管理。

## 1. 任务总览（顺序与依赖）

```text
T0 骨架与依赖预检（离线，~20min）        ← 唯一入口；pom + 全部实体与服务 + 依赖树预检
 └→ T1 D2 pragma 生效性（~30min）        ← 连接串机制的接线前提，最先做
     └→ T2 D4 方言常规 JPA 面（~30min）  ← 最小 JPA 链路首次全链实跑
         └→ T3 D3 轮原子 + 并发（~45min） ← 并发组连接串用 D2 已验证写法
             └→ T4 D1 表演进（~30min）    ← 独立实验，排此处靠前收 P0
                 └→ T5 D5 提交耗时对照（~20min）← 需机器空闲，排在收集类实验之后
                     └→ T6 D6 收口（~10min）  ← 依赖 D1-D5 全部完成
                         └→ T7 README 收口（~30min）
```

- 线性推进：前一任务完成判定未过、且不命中第 5 章分支时，不进入下一任务。
- **D2 排最前的理由**：D3 并发组的连接串"写法与 D2 已验证的写法一致"（spec 2.3 行为规约第 3 条），D5 两组对照的 URL 也压在连接串参数机制上。D2 若发现参数不生效，D3/D5 按第 5 章分支换备选路径，避免返工。D2 正向组上下文启动同时就是"数据源 + 方言 + 驱动成套"的最小链路冒烟。
- **D1 排 D4 之后**：两者都走 ddl-auto=update 建表，互不依赖；D4 先行可提前暴露方言级问题。
- spec 第 6 章两个新增候选验证点在需求方裁决前不排入任务（本文不含其任务与判据）。

## 2. 工程骨架清单

### 2.1 pom.xml（T0-1，全文可照抄）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
  </parent>

  <groupId>com.agentos.spike</groupId>
  <artifactId>sqlite-spike</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>sqlite-spike</name>
  <description>AgentOS spike: SQLite persistence mechanism verification (spec: see spec/)</description>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <!-- JPA 全链：Hibernate + HikariCP + 事务管理（生产同款入口） -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- SQLite JDBC 驱动。不写版本号：能否被 Boot BOM 仲裁出 3.49.1.0 正是 D6 的核验点 -->
    <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
    </dependency>
    <!-- SQLite 无官方方言，显式引入社区方言（agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）方言成套条款）。
         版本用 ${hibernate.version}（Boot BOM 属性），与 agentos-storage 模块 pom 同写法；
         本 pom 禁自定义该属性（禁定义与父链同名 property） -->
    <dependency>
      <groupId>org.hibernate.orm</groupId>
      <artifactId>hibernate-community-dialects</artifactId>
      <version>${hibernate.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <!-- spike 超时兜底（spike/CLAUDE.md - 编码规范（2026-09-15 起：spike 的 Java 代码同样遵守）） -->
          <forkedProcessTimeoutInSeconds>180</forkedProcessTimeoutInSeconds>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

依赖坐标与仲裁预期（D6 核验的对照清单）：

| 构件 | pom 写法 | 预期仲裁结果 | 出处 |
|---|---|---|---|
| parent `spring-boot-starter-parent` | **3.5.16**（锁定） | — | req 1.3；与 agentos-storage 依赖树同基线 |
| `org.springframework.boot:spring-boot-starter-data-jpa` | 不写版本号 | Boot 3.5.16 BOM 托管 | agentos/agentos-storage/pom.xml 同款 |
| `org.xerial:sqlite-jdbc` | 不写版本号 | **3.49.1.0**（BOM 属性 sqlite-jdbc.version） | 同上；req 1.3 |
| `org.hibernate.orm:hibernate-community-dialects` | **`${hibernate.version}`**（本 pom 不定义该属性） | **6.6.53.Final** | agentos-storage pom 同款；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）方言成套条款 |
| `org.springframework.boot:spring-boot-starter-test`（test） | 不写版本号 | Boot 托管 | 007/008 spike 同款 |

### 2.2 目录结构（`#` 注释 = 创建任务归属，T 编号见第 3 章）

```text
spike/009-sqlite/
├── pom.xml                                         # T0-1（2.1 全文）
├── .gitignore                                      # T0-2：target/ logs/ *.iml .DS_Store（007 同款）
├── spec/                                           # 已有：001-req / 001-spec / 001-plan（本文）/ validate/
├── logs/                                           # 证据日志（.gitignore 忽略；T1 起逐个落）
└── src/test/
    ├── java/spike/sqlite/
    │   ├── entity/
    │   │   ├── d1v1/SampleRowV1.java               # T0-4（表 sample_row：id IDENTITY + name(String) + quantity(Long)）
    │   │   ├── d1v1/SampleRowV1Repository.java     # T0-4（JpaRepository）
    │   │   ├── d1v2/SampleRowV2.java               # T0-4（同表 sample_row：V1 三字段不动，只加 remark(String)）
    │   │   ├── d1v2/SampleRowV2Repository.java     # T0-4
    │   │   ├── d3/SpikeMessage.java                # T0-5（表 spike_message：id IDENTITY / session_id / role / payload / created_at）
    │   │   ├── d3/SpikeMessageRepository.java      # T0-5
    │   │   ├── d4/TypeProbe.java                   # T0-6（表 type_probe：id IDENTITY / flag_value(Boolean) / count_value(long) / name_value(String) / event_time(LocalDateTime) / create_time / update_time）
    │   │   └── d4/TypeProbeRepository.java         # T0-6
    │   ├── service/
    │   │   └── RoundCommitService.java             # T0-7（D3：见 2.4 方法说明）
    │   ├── config/
    │   │   ├── D1V1Config.java                     # T0-4（@SpringBootConfiguration + @EnableAutoConfiguration + @EntityScan("spike.sqlite.entity.d1v1") + @EnableJpaRepositories(同包)）
    │   │   └── D1V2Config.java                     # T0-4（同构，只圈 d1v2 包）
    │   ├── D2PragmaEffectivenessTest.java          # T1
    │   ├── D4CommunityDialectJpaTest.java          # T2
    │   ├── D3RoundAtomicCommitTest.java            # T3
    │   ├── D1SchemaEvolutionTest.java              # T4
    │   └── D5FsyncTimingTest.java                  # T5
    └── resources/
        └── application.yaml                        # T0-3（2.3 全文）
```

骨架设计三条说明：

1. **无 main 源码集**：本 spike 无生产入口，全部验证由测试承载（req 交付物第 1 项即"D1-D5 测试代码"）。实体、服务、配置、yaml 全放 test 源码集。
2. **每类实验独立圈定实体**：D2/D3/D4 测试类用内嵌 `@SpringBootConfiguration` + 显式 `@EntityScan` / `@EnableJpaRepositories` 只圈本实验的包；D1 用独立 config 类（D1V1Config / D1V2Config）配 `SpringApplicationBuilder` 程序化起停。依据：agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）"实体/仓库在主类包之外时必须显式 @EntityScan/@EnableJpaRepositories"条款。用途：防止其他实验的实体混进当前上下文（尤其 D1 两个实体同表名 `sample_row`，绝不能同上下文共存）。
3. **库文件统一放 `target/spike-data/`**：`.gitignore` 的 `target/` 已覆盖；各测试类 `@BeforeAll` 建目录并删除本类旧库文件，保证"全新库文件"语义可复现（spec 0.2 术语：全新库文件）。

### 2.3 application.yaml（T0-3，src/test/resources，全文可照抄）

生产同款装配（与 agentos/agentos-boot/src/main/resources/application.yaml 的 datasource/jpa 段同款；路径换成 spike 本地文件）：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:target/spike-data/d2.db?journal_mode=WAL&busy_timeout=5000&synchronous=FULL
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    open-in-view: false
```

- 此 URL 即 D2 正向组的生产同款装配（spec 2.2 行为规约：单一入口约束，经 HikariCP，不自建 DataSource Bean）。
- D3/D4 测试类用 `@SpringBootTest(properties = "spring.datasource.url=…")` 覆盖为自己的文件（`d3.db` / `d4.db`，参数段原样保留）；其余属性沿用本 yaml。
- D1 两个 config 各自用 `SpringApplicationBuilder.properties(...)` 覆盖 URL 为 `d1.db`（V1/V2 两上下文共用）与 `d1-fresh.db`（首建对照）；同一 yaml 照常加载——满足 spec 2.1"同一 application.yaml、同一库文件、ddl-auto 不变"。

### 2.4 测试类清单（5 类 + 1 个命令收口项）

| 测试类 | 覆盖 | 方法（@Order 顺序执行） | 证据文件 |
|---|---|---|---|
| D2PragmaEffectivenessTest | D2 | ① `positiveGroup_readbackThreePragmas`：起 Spring 上下文（2.3 yaml 原样），从 DataSource（HikariCP 连接池）取连接，依次执行并打印 `PRAGMA journal_mode` / `PRAGMA busy_timeout` / `PRAGMA synchronous`；② `walCompanionFiles_visibleWhileConnectionOpen`：同一上下文建一张临时表并写入 1 行，**连接存活窗口内**列 `target/spike-data/` 目录，打印 `d2.db-wal` 与 `d2.db-shm` 存在性；③ `negativeJournalMode_freshFileWithoutParams`：裸 JDBC（DriverManager，不经连接池）无参打开全新文件 `d2-fresh.db`，回读 journal_mode；④ `negativeBusyTimeoutSync_reopenSameFileWithoutParams`：裸 JDBC 无参重开 `d2.db`（同文件），回读 busy_timeout 与 synchronous 缺省值；⑤ `typoProbe_misspelledParamRecorded`：URL 写 `journalmode=WAL`（少一个下划线）打开另一全新文件，回读 journal_mode，如实记录驱动有无报错或日志 | logs/d2-pragma.txt |
| D4CommunityDialectJpaTest | D4 | ① `ddlSnapshot_autoincrementAndColumnTypes`：裸连接查 `PRAGMA table_info(type_probe)` 逐列打印 + `SELECT sql FROM sqlite_master WHERE name='type_probe'` 打印建表语句，断言小写化后含 `autoincrement`，逐列声明类型落日志；② `crudFullChain_fourSteps`：save → findById → 字段修改再 save → 再查 → delete → 查空，全程 AssertJ 断言（update 路径同步刷新 update_time）；③ `typeRoundTrip_boundarySamples`：写入 Boolean/long/String/LocalDateTime 边界样本（纳秒精度 `2026-10-09T12:34:56.123456789`、跨日时刻 `2026-10-09T23:59:59.999999999`），读出逐字段比对；裸连接 SELECT 原始值打印实际存储形态（文本串还是整数） | logs/d4-dialect-jpa.txt |
| D3RoundAtomicCommitTest | D3 | ① `normalPath_roundOfFive_idsContinuous`：调 `RoundCommitService.commitRound`（@Transactional），断言行数=5、事务内分配的 id 排序后逐个 +1 连续，id 清单打印；② `exceptionPath_rollbackZeroRows`：调 `commitThenThrow`（插入后抛 RuntimeException），断言行数=0；③ `concurrentGroup1_holdShort_waitThenSuccess`：两个裸 JDBC 连接交叠写 `d3-conc.db`（连接串三参数与 D2 正向组同款）——A 关自动提交、INSERT、持锁等待 1000ms（<5000ms）后提交；同窗口 B 发起 INSERT，用 System.nanoTime 计打印 B 等待时长与结果；④ `concurrentGroup2_holdBeyondTimeout_busySample`：同上但 A 持锁 6500ms（>5000ms），捕获 B 的异常类型名与完整消息原文打印 | logs/d3-round-atomic.txt |
| D1SchemaEvolutionTest | D1 | ① `step1_baselineV1_createAndInsert`：起上下文 A（D1V1Config + URL 指向 `d1.db`），插入 2 行样本，打印 `PRAGMA table_info(sample_row)` 快照与 sqlite_master 建表语句，关闭上下文 A；② `step2_evolveV2_sameFile`：起上下文 B（D1V2Config，同 yaml 同文件），打印 V2 阶段 `PRAGMA table_info` 快照，查 V1 样本行行数与 remark 列在旧行上的取值并打印；③ `step3_freshFile_firstBuildV2`：换全新文件 `d1-fresh.db` 直接用 V2 首建，打印快照（预期 V2 全部列，如实记录） | logs/d1-schema-evolution.txt |
| D5FsyncTimingTest | D5 | ① `groupFull_thousandCommits`：裸 JDBC 打开 `d5-full.db`（URL 带 `journal_mode=WAL&synchronous=FULL`），先回读 journal_mode 与 synchronous 打印（守门：确认参数就位再计时），建表，10 次预热提交（不计时），System.nanoTime 计时连续 1000 次"开事务 → INSERT 1 行 → 提交"，打印总耗时与单次均值（ms）；② `groupNormal_thousandCommits`：同流程开 `d5-normal.db`（仅 synchronous 换 NORMAL），计时后一并打印 FULL/NORMAL 倍数（组 1 总耗时 ÷ 组 2 总耗时） | logs/d5-fsync-timing.txt |
| （无测试类） | D6 | 命令收口：`dependency:tree` 终态 + `mvn test` 全量（见 T6） | logs/d6-dependency-tree.txt、logs/d6-full-test.txt |

RoundCommitService（T0-7，两个 @Transactional 方法，供 D3 JPA 路径调用）：

- `commitRound(String sessionId)`：单事务内插入 1 条 role=user + 2 条 role=assistant + 2 条 role=tool，共 5 行；返回 5 个 id。
- `commitThenThrow(String sessionId)`：同结构插入后抛 RuntimeException（触发回滚）。
- 纪律对齐：事务体内只做写库（spec 2.3 短事务纪律；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）"物理事务尽可能短"条款）。

## 3. 任务明细

### T0 骨架与依赖预检（离线，~20min）

| # | 步骤 |
|---|---|
| T0-1 | 写 `pom.xml`（2.1 全文照抄） |
| T0-2 | 写 `.gitignore`：`target/`、`logs/`、`*.iml`、`.DS_Store` |
| T0-3 | 写 `src/test/resources/application.yaml`（2.3 全文照抄） |
| T0-4 | 写 D1 两组实体与仓库 + D1V1Config / D1V2Config（2.2 注释所列字段） |
| T0-5 | 写 D3 实体与仓库（SpikeMessage / SpikeMessageRepository） |
| T0-6 | 写 D4 实体与仓库（TypeProbe / TypeProbeRepository） |
| T0-7 | 写 RoundCommitService（2.4 末尾说明） |
| T0-8 | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` 后 `/opt/homebrew/bin/mvn -version`（确认报 Java 21.0.10） |
| T0-9 | `/opt/homebrew/bin/mvn dependency:tree > logs/d6-dependency-tree.txt`（预检；T6 会重跑覆盖为终态） |
| T0-10 | `grep -nE 'org.xerial:sqlite-jdbc\|hibernate-community-dialects' logs/d6-dependency-tree.txt`（预期各 1 行，版本 3.49.1.0 与 6.6.53.Final；表格内竖线按 GFM 转义为 \|，照抄时用回竖线） |
| T0-11 | `/opt/homebrew/bin/mvn test-compile`（编译全部骨架代码） |

**完成判定**：test-compile 绿；T0-10 两构件各 1 行且版本与 2.1 对照表一致。失败 → 第 5 章 R0。**证据**：`logs/d6-dependency-tree.txt`（预检版）。

### T1 D2 pragma 生效性（~30min）

| # | 步骤 |
|---|---|
| T1-1 | 写 `D2PragmaEffectivenessTest`（2.4 方法清单；五个方法 @Order 顺序执行；证据全部 SLF4J 打印） |
| T1-2 | `/opt/homebrew/bin/mvn test -Dtest=D2PragmaEffectivenessTest > logs/d2-pragma.txt 2>&1` |

**完成判定**（= spec 2.2 通过判据 1-5，转录见「4 测试矩阵」D2 行）：正向组三回读 = wal / 5000 / 2；负向组两组回读落档；`-wal`、`-shm` 文件出现；拼错探针行为在案；任一参数不生效时备选路径给确定结论。失败 → R1 / R2。**证据**：`logs/d2-pragma.txt`。

执行注记两条：① `-wal`/`-shm` 必须在连接存活窗口内列目录——SQLite 干净关闭最后一个连接会收走伴生文件（方法 ② 已按此设计）；② 方法 ③⑤ 的全新文件由 @BeforeAll 统一清理重建，保证"从未设过 WAL"语义。

### T2 D4 方言常规 JPA 面（~30min）

| # | 步骤 |
|---|---|
| T2-1 | 写 `D4CommunityDialectJpaTest`（2.4 方法清单；内嵌 config 只圈 `spike.sqlite.entity.d4`；URL 覆盖为 `d4.db` 三参数同款） |
| T2-2 | `/opt/homebrew/bin/mvn test -Dtest=D4CommunityDialectJpaTest > logs/d4-dialect-jpa.txt 2>&1` |

**完成判定**（= spec 2.4 通过判据 1-5）：CRUD 四步断言全过；四类字段往返逐字段一致；DDL 快照落档且 AUTOINCREMENT 可检索命中；存储形态实录；坑位三级归类（若有）。失败 → R6。**证据**：`logs/d4-dialect-jpa.txt`。

### T3 D3 轮原子提交与并发 BUSY（~45min）

| # | 步骤 |
|---|---|
| T3-1 | 写 `D3RoundAtomicCommitTest`（2.4 方法清单；JPA 路径走 RoundCommitService；并发两组裸 JDBC，连接串三参数与 D2 正向组同款） |
| T3-2 | `/opt/homebrew/bin/mvn test -Dtest=D3RoundAtomicCommitTest > logs/d3-round-atomic.txt 2>&1` |

**完成判定**（= spec 2.3 通过判据 1-5）：行数=5 且 id 连续；回滚后行数=0；并发组一实测结果落档并标注命中预登记预期（等待后成功）或偏差（异常）；并发组二 BUSY 异常类型名与消息原文落档；四条证据各有日志段。失败 → R3 / R4。**证据**：`logs/d3-round-atomic.txt`。

执行注记两条：① 组一 A 持锁 1000ms、组二 6500ms，B 的等待时长一律 System.nanoTime 实测打印；② 组二单方法耗时约 7 秒，surefire 180 秒兜底内。

### T4 D1 表演进（~30min）

| # | 步骤 |
|---|---|
| T4-1 | 写 `D1SchemaEvolutionTest`（2.4 方法清单；三步 @Order；上下文用后即 close） |
| T4-2 | `/opt/homebrew/bin/mvn test -Dtest=D1SchemaEvolutionTest > logs/d1-schema-evolution.txt 2>&1` |

**完成判定**（= spec 2.1 通过判据 1-4）：两份快照 + 首建快照落档可逐列对照；V1 样本行行数不变且对照值留档；结论行明确写 (a) 或 (b)、无第三种模糊态。失败 → R5。**证据**：`logs/d1-schema-evolution.txt`。

### T5 D5 提交耗时对照（~20min，须机器空闲时段）

| # | 步骤 |
|---|---|
| T5-1 | 关闭重 IO 应用（浏览器多标签、同步盘等），确认机器空闲（spec 2.5 实验控制） |
| T5-2 | 写 `D5FsyncTimingTest`（2.4 方法清单；两组同构、只差 synchronous；先回读守门再计时） |
| T5-3 | `date +%F`、`sysctl -n machdep.cpu.brand_string`、`diskutil info /`（第三条取输出里 "Type (Bundle)" 一行；环境三件：日期、机型、磁盘类型，记入 README 执行记录） |
| T5-4 | `/opt/homebrew/bin/mvn test -Dtest=D5FsyncTimingTest > logs/d5-fsync-timing.txt 2>&1` |

**完成判定**（= spec 2.5 通过判据 1-3）：两组总耗时、单次均值（ms）、FULL/NORMAL 倍数落档；README 明确回答单次 FULL 均值是否 < 10ms；数字全部实测。失败 → R7。**证据**：`logs/d5-fsync-timing.txt`。

量级换算（README 结论用，公式出自 spec 2.5 行为规约第 3 条）：一轮 = 1 次消息事务 + 每迭代 2 次审计事务，10 次迭代一轮 = 1 + 2×10 = 21 次提交事务；21 × 单次 FULL 均值 = 单轮提交开销，对照"60s 级 LLM 调用"量级。

### T6 D6 版本组合整体回归（~10min）

| # | 步骤 |
|---|---|
| T6-1 | `/opt/homebrew/bin/mvn dependency:tree > logs/d6-dependency-tree.txt`（终态，覆盖 T0 预检文件） |
| T6-2 | `grep -nE 'org.xerial:sqlite-jdbc\|hibernate-community-dialects' logs/d6-dependency-tree.txt`（判据见矩阵 D6 行；表格内竖线按 GFM 转义，照抄时用回竖线） |
| T6-3 | `/opt/homebrew/bin/mvn test > logs/d6-full-test.txt 2>&1`（全量一次跑通 D1-D5） |
| T6-4 | `grep 'Tests run' logs/d6-full-test.txt`（全类汇总行落档，逐行可见 Failures=0、Errors=0；构建结果行另看文件尾部） |

**完成判定**（= spec 2.6 通过判据 1-3）：两构件各只一个版本、BOM 仲裁；全量 Tests run 在案且 Failures=0、Errors=0；版本组合决议行可写进 README。失败 → R8。**证据**：`logs/d6-dependency-tree.txt`、`logs/d6-full-test.txt`。

### T7 README 收口（~30min）

| # | 步骤 |
|---|---|
| T7-1 | 按第 6 章模板写 `README.md`：三问结构（验证了什么 / 结果如何 / 是否采纳）+ 打勾表（D1-D6 六行）+ 决议表（逐条实测结论与依据）+ 附带实测发现 + 联动命中表（req 第 7 章 7 行逐条勾选）+ 执行记录 |
| T7-2 | 对照 req 第 6 章五条验收逐条自查；每条判据细节回指 spec 第 2 章同名条款，不复制不弱化 |

**完成判定**（= req 第 6 章五条全满足）。**证据**：README 本身。

## 4. 测试矩阵

判据列逐条转录 spec 第 2 章同名条款，未改一个数字、未另立口径。

| D | 实验方法（一段话） | 命令（在 spike/009-sqlite/ 内） | 通过判据（转录 spec） | 证据文件 |
|---|---|---|---|---|
| D1 | V1 建表插入 → 同文件换 V2 上下文再启 → 回读对照；另用全新文件 V2 首建对照（spec 2.1） | `/opt/homebrew/bin/mvn test -Dtest=D1SchemaEvolutionTest > logs/d1-schema-evolution.txt 2>&1` | ① 基线（V1）与加列后（V2）两次启动各一份 `PRAGMA table_info` 快照落 logs/，可逐列对照；② V1 样本行在 V2 阶段可查询、行数不变（对照值留档），新列在旧行上的取值如实记录；③ 全新库文件首建快照落 logs/，显示 V2 全部列；④ 结论行明确写 (a)（加列且旧数据保留）或 (b)（表结构不变，记录实际现象），无第三种模糊态 | logs/d1-schema-evolution.txt |
| D2 | 正向组生产同款装配三参数回读；负向组分两类（journal_mode 用全新库文件、另两条同文件无参重开）；文件面 + 拼错探针（spec 2.2） | `/opt/homebrew/bin/mvn test -Dtest=D2PragmaEffectivenessTest > logs/d2-pragma.txt 2>&1` | ① 正向组三项回读：`PRAGMA journal_mode`=`wal`、`PRAGMA busy_timeout`=`5000`、`PRAGMA synchronous`=`2`（FULL 的官方数值）；② 负向组回读落档：journal_mode 用全新库文件（预期 delete，以实测为准），busy_timeout 与 synchronous 用同文件无参重开、缺省值实测记录；③ 一次写入后的库目录列表落档，`-wal` 与 `-shm` 文件出现；④ 拼错参数名后回读值不变，并记录驱动有无报错或日志（预期：静默忽略坐实）；⑤ 任一参数不生效时，备选路径给出"哪条路可行"的确定结论，同样算通过 | logs/d2-pragma.txt |
| D3 | 单事务写整轮 5 行 + 事务内抛异常回滚 + 两个裸 JDBC 连接交叠写两组（占用 <5000ms / >5000ms）（spec 2.3） | `/opt/homebrew/bin/mvn test -Dtest=D3RoundAtomicCommitTest > logs/d3-round-atomic.txt 2>&1` | ① 正常路径：行数=5、事务内 id 连续，两个数字实测留档；② 异常路径：回滚后查询行数=0；③ 并发组一：实测结果落档，明确标注命中预登记预期（等待后成功）或偏差（异常）；④ 并发组二：BUSY 异常类型名与消息原文样本落档；⑤ 四条证据各自在 logs/ 有对应日志段 | logs/d3-round-atomic.txt |
| D4 | 一个实体四类字段 CRUD 全链 + DDL 回读（AUTOINCREMENT、逐列声明类型）+ 往返校验含两组 LocalDateTime 边界样本（spec 2.4） | `/opt/homebrew/bin/mvn test -Dtest=D4CommunityDialectJpaTest > logs/d4-dialect-jpa.txt 2>&1` | ① CRUD 四步断言全过（AssertJ；输出落 logs/）；② 四类字段往返逐字段比对结果落档，全部一致；③ 建表 DDL 快照落 logs/，AUTOINCREMENT 关键字在主键列声明中可检索命中；④ 各字段存储形态实录落档；⑤ 若有坑：坑清单 + 三级归类落档，"阻断"级写明影响哪些表与字段并列入 req 第 7 章联动 5 | logs/d4-dialect-jpa.txt |
| D5 | 文件库两组对照：FULL 与 NORMAL，其余全同（WAL 开、每提交写 1 行、连续 1000 次），System.nanoTime 计时（spec 2.5） | `/opt/homebrew/bin/mvn test -Dtest=D5FsyncTimingTest > logs/d5-fsync-timing.txt 2>&1` | ① 两组数字落 logs/：总耗时、单次提交均值（ms）、FULL/NORMAL 倍数；② 结论明确回答：单次 FULL 提交均值是否 < 10ms——成立维持现行表述，不成立触发 req 第 7 章联动 3 改写；③ 数字全部实测，禁估算 | logs/d5-fsync-timing.txt |
| D6 | 无独立实验：`dependency:tree` 核两构件仲裁来源 + `mvn test` 全量回归（spec 2.6） | `/opt/homebrew/bin/mvn dependency:tree > logs/d6-dependency-tree.txt` 然后 `/opt/homebrew/bin/mvn test > logs/d6-full-test.txt 2>&1` | ① `mvn dependency:tree` 输出留档：`org.xerial:sqlite-jdbc:jar:3.49.1.0` 与 `org.hibernate.orm:hibernate-community-dialects:jar:6.6.53.Final` 各只出现一个版本、来源为 BOM（spring-boot-dependencies 3.5.16）仲裁；② `mvn test` 输出留档：Tests run 数字在案，Failures=0、Errors=0；③ 结论可写进 README 的"版本组合"决议行 | logs/d6-dependency-tree.txt、logs/d6-full-test.txt |

## 5. 分支与风险（每步失败观察点与下一步）

| 编号 | 触发（哪步、什么现象） | 观察点 | 下一步 |
|---|---|---|---|
| R0 | T0 依赖解析失败（拉不到构件 / 版本出不来） | 报错原文（落 `logs/d6-dependency-tree.txt` 旁注）；两构件在 2.1 对照表的预期仲裁值 | 网络问题则给 mvn 前置 `export http_proxy=http://127.0.0.1:15236; export https_proxy=http://127.0.0.1:15236` 后重跑（007 应急同款）；版本不符则核 BOM 行号（req 1.3：hibernate 第 68 行、sqlite-jdbc 第 203 行），禁手写版本号救场 |
| R1 | T1 正向组某参数回读不符预期（≠wal/5000/2） | 具体哪个参数、实测值；URL 参数逐字符拼写（`journal_mode` / `busy_timeout` / `synchronous`）；驱动对参数名的大小写处理；`mvn dependency:tree` 里 sqlite-jdbc 版本是否被改动 | 换 spec 2.2 备选路径复测：`SQLiteConfig`（xerial 驱动的配置对象）注入，两路结论都落档；备选路径若与 agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）"禁第二入口"条款冲突，移交 req 第 7 章联动流程，不在 spike 内放宽 |
| R2 | T1 方法 ② 列不到 `-wal`/`-shm` 文件 | journal_mode 回读是否 wal；上下文是否仍在存活（连接是否已全关） | 在连接存活窗口内重查（方法 ② 设计已防）；仍无则把回读输出与目录列表原文落档，按"参数未生效"并 R1 处理 |
| R3 | T3 并发组一命中异常分支（B 未等待直接抛错） | B 的异常类型与消息原文；B 连接 `PRAGMA busy_timeout` 回读是否 5000；A 是否真持写锁（autocommit 已关、INSERT 已执行未提交） | 属预登记分支之一：照实登记 README 附带发现（spec 2.3：两分支都可接受，但必须登记到明确一栏）；不判失败、不重跑改结果 |
| R4 | T3 并发组二未拿到 BUSY（B 等待后成功） | B 实际等待时长（nanoTime 实测值）是否 ≈5000ms；A 占用计时是否真 >5000ms | 核实两组计时后加大 A 占用时长（如 8000ms）重跑一次并记录；两轮结果都落档，结论取实测 |
| R5 | T4 V2 上下文启动报错（非静默、非加列） | 异常类型与消息原文（spec 2.1 (b) 分支明确要求记录） | 结论仍归 (b)：现象=报错，表结构不变；禁止改实体或配置"救场"后再测（那会污染实验条件） |
| R6 | T2 DDL 快照检索不到 AUTOINCREMENT | 建表语句原文（主键列声明长什么样）；IDENTITY 生成的实际 DDL | 结论照实登记为坑（三级归类）+ req 第 7 章联动 5；不改判据、不降级为"通过" |
| R7 | T5 两组数字接近或倍数明显异常（如 FULL 反而更快） | 先回读两连接 `PRAGMA synchronous` 确认 2/1 各就位；确认用的是文件库（非 `:memory:`）；磁盘类型（网络盘会失真） | 排除干扰后机器空闲重跑一次，两轮日志都留档；结论取实测数字，禁估算补数 |
| R8 | T6 全量 `mvn test` 有失败 | 失败测试类与断言消息；单类重跑是否复现 | 确因实验代码缺陷则修复代码重跑全量（修复记录进 README 执行记录）；机制性失败按对应 D 的风险行处理 |
| R9 | 改了代码但测试行为像旧代码（陈旧产物嫌疑） | target/ 内是否有陈旧 .class | `/opt/homebrew/bin/mvn clean` 后重跑；判定只认 mvn 结果（根 CLAUDE.md -「构建验证与 LSP 边界」同一口径） |

## 6. README 结论模板（预填槽位）

执行到 T7 时按本模板落 `README.md`；【槽】处回填实测值。格式对齐 spike/CLAUDE.md - 规则（三问：验证了什么 / 结果如何 / 是否采纳）与 spike/007-react-loop/README.md 的结论留档格式（打勾表 + D 编号决议表 + 证据指针，只学格式不复制内容）。

```markdown
# Spike 结论：SQLite 持久化先导验证（spike/009-sqlite）

> 位置：`spike/009-sqlite/README.md`
> 结论引用方式：实名引用，如"`spike/009-sqlite/README.md` 的 D1 决议"。

## 0. 本文件是什么、为什么做这个实验（零背景读者从这里读）

【槽】一段话：项目第三周要交付 Session 持久化到 SQLite（TechnicalSolution.md - 9.2 SQLite 关系型数据）。
设计上有五项机制挂着"实施期实测"的账（req 1.2 五项挂账）。本 spike 用独立小工程把账提前清掉，
实测六问（D1-D6），全部离线、不调模型 API。
【槽】执行基线一行：执行日期、JDK、Maven 全路径、Spring Boot / hibernate-community-dialects / sqlite-jdbc 三版本。
【槽】术语提示一行：术语表见 req 0.1（本文按同一含义使用，最常用的五个：WAL、pragma、ddl-auto、社区方言、轮原子提交）。

## 一、验证项打勾表（D1-D6）

| 项 | 测试类 | 结果 | 关键证据 |
|---|---|---|---|
| D1 ddl-auto=update 对既有表加不加列 | D1SchemaEvolutionTest | 【槽：结论(a)加列 / 结论(b)不变】 | 【槽：logs/d1-schema-evolution.txt】 |
| D2 WAL 三件套 pragma 走连接串生效性 | D2PragmaEffectivenessTest | 【槽】 | 【槽：logs/d2-pragma.txt】 |
| D3 轮原子提交 + 异常零提交 + 并发 BUSY 面 | D3RoundAtomicCommitTest | 【槽】 | 【槽：logs/d3-round-atomic.txt】 |
| D4 社区方言常规 JPA 面够用性 | D4CommunityDialectJpaTest | 【槽】 | 【槽：logs/d4-dialect-jpa.txt】 |
| D5 synchronous=FULL 提交耗时量级 | D5FsyncTimingTest | 【槽：两组均值 + 是否 <10ms】 | 【槽：logs/d5-fsync-timing.txt】 |
| D6 版本组合整体回归 | 无（命令收口） | 【槽】 | 【槽：logs/d6-dependency-tree.txt、logs/d6-full-test.txt】 |

## 二、决议（D1-D6）

| # | 决议（验证了什么 / 结果如何 / 是否采纳） | 依据 |
|---|---|---|
| D1 | 验证了什么：update 对既有表（实体只加字段）会不会自动加列。结果：【槽：(a) 加列且旧数据保留 / (b) 表结构不变（现象原文：静默跳过或报错原文）】。是否采纳：【槽：(a)→断言过强，触发 req 第 7 章联动 1 改写 TechnicalSolution.md - 9.2 SQLite 关系型数据 工程风险提示段 / (b)→断言成立，维持原文加实测注记（日期）】 | 【槽：logs/d1-schema-evolution.txt】 |
| D2 | 验证了什么：journal_mode=WAL / busy_timeout=5000 / synchronous=FULL 经连接串在 xerial 驱动下是否生效、怎么回读。结果：【槽：逐参数生效/不生效 + 缺省值实测值 + 拼错探针行为】。是否采纳：【槽：生效→兑现 req 第 7 章联动 2 的"实施期核验"注记 / 不生效→备选路径结论并走联动】 | 【槽：logs/d2-pragma.txt】 |
| D3 | 验证了什么：轮原子提交（单事务 5 行、异常零提交）与并发 BUSY 错误面。结果：【槽：正常/异常/两组并发的实测数字与 BUSY 异常类型名 + 消息原文样本】。是否采纳：【槽：机制成立→轮原子提交照第三周设计执行；BUSY 样本供正式实现"报错与有限重试出口"设计引用（req 第 7 章联动 4）】 | 【槽：logs/d3-round-atomic.txt】 |
| D4 | 验证了什么：社区方言 CRUD、四类字段映射、IDENTITY、AUTOINCREMENT、时间往返。结果：【槽：四组观察实测 + 坑清单三级归类（若有）】。是否采纳：【槽：无坑→第三周七张表照此地基开工；有坑→坑位按 req 第 7 章联动 5 进 agentos/CLAUDE.md 第 3 章维护流程】 | 【槽：logs/d4-dialect-jpa.txt】 |
| D5 | 验证了什么：FULL 相对 NORMAL 的提交耗时量级。结果：【槽：两组总耗时与均值、FULL/NORMAL 倍数、21 次提交/轮的开销合计】。是否采纳：【槽：均值 <10ms→维持 TechnicalSolution.md - 9.2 SQLite 关系型数据 现行表述加实测注记；≥10ms→触发 req 第 7 章联动 3 改写】。结论口径：【槽：本机量级实测 + 机型与磁盘类型】 | 【槽：logs/d5-fsync-timing.txt】 |
| D6 | 验证了什么：版本组合整体回归（两构件 BOM 仲裁 + 全量测试）。结果：【槽：dependency:tree 两行 + Tests run 数字，Failures=0、Errors=0】。是否采纳：版本组合结论（Boot 3.5.16 / hibernate-community-dialects 6.6.53.Final / sqlite-jdbc 3.49.1.0）可被第三周直接引用（req 第 7 章联动 6） | 【槽：logs/d6-dependency-tree.txt、logs/d6-full-test.txt】 |

## 三、附带实测发现

【槽：执行中顺带确认的条目，逐条列——例如拼错参数是否有任何报错或日志、synchronous 缺省值实测值、
D3 并发组一实际命中哪个分支、-wal/-shm 的收走时机、方言的其他小差异。每条给证据文件名。】

## 四、结论回填联动清单命中情况（req 第 7 章逐行）

| req 第 7 章行 | 命中 | 状态 / 动作 |
|---|---|---|
| 1 → D1（TechnicalSolution.md - 9.2 SQLite 关系型数据 工程风险提示段） | 【槽】 | 【槽】 |
| 2 → D2（同文档"实施期核验驱动对各参数名的支持"注记） | 【槽】 | 【槽】 |
| 3 → D5（同文档"每轮 2~4 次毫秒级 fsync"量级句） | 【槽】 | 【槽】 |
| 4 → D3（TechnicalSolution.md - 4.3 关键设计点 (2)，BUSY 行为与设计假设不符时） | 【槽】 | 【槽】 |
| 5 → D4（agentos/CLAUDE.md 第 3 章方言坑位） | 【槽】 | 【槽】 |
| 6 → D6（同文档版本组合口径） | 【槽】 | 【槽】 |
| 7 → 附带修正（方言类名笔误 org.hibernate.orm.dialect → org.hibernate.community.dialect，随联动 2/6 一并改） | 【槽】 | 【槽】 |

注：回填动作本身不在本 spike 时间盒内执行（req 第 7 章末行），README 只列清单。

## 五、执行记录

【槽：日期、机器与磁盘（T5-3 三件）、JDK 与 Maven 版本、全量 mvn test 的 Tests run 数字、
重跑与修复记录（若有）、各任务实际耗时】
```

## 7. 实施注意（硬约束继承）

1. **独立 Maven 工程**（spike/CLAUDE.md - 规则）：所有 mvn 命令在 `spike/009-sqlite/` 内执行；本 pom 永不加入仓库根 pom.xml 的 `<modules>`（根 pom 模块固定为 agentos/ 下 9 个）。
2. **禁 git 写操作**：执行本计划全程不做 add / commit / checkout / restore 等任何改变仓库状态的命令；文件只允许在 `spike/009-sqlite/` 内新建或修改。
3. **禁 chat/ 指针**：spike/ 是仓库长期目录，全部产物（README、日志、代码注释）禁止出现指向 chat/ 下任何路径的引用；需要引用评审结论时，把结论本身连同依据（文档章节号、日期）独立成文（spec 3.4 同款条款）。
4. **引用实名**（根 CLAUDE.md -「沟通要点」）：跨文件引用一律"<文档名> - <标题原文>"实名全称形；文件内引用用"<标题原文>"形；裸缩写禁用（本文沿用 spec 0.2 简写规则，简写规则本身已声明）。
5. **无 live 项**：全部离线（req 1.1），不需要 `source` 任何环境变量脚本，不需要任何 API 凭证。
6. **超时兜底**（spike/CLAUDE.md - 编码规范（2026-09-15 起：spike 的 Java 代码同样遵守））：本 spike 无远程调用；测试进程兜底走 surefire `forkedProcessTimeoutInSeconds=180`（pom 已配，2.1）。
7. **断言与日志出口**：断言一律 AssertJ；证据打印一律 SLF4J（stdout 重定向即留档）；禁 System.out 新增（同上规范来源）。
8. **版本仲裁唯一**（agentos/CLAUDE.md - 4 构建、依赖与 CLI）：sqlite-jdbc 不写版本号；hibernate-community-dialects 用 `${hibernate.version}` 且本 pom 禁自定义该属性（禁定义与父链同名 property）；parent 3.5.16 禁擅动；无同 GAV 双版本。
9. **文档冻结**：执行期间不改 docs/design/ 与根 CLAUDE.md；结论回填是评审采纳后的独立修订（req 第 7 章末行），README 只列联动清单。
10. **判定只认 mvn 结果**（根 CLAUDE.md -「构建验证与 LSP 边界」同一判据口径）：LSP / 编辑器提示不作数；疑似陈旧产物先 `mvn clean` 重跑（R9）。

## 8. 命令速查

```bash
cd /Users/ken/Code/idea/agent-os-poc/spike/009-sqlite

# 每次新开终端先执行（JDK 21 对齐）
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 单类实验（证据日志 = 每测试类一个文件）
/opt/homebrew/bin/mvn test -Dtest=D2PragmaEffectivenessTest > logs/d2-pragma.txt 2>&1
/opt/homebrew/bin/mvn test -Dtest=D4CommunityDialectJpaTest > logs/d4-dialect-jpa.txt 2>&1
/opt/homebrew/bin/mvn test -Dtest=D3RoundAtomicCommitTest > logs/d3-round-atomic.txt 2>&1
/opt/homebrew/bin/mvn test -Dtest=D1SchemaEvolutionTest > logs/d1-schema-evolution.txt 2>&1
/opt/homebrew/bin/mvn test -Dtest=D5FsyncTimingTest > logs/d5-fsync-timing.txt 2>&1

# D6 收口
/opt/homebrew/bin/mvn dependency:tree > logs/d6-dependency-tree.txt
grep -nE 'org.xerial:sqlite-jdbc|hibernate-community-dialects' logs/d6-dependency-tree.txt
/opt/homebrew/bin/mvn test > logs/d6-full-test.txt 2>&1
grep -E 'Tests run|BUILD' logs/d6-full-test.txt | tail -10

# 骨架期与排障
/opt/homebrew/bin/mvn test-compile
/opt/homebrew/bin/mvn clean   # 陈旧产物嫌疑时（R9）
```

## 9. 自检记录

起草完成后按下方各命令实测自查（2026-10-09 执行，命令均在仓库根运行，`$P` = `spike/009-sqlite/spec/001-plan.md`、`$S` = `001-spec.md`；数字均为实测值，非手数）。每项一行：检查项 / 实测命令与数字 / 结论。

| 检查项 | 实测命令与数字 | 结论 |
|---|---|---|
| 1 步骤链覆盖（8 任务、每 D 唯一承载） | `grep -c '^### T[0-9]'` = 8（T0-T7）；循环 `grep -c "^### T[0-9] D<d>"`（d 取 D1-D6）各 = 1（T1 D2 / T2 D4 / T3 D3 / T4 D1 / T5 D5 / T6 D6，映射见「1 任务总览（顺序与依赖）」）；矩阵行在「4 测试矩阵」章范围内 `grep -c` 以竖线加 D[1-6] 开头的行 = 6 | 通过：spec 六条款逐一有任务与矩阵行承载、无缺号重号 |
| 2 判据与 spec 逐字一致（关键片段双命中） | 14 个判据片段 `grep -cF` 双侧 ≥1（前 13 个在 spec 与本文双侧、第 14 个连接串全形在 req 与本文双侧）：`PRAGMA journal_mode`=`wal`、`PRAGMA busy_timeout`=`5000`、`PRAGMA synchronous`=`2`、`行数=5、事务内 id 连续`、`回滚后查询行数=0`、`BUSY 异常类型名与消息原文样本落档`、`org.xerial:sqlite-jdbc:jar:3.49.1.0`、`org.hibernate.orm:hibernate-community-dialects:jar:6.6.53.Final`、`Failures=0、Errors=0`、`System.nanoTime`、`-shm`、`< 10ms`、`AUTOINCREMENT` 各双侧命中（spec 1~3 次、本文 1~6 次）；连接串全形 `journal_mode=WAL&busy_timeout=5000&synchronous=FULL` 在 req 与本文各命中 1（spec 以反引号分段写同一组参数）；判据列未改任何数字与阈值 | 通过：矩阵判据逐条转录 spec，零另立口径 |
| 3 引用锚点存在且唯一 | 12 个跨文件锚点逐条 `grep -cF` 目标文件标题行全部 = 1：TechnicalSolution.md 3 个 `###` 级（9.2 / 4.3 关键设计点 / 9.3 未引）中实引 9.2 与 4.3，四级条目简写形 2 个（`#### (2) 消息累积与提交纪律（同会话并发裁决，2026-09-25）`、`#### (2) \`session_messages\` 实体字段（同会话并发裁决，2026-09-25）`，全形标题各命中 1，简写规则承自 spec 0.2）；DemandAnalysis.md 2 个（8.1 / 8.2）；agentos/CLAUDE.md 2 个（3 章 / 4 章）；spike/CLAUDE.md 2 个（规则 / 编码规范）；根 CLAUDE.md 2 个（「沟通要点」/「构建验证与 LSP 边界」）；参照件 spike/007-react-loop/README.md 与同款基线文件（agentos/agentos-storage/pom.xml、agentos/agentos-boot/src/main/resources/application.yaml）`test -f` 全部在盘 | 通过：引用全实名、锚点零失效 |
| 4 禁指针（chat/ 零真指针） | `grep -n 'chat/'` = 1 命中（第 7 章「实施注意（硬约束继承）」第 3 条禁令条款原文，非指针）；本自检行命令文本另含 1 处自指命中；本文其余引用只指向 spike/009-sqlite/ 内文件与 docs/、agentos/ 权威文档、spike/007-react-loop/ 参照件 | 通过：零 chat/ 真指针 |
| 5 命令可执行（环境实测） | `test -x /opt/homebrew/bin/mvn` 退出 0；`JAVA_HOME=$(/usr/libexec/java_home -v 21) /opt/homebrew/bin/mvn -version` 报 `Java version: 21.0.10, vendor: Microsoft`（不设 JAVA_HOME 时同命令报 26.0.2.1，约定 ③ 据此写入）；上游三件套（req / spec / validate/002-validate-spec.md）`test -f` 全在盘 | 通过：命令照抄可跑，JDK 21 对齐步骤必要且已验证 |
| 6 无 spec 外私货验证点 | `grep -c 'D7'` = 0；`grep -c '待-'` = 0（spec 第 6 章两个新增候选验证点未采纳前不排任务，见「1 任务总览（顺序与依赖）」末行）；第 4 章矩阵 6 行与 spec 第 2 章六条款一一对应 | 通过：验证范围未扩项 |
| 7 表达（ASD-STE100 约 80%） | perl -CSD 句长实测（口径与 req/spec 同款：剥离表格行、`#` 标题行、`>` 引用行、反引号代码围栏行、分隔线与空行；去列表标记；按句号/分号/问号/叹号切句；句长按字符数计）：散文句 174 句、超 120 字符 21 句（12.1%）、短句侧 87.9%；超 80 字符 48 句；最长句 343 字符，载荷为骨架设计说明的注解类名与扫描包名清单（@SpringBootConfiguration / @EntityScan / @EnableJpaRepositories 实名锚）；次长 300 字符为环境基线行（版本与命令实测值载荷） | 通过：短句占比 87.9%，达"约 80%"口径线；长句均为引用/括注/坐标载荷 |
