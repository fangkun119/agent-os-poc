package spike.mcp.loop;

import java.util.List;

/**
 * 循环结果载体（002 §3.7；字段沿用 007 口径）。
 *
 * @param finalText      最终回答（MiniMax 思考内容可能混在正文，断言前先经 ThinkStripper）
 * @param iterations     实际迭代次数
 * @param toolCallRounds 每轮模型发起的工具名（外层 size = 我方执行 executeToolCalls 的次数）
 * @param usages         每轮 token 用量（一次模型调用 ↔ 一组用量）
 * @param durationsMs    每轮毫秒耗时
 * @param hitLimit       是否触达迭代上限
 */
public record LoopResult(
        String finalText,
        int iterations,
        List<List<String>> toolCallRounds,
        List<String> usages,
        List<Long> durationsMs,
        boolean hitLimit) {

    /** 模型发起工具调用的总轮数（M5 断言"执行计数 = 该值"的一侧） */
    public int totalToolCalls() {
        return toolCallRounds.stream().mapToInt(List::size).sum();
    }
}
