package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos gateway}：守护进程模式（TechnicalSolution.md - 8.6 三种运行模式）。
 *
 * <p>核心阶段与 serve 同义（预挂 CLI Channel），多 Channel 挂载扩展阶段启用（TechnicalSolution.md - 8.6 三种运行模式）。
 * 需要 LLM 调用，启动 Spring 上下文（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "gateway",
        mixinStandardHelpOptions = true,
        description = "守护进程模式（核心阶段与 serve 同义，预挂 CLI Channel，TechnicalSolution.md - 8.6 三种运行模式）")
public class GatewayCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.6 三种运行模式）");
        return 0;
    }
}
