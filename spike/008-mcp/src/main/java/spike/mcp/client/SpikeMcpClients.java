package spike.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import spike.mcp.config.McpServerEntry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP server 连接维护（TS 6.4 McpClientService 的 spike 形态；002 §3.3）。
 *
 * <p>对每条配置建立同步客户端（红线：只用 sync 客户端，TS 4.2 线程约束——001-plan §6.2 第 2 条）。
 * 链路：command 拆 argv → ServerParameters → StdioClientTransport → McpClient.sync(...)
 * → initialize → listTools。
 * 单个 server 连接失败：记录异常并跳过，不阻断其余 server（002 §3.3）。
 *
 * <p>API 面为 io.modelcontextprotocol.sdk:mcp:0.17.0（组合 A；类名经 jar 清单核实，2026-09-14）。
 */
@Component
public class SpikeMcpClients {

    private static final Logger log = LoggerFactory.getLogger(SpikeMcpClients.class);

    /** 实验默认 requestTimeout；SDK 默认值与该取值的实测记录归 M6③（002 开放项 8） */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final List<McpSyncClient> clients = new ArrayList<>();

    /**
     * 按配置逐条建立连接并取回工具清单。
     *
     * @param entries 已校验的 server 配置（McpServersYamlLoader 产出）
     * @return 连接成功的 (server 名, 客户端, 工具清单) 列表
     */
    public List<ConnectedServer> connectAll(List<McpServerEntry> entries) {
        var connected = new ArrayList<ConnectedServer>();
        for (var entry : entries) {
            try {
                connected.add(connect(entry));
                log.info("server [{}] 连接成功", entry.name());
            } catch (Exception e) {
                // 单 server 失败不阻断其余（M6① 的正常路径对照：坏命令在 M6 专项里验证异常形态）
                log.error("server [{}] 连接失败，已跳过", entry.name(), e);
            }
        }
        return connected;
    }

    private ConnectedServer connect(McpServerEntry entry) {
        var argv = entry.command().trim().split("\\s+");
        var params = ServerParameters.builder(argv[0])
                .args(java.util.Arrays.copyOfRange(argv, 1, argv.length))
                .env(entry.env())
                .build();
        var transport = new StdioClientTransport(params,
                io.modelcontextprotocol.json.McpJsonMapper.getDefault());
        var client = McpClient.sync(transport)
                .requestTimeout(REQUEST_TIMEOUT)
                .build();
        client.initialize();
        var toolsResult = client.listTools();
        clients.add(client);
        log.info("server [{}] tools/list 返回 {} 个工具", entry.name(), toolsResult.tools().size());
        return new ConnectedServer(entry.name(), client, toolsResult);
    }

    /** 收尾：优雅关闭全部客户端（002 §3.3；进程残留检查归 M6） */
    public void shutdown() {
        for (var client : clients) {
            try {
                client.closeGracefully();
            } catch (Exception e) {
                log.warn("客户端关闭异常: {}", e.getMessage());
            }
        }
        clients.clear();
    }

    /** 连接成功的 server 一条记录 */
    public record ConnectedServer(String serverName, McpSyncClient client, McpSchema.ListToolsResult tools) {
    }
}
