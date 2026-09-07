package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * E7：token 用量与耗时采集（审计对齐）——一次模型调用 ↔ 一组 (usage, duration)。
 * 追溯：001 §5 E7，002 §4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class E7UsageCaptureTest {

    @Autowired
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void usageAndDurationCaptured() {
        CountingTools tools = new CountingTools();
        ManualLoop loop = new ManualLoop(chatModel, toolCallingManager,
                ToolCallbacks.from(tools), 10);

        LoopResult r = loop.run("北京 2026-09-07 的天气是什么？必须调用工具查询，不要猜测。");

        System.out.println("[E7] iterations=" + r.iterations());
        System.out.println("[E7] usages=" + r.usages());
        System.out.println("[E7] durationsMs=" + r.durationsMs());

        // 判定：每轮模型调用都有一组非零 (token 用量, 毫秒耗时)
        assertThat(r.usages()).hasSize(r.iterations());
        assertThat(r.durationsMs()).hasSize(r.iterations());
        assertThat(r.usages()).allSatisfy(u -> assertThat(u).contains("in=").contains("out="));
        assertThat(r.durationsMs()).allSatisfy(d -> assertThat(d).isGreaterThan(0));
    }
}
