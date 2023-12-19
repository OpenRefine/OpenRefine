describe('Match each cell to its best candidate', () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Match each cell to its best candidate', () => {
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

        //clicking the see more option
        cy.getCell(0, 'species').find('.data-table-recon-visibility').click();
        cy.getCell(1, 'species').find('.data-table-recon-visibility').click();
        cy.getCell(2, 'species').find('.data-table-recon-visibility').click();
        cy.getCell(3, 'species').find('.data-table-recon-visibility').click();
        cy.getCell(4, 'species').find('.data-table-recon-visibility').click();
        cy.getCell(5, 'species').find('.data-table-recon-visibility').click();

        //verifying that the see more option has changed to see less
        cy.getCell(0, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');
        cy.getCell(1, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');
        cy.getCell(2, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');
        cy.getCell(3, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');
        cy.getCell(4, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');
        cy.getCell(5, 'species').find('.data-table-recon-visibility').should('to.contain', 'See less');

        
       
        
        
    });
});


