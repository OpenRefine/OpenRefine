describe('Show more or less reconciliation candidates', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Testing see more / see less', () => {
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
});


