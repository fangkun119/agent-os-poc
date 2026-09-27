# 实施验收报告（第 1 轮）：spike/009-sqlite README 与证据日志

> 位置：`spike/009-sqlite/spec/validate/003-validate-implementation.md`
> 检查日期：2026-10-09 · 轮次：第 1 轮 · 检查人：独立验收 agent（只核查与写报告，未改被验收文件）
> 被验收对象：`spike/009-sqlite/README.md`（下称 README）与 `spike/009-sqlite/logs/` 下 8 个证据日志
> 判据来源（唯一口径）：`spike/009-sqlite/spec/001-spec.md`（下称 spec）第 2 章、`spike/009-sqlite/spec/001-req.md`（下称 req）第 6 章「验收标准」与第 7 章「结论回填联动（预登记）」；任务分解参照 `spike/009-sqlite/spec/001-plan.md`（下称 plan）第 4 章矩阵、第 6 章模板、第 7 章
> **判定结论：pass（阻断 0 项，非阻断 5 项）**——req 第 6 章五条验收逐条核查通过，其中第 3 条存在一项已由 README 如实登记的字面偏差（见问题清单 N-1，属上游判据设计张力，需需求方裁决，不构成本轮阻断）

## 0. 检查范围与方法

- 六项核查清单逐条实测：① 六问全答；② 判据符合（抽 D1/D2/D3/D5 四条与 spec 通过判据逐字对照）；③ 数字保真（10 组 grep 抽样）；④ 五账全销与 req 第 7 章 7 行联动逐行状态；⑤ 红线（chat/ 真指针、错误方言类名、裸缩写）；⑥ 表达（ASD-STE100 抽查）。
- 本报告所有计数与检索结论均为实测（命令与数字随项给出），未手数。
- 本报告沿用被验收三件套自己的简写声明（req / spec / plan 指代 `spike/009-sqlite/spec/` 下三个文件）；"TechnicalSolution.md" 指 `docs/design/TechnicalSolution.md`。

## 1. 核查清单逐项结论

### 1.1 六问全答 —— 通过

- README「二、决议（D1-D6）」表 6 行逐行有明确结论：D1 结论 (a)（推翻预期）、D2 三参数逐一生效、D3 四判据全过、D4 机制面可用 + 两坑三级归类、D5 实测数字 + <10ms 成立、D6 仲裁成立 + 全量数字在案。无一处"没测出来"。
- 实测：`grep -n '没测出' README.md` = 1 命中，即自检表第 1 行转述判据原文"非'没测出来'"处，非含糊结论；`grep -n '待定\|TBD\|无法确定' README.md` = 0。
- 证据文件在盘：`test -f` 对 logs/ 下 8 个文件（d1-schema-evolution.txt、d2-pragma.txt、d3-round-atomic.txt、d4-dialect-jpa.txt、d4-dialect-jpa-run1.txt、d5-fsync-timing.txt、d6-dependency-tree.txt、d6-full-test.txt）+ README + pom.xml 全部为存在，8/8。
- README「一、验证项打勾表」6 行关键证据列逐行指向上述文件。

### 1.2 判据符合（抽 D1/D2/D3/D5）—— 通过（4/4，无含糊第三态）

