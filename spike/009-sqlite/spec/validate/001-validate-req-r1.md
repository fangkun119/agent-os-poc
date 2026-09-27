# 001-req.md 检查报告（第 1 轮）

> 被核查对象：`spike/009-sqlite/spec/001-req.md`（第 1 轮稿）
> 核查日期：2026-10-09 · 核查人：独立评审 agent（未参与起草）
> 核查依据（前置阅读）：spike/CLAUDE.md、docs/design/TechnicalSolution.md 的「9.2 SQLite 关系型数据」、docs/design/DemandAnalysis.md 的「8.1 性能」「8.2 可靠性」、agentos/CLAUDE.md 的「3 存储与事务（SQLite/JPA）」、spike/007-react-loop/README.md（格式参照）、仓库根 CLAUDE.md「沟通要点」
> 判定口径：按 001-req.md 第 6 章「验收标准」同款精神——问题分阻断 / 非阻断两级；有任一阻断 = 本轮 fail

## 0. 结论速览

| 项 | 结果 |
|---|---|
| 阻断问题 | **1 项**（D2 负向对照的 journal_mode 项按现文执行必然失败，见问题 P0-1） |
| 非阻断问题 | 6 项（N-1 ~ N-6） |
| 附带发现 | 3 项（不属本文档缺陷，A-1 ~ A-3） |
| 本轮判定 | **fail**（修复 P0-1 后可复审） |
| 正文质量底评 | 15 处机制/引文锚点 grep 全部命中；版本三坐标、模块文件数、两笔裁决出处全部实测坐实；五项挂账全覆盖；六条验证问题四段齐全。骨架是好的，问题集中在 D2 实验设计与第 8 章自检记录的计数精度 |

---

## 1. 内容正确（清单项 1）——通过（正文层面）

### 1.1 机制转述逐条 grep 对照（15 处锚点）

命令形：`grep -cF "<引文>" <文件>`，全部在仓库根执行。结果（命中行数）：

| 文件 | 锚点 | 命中 |
|---|---|---|
| TechnicalSolution.md | `只做逐条 `CREATE TABLE`，不涉及 `ALTER`` | 1 |
| TechnicalSolution.md | `实施期核验驱动对各参数名的支持` | 1 |
| TechnicalSolution.md | `每轮 2~4 次毫秒级 fsync` | 1 |
| TechnicalSolution.md | `单写者下事务内行 id 连续` | 1 |
| TechnicalSolution.md | `Session 持久化到 SQLite（含 `tool_invocations`、`llm_calls` 写入）` | 1（L1019，属「13.3 第三周（3 小时）：核心能力五 Web Service」） |
| TechnicalSolution.md | `逐次即时落库` | **2**（L245、L261，两处语义均与转述一致） |
| TechnicalSolution.md | `扩展阶段建议引入 Flyway` | 1 |
| agentos/CLAUDE.md | `拼错整条静默忽略` | 1 |
| agentos/CLAUDE.md | `SQLite 主键走 IDENTITY` | 1 |
| agentos/CLAUDE.md | `持久层抛异常即回滚并放弃该工作单元` | 1 |
| agentos/CLAUDE.md | `写库路径保留报错与有限重试出口` | 1 |
| agentos/CLAUDE.md | `新表带 id、create_time、update_time` | 1 |
| agentos/CLAUDE.md | `禁自建 DataSource Bean` | 1 |
| DemandAnalysis.md | `已写入的 Session 数据保证不丢` | 1（L600） |
| DemandAnalysis.md | `单节点并发 Session 数` | 1（L592，值 ≥ 100，与转述"100 并发"一致） |

结论：正文对三份权威文档的机制转述零错位。

### 1.2 版本与现状坐标实测（1.3 节五条全查）

