describe('Create new item for each cell', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Test mark to create new items in many cells, previously reconciled', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('#or-import-locate').should('to.contain', 'Locate an existing Refine project file');

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

    it('Test mark to create new items in many cells, previously reconciled', () => {
        const fixture = [
          ['identifier'],
          ['2253634'],
          ['2328088'],
          ['2868241'],
          [null],
          ['8211794'],
          [null],
        ];

        cy.loadAndVisitProject(fixture);

        cy.columnActionClick('identifier', [
          'Reconcile',
          'Actions',
          'Create a new item for each cell',
        ]);

        cy.get('.dialog-container .dialog-footer button').contains('OK').click();

        // ensure column is reconciled
        cy.assertColumnIsReconciled('identifier');

        // ensure 4 rows are matched based on the identifier
        cy.getCell(0, 'identifier').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(1, 'identifier').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(2, 'identifier').find('.data-table-recon-new').should('to.contain', 'new');
        cy.getCell(4, 'identifier').find('.data-table-recon-new').should('to.contain', 'new');
    });
});

