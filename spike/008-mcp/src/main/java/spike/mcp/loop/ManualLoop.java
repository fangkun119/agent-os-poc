package spike.mcp.loop;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 手动 ReAct 循环（002 §3.7；按 007 README D3 五步标准路径重写，不拷 007 代码）。
 *
 * <p>五步：构建带开关的选项 → 调模型 → 检查工具调用请求 → 无则结束 / 有则经 ToolCallingManager
 * 执行 → 用更新后的对话历史重建请求再调。红线：internalToolExecutionEnabled(false)（002 §5 红线 4）。
 *
 * <p>与 007 的差异仅一处：toolCallbacks 的来源是 MCP 工具（候选一经 SyncMcpToolCallback，
 * 候选二经 AgentOSTool 自适配——见 McpToolCallbacks）。
 */
public class ManualLoop {

    private static final int DEFAULT_MAX_ITERATIONS = 10;

    /**
     * @param userText      用户消息
     * @param chatModel     模型连接（单腿 OPENAI，MiniMax-M2.7）
     * @param toolCallbacks 本轮可用的工具（MCP 工具的 ToolCallback 形态）
     * @param maxIterations 迭代上限（默认 10；触限置 hitLimit 不抛异常）
     */
    public LoopResult run(String userText, ChatModel chatModel, List<ToolCallback> toolCallbacks, int maxIterations) {
        var options = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false) // 第一红线：禁用框架自动执行
                .build();

        Prompt prompt = new Prompt(userText, options);
        var toolCallingManager = org.springframework.ai.model.tool.ToolCallingManager.builder().build();
        var toolCallRounds = new ArrayList<List<String>>();
        var usages = new ArrayList<String>();
        var durationsMs = new ArrayList<Long>();
        int iterations = 0;
        boolean hitLimit = false;
        String finalText = "";

        while (true) {
            long start = System.currentTimeMillis();
            ChatResponse response = chatModel.call(prompt);
            durationsMs.add(System.currentTimeMillis() - start);
            iterations++;
            usages.add(usageOf(response));
            finalText = textOf(response);

            if (!response.hasToolCalls()) {
                break; // 最终回答，循环结束
            }
            toolCallRounds.add(toolNamesOf(response));
            if (iterations >= maxIterations) {
                hitLimit = true; // 迭代上限：防死循环，不抛异常
                break;
            }
            var executed = toolCallingManager.executeToolCalls(prompt, response);
            prompt = new Prompt(executed.conversationHistory(), options);
        }
        return new LoopResult(finalText, iterations, toolCallRounds, usages, durationsMs, hitLimit);
    }

    public LoopResult run(String userText, ChatModel chatModel, List<ToolCallback> toolCallbacks) {
        return run(userText, chatModel, toolCallbacks, DEFAULT_MAX_ITERATIONS);
    }

    private String usageOf(ChatResponse response) {
        var usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        if (usage == null) {
            return "in=?,out=?";
        }
        return "in=" + usage.getPromptTokens() + ",out=" + usage.getCompletionTokens();
    }

    private String textOf(ChatResponse response) {
        return response.getResults().stream()
                .map(g -> g.getOutput().getText())
                .filter(Objects::nonNull)
                .reduce("", (a, b) -> a + b);
    }

    private List<String> toolNamesOf(ChatResponse response) {
        return response.getResults().stream()
                .map(g -> g.getOutput().getToolCalls())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(org.springframework.ai.chat.messages.AssistantMessage.ToolCall::name)
                .toList();
    }
}
