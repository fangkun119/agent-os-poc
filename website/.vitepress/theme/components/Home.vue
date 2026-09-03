<script setup>
// AgentOS 官网主页：八板块单组件（v3「机房 LED」+ 工作台结构，见刷新 SPEC）。
// 约定（website/CLAUDE.md）：
// - 文案一律 t(zh, en)（zh 在前）；随语言变化的数据数组包 computed
// - 代码示例单语（不翻译），模板字符串内 shell 变量必须写成 \${VAR}
// - 段落/条目只改本文件顶部数据数组，模板只做 v-for 渲染
import { computed, onMounted, ref } from 'vue'
import { useData, withBase } from 'vitepress'
import { REPO_URL, SITE_NAME, TAGLINE_EN, TAGLINE_ZH } from '../../constants'

const { lang } = useData()
const isZh = computed(() => lang.value === 'zh-CN')
const t = (zh, en) => (isZh.value ? zh : en)

const archImage = computed(() =>
  withBase(isZh.value ? '/images/architecture-zh.svg' : '/images/architecture-en.svg')
)

// ⟶ 溯源链接：页面数字/机制的出处（「页面事实仅取自权威文档」，
// spec/20260830-001-create-website plan §2）；DA = 需求文档，TS = 技术方案。
// 映射在每次调用时求值（内含 t()，普通 const 会固化首载语言）
const docRefs = (refs) =>
  refs.map((ref) => {
    const [key, ...num] = ref.split(' ')
    const doc = {
      DA: { file: 'DemandAnalysis.md', title: t('需求文档 · ', 'Demand analysis · ') },
      TS: { file: 'TechnicalSolution.md', title: t('技术方案 · ', 'Technical solution · ') },
    }[key]
    return {
      label: ref,
      href: `${REPO_URL}/blob/main/docs/${doc.file}`,
      title: `${doc.title}${doc.file} §${num.join(' ')}`,
    }
  })

const stats = computed(() => [
  { num: '9', label: t('Maven 模块', 'Maven modules') },
  { num: '9', label: t('内置 Tool', 'built-in tools') },
  { num: '18', label: t('REST 端点', 'REST endpoints') },
  { num: '12', label: t('CLI 命令', 'CLI commands') },
  { num: '3', label: t('触发源，一个入口', 'triggers, one entry') },
])

// 能力板块 = 工作区文件树：装饰编号退场，树形符与条目名来自数据（结构即产品真话）
const capabilities = computed(() => [
  {
    glyph: '├──',
    entry: 'AGENT.md',
    title: t('一个目录 = 一个 Agent', 'One directory = one agent'),
    desc: t(
      '一个 Agent 就是一个目录：AGENT.md = frontmatter（配置）+ 正文（任务指令），可选 skills/ 软连接与 scripts/。不写代码，放下目录即是一个完整可用的业务 Agent。',
      'An agent is a directory: AGENT.md = frontmatter (config) + a body of instructions, plus optional skills/ symlinks and scripts/. No code — a directory dropped in is a complete, working business agent.'
    ),
    code: `.agentos/agents/ops-agent/
├── AGENT.md    # config + instructions
├── skills/     # on demand
└── scripts/    # on demand

❯ agentos profile create ops-agent`,
    source: docRefs(['DA 5.2', 'TS 11.1']),
  },
  {
    glyph: '├──',
    entry: 'providers/',
    title: t('对接主流大模型', 'Mainstream LLM providers'),
    desc: t(
      'Provider 抽象对接 DeepSeek、通义、Kimi、智谱、Anthropic、OpenAI 等主流模型；provider name → ChatModel 显式映射，多 Provider 并存路由无歧义，OpenAI / Anthropic / Gemini 三家 tools 协议差异由适配层吸收。',
      'The provider abstraction connects DeepSeek, Qwen, Kimi, GLM, Anthropic, OpenAI and more; explicit name → ChatModel mapping keeps multi-provider routing unambiguous, and tool-call protocol differences (OpenAI / Anthropic / Gemini) are absorbed by adapters.'
    ),
    code: `# application.yaml — keys via env
providers:
  deepseek:
    api-key: \${DEEPSEEK_API_KEY}

# AGENT.md (frontmatter)
provider:
  name: deepseek
  model: deepseek-chat`,
    source: docRefs(['TS 3', 'DA 5.3']),
  },
  {
    glyph: '├──',
    entry: 'react-loop',
    title: t('自实现 ReAct 循环', 'Self-implemented ReAct loop'),
    desc: t(
      'Reason → Act → Observe 循环由数十行 Java 自实现，同步执行 + 虚拟线程，不被任何外部 Agent 框架包裹；执行全程不切换线程，每一步可查可审计，最大迭代次数可按 Agent 覆盖。',
      'The Reason → Act → Observe loop is a few dozen lines of in-house Java — synchronous on virtual threads, wrapped by no external agent framework. Execution never switches threads, every step is inspectable, and the iteration cap is per-agent configurable.'
    ),
    code: `User message
 → PromptBuilder (5-part prompt)
 → ProviderService.call()
 → tool call? → sandbox → execute
   → audit → loop, max 10
 → final reply → persist session`,
    source: docRefs(['TS 4.1', 'DA 5.4']),
  },
  {
    glyph: '├──',
    entry: 'memory/',
    title: t('三层记忆，一个门面', 'Three-layer memory, one facade'),
    desc: t(
      '会话记忆 + 长期记忆统一经 MemoryService 门面；长期记忆默认 Markdown 档 MEMORY.md，双分区——核心记忆永不截断、归档记忆按需截断；换后端只改 memory.backend 一行配置，Agent 无感。情景记忆随扩展阶段落地。',
      'Session and long-term memory sit behind one MemoryService facade. Long-term memory defaults to a Markdown MEMORY.md with two partitions — the core partition is never truncated, the archive is trimmed on demand. Switching backends is a one-line memory.backend change. Episodic memory lands with Phase 2.'
    ),
    code: `you: remember: report due Fri 17:00

Tool: save_memory  →  MEMORY.md

## Core memory  # never truncated
- 2026-08-30 Report due Fri 17:00

## Archive      # trimmed on demand`,
    source: docRefs(['TS 5', 'DA 5.5']),
  },
  {
    glyph: '├──',
    entry: 'tools/',
    title: t('插件工具三档接入', 'Three-tier plugin tools'),
    desc: t(
      '9 个内置 Tool 覆盖读写文件、执行命令、调用 HTTP、记忆与通知的最短链路；扩展走三档接入——零代码（AGENT.md + 现成 MCP server，主推）、轻代码（任意语言写 MCP server）、重代码（@Tool Java Bean）；原则：能用一不用二。',
      'Nine built-in tools cover the shortest path: read/write files, run commands, call HTTP, remember, notify. Extensions plug in at three tiers — zero-code (an AGENT.md plus ready-made MCP servers, the primary path), low-code (your own MCP server in any language), or heavy-code (@Tool Java beans). Rule of thumb: always prefer the lower tier.'
    ),
    code: `# zero  : AGENT.md + MCP servers
# light : own MCP server, any language
# heavy : @Tool Java bean, in-process
#
# all wrap into AgentOSTool — the ReAct
# loop never sees where a tool came from`,
    source: docRefs(['TS 6', 'DA 5.6']),
  },
  {
    glyph: '└──',
    entry: 'service/',
    title: t('Web Service 门面', 'Web Service facade'),
    desc: t(
      '18 个 REST 端点对外暴露完整运行时：会话管理、无状态调用、信息查询与系统状态；OpenAPI 3.0 文档随服务暴露在 /swagger-ui，任何能发 HTTP 请求的语言都能接入。',
      'Eighteen REST endpoints expose the runtime: session management, stateless invocation, lookups, and system status. OpenAPI 3.0 docs ship at /swagger-ui — any language that can send an HTTP request can integrate.'
    ),
    code: `❯ agentos serve        # :8080

POST  /api/v1/sessions
POST  /api/v1/sessions/{id}/messages
POST  /api/v1/agents/{name}/invoke
GET   /swagger-ui`,
    source: docRefs(['TS 7', 'DA 5.8']),
  },
])

