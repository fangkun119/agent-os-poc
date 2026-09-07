package spike.reactloop.loop;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

/**
 * 手动 ReAct 循环封装（官方标准路径，v1.1.8 同构核验）：
 * call → hasToolCalls → executeToolCalls → conversationHistory 重建 → 再 call。
 * 追溯：002 §3.4。
 */
public class ManualLoop {

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallback[] toolCallbacks;
    private final int maxIterations;

    public ManualLoop(ChatModel chatModel, ToolCallingManager toolCallingManager,
                      ToolCallback[] toolCallbacks, int maxIterations) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbacks = toolCallbacks;
        this.maxIterations = maxIterations;
    }

    public LoopResult run(String userText) {
        List<Long> durationsMs = new ArrayList<>();
        List<String> usageSummaries = new ArrayList<>();
        List<String> toolCallRounds = new ArrayList<>();

        ChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false) // 第一红线：禁用框架自动执行
                .build();

        Prompt prompt = new Prompt(userText, options);
        ChatResponse response = null;
        int iterations = 0;
        boolean hitLimit = false;

        while (true) {
            long start = System.nanoTime();
            response = chatModel.call(prompt);
            durationsMs.add((System.nanoTime() - start) / 1_000_000);
            iterations++;

            var usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            usageSummaries.add(usage == null
                    ? "usage=null"
                    : "in=" + usage.getPromptTokens() + ",out=" + usage.getCompletionTokens());

            if (!response.hasToolCalls()) {
                break; // 最终回答，循环结束
            }

            StringBuilder names = new StringBuilder();
            for (var generation : response.getResults()) {
                AssistantMessage msg = generation.getOutput();
                if (msg.getToolCalls() != null) {
                    for (AssistantMessage.ToolCall tc : msg.getToolCalls()) {
                        if (names.length() > 0) {
                            names.append(",");
                        }
                        names.append(tc.name());
                    }
                }
            }
            toolCallRounds.add(names.toString());

            if (iterations >= maxIterations) {
                hitLimit = true;
                break; // 迭代上限：防死循环（001 §4.3），不抛异常
            }

            var result = toolCallingManager.executeToolCalls(prompt, response);
            prompt = new Prompt(result.conversationHistory(), options);
        }

        String finalText = "";
        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
            finalText = response.getResult().getOutput().getText();
        }
        return new LoopResult(finalText, iterations, toolCallRounds, usageSummaries, durationsMs, hitLimit);
    }
}
