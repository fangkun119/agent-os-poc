package com.agentos.core.react;

import com.agentos.core.tool.ToolResult;

/**
 * Tool 执行器（TS 4.2）：从 ToolRegistry 找到对应 Tool，做 Sandbox 检查，执行 Tool，
 * 把结果包装成 {@link ToolResult} 返回给 ReAct 循环，并写入 tool_invocations 审计表。
 *
 * <p>失败时返回可重试标识，由 Agent（LLM）自行决定是否重试；框架级自动重试放扩展阶段。
 * Sandbox 校验失败抛 SandboxViolationException，直接复用本类失败审计路径（success=false + error_message，TS 6.7）。
 */
public class ToolExecutor {

    // TODO: 实施阶段补 execute(...)（TS 4.2）

    public ToolResult execute(String toolName, String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：ToolExecutor.execute（TS 4.2）");
    }
}
