# 001-req.md 检查报告（第 2 轮）

> 被核查对象：`spike/009-sqlite/spec/001-req.md`（第 2 轮稿）
> 核查日期：2026-10-09 · 核查人：独立评审 agent（未参与起草）
> 核查依据（前置阅读）：spike/CLAUDE.md、docs/design/TechnicalSolution.md 的「9.2 SQLite 关系型数据」全文、docs/design/DemandAnalysis.md 的「8.2 可靠性」与「8.1 性能」、agentos/CLAUDE.md 的「3 存储与事务（SQLite/JPA）」、spike/007-react-loop/README.md（只参照结论留档格式）、仓库根 CLAUDE.md「沟通要点」
> 判定口径：问题分阻断 / 非阻断两级；有任一阻断 = 本轮 fail
> 第 1 轮报告保留为同目录 `001-validate-req-r1.md`（本轮覆盖原路径前先存档）

## 0. 结论速览

| 项 | 结果 |
|---|---|
| 阻断问题 | **0 项** |
| 非阻断问题 | 3 项（N-1 ~ N-3） |
| 本轮判定 | **pass** |
| 第 1 轮阻断项（P0-1：D2 journal_mode 负向对照按现文必失败） | 已修复：D2 实验方法第 2 步改用"全新库文件无参打开读 delete"，同文件无参重开读 wal 的持久属性行为显式写明，判据同步改写。修复方向与第 1 轮建议一致 |

---

## 1. 清单项 1：内容正确——通过

机制/引文转述抽 15 处锚点，逐条 `grep -cF "<引文>" <文件>` 对照原文（仓库根执行），2026-10-09 实测：

| # | 文件 | 锚点引文 | 命中 |
|---|---|---|---|
| T1 | TechnicalSolution.md | `` `update` 在 SQLite 上只做逐条 `CREATE TABLE`，不涉及 `ALTER` `` | 1（L769 工程风险提示段） |
| T2 | TechnicalSolution.md | `实施期核验驱动对各参数名的支持` | 1（L771 工程要求段） |
| T3 | TechnicalSolution.md | `每轮 2~4 次毫秒级 fsync，相对 60s 级 LLM 调用可忽略` | 1（L771） |
| T4 | TechnicalSolution.md | `单写者下事务内行 id 连续` | 1（L808，9.2 (2) 提交纪律） |
| T5 | TechnicalSolution.md | `进程崩溃、总超时打断、LLM 调用报错、轮末提交失败四种异常结束时消息缓冲全部丢弃、零提交` | 1（L245，4.3 (2)） |
| T6 | TechnicalSolution.md | `逐次即时落库` | 2（L245、L261，两处均指 tool_invocations/llm_calls，与转述一致） |
| T7 | TechnicalSolution.md | `INTEGER AUTOINCREMENT` | 1（L802，9.2 (2) id 字段） |
| A1 | agentos/CLAUDE.md | `持久层抛异常即回滚并放弃该工作单元` | 1（L79） |
| A2 | agentos/CLAUDE.md | `新表带 id、create_time、update_time` | 1（L59） |
| A3 | agentos/CLAUDE.md | `拼错整条静默忽略` | 1（L73） |
| A4 | agentos/CLAUDE.md | `HikariCP 不透传独立 pragma` | 1（L72） |
| A5 | agentos/CLAUDE.md | `SQLite 主键走 IDENTITY` | 1（L64） |
| A6 | agentos/CLAUDE.md | `@Transactional 体内禁 LLM 调用、tool 执行、任何阻塞等待` | 1（L76） |
| D1 | DemandAnalysis.md | `已写入的 Session 数据保证不丢` | 1（L600，8.2 可靠性） |
| D2 | DemandAnalysis.md | `单节点并发 Session 数 \| ≥ 100 个`（表行按 `\|` 含空格原样匹配命中） | 1（L592，8.1 性能） |

补充实测（1.3 版本组合基线五条全查）：

