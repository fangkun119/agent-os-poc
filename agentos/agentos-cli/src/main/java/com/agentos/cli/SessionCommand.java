package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos session}：Session 查询，子命令 list / show（TechnicalSolution.md - 8.7 命令行工具）。
 *
 * <p>对应 TechnicalSolution.md - 8.7 命令行工具 的 13 命令口径中的 {@code session list} / {@code session show}
 * （list 按 last_active_at 倒序取前 N、show 返回 7 项元数据 + messages 全量）。
 */
@Command(
        name = "session",
        mixinStandardHelpOptions = true,
        description = "session：Session 查询（TechnicalSolution.md - 8.7 命令行工具）",
        subcommands = {
                SessionListCommand.class,
                SessionShowCommand.class
        })
public class SessionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具，子命令 list / show）");
        return 0;
    }
}
