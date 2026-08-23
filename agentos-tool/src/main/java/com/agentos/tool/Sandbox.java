package com.agentos.tool;

/**
 * Sandbox 校验接口（TS 6.7，接口先行）。
 *
 * <p>纯校验接口（策略层：动作允不允许），签名不携带白名单/容器/VM 等实现特有概念——
 * 用最重的 microVM 实现去反向套这个签名也应能干净套入，这是校验接口是否中立的办法。
 * 核心阶段只在接口后面挂 {@link SandboxChecker} 一档实现；未来加重隔离方案时只新增实现类，
 * 不改接口、不改调用方。
 */
public interface Sandbox {

    /**
     * 校验一个受控动作是否允许执行。
     *
     * @param action 动作（类型 + 目标）
     * @throws SandboxViolationException 校验失败时抛出，Tool 执行终止
     */
    void check(SandboxAction action);
}
