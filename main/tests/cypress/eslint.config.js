const js = require('@eslint/js');
const google = require('eslint-config-google');
const prettier = require('eslint-config-prettier');
const cypress = require('eslint-plugin-cypress');
const globals = require('globals');

const googleRules = { ...google.rules };
delete googleRules['require-jsdoc'];
delete googleRules['valid-jsdoc'];

module.exports = [
  {
    ignores: ['out/**', 'node_modules/**', '*.md'],
  },
  js.configs.recommended,
  {
    languageOptions: {
      ecmaVersion: 2021,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
        ...globals.es2021,
      },
    },
    // eslint-config-google still enables two JSDoc rules removed from ESLint.
    rules: googleRules,
  },
  cypress.configs.recommended,
  {
    rules: {
      'max-len': ['error', { code: 150 }],
      'cypress/no-assigning-return-values': 'error',
      'cypress/no-unnecessary-waiting': 'error',
      'cypress/assertion-before-screenshot': 'warn',
      'cypress/no-force': 'warn',
      'cypress/no-async-tests': 'error',
      'cypress/unsafe-to-chain-command': 'error',
    },
  },
  prettier,
];
