# 一个提交立起九模块工程：AgentOS 的 Maven 骨架是怎么搭的

## 1. 变更全貌：一次提交，97 个文件

AgentOS 是一个用 Java 写的「Agent 操作系统内核」——让 AI 助手能调用工具、记住事情、定时干活的运行时底座。在这个提交之前，仓库里只有设计文档；这个提交之后，仓库第一次有了能通过 `mvn clean package` 的可编译工程。问题很朴素：一份写了几周的技术方案，怎么变成第一行能跑的代码？答案是一次性立起整个九模块的工程骨架，全部类只有签名和设计注释、没有实现体。

全部变更来自同一个提交：

| 提交 | 主题 | 文件数 | 行数 |
|---|---|---|---|
| [f93bd27](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614) | 按技术方案第 10 章初始化 Maven 九模块工程骨架，clean package 全绿 | 97 | +3058 / -0 |

合计：97 文件，+3058 / -0，全部为新增。按模块分组：

| 模块 | 内容 | 规模 |
|---|---|---|
| 根目录 | parent POM、Maven Wrapper、.gitignore | 4 文件 |
| agentos-core | 领域骨架：Tool/Session/Profile/ReAct 循环/调度契约等 | 13 类 |
| agentos-provider | LLM 接入占位：ProviderConfig、ProviderService | 2 类 |
| agentos-memory | 长期记忆：接口、Markdown 默认档、门面、2 个记忆 Tool | 6 类 |
| agentos-tool | 工具子系统：沙箱、注册表、7 个内置 Tool、MCP 适配 | 18 类 |
| agentos-channel-cli | CLI 触发通道：CliChannel | 1 类 |
| agentos-web | REST 层：响应信封、全局异常、8 个 Controller | 11 类 |
| agentos-storage | 持久层：7 实体 + 7 仓储 + JPA 调度存储 | 15 类 |
| agentos-cli | 命令行：主入口 + 12 子命令 + 配置加载 | 15 类 |
| agentos-boot | Spring Boot 启动模块：主类 + application.yaml | 2 文件 |

其中 mvnw、mvnw.cmd、.mvn/wrapper 三个文件是构建工具自动生成的自举脚本（约 487 行），属生成物噪音，本文不展开。

## 2. 为什么先立骨架，而不是先写功能

你可能会问：写程序不都是先做一个小功能跑起来吗？一口气提交 97 个空壳类，不是本末倒置？

这叫 **骨架优先（walking skeleton，行走的骨架）**：先搭一个能从头到尾走通的极薄结构——编译通过、模块就位、依赖方向定死——再往每根骨头上一块一块填肉。它换来的好处在多人协作和多周排期里才显形：

| 好处 | 在本提交里的体现 |
|---|---|
| 结构先行，防止实现 drift | 每个类的 Javadoc 都标注了它对应技术方案的哪一节，实现者没得发挥 |
| 编译期就锁死依赖方向 | 九个 pom.xml 的依赖关系就是架构图，谁越界谁编译报错 |
| 后续工作可并行 | 九个模块互不挡路，core 和 web 可以两个人同时填 |

反过来，如果先写功能再拆模块，最常见的结果是：所有代码堆在一个包里，功能写完那天，「有空再拆」永远排不上日程。

所以这个提交里几乎每个方法体都是这一句：

```java
throw new UnsupportedOperationException("尚未实现：ReadFileTool.execute（TS 6.2）");
```

出自 [f93bd27:ReadFileTool.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-tool/src/main/java/com/agentos/tool/ReadFileTool.java)。空实现不等于没内容——签名、参数、抛出的异常文案都是设计决策，下文逐个拆。

**Maven** 在这里顺带解释：Java 世界最主流的构建工具，负责下载依赖库、编译、打包。「**Maven 多模块**」指一个父工程下辖多个子工程（模块），各自是独立的 jar，又能被一根线一起构建。

## 3. parent POM：九个模块怎么被一根线串起来

### 3.1 改动走读

根 [f93bd27:pom.xml](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/pom.xml) 只有 39 行，做了四件事：

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.16</version>
</parent>
...
<packaging>pom</packaging>
...
<modules>
  <module>agentos-core</module>
  <module>agentos-provider</module>
  <module>agentos-memory</module>
  <module>agentos-tool</module>
  <module>agentos-channel-cli</module>
  <module>agentos-web</module>
  <module>agentos-storage</module>
  <module>agentos-cli</module>
  <module>agentos-boot</module>
