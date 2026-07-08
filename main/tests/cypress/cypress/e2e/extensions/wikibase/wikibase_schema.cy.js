// add test for SchemaAlignment wikibase schema
describe('SchemaAlignment.setUpTabs', () => {

    it('should create tabs', () => {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Edit Wikibase schema').click();

        // Check tabs
        cy.get('.main-view-panel-tab-header').should('to.exist');
        cy.get('.main-view-panel-tab-header').should('to.have.length', 4);
        cy.get('#wikibase-schema-panel').should('to.exist');
        cy.get('#wikibase-issues-panel').should('to.exist');
        cy.get('#wikibase-preview-panel').should('to.exist');
        cy.get('.schema-alignment-total-warning-count').should('to.exist');          
    });

    it('should show the correct number of warnings', () => {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Edit Wikibase schema').click();

        // Check warnings
        cy.get('.schema-alignment-total-warning-count').should('to.contain', '1');
    });

    // add 2 item in schema and check if issue count is updated
    it('should update the number of warnings', () => {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Edit Wikibase schema').click();

        // Check warnings
        cy.get('.schema-alignment-total-warning-count').should('to.contain', '1');


        cy.get('#wikibase-schema-panel').click();

        // Add 2 items
        cy.get('.wbs-toolbar button').click();
        cy.get('.wbs-toolbar button').click();

        // Check warnings
        cy.get('.schema-alignment-total-warning-count').should('to.contain', '2');
    });

    // Regression test for #7609: the statement-configuration dialog must focus
    // its first input on open so the user can interact via the keyboard
    // without having to click into the dialog first.
    it('focuses the mode select when the statement configuration dialog opens', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('#project-tar-file-input').selectFile('cypress/fixtures/wikidata-schema.tar.gz.zip');
        cy.get('#import-project-button').click();

        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Edit Wikibase schema').click();
        cy.get('#wikibase-schema-panel').click();

        cy.get('.wbs-configure').first().click();
        cy.get('.dialog-frame').should('be.visible');
        cy.focused().should('have.id', 'modeId');
    });

  });
