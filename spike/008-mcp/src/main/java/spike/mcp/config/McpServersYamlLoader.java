package spike.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * mcp_servers.yaml 加载器（002 §3.2）。
 *
 * <p>职责：解析四字段（name/transport/command/env）→ name 唯一校验 → 非 stdio transport
 * 跳过并记录（对齐 TS 8.2"校验失败不阻断启动但记录错误日志"）→ env 值中的 ${环境变量名}
 * 从注入的环境变量解析，缺失给清晰报错。
 *
 * <p>密钥纪律（TS 8.8）：本类日志中 env 值最多输出前 5 位前缀。
 * 解析库用 SnakeYAML（Spring Boot 自带；001-plan §6.1 第 6 条，与正式技术栈同库）。
 */
public class McpServersYamlLoader {

    private static final Logger log = LoggerFactory.getLogger(McpServersYamlLoader.class);

    private static final String SUPPORTED_TRANSPORT = "stdio";

    /**
     * @param yamlStream 配置文件流
     * @param envResolver 环境变量来源（生产传 System.getenv()；测试注入构造值——002 §3.2 注入式设计）
     * @return 校验通过的 server 配置列表（跳过项不包含在内）
     */
    public List<McpServerEntry> load(InputStream yamlStream, Map<String, String> envResolver) {
        var root = new Yaml().load(yamlStream);
        if (!(root instanceof Map<?, ?> rootMap)) {
            throw new IllegalStateException("mcp_servers.yaml 顶层必须是 servers: 列表结构");
        }
        var rawServers = rootMap.get("servers");
        if (!(rawServers instanceof List<?> serverList)) {
            throw new IllegalStateException("mcp_servers.yaml 缺少 servers: 列表");
        }

        var seenNames = new java.util.HashSet<String>();
        var entries = new ArrayList<McpServerEntry>();
        for (Object item : serverList) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IllegalStateException("servers 列表项必须是映射结构");
            }
            var raw = castToStringMap(map);
            String name = requireField(raw, "name");
            String transport = requireField(raw, "transport");
            String command = requireField(raw, "command");

            if (!seenNames.add(name)) {
                throw new IllegalStateException("server name 重复: " + name);
            }
            if (!SUPPORTED_TRANSPORT.equals(transport)) {
                // 非 stdio：记录后跳过，不阻断其余 server（M1 辅助证据；SSE 属扩展阶段）
                log.warn("server [{}] 的 transport=[{}] 不受支持（核心阶段仅 stdio），已跳过", name, transport);
                continue;
            }

            var resolvedEnv = new LinkedHashMap<String, String>();
            // env 是嵌套 Map，从原始 map 取——castToStringMap 会把嵌套结构扁平化成字符串，不能用
            if (map.get("env") instanceof Map<?, ?> envMap) {
                for (var e : castToStringMap(envMap).entrySet()) {
                    resolvedEnv.put(e.getKey(), resolvePlaceholder(name, e.getKey(), e.getValue(), envResolver));
                }
            }
            entries.add(new McpServerEntry(name, transport, command, resolvedEnv));
        }
        return entries;
    }

    /**
     * 占位符解析：${VAR} → envResolver 中的值；缺失报清晰错误（M7 验证行为之一）。
     * 非占位符值原样返回（非敏感值直写的口子）。
     */
    private String resolvePlaceholder(String serverName, String envKey, String rawValue, Map<String, String> envResolver) {
        if (rawValue == null) {
            return null;
        }
        var trimmed = rawValue.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return rawValue;
        }
        var varName = trimmed.substring(2, trimmed.length() - 1);
        var resolved = envResolver.get(varName);
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException(
                    "环境变量缺失: " + varName + "（server=" + serverName + " 的 env." + envKey + " 引用了它）；"
                            + "请先 export 该变量或 source 密钥脚本后重试");
        }
        return resolved;
    }

    /** env 值的日志脱敏：最多前 5 位前缀（TS 8.8；M7 判定项） */
    public static String mask(String value) {
        if (value == null || value.length() <= 5) {
            return "***";
        }
        return value.substring(0, 5) + "***";
    }

    private String requireField(Map<String, String> raw, String field) {
        var value = raw.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("mcp_servers.yaml 条目缺少必填字段: " + field);
        }
        return value;
    }

    private Map<String, String> castToStringMap(Map<?, ?> map) {
        var result = new LinkedHashMap<String, String>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        return result;
    }
}
