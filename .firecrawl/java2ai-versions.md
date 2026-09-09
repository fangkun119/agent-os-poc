[跳到主要内容](https://java2ai.com/docs/versions/#__docusaurus_skipToContent_fallback)

本页总览

本文面向 **终端用户** 和 **开发者**，说明 Spring AI Alibaba（SAA）与上游框架的版本对应关系、如何选型与迁移，以及文档与组件的版本信息。

## Spring AI Alibaba 框架版本 [​](https://java2ai.com/docs/versions/\#spring-ai-alibaba-%E6%A1%86%E6%9E%B6%E7%89%88%E6%9C%AC "Spring AI Alibaba 框架版本的直接链接")

### 版本兼容表 [​](https://java2ai.com/docs/versions/\#%E7%89%88%E6%9C%AC%E5%85%BC%E5%AE%B9%E8%A1%A8 "版本兼容表的直接链接")

| SAA 版本 | Spring AI | Spring AI Extensions | Spring Boot | 说明 |
| --- | --- | --- | --- | --- |
| **1.1.2.0**（当前推荐） | 1.1.2 | 1.1.2.1 或 1.1.2.0 | 3.5.x | 支持 Agent Skills，提供 Supervisor、Routing 等 Multi-agent 能力。 |
| [1.1.0.0](http://v1100.java2ai.com/docs/overview) | 1.1.0 | 1.1.0.0 | 3.4.x | 1.1.0 首个正式版 |
| 1.1.0.0-RC2 | 1.1.0-RC2 | 1.1.0.0-RC2 | 3.4.x | 1.1.0 候选版，请使用 1.1.0.0 或 1.1.2.0 版本 |
| 1.1.0.0-RC1 | 1.1.0-RC1 | 1.1.0.0-RC1 | 3.4.x | 1.1.0 候选版，请使用 1.1.0.0 或 1.1.2.0 版本 |
| 1.0.x | 1.0.0 | — | 3.4.x | 1.0 系列 |

> 具体 Spring Boot 小版本以 [spring-ai-alibaba-bom](https://github.com/alibaba/spring-ai-alibaba/blob/main/pom.xml) 及各模块 `pom.xml` 为准。升级前请核对 [Release Notes](https://github.com/alibaba/spring-ai-alibaba/releases)。

### 依赖管理（推荐使用 BOM） [​](https://java2ai.com/docs/versions/\#%E4%BE%9D%E8%B5%96%E7%AE%A1%E7%90%86%E6%8E%A8%E8%8D%90%E4%BD%BF%E7%94%A8-bom "依赖管理（推荐使用 BOM）的直接链接")

新项目建议通过 BOM 统一版本，避免与 Spring AI、Spring Boot 冲突：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-bom</artifactId>
            <version>1.1.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-extensions-bom</artifactId>
            <version>1.1.2.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-agent-framework</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    </dependency>
</dependencies>
```

* * *

## 组件与生态版本 [​](https://java2ai.com/docs/versions/\#%E7%BB%84%E4%BB%B6%E4%B8%8E%E7%94%9F%E6%80%81%E7%89%88%E6%9C%AC "组件与生态版本的直接链接")

以下组件随 Spring AI Alibaba 主仓库发布，版本与 BOM 中的 SAA 版本一致（如 1.1.2.0）。

| 组件 / 模块 | artifactId | 说明 | 版本约定 |
| --- | --- | --- | --- |
| **BOM** | `spring-ai-alibaba-bom` | 依赖管理，统一 SAA 各模块版本 | 同 SAA 主版本 |
| **Agent Framework** | `spring-ai-alibaba-agent-framework` | ReactAgent、多智能体编排、Hooks、Skills 等 | 同 BOM |
| **Graph Core** | `spring-ai-alibaba-graph-core` | 图工作流运行时、持久化、流式、MCP 节点等 | 同 BOM |
| **Studio** | `spring-ai-alibaba-studio` | 嵌入式 Agent 调试与可视化 UI | 同 BOM |
| **Sandbox** | `spring-ai-alibaba-sandbox` | Agent 沙箱运行时 | 同 BOM |
| **Admin** | `spring-ai-alibaba-admin` | 一站式 Agent 平台（可视开发、可观测、MCP 管理） | 随主仓库发布 |
| **Starter A2A Nacos** | `spring-ai-alibaba-starter-a2a-nacos` | 基于 Nacos 的 A2A 通信 | 同 BOM |
| **Starter Config Nacos** | `spring-ai-alibaba-starter-config-nacos` | 基于 Nacos 的动态配置与模型热更新 | 同 BOM |
| **Starter Graph Observation** | `spring-ai-alibaba-starter-graph-observation` | Graph 可观测性（Micrometer/OpenTelemetry） | 同 BOM |
| **Starter Builtin Nodes** | `spring-ai-alibaba-starter-builtin-nodes` | 预置图节点（LlmNode、AgentNode 等） | 同 BOM |

### 示例项目 [​](https://java2ai.com/docs/versions/\#%E7%A4%BA%E4%BE%8B%E9%A1%B9%E7%9B%AE "示例项目的直接链接")

示例项目位于主仓库 `examples/` 下，每个示例独立工程，依赖 BOM 中的 SAA 版本。

| 示例项目 | artifactId | 说明 | 项目地址 |
| --- | --- | --- | --- |
| **Chatbot** | `chatbot` | 对话式 Agent 示例（DashScope、Studio、Python/Shell 等） | [github.com/alibaba/spring-ai-alibaba/tree/main/examples/chatbot](https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/chatbot) |
| **DeepResearch** | `deepresearch` | 深度研究 Agent 示例（MCP、多步推理） | [github.com/alibaba/spring-ai-alibaba/tree/main/examples/deepresearch](https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/deepresearch) |
| **Documentation** | `documentation` | 文档配套示例（Agent 与 Graph 教程、进阶示例） | [github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation](https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation) |

### 第三方与社区生态 [​](https://java2ai.com/docs/versions/\#%E7%AC%AC%E4%B8%89%E6%96%B9%E4%B8%8E%E7%A4%BE%E5%8C%BA%E7%94%9F%E6%80%81 "第三方与社区生态的直接链接")

以下项目位于 [GitHub spring-ai-alibaba 组织](https://github.com/spring-ai-alibaba) 下，与 Spring AI Alibaba 主仓库协同构成生态。 **版本与发布节奏请以各仓库或文档为准。**

| 项目名称 | 说明 | 项目地址 |
| --- | --- | --- |
| **Spring AI Extensions** | Spring AI 扩展实现（DashScopeChatModel、MCP Registry 等），为 LLM 应用提供模型、工具、向量存储等基础抽象 | [github.com/spring-ai-alibaba/spring-ai-extensions](https://github.com/spring-ai-alibaba/spring-ai-extensions) |
| **Examples** | Spring AI 与 Spring AI Alibaba 用法示例集合（独立仓库） | [github.com/spring-ai-alibaba/examples](https://github.com/spring-ai-alibaba/examples) |
| **DataAgent** | 基于 Spring AI Alibaba 的自然语言转 SQL，支持用自然语言查询数据库而无需手写 SQL | [github.com/spring-ai-alibaba/dataagent](https://github.com/spring-ai-alibaba/dataagent) |
| **DeepResearch (Graph 版本)** | 基于 spring-ai-alibaba-graph 的深度研究应用（独立仓库发行版） | [github.com/spring-ai-alibaba/deepresearch](https://github.com/spring-ai-alibaba/deepresearch) |
| **AssistantAgent** | 面向企业级场景的助手型 Agent 构建框架 | [github.com/spring-ai-alibaba/AssistantAgent](https://github.com/spring-ai-alibaba/AssistantAgent) |
| **Lynxe (原Jmanus)** | 高确定性、无代码的 Prompt 编程工作站（基于 Java） | [github.com/spring-ai-alibaba/Lynxe](https://github.com/spring-ai-alibaba/Lynxe) |

* * *

## 文档目录大纲 [​](https://java2ai.com/docs/versions/\#%E6%96%87%E6%A1%A3%E7%9B%AE%E5%BD%95%E5%A4%A7%E7%BA%B2 "文档目录大纲的直接链接")

文档按以下结构组织，便于查找入门、教程与进阶内容。

| 分类 | 路径 / 主题 | 说明 |
| --- | --- | --- |
| **入门** | [概览](https://java2ai.com/docs/overview) | 项目定位、架构、核心能力与安装方式 |
|  | [快速开始](https://java2ai.com/docs/quick-start) | 从零搭建 ReactAgent，含环境、依赖、API Key、示例代码 |
| **Agent 框架教程** | [frameworks/agent-framework/tutorials/](https://java2ai.com/docs/frameworks/agent-framework/tutorials/agents) | Agents、Models、Tools、Hooks、Memory、Messages、Skills、Structured Output 等 |
| **Agent 框架进阶** | [frameworks/agent-framework/advanced/](https://java2ai.com/docs/frameworks/agent-framework/advanced/context-engineering) | RAG、多智能体、人机协同、上下文工程、A2A、Workflow 等 |
| **Graph 核心** | [frameworks/graph-core/quick-start.md](https://java2ai.com/docs/frameworks/graph-core/quick-start) | Graph 快速上手 |
|  | [frameworks/graph-core/core/](https://java2ai.com/docs/frameworks/graph-core/core/core-library) | 核心库、持久化、内存、流式等 |
|  | [frameworks/graph-core/examples/](https://java2ai.com/docs/frameworks/graph-core/examples/llm-streaming-springai) | 子图、并行、取消、MCP、持久化、人机协同等示例 |
| **Studio** | [frameworks/studio/quick-start.md](https://java2ai.com/docs/frameworks/studio/quick-start) | Studio 嵌入式调试与使用 |

* * *

## 相关链接 [​](https://java2ai.com/docs/versions/\#%E7%9B%B8%E5%85%B3%E9%93%BE%E6%8E%A5 "相关链接的直接链接")

- [Spring Ai Alibab GitHub 仓库](https://github.com/alibaba/spring-ai-alibaba)
- [Spring Ai Alibab Extensions GitHub 仓库](https://github.com/spring-ai-alibaba/spring-ai-extensions)
- [Spring Ai Alibaba Release Notes](https://github.com/alibaba/spring-ai-alibaba/releases)
- [Spring Ai Extensions Release Notes](https://github.com/spring-ai-alibaba/spring-ai-extensions/releases)
- [Spring AI 参考文档](https://docs.spring.io/spring-ai/reference/)

- [Spring AI Alibaba 框架版本](https://java2ai.com/docs/versions/#spring-ai-alibaba-%E6%A1%86%E6%9E%B6%E7%89%88%E6%9C%AC)
  - [版本兼容表](https://java2ai.com/docs/versions/#%E7%89%88%E6%9C%AC%E5%85%BC%E5%AE%B9%E8%A1%A8)
  - [依赖管理（推荐使用 BOM）](https://java2ai.com/docs/versions/#%E4%BE%9D%E8%B5%96%E7%AE%A1%E7%90%86%E6%8E%A8%E8%8D%90%E4%BD%BF%E7%94%A8-bom)
- [组件与生态版本](https://java2ai.com/docs/versions/#%E7%BB%84%E4%BB%B6%E4%B8%8E%E7%94%9F%E6%80%81%E7%89%88%E6%9C%AC)
  - [示例项目](https://java2ai.com/docs/versions/#%E7%A4%BA%E4%BE%8B%E9%A1%B9%E7%9B%AE)
  - [第三方与社区生态](https://java2ai.com/docs/versions/#%E7%AC%AC%E4%B8%89%E6%96%B9%E4%B8%8E%E7%A4%BE%E5%8C%BA%E7%94%9F%E6%80%81)
- [文档目录大纲](https://java2ai.com/docs/versions/#%E6%96%87%E6%A1%A3%E7%9B%AE%E5%BD%95%E5%A4%A7%E7%BA%B2)
- [相关链接](https://java2ai.com/docs/versions/#%E7%9B%B8%E5%85%B3%E9%93%BE%E6%8E%A5)

[编辑此页](https://github.com/spring-ai-alibaba/website/tree/main/docs/versions.md) [问题反馈](https://github.com/alibaba/spring-ai-alibaba/issues/new)

Spring AI Alibaba 开源项目基于 Spring AI 构建，是阿里云通义系列模型及服务在 Java AI 应用开发领域的最佳实践，提供高层次的 AI API 抽象与云原生基础设施集成方案，帮助开发者快速构建 AI 应用。

[浙ICP备2021005855号-33](https://beian.miit.gov.cn/)

AI