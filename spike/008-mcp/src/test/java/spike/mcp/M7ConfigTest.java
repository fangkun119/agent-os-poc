package spike.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import spike.mcp.config.McpServersYamlLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M7 mcp_servers.yaml 四字段验证（002 §4 M7 行，离线部分）。
 * 占位符→子进程通路的 live 证据在 T2-4（M3DiscoveryTest 日志）。
 */
@ExtendWith(OutputCaptureExtension.class)
class M7ConfigTest {

    private final McpServersYamlLoader loader = new McpServersYamlLoader();

    private InputStream yaml(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** 用例 1：真实实验稿解析——两条四字段齐全，占位符经注入的环境变量解析 */
    @Test
    void parseExperimentYaml() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/mcp-servers.yaml")) {
            var entries = loader.load(in, Map.of("MCP_SPIKE_PROBE", "probe-value-2026"));

        assertThat(entries).hasSize(2);
        var everything = entries.get(0);
        assertThat(everything.name()).isEqualTo("everything");
        assertThat(everything.transport()).isEqualTo("stdio");
        assertThat(everything.command()).isEqualTo("npx -y @modelcontextprotocol/server-everything");
        assertThat(everything.env()).containsEntry("MCP_SPIKE_PROBE", "probe-value-2026");

        var filesystem = entries.get(1);
        assertThat(filesystem.name()).isEqualTo("filesystem");
        assertThat(filesystem.env()).isEmpty();
        }
    }

    /** 用例 2：name 重复 → 清晰报错 */
    @Test
    void duplicateNameRejected() {
        var yaml = """
                servers:
                  - name: a
                    transport: stdio
                    command: "cmd-a"
                  - name: a
                    transport: stdio
                    command: "cmd-b"
                """;
        assertThatThrownBy(() -> loader.load(yaml(yaml), Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复");
    }

    /** 用例 3：占位符变量缺失 → 清晰报错（消息含变量名与归属 server） */
    @Test
    void missingPlaceholderFailsWithClearMessage() {
        var yaml = """
                servers:
                  - name: needs-var
                    transport: stdio
                    command: "cmd"
                    env:
                      TOKEN: ${NOT_SET_VAR}
                """;
        assertThatThrownBy(() -> loader.load(yaml(yaml), Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOT_SET_VAR")
                .hasMessageContaining("needs-var");
    }

    /** 用例 4：非 stdio transport → 跳过并记录（不阻断其余 server；M1 辅助证据） */
    @Test
    void unsupportedTransportSkipped(CapturedOutput output) {
        var yaml = """
                servers:
                  - name: sse-one
                    transport: sse
                    command: "ignored"
                  - name: stdio-one
                    transport: stdio
                    command: "cmd"
                """;
        var entries = loader.load(yaml(yaml), Map.of());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).name()).isEqualTo("stdio-one");
        assertThat(output.getAll()).contains("sse-one").contains("sse").contains("跳过");
    }

    /** 用例 5：env 值日志脱敏——最多前 5 位前缀（TechnicalSolution.md - 8.8 配置与密钥加载 的红线判定项） */
    @Test
    void envValueMaskedToFiveCharPrefix() {
        assertThat(McpServersYamlLoader.mask("probe-value-2026")).isEqualTo("probe***");
        assertThat(McpServersYamlLoader.mask("abc")).isEqualTo("***");
        assertThat(McpServersYamlLoader.mask(null)).isEqualTo("***");
    }

    /** 用例 6：缺必填字段 → 清晰报错 */
    @Test
    void missingRequiredFieldRejected() {
        var yaml = """
                servers:
                  - name: no-command
                    transport: stdio
                """;
        assertThatThrownBy(() -> loader.load(yaml(yaml), Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("command");
    }
}
