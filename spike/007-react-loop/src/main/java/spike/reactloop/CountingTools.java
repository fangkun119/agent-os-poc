package spike.reactloop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 确定性夹具工具：固定返回 + 执行计数 + 最近入参记录（参照第一组思路重写）。
 * 计数器是 V4 按 E3 计数法判"无双执行"的依据。追溯：002-spec §3.1。
 */
public class CountingTools {

    private final AtomicInteger weatherCalls = new AtomicInteger();
    private final Map<String, String> lastArgs = new LinkedHashMap<>();

    @Tool(description = "查询指定城市和日期的天气，返回固定内容")
    public String getWeather(
            @ToolParam(description = "城市名，例如：北京") String city,
            @ToolParam(description = "日期，格式 yyyy-MM-dd") String date) {
        weatherCalls.incrementAndGet();
        lastArgs.put("city", city);
        lastArgs.put("date", date);
        return "晴，12°C";
    }

    /** 全部工具的执行总次数（V4 判定用：应等于模型发起工具调用的总个数）。 */
    public int totalCount() {
        return weatherCalls.get();
    }

    public Map<String, String> lastArgs() {
        return Map.copyOf(lastArgs);
    }

    public void reset() {
        weatherCalls.set(0);
        lastArgs.clear();
    }
}
