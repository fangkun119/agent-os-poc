# agentos Java 编码规范

> 本文件约束 agentos/ 子树的 Java 编码行为；与仓库根 CLAUDE.md 冲突时以根文件为准——根文件管架构与治理，本文件管代码怎么写。
> 条款由三份异源草稿合并落盘（google 判例 / alibaba 判例 / 本仓 stack 实测），条款末尾 [源:草稿:C-ID] 为回查锚、不占语义；逐条取舍依据见文末注释区「来源与终选档案」。

## 适用范围与依据

- 适用：agentos/ 九模块 Java 源码（Java 21 + Spring Boot 3.5.16 + Spring AI 1.1.x 只用 Provider 层 + SQLite + Picocli CLI；模块清单见 TS 第 10 章）。骨架仓、实现期尚未开始——条款重心是「实现期不踩坑、不破坏已立契约」。
- 强度约定：禁/必须为强制；应为项目级收口；宜为源判例建议级、不升格；例外一律显式以「例外：」前缀标出。
- 条件式条款以「引入 X 时」或章题括注为生效前提。

## 0 骨架契约（红线指针与零依赖现状）

- 全仓现状零 Lombok、零日志框架：禁惯性写 @Slf4j/@Data 等注解（依赖不存在，编译失败）、禁各文件分叉自加依赖；首个引入者一次性完成选型并把决定落回本文件。[源:stack:C-23]
- CLI 命令面 System.out 直出是骨架期功能输出（豁免）；应用/运行时代码日志待日志门面引入后走门面，禁混入 CLI 命令面。[源:stack:C-23]
- 五条红线以仓库既有 CLAUDE.md 为准，此处只列开关级陷阱。① tool 只准由自研 ReAct 循环 + ToolExecutor 执行：`internalToolExecutionEnabled` 必须显式 false（官方默认 true、官方示例不关，照抄即框架内双执行、审计取不到数）。② Provider 按 name 显式映射 ChatModel，禁注入 List / `Map<ChatModel>` 按类型扫描。③ 会话历史唯一入口 PromptBuilder，禁 ChatMemory 族。④ llm/tool/total 三档超时只落 application.yaml 默认 + Profile settings 覆盖（键名 llm-call，见 P-01），代码禁硬编码。⑤ 凭证只走 `${ENV_VAR}` 占位，日志与命令行最多打印前 5 位前缀。[源:stack:C-01,C-22,C-40,C-20,C-02; alibaba:C-13,C-19]

## 1 Spring AI 引入期（模型接入与 ReAct 循环；引入 Spring AI 时生效）

- 锁定 Spring AI 1.1.x 口径：2.0-only 机制（AdvisorParams 自动注册、toolNames 移除等）不照抄；启停 chat 自动配置用顶层键 spring.ai.model.chat——spring.ai.openai.chat.enabled 在 1.1.x 已移除，写了不报错也不生效。[源:stack:C-34]
- 自研循环走官方用户控制路径：call → hasToolCalls → executeToolCalls → conversationHistory() 重建 Prompt 再 call；禁从 ChatResponse 抠 tool 消息手拼历史（配对漏错即 400 或上下文静默丢失）。@Tool 不设 returnDirect=true（结果直返调用方 = 循环提前终止）。[源:stack:C-55,C-89]
- ReAct 循环全程禁切换执行线程：禁 @Async、禁跨线程 CompletableFuture、禁把实体/EntityManager 传进异步闭包（ProfileContext 是 ThreadLocal）。例外：虚拟线程是线程形态不是切线程；并行化一律传 ID/DTO 跨线程、目标线程重查。[源:stack:C-07]
- 若启用流式：Flux 在 Reactive 栈线程发射，必须回调用线程收口（collectList().block() / toIterable() / toStream()）再进循环。[源:stack:C-76]「站点备注 2026-09-12：这一条只适用于项目扩展阶段，在核心阶段不适用——核心阶段不启用流式（TS 决策三、DA「核心阶段不做」清单已定）；逐 token 推送场景的收口方式本条未覆盖，留待扩展阶段流式功能 spec 定义。经用户确认加入。」
- 经 ChatClient 注入的文本默认按 StringTemplate 渲染：prompt 夹带字面 `{}`（JSON/schema/代码）会报错或被静默改写——换分隔符或用 NoOpTemplateRenderer；直用 ChatModel/Prompt 无此面。[源:stack:C-77]
- MCP server 连接懒加载（要用时才连接/列取），禁 @PostConstruct 全量初始化——某 server 不可达会拖挂整个启动；懒加载让不可达只在用到时暴露。[源:stack:C-86]
- 思维链从 AssistantMessage metadata 的 reasoningContent 读：字段有无取决于服务端，拿不到是空串不报错，读取处必须判空（限 OpenAI 兼容腿）。[源:stack:C-74]
- 内置 Tool 走仓定四方法接口（getName/getDescription/getInputSchema/execute，见 P-02）：schema 是手写 JSON text block（三引号多行字符串字面量），改 execute 参数必须同步改 text block，否则静默漂移；给实现标 @Tool 替代实现接口属通道错位（@Tool 只属 Plugin 方式三）。[源:stack:C-21]
- @Tool 方法参数/返回类型禁 Optional、Future/CompletableFuture、Mono/Flux、Function/Supplier/Consumer（不可序列化、无 schema 表示）；返回值须可序列化——失败在 schema 生成或执行期才显形。[源:stack:C-19]
- @Tool 必写详细 description（何时该用、参数格式、允许取值）——缺省用方法名顶替，模型该用不用/乱用；参数默认全必填，业务可选参数显式 @ToolParam(required=false)，模型无法自行获值的参数不得标必填（标了模型就编一个值，官方明示的幻觉来源）。[源:stack:C-56,C-57]
- 工具名在同一请求可用范围内唯一：三路工具（内置/@Tool/MCP 包装）汇入 ToolRegistry 后子集内不重名；MCP 与本地同名框架不代为消解，须自行改名——重名静默覆盖/派发歧义。[源:stack:C-58]
- 工具出错抛 ToolExecutionException 包装原异常、禁吞异常返回 null/空串；失败包装进 ToolResult 回喂模型由其决定重试，禁向上抛中断 ReAct 循环（仅不可由模型修正的错误上抛：装配/配置缺失、鉴权失败、预算耗尽等框架级错误）——本条是工具执行面特例，优先于通用异常条款。[源:stack:C-88]
- 禁照官方入门示例自动装配单一 ChatClient.Builder：设 spring.ai.chat.client.enabled=false（见 P-08），ChatClient/ChatModel 由 ProviderService 按显式映射构建。[源:stack:C-78]
- 禁 defaultTools/defaultToolCallbacks/defaultToolNames 与配置侧 spring.ai.openai.chat.options.tool-names/tool-callbacks 全局工具供给（跨请求共享越权、绕开审计）；ToolExecutor 只执行当前 Profile 绑定的工具子集，禁 registry 按名直查就执行的兜底路径。[源:stack:C-29,C-31]
- 禁 defaultSystem()/system() 另立 system prompt 路径：唯一组装入口是 PromptBuilder 五部分——两条路径并存后 prompt 静默缺段（末尾日期时间、Skill 元数据丢失）。[源:stack:C-59]
- base-url（含自建 Provider baseUrl）禁以 /v1 结尾：Spring AI 自动追加 completions-path，手写 /v1 拼成 /v1/v1 每请求 404 且难归因（OpenAI SDK 惯例平移）；协议路径段（如 /anthropic）以各腿实测为准，不在禁列。[源:stack:C-33]
- 必须显式配置 spring.ai.retry.*：默认 max-attempts 10、退避最长 3min，重试链会把 llm 档超时预算形同虚设——收进三档预算或显式关停。[源:stack:C-32]
- 运行时按请求换模型/温度在调用处带 options 覆盖：不改 yaml、不自造「模型选择」环境变量间接层（需重启、多套配置互踩）。[源:stack:C-68]
- 定制 Model 的 HTTP 客户端 RestClient 与 WebClient 必须同时配：只配 RestClient 则流式路径沿用默认，llm 档超时静默失配。[源:stack:C-80]
- 兼容端点非标参数（top_k 等）走 options.extra-body.*（拍平进请求体、不校验拼写，拼错静默透传）；不对官方 OpenAI API 用（400）。[源:stack:C-82]

