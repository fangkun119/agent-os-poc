package com.agentos.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentOS 启动模块（TS 10）：主类、自动配置、依赖聚合。
 *
 * <p>运行模式（TS 8.6）：agentos chat（交互对话）/ agentos serve（Web Service，默认端口 8080）/
 * agentos gateway（核心阶段与 serve 同义，预挂 CLI Channel）——由 agentos-cli 的 Picocli 入口分发。
 */
@SpringBootApplication
public class AgentOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentOsApplication.class, args);
    }
}
