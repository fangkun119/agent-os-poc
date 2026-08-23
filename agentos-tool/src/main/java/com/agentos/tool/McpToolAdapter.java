package com.agentos.tool;

import com.agentos.core.tool.AgentOSTool;
import com.agentos.core.tool.ToolResult;

/**
 * MCP Tool 适配器（TS 6.4）：把 MCP Tool 适配成 AgentOSTool 接口。
 *
 * <p>Tool 调用时通过 MCP 协议（JSON-RPC over stdio 或 SSE）转发给对应 MCP server 执行，
 * 结果包装成 ToolResult 返回。
 */
public class McpToolAdapter implements AgentOSTool {

    @Override
    public String getName() {
        // TODO: 返回 MCP tool 的名称（tools/list 取得）
        return null;
    }

    @Override
    public String getDescription() {
        // TODO: 返回 MCP tool 的描述
        return null;
    }

    @Override
    public String getInputSchema() {
        // TODO: 返回 MCP tool 的 inputSchema
        return null;
    }

    @Override
    public ToolResult execute(String jsonInput) {
        throw new UnsupportedOperationException("尚未实现：McpToolAdapter.execute（TS 6.4）");
    }
}