## 2 配置纪律

- 任何远程调用（HTTP/RPC/外部 API）必须显式设连接与读取超时，禁依赖客户端默认——复制示例代码最常省略的就是超时；与三档超时红线分工不合并（三档管上限值，本条管必须显式设）。例外：长连接/流式（SSE/WebSocket）改设空闲超时与总时长上限，不套用连接-读取二段超时。[源:alibaba:C-02]
- yaml 是唯一配置格式，不新增 .properties：同位置并存时 properties 同名键静默优先。[源:stack:C-42]
- 配置键统一小写 kebab-case（本仓自有键与 Spring Boot/Spring AI 自有键）：定义、注释提及、@Value 占位、@ConfigurationProperties prefix 同一口径——驼峰占位符匹配面收窄会静默落默认值。例外：spring.jpa.properties.* 下是 Hibernate 原生键名（如 hibernate.jdbc.batch_size），以官方原生拼写为准、不受 kebab 化。[源:stack:C-18]
- 时长值一律带单位（60s/300s），禁裸数字表秒——裸数字按毫秒解释，60 静默变 60ms；层级配置收拢 @ConfigurationProperties POJO（如 agentos.timeout.*），零散单值才 @Value。[源:stack:C-17,C-123]
- 属性名转环境变量名三步：`.`→`_`、`-` 删除、全大写——连字符段不补下划线，分隔符归一化惯性会造出绑不上的多下划线名。[源:stack:C-108]

## 3 存储与事务（SQLite/JPA）

