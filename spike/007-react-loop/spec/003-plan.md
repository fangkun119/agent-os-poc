# Spike 实施计划（003-plan）

> 位置：`spike/007-react-loop/spec/003-plan.md`
> 创建：2026-09-07 · 状态：待执行（基于 2026-09-06 决议变更后的 1.1.x 主线重新生成；初版已删除）
> 上游：`001-expirement.md`（下称 001，实验流程与决议规则）、`002-spec.md`（下称 002，工程形态 / 组件规格 / 验收标准）
> 约定：① 所有命令在 `spike/007-react-loop/` 目录内执行；② 真实模型调用任务前先 `source ~/.agent-os-poc/script/agent-os-env.sh`；③ 术语沿用 001 §0.2

## 0. 文档定位

- 003 回答三件事：**按什么顺序做**（任务依赖）、**每步敲什么命令**（可执行）、**怎么算完成**（预期输出）。
- 完成定义（整体 DoD）：T0-T5 全部完成 + T6 按条件执行或说明不触发理由 + T7 的 README.md 落盘 + 001 §9 回填动作清单执行或注明挂起原因。

## 1. 任务总览（顺序与依赖）

```text
T0 骨架与离线验收（E1、E2）          ← 唯一入口，不需要 key
 └→ T1 自动执行对照实证（E3）        ← 需要 key（live）
     └→ T2 手动循环（E4 → E5）
         └→ T3 工具参数描述（E6）
             └→ T4 token 用量与耗时采集（E7）
                 └→ T5 双 Provider 显式映射（E8）
                     └→ T6 条件支线（E9 仅当 E1 失败：切 1.0.x 对照线）
                         └→ T7 结论落盘（README.md + 回填动作）
```

- T0 → T5 线性：前一任务完成判定未过，不进入下一任务。
- T6 条件分支：E9 命中时，T2-T5 剩余任务换到对照组合（SAA 1.0.0.4 优先 + Spring AI 1.0.x + parent 3.4.x）下重做（顺序不变）。

## 2. 任务明细

### T0 骨架与离线验收（E1、E2）——不需要 key

| # | 步骤 |
|---|---|
| T0-1 | 建 002 §1.1 目录结构；写 `pom.xml`（002 §1.2：parent 3.5.16、Java 21、双 BOM 1.1.2.0 / 1.1.2、两个 starter、graph-core 探针（test）、starter-test、surefire `forkedProcessTimeoutInSeconds=180`） |
| T0-2 | 写 `src/main/resources/application.yaml`（002 §2.1 全文草稿） |
| T0-3 | 写 `SpikeApp.java`、`E1E2BootstrapTest.java`（含 `internalToolExecutionEnabled(false)` 引用代码，即 E2 编译载体） |
| T0-4 | 依赖树留档 + 三条检查（命令见下） |
| T0-5 | `mvn test -Dtest=E1E2BootstrapTest` |

T0-4 命令与预期：

```bash
mvn dependency:tree -Dverbose > logs/e1-dependency-tree.txt
# 检查 1：spring-ai / Boot 坐标无冲突弃用（预期：无输出）
grep -E "org.springframework.ai|org.springframework.boot" logs/e1-dependency-tree.txt | grep "omitted for conflict"
# 检查 2：探针构件（SAA）落 1.1.2.0 且无冲突（预期：第一行有解析结果且含 1.1.2.0；第二行无输出）
grep "com.alibaba.cloud.ai" logs/e1-dependency-tree.txt
grep "com.alibaba.cloud.ai" logs/e1-dependency-tree.txt | grep -vE "1\.1\.2\.0|omitted"
# 检查 3：starter 实际解析版本 = 1.1.2（预期两行均含 1.1.2）
grep -E "spring-ai-starter-model-(openai|anthropic)" logs/e1-dependency-tree.txt
```

T0 完成判定：检查 1、2 无异常输出；检查 3 两个 starter 落 1.1.2；T0-5 测试绿。证据：`logs/e1-dependency-tree.txt`。

开放项落定（002 §6）：依赖树显示双 BOM 冲突或探针未落 1.1.2.0 → 停止 T0，走 001 §8 的 E9 路径；顺带记录 spring-ai 构件由哪个 BOM 管理（开放项 3）。

### T1 自动执行对照实证（E3）——live

| # | 步骤 |
|---|---|
| T1-1 | 写 `tool/CountingTools.java`（002 §3.2） |
| T1-2 | 写 `E3AutoExecComparisonTest.java`：组 1 = 默认开关、不写自研执行代码；组 2 = 开关关 + ManualLoop。提问引导模型只发起一轮工具调用 |
| T1-3 | `source ~/.agent-os-poc/script/agent-os-env.sh && mvn test -Dtest=E3AutoExecComparisonTest > logs/e3-autoexec.txt` |

T1 完成判定：两组"工具执行次数 = 该组日志中模型发起工具调用的轮数"；组 1 计数增加（框架自动执行实证）、组 2 无我方代码之外的执行。出现框架侧执行 → 按 001 §8"双执行"行上报。证据：`logs/e3-autoexec.txt`。

### T2 手动循环（E4 → E5）——live

