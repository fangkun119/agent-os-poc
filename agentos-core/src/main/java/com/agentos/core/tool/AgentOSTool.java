package com.agentos.core.tool;

/**
 * AgentOS 内部统一的 Tool 抽象接口（TS 6.1）。
 *
 * <p>内置 Tool、{@code @Tool} 注解的 Plugin Tool、MCP Tool 都被包装成本接口实例注册到 ToolRegistry，
 * ReAct 循环不感知 Tool 的来源。四个核心方法：getName / getDescription / getInputSchema / execute。
 *
 * <p>注意：Spring AI 只用其 Provider 抽象 + 协议转换 + {@code @Tool} schema 生成，
 * <b>禁用自动 tool 执行</b>——Tool 的实际调度和执行由 ReActLoop + ToolExecutor 控制（TS 1.1 决策二）。
 */
public interface AgentOSTool {

    /** Tool 唯一名称（Function Calling 的 function name，如 read_file / notify）。 */
    String getName();

    /** 给 LLM 看的能力描述。 */
    String getDescription();

    /** JSON Schema 格式的输入参数描述。 */
    String getInputSchema();

    /** 执行 Tool：接收 JSON 输入字符串，返回 {@link ToolResult}（由 ToolExecutor 调用，TS 4.2）。 */
    ToolResult execute(String jsonInput);
}