- SQL 一律参数绑定、禁字符串拼接；动态表名/排序列等无法绑定的片段用白名单校验（操作化转写，非源原文子句）。[源:alibaba:C-01]
- 数据订正与直连变更（人工或 agent/CLI 直发 UPDATE/DELETE）前，先用同一条件 SELECT 确认影响面再变更（agent 拿到 DB 通道可直达变更，本仓 CLI 直连场景尤其真实）；应用内经 Repository 的键级更新不在要求内（防重靠本章唯一约束，原子性靠事务边界）。[源:alibaba:C-21]
- 业务唯一字段（含组合唯一）在库层建唯一约束，禁只靠应用层「先查后插」防重。[源:alibaba:C-14]
- 分页在查询层完成（LIMIT/OFFSET、Pageable），禁查全量后内存 subList 截取。例外：已知有界的小数据集（如固定配置清单）。[源:alibaba:C-22]
- 无界大表禁一次载入单个持久化上下文（EntityManager 的一级缓存，只增不减，终将 OOM 内存耗尽）；批量写每 30 条左右（10~50 区间）flush()+clear() 并配 hibernate.jdbc.batch_size（10~50）。[源:stack:C-30]
- native query 禁 SELECT *、逐列写明；只读场景（列表/统计/审计查询）用 DTO 投影或标量查询只取所需列，禁整实体取出只用两三字段。[源:alibaba:C-98; stack:C-92]
- 查询结果禁 HashMap/Hashtable 接收；超 2 个参数的查询封装禁 Map 传输，用具名类型（AI 懒建 DTO 惯性）。[源:alibaba:C-58,C-48]
- sum() 等聚合结果接收前必须防空（IFNULL/COALESCE 或对象型判空）：空集聚合返回 NULL，直接拆箱 NPE。[源:alibaba:C-57]
- 新表带 id、create_time、update_time；任何更新路径（含 native update 绕过 @PreUpdate）必须同步刷新 update_time。[源:alibaba:C-59,C-98]
- 新增 @ManyToOne/@OneToOne 显式 fetch=LAZY：JPA 默认 EAGER、EAGER 无法按查询覆盖、secondary select 静默 N+1（循环内逐条发查询的反模式；官方原文 recommendation，本条为项目级收口 + 评审出口）。例外：确需 EAGER 的关联评审说明理由。[源:stack:C-16]
- 一条 JPQL 里 JOIN FETCH 至多一个集合（多集合拆查询或 Hibernate.initialize）；禁映射级 @Fetch(JOIN)（标 LAZY 也 eager 化、SELECT 自带 N+1）。[源:stack:C-90,C-91]
- N+1 先改单查询（JOIN FETCH/DTO 投影）、@BatchSize 兜底；循环内逐条查库/写入宜批量化（源为建议级措辞）。[源:stack:C-91; alibaba:C-98]
- 实体手写带参构造器必须同时保留无参构造器（protected 即可，JPA 规范要求）。[源:stack:C-48]
- SQLite 主键走 IDENTITY/代码赋值、禁套 @GeneratedValue(strategy=SEQUENCE)（SQLite 无序列对象；官方双分支规则按例外取 fallback 分支，勿按官方主路径改 SEQUENCE + allocationSize）；表达外键优先子端 @ManyToOne，多对多用中间 link 实体、不用裸 @ManyToMany。[源:stack:C-93,C-94]
- 实体/仓库在主类包之外时必须显式 @EntityScan/@EnableJpaRepositories（见 P-10）：auto-configuration packages 只取主类所在包（com.agentos.boot）、不受 scanBasePackages 影响；判据看 Spring Data 扫描日志与建表实况、不看启动成败（实测：零报错 + Found 0 repositories + 零建表）。[源:stack:C-26]
- 主类固定 com.agentos.boot（见 P-10）且保留 scanBasePackages="com.agentos"，新建模块包必须落在 com.agentos 根下。例外（官方出处裁决）：官方建议主类放根包，本仓跨模块 Bean 装配依赖此显式声明，不移。[源:stack:C-03]
- 主类只做装配起点：禁堆 @EnableXxx、禁显式 @ComponentScan。例外：@EntityScan/@EnableJpaRepositories 是上一条的官方许可出口，不属此禁；领域专项 @EnableXxx（如调度 @EnableScheduling）放独立 @Configuration 类按需 @Import，不上主类。[源:stack:C-27]
- spring.jpa.hibernate.ddl-auto 保持 update（SQLite 首建唯一可靠路径）：不删行（落 none 则一张表不建）、禁 create-drop（重启删表）、禁自引 Flyway/Liquibase。例外（官方出处裁决）：官方「生产用增量迁移」警示与本仓 SQLite 首建豁免冲突，按例外执行。[源:stack:C-08]
- SQLite 社区方言须成套：pom 引 hibernate-community-dialects（版本 ${hibernate.version} 不写死）+ yaml 指 org.hibernate.community.dialect.SQLiteDialect，缺一即启动炸。例外（官方出处裁决）：Hibernate 6「显式设 dialect 已 discouraged」带第三方方言例外，本仓属例外，禁按官方字面当遗留清理。[源:stack:C-15]
- spring.jpa.open-in-view=false 该行不得删（删行静默回退 open-in-view、改变连接占用模式且无报错）；Hibernate 原生属性只写 `spring.jpa.properties.<原生键名>`（前缀拼错静默忽略）。[源:stack:C-109,C-110]
- SQLite journal_mode 收口 {WAL}（Write-Ahead Log，先写日志后落页的崩溃安全模式）：禁 OFF/MEMORY（MEMORY 崩溃即损坏、OFF 回滚未定义）及其余取值——本仓收口为封闭枚举，属站点例外裁决，勿据官方多模式文档放宽。[源:stack:C-04]
- 连接参数只走 spring.datasource.url 连接串（journal_mode=WAL&busy_timeout=5000，pragma 即 SQLite 连接级配置语句、单位毫秒，见 P-11）：禁自建 DataSource Bean（自建即关自动装配）、禁第二入口——HikariCP 不透传独立 pragma，WAL 静默失效。[源:stack:C-14]
- 连接参数 pragma 白名单 {journal_mode, busy_timeout, foreign_keys}（限连接串参数），拼错整条静默忽略；运维/检查类 PRAGMA 语句（如 wal_checkpoint、复查 journal_mode）不在白名单、但只准对照官方文档拼写；foreign_keys 默认 OFF 且按连接生效、事务内设置是 no-op。[源:stack:C-63,C-61,C-62]
- WAL 三件套（.db+-wal+-shm）：备份/迁移整体拷或走官方 backup API/VACUUM INTO 生成单文件快照；禁活跃期只拷 .db、禁手删 -wal/-shm（站点例外收口，随本簇例外裁决）。[源:stack:C-06]
- 「读写互不阻塞」非绝对——BUSY 仍会发生（官方口径按本仓例外裁决，勿据官方文档放宽），写库路径保留报错与有限重试出口；checkpoint 走默认自动机制、禁关 wal_autocheckpoint 无替代；禁 NFS/网络同步盘（WAL 依赖同机共享内存，同一例外裁决）。[源:stack:C-101,C-103,C-65]
- 物理事务尽可能短：@Transactional 体内禁 LLM 调用、tool 执行、任何阻塞等待——SQLite 单写者 + 100 并发目标下，长写事务把其他会话顶到 busy 超时；先完成外部工作再开事务落库。[源:stack:C-05]
- 长/流式/分页读用短事务分批：持续读事务饿死 checkpoint 使 WAL 无界增长（磁盘膨胀 + 全库读变慢）。[源:stack:C-64]
- 一个业务动作的多次写库落同一事务边界（服务层 @Transactional），禁逐语句独立提交（session-per-operation）。[源:stack:C-72]
- 持久层抛异常即回滚并放弃该工作单元：禁 catch 后在同一事务/持久化上下文继续写库或复用受管实体（rollback-only 到提交期才爆）；事务内吞掉的业务异常若不应半程提交必须显式回滚（吞异常不触发自动回滚；注释出口仅适用业务语义异常，持久层异常一律弃单）。[源:stack:C-73; alibaba:C-04]
- EntityManager 与受管实体只在本线程用：跨线程只传 ID/DTO 到目标线程重查（与 ReAct 线程红线互补）。[源:stack:C-87]

## 4 构建、依赖与 CLI