</modules>
<properties>
  <java.version>21</java.version>
  ...
  <picocli.version>4.7.7</picocli.version>
</properties>
```

第一，`<parent>` 指向 **Spring Boot starter parent**——Spring Boot 官方的「家長工程」，它自带一张 **BOM（Bill of Materials，物料清单）**：几百个常用依赖的版本号都替你选好、测过兼容。子工程引依赖时不写版本号，全部听 BOM 的。这就是「依赖版本集中管理」。

第二，`<packaging>pom</packaging>` 声明本工程自己不出 jar，只当聚合壳。

第三，`<modules>` 列出九个模块——注意这是有序的排布，但 Maven 自己会按依赖关系决定构建顺序，人工列表不需要操心拓扑。

第四，`picocli.version` 被显式钉在 4.7.7。**picocli** 是 Java 的命令行解析框架（`agentos chat --profile xxx` 这种命令靠它解析）。为什么要钉？提交信息给了答案：Boot BOM 不管理 picocli，不钉版本的话版本会漂。这是 BOM 时代的常规操作：BOM 管的不用写，BOM 不管的必须自己钉。

### 3.2 影响什么

- 一条 `./mvnw clean package` 构建全部九个模块，提交信息确认「clean package 全绿」；
- JDK 21、编码 UTF-8 在根上定一次，九个模块继承，无人可私改；
- 后来者加第十个模块，只需加一行 `<module>`——结构的扩展成本是常数。

## 4. 依赖方向就是架构：谁依赖谁，为什么

### 4.1 依赖图

多模块工程真正的架构不在类图里，在各模块 pom.xml 的 `<dependencies>` 里。本提交定下的依赖图（箭头 = 依赖）：

| 模块 | 依赖谁 | 要点 |
|---|---|---|
| agentos-core | 无（只依赖 JDK） | 最底层，被所有人依赖 |
| agentos-provider / memory / tool / web / storage | core | 各自独立，互不依赖 |
| agentos-channel-cli | 仅 core | 同进程直调，绕开 Web |
| agentos-cli | core + channel-cli + picocli | 入口层 |
| agentos-boot | 上面全部八个 | 聚合成一个可执行整体 |

### 4.2 两处最有讲究的走读

第一处，[f93bd27:agentos-channel-cli/pom.xml](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-channel-cli/pom.xml) 只声明了一个内部依赖：agentos-core。它的 pom 描述写明：CLI 通道「核心阶段与 AgentScheduler 同进程直调 AgentService（不经 Web API）」。

为什么不绕 Web API？这个系统的三种触发源——CLI 命令、REST 接口、定时调度——最终都汇入同一个 `AgentService.process` 入口。CLI 用户敲一条命令还要先给自己的 Web 服务发一个 HTTP 请求，纯属自找网络开销。让 CLI 直调 core 的服务类，绕开网络层，是单二进制部署里典型的省钱路径。而这条约束不是靠口头约定，是靠「channel-cli 的 pom 里根本没有 web 这个依赖」——想违规，编译不过。

第二处，[f93bd27:agentos-storage/pom.xml](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-storage/pom.xml) 依赖的是 agentos-core，而不是反过来。这引出本提交里最值得学的一个模式——

### 4.3 契约在 core，实现在 storage：依赖倒置

你可能会问：core 是业务心脏，storage 是存数据库的，难道不该 core 调 storage 吗？

直觉上是的，但那样 core 就 import 了 JPA 的类，从此「心脏离不开数据库」。这个提交的做法是反过来，看 [f93bd27:ScheduledTaskStore.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-core/src/main/java/com/agentos/core/schedule/ScheduledTaskStore.java)（在 core 模块里）：

```java
/** 定时任务状态契约（TS 8.5）：契约在 core、实现在 storage（依赖倒置）。 */
public interface ScheduledTaskStore {
    void register(String taskId, String profileName, String cron, String zone, String message);
    void recordExecution(String taskId, String sessionId, boolean success,
                         String errorMessage, long durationMs);
    boolean isEnabled(String taskId);
    void setEnabled(String taskId, boolean enabled);
    List<String> list();
    List<String> executions(String taskId);
}
```

而它的实现 [f93bd27:JpaScheduledTaskStore.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-storage/src/main/java/com/agentos/storage/JpaScheduledTaskStore.java) 在 storage 模块，类声明是 `implements ScheduledTaskStore`，六个方法全部抛「尚未实现」。

这就是 **依赖倒置（Dependency Inversion）**：业务方定义「我需要什么」（接口放 core），供给方实现「怎么给」（实现放 storage）。箭头从 storage 指向 core，core 对 JPA 一无所知。收益很实际：将来换 PostgreSQL、换成内存版测试替身，core 一行不改。注意接口 Javadoc 里还有一条产品决策：「定义来源仍是 AGENT.md frontmatter 的 schedules——本接口只存状态 + 历史，不作为定义源」——即定时任务的定义在 Agent 的 Markdown 文件里，重启时从文件重新注册，数据库只留痕。持久层不篡权当定义源，这在一个「文件优先」的系统里是必须写死的边界。

### 4.4 影响什么

- 任何人打开任一模块的 pom，三秒钟知道它能碰什么、不能碰什么；
- core 保持零框架依赖（连 Spring 都不依赖），理论上可以脱离容器单测；
- agentos-boot 是唯一依赖全部模块的地方——[f93bd27:agentos-boot/pom.xml](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-boot/pom.xml) 聚合八个模块加 spring-boot-starter，并用 spring-boot-maven-plugin 打成 **fat JAR**（把所有依赖打进一个 jar，`java -jar` 一条命令启动的单二进制交付形态）。

## 5. core：把领域概念先长出来

core 的 13 个类就是这个系统的名词表。骨架阶段它们大多只有字段加 getter/setter，但 Javadoc 把语义写满了——这正是「概念先行」的做法：先把领域语言定准，实现只是翻译。

### 5.1 Profile：一个 Agent 的配置档案

[f93bd27:Profile.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-core/src/main/java/com/agentos/core/profile/Profile.java) 代表一个 Agent 的运行配置。这个项目里「一个目录 = 一个 Agent」：每个 Agent 是磁盘上一个带 AGENT.md 文件的目录，文件头部的 **frontmatter**（Markdown 文件顶部一段 `key: value` 元数据块，源自静态博客的约定）写它的供应商、模型、工具清单，`AgentLoader.deriveProfile` 把这段元数据解析成 Profile 对象。

骨架只留了七个代表性字段（name、description、providerName、model、tools、settings）。Javadoc 里特意记录了一条排除决策：notify_channels 不属于 Profile——通知渠道由 SQLite 全局注册表管理，Agent 只在正文中按名引用。配置归文件、注册表归库，两套数据不打架。

### 5.2 Session：会话的身份从哪来

[f93bd27:Session.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-core/src/main/java/com/agentos/core/session/Session.java) 定义「一次会话」。最有信息量的是这句：session_id 由 channel + user + profile 三元联合生成。同一个用户、同一个 Agent、走同一个通道，就是同一会话，历史自然接上。定时调度场景（channel 固定为 scheduler）历次触发复用同一 session_id，对话历史每次落盘时按 max_history_turns 物理裁剪，但完整审计链路保留在 tool_invocations / llm_calls 两张表里。会话可以裁，账本不能裁。

### 5.3 AgentOSTool：所有工具的统一插座

[f93bd27:AgentOSTool.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-core/src/main/java/com/agentos/core/tool/AgentOSTool.java) 定义了工具协议：getName、getDescription、getInputSchema、execute。看 tool 模块的 [f93bd27:ReadFileTool.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-tool/src/main/java/com/agentos/tool/ReadFileTool.java) 怎么实现它：

```java
@Override
public String getInputSchema() {
    return """
            {"type": "object", "properties": {"path": {"type": "string",
              "description": "文件路径"}}, "required": ["path"]}
            """;
}
```

这段 JSON 是 **JSON Schema**——大模型调用工具时的标准「参数说明书」。AI 不是天然会调函数：运行时把每个工具的名字、描述、参数 schema 塞进提示词，模型据此决定调谁、传什么参数，运行时再执行并回填结果。这个「思考—调工具—看结果—再思考」的循环就是 **ReAct**（Reason + Act），对应 core 里的 ReActLoop 与 PromptBuilder 两个占位类。Javadoc 记录了 PromptBuilder 的五段式提示词结构，其中一条：system prompt 末尾必须附当前日期时间——定时任务凌晨触发时，模型的「今天」全靠它。

### 5.4 影响什么

core 定下来的名词与契约，是其余八个模块的公共语言。tool 模块实现 AgentOSTool，storage 实现ScheduledTaskStore，web 与 cli 环绕 AgentService 编排——骨架阶段这些都还是空method，但「谁向谁负责」已经不可更改。

## 6. storage：7 实体 + 7 仓储 + SQLite 三件套

### 6.1 实体：数据库表在 Java 里的镜像

storage 的 15 个类 = 7 个实体 + 7 个仓储 + 1 个 JpaScheduledTaskStore。**JPA（Jakarta Persistence API）** 是 Java 的对象-关系映射标准：把 Java 类映射成数据库表。**实体（Entity）** 就是映射到表的类，**仓储（Repository）** 则是类型化的数据访问接口——不用手写 SQL，声明接口即得增删改查。

以 [f93bd27:ToolInvocationEntity.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-storage/src/main/java/com/agentos/storage/entity/ToolInvocationEntity.java) 为例：

```java
@Entity
@Table(name = "tool_invocations")
public class ToolInvocationEntity {
    @Id @Column(name = "id") private Long id;
    @Column(name = "session_id") private String sessionId;
    @Column(name = "tool_name") private String toolName;
    @Column(name = "invoked_at") private Instant invokedAt;
}
```

类上的注解说清了它的定位：「每次 Tool 调用记录（含 Sandbox 拒绝，success=false）……核心阶段就写入落库，不是只放日志」。这是一条审计红线：AI 每次动了什么工具、成功与否，都要进数据库留痕，而不是散落在日志文件里随轮转消失。七张表覆盖会话（sessions）、两次审计（tool_invocations / llm_calls）、定时任务（scheduled_tasks / task_executions）、通知渠道（notify_channels）、记忆（memory_entries），表名与字段全部对齐技术方案第 9.2 节。

### 6.2 SQLite 三件套：方言、WAL、建表策略

**SQLite** 是一个嵌入式数据库——整个数据库就是一个文件（这里是 .agentos/agentos.db），不用装服务进程。它适合单二进制部署，但配 Spring Boot 有三个坑，这个提交在每个坑上都预埋了解法，全部集中在 [f93bd27:application.yaml](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-boot/src/main/resources/application.yaml)：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:.agentos/agentos.db?journal_mode=WAL&busy_timeout=5000
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.community.dialect.SQLiteDialect
```

