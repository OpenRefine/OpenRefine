describe('Discard reconciliation judgments', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Test discard existing reconciliation judgments', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('#or-import-locate').should('to.contain', 'Locate an existing Refine project file');

        //we're using here the "automatched" project, to have some rows that are matched
        cy.get('#project-tar-file-input').attachFile('reconciled-project-automatch.zip')
        cy.get('#import-project-button').click();

        // quick check to ensure some cells are matched
        cy.getCell(0, 'species').should(
            'to.contain',
            'Choose new match'
        );
        cy.getCell(1, 'species').should(
            'to.contain',
            'Choose new match'
        );

        // Discard those judgments
        cy.columnActionClick('species', [
            'Reconcile',
            'Actions',
            'Discard reconciliation judgments',
        ]);

        // check notification and ensure no rows it matched anymore
        cy.assertNotificationContainingText('Discard recon judgments for 6 cells');
        // Check that all matches are gone (they contains Choose new match )
        cy.get('table.data-table td .data-table-cell-content').should(
            'not.to.contain',
            'Choose new match'
        );

    });
});
