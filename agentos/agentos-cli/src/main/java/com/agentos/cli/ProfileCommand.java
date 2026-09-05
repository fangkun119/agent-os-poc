package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile}：操作 .agentos/agents/ 下的 Agent 目录（TS 8.7）。
 *
 * <p>子命令 list / create / show / delete；create 生成最小 AGENT.md 模板。
 * 与 init / status / provider list / tool list / session list 一起构成
 * TS 8.7 的 12 个叶子子命令口径（profile 四个 + 其余八个）。
 * list / create / show / delete 均为文件操作，不需要 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "profile",
        mixinStandardHelpOptions = true,
        description = "操作 .agentos/agents/ 下的 Agent 目录（TS 8.7）",
        subcommands = {
                ProfileListCommand.class,
                ProfileCreateCommand.class,
                ProfileShowCommand.class,
                ProfileDeleteCommand.class
        })
public class ProfileCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7，子命令 list / create / show / delete）");
        return 0;
    }
}
