package com.agentos.core.context;

import com.agentos.core.profile.Profile;

/**
 * Agent 加载器（TS 8.2）：扫 .agentos/agents/ 各子目录，把每个 AGENT.md 的 frontmatter
 * （SnakeYAML 解析）派生成一个 {@link Profile} 并注册到 ProfileRegistry。
 * 一个目录 = 一个 Agent（TS 11.1）；Agent 目录不是 Tool。
 */
public class AgentLoader {

    // TODO: 实施阶段补扫描与启动合法性校验（Provider 存在 / Tool 已注册 / Channel 支持 / Bootstrap 存在，TS 8.2）

    public Profile deriveProfile(java.nio.file.Path agentDir) {
        // TODO: AGENT.md frontmatter → Profile 派生（TS 8.2 / 11.1）
        throw new UnsupportedOperationException("尚未实现：AgentLoader.deriveProfile（TS 8.2）");
    }
}
