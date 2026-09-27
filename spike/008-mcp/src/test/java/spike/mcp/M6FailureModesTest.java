package spike.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import spike.mcp.client.SpikeMcpClients;
import spike.mcp.config.McpServerEntry;


import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M6 失败模式三测（002 §4 M6 行，live）：
 * ①坏命令 → 连接阶段异常形态（其余 server 不受影响由 SpikeMcpClients.connectAll 跳过逻辑保证）
 * ②运行中 kill server 子进程 → 调用异常形态 + SDK 有无自动重连
 * ③requestTimeout 行为（默认值经 javap 常量 + 实测双重确认；正式实现对齐 TechnicalSolution.md - 7.4 关键设计点 的说明写入 README）
 */
@SpringBootTest
@Tag("live")
class M6FailureModesTest {

    private static final Logger log = LoggerFactory.getLogger(M6FailureModesTest.class);

    private McpSyncClient newClient(String command, Duration timeout) {
        var argv = command.trim().split("\\s+");
        var params = ServerParameters.builder(argv[0])
                .args(java.util.Arrays.copyOfRange(argv, 1, argv.length))
                .build();
        var transport = new StdioClientTransport(params, McpJsonMapper.getDefault());
        var builder = McpClient.sync(transport);
        if (timeout != null) {
            builder.requestTimeout(timeout);
        }
        var client = builder.build();
        client.initialize();
        return client;
    }

    /** ①坏命令：进程起不来 → initialize 抛异常，异常形态落盘 */
    @Test
    void badCommandFailsAtConnect() {
        var entry = new McpServerEntry("bad-m6", "stdio",
                "/no/such/binary-m6-probe", Map.of());
        var clients = new SpikeMcpClients();
        var connected = clients.connectAll(List.of(entry));
        // SpikeMcpClients 的跳过逻辑：连接失败不抛、不阻断（001 M6① 正常路径对照）
        assertThat(connected).isEmpty();
    }

    /** ②运行中 kill：连接成功后杀子进程 → callTool 异常形态 + 再调用形态（SDK 有无自动重连的实测） */
    @Test
    void killedServerFailsCallsWithoutAutoReconnect() throws Exception {
        var client = newClient("npx -y @modelcontextprotocol/server-everything", Duration.ofSeconds(30));
        try {
            var before = client.listTools();
            assertThat(before.tools()).isNotEmpty();

            // 杀掉本测试拉起的 server 子进程（npx 链路末端 node 进程）
            new ProcessBuilder("pkill", "-f", "@modelcontextprotocol/server-everything").start().waitFor();
            Thread.sleep(500);

            // 第一次调用：预期失败。异常形态落盘（M6② 素材一）
            Exception firstCall = null;
            try {
                client.listTools();
            } catch (Exception e) {
                firstCall = e;
            }
            assertThat(firstCall).as("server 被杀后调用应失败").isNotNull();
            log.info("[M6] kill 后首调异常: {}: {}", firstCall.getClass().getName(), firstCall.getMessage());

            // 第二次调用：SDK 是否自动重连？（M6② 素材二：有无重连的实测判定）
            Exception secondCall = null;
            try {
                client.listTools();
            } catch (Exception e) {
                secondCall = e;
            }
            log.info("[M6] kill 后再调: {}", secondCall == null
                    ? "成功（SDK 自动重连或进程存活）"
                    : secondCall.getClass().getName() + ": " + secondCall.getMessage());
            assertThat(secondCall).as("sync 客户端无自动重连，第二次调用应仍失败（实测口径）").isNotNull();
        } finally {
            try {
                client.closeGracefully();
            } catch (Exception ignored) {
                // 进程已被杀，close 异常属预期
            }
        }
    }

    /**
     * ③requestTimeout：默认值实测 + 关键发现落盘。
     * 实测注记（M6③）：0.17.0 的 SyncSpec 有两个独立预算——requestTimeout（常规调用）与
     * initializationTimeout（initialize 握手）；M4 超时用例中 1s 卡死 initialize 的正解
     * 就是分开设 initializationTimeout。正式实现超时对齐 TechnicalSolution.md - 7.4 关键设计点 的三档预算，此处取数仅供回填。
     */
    @Test
    void defaultRequestTimeoutProbe() throws Exception {
        var specClass = Class.forName("io.modelcontextprotocol.client.McpClient$SyncSpec");
        // SyncSpec 构造校验 transport 非空；给真实 transport 对象但绝不 connect（进程不会拉起）
        var dormantTransport = new StdioClientTransport(
                ServerParameters.builder("true").build(), McpJsonMapper.getDefault());
        var constructor = specClass.getDeclaredConstructor(io.modelcontextprotocol.spec.McpClientTransport.class);
        constructor.setAccessible(true);
        var instance = constructor.newInstance(dormantTransport);
        for (var f : specClass.getDeclaredFields()) {
            if (f.getType() == Duration.class) {
                f.setAccessible(true);
                log.info("[M6] SyncSpec 默认 {} = {}", f.getName(), f.get(instance));
            }
        }
        // 行为佐证：默认配置下正常客户端可完成调用（默认值不阻碍常规场景）
        var client = newClient("npx -y @modelcontextprotocol/server-everything", null);
        try {
            assertThat(client.listTools().tools()).isNotEmpty();
            log.info("[M6] 默认 requestTimeout 下 initialize+listTools 正常");
        } finally {
            client.closeGracefully();
        }
    }
}
