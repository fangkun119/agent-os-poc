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
import spike.reactloop.tool.ChainTools;
import spike.reactloop.util.ThinkStripper;

/**
 * E5：多轮工具链 + 迭代上限。追溯：001 §5 E5，002 §4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class E5MultiToolChainTest {

    @Autowired
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void multiToolChain() {
        ChainTools chain = new ChainTools();
        ManualLoop loop = new ManualLoop(chatModel, toolCallingManager,
                ToolCallbacks.from(chain), 10);

        LoopResult r = loop.run("依次完成三步：第一步查询目标城市；第二步查询该城市的日期；"
                + "第三步查询该城市该日期的天气。每一步都必须调用对应工具，禁止猜测。"
                + "全部完成后，输出最终回答，最终回答必须同时包含三项信息：城市名、日期、天气状况，缺一不可。");
        System.out.println("[E5] iterations=" + r.iterations()
                + ", toolCallRounds=" + r.toolCallRounds()
                + ", hitLimit=" + r.hitLimit());
        System.out.println("[E5] finalText(剥离后)=" + ThinkStripper.strip(r.finalText()));

        assertThat(r.hitLimit()).isFalse();                        // 上限内正确终止
        assertThat(String.join("|", r.toolCallRounds())).contains("getCity");   // 三步都被调用
        assertThat(String.join("|", r.toolCallRounds())).contains("getDate");
        assertThat(String.join("|", r.toolCallRounds())).contains("getWeather2");

        String cleaned = ThinkStripper.strip(r.finalText());
        assertThat(cleaned).contains("2026-09-07");                // 只能来自 getDate 工具
        assertThat(cleaned).contains("晴");                         // 只能来自 getWeather2 工具
    }
}