- 构建与测试一律在仓库根 ./mvnw 对聚合 pom 执行：改了被依赖模块必须覆盖下游（全 reactor 或 --also-make-dependents；缩范围 -am/-amd），子模块目录单跑 = 旧 SNAPSHOT 假通过。[源:stack:C-10]
- 提交前干净构建并等全量测试通过：只跑受影响模块或凭推断宣称通过不算数（现状零测试 = 构建通过，首个测试落地后自动升全量门）。[源:stack:C-11]
- Maven 版本仲裁唯一：模块 pom 三方依赖不写 `<version>`（BOM 管理；BOM 外收敛根 pom 属性）；内部模块互依一律 ${project.version}；同一版本根 properties 定义一次；parent 3.5.16 禁擅动。[源:stack:C-09]
- 同一 GAV（Maven 坐标 Group:Artifact:Version）禁出现不同 Version。[源:alibaba:C-80]
- 子模块 pom 只写 artifactId（groupId/version 继承聚合 pom）；parent 写完整 GAV 且与聚合 pom 逐字一致；深两层模块显式 relativePath=../../pom.xml。[源:stack:C-36,C-38]
- 禁定义与父链同名 property（继承合并后才求值，静默改写父侧引用；确需覆盖时放行、但必须注释显式标注意图）；模型变量统一 ${project.*} 全前缀；新增模块三处同步（聚合 pom 加 `<module>`、新模块写 `<parent>`、packaging=pom 保持——缺 module = 静默漏构建）；目录名 = artifactId = `<module>` 值，三者禁漂移。[源:stack:C-37,C-39,C-75,C-102]
- core 是纯契约层（唯一三方依赖 SnakeYAML）：JPA/Web/Picocli 依赖只出现在子模块；模块依赖单向向下禁成环。[源:stack:C-71]
- BOM/dependencyManagement 只定版本不进 classpath：删依赖声明前确认无直接 import 且传递链不覆盖（hibernate-community-dialects 不在 data-jpa 传递链，删声明 = 启动期 ClassNotFound）；同厂商多构件用 type=pom/scope=import 导 BOM（首例 spring-ai-bom），禁逐构件手写版本。[源:stack:C-28,C-69]
- 源码直接 import 的第三方构件直接声明（Spring Boot/Spring AI starter 及其 BOM 受管传递构件——org.hibernate.*、com.fasterxml.jackson.* 等——除外）；仅测试用 scope=test；依赖冲突先 mvn dependency:tree 再精确 exclusion（凭猜写错静默不生效）；断言「没配 X」先看 mvn help:effective-pom。[源:stack:C-96,C-97,C-98,C-100]
- 双入口分工（见 P-03）：agentos-cli 走 Picocli 不启 Spring 上下文、java -jar 走 boot Spring 入口，禁互相塞启动逻辑。[源:stack:C-25]
- CLI 入口统一 new CommandLine(new X()).execute(args)：禁 parseArgs/populateCommand/parse（三者不自动打印帮助，-h 静默无效）。[源:stack:C-12]
- call() 返回值经 execute 透传退出码：未实现与执行失败不得与成功共用 0（码表数值本稿不定义、留实现期裁定，暂不自创）。[源:stack:C-13]
- 每个 @Command 显式 name 与 description（缺 name synopsis 退化为 `<main class>`）+ 一律 mixinStandardHelpOptions=true（禁手写 help/version 布尔字段）；版本号只写 @Command(version=...)，不塞 description/footer。[源:stack:C-41,C-105,C-67]
- 单值 @Parameters 显式 index（反射不保证声明顺序，多参数重排静默错位；多值可省）；defaultValue 在 description 写 (default: ${DEFAULT-VALUE}) 变量注入、禁手抄字面值；命令选项禁收密钥明文（挂凭证红线）。[源:stack:C-104,C-106,C-66]
- 禁 org.apache.commons.beanutils 属性拷贝（用 Spring BeanUtils 或 cglib BeanCopier，均浅拷贝）；Pattern 预编译为类级常量、禁方法体内反复 compile；整数随机数用 nextInt/nextLong、禁 (int)(Math.random()*n) 放大取整。[源:alibaba:C-52,C-65,C-92]

## 5 日志与测试（日志条款引入日志时、测试条款首个测试落地时生效）

