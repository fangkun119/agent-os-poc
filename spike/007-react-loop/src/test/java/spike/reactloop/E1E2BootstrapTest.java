package spike.reactloop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * E1：依赖解析 + Spring 容器启动（离线，不需要 key）。
 * E2：关闭自动执行的开关存在（编译 + 构建即实证）。
 * 追溯：001 §5 E1/E2，002 §4。
 */
class E1E2BootstrapTest {

    @Test
    void contextStarts() {
        try (ConfigurableApplicationContext ctx = new SpringApplication(SpikeApp.class)
                .run("--spring.main.web-application-type=none")) {
            assertThat(ctx.isActive()).isTrue();
        }
    }

    @Test
    void e2_switchReferenceCompilesAndBuilds() {
        // E2：1.1.x 线开关存在性——能编译、能构建出"关闭自动执行"的选项对象
        ChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
        assertThat(options).isNotNull();
    }
}
