// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands';
import './openrefine_api';
import './ext_wikibase';

let token;

beforeEach(() => {
  cy.server({
    ignore: (xhr) => {
      // Hide XHR Requests from log, OpenRefine is making too many XHR requests, it's polluting the test runner
      return true;
    },
  });

  cy.wrap(token, { log: false }).as('token');
  cy.wrap(token, { log: false }).as('deletetoken');
  cy.wrap([], { log: false }).as('loadedProjectIds');
});

afterEach(() => {
  // DISABLE_PROJECT_CLEANUP is used to disable projects deletion
  // Mostly used in CI/CD for performances
  if(parseInt(Cypress.env('DISABLE_PROJECT_CLEANUP')) != 1){
    cy.cleanupProjects();
  }
});

before(() => {
  cy.request(
    Cypress.env('OPENREFINE_URL') + '/command/core/get-csrf-token'
  ).then((response) => {
    // store one unique token for block of runs
    token = response.body.token;
  });
});


// See https://docs.cypress.io/api/events/catalog-of-events#Uncaught-Exceptions
// We want to catch some Javascript exception that we consider harmless
Cypress.on('uncaught:exception', (err, runnable) => {
  // This message occasionally appears with edge
  // Doesn't seems like a blocket, and the test should not fail 
  if (err.message.includes("Cannot read property 'offsetTop' of undefined")
      || err.message.includes("Cannot read properties of undefined (reading 'offsetTop')")
    ) {
    return false
  }
  // we still want to ensure there are no other unexpected
  // errors, so we let them fail the test
})
