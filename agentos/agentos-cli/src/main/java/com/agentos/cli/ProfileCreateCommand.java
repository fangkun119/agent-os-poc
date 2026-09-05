package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos profile create}：在 .agentos/agents/ 下新建 Agent 目录（TS 8.7）。
 *
 * <p>生成最小 AGENT.md 模板（frontmatter = 配置，正文 = 指令，TS 8.2）。
 * 纯文件操作，不需要 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "create",
        mixinStandardHelpOptions = true,
        description = "新建 Agent 目录并生成最小 AGENT.md 模板（TS 8.7）")
public class ProfileCreateCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7）");
        return 0;
    }
}