// 场景板块 = 三次已验证的运行（DA 13 验收 Demo + DA 5.6 零代码示例）+ 人设一行
const runs = computed(() => [
  {
    title: t('每日天气与穿搭', 'Daily weather and outfit'),
    schedule: t('每天 8:00', 'Daily at 8:00'),
    trigger: t('AgentScheduler（钟推）', 'AgentScheduler (cron)'),
    dest: t('企业 IM 群 · Webhook', 'Enterprise IM via webhook'),
    desc: t(
      'AGENT.md 里写一行 cron：每天 8 点自动查天气、生成穿搭建议，经 Webhook 推进企业 IM 群。',
      'A cron line in AGENT.md: every day at 8am it checks the weather, drafts an outfit suggestion, and pushes it to your enterprise IM via webhook.'
    ),
    source: docRefs(['DA 13', 'TS 12.1']),
  },
  {
    title: t('每日 PR 评审日报', 'Daily PR review digest'),
    schedule: t('每日早晨', 'Every morning'),
    trigger: t('AgentScheduler（钟推）', 'AgentScheduler (cron)'),
    dest: 'Slack · Webhook',
    desc: t(
      '一个目录 + 两个现成 MCP server：每天早上把 GitHub PR 评审进度推到 Slack，全程零代码。',
      'One directory plus two ready-made MCP servers: GitHub PR review progress pushed to Slack every morning, zero code.'
    ),
    source: docRefs(['DA 5.6']),
  },
  {
    title: t('每日科技日报', 'Daily tech digest'),
    schedule: t('每天', 'Daily'),
    trigger: t('AgentScheduler（钟推）', 'AgentScheduler (cron)'),
    dest: t('企业 IM 渠道', 'Enterprise IM channels'),
    desc: t(
      '定时抓取科技资讯，汇总成日报自动推送，全程无需人工触发。',
      'Fetches tech news on a schedule, compiles a daily digest, and pushes it — no human in the loop.'
    ),
    source: docRefs(['DA 13']),
  },
])

const personas = computed(() => [
  t(
    '也常用于：运维 · 客服 · HR · 知识管理 · 销售',
    'Also common: DevOps · customer service · HR · knowledge management · sales'
  ),
])

