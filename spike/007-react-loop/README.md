# Spike 结论：react-loop（自研 ReAct 循环 + Spring AI 1.1.x）

> 位置：`spike/007-react-loop/README.md`
> 执行日期：2026-09-07 · 状态：采纳 · 主线组合：SAA 1.1.2.0 + Spring AI 1.1.2 + Boot 3.5.16 + JDK 21
> 规格三件套：`spec/001-expirement.md`、`spec/002-spec.md`、`spec/003-plan.md`
> 引用方式：结论请实名引用为"`spike/007-react-loop/README.md` 的 D2 决议"这类形式

**总体结论：采纳。** 主线组合全部实验通过（E1-E8 全绿，E9 未触发），四项决议（D1-D4）全部落定。

## 一、验证项打勾表

| 项 | 结果 | 关键证据 |
|---|---|---|
| E1 依赖解析 + 容器启动 | ✅ | 依赖树零冲突（spring-ai/Boot 无 omitted for conflict）；graph-core 探针落 1.1.2.0；容器启动（Boot v3.5.16 banner）；`logs/e1-dependency-tree.txt` |
| E2 开关存在 | ✅ | `internalToolExecutionEnabled(false)` 编译通过并构建出选项对象（1.1.x 线代码实证） |
| E3 自动执行对照 | ✅ | 组 1（默认开）计数=1 → 框架确实自动执行；组 2（开关关+手动）我方执行=2、计数增量=2，精确吻合无双执行；`logs/e3-autoexec.txt` |
| E4 单工具闭环 | ✅ | 2 轮迭代，`toolCallRounds=[getWeather]`，最终回答含"晴，12°C"；`logs/e4-e5-loop.txt` |
| E5 多轮工具链 | ✅ | `[getCity, getDate, getWeather2]` 三步严格串行，4 轮迭代，未触上限，最终回答含三要素；`logs/e4-e5-loop.txt`（E5 专项 `logs/e5-loop.txt`） |
| E6 参数描述正确性 | ✅ | 第 1 次尝试即命中：`city=北京, date=2026-09-07`；`logs/e6-args.txt` |
| E7 用量/耗时采集 | ✅ | usages=[in=359,out=115, in=494,out=74]，durationsMs=[6063, 2750]——一次模型调用 ↔ 一组 (usage, duration)，审计对齐可行；`logs/e7-usage.txt` |
| E8 双 Provider 映射 | ✅ | openai + anthropic 两腿各自闭环，按名取用无类型扫描；**anthropic 腿实测证明 `spring.ai.anthropic.base-url` 属性生效**（未出现 api.anthropic.com 报错）；`logs/e8-providers.txt` |
| E9 对照线回归 | 未触发 | E1 通过，无需切 1.0.x 对照线 |

## 二、四项决议（D1-D4）

| # | 决议 | 依据 |
|---|---|---|
| D1 | **parent 保持 3.5.16**。1.1.x 官方配套即 Boot 3.5.x，E1 依赖树零冲突 + 容器启动实证 | E1；纪要 R1 待补实测结论（已完成，见下方回填记录） |
| D2 | **依赖坐标清单**：parent `spring-boot-starter-parent:3.5.16`；BOM `spring-ai-alibaba-bom:1.1.2.0` + `spring-ai-bom:1.1.2`（双 import，官方 quickstart 同款）；`spring-ai-starter-model-openai` / `spring-ai-starter-model-anthropic`（版本由 spring-ai-bom 管理，实际 1.1.2）；starter-test（test）。**正式实现第一周在 agentos-provider 照单引入** | E1；版本官方推荐核验记录见评审纪要 2.6 节 |
| D3 | **手动循环官方标准路径实证成立**：`ToolCallingChatOptions.builder().toolCallbacks(...).internalToolExecutionEnabled(false).build()` → `new Prompt(text, options)` → `call` → `hasToolCalls()` → `executeToolCalls` → `conversationHistory()` 重建 → 再 `call`——与纪要 8.1(3) 复核修正后的落地要点完全一致，**无需回填修正** | E3-E5 |
| D4 | **Provider 显式映射模式成立**：`Map<provider 名, ChatModel>`（key=openai/anthropic）按名取用，无类型扫描；样例代码见下节 | E8 |

## 三、结论 → 正式实现落点（W1 借助指南）

| 结论 | 正式实现落点 | 怎么用 |
|---|---|---|
| D1 parent 保持 3.5.16 | 根 pom.xml | 无需动作（已确认） |
| D2 依赖坐标清单 | agentos-provider/pom.xml（第一周） | 照"依赖坐标清单"节照单引入；环境变量与 yaml 接线按定稿（docs/design/detail-supplement/001-model-config-export.md）§4.2 |
| D3 手动循环标准路径 | ReActLoop / ToolExecutor | 按 ManualLoop 的路径**重写**（不拷代码，spike 已封存）；响应处理带 `<think>` 剥离（参照 ThinkStripper） |
| D4 显式映射 | ProviderService | 按"ProviderRegistry 样例"实现 `Map<provider 名, ChatModel>`；E3 的双 Bean 歧义就是必须显式映射的现场实证 |
| 附带：usage/耗时采集 | llm_calls 审计写入 | 按"一次调用 ↔ 一组 (usage, duration)"取数（E7 已验证取数路径） |

## 四、失败与修复记录（诚实留档）

1. **E3 首跑**：`ChatModel` 按类型注入失败——容器里有 openAiChatModel / anthropicChatModel 两个 Bean。修复：`@Qualifier` 按名注入。这个失败本身就是 E8（显式映射）必要性的现场证据。
2. **E5 首跑**：断言失败——工具链行为完全正确（三步严格串行），但模型最终回答只汇总了第三步，未包含城市和日期。修复：提示词硬化（"最终回答必须同时包含三项信息"）后通过。属断言/提示词设计问题，非框架问题。
3. **E8 首跑**：`BeanDefinitionOverrideException`——`@Configuration` 类名与 `@Bean` 方法名同为 `providerRegistry` 撞名。修复：方法改名 `providerRegistryMap` + 测试按名注入。

## 五、D4 样例：Provider 显式映射

```java
@Configuration
public class ProviderRegistry {

    @Bean
    public Map<String, ChatModel> providerRegistryMap(
            @Qualifier("openAiChatModel") ChatModel openAi,
            @Qualifier("anthropicChatModel") ChatModel anthropic) {
        return Map.of("openai", openAi, "anthropic", anthropic);
    }
}

// 使用：按名取用
ChatModel model = providerRegistry.get("openai");
```

## 六、附带实测结论

- **`spring.ai.anthropic.base-url` 属性生效**（E8 anthropic 腿闭环成功、未出现 api.anthropic.com 报错）→ 定稿 5.5 节行 8 的"手动构造 `AnthropicApi` 备选"无需启用；002 §6 开放项 2 关闭。
- **MiniMax-M3 思考内容确实混在 `<think>` 标签里**（E4/E5/E8 的 finalText 均观察到），ThinkStripper 剥离有效。
- **开放项 1/3 关闭**：双 BOM 协作无冲突；spring-ai 构件版本管理来源无需再深究（依赖树干净即为充分）。