- `sed -n '10p' pom.xml` → `<version>3.5.16</version>`（parent 三行块的版本行），与"第 10 行实测"吻合；
- `grep -n "hibernate.version\|sqlite-jdbc" ~/.m2/repository/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom` → L68 `<hibernate.version>6.6.53.Final</hibernate.version>`、L203 `<sqlite-jdbc.version>3.49.1.0</sqlite-jdbc.version>`，与"BOM 第 68 行 / 第 203 行"吻合；
- `find agentos/agentos-storage/src -name "*.java" | wc -l` → **15**；分类清点 entity/ 7 个、repository/ 7 个、JpaScheduledTaskStore.java 1 个，与"7 实体 + 7 Repository + 1 Store"吻合；
- `unzip -l hibernate-community-dialects-6.6.53.Final.jar | grep SQLiteDialect` → `org/hibernate/community/dialect/SQLiteDialect.class` 在包内，联动 7 "TS 9.2 工程要求段类名笔误（现文 `org.hibernate.orm.dialect.SQLiteDialect`，TS L771 实存）"的前提成立；
- agentos/agentos-storage/pom.xml L32（sqlite-jdbc 无版本行）与 L38（`<version>${hibernate.version}</version>`），与 D6"两构件写法"描述吻合。

D2 内嵌的三条 sqlite3 缺省值实测声明，本轮独立复现（同机 sqlite3 3.51.0，2026-10-09）：全新文件 `PRAGMA journal_mode`=delete、`PRAGMA synchronous`=2；同文件设 WAL 后关闭、不带参数重开 `journal_mode`=wal 且 `synchronous`=1；`:memory:` 与全新 delete 文件 `synchronous`=2。四组数字与 2.2 实验方法第 2/6 步所记完全一致，负向对照设计与实测事实相容。

结论：15 处机制/引文锚点 + 5 条版本/现状坐标 + 3 条 sqlite3 缺省值声明全部实测吻合，零错位。

## 2. 清单项 2：设计合理——通过

四段齐全性实测（仓库根）：`grep -c '^\*\*验证问题\*\*'`=6、`^\*\*实验方法\*\*`=6、`^\*\*通过判据\*\*`=6、`^\*\*优先级\*\*：P[01]`=6——D1-D6 每条四段齐备。

逐条判据可执行性核对：

| D | 实验方法 | 通过判据（可执行形） | 优先级 |
|---|---|---|---|
| D1 | 5 步：V1 建基线→V2 只加列重启第二上下文→PRAGMA table_info 对照→全新库首建对照→快照落档 | 二选一分支均可判：(a) table_info 出现新列；(b) 表结构不变（记录实际现象） | P0 ✓ |
| D2 | 6 步：正向回读→负向对照（journal_mode 用全新文件、另两条同文件重开）→`-wal`/`-shm` 文件面→拼错探针→备选 SQLiteConfig→缺省值注记 | 每参数一个回读期望值 + 文件存在性 + 探针行为记录 | P0 ✓ |
| D3 | 5 步：正常 5 行事务→异常零提交→并发组一（占用 < busy_timeout）→并发组二（占用 > 5000ms）→短事务纪律注记 | 行数=5、id 连续、回滚后 0 行、BUSY 异常类型与消息样本，各自落日志 | P0 ✓ |
| D4 | 5 步：四类型实体→CRUD 全链 AssertJ→DDL 回读（AUTOINCREMENT 落 DDL）→往返边界样本→坑位三级归类 | CRUD 零错 + 逐字段往返一致 + DDL 实录 + 坑清单 | P0 ✓ |
| D5 | 5 步：FULL/NORMAL 两组对照→nanoTime 计时→10 次迭代换算→文件库与 IO 控制→环境记录 | 总耗时/均值/倍数落档 + 明确回答"FULL 均值 < 10ms"判据线，两分支都有下文（维持或回填改写） | P1 ✓ |
| D6 | 3 步：依赖坐标写法→dependency:tree 核查→mvn test 全量 | 两构件版本仲裁来源 + Tests run 留档（Failures=0、Errors=0）+ README 决议行 | P1 ✓ |

"两类结果都算完成、唯一不算完成的是没拿到确定答案"的判据口径在第 2 章开头、第 6 章验收标准第 1 条两处一致。优先级定义（P0=不先拿到答案第三周不能开工；P1=不阻塞开工）在术语表 0.1 定义，D1-D4 判 P0、D5/D6 判 P1 的理由各条有写（D5 写明"不阻塞开工、影响性能表述"；D6 写明"收口性质、依赖 D1-D5"），无悬空。

