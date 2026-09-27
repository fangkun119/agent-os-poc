package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile}：操作 .agentos/agents/ 下的 Agent 目录（TechnicalSolution.md - 8.7 命令行工具）。
 *
 * <p>子命令 list / create / show / delete；create 生成最小 AGENT.md 模板。
 * 与 init / status / chat / serve / gateway / provider list / tool list / session list / show 一起构成
 * TechnicalSolution.md - 8.7 命令行工具 的 13 个叶子子命令口径（profile 四个 + 其余九个）。
 * list / create / show / delete 均为文件操作，不需要 Spring 上下文（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "profile",
        mixinStandardHelpOptions = true,
        description = "操作 .agentos/agents/ 下的 Agent 目录（TechnicalSolution.md - 8.7 命令行工具）",
        subcommands = {
                ProfileListCommand.class,
                ProfileCreateCommand.class,
                ProfileShowCommand.class,
                ProfileDeleteCommand.class
        })
public class ProfileCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具，子命令 list / create / show / delete）");
        return 0;
    }
}
