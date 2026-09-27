package spike.mcp.tool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具统一注册表（TechnicalSolution.md - 6.6 ToolRegistry 的 spike 形态；002 §3.4）。
 * 正式实现按 Profile 的 tools 字段过滤可用子集，这里用 subset() 模拟同一语义。
 */
public class ToolRegistry {

    private final Map<String, AgentOSTool> tools = new ConcurrentHashMap<>();

    /** 注册一个工具；name 重复视为配置错误，抛出以暴露问题 */
    public void register(AgentOSTool tool) {
        var previous = tools.putIfAbsent(tool.getName(), tool);
        if (previous != null) {
            throw new IllegalStateException("工具名重复注册: " + tool.getName());
        }
    }

    /** 按名查找；不存在返回 null */
    public AgentOSTool lookup(String name) {
        return tools.get(name);
    }

    /** 当前注册的全部工具 */
    public List<AgentOSTool> list() {
        return List.copyOf(tools.values());
    }

    /** 白名单子集（模拟 Profile 的 tools 字段过滤，TechnicalSolution.md - 6.6 ToolRegistry）；names 中不存在的名字静默忽略 */
    public List<AgentOSTool> subset(List<String> names) {
        return names.stream()
                .filter(tools::containsKey)
                .map(tools::get)
                .toList();
    }
}
