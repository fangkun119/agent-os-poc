package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import spike.reactloop.loop.LoopResult;
import spike.reactloop.loop.ManualLoop;
import spike.reactloop.tool.CountingTools;
import spike.reactloop.util.ThinkStripper;

/**
 * E8：双 Provider 显式映射——两条接入路径各自完成闭环。
 * 注意：anthropic 腿同时是 spring.ai.anthropic.base-url 属性的实测点
 * （若属性不生效，请求会打到 api.anthropic.com 并鉴权失败，错误信息会指明端点）。
 * 追溯：001 §5 E8，002 §3.5 / §4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class E8ProviderRegistryTest {

    @Autowired
    @Qualifier("providerRegistryMap")
    private Map<String, ChatModel> registry;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void bothProvidersCloseLoop() {
        assertThat(registry).containsKeys("openai", "anthropic");

        for (String name : List.of("openai", "anthropic")) {
            CountingTools tools = new CountingTools();
            ManualLoop loop = new ManualLoop(registry.get(name), toolCallingManager,
                    ToolCallbacks.from(tools), 10);

            LoopResult r = loop.run("北京 2026-09-07 的天气是什么？必须调用工具查询，不要猜测。");
            String cleaned = ThinkStripper.strip(r.finalText());
            System.out.println("[E8] provider=" + name
                    + ", iterations=" + r.iterations()
                    + ", toolCallRounds=" + r.toolCallRounds()
                    + ", finalText(剥离后)=" + cleaned);

            assertThat(cleaned).as("provider=%s 的最终回答", name).containsAnyOf("12°C", "晴");
            assertThat(r.toolCallRounds()).as("provider=%s 的工具调用轮", name).isNotEmpty();
        }
    }
}
