package spike.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import spike.mcp.client.SpikeMcpClients;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 工具 → AgentOSTool 适配（TS 6.4 McpToolAdapter 的 spike 形态；002 §3.5）。
 *
 * <p>映射：tools/list 的每个工具包装成一个 AgentOSTool 注册进 ToolRegistry（M3）。
 * 结果映射（M4）：成功取全部 text 段拼接；isError / 异常 / 超时 → ToolResult 四要素。
 * 审计口径（002 §3.9）：每次执行打一行 tool_invocations 同构日志，不自建留痕。
 */
@Component
public class McpToolAdapter {

    private static final Logger log = LoggerFactory.getLogger(McpToolAdapter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 把一个 server 的全部工具批量注册进注册表（M3：注册数 = listTools 返回数） */
    public void registerAll(SpikeMcpClients.ConnectedServer server, ToolRegistry registry) {
        for (McpSchema.Tool tool : server.tools().tools()) {
            registry.register(new McpTool(server.serverName(), server.client(), tool));
        }
    }

    /** AgentOSTool 的 MCP 实现侧：一个 McpSchema.Tool 对应一个实例 */
    private final class McpTool implements AgentOSTool {

        private final String serverName;
        private final McpSyncClient client;
        private final McpSchema.Tool tool;

        private McpTool(String serverName, McpSyncClient client, McpSchema.Tool tool) {
            this.serverName = serverName;
            this.client = client;
            this.tool = tool;
        }

        @Override
        public String getName() {
            return tool.name();
        }

        @Override
        public String getDescription() {
            return tool.description() == null ? "" : tool.description();
        }

        /** inputSchema（McpSchema.JsonSchema record）序列化为 JSON Schema 字符串直传（M3 直映射实证点） */
        @Override
        public String getInputSchema() {
            try {
                return objectMapper.writeValueAsString(tool.inputSchema());
            } catch (Exception e) {
                throw new IllegalStateException("inputSchema 序列化失败: " + tool.name(), e);
            }
        }

        @Override
        public ToolResult execute(String jsonInput) {
            long start = System.currentTimeMillis();
            try {
                Map<String, Object> args = jsonInput == null || jsonInput.isBlank()
                        ? Map.of()
                        : objectMapper.readValue(jsonInput, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
                McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(tool.name(), args));
                long duration = System.currentTimeMillis() - start;

                String text = joinText(result);
                boolean failed = Boolean.TRUE.equals(result.isError());
                auditLine(tool.name(), jsonInput, !failed, failed ? text : null, duration);
                if (failed) {
                    // server 侧报错（isError=true）：协议层成功但业务失败，重试多半同样失败 → 不可重试初值
                    return ToolResult.permanentFailure(text);
                }
                return ToolResult.ok(text);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                var msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                boolean retryable = looksLikeTransient(e);
                auditLine(tool.name(), jsonInput, false, msg, duration);
                return retryable ? ToolResult.retryableFailure(msg) : ToolResult.permanentFailure(msg);
            }
        }

        /** 拼接结果全部 text 段（002 §3.5：多段内容默认拼接，实测形态归 M4 记录） */
        private String joinText(McpSchema.CallToolResult result) {
            if (result.content() == null) {
                return "";
            }
            var sb = new StringBuilder();
            for (McpSchema.Content c : result.content()) {
                if (c instanceof McpSchema.TextContent t) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(t.text());
                }
            }
            return sb.toString();
        }

        /**
         * 可重试初值判定：连接 / 超时类 true，其余 false（002 开放项 6，M4/M6 实测定案）。
         * 实测（M4）：SDK 超时抛包装异常（message 形如 "Did not observe any item or terminal
         * signal within {n}ms"，不含 timeout 字样），TimeoutException 在 cause 链上——两层都要查。
         */
        private boolean looksLikeTransient(Throwable e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t instanceof java.util.concurrent.TimeoutException) {
                    return true;
                }
                var m = String.valueOf(t.getMessage()).toLowerCase();
                if (m.contains("timeout") || m.contains("timed out") || m.contains("did not observe any item")) {
                    return true;
                }
            }
            return false;
        }

        /** tool_invocations 同构审计日志行（002 §3.9；正式实现落点 ToolExecutor） */
        private void auditLine(String name, String input, boolean success, String error, long durationMs) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("tool_name", name);
            line.put("server", serverName);
            line.put("input_json", input);
            line.put("success", success);
            line.put("error_message", error);
            line.put("duration_ms", durationMs);
            try {
                log.info("TOOL_INVOCATION {}", objectMapper.writeValueAsString(line));
            } catch (Exception ignored) {
                log.info("TOOL_INVOCATION tool_name={} success={} duration_ms={}", name, success, durationMs);
            }
        }
    }
}