const roadmapPhases = computed(() => [
  {
    phase: t('核心阶段', 'Phase 1'),
    status: t('当前', 'Current'),
    active: true,
    title: t('运行时内核', 'Runtime kernel'),
    items: [
      t('五大核心能力', 'Five core capabilities'),
      t('9 个内置 Tool + MCP', '9 built-in tools + MCP'),
      t('18 个 REST 端点 / 12 个 CLI 命令', '18 REST endpoints / 12 CLI commands'),
      t('审计表 day one 落库', 'Audit tables from day one'),
    ],
  },
  {
    phase: t('扩展阶段', 'Phase 2'),
    status: t('规划中', 'Planned'),
    active: false,
    title: t('企业治理层', 'Enterprise governance'),
    items: [
      t('多租户与 SSO', 'Multi-tenancy and SSO'),
      t('完整审计查询与 Tool Policy', 'Full audit query and tool policy'),
      t('沙箱升级：容器 / microVM 隔离', 'Sandbox upgrade: container / microVM isolation'),
      t('情景记忆、Webhook / SSE / Metrics', 'Episodic memory, webhook / SSE / metrics'),
    ],
  },
  {
    phase: t('社区共建', 'Phase 3'),
    status: t('愿景', 'Vision'),
    active: false,
    title: t('长期方向', 'Long-term direction'),
    items: [
      t('文档站与 SDK', 'Docs site and SDK'),
      t('工具 Marketplace', 'Tool marketplace'),
      t('移动端管理台', 'Mobile admin console'),
      t('社区共建渠道与工具生态', 'Community channel and tool ecosystem'),
    ],
  },
])

// 终端演示逐行数据：type 决定渲染形态（cmd/out/dim/ok/user/agent/comment/spacer/cursor）
const heroLines = computed(() => [
  { type: 'cmd', text: 'agentos init' },
  { type: 'ok', text: t('✓ 工作区已初始化：.agentos/', '✓ Workspace initialized at .agentos/') },
  { type: 'dim', text: 'agents/ · skills/ · memory/ · mcp_servers.yaml · agentos.db' },
  { type: 'dim', text: t('+ Bootstrap：AGENTS.md · SOUL.md · USER.md', '+ bootstraps: AGENTS.md · SOUL.md · USER.md') },
  { type: 'spacer' },
  { type: 'cmd', text: 'agentos profile create ops-agent' },
  { type: 'ok', text: t('✓ 已生成最小 AGENT.md', '✓ Minimal AGENT.md generated') },
  { type: 'spacer' },
  { type: 'cmd', text: 'agentos chat --profile ops-agent' },
  { type: 'dim', text: t('已加载 Profile：ops-agent（deepseek-chat）', 'Loaded profile: ops-agent (deepseek-chat)') },
  { type: 'spacer' },
  { type: 'user', text: t('记住：周报每周五 17:00 发出', 'Remember: the weekly report goes out Friday 17:00') },
  { type: 'spacer' },
  { type: 'agent', text: t('[ops-agent] 思考中…', '[ops-agent] Thinking...') },
  { type: 'dim', text: '  → Tool: save_memory' },
  { type: 'dim', text: t('  → 沙箱白名单：✓ 放行，已写审计', '  → sandbox whitelist: ✓ allowed, audited') },
  { type: 'agent', text: t('[ops-agent] 已写入长期记忆（MEMORY.md），下次会话我会记得。', '[ops-agent] Saved to long-term memory (MEMORY.md). See you next session.') },
  { type: 'spacer' },
  { type: 'cursor' },
])

const ctaLines = computed(() => [
  { type: 'comment', text: '# 1. init the workspace' },
  { type: 'cmd', text: 'agentos init' },
  { type: 'spacer' },
  { type: 'comment', text: '# 2. configure a provider — keys via env, never plain text' },
  { type: 'cmd', text: 'export DEEPSEEK_API_KEY=your-key-here' },
  { type: 'spacer' },
  { type: 'comment', text: '# 3. create your first agent' },
  { type: 'cmd', text: 'agentos profile create ops-agent' },
  { type: 'spacer' },
  { type: 'comment', text: '# 4. chat — or serve the REST API on :8080' },
  { type: 'cmd', text: 'agentos chat --profile ops-agent' },
  { type: 'cmd', text: 'agentos serve' },
])

const footerBrand = computed(() => [SITE_NAME.slice(0, -2), SITE_NAME.slice(-2)])

// 唯一编排时刻：hero 终端逐行显现（≤600ms）。逐行延迟各不相同，而模板内联 style 属性
// 被 website/CLAUDE.md 禁止（绕开 scoped 隔离与色板审计），故在 mounted 以 JS 注入动画
// 时序（仅时序，非样式定值）；prefers-reduced-motion 时整段跳过，内容静态全显。
const terminalBody = ref(null)
onMounted(() => {
  if (!terminalBody.value) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  terminalBody.value.classList.add('is-animated')
  Array.from(terminalBody.value.children).forEach((el, i) => {
    el.style.animationDelay = Math.min(i * 30, 570) + 'ms'
  })
})
</script>

