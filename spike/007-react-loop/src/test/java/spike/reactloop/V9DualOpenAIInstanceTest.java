package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * V9：双 OpenAI 协议实例并存——openai 腿（MiniMax 兼容端点）与 zhipu 腿（智谱 OpenAI 兼容端点，
 * 以智谱模拟"无 Spring AI Starter、仅提供 OpenAI 兼容 API 的厂商"）各跑单工具闭环、互不干扰。
 * 附带探测：mutate() 派生 builder 是否透出 completionsPath（开放项 8；用反射探测，不引编译依赖）。
 * 追溯：002-spec §4 V9；002-plan T6.5。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V9DualOpenAIInstanceTest {

    private static final Logger log = LoggerFactory.getLogger(V9DualOpenAIInstanceTest.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("providerRegistryMap")
    private Map<String, ChatModel> registry;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Autowired
    private OpenAiApi openAiApi;

    @Test
    void dualOpenAIProtocolInstancesCoexist() {
        // 开放项 8 探测（反射，不做运行时取用）：mutate() 返回的 builder 是否带 completionsPath 方法
        boolean mutateHasCompletionsPath;
        try {
            openAiApi.mutate().getClass().getMethod("completionsPath", String.class);
            mutateHasCompletionsPath = true;
        } catch (NoSuchMethodException e) {
            mutateHasCompletionsPath = false;
        }
        log.info("[V9] 开放项 8：mutate() builder 透出 completionsPath = {}"
                + "（registry 的 zhipu 实例采用全手工 builder，不依赖此探测结果）", mutateHasCompletionsPath);

        String question = "杭州 2026-09-15 的天气？必须调用工具查询。";
        for (String key : new String[] {"openai", "zhipu"}) {
            CountingTools tools = new CountingTools();
            ManualLoop loop = new ManualLoop(registry.get(key), toolCallingManager, ToolCallbacks.from(tools));
            LoopResult r = loop.run(question, null);
            String cleaned = ThinkStripper.strip(r.finalText());
            log.info("[V9] key={} → iterations={}, rounds={}, finalText(剥离后)={}",
                    key, r.iterations(), r.toolCallRounds(), cleaned);
            assertThat(r.hitLimit()).as("key=%s 不应触上限", key).isFalse();
            assertThat(r.toolCallRounds()).as("key=%s 应发起工具调用", key).isNotEmpty();
            assertThat(cleaned).as("key=%s 应含工具结果要素", key).containsAnyOf("12°C", "晴");
        }
        log.info("[V9] 结论：两个 OpenAI 协议实例（不同 base-url/completions-path/凭证）并存互不干扰，"
                + "各自完成闭环——借 OpenAI starter 接'无 Starter 厂商'的形态成立");
    }
}
