# 一个提交,从零交付双语官网:VitePress 站点变更精讲

## 1. 变更全貌:Java 项目为什么要一张网页门面

AgentOS 是一个 Java 写的 Agent 运行时内核,代码、文档都在仓库里,但对外没有门面:潜在用户搜到仓库,看到的是一堵 README 墙。本次唯一提交 [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) 补上这张门面——单页官网主页,中英双语,深色「机房 LED」视觉,单页八个板块,外加站点自己的开发规范文档。

先解释主角。**VitePress** 是一个静态站点生成器:你在本地写 Markdown 与 Vue 组件,执行一条构建命令,它把所有内容提前「烤」成纯 HTML/CSS/JS 文件;部署时把这摞文件往任何静态托管一放,服务器不需要运行任何后端代码。类比中央厨房:面包在厨房烤好再配送,门店只负责摆出来,不现场开炉。

你可能会问:一个 Java 项目,为什么官网要用 JS 生态的工具?因为官网是内容站,不是应用。它和 Java 内核零耦合——整个前端设施就是一个 `package.json`(仅 1 个依赖 vitepress)加一个 Makefile;构建产物是纯静态文件,可以直接挂在 GitHub Pages 这类免费托管上。用 Java 渲染页面反而要引入模板引擎、常驻进程与运维,得不偿失。

本次变更合计 35 个文件、+4851/-369 行,全部落在 `website/` 目录,按功能分组如下(提交池仅此一个提交;仓库中其他未提交改动按约定不纳入本篇):

| 分组 | 提交 | 文件(链接直达该提交对该文件的 diff) | 规模 | 深度 |
| --- | --- | --- | --- | --- |
| 站点骨架 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | [config.mts](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/config.mts) +90、[constants.ts](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/constants.ts) +11、[index.md](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/index.md) +18、[zh/index.md](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/zh/index.md) +18、[package.json](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/package.json) +14、[.gitignore](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.gitignore) +4、[Makefile](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/Makefile) +21 | +176 | 核心深讲(第 2 章) |
| 自定义主题 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | [theme/index.ts](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/index.ts) +15、[custom.css](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/custom.css) +166、[Layout.vue](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/components/Layout.vue) +15 | +196 | 核心深讲(第 3 章) |
| 主页组件 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | [Home.vue](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/components/Home.vue) +1225 | +1225 | 核心深讲(第 4 章) |
| 品牌资产 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | 双语架构图、og 分享图、favicon、logo、自托管字体 | 见第 5 章 | 次要合讲(第 5 章) |
| 旧占位图清退 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | 删除 12 个旧 SVG | -369 | 次要合讲(第 5 章) |
| 治理文档 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | [website/CLAUDE.md](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/CLAUDE.md) +102、[spec plan.md](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/spec/20260830-001-create-website/plan.md) +332 | +434 | 次要合讲(第 5 章) |
| 生成物 | [6c168a7](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99) | package-lock.json +2561 | +2561 | 噪音,略过(理由见第 7 章) |
| 合计 | 1 个提交 | 35 文件 | +4851 / -369 | — |

本提交未触及本 skill 输出目录 chat/,无目录噪音需剔除。

## 2. 站点骨架:一个配置文件定下的六件事

### 2.1 双 locale:同一家店的两块门牌

官网要服务中英两类访客。最直觉的做法是建两个独立站点,各自维护;这个提交没有这么做,而是在 [config.mts](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/config.mts) 里用一套配置声明两个 **locale**(语言区域:同一站点的一种语言版本):

```ts
locales: {
  root: { label: 'English', lang: 'en-US', ... },   // 英文住根路径 /
  zh:   { label: '中文',    lang: 'zh-CN', link: '/zh/', ... }, // 中文住 /zh/
}
```