- 统一 SLF4J API + starter 默认 Logback：不引第二套框架、不 import 实现类（JUL 日志不路由进应用日志，尤避）。[源:stack:C-43]
- 默认只写 console，落盘必须显式 logging.file.name，轮转走 logging.logback.rollingpolicy.*；调级唯一入口 `logging.level.<logger-name>`（logger 按所在类声明作坐标）；LOGGING_LEVEL_* 环境变量仅包级有效（relaxed binding 强制全小写）；Logback 无 FATAL，统一写 ERROR。[源:stack:C-44,C-46,C-45]
- 定制日志配置文件名必须 logback-spring.xml（logback.xml 加载过早且扩展标签不可用），logging 属性占位符用 Spring `:` 非 Logback `:-`；日志初始化早于 ApplicationContext（@PropertySource/@Configuration 控制不了日志）。[源:stack:C-47]
- 日志用 {} 占位符传参，字符串变量间禁 + 拼接（常量字面量不受限）；error 必带现场参数与异常对象（异常作最后一个参数），不处理则上抛。[源:alibaba:C-55]
- 生产代码禁 System.out/System.err/printStackTrace 充当日志。例外：CLI 命令面标准输出是功能输出（见「骨架契约」章）；禁用 JSON 工具序列化整个对象打日志（敏感字段随对象外泄）；用户输入/参数错误宜记 warn，error 只留系统逻辑故障（源【推荐】）。[源:alibaba:C-66,C-95,C-96]
- 禁给 debug/info 普遍加 isDebugEnabled 类级别 guard（SLF4J 占位符已惰性求值，机械包开关会诱导回退拼接写法）：仅当日志参数本身需昂贵计算（对象深加工、远程查询）时才加级别 guard——本条为改判条款，禁按手册原文【强制】恢复。[源:alibaba:C-102 改判]
- 单测必须用测试框架断言 API（JUnit/AssertJ assertXxx、Mockito verify），禁只调用不断言、禁 System.out 人肉验证；不依赖外部环境（网络/真实服务/中间件），依赖注入 mock。[源:alibaba:C-24,C-20]
- 单测之间禁互相调用、禁依赖执行次序与共享可变状态；测试数据程序化造数宜回滚或带标识；修 bug 附复现测试、做功能附验证测试并入仓库防回归——只在会话里跑一遍不算验证。[源:alibaba:C-56,C-20; stack:C-70]
- 测试统一 JUnit 5 并随首个测试引入 spring-boot-starter-test：禁混 JUnit 4（@RunWith/junit-vintage）、禁冗余 @ExtendWith(SpringExtension)（@*Test 已元注解）。[源:stack:C-24]
- 集成测试类放主应用类同包或子包（@SpringBootTest 从测试类包向上搜主类，放错包搜不到且报错难归因）；单测试类只允许一个 @*Test 切片注解（叠加能力用 @AutoConfigure… 手动补）；测试自定义配置一律 @TestConfiguration（内嵌 = 追加、顶层 = @Import 防扫描），禁内嵌 @Configuration 整体替换主配置、禁顶层裸 @Configuration 被全局捞进；替换/spy Bean 用 @MockitoBean/@MockitoSpyBean、禁旧 @MockBean/@SpyBean（语料滞后惯性）。[源:stack:C-54,C-111,C-52,C-112]
- Web 层测试默认 @SpringBootTest + @AutoConfigureMockMvc（webEnvironment 默认 MOCK），不默认 RANDOM_PORT；确需真服务器用 RANDOM_PORT + @LocalServerPort，禁 DEFINED_PORT（撞 application.yaml 端口/常驻进程）。[源:stack:C-49,C-50]
- 真服务器测试标 @Transactional 也不回滚（客户端与服务端不同线程不同事务）：测后清理显式做。[源:stack:C-51]
- @DataJpaTest/@JdbcTest 一律显式 @AutoConfigureTestDatabase(replace=Replace.NONE)（见 P-09）走真实 SQLite DataSource：默认替换装配内嵌库、classpath 有 H2 静默换库漂移；只测 Web 层用 @WebMvcTest 切片 + @MockitoBean 提供协作者（切片/集成测试连真实 SQLite 属站点绑定，不受上一条单测 mock 子句约束——本地库非外部环境）。[源:stack:C-53,C-113]

## 6 接口与远程调用

- 错误响应统一非泛型 ApiResponse record（code 为 HTTP 风格字符串，见 P-07）：复用不另建 ErrorBody、禁自建错误响应类或泛型化；请求体解析现状裸 String 收包（DTO 化属实现期决策，不在禁令面）。[源:stack:C-119]
- 代表资源的路径只能为名词（集合宜复数）、全小写禁 .json 后缀、URL 参数不带敏感信息；资源 CRUD 之外的动作用资源子路径动词表达（既有契约如 POST /schedules/{id}/run、POST /agents/{name}/invoke 照旧，TS 7.2，不属违规）；JSON key 一律小驼峰；列表接口无数据返回空集合、禁 null。[源:alibaba:C-49,C-64,C-50]
- 外部入参不信任：逐参验证（分页上限、排序字段白名单、输入长度），批量接口设数量上限，批量 id 禁 GET 超长 query（超 2048 字节，放 body 或改 POST）；errorMessage 不含敏感数据；接口返回敏感字段（手机号/证件号）脱敏、禁原样透出实体；对外签名与路径不直接改名删除，废弃标 @Deprecated 并注明替代。错误响应载体以 ApiResponse record 为准、不引入四要素改造。[源:alibaba:C-30,C-77,C-91,C-51,C-67,C-75; stack:C-119 载体裁决]
- 可能超过 2^53 的整数字段（雪花 id、订单号）对外 JSON 必须声明 String 返回，禁 Long 直接序列化（JS 侧精度静默截断、无异常可捕）。[源:alibaba:C-03]
- 禁 newFixedThreadPool / newCachedThreadPool / newSingleThreadExecutor（Executors 工厂，无界队列或无界线程数，OOM 面）与裸 new Thread 建平台线程，线程池用 ThreadPoolExecutor 显式构造写明核心参数；虚拟线程不受此限：Executors.newVirtualThreadPerTaskExecutor() 或 Thread.ofVirtual()（跨线程仍按第 1 章只传 ID/DTO）；自定义 ThreadLocal 必须 try-finally remove()（线程池复用串值；本仓 ProfileContext 即 ThreadLocal）；定时任务禁 java.util.Timer（单任务未捕获异常静默终止全部任务），用 ScheduledExecutorService。[源:alibaba:C-27,C-28,C-29]

## 7 控制流与异常

- 改动只涉及某几行时，宜不为恢复对齐顺手改未受影响的邻近行：污染版本历史、拖慢评审、加剧合并冲突（源为建议强度 Tip 级，不作强制承载）。[源:google:C-18]
- catch 块不得为空块、禁紧凑空块 `} catch (Exception e) {}`；要么处理（记录日志）要么上抛，最外层转用户可读内容、不透出堆栈。例外：确属无需处理的正当场合，必须在块内注释说明理由（含「认定不可能发生」改抛 AssertionError 的路径）。[源:google:C-02,C-30; alibaba:C-12]
- 禁吞异常返回 null/空串充当正常结果。[源:google:C-02; alibaba:C-12; stack:C-88]
- finally 块内禁 return——覆盖 try 返回点并吞掉异常，双后果一判据。[源:alibaba:C-18]
- 可预检查规避的运行时异常（NPE/越界、级联取值/拆箱/查询结果等高危点）先判空，禁 catch 兜底。例外：数字解析等无法预检查的场景。[源:alibaba:C-53,C-76]
- try 只包非稳定代码；catch 按异常类型区分，可区分时禁 catch (Exception) 一把抓。例外：最外层统一兜底入口。[源:alibaba:C-54]
- switch 穷尽性对语句与表达式一律适用：每个可能值被逐值 case 覆盖或有 default；已逐值穷尽的 switch 表达式宜省略 default（default 会关掉编译器对新增枚举值的漏配报错）；枚举值可能来自旧数据或外部输入时，可用 default -> throw 防御式兜底并注释理由。[源:google:C-01; alibaba:C-47]
- 老式（冒号）switch 分支组必须以 break/continue/return/抛异常终止或注释 fall-through；老式非穷尽场合必须带 default 且置于末尾；switch(字符串外部参数) 先判 null。[源:google:C-20; alibaba:C-47]
- 不应直接抛 RuntimeException/Exception/Throwable，宜抛有业务含义的自定义异常（防 throw new RuntimeException(e) 偷懒包装丢语义；源【推荐】）。[源:alibaba:C-32]
- 异常分支宜用卫语句（guard clause，提前返回的条件判断）或策略模式表达；必须 if-else 嵌套时不宜超 3 层（源【推荐】）。[源:alibaba:C-62]
- 单方法总行数宜不超 80 行，超出拆子操作与共性方法（源【推荐】）。[源:alibaba:C-85]

