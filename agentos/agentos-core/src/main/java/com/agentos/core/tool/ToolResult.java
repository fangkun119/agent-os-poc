package com.agentos.core.tool;

/**
 * Tool 执行结果（TS 6.1）：成功标识、结果内容、错误信息、是否可重试。
 *
 * <p>失败时返回可重试标识，由 Agent（LLM）自行决定是否重试；框架级自动重试放扩展阶段（TS 4.2）。
 * 每次执行（无论成败）由 ToolExecutor 写入 tool_invocations 审计表。
 */
public record ToolResult(boolean success, String content, String errorMessage, boolean retryable) {

    public static ToolResult ok(String content) {
        return new ToolResult(true, content, null, false);
    }

    public static ToolResult fail(String errorMessage, boolean retryable) {
        return new ToolResult(false, null, errorMessage, retryable);
    }
}
