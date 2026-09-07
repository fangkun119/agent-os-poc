package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * E6：工具参数描述正确性——模型按 @Tool/@ToolParam 描述正确填充参数。
 * 偶发偏差允许固定次数重跑（最多 2 次）并记录实际值。追溯：001 §5 E6，002 §4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class E6ToolSchemaTest {

    @Autowired
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void toolArgsMatchGivenValues() {
        CountingTools tools = new CountingTools();
        ManualLoop loop = new ManualLoop(chatModel, toolCallingManager,
                ToolCallbacks.from(tools), 10);

        Map<String, String> args = Map.of();
        boolean pass = false;
        for (int attempt = 1; attempt <= 2 && !pass; attempt++) {
            tools.reset();
            LoopResult r = loop.run("查北京 2026-09-07 的天气，必须调用工具查询，不要猜测。");
            args = tools.lastArgs();
            pass = "北京".equals(args.get("city")) && "2026-09-07".equals(args.get("date"));
            System.out.println("[E6] attempt=" + attempt + ", args=" + args
                    + ", pass=" + pass);
        }

        assertThat(pass).as("实际收到的入参：%s", args).isTrue();
        assertThat(args).containsEntry("city", "北京").containsEntry("date", "2026-09-07");
    }
}
