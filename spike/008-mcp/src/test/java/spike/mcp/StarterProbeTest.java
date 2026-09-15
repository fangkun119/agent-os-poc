package spike.mcp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import spike.mcp.client.SpikeMcpClients;
import spike.mcp.config.McpServersYamlLoader;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T6 组合 D：starter 自动装配 vs 自持连接（001-plan §3 T6；-Dstarter.probe=true 才启用）。
 * 判定：①显式 enabled + 配置后 starter 拉起连接与工具；②自持 McpClients 同时连同一 server
 * → 同一 server 被拉起两个子进程（"谁拥有连接"的冲突实证）。
 * 注：T6-4 编排微调——探测从 M2DependencyMatrixTest 分支改为独立类（starter 配置必须类级
 * properties，放 M2 类会激活离线测试的 starter 连接），已记入 plan 偏差。
 */
@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=true",
        "spring.ai.mcp.client.type=SYNC",
        "spring.ai.mcp.client.stdio.connections.everything.command=npx",
        "spring.ai.mcp.client.stdio.connections.everything.args[0]=-y",
        "spring.ai.mcp.client.stdio.connections.everything.args[1]=@modelcontextprotocol/server-everything"
})
@Tag("live")
@EnabledIfSystemProperty(named = "starter.probe", matches = "true")
class StarterProbeTest {

    private static final Logger log = LoggerFactory.getLogger(StarterProbeTest.class);

    @Autowired
    private ApplicationContext context;

    @Test
    void starterTakesConnectionWhileSelfManagedAlsoWorks() throws Exception {
        // ① starter 自动装配：Bean 出现、连接由 starter 按 spring.ai.mcp.client.* 属性拉起
        var provider = context.getBeanProvider(SyncMcpToolCallbackProvider.class).getIfAvailable();
        assertThat(provider).as("显式 enabled 后 starter 应注册 ToolCallbackProvider").isNotNull();
        var starterTools = provider.getToolCallbacks();
        log.info("[T6] starter 自动装配工具数 = {}", starterTools.length);
        assertThat(starterTools.length).as("starter 按自身配置拉起 everything 并发现工具").isGreaterThan(0);

        // 计数窗口：starter 连接存活 + 自持连接刚建立、尚未关闭
        var clients = new SpikeMcpClients();
        try {
            InputStream in = getClass().getResourceAsStream("/mcp-servers.yaml");
            var entries = new McpServersYamlLoader().load(in, System.getenv());
            var connected = clients.connectAll(entries);
            assertThat(connected).as("自持连接照常建立（互不感知）").isNotEmpty();

            // ② 同一 server 双子进程计数（此时 starter 进程与自持进程都活着）
            var p = new ProcessBuilder("pgrep", "-f", "@modelcontextprotocol/server-everything").start();
            var output = new String(p.getInputStream().readAllBytes()).trim();
            var count = output.isEmpty() ? 0 : output.split("\n").length;
            log.info("[T6] server-everything 子进程数 = {}（≥2 即双连接冲突实证）", count);
            assertThat(count).as("starter 与自持各拉一份子进程").isGreaterThanOrEqualTo(2);
        } finally {
            clients.shutdown();
        }
    }
}
