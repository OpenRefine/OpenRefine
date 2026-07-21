const { defineConfig } = require('cypress')

module.exports = defineConfig({
  keystrokeDelay: 0,
  viewportWidth: 1280,
  viewportHeight: 768,
  retries: {
    runMode: 3,
    openMode: 0,
  },
  env: {
    OPENREFINE_URL: 'http://localhost:3333',
    DISABLE_PROJECT_CLEANUP: 0,
  },
  expose: {
    OPENREFINE_URL: 'http://localhost:3333',
    DISABLE_PROJECT_CLEANUP: 0,
  },
  allowCypressEnv: false,
  e2e: {
    experimentalRunAllSpecs: true,
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      // Bridge env vars to expose so browser code can use Cypress.expose()
      config.expose = {
        ...config.expose,
        OPENREFINE_URL: config.env.OPENREFINE_URL,
        DISABLE_PROJECT_CLEANUP: config.env.DISABLE_PROJECT_CLEANUP,
      }
      return require('./cypress/plugins/index.js')(on, config)
    },
    specPattern: './cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
  },
})