| 条款 | spec 通过判据 | 实测对照（logs/ 行号实测） | 结论 |
|---|---|---|---|
| D1（spec - 2.1 条款 D1（P0）：ddl-auto=update 对既有表的真实行为） | ① V1/V2 两份 PRAGMA table_info 快照可逐列对照；② 样本行行数不变 + 新列旧行取值如实记录；③ 首建快照显示 V2 全部列；④ 结论明确写 (a) 或 (b) | ① logs/d1-schema-evolution.txt 第 65-69 行（V1 三列）/ 107-111 行（V2 四列）；② 第 112-119 行（COUNT=2、id=1/2 逐行对照、remark=null 如实记录）；③ 第 158-163 行（四列 + 建表语句原文）；④ 第 120 与 169 行明确写 "(a)"，README D1 决议同口径并触发 req 第 7 章联动 1 | 通过 |
| D2（spec - 2.2 条款 D2（P0）：WAL 三件套 pragma 经连接串的生效性） | ① 正向组 wal/5000/2；② 负向组缺省值实测落档；③ -wal/-shm 出现；④ 拼错探针行为在案；⑤ 备选路径仅不生效时启用 | ① logs/d2-pragma.txt 第 69-71 行 = wal/5000/2；② 第 76 行（全新文件 delete）、78-79 行（3000/2）；③ 第 73-74 行（[d2.db, d2.db-shm, d2.db-wal]）；④ 第 81-83 行（回读 delete 不变、抛异常=false、无驱动 warn/error）；⑤ 三参数全生效，备选零启用与判据 ⑤ 条件一致 | 通过 |
| D3（spec - 2.3 条款 D3（P0）：轮原子提交事务与并发 BUSY 面） | ① 行数=5、id 连续；② 回滚后行数=0；③ 并发组一明确标注命中预登记预期或偏差；④ BUSY 异常类型名与消息原文落档；⑤ 四段日志齐 | ① logs/d3-round-atomic.txt 第 75 行（行数=5、id=[1,2,3,4,5]）；② 第 78 行（行数=0）；③ 第 86 行【分支登记】命中预登记预期（等待后成功）；④ 第 97 行（org.sqlite.SQLiteException + "[SQLITE_BUSY] The database file is locked (database is locked)"）；⑤ [D3-正常路径]/[D3-异常路径]/[D3-并发组一]/[D3-并发组二] 四段齐 | 通过 |
| D5（spec - 2.5 条款 D5（P1）：synchronous=FULL 的提交耗时量级） | ① 两组总耗时/均值/倍数落档；② 明确回答 <10ms 与否；③ 数字全实测 | ① logs/d5-fsync-timing.txt 第 35 行（FULL 102.274 ms / 0.102 ms）、41 行（NORMAL 31.323 ms / 0.031 ms）、43 行（倍数 3.265）；② 第 44 行明确回答"是否 < 10ms → 是"；③ 第 46 行声明 System.nanoTime 实测，README 执行记录 T5 同时如实登记 T5-1 机器空闲不可核验的局限（未编造） | 通过 |

D4/D6 未列入逐字抽样（按任务指派），其结论确定性已在 1.1 核过；D6 全量 Failures=2 的字面偏差见 N-1。

### 1.3 数字保真 —— 通过（10 组抽样全部一致）

| # | README 数字 | 实测命令（在 spike/009-sqlite/ 内） | 实测结果 |
|---|---|---|---|
| 1 | busy_timeout 无参缺省=3000 | `grep -n 'busy_timeout = 3000' logs/d2-pragma.txt` | 第 78 行命中 |
| 2 | D3 B 等待 5197.739ms / A 持锁 6505.082ms | `grep -c '5197\.739' logs/d3-round-atomic.txt`、`grep -c '6505\.082' …` | 各 3 次命中（第 93/95/97 与 94/95/97 行） |
| 3 | D3 组一 1091.923ms / 1005.059ms | 读 logs/d3-round-atomic.txt 第 84-86 行 | 逐字一致（+约 87ms 调度差 = 1091.923−1005.059，算术核对成立） |
| 4 | D5 102.274 / 31.323 / 3.265 / 2.148 | `grep -c '102\.274' logs/d5-fsync-timing.txt` 等 | 2 / 2 / 3 次命中；2.148 在第 45 行（21 × 0.102 算式在案） |
| 5 | D1 remark 列 varchar(255) 追加末位（cid=3）、行数 2=2 | `grep -c 'name = remark，声明类型 = varchar(255)' logs/d1-schema-evolution.txt`、`grep -c '实测 = 2，步骤①插入 = 2' …` | 2 次 / 1 次命中 |
| 6 | D4 纳秒 .123456789 读回 .123 | `grep -c '2026-10-09T12:34:56.123456789 读回 2026-10-09T12:34:56.123' logs/d4-dialect-jpa.txt` | 命中；跨日 .999999999→.999 同核（第 93 行，4 处不一致断言与失败详情 4 failures 一致） |
| 7 | D4 epoch 毫秒实录 1791520496123 | `grep -c '1791520496123' logs/d4-dialect-jpa.txt` | 第 90 行命中 |
| 8 | D6 两构件各恰好 1 行、第 52/53 行 | `grep -c 'org.xerial:sqlite-jdbc' logs/d6-dependency-tree.txt`、`sed -n '52,53p' …` | 计数 1/1；两行坐标与 README 逐字一致（3.49.1.0 / 6.6.53.Final） |
| 9 | D6 全量 Tests run: 17, Failures: 2, Errors: 0、12.456s、逐类行号 97/243/293/314/374/456 | `grep -n 'Tests run' logs/d6-full-test.txt`、`grep -n 'Total time' …` | 全部命中且行号与 README 引用逐一相同 |
| 10 | pom 零定义 `<hibernate.version>` 属性 | `grep -c '<hibernate.version>' pom.xml` | = 0；`${hibernate.version}` 使用在第 41 行（pom.xml 第 33 行 sqlite-jdbc 不写版本号） |

