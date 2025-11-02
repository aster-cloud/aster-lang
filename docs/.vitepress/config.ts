import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'Aster Language',
  description: 'A pragmatic, safe, fast language with a human CNL surface',
  base: '/',
  ignoreDeadLinks: [
    // Ignore links to files outside docs directory (aster-vscode, etc.)
    (url) => url.includes('../../../'),
  ],
  markdown: {
    // 配置 Shiki 语法高亮主题
    theme: {
      light: 'github-light',
      dark: 'github-dark',
    },
    // 临时方案：将 aster 映射到 typescript 语法高亮
    // TODO: 等待 VitePress 2.x 支持自定义 tmLanguage 加载后改用 Aster 自定义语法
    languageAlias: {
      'aster': 'typescript'
    },
  },
  vite: {
    build: {
      // 增加 chunk size warning 阈值到 2000kb 以避免大型文档库的警告
      chunkSizeWarningLimit: 2000,
    },
  },
  themeConfig: {
    logo: '/logo.svg',
    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'Formatting', link: '/guide/formatting' },
      { text: 'Reference', link: '/reference/syntax' },
      { text: 'API', link: '/api/overview' },
      { text: 'GitHub', link: 'https://github.com/wontlost-ltd/aster-lang' },
    ],
    sidebar: {
      '/guide/': [
        {
          text: 'Guide',
          items: [
            { text: 'Getting Started', link: '/guide/getting-started' },
            { text: 'Language Overview', link: '/guide/language-overview' },
            { text: 'Examples', link: '/guide/examples' },
            { text: 'Formatting, LSP & CLI', link: '/guide/formatting' },
            { text: 'JVM Interop Overloads', link: '/guide/interop-overloads' },
            { text: 'Contributing', link: '/guide/contributing' },
          ],
        },
      ],
      '/reference/': [
        {
          text: 'Reference',
          items: [
            { text: 'Syntax', link: '/reference/syntax' },
            { text: 'Types', link: '/reference/types' },
            { text: 'Effects', link: '/reference/effects' },
          ],
        },
      ],
      '/api/': [
        {
          text: 'API',
          items: [
            { text: 'Overview', link: '/api/overview' },
            { text: 'Canonicalizer', link: '/api/canonicalizer' },
            { text: 'Lexer', link: '/api/lexer' },
            { text: 'Parser', link: '/api/parser' },
            { text: 'Core IR', link: '/api/core' },
            { text: 'Generated (TypeDoc)', link: '/api/typedoc/' },
          ],
        },
      ],
    },
    search:
      process.env.DOCSEARCH_APP_ID &&
      process.env.DOCSEARCH_API_KEY &&
      process.env.DOCSEARCH_INDEX_NAME
        ? {
            provider: 'algolia',
            options: {
              appId: process.env.DOCSEARCH_APP_ID,
              apiKey: process.env.DOCSEARCH_API_KEY,
              indexName: process.env.DOCSEARCH_INDEX_NAME,
            },
          }
        : {
            provider: 'local',
          },
    socialLinks: [{ icon: 'github', link: 'https://github.com/wontlost-ltd/aster-lang' }],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2025 Aster Language Team',
    },
  },
});
