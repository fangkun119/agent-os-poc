import { defineConfig } from 'vitepress'
import { SITE_NAME, REPO_URL, SITE_URL, TAGLINE_EN, TAGLINE_ZH } from './constants'

// 站点级配置。要点（详见 website/CLAUDE.md）：
// - locale 级只允许七键（lang/dir/title/titleTemplate/description/head/themeConfig）；
//   base/cleanUrls/srcExclude 等站点级键写进 locale 层会被静默忽略
// - siteTitle: false（nav 只留 logo）与隐藏暗色切换（custom.css）均为有意设置
// - zh 侧默认主题文案本地化覆盖，build 后需到产物页核验
export default defineConfig({
  title: SITE_NAME,
  titleTemplate: `:title — ${SITE_NAME}`,
  description: TAGLINE_EN,
  // GitHub Pages 子路径部署：仓库 Pages URL 为 /agent-os-poc/，base 必须与之一致
  base: '/agent-os-poc/',
  cleanUrls: true,
  // appearance: false —— 单主题站点，isDark 恒 false：永不写 html.dark、
  // 切换按钮原生不渲染（vitepress 1.6.4 无 'force-light' 取值，勿改回）
  appearance: false,
  // spec/（按「日期-序号-主题」分目录的实施计划）与本规范文件是内部文档，
  // 不作为站点页面发布（漏排会被发布成公开页面）
  srcExclude: ['spec/**', 'CLAUDE.md'],

  // og:title / og:description / og:image / og:image:alt 按locale 分写（双语访客 meta，含分享图）；
  // 站点级只留双语共用项。注意：mergeHead 按 tag+attrs 去重，同名 og 标签不能站点级与 locale 级并存
  head: [
    // head 内根绝对路径 VitePress 不做 base 重写（favicon 在 GitHub Pages 子路径下会 404），
    // 故此处唯一例外地硬编码 base 前缀；改 base 时本行同步改
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/agent-os-poc/favicon.svg' }],
    ['meta', { name: 'author', content: SITE_NAME }],
    ['meta', {
      name: 'keywords',
      content: 'AgentOS, enterprise agent OS, agent operating system, Java agent runtime, ReAct loop, MCP, self-hosted AI, Spring Boot',
    }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:site_name', content: SITE_NAME }],
    ['meta', { property: 'og:image:width', content: '1200' }],
    ['meta', { property: 'og:image:height', content: '630' }],
    ['meta', { name: 'twitter:card', content: 'summary' }],
  ],

  locales: {
    root: {
      label: 'English',
      lang: 'en-US',
      head: [
        ['meta', { property: 'og:title', content: `${SITE_NAME} — Enterprise Agent OS` }],
        ['meta', { property: 'og:description', content: TAGLINE_EN }],
        // og:image 要求绝对 URL；base 前缀同 favicon 约定，改 base 时本行同步改
        ['meta', { property: 'og:image', content: `${SITE_URL}/agent-os-poc/images/og.png` }],
        ['meta', { property: 'og:image:alt', content: `${SITE_NAME} — ${TAGLINE_EN}` }],
      ],
      themeConfig: {
        nav: [{ text: 'Home', link: '/' }],
      },
    },
    zh: {
      label: '中文',
      lang: 'zh-CN',
      link: '/zh/',
      description: TAGLINE_ZH,
      head: [
        ['meta', { property: 'og:title', content: `${SITE_NAME} — 企业级 Agent OS 运行时内核` }],
        ['meta', { property: 'og:description', content: TAGLINE_ZH }],
        ['meta', { property: 'og:image', content: `${SITE_URL}/agent-os-poc/images/og-zh.png` }],
        ['meta', { property: 'og:image:alt', content: `${SITE_NAME} — ${TAGLINE_ZH}` }],
      ],
      themeConfig: {
        nav: [{ text: '首页', link: '/zh/' }],
        // 默认主题文案本地化（英文界面源码不可见，只能 build 后到产物页核验）
        docFooter: { prev: '上一篇', next: '下一篇' },
        outline: { label: '本页目录' },
        sidebarMenuLabel: '目录',
        returnToTopLabel: '回到顶部',
        langMenuLabel: '切换语言',
      },
    },
  },

  themeConfig: {
    // 有意设置：nav 只留 logo，站名经 logo 承载（勿当缺陷修复补回文字）
    siteTitle: false,
    // 对象形态：siteTitle: false 下 logo 链接唯一内容是图，alt 空则链接无可达名
    logo: { src: '/logo.svg', alt: SITE_NAME },
    socialLinks: [{ icon: 'github', link: REPO_URL }],
  },

  // hostname 需自带 base（含尾斜杠）：vitepress 1.6.4 generateSitemap 只拼 hostname + 页面相对
  // 路径（root 页相对路径为空串），不加 base 且缺尾斜杠会多一次 301
  sitemap: { hostname: `${SITE_URL}/agent-os-poc/` },
})
