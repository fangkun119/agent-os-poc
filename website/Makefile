# AgentOS 官网构建入口（website/CLAUDE.md「make 单一入口」：构建/预览一律走 make 目标）
# 指令文件命令红线：以下命令均可安全重复执行，无 clean/rm 类破坏性目标
.PHONY: install dev build preview

## install: 安装依赖
install:
	npm install

## dev: 启动本地开发服务器（默认 http://localhost:5173/）
dev:
	npm run dev

## build: 生产构建（验证定义：退出码 0 即通过）；末步补 .nojekyll——public/ 点文件不被
## copyPublicDir 拷贝，而 GitHub Pages 默认 Jekyll 会忽略下划线开头的 assets 路径，缺它全站 404
build:
	npm run build
	touch .vitepress/dist/.nojekyll

## preview: 本地预览生产构建产物（须配合人工/agent 目检，禁裸静态服务器）
preview:
	npm run preview