describe('Create new item for each cell', () => {
    it('Test discard existing reconciliation judgments', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-automatch.zip')
        cy.get('#import-project-button').click();

        // quick check to ensure some cells are matched

        // Discard those judgments
        cy.columnActionClick('species', [
            'Reconcile',
            'Actions',
            'Create a new item for each cell',
        ]);

        // check notification and ensure no rows it matched anymore
        cy.assertNotificationContainingText('Mark to create new items for 6 cells in column species, one item for each cell');

        cy.getCell(0, 'species').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(1, 'species').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(2, 'species').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(3, 'species').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(4, 'species').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(5, 'species').find('.data-table-recon-new').should('to.contain', 'new');

    });
});
