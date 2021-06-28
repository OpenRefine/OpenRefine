const VersionsArchived = require('./versionsArchived.json');

const isDeployPreview =
  process.env.NETLIFY && process.env.CONTEXT === 'deploy-preview';

// This probably only makes sense for the beta phase, temporary
function getNextBetaVersionName() {
  const expectedPrefix = '3.6.0-beta.';

  const lastReleasedVersion = versions[0];
  if (!lastReleasedVersion.includes(expectedPrefix)) {
    throw new Error(
      'this code is only meant to be used during the beta phase.',
    );
  }
  const version = parseInt(lastReleasedVersion.replace(expectedPrefix, ''), 10);
  return `${expectedPrefix}${version + 1}`;
}

// Special deployment for staging locales until they get enough translations
// https://app.netlify.com/sites/docusaurus-i18n-staging
// https://docusaurus-i18n-staging.netlify.app/
const isI18nStaging = process.env.I18N_STAGING === 'true';

const isVersioningDisabled = !!process.env.DISABLE_VERSIONING || isI18nStaging;

module.exports = {
  onBrokenLinks: 'error',
  onBrokenMarkdownLinks: 'error',
  title: 'OpenRefine',
  tagline: 'A power tool for working with messy data.',
  url: 'https://docs.openrefine.org/',
  baseUrl: '/',
  favicon: 'img/openrefine_logo.png',
  organizationName: 'OpenRefine', // Usually your GitHub org/user name.
  projectName: 'OpenRefine', // Usually your repo name.
  i18n: {
    defaultLocale: 'en',
    locales: isDeployPreview
      ? // Deploy preview: keep it fast!
        ['en']
      : isI18nStaging
      ? // Staging locales: https://openrefine-i18n-staging.netlify.app/
        ['en', 'fr']
      : // Production locales
        ['en', 'fr', 'ko', 'zh-CN'],
  },
  themeConfig: {
    navbar: {
      title: 'OpenRefine Documentation',
      logo: {
        alt: 'OpenRefine diamond logo',
        src: 'img/openrefine_logo.png',
      },
      items: [
        // left side of nav bar
        {
          to: '/',
          activeBasePath: 'docs',
          label: 'User Manual',
          position: 'left',
        },
        {to: 'technical-reference/technical-reference-index',
         label: 'Technical Reference',
         position: 'left'},
         // right side of nav bar
        {
          type: 'docsVersionDropdown',
          position: 'right',
          dropdownActiveClassDisabled: true,
          dropdownItemsAfter: [
            ...Object.entries(VersionsArchived).map(
              ([versionName, versionUrl]) => ({
                label: versionName,
                href: versionUrl,
              }),
            ),
            {
              href: 'https://v1.docusaurus.io',
              label: '1.x.x',
            },
            {
              to: '/versions',
              label: 'All versions',
            },
          ],
        },
        {
          type: 'localeDropdown',
          position: 'right',
          dropdownItemsAfter: [
            {
              href: 'technical-reference/translating',
              label: 'Help Us Translate',
            },
          ],
        },
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

          /*
          editUrl: ({locale, docPath}) => {
            if (locale !== 'en') {
              return `https://crowdin.com/project/openrefine/${locale}`;
            }
            // We want users to submit doc updates to the upstream/next version!
            // Otherwise we risk losing the update on the next release.
            const nextVersionDocsDirPath = 'docs';
            return `https://github.com/OpenRefine/OpenRefine/edit/master/docs/${nextVersionDocsDirPath}/${docPath}`;
          },
          */

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
  scripts: [
    {
      src: '/js/fix-location.js',
      async: false,
      defer: false,
    },
  ],
};
