package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile list}：列出 .agentos/agents/ 下的 Agent 目录（TechnicalSolution.md - 8.7 命令行工具）。
 *
 * <p>纯文件操作，不需要 Spring 上下文，启动快（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "list",
        mixinStandardHelpOptions = true,
        description = "列出 .agentos/agents/ 下的 Agent（TechnicalSolution.md - 8.7 命令行工具）")
public class ProfileListCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具）");
        return 0;
    }
}
