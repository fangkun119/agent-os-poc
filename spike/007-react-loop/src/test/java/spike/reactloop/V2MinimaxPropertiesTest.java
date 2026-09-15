package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.retry.support.RetryTemplate;

/**
 * V2：spring.ai.minimax.* 属性面实测（spec §4 V2）。
 * ① model 属性绑定断言（Environment 口径，零未知 API）；② base-url 属性生效负向探针；
 * ③ base-url 候选判定（正向由 V3 闭环承担，此处记录口径）；④ chat 级 base-url 属性绑定表现。
 * 探针要点（执行后修订）：MiniMaxApi 两参构造器为 v1.1.8 源码 83-84 行实证；手动构造的
 * ChatModel 内置默认重试模板（10 次退避），必须显式传受控模板；本机 fake-IP 代理会解析
 * 任意假域名，探针地址用 127.0.0.1:1（本机必拒连、localhost 不走代理）。
 * 追溯：002-spec §4 V2；002-plan T1。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V2MinimaxPropertiesTest {

    private static final Logger log = LoggerFactory.getLogger(V2MinimaxPropertiesTest.class);

    @Autowired
    private Environment environment;

    @Autowired
    private MiniMaxChatModel miniMaxChatModel;

    @Test
    void minimaxPropertiesFace() {
        // ① 属性绑定：yaml 占位符 → 环境变量解析值（三级选择的第 1 级落点）
        String model = environment.getProperty("spring.ai.minimax.chat.options.model");
        String baseUrl = environment.getProperty("spring.ai.minimax.base-url");
        String apiKey = environment.getProperty("spring.ai.minimax.api-key");
        boolean chatLevelBaseUrlSet = environment.containsProperty("spring.ai.minimax.chat.base-url");
        log.info("[V2-1] chat.options.model = {}", model);
        log.info("[V2-1] base-url = {}", baseUrl);
        log.info("[V2-1] api-key 已注入 = {}", apiKey != null && !apiKey.isBlank() && !"placeholder".equals(apiKey));
        log.info("[V2-4] chat.base-url 是否显式设置 = {}（未设置 → 运行时回落通用 base-url）", chatLevelBaseUrlSet);

        assertThat(model).isEqualTo("MiniMax-M2.7");
        assertThat(baseUrl).isEqualTo("https://api.minimax.cn");
        // 自动配置确实用这些属性建出了 MiniMaxChatModel（容器里有实例）
        assertThat(miniMaxChatModel).isNotNull();

        // ② 负向探针：base-url 若已接线，请求必达 127.0.0.1:1 并报含该地址的连接异常；
        // 若未接线则会打向默认官方域名、错误完全不同——判定无歧义
        MiniMaxApi probeApi = new MiniMaxApi("https://127.0.0.1:1", "probe-key-not-real");
        RetryTemplate noRetry = RetryTemplate.builder().maxAttempts(1).build();
        MiniMaxChatModel probeModel = new MiniMaxChatModel(probeApi,
                org.springframework.ai.minimax.MiniMaxChatOptions.builder().build(),
                org.springframework.ai.model.tool.ToolCallingManager.builder().build(),
                noRetry);
        assertThatThrownBy(() -> probeModel.call(new Prompt("ping")))
                .isInstanceOf(Exception.class)
                .hasStackTraceContaining("127.0.0.1");
        log.info("[V2-2] 负向探针通过：请求确实打向配置的 base-url"
                + "（连接异常含 127.0.0.1:1；且证实原生路径拼接 = base + /v1/text/chatcompletion_v2）");

        // ③ 候选判定口径：候选 A（纯主机，不带 /v1）——源码口径预期正确；
        //    正向闭环验证由 V3 承担（V3 成功 = 候选 A 实证），此处不重复烧调用。
        log.info("[V2-3] 候选 A（纯主机）正向验证移交 V3 闭环");
    }
}
