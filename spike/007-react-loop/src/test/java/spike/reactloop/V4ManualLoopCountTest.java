package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * V4：手动循环 + 执行计数对照（E3 计数法）——在 MiniMaxChatModel（原生 starter）上复验
 * internalToolExecutionEnabled(false) 的行为与 openai 腿一致：工具执行只来自自研循环，
 * 计数器读数 = 模型发起工具调用的总个数，无双执行。
 * 追溯：002-spec §4 V4；002-plan T4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V4ManualLoopCountTest {

    private static final Logger log = LoggerFactory.getLogger(V4ManualLoopCountTest.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("providerRegistryMap")
    private Map<String, ChatModel> registry;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void manualLoopNoDoubleExecution() {
        ChatModel minimax = registry.get("minimax");
        CountingTools tools = new CountingTools();
        ManualLoop loop = new ManualLoop(minimax, toolCallingManager, ToolCallbacks.from(tools));

        LoopResult r = loop.run("查一下上海 2026-09-15 的天气，必须调用工具。", "MiniMax-M3");

        int expectedCalls = r.toolCallRounds().stream()
                .mapToInt(round -> (int) round.chars().filter(c -> c == ',').count() + 1)
                .sum();

        boolean noDoubleExecution = tools.totalCount() == expectedCalls;
        log.info("[V4] 计数器读数 = {}；模型发起工具调用总个数 = {}；toolCallRounds = {}",
                tools.totalCount(), expectedCalls, r.toolCallRounds());
        log.info("[V4] 结论：{}", noDoubleExecution
                ? "无双执行——每次模型侧工具调用恰被执行一次，全部由自研循环触发"
                : "不吻合，需上报");

        assertThat(r.hitLimit()).isFalse();
        assertThat(tools.totalCount()).as("E3 计数法：执行次数应恰等于模型发起的工具调用个数").isEqualTo(expectedCalls);
        assertThat(r.finalText()).isNotBlank();
    }
}
