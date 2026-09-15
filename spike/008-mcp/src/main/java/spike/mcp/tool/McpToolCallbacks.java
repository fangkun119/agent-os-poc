package spike.mcp.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import spike.mcp.client.SpikeMcpClients;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP 工具 → ToolCallback 供给（M5 两条候选；002 §3.6）。
 *
 * <p>候选一：spring-ai-mcp 现成适配 SyncMcpToolCallback（框架侧包装）。
 * 候选二：MCP 工具先经 McpToolAdapter 包装成 AgentOSTool，再由本类内部 AdapterToolCallback
 * 自适配成 ToolCallback（我方包装）。
 * 两候选都包计数装饰器（CountingToolCallback），为 M5 的"无双执行"计数断言服务。
 */
public final class McpToolCallbacks {

    private McpToolCallbacks() {
    }

    /** 候选一：SyncMcpToolCallback 直接包装（builder 路径——两参构造在 1.1.2 已标 deprecated），外挂执行计数 */
    public static List<ToolCallback> fromSyncAdapters(SpikeMcpClients.ConnectedServer server, AtomicInteger counter) {
        return server.tools().tools().stream()
                .map(tool -> (ToolCallback) new CountingToolCallback(
                        SyncMcpToolCallback.builder()
                                .mcpClient(server.client())
                                .tool(tool)
                                .build(),
                        counter))
                .toList();
    }

    /** 候选二：AgentOSTool（McpToolAdapter 产出）自适配成 ToolCallback，外挂执行计数 */
    public static List<ToolCallback> fromAgentOSTools(SpikeMcpClients.ConnectedServer server,
                                                      McpToolAdapter adapter,
                                                      ToolRegistry registry,
                                                      AtomicInteger counter) {
        adapter.registerAll(server, registry);
        return registry.list().stream()
                .map(t -> (ToolCallback) new CountingToolCallback(new AdapterToolCallback(t), counter))
                .toList();
    }

    /** 执行计数装饰器：call 被调一次计一次（M5：计数增量应等于模型发起工具调用的轮数） */
    static final class CountingToolCallback implements ToolCallback {

        private final ToolCallback delegate;
        private final AtomicInteger counter;

        CountingToolCallback(ToolCallback delegate, AtomicInteger counter) {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public String call(String toolInput) {
            counter.incrementAndGet();
            return delegate.call(toolInput);
        }
    }

    /** 候选二的自适配层：AgentOSTool → ToolCallback（ToolDefinition 三属性直映射） */
    static final class AdapterToolCallback implements ToolCallback {

        private final AgentOSTool delegate;

        AdapterToolCallback(AgentOSTool delegate) {
            this.delegate = delegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                    .name(delegate.getName())
                    .description(delegate.getDescription())
                    .inputSchema(delegate.getInputSchema())
                    .build();
        }

        @Override
        public String call(String toolInput) {
            ToolResult result = delegate.execute(toolInput);
            // 失败按工具执行面条款抛 ToolExecutionException 包装（agentos/CLAUDE.md §1 C-88）：
            // executeToolCalls 会转为工具错误结果回喂模型由其决定重试；成功原样返回内容文本
            if (!result.success()) {
                throw new ToolExecutionException(getToolDefinition(),
                        new IllegalStateException(result.error()));
            }
            return result.content();
        }
    }
}
