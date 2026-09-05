package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile delete}：删除 .agentos/agents/ 下指定的 Agent 目录（TS 8.7）。
 *
 * <p>纯文件操作，不需要 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "delete",
        mixinStandardHelpOptions = true,
        description = "删除指定的 Agent 目录（TS 8.7）")
public class ProfileDeleteCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7）");
        return 0;
    }
}
