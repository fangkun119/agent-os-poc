# 给 OpenCode v2 装上 Java 代码智能：opencode-lsp 安装、卸载与可靠性手册（2026-10-12）

> 装完你会得到什么：
>
> OpenCode v2 里的 AI 助手多出 7 个 Java 语义工具——跳转定义、查引用、实时诊断、安全改名。此前它读 Java 工程只能按文本搜关键词，注释和同名符号会把结果搅浑。
>
> 安装约 20 分钟（大头是等 jdtls 首次预热），卸载约 2 分钟，不留残余。

本文只讲三件事：怎么装、怎么卸、凭什么信。背景调研（OpenCode v2 为什么没有 LSP、社区方案怎么选出来的）不在本文范围内，结论只在用到时一句话带过，不影响动手。

## 1. 它解决什么需求

一句话：**让 AI 助手按"符号"而不是"文本"理解 Java 代码。**

| 你让 AI 做的事 | 没装（只能文本搜索） | 装了（LSP 语义工具） |
|---|---|---|
| "这个方法在哪定义" | 搜方法名，命中一堆调用点和注释 | 返回准确定义位置 |
| "谁调用了这个方法" | 混入注释、字符串里的同名文本 | 只返回真实调用点 |
| "把这个方法改个名" | 改完靠编译报错兜底 | 改前就拿到完整影响面 |

一条边界先说清：LSP 诊断不等于构建结果。**代码对不对，最终以 `mvn compile` / `mvn test` 为准**；这套工具的价值是即时反馈和精确导航，两者互补。

术语备查：**LSP**（Language Server Protocol）是微软 2016 年定的协议，把代码智能从编辑器拆到独立进程；**jdtls**（Eclipse JDT Language Server）是 Java 的事实标准语言服务器，要求 JDK 21+；**MCP**（Model Context Protocol）是 AI 助手调用外部工具的开放协议——本文方案就是"挂一个 MCP 服务器去驱动 jdtls"。

## 2. 安装（约 20 分钟，六步）

前置检查（1 分钟，三条命令全过再往下走）：

```sh
node --version    # 需要 v24 及以上
java -version     # 需要 21 及以上
jdtls --help      # 需要有输出；没有就先 brew install jdtls
```

### 第 1 步：获取并构建（约 2 分钟）

```sh
git clone --depth 1 https://github.com/WhiteGiverMa/opencode-lsp
cd opencode-lsp && bun install --frozen-lockfile && bun run build
```

装完你会看到：`dist/` 目录出现。该项目没有预构建包，构建用的 bun 只是构建期需要，运行期零 npm 依赖。

### 第 2 步：复制到长期目录（约 1 分钟）

```sh
mkdir -p ~/.local/share/opencode-lsp
cp -R package.json bin dist vendor skills docs README.md README.en.md LICENSE NOTICE ~/.local/share/opencode-lsp/
```

关键：**必须放长期目录**。放进临时目录再配进正式配置，哪天清理临时文件，工具就静默失效。

### 第 3 步：写用户级语言服务器配置（约 1 分钟）

创建 `~/.config/opencode-lsp/lsp.json`：

```json
{
  "lsp": {
    "jdtls": {
      "command": ["/usr/local/bin/jdtls"]
    }
  }
}
```

两个要点：内置的 43 个服务器条目只给扩展名映射、**不给启动命令**，命令必须自己写；写绝对路径可以绕开"运行环境 PATH 不含 `/opt/homebrew/bin`"这类环境差异。

### 第 4 步：注册进 OpenCode（约 5 分钟）

编辑 `~/.config/opencode/opencode.json`，在 `mcp` 块里加 `servers.lsp`（保留既有条目，只增不改）。拿不准格式就先跑工具自带的打印命令，照抄合并：

```sh
node ~/.local/share/opencode-lsp/bin/opencode-lsp.js config v2
```

要合并进去的条目：

```json
{
  "mcp": {
    "servers": {
      "lsp": {
        "type": "local",
        "command": ["node", "/Users/you/.local/share/opencode-lsp/bin/opencode-lsp.js", "mcp"],
        "codemode": false
      }
    }
  }
}
```

同一次编辑建议顺手把 `"skills": ["/Users/you/.local/share/opencode-lsp/skills"]` 也加上——它带一个 `lsp-setup` 技能，AI 助手遇到 LSP 问题时会拿来自查。

### 第 5 步：doctor 验证服务器可发现（约 1 分钟）

```sh
node ~/.local/share/opencode-lsp/bin/opencode-lsp.js doctor
```

通过标准：输出里有一行 `jdtls: installed; source=user; extensions=.java`。`source=user` 说明读到的是你写的配置。

### 第 6 步：verify 跑一次真实诊断（约 10 分钟，大头是等预热）

```sh
node ~/.local/share/opencode-lsp/bin/opencode-lsp.js verify path/to/Some.java
```

第一次**必然超时**：`Timed out waiting for fresh diagnostics ... within 3000ms`。这不是坏了——jdtls 冷启动加 Maven 首次导入要几十秒，工具的新鲜度窗口只有 3 秒。此时 jdtls 进程已被拉起并在预热（后台守护进程会保住它）。

等半分钟，重跑同一条命令，得到 `"No diagnostics found"` 即为通过。工作区缓存在 `~/Library/Caches/jdtls/` 下按项目哈希分目录，不污染工程。

最后确认接入：

```sh
opencode mcp list    # 出现 ✓ lsp connected 即成功
```

