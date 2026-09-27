package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos session show}：查看单个会话（TechnicalSolution.md - 8.7 命令行工具 / DemandAnalysis.md - 5.11 命令行工具）。
 *
 * <p>返回 7 项元数据（session_id、profile_name、channel、user_id、created_at、
 * last_active_at、last_termination）+ messages 全量（与 GET /api/v1/sessions/{id} 同口径）；
 * 需读 SQLite，属需 Spring 上下文的命令（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "show",
        mixinStandardHelpOptions = true,
        description = "session show：查看单个会话（TechnicalSolution.md - 8.7 命令行工具）")
public class SessionShowCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具，子动作 show）");
        return 0;
    }
}
