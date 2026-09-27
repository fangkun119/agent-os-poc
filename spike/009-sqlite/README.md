# Spike 结论：SQLite 持久化先导验证（spike/009-sqlite）

> 位置：`spike/009-sqlite/README.md`
> 结论引用方式：实名引用，如"`spike/009-sqlite/README.md` 的 D1 决议"。

## 0. 本文件是什么、为什么做这个实验（零背景读者从这里读）

本项目第三周要交付 Session 持久化到 SQLite（TechnicalSolution.md - 9.2 SQLite 关系型数据）。设计上有五项机制挂着"实施期实测"的账（req 1.2 五项挂账）。本 spike 用一个独立小工程把账提前清掉：实测六个验证问题（D1-D6），全部离线、不调模型 API。本文是结论文件。证据日志在 `logs/`（每个验证项一个文件）。需求、规格、计划三件套在 `spec/`：`spec/001-req.md`（下称 req）、`spec/001-spec.md`（下称 spec）、`spec/001-plan.md`（下称 plan）。下文"req 第 N 章"指 req 的对应章节（如 req 第 7 章 = "spec/001-req.md - 7 结论回填联动（预登记）"）。

执行基线一行：执行日期 2026-10-09；JDK 21.0.10（Microsoft）；Maven 3.9.10（全路径 /opt/homebrew/bin/mvn）；版本组合 Spring Boot 3.5.16 / hibernate-community-dialects 6.6.53.Final / sqlite-jdbc 3.49.1.0（三件均由 Boot BOM 仲裁，核验见 D6 决议）。

术语提示一行：术语表见 spec/001-req.md - 0.1 术语表（本文按同一含义使用）。最常用的五个：WAL（先写日志文件再落数据文件的崩溃安全模式）、pragma（SQLite 连接级配置语句）、ddl-auto（Hibernate 建表改表开关）、社区方言（hibernate-community-dialects 包里的 SQLiteDialect，SQLite 没有官方方言）、轮原子提交（整轮消息在一个事务里一次写入、异常零提交）。

## 一、验证项打勾表（D1-D6）

| 项 | 测试类 | 结果 | 关键证据 |
|---|---|---|---|
| D1 ddl-auto=update 对既有表加不加列 | D1SchemaEvolutionTest | 结论 (a)：加列且旧数据保留（推翻预期，触发 req 第 7 章联动 1） | logs/d1-schema-evolution.txt |
| D2 WAL 三件套 pragma 走连接串生效性 | D2PragmaEffectivenessTest | 三参数全部生效（回读 wal / 5000 / 2）；拼错参数静默忽略坐实；备选路径零启用 | logs/d2-pragma.txt |
| D3 轮原子提交 + 异常零提交 + 并发 BUSY 面 | D3RoundAtomicCommitTest | 机制成立（4 项判据全过）：单事务 5 行、id 连续、回滚零行、BUSY 样本落档 | logs/d3-round-atomic.txt |
| D4 社区方言常规 JPA 面够用性 | D4CommunityDialectJpaTest | 机制面可用 + 两坑登记（AUTOINCREMENT 缺失、LocalDateTime 亚毫秒截断；均"有绕法"、无阻断级） | logs/d4-dialect-jpa.txt |
| D5 synchronous=FULL 提交耗时量级 | D5FsyncTimingTest | FULL 单次均值 0.102 ms / NORMAL 0.031 ms（倍数 3.265）；<10ms 成立 | logs/d5-fsync-timing.txt |
| D6 版本组合整体回归 | 无（命令收口） | 版本仲裁成立；全量 Tests run: 17、Failures: 2（均为 D4 既登记坑断言）、Errors: 0 | logs/d6-dependency-tree.txt、logs/d6-full-test.txt |

## 二、决议（D1-D6）

