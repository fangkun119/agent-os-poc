# Spike 实施计划（002-plan）

> 位置：`spike/007-react-loop/spec/002-plan.md`
> 创建：2026-09-14 · 状态：待执行
> 上游：`spec/002-req.md`（下称 req，验收源头与决议规则）、`spec/002-spec.md`（下称 spec，工程形态与判定细则）
> 格式底稿：第一组计划（`spec/001-plan.md`，其任务化结构与本计划同构；从未提交，2026-09-15 删除——已被 002- 替代）
> 约定：① 所有命令在 `spike/007-react-loop/` 目录内执行；② mvn 一律用全路径 `/opt/homebrew/bin/mvn`（2026-09-14 定案，不建软链）；③ live 任务前先 `source ~/.agent-os-poc/script/agent-os-env.sh`（必须 source 不能执行）；④ 术语与验收标准沿用 spec——本计划只做任务化，**完成判定不弱于 spec §4 任何一条**

## 0. 文档定位与前置状态

- 本计划回答三件事：**按什么顺序做**（§1）、**每步敲什么命令**（§3）、**怎么算完成**（§3 各任务判定 + §2 分支规则）。
- **整体 DoD**：T0-T6.5 全部完成、或按 §2 分支规则写明跳过理由 + T7 的 README 增补节落盘 + req 4.4 五项产出物齐 + 联动清单命中勾选（spec §7.2 ⑤）。
- **前置状态（2026-09-14 核验）**：
  - V0 已完成：路径 C 出局，证据 `logs/r2/v0-connector-search.txt`（含两次补充：原生端点探测、委托链源码）
  - 四腿凭证就位：`MINIMAX_*` / `OPENAI_*` / `ANTHROPIC_*` / `ZHIPU_*` 四元组已入脚本，key 均已填入并核验（ZHIPU `0c91b***`）
  - 时间盒：**2-3 小时**（req 4.3），到盒即停：已完成项落 README、未完成项写明状态上报评审、不自动延期
  - 各任务参考耗时见 §1（合计约 2.5h，含重试余量偏紧——超盒优先保 T7 收口）
  - **方案 B 已执行（2026-09-14）**：第一组 `src/` 从工作区移除（代码全量保留于 git 历史，提交 2a01bee；15 个源文件：7 个主类 + 1 个配置 + 7 个测试类），工作区零残留；本轮代码 100% 新写、自含启动类（App）与全量配置（application.yaml）

### 0.1 依赖与版本基线（T0-5 依赖树核对的对照清单）

| 构件 | 版本写法 | 预期仲裁结果 | 出处 |
|---|---|---|---|
| parent `spring-boot-starter-parent` | **3.5.16**（锁定） | — | req 4.1 第 4 条；007 README D1 |
| `java.version` | **21** | — | 同上 |
| `spring-ai-bom` | **1.1.2**（import） | — | pom 现状；007 README D2 |
| `spring-ai-alibaba-bom` | **1.1.2.0**（import） | — | 同上 |
| `spring-ai-starter-model-openai` | 不写版本号 | 1.1.2（第一组 E1 已实测） | 同上 |
| `spring-ai-starter-model-anthropic` | 不写版本号 | 1.1.2（第一组 E1 已实测） | 同上 |
| `spring-ai-starter-model-minimax` | 不写版本号（**本轮新增**） | **预期 1.1.2，V1 实测确认**——解析不出即路径 B 出局 | spec §1.2；req 3.1 V1 |
| `spring-ai-alibaba-graph-core`（test） | 不写版本号（探针） | 1.1.2.0（第一组 E1 已实测） | pom 注释；README 第一节存档 E1 行 |
| `spring-boot-starter-test`（test） | 不写版本号 | parent 3.5.16 管理的对应版本 | pom 现状 |

版本纪律（对应 agentos/CLAUDE.md 第 86/87 条同款原则）：三方依赖一律 BOM 仲裁、pom 内无手写版本号、无同一 GAV 双版本；版本线不擅自升级（SAA 1.1.2.2 / 1.1.2.3 差异只记录，属 V8 决议内容，req 1.3）。

## 1. 任务总览（顺序与依赖）

```text
T0 pom+yaml+V1 解析与启动（离线，~25min）   ← 唯一入口，不需要 key
 └→ T1 V2 属性面（live，~20min）            ← 需要 key
     └→ T2 组件五类（纯编码，~30min）
         ├→ T3 V3+V7 单工具闭环（live，~15min）
         │   └→ T4 V4 手动循环计数（live，~15min）
         │       └→ T5 V5 显式映射复验（live，~15min）
         │           └→ T6 V6 usage/耗时双腿（live，~15min）
         │               └→ T6.5 V9 双 OpenAI 实例 + 智谱（live，~20min）
         │                   └→ T7 V8 README 收口（~20min）
```