为什么不做两个独立站?因为 VitePress 的 locale 是「同一家店的两块门牌」:厨房(构建管线、主题、组件)只有一套,门牌(路径前缀、语言标签、导航文案)各挂各的。英文访客走 `/`,中文访客走 `/zh/`,右上角语言切换器由框架自动生成并按路径前缀互跳。两套独立站点则意味着两份配置、两份依赖、两套构建,切换器还得手写。

配置里有两条值得记住的纪律,都以注释形式写进了 diff:

- locale 级只允许七键(`lang`/`dir`/`title`/`titleTemplate`/`description`/`head`/`themeConfig`)加语言切换器字段;`base`、`cleanUrls` 这类站点级键误写进 locale 层会被**静默忽略**——不报错、不生效,是最难查的一类配置错误。
- 中文 locale 需要手动覆盖默认主题的界面文案(「上一篇/下一篇」「本页目录」等)。这些文案在源码里不可见——它们长在 VitePress 内部,只有构建后在产物页才能核验。

### 2.2 base:住在子路径里的站点

config 第 22 行一行字决定全站资源路径:`base: '/agent-os-poc/'`。**base** 指站点部署在域名下的哪条子路径。GitHub Pages 给仓库站的 URL 形如 `用户名.github.io/仓库名/`,即站点不住在域名根部,而住在一个「文件夹」里;所有资源引用都必须带上这个前缀,否则上线即 404。

麻烦在于,前缀不是一处配置就完事。diff 注释点名了三处联动,各有各的坑:

| 位置 | 坑 | 该提交的解法 |
| --- | --- | --- |
| favicon(head 里的根绝对路径) | VitePress 不对 head 内路径做 base 重写,`/favicon.svg` 上线后指向域名根,404 | 唯一例外地硬编码 `/agent-os-poc/favicon.svg`,注释标注「改 base 时本行同步改」 |
| og:image 分享图 | 协议要求绝对 URL(带域名),框架无法代劳 | `${SITE_URL}/agent-os-poc/images/og.png` 拼完整地址 |
| sitemap hostname | 1.6.4 版生成站点地图只拼 hostname 加页面相对路径,不带 base 且缺尾斜杠会多一次 301 跳转 | hostname 写成 `${SITE_URL}/agent-os-poc/`,自带 base 与尾斜杠 |

**sitemap**(站点地图)是给搜索引擎看的全站 URL 清单;**301** 是「永久搬家」重定向,多一次意味着搜索引擎多绕一跳。三处坑的共同教训:base 类配置不是改一个字符串,而是改一组约定,每一处都要留注释说明成因。

### 2.3 srcExclude 与 head:不发布的与要补写的

同文件里还有两类容易忽略的配置。其一是 `srcExclude: ['spec/**', 'CLAUDE.md']`:VitePress 默认把源码目录里所有 Markdown 都当页面发布,站点的实施计划与开发规范属于内部文档,漏排的结果是内部文件变成公开网页。其二是 `head` 数组:这里补写搜索引擎与社交平台用的 meta 标签,其中 **og meta**(Open Graph 协议)决定链接被分享到聊天工具、社交平台时显示的标题、描述与预览图。该提交把 og 的 title/description/image 按语言分写进两个 locale,中文访客分享出去看到中文卡片——双语不止翻译正文,访客可见的 meta 同属双语范围。注释同时警告:`mergeHead` 按「标签名加属性」去重,同一个 og 标签不能站点级与 locale 级并存,写了后者会静默覆盖前者。

### 2.4 constants.ts:常量的单一来源

[constants.ts](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/constants.ts) 只有 11 行,导出站名、仓库地址、部署根、双语副标语五个常量。它的关键约束写在头注释:**保持零依赖**——这个文件同时被 Node 侧的 config 和浏览器侧的组件 import,任何一侧引入了依赖,另一侧的构建就会炸。这是「共享文件保持零依赖」的典型场景:一个文件被两个运行环境引用,它自己就必须哪边都不偏袒。config 与组件一律 import 使用,禁止在使用处写死字面量,站名改一处即全站生效。