你可能会问：连 PostgreSQL 都不用配「方言」，SQLite 为什么要？

**方言（Dialect）** 是 ORM 框架里「这门数据库的 SQL 方言翻译器」——同一件事（翻页、取当前时间）每种数据库写法不同，方言负责把标准语义翻译成具体 SQL。Spring Boot 3 内置的主流方言没有 SQLite，因为官方没出。解法是 storage 模块显式引入 hibernate-community-dialects 依赖，再在 yaml 里手工指定 `org.hibernate.community.dialect.SQLiteDialect`。版本还有讲究：pom 里写的是 `${hibernate.version}`——直接引用 Boot parent 传入的 Hibernate 版本属性，保证方言和 ORM 主版本严丝合缝，不另起版本。

第二件，URL 里那两个参数。**WAL（Write-Ahead Logging，预写日志）** 改变 SQLite 的写盘方式：写操作先追加到日志文件，读者和写者不再互相阻塞——多线程场景下吞吐显著提升。busy_timeout=5000 则是遇到锁时先等 5 秒再报错。注释点明了一个隐蔽陷阱：连接池 HikariCP 默认不透传 pragma，所以这两个开关必须写在 JDBC 连接串里，而不是配置块里。

第三件，`ddl-auto: update`——启动时自动比对实体与表结构，缺的表自动建。注释记录了边界判断：「update 在 SQLite 上只做 CREATE TABLE，其唯一可靠场景」——即只信任它建首版表，别指望它做复杂的列变更迁移。