- 线性推进：前一任务完成判定未过、且不命中 §2 分支规则时，不进入下一任务。
- **V9 挂在主链末端只为编码依赖（需要 T2 组件与 T5 的 Bean 名经验），其结论独立于 A/B/C 决议**（req 3.1 前置依赖节）——V1/V2 失败时 V9 照跑，见 §2。
- 方案 B 后 工程完全自含：T0 一次建齐 pom 依赖 + App 启动类 + application.yaml 全量配置（原"yaml 提前到 T0"的顺序微调已并入此整合，profile 机制不再需要）。

## 2. 分支规则（执行前先读，防现场拍脑袋）

| 触发 | 动作 |
|---|---|
| T0 的 V1 失败（starter 解析不出 / 容器起不来） | **路径 B 出局**：跳过 T1、T3-T6；T2 照做（V9 需要）；直接跳 T6.5 跑 V9；然后 T7 收口。解析报错原文落 `logs/r2/v1-resolve.txt` |
| T1 的 V2 失败（两候选 base-url 均不通） | 同上口径：V3-V6 跳过、V9 照跑、T7 收口。两候选实测结果照常落 `logs/r2/v2-properties.txt` |
| 任一 live 任务失败 | 失败项如实落盘（响应原文、HTTP 码、`base_resp` 内容），**不阻塞后续独立项**；模型侧偶发失败允许固定次数（≤3）重跑并记录 |
| 任何路径违反 req 4.1 八条硬约束 | 该路径否决、上报，不进入候选（req 3.3 末行） |
| 时间盒到 | 立即停在当前任务，跳 T7 收口（已完成项落 README，未完成写状态） |
| V6 的 anthropic 国内站探针失败 | 结论如实落盘并把 application.yaml 的 anthropic base-url **改回国际站值**（spec §4 V6 行）；定稿 / 根 CLAUDE.md 的三处国内站记载需回改并另报评审（spec §7.2 ⑥ 约定） |

## 3. 任务明细

### T0 pom + App + 全量 yaml + V1（离线，不需要 key）

| # | 步骤 |
|---|---|
| T0-1 | `pom.xml` 只增一条依赖：`org.springframework.ai:spring-ai-starter-model-minimax`（**逐字照抄 spec §1.2 的注释块**，不写版本号交给 BOM 仲裁）；其余一切不动 |
| T0-2 | 写 `src/main/java/spike/reactloop/App.java`：`@SpringBootApplication` 最小启动类（spec §3.0） |
| T0-3 | 新建 `src/main/resources/application.yaml`（**全文照抄 spec §2.1**：三腿全量 + retry 关闭） |
| T0-4 | 写 `src/test/java/spike/reactloop/V1BootstrapTest.java`：`@SpringBootTest(NONE)` + `contextStarts` 用例（无 profile 机制，application.yaml 即唯一配置） |
| T0-5 | `/opt/homebrew/bin/mvn dependency:resolve > logs/r2/v1-resolve.txt` |
| T0-6 | `/opt/homebrew/bin/mvn dependency:tree -Dverbose > logs/r2/v1-dependency-tree.txt` |
| T0-7 | `/opt/homebrew/bin/mvn test -Dtest=V1BootstrapTest` |

**完成判定**（= spec §4 V1 全项）：resolve 成功且 minimax starter 构件版本落 **1.1.2**；依赖树无新增 "omitted for conflict"；测试绿。失败 → §2 分支规则第一行。**证据**：`v1-resolve.txt`、`v1-dependency-tree.txt`。
顺带观察（spec §6 开放项 4）：minimax 自动配置在 placeholder key 下容器是否正常启动，结论记入 v1-resolve.txt 尾部。

### T1 V2 属性面（live）

| # | 步骤 |
|---|---|
| T1-1 | `source ~/.agent-os-poc/script/agent-os-env.sh` |
| T1-2 | 写 `V2MinimaxPropertiesTest.java`：① model 绑定断言（离线可先单跑）；② base-url 负向探针（**执行后修订**：本机 fake-IP 代理会解析任意假域名，改为 `https://127.0.0.1:1`——必拒连且 localhost 不走代理；手动构造的 ChatModel 须显式传 maxAttempts=1 的重试模板，否则被默认 10 次退避链拖满 surefire 180 秒）；③ base-url 候选判定（候选 A `https://api.minimax.cn` 失败才试候选 B 带 `/v1`；切换方式 = 改 application.yaml 的 base-url 值后重跑本测试类）；④ `spring.ai.minimax.chat.base-url` chat 级覆盖属性绑定表现 |
| T1-3 | `/opt/homebrew/bin/mvn test -Dtest=V2MinimaxPropertiesTest > logs/r2/v2-properties.txt` |