| # | 决议（验证了什么 / 结果如何 / 是否采纳） | 依据 |
|---|---|---|
| D1 | 验证了什么：update 对既有表（实体只加字段）会不会自动加列。结果：结论 (a)——新列自动出现且旧数据保留。V2 阶段 `PRAGMA table_info` 快照出现第 3 列 remark varchar(255)（cid=3、追加末位，ADD COLUMN 行为特征），V1 三列原样不动；V1 样本行行数 2=2 不变，name/quantity 对照值逐行一致（裸 JDBC 与 V2 仓库 findAll 双路径读回），remark 在旧行上 = null（如实记录）；V2 上下文启动无报错、全程零 WARN/ERROR。首建路径对照（全新文件 d1-fresh.db 直接用 V2 首建）正常：四列齐、建表语句原文已落档。本结论推翻 2026-10-03 设计评审 Q4 的断言"`update` 在 SQLite 上只做逐条 `CREATE TABLE`，不涉及 `ALTER`"。是否采纳：(a)→断言过强，触发 req 第 7 章联动 1 改写 TechnicalSolution.md - 9.2 SQLite 关系型数据 工程风险提示段（回填不在本 spike 内执行）。证据注记：日志中无 Hibernate 发出的 ALTER 语句原文（schema migrator 语句不打 INFO 级日志），(a) 的证据为表结构快照 + 旧行数据完整保留 + remark 列追加末位（cid=3） | logs/d1-schema-evolution.txt（[D1-前置]/[D1-V1基线]/[D1-V2演进]/[D1-V2首建]/[D1-结论] 五段；Tests run: 3, Failures: 0, Errors: 0, BUILD SUCCESS） |
| D2 | 验证了什么：journal_mode=WAL / busy_timeout=5000 / synchronous=FULL 经连接串在 xerial 驱动下是否生效、怎么回读。结果：三参数逐一生效。正向组（生产同款装配、经 HikariCP）回读 `PRAGMA journal_mode`=`wal`、`PRAGMA busy_timeout`=`5000`、`PRAGMA synchronous`=`2`（FULL 官方数值）。负向组：全新文件 d2-fresh.db 无参打开 journal_mode=delete；同文件 d2.db 无参重开 busy_timeout=3000（xerial 驱动缺省，非 SQLite 核心缺省 0）、synchronous=2；同文件重开 journal_mode 仍读 wal（WAL 落库文件头，同文件重开不作负向证据，与 spec 2.2 行为规约注记一致）。文件面：连接存活窗口内目录列表 [d2.db, d2.db-shm, d2.db-wal]，两伴生文件同时在列。拼错探针：URL 写 `journalmode=WAL`（少下划线）回读仍 delete、驱动抛异常=false、全日志无任何 org.sqlite 驱动 warn/error 条目——静默忽略坐实。是否采纳：生效→兑现 req 第 7 章联动 2 的"实施期核验"注记（回填不在本 spike 内执行）；agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）"拼错整条静默忽略"风险提示实测属实；备选路径（R1/SQLiteConfig）零启用 | logs/d2-pragma.txt（第 68-71 行正向组、72-75 行伴生文件、76-80 行负向组、81-83 行拼错探针；Tests run: 5, Failures: 0, Errors: 0, BUILD SUCCESS） |
| D3 | 验证了什么：轮原子提交（单事务 5 行、异常零提交）与并发 BUSY 错误面。结果：四项判据全过。正常路径：单事务写整轮 5 行（1 user + 2 assistant + 2 tool），提交后行数=5，事务内 id=[1,2,3,4,5] 排序后逐个 +1 连续，角色序列=[user, assistant, assistant, tool, tool]（TechnicalSolution.md - 4.3 关键设计点 (2) 的"单写者下事务内行 id 连续"实测成立）。异常路径：事务内插入 5 行后抛 RuntimeException（探针消息"D3 异常路径探针：事务内插入 5 行后主动抛出，预期整轮回滚"），回滚后全表行数=0（agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）"持久层抛异常即回滚并放弃该工作单元"条款实测成立）。并发组一（等待成功面）：命中预登记预期分支（等待后成功，非偏差）——A 持锁实测 1005.059ms（<5000ms），B 从发起 INSERT 到写入成功实测等待 1091.923ms（紧跟 A 提交解除阻塞，+约 87ms 调度延迟），B 连接 busy_timeout 回读=5000，g1-A=1 行、g1-B=1 行。并发组二（超时错误面）：A 持锁 6505.082ms（>5000ms），B 捕获 BUSY——异常类型 org.sqlite.SQLiteException，消息原文"[SQLITE_BUSY] The database file is locked (database is locked)"，结果码 SQLITE_BUSY、vendorErrorCode=5、SQLState=null；B 等待实测 5197.739ms（≈busy_timeout 5000ms）。是否采纳：机制成立→轮原子提交照第三周设计执行；BUSY 样本供正式实现"报错与有限重试出口"设计引用（req 第 7 章联动 4，回填不在本 spike 内执行） | logs/d3-round-atomic.txt（[D3-正常路径]/[D3-异常路径]/[D3-并发组一]/[D3-并发组二] 四段齐全；Tests run: 4, Failures: 0, Errors: 0, BUILD SUCCESS，10.24s） |
| D4 | 验证了什么：社区方言 CRUD、四类字段映射、IDENTITY、AUTOINCREMENT、时间往返。结果：机制面可用 + 两坑登记（均"有绕法"、无阻断级）。机制面：CRUD 四步全过（save 落库 id=1 IDENTITY 回填 → findById 六字段全等 → 修改再 saveAndFlush 再查全等且 update_time 同步刷新 → delete 后查空、全表 count=0）；Boolean/long/String 三类往返全域一致（含 0 与 Long.MAX_VALUE=9223372036854775807 两端、中文引号 emoji 与 72 长串）。坑 1（R6）：方言实际生成 `create table type_probe (id integer, ..., primary key (id))`，AUTOINCREMENT 关键字不存在（两跑确定性复现）；IDENTITY 自增机制本身可用（id 连续回填），缺的是 AUTOINCREMENT 的永不复用语义（sqlite_sequence）；差异=SQLite rowid 的 max+1 可复用已删行 id；影响面=第三周全部 IDENTITY 主键表；归类建议=有绕法（append-only 表 rowid 语义足够，需永不复用语义则须手工补 DDL），最终归类由需求方裁决。坑 2：LocalDateTime 亚毫秒位写入即截断——纳秒样本 .123456789 读回 .123、跨日样本 .999999999 读回 .999（4 处不一致断言留档），毫秒精度样本往返无损；裸连接 typeof() 实录时间三列均存 INTEGER（epoch 毫秒整数，截断发生在写入侧）；影响面=全部 LocalDateTime 列（含 create_time/update_time 时间三件套）；归类建议=有绕法（生产统一 truncatedTo(ChronoUnit.MILLIS) 后落库即同栈无损；确需纳秒须改列映射为 TEXT，代价另评）。是否采纳：有坑→坑位按 req 第 7 章联动 5 进 agentos/CLAUDE.md 第 3 章维护流程（回填不在本 spike 内执行）；第三周表设计按毫秒精度与 append-only 主键语义执行（归类建议，待需求方裁决） | logs/d4-dialect-jpa.txt（终态 run 2：DDL 快照 68-79 行、CRUD 四步 80-86 行、存储形态 89-91 行、逐字段比对 92-93 行、两失败详情 144-149 行；Tests run: 3, Failures: 2, Errors: 0——两失败即坑 1/坑 2 机制断言，R6 纪律不降级） |
| D5 | 验证了什么：FULL 相对 NORMAL 的提交耗时量级。结果：FULL 组 1000 次提交总耗时 102.274 ms、单次均值 0.102 ms（102.3 µs）；NORMAL 组总耗时 31.323 ms、单次均值 0.031 ms（31.3 µs）；FULL/NORMAL 倍数（组 1 总耗时 ÷ 组 2 总耗时）=3.265。量级换算（spec 2.5 行为规约第 3 条公式）：10 次迭代一轮=21 次提交事务，单轮提交开销 21 × 0.102 ms = 2.148 ms，对照 60s 级 LLM 调用可忽略。是否采纳：单次 FULL 均值 0.102 ms < 10ms 成立→维持 TechnicalSolution.md - 9.2 SQLite 关系型数据 现行表述（回填时可加实测注记），不触发 req 第 7 章联动 3。结论口径：本机量级实测（2026-10-09 / Apple M4 / apfs 本地盘）。实验控制局限（如实登记）：执行时机器负载未知——执行 agent 无法替用户关闭浏览器等重 IO 应用，plan T5-1（确认机器空闲）无法落实为可核验状态；已按 R7 口径做替代核验（两组守门回读 synchronous=2/1 就位、两库均为文件库而非 ：memory: 内存库、倍数 3.265 方向正常），详见 logs/d5-fsync-timing.txt 末尾 [D5-执行环境注记] 块 | logs/d5-fsync-timing.txt（判据数字 35/41-45 行；Tests run: 2, Failures: 0, Errors: 0, BUILD SUCCESS） |
| D6 | 验证了什么：版本组合整体回归（两构件 BOM 仲裁 + 全量测试）。结果：仲裁面成立——logs/d6-dependency-tree.txt 终态第 52 行 org.xerial:sqlite-jdbc:jar:3.49.1.0:compile、第 53 行 org.hibernate.orm:hibernate-community-dialects:jar:6.6.53.Final:compile 各恰好 1 行、第二版本计数=0；pom 侧 sqlite-jdbc 不写版本号、hibernate-community-dialects 用 `${hibernate.version}` 且本 pom 零定义该属性（grep -c '<hibernate.version>' = 0），hibernate-core 同为 6.6.53.Final 相互印证——版本由 parent spring-boot-starter-parent 3.5.16 的 BOM 链（spring-boot-dependencies 3.5.16）仲裁。全量回归：Tests run: 17, Failures: 2, Errors: 0, Skipped: 0（BUILD FAILURE，12.456s）；逐类 D3 4/0/0、D1 3/0/0、D2 5/0/0、D5 2/0/0 全绿，D4 3 例中 2 失败=坑 1（DDL 无 AUTOINCREMENT，D4CommunityDialectJpaTest.java:161）+ 坑 2（LocalDateTime 亚毫秒截断，:284，共 4 处 SoftAssertions），失败详情与 T2 登记逐字一致；剔除两坑断言后其余 15 例全绿。两失败按 plan 第 5 章 R8"机制性失败按对应 D 的风险行处理"裁决：归属 D4 既登记坑（R6 断言未降级纪律），非新增回归、零代码改动；单类复跑确定性复现（Tests run: 3, Failures: 2，累计 4 次同结果）。是否采纳：版本组合结论（Spring Boot 3.5.16 / hibernate-community-dialects 6.6.53.Final / sqlite-jdbc 3.49.1.0）可被第三周直接引用（req 第 7 章联动 6）；引用本决议时须如实写"全量 Failures=2=D4 坑 1/坑 2 既登记断言（R6 保留）"，不得写全绿 | logs/d6-dependency-tree.txt（第 52/53 行，BUILD SUCCESS，2026-10-09 20:46:54 终态）、logs/d6-full-test.txt（第 97/243/293/314/374 行逐类、第 424-444 行两失败断言原文、第 456 行汇总行） |

