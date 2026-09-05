package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos chat}：本地 CLI 交互对话（TS 8.6）。
 *
 * <p>交互对话委托 agentos-channel-cli 的 CliChannel（TS 8.4），
 * 与 serve / gateway 共享同一份 Profile 配置与 Session 存储，差异只在接入层（TS 8.6）。
 * 需要 LLM 调用，启动 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "chat",
        mixinStandardHelpOptions = true,
        description = "交互对话，委托 CliChannel（TS 8.4）")
public class ChatCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.4）");
        return 0;
    }
}