唯一保留意见见 N-2（并发组一判据是纯观察项），不阻断。

## 3. 清单项 3：与其他设计无冲突——通过

逐条对照，全部实测核过原文：

| 对照对象 | 核查点 | 结论 |
|---|---|---|
| 宪章原则 6（根 CLAUDE.md「非协商原则」第 6 条：SQLite 关系持久化 + 审计两表核心阶段写入落库） | spike 不改数据库选型、只验证它；术语表"审计两表"行转述"核心阶段就写入落库"与原文一致 | 无冲突 |
| TechnicalSolution.md - 9.2 SQLite 关系型数据 | D1/D2/D5 三条回填目标都在本节，判据两分支均"成立→维持原文加注记 / 过强→改写"，不单方放宽；存量迁移口径未被触碰；`memory_entries`（第 8 条）、scheduled_tasks/task_executions（第四周收尾，L779-780）、Flyway（扩展阶段建议引入，L769）三条非目标理由均与原文吻合 | 无冲突 |
| agentos/CLAUDE.md - 3 存储与事务（SQLite/JPA） | D2 正向组按"连接参数只走 spring.datasource.url"（L72）单一入口配，备选路径触发时显式移交第 7 章联动流程、不在 spike 内放宽（L73 白名单与"禁第二入口"条款被正面引用）；D3 短事务纪律（L76）以注记形式声明"计时等待只模拟占用、不仿红线行为"；D4 IDENTITY（L64）与"新表带 id、create_time、update_time"（L59）按条款执行 | 无冲突 |
| spike/CLAUDE.md | 4.1 四条工程约束与「规则」两条逐字对应（独立 Maven 工程、pom 不进根 modules、README 三问结论）；4.2 豁免清单与「编码规范（2026-09-15 起：spike 的 Java 代码同样遵守）」的适配/豁免条目一一对应（surefire 兜底、主类包/九模块豁免、AssertJ/SLF4J） | 无冲突 |
| DemandAnalysis.md - 8.1 性能（100 并发）/ 8.2 可靠性 | D2 优先级理由引 8.2 机制前提（WAL+FULL 支撑"已写入不丢"）；3.2 非目标把"100 并发全量压测"明确划出、D5 只测单次提交耗时——不越权替代正式实现的性能验收 | 无冲突 |

D3 实验用裸 JDBC（DriverManager）与 agentos/CLAUDE.md"禁自建 DataSource Bean"不冲突：该条款约束的是应用 Spring 装配入口，实验代码绕开连接池语义是为隔离变量，且文档显式写明动机（"隔离连接池语义"）。

## 4. 清单项 4：完整无遗漏（五账覆盖）——通过

1.2 五项挂账表末列 awk 提取（`awk -F'|' '/^\| [一二三四五] \|/ {print $2 "->" $(NF-1)}'`）实测输出：

```text
一 -> D1
二 -> D2
三 -> D3
四 -> D4
五 -> D5
```

任务书五项挂账 A-E 逐条对得上：A（ddl-auto 加列）→挂账一→D1；B（WAL/busy_timeout/synchronous 连接串生效与回读）→挂账二→D2；C（轮原子提交整轮事务与异常零提交）→挂账三→D3；D（社区方言 CRUD/类型映射/主键面）→挂账四→D4；E（fsync 写放大量级）→挂账五→D5。D6 为版本组合收口项、不对应单笔挂账，第 6 章验收标准第 2 条逐条点账可复核。挂账来源（2026-10-03 Q4 裁决、2026-10-06 fsync 裁决）按红线写成"结论 + 日期 + 问题编号"独立成文，未引任何临时目录路径。

## 5. 清单项 5：引用格式——通过

