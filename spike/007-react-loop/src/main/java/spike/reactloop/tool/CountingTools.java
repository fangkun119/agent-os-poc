package spike.reactloop.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * E3 对照 / E4 / E6 的确定性夹具工具：固定返回 + 执行计数 + 最近入参记录。
 * 追溯：002 §3.2。
 */
public class CountingTools {

    private final AtomicInteger weatherCalls = new AtomicInteger();
    private final AtomicInteger adviceCalls = new AtomicInteger();
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

    @Tool(description = "根据天气给穿衣建议，返回固定内容")
    public String getAdvice(
            @ToolParam(description = "天气描述，例如：晴") String weather) {
        adviceCalls.incrementAndGet();
        lastArgs.put("weather", weather);
        return "穿外套";
    }

    public int totalCount() {
        return weatherCalls.get() + adviceCalls.get();
    }

    public Map<String, String> lastArgs() {
        return Map.copyOf(lastArgs);
    }

    public void reset() {
        weatherCalls.set(0);
        adviceCalls.set(0);
        lastArgs.clear();
    }
}
