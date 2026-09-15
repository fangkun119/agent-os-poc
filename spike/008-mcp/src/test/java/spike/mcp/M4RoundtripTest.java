package spike.mcp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spike.mcp.client.SpikeMcpClients;
import spike.mcp.config.McpServersYamlLoader;
import spike.mcp.tool.AgentOSTool;
import spike.mcp.tool.McpToolAdapter;
import spike.mcp.tool.ToolRegistry;

import java.io.InputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M4 工具调用往返（002 §4 M4 行，live）：成功 / 失败 / 超时三形态 → ToolResult 映射；
 * 顺带取 M7 强证据（get-env 回显 MCP_SPIKE_PROBE，002 开放项 7）。
 */
@SpringBootTest
@Tag("live")
class M4RoundtripTest {

    private static final Logger log = LoggerFactory.getLogger(M4RoundtripTest.class);

    @Autowired
    private McpToolAdapter adapter;

    private record Env(ToolRegistry registry, SpikeMcpClients clients) {
        void shutdown() {
            clients.shutdown();
        }
    }

    /** 连接 + 注册，返回注册表；server 名过滤取用 */
    private Env setup() {
        var probe = System.getenv("MCP_SPIKE_PROBE");
        assertThat(probe).as("前置：先 export MCP_SPIKE_PROBE").isNotBlank();
        try (InputStream in = getClass().getResourceAsStream("/mcp-servers.yaml")) {
            var entries = new McpServersYamlLoader().load(in, System.getenv());
            var clients = new SpikeMcpClients();
            var connected = clients.connectAll(entries);
            assertThat(connected).hasSize(2);
            var registry = new ToolRegistry();
            connected.forEach(s -> adapter.registerAll(s, registry));
            return new Env(registry, clients);
        } catch (Exception e) {
            throw new IllegalStateException("加载 mcp-servers.yaml 失败", e);
        }
    }

    /** 探测：打印关注工具的 inputSchema（首次执行时看参数名，据此核对断言用例） */
    @Test
    void printKeySchemas() {
        var env = setup();
        try {
            for (var name : new String[]{"echo", "get-env", "get-sum", "trigger-long-running-operation", "read_file"}) {
                var tool = env.registry().lookup(name);
                assertThat(tool).as("工具应已注册: " + name).isNotNull();
                log.info("[SCHEMA] {} -> {}", name, tool.getInputSchema());
            }
        } finally {
            env.shutdown();
        }
    }

    /** 成功形态：echo 回显 → success=true 且内容含入参 */
    @Test
    void successEcho() {
        var env = setup();
        try {
            var echo = env.registry().lookup("echo");
            var result = echo.execute("{\"message\": \"spike-008-m4\"}");
            log.info("[M4] echo -> success={} content={} retryable={}", result.success(), result.content(), result.retryable());
            assertThat(result.success()).isTrue();
            assertThat(result.content()).contains("spike-008-m4");
            assertThat(result.retryable()).isFalse();
        } finally {
            env.shutdown();
        }
    }

    /**
     * M7 强证据：get-env 回显子进程环境变量（占位符 → ServerParameters.env → 子进程 → 协议返回）。
     * 安全纪律（TS 8.8）：get-env 会回显子进程全量环境变量，可能含宿主凭证——打印一律脱敏，
     * 只输出"是否含探测值"的布尔结论，不输出回显原文。
     */
    @Test
    void m7ProbeEnvEchoedFromSubprocess() {
        var env = setup();
        try {
            var probe = System.getenv("MCP_SPIKE_PROBE");
            var getEnv = env.registry().lookup("get-env");
            var result = getEnv.execute("{}");
            boolean containsProbe = result.success() && result.content() != null
                    && result.content().contains(probe);
            log.info("[M7-PROBE] get-env -> success={} 回显含探测值={}（回显原文不打印：含宿主全量 env，防凭证落日志）",
                    result.success(), containsProbe);
            assertThat(containsProbe).isTrue();
        } finally {
            env.shutdown();
        }
    }

    /** 失败形态：filesystem read_file 读不存在文件 → success=false、error 非空 */
    @Test
    void failureOnMissingFile() {
        var env = setup();
        try {
            var readFile = env.registry().lookup("read_file");
            var result = readFile.execute("{\"path\": \"no-such-file-m4.txt\"}");
            log.info("[M4] read_file(missing) -> success={} error={} retryable={}",
                    result.success(), result.error(), result.retryable());
            assertThat(result.success()).isFalse();
            assertThat(result.error()).isNotBlank();
        } finally {
            env.shutdown();
        }
    }

    /**
     * 超时形态：短 requestTimeout 客户端调长任务工具 → 超时异常 → ToolResult（retryable 初值）。
     * 实测注记（M6③ 素材）：requestTimeout 是 client 级、覆盖包括 initialize 在内的全部请求——
     * 1s 会在 initialize 阶段即超时（npx 冷启动 > 1s）；这里取 3s（包已缓存时 initialize 约 2s，
     * 若偶发 initialize 超时按固定次数重跑并记录，007 验收基调同款）。
     */
    @Test
    void timeoutMapsToRetryableFailure() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/mcp-servers.yaml")) {
            var entries = new McpServersYamlLoader().load(in, System.getenv());
            var everything = entries.stream().filter(e -> e.name().equals("everything")).findFirst().orElseThrow();

            var clients = new SpikeMcpClients();
            try {
                var field = SpikeMcpClients.class.getDeclaredField("clients");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                var list = (java.util.List<io.modelcontextprotocol.client.McpSyncClient>) field.get(clients);

                var argv = everything.command().trim().split("\\s+");
                var params = io.modelcontextprotocol.client.transport.ServerParameters.builder(argv[0])
                        .args(java.util.Arrays.copyOfRange(argv, 1, argv.length))
                        .env(everything.env())
                        .build();
                var transport = new io.modelcontextprotocol.client.transport.StdioClientTransport(params,
                        io.modelcontextprotocol.json.McpJsonMapper.getDefault());
                var client = io.modelcontextprotocol.client.McpClient.sync(transport)
                        .requestTimeout(Duration.ofSeconds(3))
                        .build();
                client.initialize();
                list.add(client);

                // 隔离注册：短超时客户端只注册进临时表，不混入主注册表
                var connected = new SpikeMcpClients.ConnectedServer("everything", client, client.listTools());
                var isolatedRegistry = new ToolRegistry();
                var one = connected.tools().tools().stream()
                        .filter(t -> t.name().equals("trigger-long-running-operation")).findFirst().orElseThrow();
                isolatedRegistry.register(wrap(connected, one));
                var tool = isolatedRegistry.lookup("trigger-long-running-operation");
                var result = tool.execute("{}");
                log.info("[M4] trigger(3s timeout) -> success={} error={} retryable={}",
                        result.success(), result.error(), result.retryable());
                assertThat(result.success()).isFalse();
                assertThat(result.error()).isNotBlank();
                assertThat(result.retryable()).as("超时应判可重试").isTrue();
            } finally {
                clients.shutdown();
            }
        }
    }

    /** 直接构造单个工具的适配（不复用 adapter.registerAll，避免主注册表混入短超时客户端的工具） */
    private AgentOSTool wrap(SpikeMcpClients.ConnectedServer server, io.modelcontextprotocol.spec.McpSchema.Tool tool) {
        var registry = new ToolRegistry();
        adapter.registerAll(server, registry);
        return registry.lookup(tool.name());
    }
}