### 6.3 超时三档与凭证占位：红线写进配置

同一份 yaml 还有两段与三件坑无关、却属于项目红线的配置：

```yaml
agentos:
  timeout:
    llm-call: 60s
    tool: 30s
    total: 300s
  provider: {}
```

超时不是一刀切的 60 秒，而是分步预算：等大模型回复最多 60 秒、跑一个工具最多 30 秒、整轮任务最多 300 秒。三层各管一段——工具卡死不该拖垮整轮预算的判定，整轮超时又不该被单次慢响应误伤。注释里的红线是「代码中不得硬编码超时」：默认值住 yaml，每个 Agent 还能在自己的 Profile 里按需覆盖（重推理的 Agent 放宽 llm-call，重批处理的收紧 total）。写死在代码里的超时值改一次要重新编译，住配置里的改一次只要重启。

`provider: {}` 空对象同样是决策：供应商凭证（API Key）一律经 `${ENV_VAR}` 环境变量占位注入，不明文写配置——密钥进了 git 历史就永久泄漏，占位符让密钥只活在运行环境里，仓库里永远只有变量名。cli 模块的 ConfigLoader 负责把这些占位符替换成真值，两个模块在这条红线上遥相呼应。

### 6.4 影响什么

- 数据层从此有固定形状：七个接口（仓储）摆在那里，实现阶段填查询逻辑即可；
- 三个坑的解法以配置形式沉淀，实现者无需再踩；
- 审计表 day one 就在实体清单里，呼应「审计不是日志」的产品承诺。

