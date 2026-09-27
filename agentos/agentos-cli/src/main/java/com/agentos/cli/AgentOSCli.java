package com.agentos.cli;

import picocli.CommandLine.Command;

/**
 * AgentOS 命令行入口，整个 AgentOS 的 main 函数（TechnicalSolution.md - 8.7 命令行工具）。
 *
 * <p>启动策略：不需要 Spring 上下文的命令（init、status、profile list·create·show·delete、provider list，共 7 个）直接文件操作启动快；
 * 需要 Spring 上下文的（chat、serve、gateway、tool list、session list·show，共 6 个）以完整 Spring 上下文启动。
 * 注册 13 个叶子子命令：init / status / chat / serve / gateway /
 * profile list·create·show·delete / provider list / tool list / session list·show（TechnicalSolution.md - 8.7 命令行工具）。
 */
@Command(
        name = "agentos",
        mixinStandardHelpOptions = true,
        version = "agentos 0.1.0-SNAPSHOT",
        description = "AgentOS 命令行入口（TechnicalSolution.md - 8.7 命令行工具）",
        subcommands = {
                InitCommand.class,
                StatusCommand.class,
                ChatCommand.class,
                ServeCommand.class,
                GatewayCommand.class,
                ProfileCommand.class,
                ProviderCommand.class,
                ToolCommand.class,
                SessionCommand.class
        })
public class AgentOSCli {

    /**
     * main 入口：统一交 Picocli 解析子命令并透传退出码（TechnicalSolution.md - 8.7 命令行工具）。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        int exitCode = new picocli.CommandLine(new AgentOSCli()).execute(args);
        System.exit(exitCode);
    }
}