## 8 数值、集合与 JDK

- 金额与精确小数字段（实体列与对外字段）禁 float/double，用 BigDecimal。[源:alibaba:C-34]
- 禁 new BigDecimal(double)（构造值即带误差），用 String 构造器或 BigDecimal.valueOf；判等必须 compareTo()==0（equals 连精度比，2.0 与 2.00 不等）。[源:alibaba:C-06,C-05]
- 两个整型包装类比值必须 equals、禁 ==（缓存区间外比引用）。例外：与字面量比较的自动拆箱；浮点等值禁 ==（基本类型）与 equals（包装类型），用误差范围或 BigDecimal.compareTo。[源:alibaba:C-15,C-16]
- 值比较禁 `变量.equals(常量)`（变量为 null 即 NPE）：用常量/有值对象作调用方或 Objects.equals；三目两侧避免包装/原始类型混用（强制拆箱可 NPE）；split 结果尾空串被丢弃，按索引访问前宜先查长度（此半句源【推荐】）。[源:alibaba:C-23,C-70,C-72]
- Arrays.asList、List.of/Set.of/Map.of、Collections.empty*/singleton*、Stream.toList() 及 Map 的 keySet()/values()/entrySet() 视图禁 add/remove/clear（asList 的 set 写穿原数组，都不是独立集合）。例外：Map 视图上的 remove/clear 是对原 Map 的合法联动修改、慎用。[源:alibaba:C-11]
- subList 是视图：父集合结构性修改后不得再遍历或增删，需独立先拷贝。[源:alibaba:C-74]
- Collectors.toMap 必用带 mergeFunction 的重载（三参或四参）声明 key 冲突策略、禁双参重载；value 不得为 null，可空值先 filter 或归一缺省再收集。[源:alibaba:C-08,C-17]
- 增强 for 体内禁对被遍历集合 remove/add：用 Iterator.remove() 或 removeIf。[源:alibaba:C-09]
- 自写 Comparator 禁二值化返回（`a > b ? 1 : -1` 式），相等必须返回 0（违反自反传递在 TimSort 排序中段才抛 = 静默失效）；优先 Comparator.comparing。[源:alibaba:C-10]
- 业务代码禁 java.sql.Date/Time/Timestamp（JDBC/ORM 框架内部产物不算违反），用 java.time；禁以 365/366 常量表达一年，用 plusYears(1)/lengthOfYear() 等。[源:alibaba:C-73,C-41]
- 格式串年份小写 y 禁 YYYY（week-year，年末数天错年）、月 M 分清 m（分）、24 小时制 H 分清 h（12 小时制）。[源:alibaba:C-07]
- SimpleDateFormat 线程不安全，禁无保护 static 共享（局部实例或 static 加锁/ThreadLocal）；JDK 8+ 宜用 DateTimeFormatter。[源:alibaba:C-46]
- char/字符串/text block 内的其他空白字符一律写成转义（防拷贝示例带入 NBSP、真实 tab 混入字面量——编译器不报的静默行为差异）；有专用转义的 9 个字符（\b \t \n \f \r \s \' \" \\）用专用转义，不用八进制/Unicode 等价替代。[源:google:C-04,C-37]
- text block 的缩进即字符串值：开三引号必须另起一行；闭三引号位置一变前导空格就进值——编译零警告产出坏内容（坏 SQL/JSON）。[源:google:C-05]
- 方括号属于类型不属变量名：写 String[] args 不写 String args[]（int a[], b 混写必误读）；long 字面量后缀大写 L（小写 l 与 1 难分辨，javac 接受、无格式器反馈）；每条声明只声明一个变量。例外：for 循环头。[源:google:C-14,C-27,C-29]
- 需要限定引用静态成员时用类名（Foo.aStaticMethod()，禁 aFoo. 就近续写，javac 不报错）；凡合法标注 @Override 的方法一律标注（漏标不触发编译错误——父方法改名后「覆盖」静默变成无关新方法）。例外：父方法已标 @Deprecated 时可省。[源:google:C-15,C-16]
- POJO 属性与对外传输字段用包装类型（局部变量宜基本类型）——null 语义与拆箱 NPE 面；POJO 属性禁写默认值（=0、=new Date() 抹掉 null 语义），null 语义由使用者显式保证。例外：只读投影 record 的聚合组件（查询侧已按第 3 章兜空）可用基本类型并注明。[源:alibaba:C-38,C-39]

## 9 命名、注释与组织