<template>
  <div class="home">
    <!-- ── HERO ── -->
    <section class="hero">
      <div class="hero-inner">
        <p class="hero-eyebrow">
          <span class="eyebrow-comment">// </span>{{ t('Apache-2.0 · 私有部署 · 数据不出域 · 不锁云生态', 'Apache-2.0 · self-hosted · your data stays home · no cloud lock-in') }}
        </p>

        <h1 class="hero-headline" :class="{ 'hero-headline--zh': isZh }">
          <span class="headline-tag">{{ t('企业级 Agent OS', 'The Enterprise Agent OS') }}</span><br>
          <span class="headline-white">{{ t('Runtime 让一个 Agent 跑起来，', 'A runtime runs one agent.') }}</span><br>
          <span class="headline-accent">{{ t('Agent OS 让一群 Agent 被管起来。', 'An Agent OS governs them all.') }}</span>
        </h1>

        <p class="hero-sub">
          {{
            t(
              'AgentOS 是基于 Java 的企业级 Agent OS 运行时内核：装在你自己的 K8s 或服务器上，一个目录一个 Agent，共享渠道接入、模型路由、工具调用、记忆、沙箱与审计。数据完全留在你的基础设施，不锁任何云生态——让每一家企业，都能用自然语言跑起自己的 Agent。',
              'AgentOS is an enterprise Agent OS runtime kernel, built on Java. It installs on your own Kubernetes or servers: one directory is one agent, sharing channel access, model routing, tool calling, memory, sandboxing, and audit. Your data never leaves your infrastructure, and no cloud locks you in — so every enterprise can run its own agents in plain language.'
            )
          }}
        </p>

        <div class="hero-ctas">
          <a class="btn-primary" href="#get-started">
            {{ t('快速开始', 'Get Started') }}
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 7h10M8 3l4 4-4 4"/></svg>
          </a>
          <a class="btn-ghost" :href="REPO_URL" target="_blank" rel="noopener">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.477 2 2 6.477 2 12c0 4.418 2.865 8.166 6.839 9.489.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.745 0 .268.18.58.688.482A10.019 10.019 0 0022 12c0-5.523-4.477-10-10-10z"/></svg>
            GitHub
          </a>
        </div>

        <div class="terminal">
          <div class="terminal-titlebar">
            <span class="dot dot-red"></span>
            <span class="dot dot-yellow"></span>
            <span class="dot dot-green"></span>
            <span class="terminal-title">agentos — bash</span>
          </div>
          <div ref="terminalBody" class="terminal-body">
            <template v-for="(line, i) in heroLines" :key="i">
              <div v-if="line.type === 'spacer'" class="term-spacer term-reveal"></div>
              <div v-else-if="line.type === 'cmd'" class="term-line term-reveal">
                <span class="term-prompt">❯</span>
                <span class="term-cmd">{{ line.text }}</span>
              </div>
              <div v-else-if="line.type === 'user'" class="term-line term-reveal">
                <span class="term-user">you</span>
                <span class="term-msg">{{ line.text }}</span>
              </div>
              <div v-else-if="line.type === 'cursor'" class="term-line term-reveal">
                <span class="term-prompt">❯</span>
                <span class="term-cursor"></span>
              </div>
              <div v-else class="term-output term-reveal" :class="'term-' + line.type">{{ line.text }}</div>
            </template>
          </div>
        </div>
      </div>
    </section>

    <!-- ── AGENTOS STATUS（账本） ── -->
    <div class="status-bar">
      <div class="status-inner">
        <span class="status-title">agentos status</span>
        <div v-for="s in stats" :key="s.label" class="status-row">
          <span class="status-label">{{ s.label }}</span>
          <span class="status-num">{{ s.num }}</span>
        </div>
        <p class="status-note">
          <span class="status-note-tag"># note:</span>
          {{
            t(
              '性能为验收目标，非承诺值：单节点 ≥100 并发 Session · Session 创建 P99 < 200ms · 新手 30 分钟单节点部署',
              'Acceptance targets, not promises: ≥100 concurrent sessions per node · session creation P99 < 200ms · a 30-minute first deployment'
            )
          }}
        </p>
      </div>
    </div>

    <!-- ── HOW IT WORKS（居中解剖） ── -->
    <section class="section">
      <div class="section-inner">
        <div class="section-header">
          <span class="section-label">{{ t('运行原理', 'How it works') }}</span>
          <h2 class="section-h2">{{ t('人推钟推，同一条链路。', 'One chain for human and clock.') }}</h2>
          <p class="how-sub">
            {{
              t(
                'CLI、REST API、定时任务三种触发源汇入同一个 AgentService.process——人推和钟推走同一条链路，审计与会话语义完全一致。',
                'CLI, REST API, and the scheduler all converge into one AgentService.process — identical audit and session semantics whether a human or a cron triggers the run.'
              )
            }}
          </p>
        </div>
        <!-- tabindex 令滚动容器键盘可达（方向键滚动）；名称供读屏播报可滚动区域 -->
        <div class="arch-diagram" tabindex="0" role="region" :aria-label="t('架构图（可左右滚动）', 'Architecture diagram (scrollable horizontally)')">
          <img :src="archImage" :alt="t('AgentOS 运行时架构：触发源 → 执行引擎 → 能力层 → SQLite 存储', 'AgentOS runtime architecture: triggers → agent core → capabilities → SQLite storage')" class="arch-img"/>
        </div>
        <p class="arch-hint">{{ t('← 左右滑动查看完整架构', '← swipe to see the full diagram') }}</p>
      </div>
    </section>

    <!-- ── 工作区（左锚解剖 + 波段底） ── -->
    <section class="section section--band">
      <div class="section-inner">
        <div class="section-header section-header--left">
          <span class="section-label">{{ t('工作区', 'Workspace') }}</span>
          <h2 class="section-h2">{{ t('一个目录，多 Agent 并存。', 'One directory. Many agents.') }}</h2>
        </div>

        <div class="caps-grid">
          <div v-for="cap in capabilities" :key="cap.entry" class="cap-card">
            <div class="cap-top">
              <span class="cap-entry"><span class="cap-glyph">{{ cap.glyph }}</span>{{ cap.entry }}</span>
              <h3 class="cap-title">{{ cap.title }}</h3>
              <p class="cap-desc">{{ cap.desc }}</p>
            </div>
            <pre class="cap-code"><code>{{ cap.code }}</code></pre>
            <span class="cap-source">⟶
              <template v-for="(s, i) in cap.source" :key="s.label">
                <span v-if="i" aria-hidden="true"> · </span><a class="source-link" :href="s.href" :title="s.title" target="_blank" rel="noopener">{{ s.label }}</a>
              </template>
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- ── 三次已验证的运行（居中解剖） ── -->
    <section class="section">
      <div class="section-inner">
        <div class="section-header">
          <span class="section-label">{{ t('使用场景', 'Use cases') }}</span>
          <h2 class="section-h2">{{ t('三次已验证的运行。', 'Three verified runs.') }}</h2>
        </div>

        <div class="runs-grid">
          <div v-for="r in runs" :key="r.title" class="run-card">
            <h3 class="run-title">{{ r.title }}</h3>
            <div class="run-meta">
              <div class="run-meta-row">
                <span class="run-meta-label">{{ t('定时', 'Schedule') }}</span>
                <span class="run-meta-value">{{ r.schedule }}</span>
              </div>
              <div class="run-meta-row">
                <span class="run-meta-label">{{ t('触发', 'Trigger') }}</span>
                <span class="run-meta-value">{{ r.trigger }}</span>
              </div>
              <div class="run-meta-row">
                <span class="run-meta-label">{{ t('推送', 'Push') }}</span>
                <span class="run-meta-value">{{ r.dest }}</span>
              </div>
            </div>
            <p class="run-desc">{{ r.desc }}</p>
            <span class="run-source">⟶
              <template v-for="(s, i) in r.source" :key="s.label">
                <span v-if="i" aria-hidden="true"> · </span><a class="source-link" :href="s.href" :title="s.title" target="_blank" rel="noopener">{{ s.label }}</a>
              </template>
            </span>
          </div>
        </div>

        <p class="personas">
          <span class="personas-label">{{ t('也常用于', 'Also common') }}</span>
          {{ t('运维 · 客服 · HR · 知识管理 · 销售', 'DevOps · customer service · HR · knowledge management · sales') }}
        </p>
      </div>
    </section>

    <!-- ── ROADMAP（左锚解剖 + 波段底） ── -->
    <section class="section section--band">
      <div class="section-inner">
        <div class="section-header section-header--left">
          <span class="section-label">{{ t('路线图', 'Roadmap') }}</span>
          <h2 class="section-h2">{{ t('慢就是快，分阶段克制。', 'Built to grow. Phase by phase.') }}</h2>
        </div>

        <div class="roadmap-grid">
          <div v-for="p in roadmapPhases" :key="p.phase" class="roadmap-card" :class="{ 'roadmap-card--active': p.active }">
            <div class="roadmap-top">
              <span class="roadmap-phase">{{ p.phase }}</span>
              <span class="roadmap-status" :class="{ 'roadmap-status--active': p.active }">{{ p.status }}</span>
            </div>
            <h3 class="roadmap-title">{{ p.title }}</h3>
            <ul class="roadmap-items">
              <li v-for="item in p.items" :key="item" class="roadmap-item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- ── CTA ── -->
    <section id="get-started" class="section-cta">
      <div class="section-inner">
        <div class="cta-grid">
          <div class="cta-left">
            <span class="section-label">{{ t('立即开始', 'Get started') }}</span>
            <h2 class="cta-h2">{{ t('从一个 Agent 到一群 Agent 的底座。', 'From one agent to a fleet.') }}</h2>
            <p class="cta-sub">
              {{
                t(
                  '初始化工作区、配置 Provider、创建第一个 Agent，随时从一个扩展到一群。',
                  'Initialize the workspace, configure a provider, create your first agent — scale from one to a fleet whenever you are ready.'
                )
              }}
            </p>
            <p class="cta-note">
              {{
                t(
                  '边界说明：核心阶段面向内网部署，不含认证、限流与 SSE 流式；多租户、SSO、审计查询与 Tool Policy 在扩展阶段路线图中。',
                  'Scope note: the core phase targets in-network deployment — no auth, rate limiting, or SSE streaming yet. Multi-tenancy, SSO, audit query, and tool policy are on the Phase 2 roadmap.'
                )
              }}
            </p>
            <div class="cta-btns">
              <a class="btn-dark" :href="REPO_URL" target="_blank" rel="noopener">GitHub</a>
            </div>
          </div>
          <div class="cta-right">
            <div class="cta-terminal">
              <div class="cta-terminal-bar">
                <span class="dot dot-dark"></span>
                <span class="dot dot-dark"></span>
                <span class="dot dot-dark"></span>
              </div>
              <div class="cta-code">
                <template v-for="(line, i) in ctaLines" :key="i">
                  <div v-if="line.type === 'spacer'" class="cta-line cta-line--spacer">&nbsp;</div>
                  <div v-else-if="line.type === 'comment'" class="cta-line code-comment">{{ line.text }}</div>
                  <div v-else class="cta-line"><span class="code-prompt">❯ </span>{{ line.text }}</div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── FOOTER ── -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">{{ footerBrand[0] }}<strong>{{ footerBrand[1] }}</strong></span>
          <span class="footer-tagline">{{ t(TAGLINE_ZH, TAGLINE_EN) }}</span>
        </div>
        <div class="footer-links">
          <a :href="REPO_URL" target="_blank" rel="noopener" class="footer-link">GitHub</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* 色板与字阶一律消费 custom.css 的 --aos-* 变量（v3「琥珀荧光」，定稿见刷新 SPEC §3.1）。
   accent 岗位五类：① CTA 健康面板描边 ② 终端系 prompt/光标/树形符 ③ 标题第二行 ④ 数据读出层
   （账本数值/板块标签/阶段名，用 accent 与 accent-dim 两档）⑤ 交互点睛
   （按钮/胶囊/slash/hover/焦点）。终端三圆点为固定语义色。断点档位：900 / 768 / 480。 */
