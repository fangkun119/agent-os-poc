<!-- 维护者注（Claude Code 注入前会剥离 HTML 注释；以下正文对 agent 可见）：
  本文件是「VitePress 1.x + Vue 3 <script setup> + 双 locale + 自定义主题」技术栈的 CLAUDE.md 通用模板。
  复用方法：拷入新项目站点根目录，按文末「站点参数清单」逐项核对替换，删除不适用的条件式条款。
  标记惯例：字面 token（配置键/字段/文件名/伪类）一律反引号。论证链见原仓库 chat/consolidate/ 系列。 -->

# 站点开发规范（VitePress · Vue 3 · 双语文档站）

适用范围：本目录下的 VitePress 站点源码。
技术栈：VitePress 1.x + Vue 3 `<script setup>` 自定义主题 + 双 locale（en 为 root、zh 为子目录）+ Makefile 工作流。
冲突裁决：本文件与通用风格指南、第三方规范（含 Vue/VitePress 官方文档的一般性建议）冲突时，以本文件为准；仓库级约定见根 CLAUDE.md，只引用不复述。
本文件是单语维护文档（中文），不做双语版本；站点内容的双语范围见「站点配置」首条。

## 通用工程纪律

- **外科手术式改动**：改动都应能对应到本次需求；与需求无关的代码不顺手重构、不顺手清理，每行待改动全部完成后，梳理并给出修改建议，排出优先级，交给开发者决定。
- **清理自己产生的垃圾**：自己改动产生的孤儿（如删了数据数组却留下对应样式）必须清干净；既有存量问题只指出、不擅自删改（批量清理需单独立项）。
- **代码注释**：写给读当前代码的人，禁止叙事变更历史（now/previously/原来/不再…）；注释必须陈述代码本身读不出的信息，否则不写；workaround 类注释注明成因与出处。
- **挂起决策不擅自落地**：标记为"待决策/待裁决"的开放问题，呈现选项而非默默选边；未决期间相关条款不得写出互相矛盾的表述。
- **零运行时外部请求**（自托管/私有部署定位）：
  - 字体、图标、图片一律站内自托管或内联；社交图标用内联 SVG，不用 iconify 类字符串图标名（字符串形态会在运行时请求图标 CDN）。
  - 新增外部 origin 先向用户提出并获确认，未获批前不引入。
  - 公有网部署的站点，本条降级为"运行时外链需显式豁免"。

## 构建与验证

- **make 单一入口**：构建/预览一律走 make 目标（dev/build/preview/install 等；目标名与 npm scripts 可能不同名）。新增 npm script 同步补 make 目标 + `##` 开头的用途注释 + `.PHONY` 声明。
- **验证定义**：`make build` 退出码 0 + `make preview` 目检改动页面。无测试套件时这是唯一完成标准；改配置类文件后必须核对构建产物里的最终效果。
- **指令文件命令红线**：凡写入指令类文件（本文件、README、AGENTS.md 等）的命令必须可安全重复执行——写进指令文件的命令会被 agent 当作可自动执行项；禁止写入 `make clean` 及任何 rm/覆盖类破坏性命令。
- **禁裸静态服务器**：cleanUrls 产物是无扩展名 URL，通用静态服务器路径解析错位——假预览等于假验证，预览只用 `make preview`。

## 站点配置与常量（.vitepress/）

- **双语范围**：新增内容先判断属于哪一侧——
  - 需要中英两份（面向访客）：页面组件文案（经 `t(zh, en)` 成对，见 Vue 组件节）、nav/sidebar 标签、文档页（`docs/` ↔ `zh/docs/` 成对）、访客可见 meta（description/og）。
  - 保持单语（勿造成对副本、勿翻译）：站内收录的文档镜像（保留来源语言原样）、代码与代码示例、代码注释、本文件与 README 等维护者文档（单语中文）。