## 7. tool 与 memory：契约里藏着的产品决策

### 7.1 沙箱：五类动作，四张白名单

AI 会执行 shell 命令、读写文件、发 HTTP 请求——不设防线就是「模型犯傻时 rm -rf 你的家目录」。[f93bd27:ActionType.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-tool/src/main/java/com/agentos/tool/ActionType.java) 把受控动作枚举成五值：FILE_READ、FILE_WRITE、SHELL_COMMAND、HTTP_REQUEST、NOTIFY。读写分成两个值，是为了将来能「给读不给写」。

[f93bd27:SandboxChecker.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-tool/src/main/java/com/agentos/tool/SandboxChecker.java) 按动作类型路由到四个私有校验方法，比对四张 yaml 白名单。这里有两个教科书级的决策写在注释里。

其一，诚实的威胁模型：「应用层白名单（劝阻级防线，防模型犯傻误操作，防不住蓄意绕过）」。没有吹成安全边界——应用层校验挡得住幻觉失误，挡不住真正想逃逸的攻击者，那是操作系统级沙箱（容器、沙箱进程）的职责。工程文档敢写「我防不住什么」，比堆砌形容词值钱。

其二，NOTIFY 独立白名单，不复用 http.allowed_domains。为什么发通知不像发请求一样走 HTTP 域名白名单？因为通知渠道的 webhook URL 里通常内嵌 token（形如 https://hooks.example.com/TOKEN），等同凭证。把它放进通用 HTTP 白名单，等于让模型有权向「存着凭证的地址」发任意请求。两张白名单分开，是一行代码都还没写时就定下的口径。

### 7.2 长期记忆：一个接口的四条契约

[f93bd27:LongTermMemoryStore.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-memory/src/main/java/com/agentos/memory/LongTermMemoryStore.java) 是本提交里注释密度最高的类——四个方法，每条都带行为契约：

| 方法 | 契约要点 |
|---|---|
| append(content, scope) | 默认落归档区；只有显式标 CORE 才进核心区——「系统不猜」 |
| load() | 返回核心区全量 + 归档区截断后内容；每次重读，不缓存 |
| recallByKeyword(keyword) | 只在归档区匹配；核心区不参与（它已被 load 全量注入） |
| truncateIfNeeded() | 只截断归档区；核心区永不截断 |

**核心区 / 归档区** 是两层记忆：核心区放永远有效的关键事实（身份、偏好），全量注入每次提示词；归档区放过程性记忆，超限截断、按关键词检索。记忆分两级、写哪级由 Agent 显式指定、读写不缓存（保证刚存的记忆下一轮立刻可见）——这四条不写进接口注释，实现者几乎必然在某条上走偏（比如顺手加个缓存）。

这个接口本身也是可插拔设计：默认档 MarkdownMemoryStore（记忆就存在 Markdown 文件里，人可以直接打开编辑），将来换 Mem0 等语义检索后端，靠 `memory.backend` 一行配置切换。而 [f93bd27:MemoryService.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-memory/src/main/java/com/agentos/memory/MemoryService.java) 是 **门面（Facade）**——ReAct 循环只跟这一个类要记忆上下文，不分别去问会话存储和记忆后端。它还立了条注入纪律：提示词的 Memory 段只放长期记忆，会话历史由另一段独立注入一次，两股数据不混流。门面在骨架里是空类，但「不得绕过门面直连后端」的规矩已经立下。

### 7.3 影响什么

- 后续 24 个工具类（7 内置 + 2 记忆 + MCP 适配）全部长在同一插座上，注册进 ToolRegistry 即可被模型发现；
- 记忆后端可替换的承诺由接口签名兜底，不靠文档口头维系；
- 沙箱口径（含「防不住什么」）在第一周就定案，避免实现时各自理解。

## 8. 次要与噪音：web、cli 与生成物

### 8.1 web：一个信封管到底