| # | 步骤 |
|---|---|
| T2-1 | 写 `loop/ManualLoop.java` + `loop/LoopResult.java`（002 §3.4）与 `util/ThinkStripper.java`（002 §3.6） |
| T2-2 | 写 `E4SingleToolLoopTest.java` 并运行：`mvn test -Dtest=E4SingleToolLoopTest > logs/e4-loop.txt` |
| T2-3 | 写 `tool/ChainTools.java`（002 §3.3）与 `E5MultiToolChainTest.java` 并运行：`mvn test -Dtest=E5MultiToolChainTest > logs/e5-loop.txt` |

T2 完成判定（E4）：`strip(finalText)` 含工具结果要素；工具执行全部由我方代码触发。（E5）：上限 10 轮内正确终止；`toolCallRounds` ≥ 3 轮；`strip(finalText)` 汇总"北京"、"2026-09-07"、"晴"三要素；每轮历史长度单调增。证据：`logs/e4-loop.txt`、`logs/e5-loop.txt`。

### T3 工具参数描述正确性（E6）——live

| # | 步骤 |
|---|---|
| T3-1 | 写 `E6ToolSchemaTest.java`：提问"查北京 2026-09-07 的天气"，断言 `lastArgs()` = (city=北京, date=2026-09-07)；运行 `mvn test -Dtest=E6ToolSchemaTest > logs/e6-args.txt` |

T3 完成判定：入参与给定值一致；偶发偏差允许固定次数重跑并记录。证据：`logs/e6-args.txt`。

### T4 token 用量与耗时采集（E7）——live

| # | 步骤 |
|---|---|
| T4-1 | 写 `E7UsageCaptureTest.java`：ManualLoop 跑 E4 同款任务，断言 `usages` / `durationsMs` 非空且 > 0；打印采集样本；运行 `mvn test -Dtest=E7UsageCaptureTest > logs/e7-usage.txt` |

T4 完成判定：样本含"一次模型调用 ↔ 一组 (token 用量, 毫秒耗时)"记录。证据：`logs/e7-usage.txt`。

### T5 双 Provider 显式映射（E8）——live

| # | 步骤 |
|---|---|
| T5-1 | 写 `provider/ProviderRegistry.java`（002 §3.5） |
| T5-2 | 写 `E8ProviderRegistryTest.java`：两实例各跑 E4 同款闭环；运行 `mvn test -Dtest=E8ProviderRegistryTest > logs/e8-providers.txt` |
| T5-3 | **Anthropic 腿实测点**：查日志中 anthropic 实例请求端点。打到 `minimaxi.com` / `minimax.cn` → `spring.ai.anthropic.base-url` 生效，开放项 2 关闭；出现 `api.anthropic.com` 报错 → 属性不生效，切手动构造 `AnthropicApi(baseUrl, apiKey)`（002 §3.5），重跑本任务 |

T5 完成判定：两条腿各自完成闭环；按名取用、无类型扫描。只有一条腿可用时记"部分执行"（001 §8）。证据：`logs/e8-providers.txt`。

### T6 条件支线（E9）

| 触发 | 任务 | 步骤 |
|---|---|---|
| E1 失败 | E9 切 1.0.x 对照线（SAA 优先 1.0.0.4） | pom 改对照组合（parent 3.4.x + spring-ai-bom 1.0.0 + spring-ai-alibaba-bom 1.0.0.4；graph-core 探针随之用 1.0.x 线），重跑 T0-4 / T0-5 → T1-3 → T2 → T3 → T4 → T5；证据文件加 `.e9` 后缀区分；全绿后按 001 §8 上报评审确认 |

### T7 结论落盘（README.md + 回填动作）

| # | 步骤 |
|---|---|
| T7-1 | 写 `README.md`（001 §9 四项：E1-E9 打勾表、D1-D4 决议及依据、失败项最小复现、ProviderRegistry 样例片段） |
| T7-2 | 执行 001 §9 回填动作清单 4 条（根 pom parent / agentos-provider 依赖清单 / 纪要 8.1(3) 校对 / R1 实测结论）；不能立即执行的注明挂起原因 |

## 3. 命令速查

```bash
source ~/.agent-os-poc/script/agent-os-env.sh        # live 任务前置
mvn test -DexcludedGroups=live                        # 离线快跑（T0）
mvn test -Dgroups=live                                # 只跑真实模型调用项
mvn test                                              # 全量（需 key）
mvn test -Dtest=E4SingleToolLoopTest                  # 单测一类
mvn dependency:tree -Dverbose > logs/e1-dependency-tree.txt
```

## 4. 证据留档约定

- 目录：`spike/007-react-loop/logs/`
- 清单：`e1-dependency-tree.txt` / `e3-autoexec.txt` / `e4-loop.txt` / `e5-loop.txt` / `e6-args.txt` / `e7-usage.txt` / `e8-providers.txt`（E9 触发时加 `.e9` 后缀）
- 测试输出重定向即留档；LoopResult 逐轮日志由 ManualLoop 统一打印

## 5. 分支与升级

全部引用 001 §8 决议规则表，本文件不重复。三条硬规则再抄一遍：不得自行改用 2.0 版本线；不得放弃"禁用自动执行"设计（根规则，变更走新一轮评审）；E9 命中后剩余任务在对照组合下继续跑完。