- **双语三处同步**：文档页 en/zh 成对落地（`docs/x.md` ↔ `zh/docs/x.md` 同名 1:1），同一提交里同步 root 与 zh 两份 nav/sidebar（两侧分组与条目一一对应、数量相等）。一侧缺失不报错，只留线上 404。zh 页 title/正文用中文，技术术语保留英文原词。镜像页除外——镜像身份优先于页面类型，按上条保持来源语言。
- **nav/sidebar 字段**：以所用版本 `DefaultTheme.NavItem`/`SidebarItem` 类型定义为准，禁臆造字段（icon/badge/description 等会被静默丢弃）。
- **站内链接扩展名**：站内导航与普通页链接省略扩展名；站内收录的文档镜像互链保留 `.md` 是本类站点约定例外——两者均可解析，但不写明例外，agent 对照官方 best practice 批量"修正"镜像链接就是默认错误动作。`i18nRouting` 保持默认（语言切换按路径前缀镜像跳转；改 false 会让切换器 404）。
- **locale 级配置**：只允许九键——七键（`lang`/`dir`/`title`/`titleTemplate`/`description`/`head`/`themeConfig`）+ 语言切换器字段 `label`（必填）/`link`（可选），二者被切换菜单真实消费，删之即坏；其余站点级键（`base`/`cleanUrls`/`ignoreDeadLinks` 等）写 locale 层会被静默忽略。themeConfig 为浅合并——locale 里写数组是整体替换不是合并，nav/sidebar 两侧各写全量、勿写增量；不建目录级 config 覆盖文件，保持单一配置源。
- **默认主题文案本地化**：非英文 locale 的默认主题文案是英文硬编码，需在 locale 级 themeConfig 覆盖（`docFooter`、`outline.label`、`sidebarMenuLabel`、`returnToTopLabel` 等；这类英文界面源码里不可见，只能 build 后到产物页核验）。
- **public/ 资产引用**：Vue 组件内必须 `withBase('/images/…')`；config head 与 markdown 用根绝对路径（`/favicon.svg` 式）。禁止在 md 里用相对路径爬出 srcDir 指向 public/（rollup resolve 失败、build 直接挂）。改 `base` 时需全局复查根绝对路径与 CSS 内 url() 引用；withBase 写法随 base 自适应。
- **themeConfig 联动陷阱**（四个静默面，均可 diff 判定）：
  - `titleTemplate` 的 `:title` 取页面首个 h1——改首行 h1 会连带改 title 与 og:title；若首页以 `titleTemplate: false` 防品牌后缀叠加，勿移除。
  - 若 `siteTitle: false`（nav 只留 logo）系有意设置，补回文字即成双品牌，勿当缺陷"修复"；显示站名的站点删去本半句。
  - 单主题（`appearance: false`）下 logo 禁写 light/dark 双值死分支。
  - footer 仅在无 sidebar 的页面渲染——要页脚走主题插槽或自定义组件，勿配 `themeConfig.footer`。
- **死链豁免纪律**（若启用 `ignoreDeadLinks`）：它同时关闭站内死链检查——新增链接后必须逐条核实目标文件存在（如 ls/grep 对应路径；此检查已被豁免关闭，build 不会代报）；收窄时改用数组/正则只豁免确需豁免的模式，禁止为绕过报错扩大豁免、禁止回退成 `true` 一刀切。
- **站点常量单一来源**（`.vitepress/constants.ts`）：
  - 站点级常量（仓库地址、站名、tagline 等）集中定义、一律 import 使用，禁止在使用处写死字面量；md frontmatter 无法 import 常量，属人工同步副本，改动时对照同改。
  - 该文件保持零依赖（不 import vitepress/Node 内置/npm 包）——它同时被 Node 侧 config 与浏览器侧组件引用，任一侧依赖都会炸掉另一侧构建；确需依赖的共享逻辑另建模块，并在文件头注明适用侧。

## Vue 组件（.vitepress/theme/*.vue）

- **双语机制**：用户可见文案一律经 `t(zh, en)` 成对书写（zh 在前，对应 root=en 的约定）；随语言变化的数据数组一律包 `computed()` 依赖语言判定，不写普通 const——组件一旦常驻或被复用，普通 const 会固化首载语言。站内链接用 `t('/zh/docs/x', '/docs/x')` 成对书写并包 `withBase()`。
- **模板字符串转义（build 级红线）**：数据里的示例代码是 JS 模板字符串，shell/YAML 占位符必须写成 `\${VAR}` 反斜杠转义——未转义的 `${...}` 会在构建期被当作 JS 表达式求值，直接 build 失败；字符串内禁止未转义的反引号与代码围栏。
- **数据驱动段落**：段落文案与条目只改 script setup 顶部的数据数组，模板只做 v-for 渲染，禁止在 template 里写静态副本（两份真相）。组件内没有 markdown 渲染管线，代码示例只能是纯文本字符串进 pre/code，或手写 span+类名高亮。新增一个段落 = 数据数组 + section 模板 + scoped 样式三件套。v-for 必带 `:key`，字符串 key 注意数据重复时的唯一性。
- **script setup 形态红线**：顶层绑定直接用于模板，禁止 Options API / `setup()` return 写法；**禁止顶层 await**——会使组件变 async setup，而自定义 Layout 通常未包 Suspense，结果是页面静默整页空白且 dev/build 均不报错；异步取数走 onMounted。
- **组件契约**：自托管字体的站点，theme 入口必须用 `vitepress/theme-without-fonts`——默认入口会把 VitePress 自带字体打进产物，与自托管字体形成双份。组件文件名与全局注册点是硬依赖（theme 入口的 `app.component('Home', …)` 与 md 里的标签字符串），禁重命名组件文件、禁在注册点之外 glob 批量注册；官方 multi-word 组件名规则在此显式豁免。
- **组件语言与块形态**（纯 JS 站点；组件采用 TS 的项目删去本条）：组件为无 `lang="ts"` 的 `<script setup>`，组件内禁 TS 语法，TS 只出现在 config 与常量文件。每个 .vue 至多各一块 template / `<script setup>` / `<style scoped>`；如需另加普通 `<script>` 块，仅限官方三场景（script setup 表达不了的选项、命名导出、一次性副作用）。

