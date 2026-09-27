package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos status}：查看配置和运行状态（DemandAnalysis.md - 5.11 命令行工具 / TechnicalSolution.md - 8.7 命令行工具）。
 *
 * <p>不需要 Spring 上下文，直接读 .agentos/ 工作区文件即可（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "status",
        mixinStandardHelpOptions = true,
        description = "查看配置和运行状态（TechnicalSolution.md - 8.7 命令行工具）")
public class StatusCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.7 命令行工具）");
        return 0;
    }
}