.home {
  min-height: 100vh;
  background: var(--aos-bg);
  color: var(--aos-text-1);
  font-family: var(--aos-font-body);
}
.home * { box-sizing: border-box; }
.home a { text-decoration: none; }

/* ── HERO ── */
.hero {
  background: var(--aos-bg);
  padding: 96px 24px 80px;
  text-align: center;
}
.hero-inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero-eyebrow {
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  color: var(--aos-text-2);
  margin: 0 0 32px;
  letter-spacing: 0.02em;
}
.eyebrow-comment { color: var(--aos-accent); }

.hero-headline {
  font-size: var(--aos-fs-display-1);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: -0.03em;
  margin: 0 0 28px;
  text-wrap: balance;
}
/* 中文标题字面更宽，降一档字号让断行落在逗号处而不是词组中间 */
.hero-headline--zh {
  font-size: clamp(32px, 5.4vw, 48px);
}
.headline-tag {
  display: inline-block;
  font-size: 0.38em;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--aos-accent);
  border: 1px solid var(--aos-accent-soft);
  border-radius: 4px;
  padding: 3px 10px;
  margin-bottom: 12px;
  vertical-align: middle;
}
/* LED 状态点：机房灯语（纯 CSS，不动数据结构；点缀于胶囊/账本标题/账本行标签） */
.headline-tag::before,
.status-title::before,
.status-label::before {
  content: '';
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--aos-accent);
  box-shadow: 0 0 0 3px var(--aos-accent-soft);
  vertical-align: middle;
  margin-right: 8px;
}
.headline-white { color: var(--aos-text-1); }
.headline-accent { color: var(--aos-accent); }