| 声明 | 实测命令 | 实测结果 |
|---|---|---|
| parent 3.5.16 在根 pom 第 10 行 | `sed -n '1,14p' pom.xml` | 第 10 行 = `<version>3.5.16</version>`，吻合 |
| `hibernate.version`=6.6.53.Final 在 BOM 第 68 行 | `sed -n '68p' ~/.m2/.../spring-boot-dependencies-3.5.16.pom` | `<hibernate.version>6.6.53.Final</hibernate.version>`，吻合 |
| `sqlite-jdbc.version`=3.49.1.0 在 BOM 第 203 行 | `sed -n '203p'`（同上） | `<sqlite-jdbc.version>3.49.1.0</sqlite-jdbc.version>`，吻合 |
| agentos-storage 有 7 实体 + 7 Repository + 1 JpaScheduledTaskStore = 15 文件 | `find agentos/agentos-storage/src -name "*.java" \| wc -l` | 15；分类清点 7/7/1，逐一吻合 |
| hibernate-community-dialects 用 `${hibernate.version}`、sqlite-jdbc 不写版本 | `grep -A2` agentos/agentos-storage/pom.xml | L32 sqlite-jdbc 无版本行；L38 `<version>${hibernate.version}</version>`，与 D6 描述一致 |

### 1.3 两笔挂账出处核实

- 挂账一（D1）：「2026-10-03 设计评审 Q4 裁决：先实测再定稿 ALTER 断言」——评审存档 `chat/temp/review/20261001-design-review-ch09.md` L19 有 Q4 行（"「ddl-auto=update 不涉及 ALTER」断言过强……第三周实测后再改写断言，选 A"），修复方案存档 L23 有裁决原文（"裁决（用户，2026-10-03……）选 A——第三周落 SQLite 时实测一次，按结果改写断言"）。日期、内容均吻合。spike 文档按红线不引 chat/ 路径、只写"结论 + 日期 + 问题编号"的写法正确。
- 挂账五（D5）：「2026-10-06 设计裁决挂账 fsync 数字实测校准」——同评审存档 L10 记"Q9 补充入册（2026-10-06）"，L339 与 q9 修复方案均载"fsync 数字随第三周实测校准"。日期、内容吻合。

### 1.4 方言类名核实（0.1 术语表）

- 实测：`unzip -l hibernate-community-dialects-6.6.53.Final.jar | grep SQLiteDialect` → 类路径为 `org/hibernate/community/dialect/SQLiteDialect.class`。
- 001-req.md 用 `org.hibernate.community.dialect.SQLiteDialect`——**正确**。
- 注意：TechnicalSolution.md - 9.2 SQLite 关系型数据 工程要求段写的是 `org.hibernate.orm.dialect.SQLiteDialect`（L771），与 jar 实测不符，系 TechnicalSolution.md 侧笔误。见附带发现 A-1。

小结：清单项 1 在正文层面通过；但文档第 8 章「自检记录」自身有多处计数与实测不符，记非阻断问题 N-1。

---

## 2. 设计合理（清单项 2）——一项阻断，其余通过

### 2.1 结构完整性

实测：`grep -c '^\*\*验证问题\*\*'` 等四项各 = **6**。D1-D6 四段（验证问题 / 实验方法 / 通过判据 / 优先级）齐全。2.0 节清单总览与各节一一对应。

### 2.2 判据可执行性逐条评估

| 编号 | 通过判据 | 可执行性 |
|---|---|---|
| D1 | PRAGMA table_info 输出二选一 + 旧数据保留性 | 可执行 |
| D2 | 三条 PRAGMA 回读值 + 伴生文件 + 拼错探针 | **部分不可执行**——负向组 journal_mode 判据按现文写法必然达不到，见 P0-1 |
| D3 | 行数=5、回滚后=0、id 连续、BUSY 异常类型与消息样本 | 可执行 |
| D4 | CRUD 断言、DDL 含 AUTOINCREMENT、逐字段往返比对 | 可执行 |
| D5 | 计时数字 + <10ms 判据线（"毫秒级"有量化定义，好） | 可执行 |
| D6 | dependency:tree 版本行 + Tests run 数字 | 可执行 |

### 2.3 阻断问题 P0-1：D2 负向对照的 journal_mode 项技术性错误

**现文**（2.2 节实验方法第 2 步）："负向对照：同一个库用不带参数的连接串回读同样三条 PRAGMA，记录缺省值（journal_mode 预期 delete……）"；通过判据第 1 条："PRAGMA journal_mode 回读 wal（正向组）vs delete（负向组）"。

**实测反证**（macOS 系统 sqlite3 3.51.0，2026-10-09，探针文件建在 `spike/009-sqlite/spec/validate/` 下、测后已删）：