- 一个 .java 只放一个顶层类，文件名与类名逐字符一致（防「小 DTO 就近追加到现有文件底部」）。[源:google:C-07]
- 禁通配导入（java.util.* 一把梭是 AI 惯性，同名类延迟爆雷 build）；禁 import module（JDK 25 新语法幻觉）。[源:google:C-03]
- 驼峰转换走固定流程：先全小写再逐词首字母大写（XML HTTP request→XmlHttpRequest、new customer ID→newCustomerId），禁 XMLHTTPRequest/newCustomerID 式缩略词保形。[源:google:C-12]
- 禁类型前后缀（mName/s_name/kName/name_）；包名全小写纯拼接（禁 deepSpace/deep_space）；非常量一律小驼峰（禁 send_message snake_case 平移）；测试类名以 Test 结尾（FooTest，惯例级）。[源:google:C-10,C-22,C-25,C-06]
- 禁 blackList/whiteList/slave，用 blockList/allowList/secondary/replica。[源:alibaba:C-82]
- UPPER_SNAKE_CASE 仅限 static final 且深层不可变——Logger、可变集合、元素可变数组不是常量，「打算永不改」不构成判据；局部变量即使 final 也不写常量风格。[源:google:C-17,C-11]
- 业务语义字面量（缓存 key、状态码、阈值）先具名常量再引用。例外：0/1/-1 等惯用占位。[源:alibaba:C-36]
- 布尔字段名禁 is 前缀（写 deleted 不写 isDeleted——部分框架属性解析错位引起序列化错误；禁的是字段名，getter 写 isXxx() 是 JavaBeans 惯例不在禁列）；同一布尔属性禁 isXxx() 与 getXxx() 并存。[源:alibaba:C-25]
- 禁子类成员变量与父类同名、同方法不同代码块局部变量同名（就近取名撞已有字段名，读代码静默取错值）。例外：访问器参数名与字段同名的惯例。[源:alibaba:C-35]
- 禁裸 `// TODO` 漂移格式：统一「TODO(负责人/日期): 说明」并指向去向（工单或 issue，本仓对应 chat/todo/ 工单体系）；「未来某时做某事」类 TODO 必须带具体日期或事件。[源:google:C-13; alibaba:C-108]
- 类/属性/方法的职责说明必须写 Javadoc（/** */）、不得用 // 行注释替代（含描述总体目的的注释）；注释不用星线等字符画框包围。[源:alibaba:C-63; google:C-39,C-28]
- 注释掉的代码应直接删除（历史查 git），确需保留待恢复的在上方写明理由（///）；不再使用的字段/方法/内部类/参数应删（源为【参考】+【推荐】强度，写「应」不写禁令）。[源:alibaba:C-88,C-89]
- 未实现方法统一抛 UnsupportedOperationException("尚未实现：<方法>（TS x.y）")（见 P-04）：禁 return null/空方法体/打印后返回默认值等自创占位（return null 被误接线静默通过是真实后果）；存量四种并存的清理不在本稿范围、留实现期统一安排。[源:stack:C-116]
- 类/方法 Javadoc 锚 TS 章节号（见 P-05）、错误文案全仓一致：新增文件禁英文 Javadoc 或无锚注释——本仓中文 Javadoc 即规格，按锚回查方案文档。[源:stack:C-117]
- Javadoc 的 {@link} 大量指向规划中尚未实现的类（见 P-06）：读注释不能当现有 API 清单，禁按 javadoc 引用直接 import（编译失败）或把规划描述当已实现行为调用。[源:stack:C-118]

## 10 风格基线

- 缩进与仓库既有一致（本仓 4 空格、0 个 tab），禁 tab/空格混用；其余排版（列宽数值、注解换行、switch 缩进）交格式化工具机械保证、不逐条手调。[源:stack:C-95; google:C-34,C-38,C-40]
- if/else/for/while/do 一律大括号、单行也不例外。例外：lambda 本身可选；else-if 链不强制给 else 套层。源文件一律 UTF-8；列宽豁免只保两处格式化器防不住的面：text block 内容、注释内可复制进 shell 的命令行不硬折行。[源:alibaba:C-60; google:C-09,C-24,C-19]

## 11 代码检索与导航（Agent 工具面）

- Java 符号级检索（跳转定义、查引用、找实现、调用链、全仓搜符号）优先 LSP 工具（后端 jdtls，brew 安装、PATH 可达）：Grep 按文本命中，查引用混入注释与同名符号，跨模块调用（web→core→storage）误报率高；文档、application.yaml、注释等文本检索仍用 Grep；LSP 工具或 jdtls 不可用时回退 Grep。装机时 brew 提示的 sudo ln 系统注册命令不执行——java_home 择最高版本 JVM，注册即把 Maven 编译带离项目钉死的 JDK 21。[源:站点增条:2026-09-12]

## 维护

- 增条门槛：能并入既有条目的不新开；可对照 diff 检验的错误、且同类错误重复出现才增条；新增站点参数记入文末参数清单。
- 全文维持 200 行以内（注入后对 agent 可见行数）、无互相矛盾的条款；条款修订时在文末注释区「维护记录」追加一行。
- 超预算裁剪按文末注释区「牺牲顺序」执行。

<!-- ====== 以下为人类专用元信息（块级 HTML 注释，注入 agent 前被剥离——Claude Code 加载器实现行为，非格式通性，跨工具消费需另议） ======

【来源与终选档案】
- 三份源稿（各带同目录 -reason 理由表）：chat/consolidate/20260909_claudemd_draft_springboot-java-monolith-google.md、…-alibaba.md、chat/consolidate/20260910_claudemd_draft_springboot-java-monolith-stack.md
- 逐条取舍唯一依据：chat/draft/20260911_3in1_merge/final-pick.md（①选中 92 簇 / ②落选 13 条 / ③31 项冲突裁决 / 牺牲顺序）；六份域对照表在同目录 domains/d1.md~d6.md
- 合并理由报告：chat/consolidate/20260911-claudemd-draft-3in1-reason.md（选中四组价值、落选死因六组、冲突逐条、站点增条 K-92 论证）
- 本文件正文条款 = 3in1 草稿全文落盘（2026-09-12），正文条款与 3in1 逐条对应；[源:…] 内 ID 为源草稿方括号 ID

【alibaba 草稿↔reason ID 错位对照】（回查 alibaba-reason 时以内容名＋reason ID 为准）
| 内容名 | 草稿 ID | reason ID |
|---|---|---|
| 裸抛 RuntimeException | C-32 | C-81 |
| 大括号 | C-60 | C-62 |
| 卫语句 | C-62 | C-85 |
| 方法 80 行 | C-85 | C-60 |
| Javadoc 载体 | C-63 | C-86 |
| TODO 落款 | C-108 | C-89 |
| 删死代码 | C-89 | C-108 |
| 接口敏感字段脱敏 | C-67 | C-33 |
| 对外签名禁改 | C-75 | C-67 |
| 批量 id 超长 query | C-91 | C-75 |
| 查询 Map 载体/传参 | C-58、C-48 | C-58 |
| N+1 批量化 | C-98 | C-48 |
| update_time 刷新 | C-98 | C-59 |
| 反射/二方包捕 Throwable | C-81 | C-32（已落选） |
| 枚举项注释 | C-86 | C-63（已落选） |
| 接口时间格式统一 | C-33 | C-91（已落选） |

