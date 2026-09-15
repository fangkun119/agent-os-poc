package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * V6：usage/耗时采集——minimax 腿（缺省模型 M2.7，三级选择第 1 级真调用）
 * + anthropic 腿（M3，配置即国内站 api.minimax.cn/anthropic，可达性随本测试实证）。
 * 判定：两腿各得 token 数 &gt; 0、毫秒 &gt; 0，一次模型调用 ↔ 一组样本（llm_calls 审计对齐的取数结构；
 * retry 已关 1 次，duration 无隐式重试污染）。anthropic 腿样本补齐第一组 E7 缺的第二协议。
 * 追溯：002-spec §4 V6；002-plan T6。
 */
@Tag("live")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V6UsageCaptureTest {

    private static final Logger log = LoggerFactory.getLogger(V6UsageCaptureTest.class);

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("providerRegistryMap")
    private Map<String, ChatModel> registry;

    @Autowired
    private ToolCallingManager toolCallingManager;

    @Test
    void usageCaptureOnBothLegs() {
        for (String key : new String[] {"minimax", "anthropic"}) {
            CountingTools tools = new CountingTools();
            ManualLoop loop = new ManualLoop(registry.get(key), toolCallingManager, ToolCallbacks.from(tools));
            LoopResult r = loop.run("深圳 2026-09-15 的天气？必须调用工具查询。", null);

            log.info("[V6] key={} → iterations={}, usages={}, durationsMs={}",
                    key, r.iterations(), r.usages(), r.durationsMs());
            log.info("[V6] key={} → finalText(剥离后)={}", key, ThinkStripper.strip(r.finalText()));

            assertThat(r.usages()).as("key=%s 每轮应有 usage 样本", key).isNotEmpty();
            assertThat(r.durationsMs()).as("key=%s 每轮应有耗时样本", key).isNotEmpty();
            for (String u : r.usages()) {
                assertThat(u).as("key=%s usage 应含正数 token", key).containsPattern("in=[1-9]|out=[1-9]");
            }
            for (Long d : r.durationsMs()) {
                assertThat(d).as("key=%s 单轮毫秒耗时应 > 0", key).isPositive();
            }
            assertThat(ThinkStripper.strip(r.finalText())).as("key=%s 应含工具结果要素", key)
                    .containsAnyOf("12°C", "晴");
        }
    }
}
