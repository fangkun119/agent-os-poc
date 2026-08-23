package com.agentos.cli;

/**
 * 统一加载 LLM API key、Provider/MCP server 凭证等敏感配置（TS 8.8）。
 *
 * <p>核心阶段基础版口径：
 * <ul>
 *   <li>敏感配置通过环境变量注入或独立本地配置文件加载，不明文写死在
 *       AGENT.md frontmatter 里（Profile 里用 {@code ${ENV_VAR}} 占位，加载时从环境变量解析）；</li>
 *   <li>配置加载时做必填项与格式的基础校验，缺失或非法时给清晰报错；</li>
 *   <li>完整的加密存储、密钥轮转、对接企业 KMS/Vault 放扩展阶段。</li>
 * </ul>
 */
public class ConfigLoader {

    /**
     * 解析配置值中的 {@code ${ENV_VAR}} 环境变量占位（TS 8.8）。
     *
     * @param raw 原始配置值，可含 {@code ${ENV_VAR}} 占位
     * @return 占位替换后的实际配置值
     */
    public String resolveEnvPlaceholder(String raw) {
        throw new UnsupportedOperationException("尚未实现：ConfigLoader.resolveEnvPlaceholder（TS 8.8）");
    }
}
