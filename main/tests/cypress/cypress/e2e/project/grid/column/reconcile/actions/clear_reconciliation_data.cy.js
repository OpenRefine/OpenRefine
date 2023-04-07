describe('Clear reconciliation data', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Test clearing reconciliation for a reconciled dataset', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('#or-import-locate').should('to.contain', 'Locate an existing Refine project file');
        cy.get('#project-tar-file-input').attachFile('reconciled-project-automatch.zip')
        cy.get('#import-project-button').click();

        cy.columnActionClick('species', [
            'Reconcile',
            'Actions',
            'Clear reconciliation data',
        ]);

        cy.get('table.data-table').should('not.to.contain', 'Choose new match');
        cy.get('table.data-table').should('not.to.contain', 'Create new item');

        // the green bar for matched item should be invisible
        cy.get(
            'table.data-table thead div.column-header-recon-stats-matched'
        ).should('not.be.visible');
    });
});
