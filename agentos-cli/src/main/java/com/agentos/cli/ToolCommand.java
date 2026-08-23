package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos tool}：Tool 查询，子动作 list（TS 8.7）。
 *
 * <p>对应 TS 8.7 的 12 叶子命令口径中的 {@code tool list}
 * （骨架阶段不拆独立子命令类，动作口径在 description 注明）。
 */
@Command(
        name = "tool",
        mixinStandardHelpOptions = true,
        description = "tool list：列出已注册的 Tool（TS 8.7）")
public class ToolCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.7，子动作 list）");
        return 0;
    }
}
