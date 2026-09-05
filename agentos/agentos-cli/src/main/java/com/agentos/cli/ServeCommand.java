package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos serve}：启动 Web Service，默认端口 8080（TS 8.6）。
 *
 * <p>定时任务随 serve 常驻调度（TS 8.6 / 8.5）。
 * 需要 LLM 调用，启动 Spring 上下文（TS 8.7）。
 */
@Command(
        name = "serve",
        mixinStandardHelpOptions = true,
        description = "启动 Web Service（默认端口 8080，定时任务随 serve 常驻调度，TS 8.6）")
public class ServeCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TS 8.6）");
        return 0;
    }
}
