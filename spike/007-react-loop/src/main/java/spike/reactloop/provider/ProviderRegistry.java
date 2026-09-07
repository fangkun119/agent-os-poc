package spike.reactloop.provider;

import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider 显式映射：provider 名 → ChatModel 实例，按名取用，禁止类型扫描（红线）。
 * 追溯：002 §3.5，定稿 §2（OPENAI / ANTHROPIC Provider）。
 */
@Configuration
public class ProviderRegistry {

    @Bean
    public Map<String, ChatModel> providerRegistryMap(
            @Qualifier("openAiChatModel") ChatModel openAi,
            @Qualifier("anthropicChatModel") ChatModel anthropic) {
        return Map.of("openai", openAi, "anthropic", anthropic);
    }
}
