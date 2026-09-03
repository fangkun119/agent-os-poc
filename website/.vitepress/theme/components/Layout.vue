<!-- 默认布局薄封装：预留主题插槽定制点。
     当前唯一用途：非主页（未来的文档页）压制 doc-after 缺省内容，主页不受影响。
     import 必须与主题入口同为 theme-without-fonts，否则字体资产仍会被打进产物 -->
<template>
  <DefaultTheme.Layout>
    <template v-if="frontmatter.layout !== 'home'" #doc-after></template>
  </DefaultTheme.Layout>
</template>

<script setup>
import DefaultTheme from 'vitepress/theme-without-fonts'
import { useData } from 'vitepress'

const { frontmatter } = useData()
</script>
