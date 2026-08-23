package com.agentos.core.react;

import com.agentos.core.session.Session;

/**
 * ReAct 循环引擎（TS 4.1 / 4.2）：输入 Session 与用户消息，输出最终响应。
 * 核心循环逻辑精简（约数十行 Java），不依赖 Spring AI 的 Agent 抽象。
 *
 * <p>硬性约束（TS 4.2）：循环全程不得切换执行线程——ProfileContext 是 ThreadLocal，
 * 禁 {@code @Async} / 跨线程 CompletableFuture（否则静默丢失）；扩展阶段做并行 Tool 时需改为显式传参。
 * MAX_ITERATIONS 默认 10 次，防 Tool 调用死循环，可在 Profile 覆盖（TS 4.3）。
 */
public class ReActLoop {

    // TODO: 实施阶段补 run(Session, String)（Reason → Act → Observe 循环，TS 4.1）

    public String run(Session session, String userMessage) {
        throw new UnsupportedOperationException("尚未实现：ReActLoop.run（TS 4.1/4.2）");
    }
}