## 样式与主题（theme/custom.css）

- **品牌色单一来源**：颜色一律从定稿色板取值；色板未定稿时以 `TODO(色板): 用途` 占位，禁止把现有色值当既定规范复用。定稿色板的权威出处由 `custom.css` `:root` 头注释指向，色板换版时同步更新指针。
  - 改色必须同步全部载体：全局 CSS 的 `:root` 变量、组件 scoped 内联色值、logo/favicon 的渐变 defs；示意图 SVG 的独立配色单独评估是否重出图。
  - 新增样式优先消费既有 CSS 变量，不新增内联色值副本。语义色分两类：固定语义色（终端红黄绿灯等）永不随色板联动；品牌语义色（`--aos-signal-*` 状态族）属色板组成部分，随定稿联动。
- **无条件深色画布**（`appearance: false` 的单主题站点）：新增样式按深底浅字假设编写。`.dark` 作用域变体是死代码（`appearance: false` 永不给 html 加 dark 类）；`prefers-color-scheme` 媒体查询会随用户系统深色模式真实生效、打破画布，同样禁止写。浅色背景仅限刻意反差段，且必须配套深色文字类。`appearance` 合法取值以所用版本实测为准（1.x 无 `'force-light'` 取值——写字符串会落入 truthy 分支变成跟随系统深色；参考文档可能滞后，勿据其反推在用配置非法）。
- **主题覆盖只写在全局 custom.css**：优先覆盖 `:root` 的 `--vp-*` 变量；必须覆盖 `.VP*` 内部类时用最窄选择器、注明压的是哪条内部规则。机制依据：scoped 样式打不进子组件内部（编译为 data-v 属性选择器只命中本组件模板；唯一例外是子组件根节点，它会同时受父组件 scoped 命中，属合法路径）——在组件 scoped 块里写主题覆盖会静默失效。代码块高亮配色的唯一落点是 config 的 `markdown.theme`，不走 CSS 覆盖。
- **覆盖增量纪律**：布局级定制先查主题 Layout 插槽与官方 CSS 变量族（`--vp-nav-*` 等），其次才是全局 CSS 新增选择器；禁止用 vite alias 替换内部组件；升级 VitePress 的提交必须 build + 目检 nav/sidebar/doc（内部类改名时覆盖静默失效无报错）。禁止把 backdrop-filter 挂在 nav 祖先级容器——会使 fixed 定位的移动端菜单塌陷，且有滚动性能代价；确需毛玻璃先在移动端实测菜单展开。
- **custom.css 增量纪律**：`!important` 只准 proactive（压第三方内部规则、保证必胜）不准 reactive（压自己的规则）；新增/覆盖均注明对象与被压的内部规则；新规则落入既有分节（盒线注释分隔的区块）或新开分节；禁新增 ID 选择器。
- **动效纪律**：所有动画/过渡必须包 `prefers-reduced-motion: reduce` 门控（门控下内容静态完整呈现，不得留半成品状态）；全站至多一个载入编排时刻（当前为 hero 终端逐行显现），禁 scroll-jack、滚动触发 reveal、视差；新增动效先在本条登记岗位。
- **可访问性地板**：文字对比度 ≥4.5:1（≥24px 或 ≥19px 粗体的大字 ≥3:1；装饰性元素豁免但须登记）；键盘焦点必须有可见样式（`:focus-visible`，深底描边 ≥2px 带 offset，反差底色区内改用底色描边）；图片 alt / 控件可达名不回退。
- **组件 scoped 纪律**：选择器首 token 必须是类（裸元素选择器叠加属性选择器后性能差数倍）；`:deep()` 是唯一穿透出口且须注明理由；禁用位置序数选择器（`nth-of-type` 等）定位内容段——与数据驱动 + v-for 互斥，插入条目即错位；模板禁写 `style="…"` 内联样式属性（同时绕开 scoped 隔离与色板审计），样式一律走 scoped 类。

