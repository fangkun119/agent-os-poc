package spike.mcp.tool;

/**
 * AgentOS 内部统一工具抽象的最小副本（TS 6.1；002 §3.4）。
 * 正式实现归 agentos-core；spike 只验证 MCP 工具能否包装成该接口。
 */
public interface AgentOSTool {

    /** 工具名（对应 MCP 工具的 name） */
    String getName();

    /** 工具描述（对应 MCP 工具的 description） */
    String getDescription();

    /** 输入参数的 JSON Schema（对应 MCP 工具的 inputSchema，JSON Schema 格式字符串） */
    String getInputSchema();

    /**
     * 执行工具。
     *
     * @param jsonInput JSON 格式的入参
     * @return 统一结果结构
     */
    ToolResult execute(String jsonInput);
}
