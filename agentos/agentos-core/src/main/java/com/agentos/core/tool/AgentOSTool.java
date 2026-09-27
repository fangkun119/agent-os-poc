package com.agentos.core.tool;

/**
 * AgentOS 内部统一的 Tool 抽象接口（TechnicalSolution.md - 6.1 AgentOSTool 抽象）。
 *
 * <p>内置 Tool、{@code @Tool} 注解的 Plugin Tool、MCP Tool 都被包装成本接口实例注册到 ToolRegistry，
 * ReAct 循环不感知 Tool 的来源。核心方法五定案（TechnicalSolution.md - 6.1 AgentOSTool 抽象的五方法口径，设计评审 Q9 决议 2026-09-14）：
 * getName / getDescription / getInputSchema / execute 已落地；第五方法 sandboxActions(inputJson)
 * 随 ToolExecutor 统一校验实施接入——返回本次调用待校验的 SandboxAction 清单、无默认实现，
 * MCP 与 @Tool 适配实现返回空清单＝豁免；内置 MemoryTools（agentos-memory 模块的
 * SaveMemoryTool/RecallMemoryTool）亦返回空清单，语义为无涉外动作可申报、非豁免
 * （TechnicalSolution.md - 6.7 Sandbox 检查，设计评审 Q5 裁决 2026-09-24）。
 *
 * <p>注意：Spring AI 只用其 Provider 抽象 + 协议转换 + {@code @Tool} schema 生成，
 * <b>禁用自动 tool 执行</b>——Tool 的实际调度和执行由 ReActLoop + ToolExecutor 控制（TechnicalSolution.md - 1.1 关键技术决策的决策二）。
 */
public interface AgentOSTool {

    /** Tool 唯一名称（Function Calling 的 function name，如 read_file / notify）。 */
    String getName();

    /** 给 LLM 看的能力描述。 */
    String getDescription();

    /** JSON Schema 格式的输入参数描述。 */
    String getInputSchema();

    /** 执行 Tool：接收 JSON 输入字符串，返回 {@link ToolResult}（由 ToolExecutor 调用，TechnicalSolution.md - 4.2 模块组成）。 */
    ToolResult execute(String jsonInput);
}
