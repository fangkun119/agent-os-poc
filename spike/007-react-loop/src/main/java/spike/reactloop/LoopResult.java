package spike.reactloop;

import java.util.List;

/**
 * 手动循环结果载体（参照第一组思路重写）。
 * usages / durationsMs 与迭代一一对应：一次模型调用 ↔ 一组 (usage, duration)——V6 审计对齐的取数结构。
 * 追溯：002-spec §3.2。
 */
public record LoopResult(
        String finalText,
        int iterations,
        List<String> toolCallRounds,
        List<String> usages,
        List<Long> durationsMs,
        boolean hitLimit) {
}
