package com.agentos.tool;

/**
 * MCP Client 服务（TS 6.4）：MCP server 连接维护与工具注册。
 *
 * <p>AgentOS 启动时连接 .agentos/mcp_servers.yaml 配置的所有 MCP server（name / transport /
 * command / env），调 tools/list 拿工具列表，把每个 MCP 工具经 McpToolAdapter 包装成
 * AgentOSTool 注册到 ToolRegistry；处理 server 失联、超时、错误恢复。
 */
public class McpClientService {

    // TODO: 启动时连接 mcp_servers.yaml 配置的全部 server，tools/list 后包装成 AgentOSTool 入 ToolRegistry（TS 6.4）
}