.hero-sub {
  font-size: var(--aos-fs-lead);
  line-height: 1.75;
  color: var(--aos-text-2);
  max-width: 620px;
  margin: 0 0 40px;
}

.hero-ctas {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 56px;
}
.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 6px;
  background: var(--aos-accent);
  color: var(--aos-bg);
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 0.01em;
  transition: background 0.15s, transform 0.15s;
}
.btn-primary:hover { background: var(--aos-accent-hover); transform: translateY(-1px); }
.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 6px;
  border: 1px solid var(--aos-line-strong);
  color: var(--aos-text-1);
  font-weight: 600;
  font-size: 14px;
  transition: border-color 0.15s, color 0.15s;
}
.btn-ghost:hover { border-color: var(--aos-accent); color: var(--aos-accent); }

/* 终端窗（hero；逐行显现的编排时刻，样式与门控见 term-reveal） */
.terminal {
  width: 100%;
  max-width: 680px;
  border-radius: 10px;
  border: 1px solid var(--aos-line);
  background: var(--aos-code-bg);
  overflow: hidden;
  text-align: left;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.8), 0 0 0 1px var(--aos-line);
}
.terminal-titlebar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--aos-panel-2);
  border-bottom: 1px solid var(--aos-line);
}
.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
/* 固定语义色：macOS 终端三圆点，永不随色板联动 */
.dot-red { background: var(--aos-dot-red); }
.dot-yellow { background: var(--aos-dot-yellow); }
.dot-green { background: var(--aos-dot-green); }
.dot-dark { background: var(--aos-line-strong); }
.terminal-title {
  font-family: var(--aos-font-mono);
  font-size: 12px;
  color: var(--aos-text-3);
  margin-left: 8px;
}
.terminal-body {
  padding: 20px 20px 24px;
  font-family: var(--aos-font-mono);
  font-size: 13px;
  line-height: 1.7;
}
.term-line { display: flex; align-items: baseline; gap: 8px; }
.term-prompt { color: var(--aos-accent); font-weight: 700; }
.term-cmd { color: var(--aos-text-1); }
.term-output { color: var(--aos-code-text); white-space: pre-wrap; }
.term-ok { color: var(--aos-signal-ok); }
.term-dim { color: var(--aos-text-3); }
.term-comment { color: var(--aos-text-3); }
.term-agent { color: var(--aos-accent); font-weight: 700; }
.term-spacer { height: 6px; }
.term-user { color: var(--aos-signal-ok); font-weight: 700; flex-shrink: 0; }
.term-msg { color: var(--aos-text-1); }
.term-cursor {
  display: inline-block;
  width: 8px;
  height: 14px;
  background: var(--aos-accent);
  animation: blink 1.2s step-end infinite;
  vertical-align: text-bottom;
  margin-left: 2px;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
/* 编排时刻：逐行显现（延迟由 mounted 注入；reduced-motion 由 custom.css 全局门控静态化） */
.terminal-body.is-animated .term-reveal {
  animation: line-in 0.28s ease-out both;
}
@keyframes line-in {
  from { opacity: 0; transform: translateY(4px); }
}

/* ── AGENTOS STATUS（账本；替代通用大数字条） ── */
.status-bar {
  background: var(--aos-panel-1);
  border-top: 1px solid var(--aos-line);
  border-bottom: 1px solid var(--aos-line);
  padding: 32px 24px;
}
.status-inner {
  max-width: 720px;
  margin: 0 auto;
  font-family: var(--aos-font-mono);
}
.status-title {
  display: block;
  font-size: var(--aos-fs-caption);
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--aos-accent);
  margin-bottom: 12px;
}
.status-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid var(--aos-line);
}
.status-label {
  font-size: var(--aos-fs-body);
  color: var(--aos-text-2);
}
.status-num {
  font-size: var(--aos-fs-data);
  font-weight: 700;
  color: var(--aos-accent);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.status-note {
  margin: 12px 0 0;
  font-size: var(--aos-fs-caption);
  line-height: 1.7;
  color: var(--aos-text-dim);
}
.status-note-tag { color: var(--aos-signal-warn); }

/* ── SECTIONS（两种解剖：居中 / 左锚；band 波段交替破节拍器） ── */
.section { padding: 88px 24px; }
.section--band { background: var(--aos-band); }
.section-inner { max-width: 1040px; margin: 0 auto; }
.section-header {
  text-align: center;
  margin-bottom: 56px;
}
.section-header--left {
  text-align: left;
}
.section-label {
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  font-weight: 700;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--aos-accent-dim);
  display: block;
  margin-bottom: 16px;
}
.section-h2 {
  font-size: var(--aos-fs-display-2);
  font-weight: 800;
  color: var(--aos-text-1);
  margin: 0;
  letter-spacing: -0.02em;
  line-height: 1.1;
}
.how-sub {
  font-size: 14px;
  line-height: 1.75;
  color: var(--aos-text-dim);
  text-align: center;
  max-width: 680px;
  margin: 20px auto 0;
}
.arch-diagram {
  width: 100%;
  margin-top: 8px;
  overflow-x: auto;
}
.arch-img {
  display: block;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  border-radius: 10px;
}
/* 窄屏专用提示（768 档显示）：架构图在该档锁定宽度横向滚动 */
.arch-hint {
  display: none;
  margin: 12px auto 0;
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  color: var(--aos-text-3);
  text-align: center;
}

