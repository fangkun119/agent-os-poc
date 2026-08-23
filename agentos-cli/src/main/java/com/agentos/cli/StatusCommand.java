package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos status}：查看当前工作区与运行状态概览（TS 8.7）。
 *
 * <p>不需要 Spring 上下文，直接读 .agentos/ 工作区文件即可（TS 8.7）。
 */
@Command(
        name = "status",
        mixinStandardHelpOptions = true,
        description = "查看工作区与运行状态（TS 8.7）")
public class StatusCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7）");
        return 0;
    }
}
