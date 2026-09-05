package com.agentos.provider;

/**
 * Provider 声明配置（TS 3.1 Provider 配置模块）。
 *
 * <p>每个 Provider 通过 {@code application.yaml} 声明唯一的 provider name、base URL
 * 与 API key 占位；{@code ProviderService} 启动时按该 name 建立 provider name 到
 * {@code ChatModel} 的显式映射表，Profile 通过 provider name 引用（TS 3.2）。
 *
 * <p>{@code apiKeyEnv} 是环境变量占位名（形如 {@code ${DEEPSEEK_API_KEY}}）：
 * 敏感凭证经环境变量注入，绝不以明文落盘或写入配置（TS 8.8 红线）。
 *
 * @param name      Provider 唯一名称（deepseek / qwen / kimi 等，TS 3.2）
 * @param baseUrl   该 Provider 的 API base URL
 * @param apiKeyEnv 存放 API key 的环境变量占位名，只存占位名不存明文凭证
 */
public record ProviderConfig(String name, String baseUrl, String apiKeyEnv) {
}
