package com.agentos.cli;

/**
 * 统一加载 LLM API key、Provider/MCP server 凭证等敏感配置（TechnicalSolution.md - 8.8 配置与密钥加载）。
 *
 * <p>核心阶段基础版口径：
 * <ul>
 *   <li>敏感配置只通过环境变量注入，禁止从任何配置文件读取（TechnicalSolution.md - 8.8 配置与密钥加载 / DemandAnalysis.md - 5.12 配置与密钥加载 的密钥红线），
 *       不明文写死在 AGENT.md frontmatter 里（Profile 里用 {@code ${ENV_VAR}} 占位，加载时从环境变量解析）；</li>
 *   <li>配置加载时做必填项与格式的基础校验，缺失或非法时给清晰报错；</li>
 *   <li>完整的加密存储、密钥轮转、对接企业 KMS/Vault 放扩展阶段。</li>
 * </ul>
 */
public class ConfigLoader {

    /**
     * 解析配置值中的 {@code ${ENV_VAR}} 环境变量占位（TechnicalSolution.md - 8.8 配置与密钥加载）。
     *
     * @param raw 原始配置值，可含 {@code ${ENV_VAR}} 占位
     * @return 占位替换后的实际配置值
     */
    public String resolveEnvPlaceholder(String raw) {
        throw new UnsupportedOperationException("尚未实现：ConfigLoader.resolveEnvPlaceholder（TechnicalSolution.md - 8.8 配置与密钥加载）");
    }
}