/* ── 工作区文件树（1px 缝隙网格；树形符即编号） ── */
.caps-grid {
  display: grid;
  /* minmax(0,1fr)：卡内 white-space:pre 长代码行不得撑爆轨道（1fr 的 auto 最小宽会被内容顶开） */
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  background: var(--aos-line);
  border: 1px solid var(--aos-line);
  border-radius: 12px;
  overflow: hidden;
}
.cap-card {
  min-width: 0; /* 清 grid item auto 最小宽：卡内 pre 长行不得撑爆收缩轨道 */
  background: var(--aos-panel-2);
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: background 0.2s;
  cursor: default;
}
.cap-card:hover {
  background: var(--aos-panel-hover-2);
  box-shadow: inset 0 0 0 1px var(--aos-accent);
}
.cap-top { display: flex; flex-direction: column; gap: 10px; }
.cap-entry {
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  font-weight: 700;
  color: var(--aos-text-1);
  letter-spacing: 0.04em;
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.cap-glyph { color: var(--aos-accent); }
.cap-title {
  font-size: var(--aos-fs-title);
  font-weight: 700;
  color: var(--aos-text-1);
  margin: 0;
  line-height: 1.2;
}
.cap-desc {
  font-size: var(--aos-fs-body);
  color: var(--aos-text-2);
  line-height: 1.7;
  margin: 0;
}
.cap-code {
  background: var(--aos-code-bg);
  border: 1px solid var(--aos-line);
  border-radius: 6px;
  padding: 16px;
  font-family: var(--aos-font-mono);
  font-size: 12px;
  line-height: 1.65;
  color: var(--aos-code-text);
  overflow-x: auto;
  margin: 0;
  white-space: pre;
  flex: 1;
}
.cap-code code {
  font-family: var(--aos-font-mono);
  background: none;
  color: inherit;
}
.cap-source, .run-source {
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  color: var(--aos-text-dim);
}
/* 溯源链接继承行内视觉，hover 点睛（焦点样式走全局 :focus-visible） */
.source-link {
  color: inherit;
  text-decoration: none;
  transition: color 0.15s;
}
.source-link:hover { color: var(--aos-accent); }

/* ── 三次已验证的运行 ── */
.runs-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}
.run-card {
  min-width: 0;
  background: var(--aos-panel-2);
  border: 1px solid var(--aos-line);
  border-radius: 12px;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.2s, background 0.2s;
}
.run-card:hover {
  border-color: var(--aos-line-strong);
  background: var(--aos-panel-hover-2);
}
.run-title {
  font-size: var(--aos-fs-title);
  font-weight: 700;
  color: var(--aos-text-1);
  margin: 0;
  line-height: 1.25;
}
.run-meta {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--aos-line);
}
.run-meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  border-bottom: 1px solid var(--aos-line);
}
.run-meta-label {
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  color: var(--aos-text-3);
  flex-shrink: 0;
}
.run-meta-value {
  font-family: var(--aos-font-mono);
  font-size: 12px;
  color: var(--aos-code-text);
  text-align: right;
}
.run-desc {
  font-size: var(--aos-fs-body);
  color: var(--aos-text-2);
  line-height: 1.7;
  margin: 0;
  flex: 1;
}
.personas {
  margin: 32px 0 0;
  text-align: center;
  font-family: var(--aos-font-mono);
  font-size: 12px;
  color: var(--aos-text-dim);
}
.personas-label {
  color: var(--aos-text-3);
  margin-right: 8px;
}

