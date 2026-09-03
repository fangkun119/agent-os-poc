// 自定义主题入口：扩展默认主题并注册主页组件。
// 组件文件名与注册点 'Home' 是硬依赖（与 index.md 里的标签字符串对应），禁重命名。
// 用 theme-without-fonts：本站纯系统字体栈，避免默认入口把自带 Inter 字体打进产物
import DefaultTheme from 'vitepress/theme-without-fonts'
import './custom.css'
import Home from './components/Home.vue'
import Layout from './components/Layout.vue'

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp({ app }) {
    app.component('Home', Home)
  },
}