首跑旁证：logs/d4-dialect-jpa-run1.txt 第 86 行 `Tests run: 3, Failures: 3` 与 README 执行记录 T2"首跑 3 失败"一致；3 失败 = 坑 1（第 87 行）+ 方法②实验代码缺陷（第 99 行）+ 坑 2（第 111 行），与"修正=②样本改毫秒精度、断言预期一字未改"的叙述吻合。

### 1.4 五账全销与 req 第 7 章联动 7 行 —— 通过

- 五账（req - 1.2 五项挂账（本 spike 要清的账））：一→D1（结论 (a)）、二→D2（三参数逐一生效）、三→D3（机制成立 + BUSY 样本）、四→D4（两坑登记）、五→D5（实测数字）——README「六、自检记录」第 2 行逐条点账，决议表逐条有结论。
- req 第 7 章 7 行：README「四、结论回填联动清单命中情况」表 7 行逐行有命中与状态（1/2/4/5/6 待回填、3 维持不改写、7 已由 spike 外独立修复方案承接）。实测表行数 = 7。
- 联动 7 状态佐证：TechnicalSolution.md 第 771 行当前仍带笔误类名 org.hibernate.orm.dialect.SQLiteDialect——与"回填动作不在本 spike 时间盒内"（req 第 7 章末行）一致，非矛盾；该修正属 spike 外待办、尚未落地。

### 1.5 红线 —— 通过（3 项全零）

| 红线 | 实测命令 | 结果 |
|---|---|---|
| chat/ 真指针 = 0（禁令/自检条款自身除外） | `grep -rn 'chat/' README.md logs/` | README 1 命中 = 自检表第 6 行命令文本自指（豁免）；logs/ 0 命中 |
| 错误类名 org.hibernate.orm.dialect.SQLiteDialect = 0 | `grep -rn 'org\.hibernate\.orm\.dialect\.SQLiteDialect' README.md logs/` | 0 命中；实际在用的是正确类名 org.hibernate.community.dialect.SQLiteDialect（src/test/resources/application.yaml 第 8 行）。README 联动 7 行出现的"org.hibernate.orm.dialect"前缀（无类名后缀）系转述 req 第 7 章行 7 规定动作原文，非使用错误类名 |
| 裸缩写 TS / DA / AIG = 0 | `grep -rnE '\bTS\b|\bDA\b|\bAIG\b' README.md logs/` | 0 命中，无豁免项 |

附带核过：模板槽位【】残留 = 0（`grep -n '【' README.md` 仅自检行命令文本 1 处自指），与 README 自检第 6 行声明一致。

### 1.6 表达（ASD-STE100 抽查 5 段）—— 通过

- 抽样 5 段散文（README 第 8/10/38/39/43 行：背景段、执行基线行、附带发现第 1/2/6 条），句长按字符数计、120 字符为线：5 段合计 19 句，超 120 字符 0 句，短句侧 100%，最长句 118 字符（载荷为版本组合坐标，规范要求的实名引用锚）。
- 全文散文总口径（perl -CSD 实测：剥离表格行、`#` 标题行、`>` 引用行、代码围栏行、分隔线与空行；按句号/分号/问号/叹号切句）：93 句、超 120 字符 8 句（8.6%）、短句侧 91.4%，高于 req/spec/plan 三件套自检同用的"约 80%"口径线；8 个长句载荷均为证据坐标、类名与连接串实名锚。

## 2. 问题清单

阻断项：0。

非阻断 5 项（不清零不影响本轮 pass；N-1 需求方裁决，其余为表述精度建议）：