## 三、附带实测发现

1. 拼错连接串参数静默忽略坐实：`journalmode=WAL`（少下划线）回读仍 delete、驱动不抛异常、全日志无任何 org.sqlite 驱动 warn/error 条目。生产排障时"参数写错不会有任何提示"。证据：logs/d2-pragma.txt（81-83 行）。
2. busy_timeout 无 URL 参数时实测缺省=3000ms——这是 xerial 驱动的设定，不是 SQLite 核心缺省 0。生产连接串若漏写 busy_timeout，写冲突等待窗是 3 秒，不是 0 也不是无限。证据：logs/d2-pragma.txt（78 行）。
3. synchronous 无参数时实测缺省=2（FULL）：URL 里的 synchronous=FULL 是显式固定缺省值，而非改变行为。证据：logs/d2-pragma.txt（79 行）。
4. journal_mode 落库文件头：同一文件无参重开仍读 wal。WAL 是库文件持久属性，同文件重开不作负向证据（与 spec 2.2 行为规约注记一致）。证据：logs/d2-pragma.txt（80 行）。
5. -wal/-shm 伴生文件仅在连接存活窗口取证（取证窗口内两文件同时在列）；干净关闭后是否收走未在本次取证范围，不作过度声明。证据：logs/d2-pragma.txt（72-75 行）。
6. D3 并发组一命中预登记预期分支（等待后成功，非偏差）：B 等待 1091.923ms 后写入成功（A 持锁 1005.059ms，紧跟 A 提交解除阻塞，+约 87ms 调度延迟）。证据：logs/d3-round-atomic.txt（[D3-并发组一] 段）。
7. B 的 BUSY 等待实测 5197.739ms，略超 busy_timeout=5000ms（+约 198ms，busy handler 重试节奏粒度所致）。正式实现"报错与有限重试出口"的等待预算应把该超出量计入。证据：logs/d3-round-atomic.txt（[D3-并发组二] 段）。
8. BUSY 重试判别依据（正式实现可引用）：vendorErrorCode==5，或 instanceof org.sqlite.SQLiteException 且 getResultCode()==SQLITE_BUSY；SQLState 为 null，不可依赖。证据：logs/d3-round-atomic.txt（[D3-并发组二] 段）。
9. FULL/NORMAL 写放大在本机 apfs 上实测约 3.3 倍（NORMAL 并非零成本）；WAL 模式下单次 FULL 提交实测约 0.1 ms 量级（Apple Silicon NVMe 上 fsync 极快）。证据：logs/d5-fsync-timing.txt。
10. D4 存储形态实录：LocalDateTime 三列全部存为 INTEGER（epoch 毫秒整数，如 .123456789 落库 1791520496123）而非文本串；Boolean 存 integer 0/1；String 存 text（逐字无损）；long 存 integer。声明类型实录：id=INTEGER、long→bigint、LocalDateTime→timestamp、Boolean→boolean、String→varchar(255)。epoch 毫秒形态暗含墙钟↔UTC 换算进链路（本机 Asia/Shanghai 同栈往返一致；跨时区语义不在本实验范围）。证据：logs/d4-dialect-jpa.txt（89-91 行）。
11. D1 无 ALTER 语句日志原文：schema migrator 语句不打 INFO 级日志，(a) 结论的证据为表结构快照 + 旧行数据完整保留（重建表不可能逐值保留旧行）+ remark 列追加末位（cid=3）。证据：logs/d1-schema-evolution.txt。
12. D1 实现细节偏差登记（不影响实验条件，已写进测试类 javadoc 与日志）：plan 2.3 写的 SpringApplicationBuilder.properties(...) 落在 Boot 外部化配置次序的 defaultProperties（最低位），会被 application.yaml 的 d2.db URL 压制导致串库；实现改用 run("--spring.datasource.url=…") 命令行参数形态（次序高于配置文件），生效 URL 运行时硬断言 + 逐上下文落日志，三次启动均验证覆盖到位（d1.db / d1.db / d1-fresh.db）。证据：logs/d1-schema-evolution.txt、src/test/java/spike/sqlite/D1SchemaEvolutionTest.java。
13. 装配注记（第三周接线同构测试上下文有用）：裸 @SpringBootConfiguration 不含组件扫描，@Service Bean 须显式 @Import 注册（D3 首轮 4 方法全 Errors 的根因）；只依赖 @EnableJpaRepositories 生成仓库 Bean 的上下文（如 D4）不踩此坑。会话级 Spring 上下文缓存与并发实验无相互干扰（并发组用独立文件 d3-conc.db + 裸 JDBC 隔离连接池语义）。证据：logs/d3-round-atomic.txt、本文「五、执行记录」T3 条。

