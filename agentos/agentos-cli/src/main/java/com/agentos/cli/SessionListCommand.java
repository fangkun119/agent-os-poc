package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos session list}：按 last_active_at 倒序列出最近 N 个会话（TechnicalSolution.md - 8.7 命令行工具 / DemandAnalysis.md - 5.11 命令行工具）。
 *
 * <p>每项 3 字段：session_id、last_active_at、messages 首条预览（无 messages 空串）；
 * 需读 SQLite，属需 Spring 上下文的命令（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "list",
        mixinStandardHelpOptions = true,
        description = "session list：列出已有 Session（TechnicalSolution.md - 8.7 命令行工具）")
public class SessionListCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具，子动作 list）");
        return 0;
    }
}
