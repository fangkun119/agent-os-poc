package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * V1：spring-ai-starter-model-minimax 进入 classpath 后，工程容器可启动
 * （全量 application.yaml、placeholder key——离线项，不需要真实 key）。
 * 追溯：002-spec §4 V1；完成判定归 002-plan T0。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class V1BootstrapTest {

    private static final Logger log = LoggerFactory.getLogger(V1BootstrapTest.class);

    @Autowired
    private ApplicationContext context;

    @Test
    void contextStarts() {
        assertThat(context).isNotNull();
        // 顺带观察（spec §6 开放项 4）：placeholder key 下 minimax 自动配置是否正常创建了 ChatModel Bean
        log.info("[V1] MiniMaxChatModel bean present = {}",
                !context.getBeansOfType(org.springframework.ai.chat.model.ChatModel.class).isEmpty());
    }
}
