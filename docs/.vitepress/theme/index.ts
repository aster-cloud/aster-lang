import DefaultTheme from 'vitepress/theme';
import { h } from 'vue';
import StarCallout from './components/StarCallout.vue';
import { useData } from 'vitepress';

export default {
  extends: DefaultTheme,
  Layout: () => {
    return h(DefaultTheme.Layout, null, {
      'sidebar-top': () =>
        h(
          'div',
          {
            class: 'vp-docs-badges',
            style: 'padding: 8px 12px;'
          },
          [
            h(
              'a',
              {
                href: 'https://github.com/wontlost-ltd/aster-lang/releases',
                target: '_blank',
                rel: 'noreferrer noopener',
                style: 'display:inline-block;margin-right:8px;'
              },
              [
                h('img', {
                  alt: 'Latest Release',
                  src: 'https://img.shields.io/github/v/release/wontlost-ltd/aster-lang?display_name=tag',
                }),
              ]
            ),
            h(
              'a',
              {
                href: 'https://github.com/wontlost-ltd/aster-lang',
                target: '_blank',
                rel: 'noreferrer noopener',
                style: 'display:inline-block;'
              },
              [
                h('img', {
                  alt: 'GitHub Stars',
                  src: 'https://img.shields.io/github/stars/wontlost-ltd/aster-lang?style=social',
                }),
              ]
            ),
          ]
        ),
      'doc-after': () => {
        const { page } = useData();
        const path = page.value.relativePath || '';
        if (path.startsWith('guide/') || path.startsWith('reference/')) {
          return h(StarCallout);
        }
        return null;
      },
    });
  },
};