注：草稿 [C-98] 一名三用——native query 禁 SELECT *（reason 同号 C-98）/ update_time 刷新（reason C-59）/ N+1 批量化（reason C-48）；正文三处 [源:…C-98] 按内容名区分回查。

【参数清单】（本表为唯一权威，改值须同步正文）
| # | 参数名 | 当前值 | 状态 | 影响条款 |
|---|---|---|---|---|
| P-01 | 超时三档键位（llm/tool/total） | application.yaml 默认 + Profile settings 覆盖；键名 llm-call | SET | 0 骨架契约 |
| P-02 | 内置 Tool 四方法接口 | getName/getDescription/getInputSchema/execute | SET | 1 章 |
| P-03 | CLI/Spring 双入口分工 | agentos-cli（Picocli 不启 Spring）/ agentos-boot（Spring） | SET | 4 章 |
| P-04 | 未实现占位格式 | UnsupportedOperationException("尚未实现：<方法>（TS x.y）") | SET | 9 章 |
| P-05 | Javadoc 锚格式 | TS <章>.<节> | SET | 9 章 |
| P-06 | 前向引用规划类清单 | ProfileContext/SessionManager/ChatModel 等 TS 规划类 | SET | 9 章 |
| P-07 | 错误响应载体 | 非泛型 ApiResponse record（code=HTTP 风格字符串） | SET | 6 章 |
| P-08 | ChatClient 自动装配开关 | spring.ai.chat.client.enabled=false | SET | 1 章 |
| P-09 | 测试数据源 | @AutoConfigureTestDatabase(replace=Replace.NONE) 走真实 SQLite | SET | 5 章 |
| P-10 | 主类与扫描基包 | com.agentos.boot；scanBasePackages="com.agentos" | SET | 3 章 |
| P-11 | SQLite 连接参数 | journal_mode=WAL&busy_timeout=5000（spring.datasource.url 连接串） | SET | 3 章 |
| P-12 | Java 符号检索后端 | jdtls 1.61.0（brew 装于 /opt/homebrew/bin；Claude Code 进程 PATH 不含该目录，经 /usr/local/bin/jdtls 软连接接入——2026-09-12 实测 LSP 全操作可用。其依赖 openjdk 26 仅作 jdtls 自身运行时，brew 提示的 sudo ln 系统注册命令不执行，否则 java_home 按最高版本解析到 26、带偏项目 JDK 21 工具链） | SET | 11 章 |

无 PENDING 参数——alibaba 原 P-01「接口时间格式」因值未决未收（见勿再加清单）。

【勿再加清单】（final-pick 落选 13 条 + 三稿既有否决承继；重复论证前先查此单与 final-pick.md ②节）
- google:C-08 package 必填+禁紧凑源文件：AI 几乎必带 package，且两类违规均为编译期报错、非静默失效
- google:C-21 Javadoc 摘要片段：被「禁英文 Javadoc」连带消解，剩余价值低
- google:C-26 重载连续成组：纯可读性、无静默后果（余量极度富余时可 1 行回收）
- google:C-31 源文件节顺序与空行、C-36 导入分组与排序：机械网，格式化工具可归一
- google:C-32 static import 嵌套类、C-33 泛型类型变量、C-35 Javadoc 标签顺序：触发面窄或弱约束
- alibaba:C-81（reason C-32）反射/二方包捕 Throwable：九模块单体无二方包生态
- alibaba:C-87 注释随代码同步更新：无一行可 diff 判据，源【推荐】
- alibaba:C-86（reason C-63）枚举项注明用途：与 AI 过量注释惯性方向相反
- alibaba:C-33（reason C-91）接口时间格式统一：统一值是站点级设计决策、未决，裁定后可 1 行补回
- stack:C-114 @DataJpaTest 默认回滚盲区：踩一次即学会，主判据（真实 SQLite 数据源）已收 5 章
- 「switch 必用新式（箭头）」维持否决不复活；finalize、每行一条语句、修饰符顺序、块注释星号对齐、K&R 括号、折行位置编号、水平空格白名单、成员间空行、公开方法单字符参数名、最低 Javadoc 覆盖、MySQL 专有簇、错误码体系等三稿否决项照旧不收

【牺牲顺序】（超 200 行预算时按序回收，细则见 final-pick.md）
- 1. K-91 格式化基线；2. K-87/K-66/K-65；3. 档 3 簇整行回收（K-49/K-57/K-58/K-64/K-86/K-88/K-80/K-85/K-69/K-77/K-78/K-79）；4. K-30/K-29/K-32；5. 条件式整段折叠（K-06/K-07/K-08/K-14/K-20/K-21/K-50 移入本注释区，Spring AI/日志引入后恢复）。永不砍清单见 final-pick.md

【维护记录】
- 2026-09-12 v1 落盘：由 3in1 草稿全文落盘（合并评审 must_fix 7 项已修复、站点增条 K-92 已含）；正文条款与 3in1 逐条对应，未做条款增删。
- 2026-09-12 站点备注：第 1 章「若启用流式」条款行内加站点备注——只适用扩展阶段、核心阶段不适用（核心阶段不启用流式，TS 决策三 / DA「核心阶段不做」清单为据），经用户确认加入。起因：SSE 流式收口方式评审——整段收集与逐 token 推送是两种场景，本条只覆盖前者；逐 token 推送的收口方式留待扩展阶段流式功能 spec 定义。行内追加、0 行净增，未触发牺牲顺序；3in1 草稿源头已同步同款备注（chat/consolidate/20260911-claudemd-draft-3in1.md 第 23 行）。

【元规范出处】行数红线、块级注释剥离、增条门槛等机制依据：.claude/skills/claude-md-draft-for-code-standard/references/meta-norms.md（原件 claude-code-memory.md、agentsmd-spec.md）

-->
