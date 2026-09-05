package com.agentos.core.profile;

import java.util.List;
import java.util.Map;

/**
 * Agent 运行配置（TS 8.2），由 {@code AgentLoader.deriveProfile} 从 AGENT.md frontmatter 派生。
 *
 * <p>派生字段全集：name、description、identity（agent_name/prompt）、provider（name/model/temperature）、
 * tools、mcp_servers、channels、schedules、bootstrap、settings（max_iterations、max_history_turns、
 * timeout：llm_call / tool / total——超时禁硬编码，application.yaml 默认 + Profile 按 Agent 覆盖，TS 7.4）。
 *
 * <p>notify_channels 不属于 Profile：通知渠道由 SQLite 全局注册表管理，Agent 只在正文中按名引用（TS 6.8/8.2）。
 * 骨架仅保留代表性字段。
 */
public class Profile {

    private String name;
    private String description;
    private String providerName;
    private String model;
    private List<String> tools;
    private Map<String, String> settings;

    // TODO: 实施阶段细化为强类型 settings（含 timeout 三档 llm_call/tool/total）与其余派生字段（TS 8.2）

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = tools;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}
