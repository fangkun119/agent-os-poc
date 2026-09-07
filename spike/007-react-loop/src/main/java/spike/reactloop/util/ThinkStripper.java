package spike.reactloop.util;

/**
 * 剥离 MiniMax 思考标签（&lt;think&gt;...&lt;/think&gt;），供最终回答断言前处理。
 * 追溯：定稿 5.4，002 §3.6。
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