## 文档内容（docs/ 与 zh/docs/）

- **内容规约**：围栏代码块必标语言（命令用 `bash`、输出清单用 `text`，直接决定高亮渲染）；每页正文唯一 H1 且与 frontmatter title 一致（站内收录的文档镜像与 layout:home 首页显式豁免）；图片 alt 写有意义描述；链接文本描述目标（禁 click here / learn more / 点击这里）。
- **默认纯 Markdown**：Markdown 做不到的效果才下沉 HTML/Vue 组件，且须能指出具体效果、当页配点名规则号的豁免注释；官方"md 里可用 Vue"是能力背书，不是默认使用理由。
- **中文排版**（zh 页面与组件中文文案）：中文与英文/数字之间加空格（度数与百分比紧跟数字属例外，防过度矫正）；全角标点后不加空格、不叠用；英文整句与行内代码保持半角。专名按官方拼写（如 GitHub、VitePress、CSS、YAML——小写回退即违规）；视觉全大写用 `text-transform` 实现，不把全大写字符串硬编码进源码。
- **语义断行（增量）**：新增或修订的段落一句一行（断在句末标点后，zh 侧到句号级）；存量段落不做全量重排（保护 diff 与 blame）；断行不得改变渲染结果。
- **示例占位符统一记法**：环境变量引用写 `${VAR}` 形态（组件内模板字符串按转义规则写 `\${VAR}`）；用户填入值统一 `your-xxx-here` 形态；不出现第三种记法。
- **内容保真**（站内收录权威文档/镜像的站点）：动笔或改写前先读权威来源对应章节；文档正文是对来源的摘要，每条实质性陈述（数字/类名/端点/机制）都应能在来源章节找到出处，找不到出处不写，禁止凭模型自身知识补写细节。镜像互链的链接形态见「站点配置」站内链接扩展名条。

## 速记约定

- 响应式样式复用既有断点档位，不另立新档。
- 本目录忽略规则以本目录 `.gitignore` 为唯一权威，新增产物目录就地补条目。
- config 里的既有键是有意设置，勿顺手改。
- 修改本文件：新增条目须可对照 diff 检验、能从代码推导的不写；仅在同类错误重复出现时增条，优先并入既有条目；保持 ≤200 行且无互相矛盾表述。

<!-- 维护者注（注入前剥离，仅人类可见）：
  · 元规范出处：Claude Code 官方 memory 文档——单文件 ≤200 行（超长降低遵从率）；矛盾条款会被随机执行其一，定期清查。
  · 勿再加清单（已论证否决，勿重新引入）：multi-word 组件名规则（已豁免）；Google "Avoid !important" 照抄版（与覆盖第三方内部类的现实冲突，可执行版已在正文）；CSS 内 v-bind()（绕过色板审计）；BEM/ITCSS/OOCSS（小组件站点过度工程）；官方"md 图片用相对 URL"（破坏 og:image 与 head 引用）；markdownlint 等工具链条款（未引入工具前写入即死配置）；Be consistent 类不可判表述；config 新增键门槛（裁量性半可检验）。
  · 站点参数清单（换项目时逐项核对，本清单为唯一权威）：
    1. locale 组合与目录名（en root + zh 子目录是本模板的书写前提）
    2. 部署形态：自托管/私有 vs 公有网——决定「零运行时外部请求」条是否降级为"外链需显式豁免"
    3. appearance 取值（深色画布条款的前提，本模板用 `false`；勿写 `'force-light'`，1.x 无此值）
    4. 是否自托管字体（决定 theme-without-fonts 条款）
    5. 是否收录文档镜像（决定双语范围与内容保真条款）
    6. siteTitle/nav 是否带站名；首页是否用 titleTemplate: false
    7. 组件语言 JS/TS（决定「组件语言与块形态」条去留）
    8. 构建入口是否 Makefile；组件内 i18n helper 名称（本模板写作 t）
    9. 断点档位；色板定稿状态 -->
