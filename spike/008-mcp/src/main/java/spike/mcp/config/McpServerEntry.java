package spike.mcp.config;

import java.util.Map;

/**
 * mcp_servers.yaml 单条配置（TechnicalSolution.md - 6.4 Plugin Tool 方式二 的四字段；002 §3.2）。
 *
 * @param name      server 名称（全局唯一）
 * @param transport 连接方式（本 spike 只认 stdio）
 * @param command   stdio 方式的启动命令字符串（加载时拆为 argv，不经 Shell 解释）
 * @param env       环境变量（值只允许 ${环境变量名} 占位符或非敏感值，TechnicalSolution.md - 8.8 配置与密钥加载 的红线）
 */
public record McpServerEntry(String name, String transport, String command, Map<String, String> env) {
}
