package spike.reactloop.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * E5 链式夹具：三个工具构成依赖链，串行才走得通；并行/猜测会拿到"缺参数"引导。
 * 追溯：002 §3.3。
 */
public class ChainTools {

    private final StringBuilder callLog = new StringBuilder();

    @Tool(description = "第一步：查询目标城市，返回固定内容")
    public String getCity() {
        log("getCity");
        return "北京";
    }

    @Tool(description = "第二步：查询指定城市的当前日期，返回固定内容")
    public String getDate(
            @ToolParam(description = "城市名，须来自 getCity 的结果") String city) {
        log("getDate");
        if (city == null || city.isBlank()) {
            return "缺少城市参数，请先调用 getCity";
        }
        return "2026-09-07";
    }

    @Tool(description = "第三步：查询指定城市和日期的天气，返回固定内容")
    public String getWeather2(
            @ToolParam(description = "城市名，须来自 getCity 的结果") String city,
            @ToolParam(description = "日期，须来自 getDate 的结果") String date) {
        log("getWeather2");
        if (city == null || city.isBlank() || date == null || date.isBlank()) {
            return "缺少参数，请先调用 getCity / getDate";
        }
        return "晴，12°C";
    }

    private void log(String toolName) {
        if (callLog.length() > 0) {
            callLog.append(",");
        }
        callLog.append(toolName);
    }

    /** 全部工具调用名序列（逗号分隔），供断言使用。 */
    public String callLog() {
        return callLog.toString();
    }

    public void reset() {
        callLog.setLength(0);
    }
}
