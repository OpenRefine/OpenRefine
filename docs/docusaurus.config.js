module.exports = {
  onBrokenLinks: 'error',
  title: 'OpenRefine',
  tagline: 'A power tool for working with messy data.',
  url: 'https://docs.openrefine.org/',
  baseUrl: '/',
  favicon: 'img/openrefine_logo.png',
  organizationName: 'OpenRefine', // Usually your GitHub org/user name.
  projectName: 'OpenRefine', // Usually your repo name.
  themeConfig: {
    navbar: {
      title: 'OpenRefine Documentation',
      logo: {
        alt: 'OpenRefine diamond logo',
        src: 'img/openrefine_logo.png',
      },
      items: [
        {
          to: '/',
          activeBasePath: 'docs',
          label: 'User Manual',
          position: 'left',
        },
        {to: 'technical-reference/technical-reference-index',
         label: 'Technical Reference',
         position: 'left'},
        {
          href: 'https://github.com/OpenRefine/OpenRefine/edit/master/docs',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    algolia: {
	    apiKey: '591fc612419d2e5b6bee6822cc17064f',
	    indexName: 'openrefine',
	    contextualSearch: true,
    },
    footer: {
      logo: {
        alt: 'OpenRefine diamond logo',
        src: 'img/openrefine_logo.png',
        href: 'https://docs.openrefine.org',
      },
      style: 'dark',
      links: [
        {
          title: 'Community',
          items: [
            {
              label: 'Mailing List',
              href: 'http://groups.google.com/group/openrefine/'
            },
            {
              label: 'Gitter Chat',
              href: 'https://gitter.im/OpenRefine/OpenRefine',
            },
            {
              label: 'Twitter',
              href: 'https://twitter.com/openrefine',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Official Website',
              href: 'https://openrefine.org',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/OpenRefine/OpenRefine',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} OpenRefine contributors`,
    },
  },
  themes: [],
  plugins: [],
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          // Docs folder path relative to website dir. Equivalent to `customDocsPath`.
          // path: 'docs',
          // Sidebars file relative to website dir.
          sidebarPath: require.resolve('./sidebars.js'),
          // Equivalent to `editUrl` but should point to `website` dir instead of `website/docs`.
          editUrl: 'https://github.com/OpenRefine/OpenRefine/edit/master/docs',
          // Equivalent to `docsUrl`.
          routeBasePath: '/',
          // Remark and Rehype plugins passed to MDX. Replaces `markdownOptions` and `markdownPlugins`.
          remarkPlugins: [],
          rehypePlugins: [],
          // Equivalent to `enableUpdateBy`.
          showLastUpdateAuthor: true,
          // Equivalent to `enableUpdateTime`.
          showLastUpdateTime: true,
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