- **N-1（需裁决，已由 README 备案）**：D6 全量回归 Failures=2 与 spec - 2.6 条款 D6（P1）：版本组合整体回归 通过判据 2 及 req 第 6 章「验收标准」第 3 条"Failures=0"字面不符。README 在打勾表 D6 行、D6 决议、执行记录 T6、自检第 3 行四处如实登记，按 plan 第 5 章 R8 裁决归属 D4 既登记坑（R6"断言不降级"纪律所致——两坑断言按 R6 必须保持红色，使 D6 判据在坑存在时结构性无法满足，属上游判据设计张力，非执行舞弊或含糊第三态）。回填联动 6 时必须连同"全量 Failures=2=D4 坑 1/坑 2 既登记断言"一并如实搬运（README 已自行写明该要求）；最终收口方式（维持登记口径，或修订 D6 判据表述为"除已登记坑断言外全绿"）留需求方裁决。
- **N-2（行号引用差一）**：README D2 决议证据栏写"72-75 行伴生文件、76-80 行负向组"，实测伴生文件段为 72-74 行、负向组段为 75-80 行（logs/d2-pragma.txt），各差一行；证据可检索性不受影响。
- **N-3（压缩引用）**：README D4 决议写"两失败详情 144-149 行"，实测 144 与 149 行为两条失败的首行，完整断言原文延伸至 174 行（logs/d4-dialect-jpa.txt）；不误导，建议回填引用时写"144-174 行"。
- **N-4（表述偏松）**：README 执行记录 T6"累计 T2 两跑 + 全量 + 单类复跑 4 次同结果"——首跑（logs/d4-dialect-jpa-run1.txt，Failures: 3）含第三处实验代码缺陷失败，与终态 Failures: 2 非同结果；"同结果"仅在"坑 1/坑 2 两断言四次复现"意义上成立（实测成立：run1 第 87/111 行即两坑断言）。回填引用时建议精确为"两坑断言 4 次复现"。
- **N-5（跨跑数字提示）**：D5 决议数字取自专跑日志（102.274/31.323/倍数 3.265），T6 全量回归中 D5 重跑样本不同（倍数 2.747，logs/d6-full-test.txt 第 314 行前的 [D5-R7] 行）——计时实验正常波动，README 证据指向正确；提示回填读者勿跨跑比对期望相等，也提示量级结论以"约 0.1 ms 量级"表述为稳。

## 3. 判定

- req 第 6 章五条验收逐条判定：① 六问全答通过；② 五账全销通过；③ 证据齐备通过（含 N-1 一项已备案字面偏差，未静默）；④ README 齐备通过（三问结构、D1-D6 编号、零背景章 0、格式对齐 spike/CLAUDE.md - 规则）；⑤ 联动明确通过（7 行逐行有状态，推翻预期项 D1/D4 已列入联动 1/5）。
- 六项核查清单全过，红线零违例，数字保真零改动，阻断 0 项 → **verdict = pass**。
- 本轮检查未修改 README、logs/ 与实验代码；报告落盘于本文件。

## 4. 自检记录（本报告）

| 检查项 | 实测命令与数字 | 结论 |
|---|---|---|
| 证据文件在盘 | `test -f` 对 logs/ 8 文件 + README + pom.xml | 10/10 存在 |
| 判定引用可复现 | 第 1.2/1.3 节全部行号经 sed/grep -n 实测（52/53、69-71、75、78、97、35/41/43/45、65-69、107-111、112-119、158-163、120/169、84-86、86-87、93-94、97、112-113、158-163 等） | 命中率 100% |
| 本报告红线 | `grep -n 'chat/'` 本文件 = 3 命中（第 11 行清单项点名红线本身、第 62/97 行两条检查命令文本，均非指针）；错误类名 = 2 命中（第 63 行检查命令文本、第 56 行转述 TechnicalSolution.md 第 771 行笔误现状的核实记录，均非使用错误类名）；裸缩写 = 1 命中（第 64 行检查命令文本及其点名红线本身，非用缩写指代文档）；跨文件引用全实名形 | 通过（命中均为命令文本与点名红线的自指/核实性转述，与 req - 8 自检记录、spec - 7 自检记录的豁免口径一致） |
