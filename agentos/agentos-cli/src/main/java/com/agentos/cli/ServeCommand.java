package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos serve}：启动 Web Service，默认端口 8080（TechnicalSolution.md - 7.1 模块组成 / 8.6 三种运行模式）。
 *
 * <p>定时任务随 serve 常驻调度（TechnicalSolution.md - 8.6 三种运行模式 / 8.5 定时任务）。
 * 需要 LLM 调用，启动 Spring 上下文（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "serve",
        mixinStandardHelpOptions = true,
        description = "启动 Web Service（默认端口 8080，定时任务随 serve 常驻调度，TechnicalSolution.md - 7.1 模块组成 / 8.6 三种运行模式）")
public class ServeCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.6 三种运行模式）");
        return 0;
    }
}
