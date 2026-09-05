package com.agentos.provider;

/**
 * 统一管理所有 LLM Provider，对 ReAct 循环屏蔽不同 LLM 厂商的差异（TS 3.1）。
 *
 * <p>ReAct 循环调用 LLM 时传入 Profile 与 Prompt，由本服务按 Profile 配置选择对应的
 * 底层 {@code ChatModel} 完成调用；调用时可经 Spring AI 的 {@code ChatClient} 封装使用
 * （TS 3.1）。
 *
 * <p>多 Provider 并存时维护 provider name 到 {@code ChatModel} 的显式映射，禁止按类型
 * 扫描容器中的 {@code ChatModel} Bean——Bean 类型相同、Bean name 未必等于 provider name，
 * 类型扫描无法可靠区分各家 Provider（TS 3.2）。
 *
 * <p>核心阶段不做 fallback 与 hedge racing：Provider 故障时直接报错给 Agent，
 * fallback 链路 / circuit breaker / hedge racing 放扩展阶段经 Profile 的 fallback
 * 字段声明（TS 3.3）。
 *
 * <p>Spring AI / Spring AI Alibaba 依赖按 TS 13 第一周 30 分钟 spike 结论引入，
 * 本骨架不预引入任何相关类型。
 */
public class ProviderService {

    /**
     * 按 provider name 与模型名发起一次 LLM 调用（TS 3.1）。
     *
     * @param providerName Provider 声明的唯一名称，如 deepseek / qwen / kimi（TS 3.2）
     * @param model        具体模型名
     * @param prompt       本次调用的 Prompt
     * @return LLM 生成的文本回复
     */
    public String chat(String providerName, String model, String prompt) {
        throw new UnsupportedOperationException("尚未实现：ProviderService.chat（TS 3.1）");
    }
}