**完成判定**（= spec §4 V2）：属性表四项各标"可配 / 不可配 / 默认值 / 实测表现"，含源码口径（原生路径 `/v1/text/chatcompletion_v2`）与运行时实测的对照结论；胜出候选记录在案供 T3-T6 使用。失败 → §2 分支规则第二行。**证据**：`v2-properties.txt`。

### T2 组件五类（纯编码，不运行）

| # | 步骤 |
|---|---|
| T2-1 | `CountingTools`（spec §3.1：固定返回 + 计数器 + lastArgs） |
| T2-2 | `ManualLoop` + `LoopResult`（spec §3.2：D3 循环路径 + 逐轮计时/usage + per-call options 可覆盖模型名 + 上限 10 不抛异常） |
| T2-3 | `ProviderRegistry`（spec §3.3：四键 `openai`/`anthropic`/`minimax`/`zhipu`；**避雷条款逐字落实**——zhipu 实例在 Map Bean 方法体内局部构造；构造细则：baseUrl 读 `ZHIPU_BASE_URL` 去尾斜杠 + `completionsPath("/chat/completions")` + key 读 `ZHIPU_API_KEY` + model 读 `ZHIPU_DEFAULT_MODEL`） |
| T2-4 | `ThinkStripper`（spec §3.4：`(?s)<think>.*?</think>` 剥离后 trim） |
| T2-5 | `/opt/homebrew/bin/mvn test-compile`（编译载体，不跑测试） |

**完成判定**：编译绿；spec §3.3 两条硬性条款（避雷、局部构造）在代码里可指认。**证据**：无日志（编译输出即可）。

### T3 V3 + V7 单工具闭环（live）

| # | 步骤 |
|---|---|
| T3-1 | 写 `V3SingleToolLoopTest.java`：MiniMax 腿（键 `minimax`）+ per-call options 覆盖 `MiniMax-M3`；CountingTools 单工具闭环；断言前 strip；剥离前后文本、有无 `<think>` 残留、与第一组差异**全部打印**（V7 证据流，spec §4.2 细则 2，不重复烧调用） |
| T3-2 | `/opt/homebrew/bin/mvn test -Dtest=V3SingleToolLoopTest > logs/r2/v3-tool-loop.txt`，打印段摘出存 `logs/r2/v7-think-observations.txt` |

**完成判定**（= spec §4 V3 + V7）：`strip(finalText)` 含"12°C"或"晴"；工具执行全部由我方代码触发；剥离后无 `<think>` 残留；V7 观察落盘。**证据**：`v3-tool-loop.txt`、`v7-think-observations.txt`。

### T4 V4 手动循环计数（live）

| # | 步骤 |
|---|---|
| T4-1 | 写 `V4ManualLoopCountTest.java`：同 T3 配置，按 E3 计数法断言"计数器读数 = 日志中模型发起工具调用轮数"（req 3.1 V4） |
| T4-2 | `/opt/homebrew/bin/mvn test -Dtest=V4ManualLoopCountTest > logs/r2/v4-manual-count.txt` |

**完成判定**（= spec §4 V4）：计数吻合、无双执行；`internalToolExecutionEnabled(false)` 在 MiniMaxChatModel 上行为与 openai 腿一致的结论落盘（spec §6 开放项 5 关闭）。**证据**：`v4-manual-count.txt`。

### T5 V5 显式映射复验（live）

| # | 步骤 |
|---|---|
| T5-1 | 写 `V5ProviderRegistryTest.java`：先打印 `context.getBeanNamesForType(ChatModel.class)` 留档（诊断动作，Javadoc 写明与"禁类型扫描"的区分，spec §3.3）；断言四键齐且互不混淆；`minimax` 与 `openai` 两键按名各跑 V3 同款闭环 |
| T5-2 | `/opt/homebrew/bin/mvn test -Dtest=V5ProviderRegistryTest > logs/r2/v5-registry.txt` |

**完成判定**（= spec §4 V5）：四键存在、两键闭环、MiniMax 腿 Bean 名落档（spec §6 开放项 3 关闭）、无类型扫描取用。**证据**：`v5-registry.txt`。

### T6 V6 usage/耗时双腿（live）

| # | 步骤 |
|---|---|
| T6-1 | 写 `V6UsageCaptureTest.java`：`minimax` 腿（缺省模型 M2.7，即三级选择第 1 级真调用）+ `anthropic` 腿（M3，**配置即国内站**）各采一组 (usage, duration) |
| T6-2 | `/opt/homebrew/bin/mvn test -Dtest=V6UsageCaptureTest > logs/r2/v6-usage.txt` |

**完成判定**（= spec §4 V6）：两腿各得 token 数 > 0、毫秒 > 0、一次调用 ↔ 一组样本——补齐第一组 E7 缺的 anthropic 协议样本；**anthropic 国内站可达性结论随本任务落盘**（失败按 §2 分支规则末行回滚覆盖）。**证据**：`v6-usage.txt`。

