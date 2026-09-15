package spike.reactloop;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

/**
 * 手动 ReAct 循环（参照第一组 D3 路径重写，非拷贝）：
 * internalToolExecutionEnabled(false) → call → hasToolCalls → executeToolCalls
 * → conversationHistory 重建 → 再 call，直到无工具调用或触迭代上限。
 * 相对第一组的差异：支持 per-call options 覆盖模型名（三级选择的第 3 级）。
 * 追溯：002-spec §3.2；007 README D3。
 */
public class ManualLoop {

    private static final int DEFAULT_MAX_ITERATIONS = 10;

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallback[] toolCallbacks;
    private final int maxIterations;

    public ManualLoop(ChatModel chatModel, ToolCallingManager toolCallingManager, ToolCallback[] toolCallbacks) {
        this(chatModel, toolCallingManager, toolCallbacks, DEFAULT_MAX_ITERATIONS);
    }

    public ManualLoop(ChatModel chatModel, ToolCallingManager toolCallingManager, ToolCallback[] toolCallbacks,
            int maxIterations) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbacks = toolCallbacks;
        this.maxIterations = maxIterations;
    }

    /**
     * @param perCallModel 为 null/空时用实例缺省模型；否则经 options 覆盖（第 3 级模型选择）
     */
    public LoopResult run(String userText, String perCallModel) {
        List<Long> durationsMs = new ArrayList<>();
        List<String> usages = new ArrayList<>();
        List<String> toolCallRounds = new ArrayList<>();

        ToolCallingChatOptions.Builder builder = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false); // 红线：禁用框架自动执行，工具调度全在自研循环
        if (perCallModel != null && !perCallModel.isBlank()) {
            builder.model(perCallModel);
        }
        ToolCallingChatOptions options = builder.build();

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
            usages.add(usage == null ? "usage=null"
                    : "in=" + usage.getPromptTokens() + ",out=" + usage.getCompletionTokens());

            if (!response.hasToolCalls()) {
                break; // 最终自然语言回答，循环结束
            }

            List<String> names = new ArrayList<>();
            for (var generation : response.getResults()) {
                AssistantMessage msg = generation.getOutput();
                if (msg.getToolCalls() != null) {
                    for (AssistantMessage.ToolCall call : msg.getToolCalls()) {
                        names.add(call.name());
                    }
                }
            }
            toolCallRounds.add("round=" + iterations + ":" + String.join(",", names));

            if (iterations >= maxIterations) {
                hitLimit = true; // 防死循环上限，不抛异常
                break;
            }

            var result = toolCallingManager.executeToolCalls(prompt, response);
            prompt = new Prompt(result.conversationHistory(), options);
        }

        String finalText = "";
        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
            finalText = response.getResult().getOutput().getText();
        }
        return new LoopResult(finalText, iterations, toolCallRounds, usages, durationsMs, hitLimit);
    }
}
