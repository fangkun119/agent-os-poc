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
import org.springframework.context.ApplicationContext;

/**
 * V5：显式映射复验——四键（openai / anthropic / minimax / zhipu）并存、按名取用、无类型扫描。
 * Bean 名诊断打印仅作留档（spec §3.3：诊断打印不进运行时取用路径，运行时取用走 @Qualifier 显式注入构造）。
 * 追溯：002-spec §4 V5；002-plan T5。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V5ProviderRegistryTest {

    private static final Logger log = LoggerFactory.getLogger(V5ProviderRegistryTest.class);

    @Autowired
    private ApplicationContext context;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("providerRegistryMap")
    private Map<String, ChatModel> registry;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void explicitRegistryDualLegLoop() {
        // 诊断留档：容器里的 ChatModel Bean 名（防止后续误读为类型扫描取用；
        // 运行时取用一律 @Qualifier 显式注入构造，见 ProviderRegistry——spec §3.3）
        String[] beanNames = context.getBeanNamesForType(ChatModel.class);
        log.info("[V5] 容器 ChatModel Bean 名（诊断留档）= {}", String.join(", ", beanNames));
        log.info("[V5] 四键映射 = openai / anthropic / minimax / zhipu（@Qualifier 取用名 "
                + "openAiChatModel / anthropicChatModel / miniMaxChatModel，zhipu 为方法内局部构造）");

        assertThat(registry).containsKeys("openai", "anthropic", "minimax", "zhipu");

        for (String key : new String[] {"minimax", "openai"}) {
            CountingTools tools = new CountingTools();
            ManualLoop loop = new ManualLoop(registry.get(key), toolCallingManager, ToolCallbacks.from(tools));
            LoopResult r = loop.run("广州 2026-09-15 的天气？必须调用工具。", "MiniMax-M3");
            String cleaned = ThinkStripper.strip(r.finalText());
            log.info("[V5] key={} → iterations={}, rounds={}, finalText(剥离后)={}",
                    key, r.iterations(), r.toolCallRounds(), cleaned);
            assertThat(r.toolCallRounds()).as("key=%s 应发起工具调用", key).isNotEmpty();
            assertThat(cleaned).as("key=%s 应含工具结果要素", key).containsAnyOf("12°C", "晴");
        }
    }
}