1. 全新文件无参打开 → `journal_mode` 回读 `delete`；
2. 同文件设 `PRAGMA journal_mode=WAL` → 回读 `wal`；
3. 关闭后**同一文件不带参数重开** → 回读 **`wal`，不是 `delete`**；
4. 对照：另一个全新文件无参打开 → `delete`。

**机理**：WAL 是写入库文件头的持久属性，不随连接消失；只有 `delete` 等回滚模式在重开时回落缺省。这是 SQLite 文件格式语义，与用 sqlite3 CLI 还是 xerial JDBC 驱动无关。因此"同一个库不带参数重开读出 delete"不可能发生——照 2.2 节现文执行，负向组 journal_mode 永远读 `wal`，实验者要么误判"参数没生效"，要么卡在判据对不上。

**修复建议**（二选一，推荐前者）：

- 负向对照改用**全新库文件**（从未设过 WAL）："journal_mode 负向证据用全新库文件无参打开回读 delete；busy_timeout 与 synchronous 为连接级参数，同文件重开对照有效"；
- 或在判据里改为："journal_mode：正向组 wal；负向组用全新库文件回读 delete（注明：同文件重开必为 wal——WAL 为文件持久属性，不作负向证据）"。

### 2.4 非阻断问题 N-2：D2 注记 6 的缺省值断言过强

现文（2.2 节注记）："synchronous 的 SQLite 缺省值就是 FULL（2）。此项回读'设定前后相同'属预期"。

**实测**（同上环境）：WAL 模式文件上无参回读 `synchronous` = **1（NORMAL）**；内存库（非 WAL）= **2（FULL)**。SQLite 的 synchronous 缺省值受编译期宏（如 `SQLITE_DEFAULT_WAL_SYNCHRONOUS`）与日志模式影响，不是处处 2。

影响：正向组判据"显式设 FULL 后回读 2"不受影响、依然成立；但注记 6 的断言与"负向组记缺省值、以实测为准"的实测导向自相矛盾，且若负向组真读出 1，实验者会误以为出错。建议改写为："synchronous 缺省值随构建与日志模式而变（官方文档默认 FULL=2；本机系统 sqlite3 实测 WAL 下为 1），以实测为准；生效性差异证明由 journal_mode 与 busy_timeout 承担"。

### 2.5 其余设计点核对（无问题）

- D1 两分支判据均不放宽现行口径，与 Q4 裁决"实测后改写"的动作一致；
- D3 并发两组（等待成功面 / 超时错误面）+ 短事务纪律注记，与 agentos/CLAUDE.md 第 3 章"物理事务尽可能短"条款吻合；
- D5 强制文件库（内存库无真实 fsync）正确；环境记录与本机量级标注到位；
- D6 依赖坐标写法与 agentos-storage pom 实测一致（见 1.2 节）；
- 优先级标注（D1-D4 P0、D5-D6 P1）与"是否阻塞第三周开工"的定义自洽。

---

## 3. 与其他设计无冲突（清单项 3）——通过

| 对照方 | 核查结论 |
|---|---|
| 宪章（.specify/memory/constitution.md） | 该文件当前为空模板（占位符原文在盘，2026-10-09 实读确认；按既有裁决属有意留空、评审后创建）。SQLite 选型的现行权威表述在仓库根 CLAUDE.md「非协商原则」第 6 条。001-req.md 第 8 章自检第 3 行正是按"根 CLAUDE.md「非协商原则」第 6 条"对照——口径正确。本 spike 不改数据库选型、只验证，无冲突 |
| TechnicalSolution.md - 9.2 SQLite 关系型数据 | D1 两分支均落在现行口径内（成立→维持+注记；过强→按 Q4 裁决改写）；D2 按"连接串参数 + 实施期核验"现行写法实测兑现；D5 是裁决本身挂账的数字校准。均无冲突 |
| agentos/CLAUDE.md 第 3 章 | D2 实验按"单一入口"（spring.datasource.url 带参、不自建 DataSource Bean）；D3 注记明写不仿"事务内阻塞"红线；D4 按 IDENTITY 与新表三列条款设计。2.2 节第 5 步把"备选路径若可行但与禁第二入口条款冲突"显式移交第 7 章联动、不在 spike 内自行放宽——潜在冲突点已闭环 |
| spike/CLAUDE.md | 独立 Maven 工程、pom 不进根 modules、README 三问结构、"回填机制结论不回填实验样本成员"（第 7 章六条动作均为机制口径，逐条核对合规）、编码规范适配与 surefire 超时豁免写法与 spike/CLAUDE.md 原文逐条对应。无冲突 |

