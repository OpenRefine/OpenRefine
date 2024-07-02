describe('In-cell reconciliation buttons', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });

    it('Show "Search for match" even if there are no candidates', () => {
          cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        //we're using here the "no automatch" project, so rows are reconciled and we have more than 3 matched candidates
        cy.get('#project-tar-file-input').attachFile('reconciled-project-no-match.zip')
        cy.get('#import-project-button').click();

        cy.getCell(0, 'entity').find('.data-table-recon-visibility').should('not.contain', 'See more');
        cy.getCell(0, 'entity').find('.data-table-recon-visibility').should('to.contain', 'Search for match');
    });
    
    it('Display see more / see less when there are candidates', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        //we're using here the "no automatch" project, so rows are reconciled and we have more than 3 matched candidates
        cy.get('#project-tar-file-input').attachFile('reconciled-project-no-automatch.zip')
        cy.get('#import-project-button').click();

        //confirming that we have see more options
        cy.getCell(0, 'species').find('.data-table-recon-visibility').should('to.contain', 'See more');
        cy.getCell(1, 'species').find('.data-table-recon-visibility').should('to.contain', 'See more');
        cy.getCell(2, 'species').find('.data-table-recon-visibility').should('to.contain', 'See more');
        cy.getCell(3, 'species').find('.data-table-recon-visibility').should('to.contain', 'See more');
        cy.getCell(4, 'species').find('.data-table-recon-visibility').should('to.contain', 'See more');
        cy.getCell(5, 'species').find('.data-table-recon-visibility').should('to.contain', 'See more');

        //confirming the initial no. of candidates
        cy.getCell(0, 'species').find('.data-table-recon-candidate:visible').should('have.length', 4);

        //clicking the see more option
        cy.getCell(0, 'species').find('.data-table-recon-visibility').contains('See more').click();

        //confirming the no. of candidates after we click on see more
        cy.getCell(0, 'species').find('.data-table-recon-candidate:visible').should('have.length', 6);
        
        //verifying that the see more option has changed to see less
        cy.getCell(0, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');

        //clicking the see less option
        cy.getCell(0, 'species').find('.data-table-recon-visibility').contains('See less').click();

        //confirming the no. of candidates after we click on see less
        cy.getCell(0, 'species').find('.data-table-recon-candidate:visible').should('have.length', 4);
    });

    it('Matching a single cell preserves pagination', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');
        cy.get('#project-tar-file-input').attachFile('reconciled-project-no-automatch.zip')
        cy.get('#import-project-button').click();

        // Set a smaller page number to make sure there are multiple pages
        cy.get('.viewPanel-pagingControls-page:nth-of-type(1)').click();
        // Go to the next page
        cy.get('.viewpanel-paging .action').first().click();
        // Check that we have a single row in view (second page)
        cy.get('.data-table tbody tr').should('have.length', 1);

        // Match the first recon candidate in the cell
        cy.get('.data-table-recon-match').first().click();

        // Make sure the cell is matched (no recon candidates are visible anymore)
        cy.get('.data-table-recon-match').should('not.exist');
        // We are still in the second page
        cy.get('.data-table tbody tr').should('have.length', 1);
    });

});


