package spike.mcp;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.model.ChatModel;
import spike.mcp.client.SpikeMcpClients;
import spike.mcp.config.McpServersYamlLoader;
import spike.mcp.loop.LoopResult;
import spike.mcp.loop.ManualLoop;
import spike.mcp.tool.McpToolAdapter;
import spike.mcp.tool.McpToolCallbacks;
import spike.mcp.tool.ToolRegistry;
import spike.mcp.util.ThinkStripper;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M5 手动循环闭环（002 §4 M5 行，live，需 key）：LLM 真调 MCP 工具完成一次任务闭环。
 * 两候选各跑一次（候选一 SyncMcpToolCallback / 候选二 AgentOSTool 自适配），
 * 双执行排除证据 = 执行计数增量 == 模型发起工具调用轮数（007 E3 计数法）。
 */
@SpringBootTest
@Tag("live")
class M5LoopClosedTest {

    private static final Logger log = LoggerFactory.getLogger(M5LoopClosedTest.class);

    /** Bean 按名取用（TS 3.2 红线的执行姿势；Bean 名为 007 E8 实证的 openAiChatModel） */
    @Autowired
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @Autowired
    private McpToolAdapter adapter;

    private static final String TASK = "请调用 echo 工具一次，message 参数填 spike-008-m5。"
            + "工具返回后会得到一行形如 Echo: spike-008-m5 的文本。"
            + "你的最终回答必须完整包含这行原文（含 Echo: 前缀），一字不漏。";

    /** 候选一：spring-ai-mcp 现成 SyncMcpToolCallback */
    @Test
    void closedLoopWithSyncMcpToolCallback() throws Exception {
        var counter = new AtomicInteger();
        var clients = new SpikeMcpClients();
        try {
            var everything = connectEverything(clients);
            var callbacks = McpToolCallbacks.fromSyncAdapters(everything, counter);
            assertThat(callbacks).hasSize(13);

            var result = new ManualLoop().run(TASK, chatModel, callbacks);
            report("候选一 SyncMcpToolCallback", result, counter);
            assertClosedLoop(result, counter);
        } finally {
            clients.shutdown();
        }
    }

    /** 候选二：AgentOSTool（McpToolAdapter）自适配成 ToolCallback */
    @Test
    void closedLoopWithAgentOSToolAdapter() throws Exception {
        var counter = new AtomicInteger();
        var clients = new SpikeMcpClients();
        try {
            var everything = connectEverything(clients);
            var callbacks = McpToolCallbacks.fromAgentOSTools(everything, adapter, new ToolRegistry(), counter);
            assertThat(callbacks).hasSize(13);

            var result = new ManualLoop().run(TASK, chatModel, callbacks);
            report("候选二 AgentOSTool 自适配", result, counter);
            assertClosedLoop(result, counter);
        } finally {
            clients.shutdown();
        }
    }

    private SpikeMcpClients.ConnectedServer connectEverything(SpikeMcpClients clients) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/mcp-servers.yaml")) {
            var entries = new McpServersYamlLoader().load(in, System.getenv());
            var connected = clients.connectAll(entries);
            return connected.stream()
                    .filter(s -> s.serverName().equals("everything"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("everything server 未连上"));
        }
    }

    private void report(String label, LoopResult r, AtomicInteger counter) {
        log.info("[M5] {} -> iterations={} toolCallRounds={} 执行计数={} usages={} durationsMs={} hitLimit={}",
                label, r.iterations(), r.toolCallRounds(), counter.get(), r.usages(), r.durationsMs(), r.hitLimit());
        log.info("[M5] {} finalText(剥离后) = {}", label, ThinkStripper.strip(r.finalText()));
    }

    /** 002 §4 M5 判定：闭环 + 开关生效 + 无双执行 */
    private void assertClosedLoop(LoopResult r, AtomicInteger counter) {
        var stripped = ThinkStripper.strip(r.finalText());
        assertThat(stripped).as("最终回答应含工具结果要素").contains("Echo: spike-008-m5");
        assertThat(r.hitLimit()).isFalse();
        assertThat(r.totalToolCalls()).as("模型应至少发起一次工具调用").isGreaterThanOrEqualTo(1);
        assertThat(counter.get())
                .as("双执行排除：工具执行计数应等于模型发起的调用轮数")
                .isEqualTo(r.totalToolCalls());
    }
}