补充实测：`grep -cE '放宽|换库|弃用' spike/009-sqlite/spec/001-req.md` = 2（L129、L310），两处均为"不在 spike 内自行放宽"的守规表述，非放宽主张。

---

## 4. 完整无遗漏（清单项 4）——通过

五项挂账与验证项映射（1.2 节挂账表逐行 + 第 6 章「验收标准」第 2 条逐条点账）：

| 挂账 | 内容 | 对应验证项 | 覆盖判定 |
|---|---|---|---|
| 一（A） | ddl-auto=update 自动加列 | D1 | 有，实验方法含 V1→V2 加列→回读→首建对照四步 |
| 二（B） | WAL/busy_timeout/synchronous 经连接串生效与回读 | D2 | 有（判据有 P0-1 缺陷，已单列） |
| 三（C） | 轮原子提交整轮事务 + 异常零提交 | D3 | 有，含并发 BUSY 两面 |
| 四（D） | 社区方言 CRUD/类型映射/主键面 | D4 | 有，含 AUTOINCREMENT 落 DDL 实证 |
| 五（E） | fsync 写放大量级 | D5 | 有，FULL/NORMAL 双组对照 + 量级换算 |

D6 为收口项、明确"不对应单笔挂账"，与 1.2 节口径一致。第 6 章「验收标准」五条（六问全答 / 五账全销 / 证据齐备 / README 齐备 / 联动明确）与第 2、5、7 章内容闭合。结论：**五项挂账全覆盖，无遗漏**。

---

## 5. 引用格式（清单项 5）——实质合规，记录层面有小瑕疵

实测（2026-10-09，仓库根）：

| 检查 | 命令 | 结果 | 判定 |
|---|---|---|---|
| chat/ 指针 | `grep -n 'chat/' 001-req.md` | 2 处（L267 = 4.3 节禁令条款原文自身；L312 = 第 8 章自检行自身） | **零真指针，合规**（两处均非指向 chat/ 内容的引用） |
| 裸缩写 TS/DA/AIG/AG | `grep -nE '\b(TS\|DA\|AIG\|AG)\b'` | 仅 L312 自检行自述"TS/DA/AG"字样 | **正文零裸缩写，合规** |
| 跨文件引用形态 | 12 个被引标题逐一 `grep -c` 带级别前缀实测 | 12 个全部 = 1（TechnicalSolution.md 4 个与 DemandAnalysis.md 2 个为 `###` 级；agentos/CLAUDE.md 2 个、spike/CLAUDE.md 2 个、根 CLAUDE.md 2 个为 `##` 级） | 锚点全部真实存在且唯一 |
| 文件内引用形态 | 通读 | 多处以纯节号指代（"1.2 节""2.6 节""第 7 章"） | 记非阻断 N-3 |
| 个别跨文件形态 | 通读 | 见 N-4（两处四级编号省略标题文字；一处未声明的"第 4 章"简称） | 记非阻断 N-4 |

说明：被引标题里最长的一条「13.3 第三周（3 小时）：核心能力五 Web Service」与 TechnicalSolution.md L1015 标题逐字一致。

---

## 6. 表达（清单项 6）——通过，附小建议

### 6.1 句长度量（perl 全文实测，剥离表格/标题/引用行、去列表标记后按句号/分号/问号/叹号切句）

- 散文句总数 **171** 句；超 120 字符 **10** 句（**5.8%**）；超 80 字符 29 句（17.0%）。
- 与第 8 章自检第 6 行声称的"120 句、超 120 字符 22 句（18.3%）"数字不一致——两侧切句与剥离口径不同所致（方法未在文中写明，无法复现）。定性结论一致：短句占比八成以上、长句多为引用锚与括注载荷。归入 N-1 一并复测改写。

### 6.2 五段抽查