## 四、结论回填联动清单命中情况（req 第 7 章逐行）

| req 第 7 章行 | 命中 | 状态 / 动作 |
|---|---|---|
| 1 → D1（TechnicalSolution.md - 9.2 SQLite 关系型数据 工程风险提示段） | 命中（推翻预期：结论 (a) 加列且旧数据保留） | 待回填：工程风险提示段改写为实测口径（回填动作不在本 spike 时间盒内，见注） |
| 2 → D2（同文档"实施期核验驱动对各参数名的支持"注记） | 命中（三参数逐一生效） | 待回填：注记改为实测结论（各参数名支持情况 + 回读方法 PRAGMA 回读；附缺省值实测 3000/2；回填动作不在本 spike 时间盒内，见注） |
| 3 → D5（同文档"每轮 2~4 次毫秒级 fsync"量级句） | 量级成立（单次 FULL 均值 0.102 ms < 10ms），不触发改写 | 维持现行表述；回填时可加实测注记（2026-10-09 本机量级实测，Apple M4 / apfs；回填动作不在本 spike 时间盒内，见注） |
| 4 → D3（TechnicalSolution.md - 4.3 关键设计点 (2)，BUSY 行为与设计假设不符时） | 命中（BUSY 错误面样本在案） | 待回填/供引用：BUSY 异常类型、消息原文、等待实测与判别依据供正式实现"报错与有限重试出口"设计引用（回填动作不在本 spike 时间盒内，见注） |
| 5 → D4（agentos/CLAUDE.md 第 3 章方言坑位） | 命中（两坑登记：AUTOINCREMENT 缺失、LocalDateTime 亚毫秒截断；均"有绕法"、无阻断级） | 待回填：按 agentos/CLAUDE.md - 维护 的流程在文末注释区「维护记录」增条或注记；坑位最终归类由需求方裁决（回填动作不在本 spike 时间盒内，见注） |
| 6 → D6（同文档版本组合口径） | 命中（版本仲裁成立、组合结论在案） | 待回填：版本组合结论可被第三周直接引用；搬运时须连同"全量 Failures=2=D4 既登记坑断言"一并如实写（回填动作不在本 spike 时间盒内，见注） |
| 7 → 附带修正（方言类名笔误 org.hibernate.orm.dialect → org.hibernate.community.dialect，随联动 2/6 一并改） | — | 已由独立修复方案承接（spike 外，2026-10-09 已裁决）——本 spike 不执行回填 |

