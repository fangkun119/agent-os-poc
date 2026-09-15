package spike.reactloop;

import java.util.regex.Pattern;

/**
 * 剥离 MiniMax 思考标签（&lt;think&gt;...&lt;/think&gt;），供最终回答断言前处理（V7）。
 * 参照第一组 ThinkStripper 的正则思路重写；Pattern 预编译为类级常量（编码规范 §4）。
 * 追溯：002-spec §3.4。
 */
public final class ThinkStripper {

    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");

    private ThinkStripper() {
    }

    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return THINK_BLOCK.matcher(text).replaceAll("").trim();
    }
}
