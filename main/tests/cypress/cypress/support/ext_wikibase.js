/**
 * Return the .facets-container for a given facet name
 */
Cypress.Commands.add('addWikibaseInstance', (url) => {
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Manage Wikibase instances').click();

    cy.get('.wikibase-dialog .dialog-footer button')
        .contains('Add Wikibase')
        .click();

    // ad a manifest
    cy.get('.add-wikibase-dialog input[bind="manifestURLInput"]').invoke(
        'val',
        url
    );
    cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();

    cy.get('.wikibase-dialog .dialog-footer button').contains('Cancel').click();
});