### 2.5 主页壳:index.md 的两行关键 frontmatter

两个主页文件(`index.md` 与 `zh/index.md`)各 18 行,逐行对称,内容是一个 frontmatter 加一个 `<Home />` 标签——**frontmatter**(文件头部的 YAML 配置块)里两行决定了主页能不能全宽铺开:

- `titleTemplate: false`:站点级配置了 `titleTemplate: ':title — AgentOS'`,会给每页标题追加站名后缀;主页标题已含站名,不关掉就会变成「AgentOS — Enterprise Agent OS — AgentOS」的品牌叠加。
- `markdownStyles: false`:VitePress 默认给每页包一层文档容器(最大宽度 1280px、内边距、标题边线),主页是整页自绘组件,要的是铺满全屏,所以显式关掉这层容器及其级联样式。

### 2.6 Makefile 与 .nojekyll:构建入口的一个隐藏动作

[Makefile](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/Makefile) 提供 install/dev/build/preview 四个目标,注释声明「构建/预览一律走 make 目标」——命令入口单一,agent 与人都不会各跑各的。真正值得注意的是 build 目标末尾的 `touch .vitepress/dist/.nojekyll`。

你可能会问:构建完为什么还要塞一个空文件?GitHub Pages 默认会请一位叫 Jekyll 的「老管家」先翻检你交付的行李,下划线开头的目录会被它扣下不入库,而 VitePress 的产物恰好放在 `assets/` 这类路径下——缺了这个名为 `.nojekyll` 的免检标志,上线全站 404。另外 VitePress 的 `copyPublicDir` 不拷贝点开头的文件,所以这个文件不能放 `public/`,只能在构建后补进产物目录。一个字符都没有的文件,救的是整站。

骨架组还剩两个标准件没有专门讲:[package.json](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/package.json) 是 npm 清单,单依赖 vitepress 加 dev/build/preview 三个脚本,第 1 章已带过;[.gitignore](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.gitignore) 四行,忽略 node_modules、构建缓存与产物目录,属通用约定,不再展开。

## 3. 主题换皮:166 行 CSS 接管默认主题

### 3.1 主题入口:theme-without-fonts 与薄封装 Layout

VitePress 默认自带一套「文档站脸」。要用品牌脸,标准做法是**扩展主题**:在 [theme/index.ts](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/index.ts) 里 `extends` 默认主题,保留它的导航、语言切换等能力,再替换布局与样式。这个文件藏着两个决策:

- 入口用的是 `vitepress/theme-without-fonts` 而非默认入口。默认入口会把 VitePress 自带的字体文件打进构建产物;本站自托管字体,不排除就出现「两套字体都进包」,白占体积。
- 全局注册 `app.component('Home', Home)`,让 Markdown 文件里能直接写 `<Home />` 标签。注释声明组件文件名与注册名是硬依赖,与 index.md 里的标签字符串一一对应,禁重命名。

[Layout.vue](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/components/Layout.vue) 只有 15 行,是对默认布局的薄封装:主页之外的页面(未来的文档页)用空插槽压掉默认底部内容,主页不受影响。先立扩展点、不写实现,是为后续文档页预留的接口。

### 3.2 色板变量:全站颜料只有一个总闸

[custom.css](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/custom.css) 的主体是 **CSS 变量**声明——把颜色、字号定义成 `--aos-*` 命名的变量,组件一律用 `var()` 取值,不许写死色值。类比:全站颜料都从同一个调色台领,换配色只动调色台,不需要挨个房间重新刷墙。

变量分三层,各有分工:

