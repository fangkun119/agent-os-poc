package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import spike.reactloop.tool.CountingTools;

/**
 * E3：关闭自动执行生效、无双执行——开/关两组对照。
 * 组 1：开关保持默认（开），代码不写任何工具执行逻辑 → 计数增加 = 框架自动执行实证。
 * 组 2：开关关，工具只由我方代码经 ToolCallingManager 执行 → 计数只由我方调用产生（无双执行）。
 * 追溯：001 §5 E3，002 §4。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class E3AutoExecComparisonTest {

    private static final String QUESTION =
            "北京 2026-09-07 的天气是什么？必须调用工具查询，不要猜测。";

    @Autowired
    @Qualifier("openAiChatModel") // 两个 starter 各有一个 ChatModel Bean——按名指定，正是 E8 显式映射要解决的问题
    private ChatModel chatModel; // 主实例：OPENAI Provider

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void group1_defaultSwitch_frameworkAutoExecutes() {
        CountingTools tools = new CountingTools();
        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        ChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks) // 开关保持默认（开），不写任何自研执行代码
                .build();

        ChatResponse response = chatModel.call(new Prompt(QUESTION, options));

        assertThat(response).isNotNull();
        // 我方代码零执行逻辑——计数增加即为框架自动执行的直接证据
        assertThat(tools.totalCount()).isGreaterThanOrEqualTo(1);
        System.out.println("[E3-组1] 框架自动执行：计数=" + tools.totalCount());
    }

    @Test
    void group2_switchOff_selfExecutionOnly() {
        CountingTools tools = new CountingTools();
        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        ChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .internalToolExecutionEnabled(false) // 开关关
                .build();

        int before = tools.totalCount();
        Prompt prompt = new Prompt(QUESTION, options);
        ChatResponse response = chatModel.call(prompt);

        int executedByUs = 0; // 我方经 ToolCallingManager 执行的工具调用数
        int rounds = 0;
        int guard = 0;
        while (response != null && response.hasToolCalls() && guard++ < 3) {
            for (var generation : response.getResults()) {
                AssistantMessage msg = generation.getOutput();
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    executedByUs++;
                }
            }
            var result = toolCallingManager.executeToolCalls(prompt, response);
            prompt = new Prompt(result.conversationHistory(), options);
            response = chatModel.call(prompt);
            rounds++;
        }

        int after = tools.totalCount();
        // 判定：执行次数 = 我方执行的工具调用数（无双执行 = 框架侧计数贡献为 0）
        assertThat(executedByUs).isGreaterThanOrEqualTo(1);
        assertThat(after - before).isEqualTo(executedByUs);
        System.out.println("[E3-组2] 手动执行：轮数=" + rounds
                + "，我方执行工具调用=" + executedByUs
                + "，计数增量=" + (after - before));
    }
}
