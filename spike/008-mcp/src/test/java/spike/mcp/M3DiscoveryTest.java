package spike.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spike.mcp.client.SpikeMcpClients;
import spike.mcp.config.McpServerEntry;
import spike.mcp.config.McpServersYamlLoader;
import spike.mcp.tool.AgentOSTool;
import spike.mcp.tool.McpToolAdapter;
import spike.mcp.tool.ToolRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M3 工具发现 + M7 通路证据（002 §4 M3 行，live：npx 拉起两个真实 server 子进程）。
 * 前置：export MCP_SPIKE_PROBE=probe-value-2026（M7 占位符通路的注入端）。
 */
@SpringBootTest
@Tag("live")
class M3DiscoveryTest {

    private static final Logger log = LoggerFactory.getLogger(M3DiscoveryTest.class);

    @Autowired
    private McpToolAdapter adapter;

    @Test
    void discoverAndRegisterAllTools() throws Exception {
        // filesystem server 的允许目录：spike/008-mcp/sandbox-dir（相对 surefire 工作目录）
        var sandbox = Path.of("sandbox-dir");
        Files.createDirectories(sandbox);
        Files.writeString(sandbox.resolve("sample.txt"), "spike 008 M3 预置内容");

        // 1. 配置加载：env 解析用真实进程环境（MCP_SPIKE_PROBE 由 mvn 进程继承）
        var probe = System.getenv("MCP_SPIKE_PROBE");
        assertThat(probe).as("前置：先 export MCP_SPIKE_PROBE").isNotBlank();
        List<McpServerEntry> entries;
        try (var in = getClass().getResourceAsStream("/mcp-servers.yaml")) {
            entries = new McpServersYamlLoader().load(in, System.getenv());
        }
        assertThat(entries).hasSize(2);
        // M7 通路证据（注入端）：占位符解析值 = 进程环境值
        assertThat(entries.get(0).env().get("MCP_SPIKE_PROBE")).isEqualTo(probe);
        log.info("[M7-PROBE] 注入端解析值 = {}（非密钥，探测变量）", probe);

        // 2. 连接 + 发现（单个失败跳过；两 server 都应成功）
        var clients = new SpikeMcpClients();
        try {
            var connected = clients.connectAll(entries);
            assertThat(connected).as("两个 server 都应连接成功").hasSize(2);

            // 3. 批量注册（M3：注册数 = listTools 返回数之和）
            var registry = new ToolRegistry();
            int listed = 0;
            for (var server : connected) {
                adapter.registerAll(server, registry);
                listed += server.tools().tools().size();
                log.info("[M3] server={} tools/list 共 {} 个: {}",
                        server.serverName(),
                        server.tools().tools().size(),
                        server.tools().tools().stream().map(t -> t.name()).toList());
            }
            assertThat(registry.list()).hasSize(listed);

            // 4. 逐工具四方法非空 + schema 是合法 JSON（M3 直映射实证）
            var om = new ObjectMapper();
            for (AgentOSTool tool : registry.list()) {
                assertThat(tool.getName()).isNotBlank();
                assertThat(tool.getDescription()).isNotBlank();
                assertThat(tool.getInputSchema()).isNotBlank();
                assertThat(om.readTree(tool.getInputSchema())).isNotNull();
            }
            log.info("[M3] 全部 {} 个工具四方法非空、schema 可解析", listed);

            // 5. subset 白名单过滤（模拟 Profile tools 字段，TechnicalSolution.md - 6.6 ToolRegistry）
            var first = registry.list().get(0).getName();
            var subset = registry.subset(List.of(first, "no-such-tool"));
            assertThat(subset).hasSize(1);
            assertThat(subset.get(0).getName()).isEqualTo(first);
        } finally {
            clients.shutdown();
        }
    }
}
