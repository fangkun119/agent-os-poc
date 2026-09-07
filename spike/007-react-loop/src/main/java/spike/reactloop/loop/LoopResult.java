package spike.reactloop.loop;

import java.util.List;

/**
 * 手动循环结果载体。追溯：002 §3.4。
 */
public record LoopResult(
        String finalText,
        int iterations,
        List<String> toolCallRounds,   // 每轮模型请求的工具名列表（无工具调用的轮不记录）
        List<String> usages,           // 每轮 token 用量摘要（in/out）
        List<Long> durationsMs,        // 每轮模型调用毫秒耗时
        boolean hitLimit               // 是否触发迭代上限
) {
}
