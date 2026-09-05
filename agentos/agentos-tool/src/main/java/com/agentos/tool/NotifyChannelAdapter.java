package com.agentos.tool;

/**
 * 出站通知抽象接口（TS 6.8，接口先行）。
 *
 * <p>表达"把一条内容送到某个通知目标"这个意图，不携带具体渠道细节。与入站 ChannelAdapter（TS 8.4）
 * 语义方向相反、分开建模：入站解决"什么触发 Agent 开始跑"，出站解决"Agent 跑完把结果送到哪"，
 * 不合并成一个抽象。
 */
public interface NotifyChannelAdapter {

    /**
     * 把内容发送到通知目标。
     *
     * @param target 通知目标（渠道类型 + 配置）
     * @param content 通知内容
     */
    void send(NotifyTarget target, String content);
}
