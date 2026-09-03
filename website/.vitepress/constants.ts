// 站点级常量单一来源（website/CLAUDE.md「站点常量单一来源」）：
// config 与组件一律 import 使用，禁止在使用处写死字面量。
// 本文件保持零依赖——同时被 Node 侧 config 与浏览器侧组件引用。
export const SITE_NAME = 'AgentOS'
export const REPO_URL = 'https://github.com/fangkun119/agent-os-poc'
// 部署根（github.io 用户页；og:image 绝对 URL 与 sitemap hostname 共用）
export const SITE_URL = 'https://fangkun119.github.io'

// 品牌副标语（footer 与访客 meta 共用；双语成对）
export const TAGLINE_EN = 'Enterprise Agent OS runtime kernel, built on Java · Self-hosted'
export const TAGLINE_ZH = '基于 Java 的企业级 Agent OS 运行时内核 · 私有部署'
