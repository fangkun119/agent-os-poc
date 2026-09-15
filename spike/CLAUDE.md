# spike — 调研实验区

存放 spike（调研实验）：写正式代码前，用独立的小项目实际跑代码来验证技术方案。

## 命名与结构

每个 spike 一个子目录，命名格式：`NNN-短名`（3 位序号 + 连字符 + 小写英文短名，序号递增）。

```text
spike/
├── 007-react-loop/
│   ├── pom.xml
│   ├── src/
│   └── README.md      # 实验结论
└── 009-memory-backend/
```

## 规则

- 每个 spike 是独立的 Maven 项目：自带 pom.xml，构建与测试都在 spike 目录内执行 `mvn`（不在仓库根执行）
- spike 的 pom.xml 永不加入根 pom.xml 的 `<modules>`（根 pom 模块固定为 agentos/ 下 9 个模块）
- 实验结束后在 README.md 写结论：验证了什么、结果如何、是否采纳（及对应正式实现的位置）
- spike 结论回填设计文档时，回填**机制结论**（证明了什么方法可行），不回填**实验样本成员**（实验里恰好用了哪几个实例/哪几家厂商）——正式实现的成员集合是配置不是代码，样本只作实证引用（2026-09-15 用户裁定，源自 007 第二组 TS 3.2 回填）

## 编码规范（2026-09-15 起：spike 的 Java 代码同样遵守）

- 权威全文：`agentos/CLAUDE.md`（三源合并编码规范：禁 Lombok、SLF4J 日志、AssertJ 断言、Maven 版本仲裁唯一、异常与命名纪律等，含 [源:…] 回查锚与参数清单 P-01~P-12）。spike 子树的 Java 代码按同一套规范执行，本节只记 spike 特有的适配与豁免，规范全文不复制（单源维护）。
- 条款适配与豁免（均有规格出处）：
  - 「单测不依赖外部环境」：live 验证项按定义必须真实调用外部模型 API（mock 无法回答端点可达性、模型名接受度、凭证兼容性这类问题）；离线项（如容器启动、属性绑定）仍守此条
  - 「远程调用必须显式设超时」：spike 以 surefire `forkedProcessTimeoutInSeconds=180` 兜底替代（超时框架化属正式实现红线）；正式实现按 agentos/CLAUDE.md「配置纪律」章强制执行
  - 「主类 com.agentos.boot / 九模块 Maven 结构」：不适用于 spike（独立 Maven 工程、包名自定）；其余构建纪律（三方依赖不写版本号交 BOM 仲裁、parent 禁擅动、无同 GAV 双版本）照常生效
- spike 已踩过的规范实证（详见 spike/007-react-loop/README.md D6）：手动构造的 ChatModel 内置默认 RetryTemplate（10 次退避），不吃 `spring.ai.retry.*`——显式构造实例必须显式传受控 RetryTemplate
- 测试约定：断言一律 AssertJ；证据打印走 SLF4J（stdout 重定向即留档），禁 System.out 新增
