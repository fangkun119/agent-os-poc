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
import spike.reactloop.util.ThinkStripper;

/**
 * E4：手动单工具闭环（ReAct 最小回合）。追溯：001 §5 E4，002 §4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class E4SingleToolLoopTest {

    @Autowired
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void singleToolLoop() {
        CountingTools tools = new CountingTools();
        ManualLoop loop = new ManualLoop(chatModel, toolCallingManager,
                ToolCallbacks.from(tools), 10);

        LoopResult r = loop.run("北京 2026-09-07 的天气是什么？必须调用工具查询，不要猜测。");
        System.out.println("[E4] iterations=" + r.iterations()
                + ", toolCallRounds=" + r.toolCallRounds()
                + ", usages=" + r.usages()
                + ", durationsMs=" + r.durationsMs());
        System.out.println("[E4] finalText(剥离后)=" + ThinkStripper.strip(r.finalText()));

        String cleaned = ThinkStripper.strip(r.finalText());
        assertThat(cleaned).containsAnyOf("12°C", "晴");          // 回答基于工具结果
        assertThat(r.toolCallRounds()).isNotEmpty();              // 存在工具调用轮
        assertThat(tools.totalCount()).isGreaterThanOrEqualTo(1); // 工具由我方代码执行
        assertThat(r.hitLimit()).isFalse();                       // 未触发迭代上限
    }
}
