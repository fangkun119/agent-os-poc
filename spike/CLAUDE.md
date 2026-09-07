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
└── 008-memory-backend/
```

## 规则

- 每个 spike 是独立的 Maven 项目：自带 pom.xml，构建与测试都在 spike 目录内执行 `mvn`（不在仓库根执行）
- spike 的 pom.xml 永不加入根 pom.xml 的 `<modules>`（根 pom 模块固定为 agentos/ 下 9 个模块）
- 实验结束后在 README.md 写结论：验证了什么、结果如何、是否采纳（及对应正式实现的位置）
