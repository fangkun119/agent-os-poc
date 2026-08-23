package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile show}：查看指定 Agent 的 AGENT.md 配置与指令（TS 8.7）。
 *
 * <p>纯文件操作，不需要 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "show",
        mixinStandardHelpOptions = true,
        description = "查看指定 Agent 的 AGENT.md（TS 8.7）")
public class ProfileShowCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7）");
        return 0;
    }
}
