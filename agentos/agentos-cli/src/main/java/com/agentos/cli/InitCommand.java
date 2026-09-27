package com.agentos.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/**
 * {@code agentos init}：初始化 .agentos/ 工作区（TechnicalSolution.md - 8.1 工作区初始化）。
 *
 * <p>生成 agents/ skills/ memory/ logs/ 四个子目录、mcp_servers.yaml 模板
 * 及三个 Bootstrap 文件（AGENTS.md / SOUL.md / USER.md）；agentos.db
 * 由 JPA 首次启用 SQLite 持久化时自动创建（TechnicalSolution.md - 8.1 工作区初始化 / DemandAnalysis.md - 5.1 工作区初始化），本命令不创建。
 * <p>幂等边界（2026-10-01 Q8（init 幂等边界）裁决，TechnicalSolution.md - 8.1 工作区初始化）：目录缺失则补建；
 * 文件缺失跳过不补（删掉的模板不恢复）；文件已存在一律不覆盖（哪怕内容为空）。
 * <p>纯文件操作，不需要 Spring 上下文，启动快（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "init",
        mixinStandardHelpOptions = true,
        description = "初始化 .agentos/ 工作区（TechnicalSolution.md - 8.1 工作区初始化）")
public class InitCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("尚未实现：骨架（TechnicalSolution.md - 8.1 工作区初始化）");
        return 0;
    }
}
