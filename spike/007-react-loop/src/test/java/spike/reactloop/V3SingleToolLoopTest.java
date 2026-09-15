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
 * V3：MiniMax 腿（原生 starter + 原生协议）单工具闭环——路径 B 的核心实测。
 * V7 内建于本测试：剥离前后文本、有无 &lt;think&gt; 残留全部打印（证据流，不重复烧调用）。
 * 候选 A（base-url 纯主机）的正向验证也由本闭环承担（spec §4.2 细则）。
 * 追溯：002-spec §4 V3/V7；002-plan T3。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V3SingleToolLoopTest {

    private static final Logger log = LoggerFactory.getLogger(V3SingleToolLoopTest.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("providerRegistryMap")
    private Map<String, ChatModel> registry;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void minimaxLegSingleToolLoop() {
        ChatModel minimax = registry.get("minimax");
        CountingTools tools = new CountingTools();
        ManualLoop loop = new ManualLoop(minimax, toolCallingManager,
                ToolCallbacks.from(tools));

        LoopResult r = loop.run("北京 2026-09-15 的天气是什么？必须调用工具查询，不要猜测。", "MiniMax-M3");

        String raw = r.finalText();
        String cleaned = ThinkStripper.strip(raw);
        boolean rawHasThink = raw != null && raw.contains("<think>");

        log.info("[V3] iterations={}, toolCallRounds={}, hitLimit={}", r.iterations(), r.toolCallRounds(), r.hitLimit());
        log.info("[V7] 剥离前（截断 400 字）: {}", raw == null ? "null" : raw.substring(0, Math.min(400, raw.length())));
        log.info("[V7] 剥离后: {}", cleaned);
        log.info("[V7] 思考标签出现={}，剥离后残留={}", rawHasThink, cleaned.contains("<think>"));

        assertThat(r.hitLimit()).as("不应触发迭代上限").isFalse();
        assertThat(r.toolCallRounds()).as("模型应发起过工具调用").isNotEmpty();
        assertThat(cleaned).as("最终回答应含工具结果要素").containsAnyOf("12°C", "晴");
        assertThat(cleaned).as("剥离后不应残留思考标签").doesNotContain("<think>");
        assertThat(tools.lastArgs()).as("工具应被真实执行并收到入参").isNotEmpty();
    }
}
