# 给 OpenCode v2 装回 Java 代码智能：一次 LSP 缺位的调研、选型与接入实录（2026-10-12）

> 读完你能带走什么：
>
> - 一套判断 AI 编码工具"有没有真 LSP"的三层证据法：查官方文档 → 查二进制字符串 → 查版本横向比对，三层都做才下结论
> - 一条在 OpenCode v2 上可复跑的 Java LSP 接入路线：独立 MCP 服务器 + 本机已有的 jdtls，六步装完、三个验证
> - 一份 v2 社区方案盘点表，以及"什么才算支持 v2"的判定标准——v1 和 v2 的插件 API 是破坏性变更，装错版本会直接加载失败
> - 一个核验"套壳项目"的溯源手法：读 provenance 清单，再把上游原文件拉下来做 SHA256 逐字节比对

本文素材来自 2026-10-12 的一次真实接入过程：给 OpenCode v2（一个 AI 编码终端工具）补上 Java 代码智能。目标是让 AI 助手能用"跳转到定义、查找引用"这类语义级操作读 Java 工程，而不是只会按文本搜关键词。所有命令都在 macOS（Apple Silicon）上实测，环境为 Node 25.9.0、bun 1.4.2、jdtls 1.61.0（Homebrew 安装）、JDK 21。

## 1. 问题全貌：编辑器都有的能力，AI 编码工具反而没有

结论先行：OpenCode v2 的正式版（截至 2026-10-12 的最新版 2.0.27）**完全没有 LSP 功能**——不是配置问题，是二进制里就没有这部分代码。v1 时代内置的 LSP 在 v2 重写时被整体移除，官方没有给出回归时间表。社区有补救方案，其中最稳的一条路是挂一个独立的 MCP 服务器来驱动语言服务器。

先解释两个名词：

