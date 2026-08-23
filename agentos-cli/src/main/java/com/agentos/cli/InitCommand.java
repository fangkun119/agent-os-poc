package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos init}：初始化 .agentos/ 工作区（TS 8.1）。
 *
 * <p>生成 agents/ skills/ memory/MEMORY.md logs/ mcp_servers.yaml agentos.db
 * 及三个 Bootstrap 文件（AGENTS.md / SOUL.md / USER.md）。
 * 纯文件操作，不需要 Spring 上下文，启动快（TS 8.7）。
 */
@Command(
        name = "init",
        mixinStandardHelpOptions = true,
        description = "初始化 .agentos/ 工作区（TS 8.1）")
public class InitCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.1）");
        return 0;
    }
}
