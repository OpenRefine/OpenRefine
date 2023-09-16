describe(__filename, function () {
    it('Ensure the wikibase extension is loaded by default', function () {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').should('to.contain', 'Wikibase');
        cy.get('#extension-bar-menu-container')
            .should('to.contain', 'Wikibase')
            .click();

        // we do only one assertion to ensure the menu appear properly
        cy.get('.menu-container').should('to.contain', 'Manage Wikibase instances');
    });
});
