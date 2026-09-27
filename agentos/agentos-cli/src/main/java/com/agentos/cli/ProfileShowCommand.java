package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile show}：查看指定 Agent 的 AGENT.md 配置与指令（TechnicalSolution.md - 8.7 命令行工具）。
 *
 * <p>纯文件操作，不需要 Spring 上下文（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "show",
        mixinStandardHelpOptions = true,
        description = "查看指定 Agent 的 AGENT.md（TechnicalSolution.md - 8.7 命令行工具）")
public class ProfileShowCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具）");
        return 0;
    }
}
