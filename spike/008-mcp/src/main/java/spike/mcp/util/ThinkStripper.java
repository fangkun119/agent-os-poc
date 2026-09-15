package spike.mcp.util;

/**
 * 思考内容剥离（002 §3.8；同款思路重写）。
 * MiniMax-M3 等思考型模型会把思考过程混在返回正文的 &lt;think&gt;...&lt;/think&gt; 标签里
 * （007 README 附带实测结论），断言前先剥离。
 */
public final class ThinkStripper {

    private ThinkStripper() {
    }

    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?s)<think>.*?</think>", "").trim();
    }
}
