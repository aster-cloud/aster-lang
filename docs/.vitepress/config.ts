import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'Aster Language',
  description: 'A pragmatic, safe, fast language with a human CNL surface',
  base: '/',
  themeConfig: {
    logo: '/logo.svg',
    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'Reference', link: '/reference/syntax' },
      { text: 'API', link: '/api/overview' },
      { text: 'GitHub', link: 'https://github.com/aster-lang/aster' },
    ],
    sidebar: {
      '/guide/': [
        {
          text: 'Guide',
          items: [
            { text: 'Getting Started', link: '/guide/getting-started' },
            { text: 'Language Overview', link: '/guide/language-overview' },
            { text: 'Examples', link: '/guide/examples' },
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
    socialLinks: [{ icon: 'github', link: 'https://github.com/aster-lang/aster' }],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2025 Aster Language Team',
    },
  },
});