- **chat/ 指针**：`grep -c 'chat/'` = **2**，逐条验明均非真指针——L268 是 4.3 禁令条款原文本身（"禁止出现指向 chat/ 下任何路径的引用"），L314 是第 8 章自检行的命令文本。真指针 **0**。
- **裸缩写 TS / DA / AIG**：`grep -nE '\b(TS|DA|AIG)\b'` = **0 命中**（exit=1）。
- **跨文件引用形态**：全文出现的跨文件锚点目标实测 13 个（grep -o 提取去重）：TechnicalSolution.md 4 个（9.2 SQLite 关系型数据、4.3 关键设计点 (2)、13.3 第三周（3 小时）：核心能力五 Web Service、9.3 文件系统数据）、DemandAnalysis.md 2 个（8.1 性能、8.2 可靠性）、agentos/CLAUDE.md 3 个（3 存储与事务（SQLite/JPA）、4 构建、依赖与 CLI、维护）、spike/CLAUDE.md 2 个（规则、编码规范（2026-09-15 起：spike 的 Java 代码同样遵守））、根 CLAUDE.md 2 个（非协商原则、沟通要点）。13 个锚点逐一 `grep -cF` 原文标题，全部唯一存在（=1）。形态统一为"<文档名> - <标题原文>"，(N) 四级条目简写形在 0.2 有声明且声明句给出的两个全形与原文四级标题（L243、L798）逐字一致。
- 发现一处自检计数误差，见 N-1（非阻断）。

## 6. 清单项 6：表达——通过

抽查 5 段（1.1 末段"一个执行特点……"、第 2 章开头"每条验证问题按四段写……"、2.2 实验方法第 2 步、2.3 并发组一、3.2 非目标表）：

- 短句直白：抽查段以句号/分号切分的散文句平均长度可读，无连环复句缠绕；2.2 第 2/6 步确有长句，载荷是实测数字、参数名与括注（属引用锚，不可拆），与第 8 章自检行自述一致。
- 生僻术语白话括注：0.1 术语表 19 个术语行逐行有白话解释；正文首次出现处再带括注（如"sqlite_master（SQLite 存放库内全部表结构与建表语句的系统表）"、"AssertJ（Java 断言库……）"、"forkedProcessTimeoutInSeconds"随 surefire 括注）。
- 数字无模糊：连接串参数值（5000）、判据线（<10ms）、样本量（1000 次提交、5 行）、超时（180s）全部给值；"毫秒级"这类定性词在 D5 判据里被换算成可执行数字（10ms 判据线），"60s 级"是原文引文非本文新造模糊。

## 7. 问题清单

### 阻断（0 项）

无。

### 非阻断（3 项）

- **N-1（第 8 章自检计数误差）**：自检第 5 行写"12 个跨文件锚点……agentos/CLAUDE.md 2 个"，实测全文锚点目标为 13 个——agentos/CLAUDE.md 实有 3 个（第 7 章联动 5 的"agentos/CLAUDE.md - 维护"未计入）。13 个锚点本身全部存在且唯一，引用零失效，纯计数误差。建议：把该行改为"13 个……agentos/CLAUDE.md 3 个"。
- **N-2（D3 并发组一判据偏松）**：通过判据写"等待后成功，或异常——哪种都行，如实记录"。作为观察项诚实，但相对其他五条判据偏松。建议：预登记预期分支（busy_timeout=5000 且占用 <5000ms 时，预期 B 等待后成功），并写明"实测若为异常分支，作为与设计假设的偏差照实登记、进 README 附带发现"——判据收紧为"两分支都可接受但须登记到哪一栏"。
- **N-3（锚点书写形式两处小瑕疵）**：① 第 5 章"按 spike/CLAUDE.md - 规则三问组织"把锚点与概括词连写，读作"<文档名> - 规则三问"会找不到标题；建议改"按 spike/CLAUDE.md - 规则 的三问组织"。② 4.3 标题用"根 CLAUDE.md - 沟通要点"连接号形，0.2 与正文他处用「沟通要点」括号形，两种形并存；建议统一为一种（推荐括号形，与 0.2 声明一致）。

## 8. 复审结论

第 1 轮唯一的阻断项（D2 journal_mode 负向对照按现文必失败）已按建议修复且修复设计经本机 sqlite3 实测复现佐证；6 项非阻断问题中，正文层面的修复均已落实（负向对照分类、缺省值不预设注记、判据两分支写法、自检实测化）。本轮实测 0 阻断，判定 **pass**；N-1 ~ N-3 供下一轮修订顺手处理，不阻塞下游 001-spec.md 起草。
