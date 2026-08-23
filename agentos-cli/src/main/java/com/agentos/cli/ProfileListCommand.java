package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile list}：列出 .agentos/agents/ 下的 Agent 目录（TS 8.7）。
 *
 * <p>纯文件操作，不需要 Spring 上下文，启动快（TS 8.7）。
 */
@Command(
        name = "list",
        mixinStandardHelpOptions = true,
        description = "列出 .agentos/agents/ 下的 Agent（TS 8.7）")
public class ProfileListCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7）");
        return 0;
    }
}