注：回填动作本身不在本 spike 时间盒内执行（req 第 7 章末行），README 只列清单。

## 五、执行记录

- 执行日期：2026-10-09（T0-T7 全部任务当日完成）。
- T5-3 环境三件：执行日期 2026-10-09（date +%F）；机型 Apple M4（sysctl -n machdep.cpu.brand_string）；磁盘类型 apfs（diskutil info / 输出中 "Type (Bundle)" 行；本地盘非网络盘）。来源：logs/d5-fsync-timing.txt 末尾 [D5-执行环境注记] 块。
- JDK 与 Maven：每条 mvn 命令前置 export JAVA_HOME=$(/usr/libexec/java_home -v 21)（本机默认 JAVA_HOME 是 JDK 26，不切会跑错版本），mvn 用全路径 /opt/homebrew/bin/mvn，工作目录 spike/009-sqlite/；实测 Apache Maven 3.9.10 / Java version: 21.0.10, vendor: Microsoft。
- 全量 mvn test（T6，logs/d6-full-test.txt）：Tests run: 17, Failures: 2, Errors: 0, Skipped: 0，BUILD FAILURE（Total time 12.456s）。逐类：D3 4/0/0、D1 3/0/0、D2 5/0/0、D5 2/0/0 全绿；D4 3 例 Failures: 2（坑 1 + 坑 2 机制断言，R6 保留）。
- 重跑与修复记录（逐任务）：
  - T0（骨架与依赖预检）：十一步一次通过，无失败分支（未动用 R0~R9）；依赖解析一次过（总耗时约 1.1s，本地仓库已有构件，未触发代理）。
  - T1（D2）：一次通过（Tests run: 5, Failures: 0, Errors: 0，耗时 2.464s），无重跑、无 R 分支触发。
  - T2（D4）：两轮实跑。首跑 3 失败，其中方法②失败属实验代码缺陷（R8）——样本时间混入纳秒，CRUD 链路后段被同一断言截停；修正=②样本改毫秒精度（坑位取证职责归方法③），另方法③存储形态探针前置 + SoftAssertions 全字段收集；断言预期与写入值一字未改，失败照常判失败。首跑日志原样保留为 logs/d4-dialect-jpa-run1.txt；终跑 logs/d4-dialect-jpa.txt：Tests run: 3, Failures: 2, Errors: 0，两失败均为真实机制坑断言（R6 口径，不降级）。
  - T3（D3）：首轮 4 方法全 Errors——根因是裸 @SpringBootConfiguration 无组件扫描、RoundCommitService（@Service）未注册（NoSuchBeanDefinitionException）；修法=内嵌 D3Context 加 @Import(RoundCommitService.class)（显式单 Bean，未开扫描），判据/URL/持锁时长/判读口径零改动，重跑全绿（Tests run: 4, Failures: 0, Errors: 0，10.24s）。并发组二第 1 轮即拿到 BUSY 样本，R4 加大占用（8000ms）重跑分支未触发；R3 偏差分支未触发。
  - T4（D1）：test-compile 先报一次错（SpringApplicationBuilder 的 import 包名误写 org.springframework.boot，正确为 org.springframework.boot.builder），修正后编译绿、一次跑通（Tests run: 3, Failures: 0, Errors: 0，2.488s）；V2 上下文正常启动，R5 分支未触发。
  - T5（D5）：一次通过（Tests run: 2, Failures: 0, Errors: 0，2.977s），R7 未触发、未重跑（单轮日志即终态）。T5-1（关闭重 IO 应用、确认机器空闲）超出执行 agent 能力，按实验控制局限如实登记（见 logs/d5-fsync-timing.txt 末尾注记块），未编造空闲状态。
  - T6（D6）：T6-1 终态 dependency:tree BUILD SUCCESS（0.933s），覆盖 T0 预检版；T6-2 两构件各恰好 1 行、第二版本计数=0；T6-3 全量 mvn test exit=1（Tests run: 17, Failures: 2, Errors: 0）；R8 处置=两失败定性为机制性失败（归属 D4 坑 1/坑 2，T2 已按 R6 登记且断言未降级），零代码改动、R9 未触发；R8 观察点「单类重跑是否复现」已执行——单类复跑 Tests run: 3, Failures: 2、同两方法、确定性复现（累计 T2 两跑 + 全量 + 单类复跑 4 次同结果）。
  - 各任务实际耗时：未记录。
