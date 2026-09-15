package spike.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M2 依赖矩阵（002 §4 M2 行，离线部分）：容器启动 + 选定组合的关键类可加载。
 * 版本落点断言由 T0-4 的 dependency:tree 三检查承担（bash 层），本类不重复。
 * T6 的 starter 探测在独立类 StarterProbeTest（starter 配置必须类级 properties，放本类
 * 会激活离线测试的 starter 连接）。
 */
@SpringBootTest
class M2DependencyMatrixTest {

    @Autowired
    private ApplicationContext context;

    /** E1 同款：容器能起来（组合 A 依赖无冲突的直接证据） */
    @Test
    void contextStarts() {
        assertThat(context).isNotNull();
    }

    /** 组合 A：mcp SDK（传递依赖 io.modelcontextprotocol.sdk:mcp:0.17.0）在 classpath */
    @Test
    void mcpSdkOnClasspath() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("io.modelcontextprotocol.client.McpSyncClient");
        assertThat(clazz).isNotNull();
    }

    /** 组合 A：spring-ai-mcp 的现成适配类在 classpath（M5 候选一的前置） */
    @Test
    void springAiMcpAdapterOnClasspath() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("org.springframework.ai.mcp.SyncMcpToolCallback");
        assertThat(clazz).isNotNull();
    }
}
