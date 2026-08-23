package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos gateway}：守护进程模式（TS 8.6）。
 *
 * <p>核心阶段与 serve 同义（预挂 CLI Channel），多 Channel 挂载扩展阶段启用（TS 8.6）。
 * 需要 LLM 调用，启动 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "gateway",
        mixinStandardHelpOptions = true,
        description = "守护进程模式（核心阶段与 serve 同义，预挂 CLI Channel，TS 8.6）")
public class GatewayCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.6）");
        return 0;
    }
}