### T6.5 V9 双 OpenAI 实例 + 智谱（live）

| # | 步骤 |
|---|---|
| T6.5-1 | 写 `V9DualOpenAIInstanceTest.java`：`openai` 与 `zhipu` 两实例各跑 V3 同款闭环；先探 `mutate()` 是否透出 completionsPath（不透出退全手工 builder，spec §3.3）；zhipu 腿记录鉴权、模型接受（`glm-5.3-flash`）、工具调用往返（spec §6 开放项 8、9） |
| T6.5-2 | `/opt/homebrew/bin/mvn test -Dtest=V9DualOpenAIInstanceTest > logs/r2/v9-dual-openai.txt` |

**完成判定**（= spec §4 V9）：两实例互不干扰各自闭环、zhipu 返回可辨识不串线；completions-path 覆盖结论落盘；自动配置 Bean 不退位（与 T5 的 Bean 名打印交叉印证）。**V9 结论独立，失败不阻塞 T7 的 A/B/C 决议**（req 3.1）。**证据**：`v9-dual-openai.txt`。

### T7 V8 README 收口（README 全新创建）

| # | 步骤 |
|---|---|
| T7-1 | 创建全新 `README.md`（2026-09-15 用户指示删除旧版），两节结构：**第一节"第一组结论存档"**——D1-D4 决议与 E1-E9 打勾表自 git 历史原文恢复（引用链不断）；**第二节"第二组结论（模型接入路径重验）"**，**必含 spec §7.2 ①-⑦ 全部七项**：V0-V9 打勾表（通过/失败/未执行 + 原因）、决议（D5 起）+ 正式实现落点、路径 B 属性表、路径 C 检索过程、req 5.2/5.3/5.4 联动清单命中勾选、域名口径变更说明、归档注记 |
| T7-2 | 对照 req 4.4 五项产出物逐项核对齐备；引用格式实名（如"`spike/007-react-loop/README.md` 的 D5 决议"） |

**完成判定**（= spec §4 V8 + req 4.4）：README 六项齐、产出物五项齐。**证据**：README 本身。

## 4. 命令速查

```bash
# 前置（每次新开终端都执行）
source ~/.agent-os-poc/script/agent-os-env.sh          # live 任务前置，必须 source

# 构建 / 测试（mvn 全路径，目录内执行）
/opt/homebrew/bin/mvn test -DexcludedGroups=live        # 离线快跑（T0、T1-1 离线段）
/opt/homebrew/bin/mvn test -Dtest='V*'                 # 全量（需 key）
/opt/homebrew/bin/mvn test -Dtest=V3SingleToolLoopTest  # 单测一类（其余类同）
/opt/homebrew/bin/mvn dependency:tree -Dverbose > logs/r2/v1-dependency-tree.txt

# 拉 Spring AI 源码/文档核对时（GitHub raw 走本机代理）
export http_proxy="http://127.0.0.1:15236"; export https_proxy="http://127.0.0.1:15236"

# 应急：Maven 中央仓库拉取新构件（minimax starter 首次下载）失败时，同款代理后重跑 mvn 命令
```

## 5. 证据留档约定

- 目录：`spike/007-react-loop/logs/r2/`（与第一组 `logs/` 隔离，req 4.2 第 4 条）
- 清单（8 个，文件名表 = spec §7.1）：`v0-connector-search.txt`（已存在）/ `v1-resolve.txt`、`v1-dependency-tree.txt` / `v2-properties.txt` / `v3-tool-loop.txt` / `v4-manual-count.txt` / `v5-registry.txt` / `v6-usage.txt` / `v7-think-observations.txt` / `v9-dual-openai.txt`
- 测试输出重定向即留档；LoopResult 逐轮日志由 ManualLoop 统一打印；重跑的轮次记在同一文件尾部（含重跑原因与次数）

## 6. 分支与升级

- 决议规则**全部引用 req 3.3 的表**，本文件不重复（组合结论必须写清各路径适用边界；部分绿上报评审，不在 spike 内拍板）。
- 三条硬规则再抄一遍：**不得自行改用 Spring AI 2.0 版本线**；**不得放弃"禁用自动 tool 执行"设计**（根规则，变更走新一轮评审）；**V9 结论独立**，不参与 A/B/C 路径决议、失败不阻塞 V8。
- **不拷贝第一组代码**（req 4.2 第 3 条）：第一组 src/ 已移除，工作区无旧代码；只参照 007 README D3 循环路径与 ThinkStripper 思路重写。
- spike 执行期间不改 `docs/design/` 与根 CLAUDE.md（req 3.4；2026-09-14 的两次例外均已注记，不构成新先例）。
