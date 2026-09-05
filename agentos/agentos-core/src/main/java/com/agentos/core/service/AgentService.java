package com.agentos.core.service;

import com.agentos.core.session.Session;

/**
 * 三种触发源（CLI / Web API / AgentScheduler 定时）共用的统一入口（TS 2 / 4.2），
 * 也是一次处理的编排者。
 *
 * <p>{@link #process} 内部依次：把当前 Profile 放进 ProfileContext（ThreadLocal，虚拟线程下每个请求天然独立）
 * → 调 ReActLoop.run 跑完循环 → 持久化 Session → finally 里清掉 ProfileContext。
 *
 * <p>硬性约束：全程不得切换执行线程（禁 @Async / 跨线程 CompletableFuture，TS 4.2）。
 */
public class AgentService {

    public String process(Session session, String userMessage) {
        // TODO: 实施阶段补编排逻辑（TS 4.2）
        throw new UnsupportedOperationException("尚未实现：AgentService.process（TS 4.2）");
    }
}
