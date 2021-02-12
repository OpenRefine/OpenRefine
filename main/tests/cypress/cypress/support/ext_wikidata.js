/**
 * Return the .facets-container for a given facet name
 */
Cypress.Commands.add('addWikibaseInstance', (url) => {
  cy.get('#extension-bar-menu-container').contains('Wikidata').click();
  cy.get('.menu-container a').contains('Select Wikibase instance').click();

  cy.get('.dialog-container .wikibase-dialog button')
    .contains('Add Wikibase')
    .click();

  // ad a manifest
  cy.get('.add-wikibase-dialog input[bind="manifestURLInput"]').invoke(
    'val',
    url
  );
  cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();

  cy.get('.dialog-container .wikibase-dialog button').contains('Close').click();
});