| 抽查段 | 评价 |
|---|---|
| 开头「一句话结论」 | 三句、短句为主、信息密度合适，好 |
| 第 2 章开头判据口径段 | "两类结果都算完成……推翻预期同样值钱"——短句、口径清楚，好 |
| D2 实验方法第 5 步（备选路径） | 一句内三个转折，偏长；语义单线、可读，建议拆两句 |
| D4「验证问题」段 | 约 220 字长句（全文第 2 长句），括号嵌套三层；建议拆为"问什么 + 为什么只能实测"两句 |
| 0.1 术语表 | 20 行逐行带白话括注，质量高 |

### 6.3 术语括注缺口（非阻断，随 N-1 顺手补）

首次出现无白话括注的词：**SLF4J**（2.5 节"经 SLF4J 打印留档"）、**AssertJ**（4.2 节）、**surefire**（4.2 节，Maven 的测试执行插件）、**sqlite_master**（2.1/2.4 节，SQLite 存放表结构的系统表）。按本文档自定的"术语首次出现带白话括注"标准补齐即可。

### 6.4 数字模糊检查

全文量级数字均具体（5000ms、1000 次、10ms 判据线、5 行、10 次迭代、15 文件）；"毫秒级"在 D5 有 <10ms 量化判据线，合规。"2~4 次"为 TechnicalSolution.md 原文照抄引用，合规。

---

## 7. 问题清单（修复者按此处理）

### 阻断（1 项）

| # | 问题 | 位置 | 修复建议 |
|---|---|---|---|
| P0-1 | D2 负向对照与判据技术性错误：WAL 是库文件持久属性，同一文件不带参数重开回读仍是 wal（本机 sqlite3 3.51.0 实测：fresh=delete → 设 WAL=wal → 同文件无参重开=wal），"同一个库负向对照读 delete"不可能发生 | 2.2 节实验方法第 2 步 + 通过判据第 1 条 | 负向组的 journal_mode 证据改用**全新库文件**；busy_timeout/synchronous 维持同文件对照（连接级参数，成立）；判据行同步改写 |

### 非阻断（6 项，建议同批修）

| # | 问题 | 位置 | 修复建议 |
|---|---|---|---|
| N-1 | 第 8 章「自检记录」多处计数/方法与实测不符：①行 1 称 7 处 TS 锚点"全部命中 1"——"逐次即时落库"实测 2；②行 3 `grep -cE "放宽\|换库\|弃用"`=0 系命令笔误（ERE 中 `\|` 为字面竖线、模式永不匹配），正写 grep -cE "放宽\|换库\|弃用"（去转义）= 2 且均为守规语境；③行 5 的 `grep -c "^### 标题原文"` 对 6 个 `##` 级标题命不中（结论"锚点存在且唯一"为真——本报告 5 章已逐一实测，但记录的命令不实）；④行 5 "chat/ 字面仅 1 次"实测现为 2（自检行自身文本又添一处，自指陷阱）；⑤行 6 句长数字与本次实测口径不符（171 句/10 句超 120 vs 120 句/22 句） | 第 8 章 | 修完后统一复测重写：命令写成实际可执行形（含正确的转义/前缀），计数重测，并注明剥离口径使句长数字可复现 |
| N-2 | D2 注记 6 断言"synchronous 的 SQLite 缺省值就是 FULL（2）"过强：实测 WAL 下本机读 1（见 2.4 节） | 2.2 节注记 | 改为"缺省值随构建与日志模式而变、以实测为准"，保留正向组显式 FULL 回读 2 的判据 |
| N-3 | 文件内引用多处仅写节号（"1.2 节""2.6 节""第 7 章""第 0 章"），缺标题文字 | 全文散布 | 按仓库根 CLAUDE.md「沟通要点」文件内引用形态补标题文字，如"第 7 章「结论回填联动（预登记）」""1.2 五项挂账（本 spike 要清的账）" |
| N-4 | 跨文件引用形态两处偏差：①"TechnicalSolution.md - 4.3 关键设计点 (2)""9.2 SQLite 关系型数据 (2)"省略四级标题文字（(2) 的标题原文是"消息累积与提交纪律（同会话并发裁决，2026-09-25）"与"(2) `session_messages` 实体字段"）；②2.6 节"agentos/CLAUDE.md 第 4 章构建纪律"——0.2 未声明该简称、且非全名形 | 0.1 术语表、1.2 节、2.3/2.5/2.6 节 | ①补四级标题文字或在 0.2 声明"(N)"简写并全文统一；②改为"agentos/CLAUDE.md - 4 构建、依赖与 CLI" |
| N-5 | D6 "2026-10-01 核验为同线可用版本"不可自含追查：全仓检索 3.49.1.0 仅命中 chat/temp/review/ch09/q7-fix-plan.md、q7-goal-prompt.md 与本文档自身；docs/ 与 001-req.md 内无该核验的可独立核对依据（spike 文档又禁引 chat/ 指针） | 2.6 节验证问题 | 改为自含写法："两构件版本均由 spring-boot-dependencies-3.5.16 BOM 托管（BOM 第 68/203 行，2026-10-09 复测）"，删去无法独立核对的日期式背书 |
| N-6 | 四个术语首次出现无白话括注：SLF4J、AssertJ、surefire、sqlite_master | 2.1/2.4/2.5/4.2 节 | 按本文档自定标准补括注（见 6.3 节给出的白话） |

