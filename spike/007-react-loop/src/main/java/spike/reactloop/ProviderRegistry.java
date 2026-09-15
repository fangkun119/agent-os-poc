package spike.reactloop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import io.micrometer.observation.ObservationRegistry;

/**
 * Provider 显式映射：provider 名 → ChatModel，四键并存、按名取用、禁止类型扫描。
 * 键：openai / anthropic（自动配置 Bean）+ minimax（路径 B 实测体）+ zhipu（腿 4，方法体内局部构造——
 * 不注册独立 Bean，防 @ConditionalOnMissingBean 令自动配置退位，见 002-spec §3.3 避雷条款）。
 * zhipu 构造要点：baseUrl 读 ZHIPU_BASE_URL 去尾斜杠、completionsPath 覆盖为 /chat/completions
 * （智谱路径布局非标准 /v1）、key 读 ZHIPU_API_KEY、模型读 ZHIPU_DEFAULT_MODEL。
 * 追溯：002-spec §3.3；007 README 第五节样例。
 */
@Configuration
public class ProviderRegistry {

    @Bean
    public Map<String, ChatModel> providerRegistryMap(
            @Qualifier("openAiChatModel") ChatModel openAi,
            @Qualifier("anthropicChatModel") ChatModel anthropic,
            @Qualifier("miniMaxChatModel") ChatModel minimax) {

        OpenAiApi zhipuApi = OpenAiApi.builder()
                .baseUrl(stripTrailingSlash(System.getenv("ZHIPU_BASE_URL")))
                .apiKey(new SimpleApiKey(System.getenv("ZHIPU_API_KEY")))
                .completionsPath("/chat/completions")
                .build();
        OpenAiChatModel zhipu = new OpenAiChatModel(zhipuApi,
                OpenAiChatOptions.builder().model(System.getenv("ZHIPU_DEFAULT_MODEL")).build(),
                ToolCallingManager.builder().build(),
                RetryTemplate.builder().maxAttempts(1).build(), // 显式受控重试（规范「配置纪律」章：禁默认 10 次退避链）
                ObservationRegistry.NOOP);

        Map<String, ChatModel> registry = new LinkedHashMap<>();
        registry.put("openai", openAi);
        registry.put("anthropic", anthropic);
        registry.put("minimax", minimax);
        registry.put("zhipu", zhipu);
        return Map.copyOf(registry);
    }

    private static String stripTrailingSlash(String url) {
        return url == null ? null : url.replaceAll("/+$", "");
    }
}
