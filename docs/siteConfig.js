// Documentation for these options can be found at:
// https://docusaurus.io/docs/en/site-config

const siteConfig = {
  title: 'OpenRefine', // Title for your website.
  tagline: 'A power tool for working with messy data.',
  url: 'https://docs.openrefine.org/',
  baseUrl: '/',

  projectName: 'OpenRefine',
  organizationName: 'OpenRefine',

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    {doc: 'index', label: 'Home'},
  ],

  users: [],
  customDocsPath: 'docs/src',
  docsUrl: '',

  /* path to images for header/footer */
  headerIcon: 'img/openrefine_logo.png',
  footerIcon: 'img/openrefine_logo.png',
  favicon: 'img/openrefine_logo.png',

  /* Colors for website */
  colors: {
    primaryColor: '#196581',
    secondaryColor: '#5a4411',
  },

  /* Custom fonts for website */
  /*
  fonts: {
    myFont: [
      "Times New Roman",
      "Serif"
    ],
    myOtherFont: [
      "-apple-system",
      "system-ui"
    ]
  },
  */

  copyright: `Copyright © ${new Date().getFullYear()} OpenRefine contributors`,

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks.
    theme: 'default',
  },

  // Add custom scripts here that would be placed in <script> tags.
  scripts: ['https://buttons.github.io/buttons.js'],

  // On page navigation for the current documentation page.
  onPageNav: 'separate',
  // No .html extensions for paths.
  cleanUrl: false,

  // Open Graph and Twitter card images.
  ogImage: 'img/openrefine_logo.png',
  twitterImage: 'img/openrefine_logo.png',

  // Expand/collapse the links and subcategories under categories.
  docsSideNavCollapsible: true,

  // Show documentation's last contributor's name.
  // enableUpdateBy: true,

  // Show documentation's last update time.
  // enableUpdateTime: true,

  // You may provide arbitrary config keys to be used as needed by your
  // template. For example, if you need your repo's URL...
  repoUrl: 'https://github.com/OpenRefine/OpenRefine',
  twitterUsername: 'OpenRefine'
};

module.exports = siteConfig;