### 附带发现（不属本文档缺陷，供后续动作）

| # | 发现 | 建议去向 |
|---|---|---|
| A-1 | TechnicalSolution.md - 9.2 SQLite 关系型数据 工程要求段方言类名写作 `org.hibernate.orm.dialect.SQLiteDialect`，jar 实测实际类为 `org.hibernate.community.dialect.SQLiteDialect`（001-req.md 用的是正确名） | 列入 001-req.md 第 7 章联动 2 / 联动 6 的顺带修正项，回填时一并改 |
| A-2 | spike/ 下存在空壳目录 `spike/009-sqllite/`（拼写多一个 l，仅含空 spec/ 子目录，2026-10-09 16:29 创建），与 `spike/009-sqlite/` 并存易混 | 建议删除；超出本评审写入权限（只允许写 spike/009-sqlite/），未动，移交主线处置 |
| A-3 | 本机 ~/.m2 中 xerial 驱动并存 3.49.1.0 与 3.53.2.1 两个版本目录。D6 判据以 BOM 仲裁为准、不受影响；但 D6 执行 `mvn dependency:tree` 时应核对解析结果确为 3.49.1.0（判据已如此写，无需改文） | 无需改文；执行期留意 |

---

## 8. 已核实为正确的要点（修复时无需动）

1. 15 处机制/引文锚点 grep 全部命中，正文对 TechnicalSolution.md、agentos/CLAUDE.md、DemandAnalysis.md 的转述零错位；
2. 版本三坐标（parent 3.5.16 / hibernate 6.6.53.Final / sqlite-jdbc 3.49.1.0）与行号、storage 模块 15 文件清点、storage pom 依赖写法全部属实；
3. 两笔挂账（Q4 2026-10-03、fsync 2026-10-06）的日期与内容均有存档佐证，且"结论 + 日期 + 问题编号"的独立成文写法符合 chat/ 禁指针红线；
4. 六条验证问题四段齐全（四段标记各 6 处）、判据除 P0-1 外均可执行；
5. 五项挂账 A-E 与 D1-D5 一一对应，第 6 章「验收标准」与第 2/5/7 章闭合；
6. 与宪章（经根 CLAUDE.md 压缩原则 6）、TechnicalSolution.md - 9.2、agentos/CLAUDE.md 第 3 章、spike/CLAUDE.md 四方无冲突，唯一潜在冲突点（D2 备选路径）已显式移交联动；
7. 正文零 chat/ 指针、正文零裸缩写、跨文件引用全名形 12 锚点全部真实唯一；
8. 0.1 术语表 20 行逐行带白话括注；方言类名与 jar 实测一致。

## 9. 本报告的实测环境与边界

- 全部命令在仓库根 /Users/ken/Code/idea/agent-os-poc 执行，日期 2026-10-09；
- SQLite 行为实测用 macOS 系统 sqlite3 3.51.0（非 xerial 驱动）：journal_mode 的文件持久性属 SQLite 文件格式语义、与驱动实现无关，足以支撑 P0-1 判定；synchronous 缺省值随构建而变，xerial 自带原生库的实际缺省正是 D2 要实测的内容，不影响 N-2 的"以实测为准"改写建议；
- 探针文件建于 spike/009-sqlite/spec/validate/ 下、测后即删，无残留；未对 spike/009-sqlite/ 之外的任何文件做写入；未执行任何 git 写操作。
