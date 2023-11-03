describe('Match each cell to its best candidate', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Match each cell to its best candidate', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        //we're using here the "no automatch" project, so rows are reconciled, but nothing has been matched yet
        cy.get('#project-tar-file-input').attachFile('reconciled-project-no-automatch.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'species').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'species').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'species').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'species').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(4, 'species').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(5, 'species').find('.data-table-recon-candidate').should('to.exist');

        // Match all
        cy.columnActionClick('species', [
            'Reconcile',
            'Actions',
            'Match each cell to its best candidate',
        ]);

        cy.assertNotificationContainingText(
            'Match each of 6 cells to its best candidate'
        );

        // ensure all cells contains 'Choose new match'
        // which means they are matched
        cy.getCell(0, 'species').should('to.contain', 'Choose new match');
        cy.getCell(1, 'species').should('to.contain', 'Choose new match');
        cy.getCell(2, 'species').should('to.contain', 'Choose new match');
        cy.getCell(3, 'species').should('to.contain', 'Choose new match');
        cy.getCell(4, 'species').should('to.contain', 'Choose new match');
        cy.getCell(5, 'species').should('to.contain', 'Choose new match');
    });
});