[f93bd27:ApiResponse.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-web/src/main/java/com/agentos/web/ApiResponse.java) 是四字段 record（code / message / data / timestamp），配 ok / error 两个静态工厂。这是 **响应信封（envelope）**：所有 REST 接口——成功与失败——返回同一种外层结构，前端只需写一次解包逻辑。Javadoc 特意声明错误也复用同一信封、不另建 ErrorBody，「随首个管理端点引入」。八个 Controller 把技术方案 7.2 节的路由清单全部占位（/api/v1/agents、sessions、memory、tools、notify-channels、schedules、system 等），路由即接口文档。

### 8.2 cli：12 个子命令与一套启动策略

[f93bd27:AgentOSCli.java](https://github.com/fangkun119/agent-os-poc/commit/f93bd27fd964e93ab8af155e9818f8f727583614/agentos-cli/src/main/java/com/agentos/cli/AgentOSCli.java) 用 picocli 的 @Command 注册九个顶层子命令（profile 再挂四个叶子，合计十二个）：init / status / chat / serve / gateway / profile×4 / provider list / tool list / session list。类注释记录了一条启动策略：不需要 Spring 的命令（init、profile list）直接做文件操作、秒回；需要调 LLM 的（chat、serve、gateway）才启动 Spring 上下文。CLI 工具体验的差异就在这一行决策——用户跑 `agentos profile list` 不该等 Spring 起五秒钟。ConfigLoader 负责把配置里的 `${ENV_VAR}` 占位符替换成环境变量真值，与下节凭证红线配套。

### 8.3 噪音略过

mvnw / mvnw.cmd / .mvn/wrapper（约 487 行）是 Maven Wrapper 自举脚本，让没有装 Maven 的机器也能构建——生成物，无设计含量，略过。.gitignore 七行加 target/ 与 IDE 目录，常规卫生操作。

## 9. 收尾：学到什么，怎么迁移

这个提交教的最重要一课：**骨架提交写的不是代码，是约束**。三类约束层层递进：

| 层 | 载体 | 约束内容 |
|---|---|---|
| 构建层 | 九个 pom.xml | 模块边界与依赖方向，越界编译不过 |
| 契约层 | core 的接口 | 依赖倒置，业务定义需求、供给方实现 |
| 行为层 | Javadoc 注释 | 不缓存、只截归档区、notify 独立白名单…… |

迁移到自己的项目，可操作的清单：

- 第一个工程提交就该是全模块骨架，编译全绿后再写实现；
- 依赖方向写在 pom 里而非 wiki 里，让编译器当架构警察；
- 每个空实现的 Javadoc 写清「对应设计文档第几节 + 关键行为契约」，让实现无可争议；
- 换实现的技术承诺（记忆后端、数据库）用接口 + 配置项落地，不留在路线图 PPT 里；
- 踩过的环境坑（方言、WAL 透传）当场沉进配置文件注释，别让第二个人再踩。

另一个可借鉴的动作是把「防不住什么」写进注释——沙箱自标「劝阻级」，比任何安全话术都更让维护者清醒。

## 10. 设计决策记录

| # | 决策 | 依据 |
|---|---|---|
| 1 | 提交池取最近 1 个提交 f93bd27（git log -1 实测），未提交内容未纳入 | 任务默认；用户未要求纳入未提交 |
| 2 | mvnw / mvnw.cmd / .mvn/wrapper / .gitignore 归为噪音略过 | 构建工具生成物与常规卫生操作，无设计内容 |
| 3 | 深读文件 12 个（pom 四件、yaml、core 三类、memory 两类、tool 三类、storage 两类、cli 与 web 各一类），超出 8 上限按主题合并讲解 | 构建主题与实体示例按组读取，未逐文件展开 |
| 4 | Spring AI 未出现在任何 pom——提交信息明示「按第一周 spike 结论再引入，暂不进依赖」，属有意延迟而非遗漏 | 提交信息原文 |
| 5 | 三视角拍板：97 文件的全量走读不可行，按「构建结构 / 依赖方向 / 领域契约 / 持久化 / 安全与记忆 / 入口」六个主题深讲，provider 两类纯占位仅在表提及 | 读者认知负荷与篇幅基准 |
| 6 | 动机仅取自提交信息、Javadoc 与 diff 注释，未引用 docs/ 原文（任务指定无额外文件）；Javadoc 中的「TS x.x」指技术方案章节，按引用处理 | 素材边界 |
| 7 | 无 chat/ 目录文件进入提交池，无需噪音过滤 | git show --stat 实测 |