| 层 | 代表变量 | 作用 |
| --- | --- | --- |
| 画布与面板 | `--aos-bg` `--aos-panel-1/2` | 绿黑底色系,页面底、卡片底、hover 底 |
| 品牌强调色 | `--aos-accent` 系(#2fe07a 绿) | 注释明文限定五类岗位(CTA 描边、终端符号、标题第二行、数据读出、交互点睛),并写明「绿色忌大面积铺底」 |
| 语义色 | `--aos-signal-*` / `--aos-dot-*` | 前者是品牌语义色随色板联动;后者是 macOS 终端红黄绿三点,「永不随色板联动」 |

最后一行的区分很有实用价值:终端窗的三个圆点是全世界 mac 用户都认识的固定符号,属于「物理常数」,配色改版不该把它们刷成品牌色;而「绿色表示健康」是品牌语义,必须跟色板走。混为一谈的后果是某次换色把固定符号也换了,用户认知错乱。

字号同理收敛为具名字阶(`--aos-fs-display-1` 到 `--aos-fs-data` 七档,大标题用 `clamp()` 随视口缩放),注释禁止组件内散点自定义字号——字号和颜色一样,闸门只留一个。

### 3.3 --vp-* 品牌映射:让框架内部跟着换装

主题里最「四两拨千斤」的一段,是把品牌变量接到 VitePress 自己的变量上:

```css
--vp-c-brand-1: var(--aos-accent-dim);
--vp-c-bg: var(--aos-bg);
--vp-c-text-1: var(--aos-text-1);
```

VitePress 内部组件(导航、链接、下拉菜单)不认你的 `--aos-*`,它们只读自己的 `--vp-*` 系列变量。把 `--vp-*` 指向 `--aos-*`,框架内部 UI 就整体换装,一行 VitePress 源码都不用改。类比:酒店房间里的电器只认墙上的标准插座,你不用改装电器,换个插座转接头就通了。

diff 里有一处典型修复:`--vp-c-bg-elv`(浮起面板底色)漏映射时保持默认白色,于是「深底浅字」的文字落进白底下拉菜单,对比度直接失效不可读。这类变量的存在只有出问题时才被注意到,注释把「漏映射会怎样」写明,给后来者省一次排查。

### 3.4 appearance: false、最窄覆盖与可访问性地板

本站是单主题站点:`appearance: false` 关掉明暗切换,全站恒深色。为什么不做亮色主题?「机房 LED」的视觉定位就是恒深画布,做双主题等于两套配色双倍维护;而 `false` 让切换按钮原生不渲染,比「渲染出来再藏起来」干净。注释还留了一条实测结论:该版本(1.6.4)没有 `'force-light'` 取值,写了这个不存在的字符串会落入 truthy 分支,退化成「跟随系统深色」——文档可能滞后,版本实测才算数。

主题覆盖纪律在这个文件里执行得很严格。这里要先补一个概念:**scoped 样式**是 Vue 组件的样式隔离机制,编译时给选择器追加本组件专属的属性标记,保证样式只命中本组件模板。代价是它进不了别人家——子组件内部的元素打不上这个标记,所以在自己组件的 scoped 块里写「覆盖 VitePress 内部类」的规则会静默失效。因此主题覆盖只能写全局 custom.css,且每条覆盖都要注明压的是哪条内部规则、用最窄选择器。diff 里三处示范:`.VPFooter` 整体隐藏(页脚由 Home.vue 自绘,防未来误配出双页脚)、`.VPHome` 清零默认底部留白(消灭页脚后的死滚动区)、`.VPPage` 等容器背景压实底(防换变量时主页边缘露白)。

文件末尾是可访问性地板:**`:focus-visible`**(键盘聚焦时才显示的焦点环)统一为品牌色描边,保证纯键盘用户知道自己在哪;`@media (prefers-reduced-motion: reduce)` 一条媒体查询全站关停动画与过渡——访客系统里开了「减少动效」,页面就静态完整呈现,不留半成品状态。

## 4. Home.vue:1225 行的单组件主页

### 4.1 数据驱动三件套:页面即数据

主页八个板块全部塞进 [Home.vue](https://github.com/fangkun119/agent-os-poc/commit/6c168a748da3c537c9231717385cfe4ad36d7e99/website/.vitepress/theme/components/Home.vue) 一个组件:约 280 行 script(数据与逻辑)、260 行 template(结构)、680 行 scoped 样式。为什么单组件而不拆八个?因为单页站点的板块间没有复用关系,拆件只会增加文件跳转;真正的维护成本在文案,于是组件确立了「数据驱动三件套」:文案与条目全部定义在 script 顶部的数据数组里,模板只做 `v-for` 循环渲染,样式按类名承接。改一句产品文案,只动数组,不碰模板——注释原话是「段落/条目只改本文件顶部数据数组,模板只做 v-for 渲染」。

以能力板块为例,数组每项是一张卡:文件树符号、条目名、标题、描述、代码示例、出处链接。模板里对应一段十行的 `v-for`。新增一张卡等于往数组加一项,模板与样式零改动。

### 4.2 t() 双语助手:computed 与「拍立得」

双语机制核心是一个三行助手:

```js
const { lang } = useData()
const isZh = computed(() => lang.value === 'zh-CN')
const t = (zh, en) => (isZh.value ? zh : en)
```

`useData()` 是 VitePress 提供的运行时数据入口,这里取当前 locale 的语言标签。所有用户可见文案写成 `t('中文', 'English')`,中文在前。stats、capabilities、heroLines 这类「随语言变化的数组」则统一包进 **computed**(计算属性:依赖变化时自动重算的派生值)。

为什么不直接写普通常量?打个比方,常量是拍立得,按下快门那刻的画面永久固定;computed 是监控画面,镜头前的内容变了画面跟着变。用户切换语言时组件不会重建,普通常量数组就固化在首载语言上,界面出现「中文导航配英文卡片」的裂缝。diff 注释把这条教训写成了红线:「普通 const 会固化首载语言」。同一个道理还有一处变体:`docRefs` 因为内部要调 `t()`,也必须保持函数形态每次调用时求值,不能缓存成常量。

### 4.3 docRefs:页面上每个数字都有出处

这个站点有一条内容纪律:页面上的每个数字与机制表述,必须能追溯到仓库权威文档。代码把纪律做成了机制——`docRefs(['DA 5.2', 'TS 11.1'])` 把「需求文档第 5.2 节」这类引用翻译成指向仓库文档对应文件的链接,渲染成每张卡片角落的「⟶ 溯源」行。访客点开能力卡,能直接跳到支撑这句话的文档章节。规划文档(plan.md)里配套一张「内容事实基线」表,列出页面允许出现的全部数字;页面侧再由 docRefs 把出处钉在文案旁边。宣传页最常见的问题是数字口径失控,这套「基线表加出处链接」的组合拳把口径锁在了代码里。

### 4.4 终端 hero:唯一编排时刻与动效门控

首屏是一扇仿终端窗,逐行「敲」出 `agentos init` 到记忆写入的完整演示。渲染同样是数据驱动:`heroLines` 数组每行带一个 `type`(`cmd`/`ok`/`user`/`agent`/`cursor`/`spacer`),模板按 type 分支渲染成命令行、确认行、光标等形态。

动画只有一处,注释称「唯一编排时刻」:页面加载后终端逐行显现,总时长控制在 600ms 内。实现方式值得细看——逐行延迟各不相同,但模板里禁止写内联 `style="..."`(内联样式绕开 scoped 隔离,也绕开色板审计),于是把「只涉及时序、不涉及样式定值」的部分放到 `onMounted`(组件挂载完成后执行的钩子)里用 JS 注入:

```js
Array.from(terminalBody.value.children).forEach((el, i) => {
  el.style.animationDelay = Math.min(i * 30, 570) + 'ms'
})
```

第 3 章的 CSS 总门控负责「访客偏好减少动效时不播动画」,这里 JS 侧还有一道 `matchMedia('(prefers-reduced-motion: reduce)')` 检查直接跳过注入——双门控互为备份,门控之下内容静态全显,没有「动画没了但内容也看不见」的半成品状态。

### 4.5 模板字符串转义:一个反斜杠的 build 红线

数据数组里的代码示例是 JS 模板字符串,其中有一段 shell 示例要写 `export DEEPSEEK_API_KEY=...`。**模板字符串**是 JS 里用反引号包裹、支持 `${...}` 内嵌表达式的字符串——而这恰好和 shell 的环境变量语法撞车:示例里的 `${DEEPSEEK_API_KEY}` 会被 JS 当成表达式在构建期求值,变量不存在,直接构建失败。解法是写成 `\${VAR}`,一个反斜杠告诉 JS「这是字面量,别求值」。这类坑不踩不知道:示例代码从「给人看的文本」变成「嵌在 JS 里的字符串」那一刻,它的转义规则就换了主人。

### 4.6 八板块走读

| 板块 | 数据源 | 内容要点 |
| --- | --- | --- |
| Hero | `heroLines` | 定位金句、双 CTA、仿终端演示(逐行动画) |
| 状态条 | `stats` | 9 模块/9 内置 Tool/18 REST 端点/12 CLI 命令/3 触发源;脚注强制标注「验收目标,非承诺值」 |
| 运行原理 | `archImage` | 双语架构图,按当前语言用 `withBase()` 切换 en/zh 两张 SVG;移动端横向滚动容器加 `tabindex` 与 `role`/`aria-label`,键盘也能滚 |
| 核心能力 | `capabilities` | 六张卡按工作区文件树形态排布(树形符加条目名),每卡带代码示例与 docRefs 出处 |
| 使用场景 | `runs` + `personas` | 三张已验证场景卡加一行「也常用于」人设清单 |
| 路线图 | `roadmapPhases` | 三阶段卡片,当前阶段高亮;文案严格按「诚实边界」表述,扩展阶段能力不提前承诺 |
| CTA | `ctaLines` | 四步快速开始终端(含 `${ENV_VAR}` 凭证占位示范);诚实脚注写明核心阶段不含认证与限流 |
| Footer | `footerBrand` | 文字标(Agent 常规加 OS 加粗)加双语副标语 |

两个细节体现「内容即产品」的克制:架构图图片引用必须走 `withBase()` 包裹(base 变了路径自动跟着变),性能数字必须带「验收目标」标注(与仓库文档的口径注记一致)。官网最容易膨胀成过度承诺的橱窗,这个组件用数据数组的结构把话术边界固化了下来。

## 5. 次要变更与噪音:资产、规范与生成物

| 组 | 文件 | 说明 |
| --- | --- | --- |
| 自托管字体 | `public/fonts/` 两个 woff2 | Inter(正文)与 JetBrains Mono(代码)的可变字体,站内托管,零运行时外部请求——私有部署定位下内网、离线都可渲染;`unicode-range` 限定只覆盖拉丁字符,中文字符自动落回系统字体栈,不为没有的字形白下载体积 |
| 图标与文字标 | favicon.svg、public/logo.svg | 小体积内联 SVG,文字标「Agent 常规 + OS 加粗」 |
| 双语架构图 | architecture-en.svg、architecture-zh.svg | 自绘,几何一致、文本互换,注释约定两图须同改;运行原理板块按语言经 `withBase()` 选用 |
| 分享图 | og.png、og-zh.png | 1200×630 社交分享卡,与 og meta 按语言配对 |
| 旧占位图清退 | 删除 12 个旧 SVG | 上一阶段为 docs 配图准备的占位文件(docs-*.svg、旧 logo 等),被自绘双语资产取代;其中 docs-architecture-light.svg 以纯改名方式变为 architecture.svg 留档 |
| 站点规范 | website/CLAUDE.md | 102 条站点开发纪律(locale 键位、scoped 纪律、转义红线等),写成可拷贝到其他 VitePress 项目的模板 |
| 实施计划 | spec/.../plan.md | 332 行:目标范围、内容事实基线(每个数字带文档出处)、设计规范、执行清单——第 4 章的「出处机制」源头在此 |
| 生成物 | package-lock.json | npm 依赖锁定文件,自动生成,保证他人安装到相同版本;无教学价值,归为噪音 |

值得一提的取舍:字体从「引外部 CDN」改成「文件进仓库」,代价是仓库体积增加约 80KB,换来的是零外部依赖与确定的加载行为。对私有部署产品,这是一笔明确的划算交易。

## 6. 收尾:学到什么,怎么迁移

这篇变更讲的是一个静态站点的三层结构,每一层都把「单一来源」落在具体文件上:

- 配置层:locale、base、meta 一次定准;base 是一组联动约定,不是单个字符串。
- 主题层:品牌色与字号收敛为 CSS 变量,经 `--vp-*` 映射让框架内部跟着换装;主题覆盖只能写全局样式,scoped 进不了子组件。
- 内容层:数据数组驱动渲染,双语走 `t()` 加 computed,出处链接把内容纪律钉进代码。

迁移到任何 VitePress 项目,可直接复用这张检查单:GitHub Pages 子路径部署必查 favicon、og:image、sitemap hostname 三处 base 联动;构建后补 `.nojekyll`;入口用 theme-without-fonts 防双份字体;`--vp-c-bg-elv` 记得映射,否则深色站的下拉菜单不可读;组件里随语言变化的数据必须包 computed;JS 模板字符串里的 shell 变量写 `\${VAR}`;动画配 `prefers-reduced-motion` 门控。

更可迁移的是工作方法:先写「内容事实基线」再写页面、每个数字带出处、扩展期能力不提前承诺——让官网说真话的从来不是文案,是机制。

## 7. 设计决策记录

1. 提交圈定:标记自 f93bd27 前进至 6c168a7(完整哈希 6c168a748da3c537c9231717385cfe4ad36d7e99),池内仅此一个提交,全部 35 个文件属本次主题,无范围外夹杂;仓库中其他未提交改动(CLAUDE.md、docs 等)按约定不纳入。
2. 同主题旧文:chat/tutorials/ 下已存在 20260902_vitepress_bilingual_site.md,系此前对同一批代码的目录模式讲解(目录模式不推进标记,故标记仍停在 f93bd27)。本篇为提交模式正式成文,文件名日期前缀不同,不覆盖旧文。
3. 色板演进考证:提交内 plan.md §1 锁定「黑底 #000 加橙 #f97316」,最终代码为绿 #2fe07a「机房 LED」v3。演进依据是 plan.md §8 变更记录「2026-08-31 视觉刷新立项并实施完成(初选琥珀荧光同日改选)」;定稿对比细节记录于 chat/consolidate/ 下的未跟踪文件,custom.css 头注释留有出处指针——指针目标不在本提交内,细节从提交中无法考证。
4. base 演进:plan.md 锁定 `base: '/'`,最终 config.mts 为 `/agent-os-poc/`(GitHub Pages 子路径),config 注释说明成因;plan.md 变更记录未收录此变更,以代码为准。
5. 场景板块缩编:plan.md §3.3 规划八张场景卡,执行清单 3.6 仍标「八卡 [x]」,最终代码为三张 run 卡加一行 personas(运维、客服、HR、知识管理、销售),3 加 5 恰覆盖原八项名单。plan 与代码不一致,按「矛盾以 git 为准」取代码口径。
6. 噪音判定:package-lock.json(+2561 行)为依赖锁定生成物,略过不讲;12 个旧 SVG 删除属资产替换的收尾动作,并入第 5 章合讲而非单列。
7. 动机来源:本篇无额外文件,全部动机取自提交信息、代码注释与提交内 plan.md;构建验证结论(make build 通过、Playwright 回归)引自提交信息,未独立复跑。