/* ── 路线图 ── */
.roadmap-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  background: var(--aos-line);
  border: 1px solid var(--aos-line);
  border-radius: 12px;
  overflow: hidden;
}
.roadmap-card {
  min-width: 0;
  background: var(--aos-panel-2);
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-left: 3px solid transparent;
  transition: background 0.2s;
}
.roadmap-card--active {
  border-left-color: var(--aos-signal-ok);
  background: var(--aos-panel-hover-1);
}
.roadmap-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.roadmap-phase {
  font-family: var(--aos-font-mono);
  font-size: var(--aos-fs-caption);
  font-weight: 700;
  color: var(--aos-accent-dim);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.roadmap-status {
  font-family: var(--aos-font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--aos-text-3);
  padding: 3px 8px;
  border: 1px solid var(--aos-line-strong);
  border-radius: 4px;
}
/* 当前阶段章 = 系统状态语义，用品牌语义色（signal-ok），不占 accent 岗位 */
.roadmap-status--active {
  color: var(--aos-signal-ok);
  border-color: var(--aos-signal-ok);
}
.roadmap-title {
  font-size: var(--aos-fs-title);
  font-weight: 700;
  color: var(--aos-text-1);
  margin: 0;
  line-height: 1.25;
}
.roadmap-items {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.roadmap-item {
  font-size: var(--aos-fs-body);
  color: var(--aos-text-dim);
  line-height: 1.5;
  padding-left: 16px;
  position: relative;
}
.roadmap-item::before {
  content: '—';
  position: absolute;
  left: 0;
  color: var(--aos-line-strong);
  font-family: var(--aos-font-mono);
}

/* ── CTA（accent 岗位①：健康面板变体——band 底 + accent 描边，绿色不铺大底） ── */
.section-cta {
  background: var(--aos-band);
  border-top: 1px solid var(--aos-line);
  padding: 88px 24px;
}
.cta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 64px;
  align-items: center;
  max-width: 1040px;
  margin: 0 auto;
  border: 1px solid var(--aos-accent-soft);
  background: var(--aos-panel-1);
  border-radius: 12px;
  padding: 48px;
}
.cta-h2 {
  font-size: var(--aos-fs-display-2);
  font-weight: 900;
  color: var(--aos-text-1);
  margin: 12px 0 16px;
  letter-spacing: -0.03em;
  line-height: 1.05;
}
.cta-sub {
  font-size: 15px;
  color: var(--aos-text-2);
  line-height: 1.7;
  margin: 0 0 16px;
}
.cta-note {
  font-size: 12px;
  color: var(--aos-text-dim);
  line-height: 1.7;
  margin: 0 0 32px;
}
.cta-btns { display: flex; gap: 12px; flex-wrap: wrap; }
/* 深底 LED 按钮：绿字 + 绿描边，保留「深底按钮」语义（候选二推导） */
.btn-dark {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  border-radius: 6px;
  background: var(--aos-bg);
  border: 1px solid var(--aos-accent-soft);
  color: var(--aos-accent);
  font-weight: 700;
  font-size: 14px;
  transition: border-color 0.15s, color 0.15s;
}
.btn-dark:hover { border-color: var(--aos-accent); color: var(--aos-accent-hover); }
.cta-terminal {
  border-radius: 10px;
  border: 1px solid var(--aos-on-accent-border);
  background: var(--aos-code-bg);
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}
.cta-terminal-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--aos-panel-2);
  border-bottom: 1px solid var(--aos-line);
}
.cta-code {
  padding: 24px 20px;
  font-family: var(--aos-font-mono);
  font-size: 13px;
  line-height: 1.75;
  margin: 0;
  overflow-x: auto;
}
.cta-line { color: var(--aos-code-text); white-space: pre; }
.cta-line--spacer { height: 0.5em; }
.code-comment { color: var(--aos-text-3); }
.code-prompt { color: var(--aos-accent); font-weight: 700; }

/* ── FOOTER ── */
.footer {
  background: var(--aos-bg);
  border-top: 1px solid var(--aos-line);
  padding: 32px 24px;
}
.footer-inner {
  max-width: 1040px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.footer-brand { display: flex; flex-direction: column; gap: 4px; }
.footer-logo {
  font-size: 18px;
  font-weight: 400;
  color: var(--aos-text-1);
  letter-spacing: -0.01em;
}
.footer-logo strong { font-weight: 900; color: var(--aos-accent); }
.footer-tagline {
  font-size: 12px;
  color: var(--aos-text-3);
}
.footer-links { display: flex; gap: 24px; }
.footer-link {
  font-size: 13px;
  color: var(--aos-text-3);
  transition: color 0.15s;
}
.footer-link:hover { color: var(--aos-accent); }

/* ── 响应式（900 / 768 / 480） ── */
@media (max-width: 900px) {
  .caps-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .runs-grid { grid-template-columns: minmax(0, 1fr); }
  .roadmap-grid { grid-template-columns: minmax(0, 1fr); }
  .cta-grid { grid-template-columns: minmax(0, 1fr); gap: 40px; padding: 32px 24px; }
}

@media (max-width: 768px) {
  .hero { padding: 72px 20px 64px; }
  /* :not(--zh)：中文标题有专属降档字号，别让本档覆盖把它顶回去（同特异性后行胜） */
  .hero-headline:not(.hero-headline--zh) { font-size: clamp(36px, 10vw, 56px); }
  .hero-headline--zh { font-size: clamp(30px, 8.4vw, 44px); }
  .section { padding: 64px 20px; }
  .section-cta { padding: 64px 20px; }
  .caps-grid { grid-template-columns: 1fr; }
  .footer-inner { flex-direction: column; gap: 20px; text-align: center; }
  .footer-links { justify-content: center; }
  /* 1040×560 SVG 整体缩放后文字 <5px 不可读：锁 960px 设计宽交给 .arch-diagram 横向滚动 */
  .arch-img { min-width: 960px; }
  .arch-hint { display: block; }
}

@media (max-width: 480px) {
  .hero-ctas { flex-direction: column; align-items: center; }
  .btn-primary,
  .btn-ghost { width: 200px; justify-content: center; }
}
</style>