**新开的** OpenCode 会话会带上 7 个工具：`lsp_status`、`lsp_diagnostics`、`lsp_goto_definition`、`lsp_find_references`、`lsp_symbols`、`lsp_prepare_rename`、`lsp_rename`。已经开着的会话看不到新工具——重启 OpenCode 即可。

## 3. 常见报错与修法（对号入座）

| 症状 | 原因 | 修法 |
|---|---|---|
| `Timed out waiting for fresh diagnostics ... 3000ms` | jdtls 冷启动超出 3 秒新鲜度窗口，正常现象 | 等半分钟重跑同一条命令；daemon 已常驻，之后秒回 |
| 新会话里没有 `lsp_*` 工具 | OpenCode 在会话启动时加载 MCP | 重启 OpenCode；用 `opencode mcp list` 确认 `lsp connected` |
| jdtls 拉起报 ENOENT（找不到命令） | 运行环境 PATH 不含 `/opt/homebrew/bin` | 配置里写绝对路径（如 `/usr/local/bin/jdtls`），或软链进 PATH 已有目录 |
| `requires Node.js 24 or newer` | Node 版本过低 | 升级 Node 后重跑 |
| `opencode mcp list` 里 lsp 显示 failed | 配置里的 node 或脚本路径写错 | 对照第 4 步打印的标准配置逐字核对绝对路径 |

## 4. 卸载（约 2 分钟，三步）

1. 编辑 `~/.config/opencode/opencode.json`，删两处：`mcp.servers.lsp` 整个条目、`skills` 数组里指向 `opencode-lsp/skills` 的那一行；
2. 删两个目录：`rm -rf ~/.local/share/opencode-lsp ~/.config/opencode-lsp`；
3. 重启 OpenCode，`opencode mcp list` 不再出现 `lsp` 即卸载完成。

可选清理：`~/Library/Caches/jdtls/` 下按项目哈希命名的工作区缓存。删之前注意其他 jdtls 使用方（比如编辑器插件）可能与它共用父目录，按需处理。

## 5. 可靠性：结论与依据

结论先行，三行：

- **引擎层：可信。** 7272 行 LSP 引擎逐字节搬运自 3760 star 项目的构建产物，搬运声明经 SHA256 校验属实；
- **外壳层：单人新作。** 自写代码约 770 行，故障模式是"工具不可用"，不是安全问题；
- **上游冻结。** 引擎取自 LTS 快照，上游修复不会流进来——能用就一直稳定，有 bug 依赖外壳作者修。

依据清单，每条附怎么自己复核：

| 依据 | 事实 | 怎么复核 |
|---|---|---|
| 溯源记录完整 | `provenance.json` 记录源仓库、commit、路径、SHA256 四要素 | `cat ~/.local/share/opencode-lsp/vendor/provenance.json` |
| 搬运未篡改 | 上游原文件哈希与声明一致（`e43097ef...3502d`） | 按 provenance 里的 commit 从 GitHub raw 拉原文件，`shasum -a 256` 比对 |
| 引擎血统 | 上游 lazycodex 3760 star；同一作者的 oh-my-openagent 69975 star | 打开两项目的 GitHub 页 |
| 无传递供应链 | package.json 无 `dependencies`，运行时只 import Node 内置模块 | `grep -oE 'from "[^"]+"' vendor/runtime.js \| sort -u` |
| 退场成本低 | 卸载 = 1 条配置 + 2 个目录 | 本文第 4 章照做 |

残余风险两条，都有兜底：上游快照冻结（接受现状，或届时换方案）；外壳单人维护（当试用用，不满意按第 4 章两分钟退场）。

时效声明：本文版本判断与 star 数均为 2026-10-12 快照。若 OpenCode 官方将来在 v2 正式版恢复内置 LSP（可跟踪官方仓库 issue #50916），按第 4 章卸载本方案即可干净切换。

## 6. 相关链接

官方与问题面：

- OpenCode v2 文档：<https://opencode.ai/v2/docs/>
- v1→v2 迁移指南（`lsp` 配置被接受但不运行的说明）：<https://opencode.ai/v2/docs/migrate-v1/>
- OpenCode 仓库：<https://github.com/anomalyco/opencode>
- LSP 缺位 issue：<https://github.com/anomalyco/opencode/issues/50916>
- 插件暴露 LSP 的请求：<https://github.com/anomalyco/opencode/issues/51416>

本方案与备选：

- 本文主角（MCP 路线）：<https://github.com/WhiteGiverMa/opencode-lsp>
- 备选 v2 原生插件：<https://github.com/christensenjairus/opencode-v2-lsp-support>
- Java 专用 MCP（备选）：<https://www.npmjs.com/package/@kealthas-dev/opencode-mcp-java-lsp>
- 通用代码智能工具包（重型备选）：<https://github.com/oraios/serena>

引擎上游与语言服务器：

- LSP 引擎源仓库：<https://github.com/code-yeongyu/lazycodex>
- 同作者旗舰项目：<https://github.com/code-yeongyu/oh-my-openagent>
- jdtls 官方仓库：<https://github.com/eclipse-jdtls/eclipse.jdt.ls>
- jdtls Homebrew 公式：<https://formulae.brew.sh/formula/jdtls>
- OpenCode v1 线（内置 LSP 的官方版本）：<https://www.npmjs.com/package/opencode-ai>

下一步：新开一个 OpenCode 会话，让它对任意一个 Java 方法跑 `lsp_goto_definition`，看着它精准跳过去，安装就算真正落地了。