- 证据文件终态：logs/d1-schema-evolution.txt、logs/d2-pragma.txt、logs/d3-round-atomic.txt、logs/d4-dialect-jpa.txt（终态）+ logs/d4-dialect-jpa-run1.txt（首跑旁证）、logs/d5-fsync-timing.txt、logs/d6-dependency-tree.txt（终态覆盖版）、logs/d6-full-test.txt。

## 六、自检记录

对照 spec/001-req.md - 6 验收标准（req 第 6 章「验收标准」）五条逐条自查，2026-10-09 执行，数字为实测值：

| 检查项 | 实测（命令与数字） | 结论 |
|---|---|---|
| 1 六问全答 | D1 结论 (a)（推翻预期）；D2 三参数生效（符合预期）；D3 四判据全过（符合预期）；D4 机制面可用 + 两坑（部分推翻，坑已三级归类）；D5 量级成立（符合预期）；D6 仲裁成立 + 全量数字在案（两失败按 R8 归属 D4 坑，非"没测出来"） | 通过：六问各有确定结论，无"没测出来" |
| 2 五账全销 | 挂账一→D1 结论 (a)；二→D2 三参数逐一生效；三→D3 机制成立 + BUSY 样本；四→D4 机制面可用 + 两坑归类；五→D5 实测数字（0.102 ms / 0.031 ms / 3.265）；D6 为收口项不对应单笔挂账 | 通过：五项挂账逐条有结论销账 |
| 3 证据齐备 | logs/ 下 8 个证据文件在盘（见「五、执行记录」末行清单）；打勾表逐条指向证据文件；全量数字 Tests run: 17, Failures: 2, Errors: 0 已留档。字面偏差如实登记：req 第 6 章第 3 条的"Failures=0"未满足——两失败为 D4 既登记坑断言，按 plan 第 5 章 R8"机制性失败按对应 D 的风险行处理"裁决保留（R6 断言未降级），归属与处置已在 D6 决议与 req 第 7 章联动 5 显式列出、未静默；2026-10-10 收口：用户裁决修订 req/spec 判据（Failures ≤ 2 且仅限 D4 两坑坐实断言），修订注见 001-req.md 第 2.6 节 | 通过（含一项已备案的字面偏差，如实登记、未静默；2026-10-10 修订判据后本偏差关闭——两红为 D4 坑的坐实证据，见 001-req.md 第 2.6 节修订注） |
| 4 README 齐备 | 三问结构完整（第二章决议表逐条"验证了什么/结果如何/是否采纳"）；决议带编号 D1-D6；0 章为零背景读者提供背景、基线与术语提示；格式对齐 spike/CLAUDE.md - 规则（打勾表 + D 编号决议表 + 附带发现 + 执行记录） | 通过 |
| 5 联动明确 | req 第 7 章 7 行逐条列于「四、结论回填联动清单命中情况」，每条标注目标文档与章节；推翻预期的结论（D1 结论 (a)、D4 两坑）已列入联动 1/5、未静默 | 通过 |
| 6 模板与红线 | `grep -c '【' README.md` = 1、`grep -c 'chat/' README.md` = 1——各 1 处命中均为本自检行命令文本自指（与本表行文同源），剥离本行后两计数均为 0：模板槽位零残留、零 chat/ 指针；跨文件引用全实名（TechnicalSolution.md - 9.2 SQLite 关系型数据 / TechnicalSolution.md - 4.3 关键设计点 (2) / agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA）等全称形）；全文数字照录六任务实测 JSON，零改动零估算 | 通过 |
