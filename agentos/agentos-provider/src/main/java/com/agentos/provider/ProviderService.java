package com.agentos.provider;

/**
 * 统一管理所有 LLM Provider，对 ReAct 循环屏蔽不同 LLM 厂商的差异（TechnicalSolution.md - 3.1 模块组成）。
 *
 * <p>ReAct 循环调用 LLM 时传入 Profile 与 Prompt，由本服务按 Profile 配置选择对应的
 * 底层 {@code ChatModel} 完成调用；调用时可经 Spring AI 的 {@code ChatClient} 封装使用
 * （TechnicalSolution.md - 3.1 模块组成）。
 *
 * <p>多 Provider 并存时维护 provider name 到 {@code ChatModel} 的显式映射，禁止按类型
 * 扫描容器中的 {@code ChatModel} Bean——Bean 类型相同、Bean name 未必等于 provider name，
 * 类型扫描无法可靠区分各家 Provider（TechnicalSolution.md - 3.2 Provider 名到 ChatModel 的显式映射）。
 *
 * <p>核心阶段不做 fallback 与 hedge racing：Provider 故障时直接报错给 Agent，
 * fallback 链路 / circuit breaker / hedge racing 放扩展阶段经 Profile 的 fallback
 * 字段声明（TechnicalSolution.md - 3.3 关键设计点）。
 *
 * <p>依赖按 spike/007-react-loop 两组实测结论引入（Spring AI 官方 starter 优先，无官方
 * starter 的厂商经 OpenAI 兼容腿显式构造兜底；Spring AI Alibaba 只以 BOM 管版本，
 * TechnicalSolution.md - 1.2 整体技术栈 / 13 实施节奏 的第一周），本骨架不预引入任何相关类型。
 */
public class ProviderService {

    /**
     * 按 provider name 与模型名发起一次 LLM 调用（TechnicalSolution.md - 3.1 模块组成）。
     *
     * @param providerName Provider 声明的唯一名称，如 openai / anthropic / minimax / zhipu（deepseek / kimi 为预留，TechnicalSolution.md - 3.2 Provider 名到 ChatModel 的显式映射）
     * @param model        具体模型名
     * @param prompt       本次调用的 Prompt
     * @return LLM 生成的文本回复
     */
    public String chat(String providerName, String model, String prompt) {
        throw new UnsupportedOperationException("尚未实现：ProviderService.chat（TechnicalSolution.md - 3.1 模块组成）");
    }
}