- **LSP**（Language Server Protocol，语言服务器协议）：微软在 2016 年定的开放协议，把"代码智能"（跳转定义、查引用、重命名、实时诊断）从各家编辑器里拆出来，交给独立的**语言服务器**进程，编辑器通过协议与它对话。VS Code 的 Java 支持就是靠它实现的。
- **jdtls**（Eclipse JDT Language Server）：Java 语言的事实标准语言服务器，Eclipse 官方维护（开源仓库 [eclipse-jdtls/eclipse.jdt.ls](https://github.com/eclipse-jdtls/eclipse.jdt.ls)，2400+ star），要求 JDK 21 或更高版本运行。

为什么 AI 编码工具需要 LSP？对比两种查代码的方式：

| | 文本检索（Grep） | 语义检索（LSP） |
|---|---|---|
| 查"这个方法在哪定义" | 搜方法名，命中处可能全是调用点、注释、同名符号 | 精确返回定义位置 |
| 查"谁在调用这个方法" | 混入注释和字符串里的同名文本 | 只返回真实的调用点 |
| 改名是否安全 | 改完靠编译报错兜底 | 改前就知道影响面 |

对人类开发者，这是"IDE 与记事本"的差距；对 AI 助手，这是"每次改代码后要不要多跑一轮全量编译"的差距。文本检索的误报率在大工程里高到经常把 AI 带偏。

OpenCode 的版本背景：v1（npm 包 `opencode-ai`）内置 LSP，配置一个开关就能用，jdtls 都会自动下载；v2（npm 包 `@opencode/cli`）架构重写，LSP 没有跟过来。于是问题变成：**v2 用户想要 Java 代码智能，怎么办？**

## 2. 三层证据法：确认"v2 真的没有 LSP"

结论先行：官方文档、二进制检查、版本比对三层证据互相印证，v2.0.26 与 2.0.27 都不含 LSP 代码；dev 开发分支的相关代码也已在 2026-07-21 的结构重构中移除。"等下一个版本"在当时不是一个有依据的期待。

### 2.1 第一层：官方文档与 issue 面

v2 官方文档（[opencode.ai/v2/docs](https://opencode.ai/v2/docs/)）的配置页接受一个 `lsp` 配置字段，但 v1 到 v2 的迁移指南明确写了：v2 会**接受并保存** `lsp` 配置，但**不会运行语言服务器、不提供 LSP 工具、不产生诊断**。配置字段像一个留好的插座，墙里没有电线。

官方仓库的两个 issue 印证了这是已知缺口：

- [#50916 "LSP support gutted from v2?"](https://github.com/anomalyco/opencode/issues/50916)：2026-09-23 开，用户在催，维护者没有给出时间表；
- [#51416 "Expose the LSP runtime to plugins"](https://github.com/anomalyco/opencode/issues/51416)：请求把 LSP 能力开放给插件系统，同样 open。

### 2.2 第二层：直接检查二进制里的字符串

文档会滞后、issue 会没人回，最硬的证据是直接看装出来的二进制。OpenCode 是 Bun（一个 JavaScript 运行时）打包的**单文件可执行程序**，源码里的字符串字面量会原样留在文件里。任何 LSP 客户端实现都必然包含协议方法名前缀 `textDocument`（LSP 协议里"文本相关操作"的统一前缀，如 `textDocument/definition`）。任何 Java 支持都必然包含 `jdtls` 这个词。

检查只需要一段 Python：

```python
d = open('/usr/local/lib/node_modules/@opencode/cli/bin/opencode.exe', 'rb').read()
for n in [b'jdtls', b'textDocument', b'goToDefinition', b'pyright']:
    print(n.decode(), d.count(n))
```

v2.0.26 的结果：`jdtls` 0 处、`textDocument` 0 处、`goToDefinition` 0 处；作为对照，`pyright`（一个格式化/类型检查工具的名字，出现在格式化配置层）有 33 处。字符串字面量是可见的，所以"0 处"不是打包压缩造成的——**协议代码确实不存在**。

一个反例：配置层曾对 `$.lsp.jdtls` 发出过"malformed value"告警，容易让人以为"配置写对就能启用"。那是配置格式校验器在工作——**校验器认识这个字段，不代表运行时有消费者**。插座有孔，不代表墙里有电。

### 2.3 第三层：版本横向比对

只查自己装的版本可能撞上"旧版恰好没带"。于是对发布次日的新版 2.0.27 重复同样的检查：下载官方 npm 包的平台子包（`@opencode/cli-darwin-arm64`），对二进制数同样的字符串。结果同样是全 0。另查了 GitHub 上 dev 开发分支的提交历史：v1 代码所在的 `packages/opencode` 目录于 2026-07-21 被整体移除（结构重构），LSP 运行时没有在新结构中出现。

三层证据闭环，结论成立：**这不是"新功能在路上"，而是"功能被移除且没有回归承诺"。** 补救方案要到社区里找。

## 3. 社区方案盘点：什么才算"支持 v2"

结论先行：能搜到不少名字带 "lsp" 的 OpenCode 插件，但大多数是给 v1 内置 LSP 做"输出裁剪"的——v2 里根本没有内置 LSP，装了它们等于空转。真正合格的 v2 方案只有三类：v2 原生插件、独立 MCP 服务器、并排安装 v1。

### 3.1 判定标准：v1 和 v2 的插件 API 是破坏性变更

OpenCode v2 的破坏性变更清单里明确包含插件 API。v2 插件要求默认导出 `{ id, setup }` 形态，用 `ctx.tool.transform` 注册工具、`ctx.tool.hook` 挂钩子；v1 插件是另一种导出形态。把 v1 插件装进 v2 的实际后果不是"功能缺失"，而是**加载直接报错**（`Plugin must export a default definition with an id and an effect or setup function`——这是知名插件 oh-my-openagent 5.x 在 v2 上的真实报错，官方 issue 至今 open）。

所以每个候选都要验三样：发布/更新时间是否在 v2 发布之后、README 是否明确声明 v2 兼容、代码是否用 v2 的插件 API 或干脆与 OpenCode 版本解耦。

### 3.2 合格候选

| 方案 | 形态 | Java 支持 | 成熟度 | 关键短板 |
|---|---|---|---|---|
| [WhiteGiverMa/opencode-lsp](https://github.com/WhiteGiverMa/opencode-lsp) | 独立 MCP 服务器（stdio） | 内置 jdtls 条目，PATH 上有就能用 | 7 个工具：诊断/定义/引用/符号/重命名 | 0 star、未发 npm、需 Node 24+ |
| [christensenjairus/opencode-v2-lsp-support](https://github.com/christensenjairus/opencode-v2-lsp-support) | v2 原生插件 | 目录含 jdtls，但需手写启动命令 | 体验最接近内置（编辑后即时诊断） | 7 star、3 次提交、无真机集成测试 |
| [@kealthas-dev/opencode-mcp-java-lsp](https://www.npmjs.com/package/@kealthas-dev/opencode-mcp-java-lsp) | Java 专用 MCP（HTTP） | 对 jdtls 1.61.0 做过端到端真机验证 | npm 已发版 | 需常驻 HTTP 进程、配置较重 |
| [oraios/serena](https://github.com/oraios/serena)（30000+ star） | 通用代码智能 MCP 工具包 | 内部驱动 Eclipse JDT LS，要求 JDK 21+ | 最活跃、最大社区 | 是完整 IDE 式工具包，比纯 LSP 重得多 |
| 并排安装 [opencode-ai](https://www.npmjs.com/package/opencode-ai) v1.19.0 | 官方 v1 线 | v1 内置 LSP + jdtls 自动下载 | 官方维护、2026-10-11 仍在发版 | 与 v2 抢同一个 `opencode` 命令名，需隔离安装 |

先解释 **MCP**（Model Context Protocol）：一个开放协议，让 AI 助手以统一方式调用外部工具服务器。对 OpenCode 来说，MCP 服务器是配置项而非插件——这带来一个关键性质：**与 OpenCode 版本彻底解耦**。v2 将来升到 2.1、3.0，只要 MCP 协议还在，这个服务器就继续能用。

### 3.3 排除清单（每条一句理由）

- **oh-my-openagent 5.x**（含一整套 LSP 子系统）：插件在 v2 上加载即报错，且项目已迁出 OpenCode 插件线；
- **oh-my-opencode-slim**：v2 兼容没问题，但通篇没有 LSP 实现——README 里的 "LSP tools" 是从上游抄来的残留文案；
- **opencode-compact-lsp / opencode-edit-diagnostics-pruner 等一族**：全是给 v1 内置 LSP 工具做输出裁剪的，v2 没有内置 LSP，装了空转；
- **[isaacphi/mcp-language-server](https://github.com/isaacphi/mcp-language-server)**：思路相同的通用 LSP-MCP 桥，但已停更约 16 个月；npm 上同名包是无关的抢注包，真身在 GitHub。

### 3.4 选型

选了 **WhiteGiverMa/opencode-lsp**（MCP 路线），理由按权重排：

1. **版本解耦**——不赌 OpenCode 的插件 API 演进；
2. **jdtls 条目内置**——本机已装 jdtls 1.61.0，配置一行就能接上；
3. **stdio 传输**——由 OpenCode 按需拉起进程，不需要常驻服务；
4. 代价：项目很新（0 star、单人维护），当试用而非依赖，验收手段见第 4、5 章。

## 4. 接入实录：六步装完

结论先行：从克隆仓库到 `opencode mcp list` 显示 `lsp connected`，六步；其中唯一的坑是 jdtls 冷启动超过工具默认的 3 秒新鲜度窗口，重试即过。

### 4.1 前置条件

- Node.js 24+（`node --version` 确认）；
- JDK 21+（jdtls 的硬要求，`java -version` 确认）；
- jdtls 本体：macOS 上 `brew install jdtls`（[Homebrew 公式](https://formulae.brew.sh/formula/jdtls)），装完在 `/opt/homebrew/bin/jdtls`。

环境细节：如果你的 AI 助手运行环境 PATH 不含 `/opt/homebrew/bin`（macOS 上 Homebrew 的 Apple Silicon 前缀），语言服务器按名字拉起会 ENOENT。解法是软链进 PATH 里已有的目录：`ln -s /opt/homebrew/bin/jdtls /usr/local/bin/jdtls`——或者更干脆，后面配置里直接写绝对路径，绕开 PATH 问题。

### 4.2 获取与构建

该项目没有发布 npm 包、也没有预构建产物，需要源码构建一次（构建工具 bun 仅构建期需要，运行期零依赖）：

```sh
git clone --depth 1 https://github.com/WhiteGiverMa/opencode-lsp
cd opencode-lsp && bun install --frozen-lockfile && bun run build
```

构建产物是 `dist/` 目录，出现该目录即构建完成。

然后把 `package.json`、`bin/`、`dist/`、`vendor/`、`skills/` 等复制到一个**长期保留的目录**（本例用 `~/.local/share/opencode-lsp`）。不要放在临时目录里再配进正式配置，否则哪天清理临时文件，工具就静默失效。

### 4.3 用户级语言服务器配置

该工具自带 43 种语言服务器的内置条目，但**内置条目只给扩展名映射，不给可执行命令**——命令要在用户配置里自己写。文件位置 `~/.config/opencode-lsp/lsp.json`：

```json
{
  "lsp": {
    "jdtls": {
      "command": ["/usr/local/bin/jdtls"]
    }
  }
}
```

### 4.4 注册进 OpenCode

在 OpenCode 全局配置（`~/.config/opencode/opencode.json`）的 `mcp` 块里加一个服务器。可以先用工具自带的命令打印标准配置，照抄合并（它只打印、不改文件）：

```sh
node ~/.local/share/opencode-lsp/bin/opencode-lsp.js config v2
```

合并后的条目长这样（`codemode: false` 的意思是不把工具暴露给代码执行模式，直接作为独立工具列出）：

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

注意合并时**保留配置里的其他既有条目**（模型、其他 MCP 服务器等），只增不改。

### 4.5 验证一：doctor 看服务器可发现性

```sh
node ~/.local/share/opencode-lsp/bin/opencode-lsp.js doctor
```

输出里应有一行：`jdtls: installed; source=user; extensions=.java`。`source=user` 说明读到的是你写的用户配置而不是内置默认。

### 4.6 验证二：verify 跑真实诊断

```sh
node ~/.local/share/opencode-lsp/bin/opencode-lsp.js verify path/to/Some.java
```

第一次大概率超时，报 `Timed out waiting for fresh diagnostics ... within 3000ms`。原因是 jdtls 冷启动加 Maven 工程首次导入要几十秒，而工具的新鲜度窗口只有 3 秒；超时的同时，jdtls 进程已经由该工具的常驻机制（daemon，后台守护进程）拉起并在预热。**等半分钟重跑同一条命令**，得到 `"No diagnostics found"`（该文件零问题）即为通过。

工作区缓存由工具自动管理（macOS 上在 `~/Library/Caches/jdtls/` 下按项目哈希分目录），不污染工程目录。

### 4.7 验证三：OpenCode 侧接入确认

```sh
opencode mcp list
```

输出中出现 `✓ lsp connected` 即接入成功。此后**新开的** OpenCode 会话会带上 7 个工具：`lsp_status`、`lsp_diagnostics`、`lsp_goto_definition`、`lsp_find_references`、`lsp_symbols`、`lsp_prepare_rename`、`lsp_rename`。已经在跑的会话看不到新工具，重启 OpenCode 即可。

最后一个使用习惯：LSP 诊断不是构建结果。**代码正确性的最终判定永远是 `mvn compile` / `mvn test` 这类真实构建**；LSP 的价值在于改代码后的即时反馈和符号级导航，两者是互补而非替代。

## 5. 供应链信任：这个"套壳项目"能不能信

结论先行：opencode-lsp 的代码 90% 是搬运的——7272 行 LSP 引擎来自另一个项目的构建产物，770 行是作者自写的外壳。搬运有完整的溯源记录，且经我们逐字节校验属实；引擎的上游是 7 万 star 项目的一部分。风险主要剩"上游快照冻结"这一条。

### 5.1 代码构成

| 部分 | 规模 | 内容 |
|---|---|---|
| `vendor/runtime.js` | 7272 行 | LSP 引擎：自实现的 LSP 协议客户端（JSON-RPC 编解码、stdio 传输、进程管理、后台守护进程） |
| `src/*.ts` | 约 770 行 | 外壳：CLI 入口、MCP 协议封装、配置加载、TypeScript 诊断粘合 |

运行时依赖为零——只 import Node.js 内置模块（`child_process`、`crypto`、`fs`、`net` 等）。语言服务器不是它"调用某个库"实现的，而是自己 spawn 子进程、自己说 LSP 协议。

### 5.2 溯源与逐字节校验

`vendor/provenance.json` 记录了四个要素：源仓库（[code-yeongyu/lazycodex](https://github.com/code-yeongyu/lazycodex)）、源 commit、源文件路径、以及**源文件的 SHA256**。搬运声明是否属实，验证只要三步：

```sh
# 1. 从溯源记录里取 commit 和路径，拉上游原文件
curl -sL "https://raw.githubusercontent.com/code-yeongyu/lazycodex/<commit>/plugins/omo/components/lsp-daemon/dist/cli.js" -o upstream.js
# 2. 算哈希
shasum -a 256 upstream.js
# 3. 与 provenance.json 里的 sha256 比对
```

实测结果：两个哈希逐字节一致（`e43097ef...3502d`）。搬运没有夹带修改，你运行的就是上游那份代码。**凡是"搬运/打包/套壳"型项目，provenance 清单 + 哈希比对是成本最低的信任建立方式**——比读源码便宜几个数量级，比"看 README 说得挺好"可靠得多。

### 5.3 作者背书与残余风险

上游 lazycodex 仓库 3760 star、MIT 协议、当天还在提交；其作者的主力项目 [oh-my-openagent](https://github.com/code-yeongyu/oh-my-openagent) 有 69975 star。引擎代码出自一个经过大规模用户锤炼的子系统。

残余风险两条，都已知可控：

1. **上游快照冻结**——搬运的是 LTS 快照，上游后续修复不会流进来；引擎有 bug 只能靠 opencode-lsp 作者自己修；
2. **外壳单人维护**——770 行自写代码的作者就是那个 0 star 仓库的主人；但外壳的故障模式是"工具不可用"，不是安全问题，卸载也只是删一条 MCP 配置加两个目录。

## 6. 收尾：学到什么、边界在哪

三件可以迁移到别的场景的事：

1. **证据分层**："这个工具有没有 X 功能"这类问题，文档只能说"宣称"，二进制和代码才说"事实"。查文档 → 查产物 → 查版本横比，三层闭环再下结论，能省掉大量基于过时信息的试错。
2. **版本解耦优先**：给快速演进的工具（这里是 OpenCode v2）补能力时，优先选与宿主版本解耦的方案（MCP、独立进程、命令行），其次才是宿主的原生插件 API——插件 API 正是重写期最容易破坏的层面。
3. **套壳不可耻，无溯源才可耻**：复用经过验证的上游是工程美德，前提是溯源记录完整、且你能用哈希校验它。反过来，遇到没有 provenance 的搬运型项目，默认按不可信处理。

边界与时效：本文所有版本判断截至 2026-10-12。如果 OpenCode 官方未来在 v2 正式版恢复内置 LSP（可跟踪 [issue #50916](https://github.com/anomalyco/opencode/issues/50916)），本文的接入路线会被内置方案自然替代。届时移除 `mcp.servers.lsp` 一条配置即可干净退场——这正是选版本解耦方案的好处。另注：本文调研范围内的 star 数、issue 状态均为 2026-10-12 快照，会随时间变化。

## 7. 参考资料

**本文方案与备选**

- 本文采用的接入方案：<https://github.com/WhiteGiverMa/opencode-lsp>（含 provenance 清单与自述文档）
- 备选 v2 原生插件：<https://github.com/christensenjairus/opencode-v2-lsp-support>
- 重型备选（通用代码智能 MCP 工具包）：<https://github.com/oraios/serena>
- OpenCode v1 线（内置 LSP 的官方版本，npm 包）：<https://www.npmjs.com/package/opencode-ai>

**OpenCode 官方资源与 issue**

- OpenCode v2 官方文档：<https://opencode.ai/v2/docs/>；v1→v2 迁移指南（含 `lsp` 配置被接受但不运行的说明）：<https://opencode.ai/v2/docs/migrate-v1/>
- OpenCode 仓库（v2）：<https://github.com/anomalyco/opencode>；LSP 缺位 issue：[#50916](https://github.com/anomalyco/opencode/issues/50916)、插件暴露 LSP 的请求：[#51416](https://github.com/anomalyco/opencode/issues/51416)

**上游项目与基础工具**

- LSP 引擎的上游项目：<https://github.com/code-yeongyu/lazycodex>；同一作者的旗舰项目：<https://github.com/code-yeongyu/oh-my-openagent>
- jdtls（Eclipse JDT Language Server）：<https://github.com/eclipse-jdtls/eclipse.jdt.ls>；Homebrew 公式：<https://formulae.brew.sh/formula/jdtls>
