describe('Add Column With Url Of Matched Entities', () => {
    afterEach(() => {
    cy.addProjectForDeletion();
    });
    it('Add Column With Url Of Matched Entities', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');
    
    //we're using here the reconciled and mateched project, so rows are reconciled and matched to their best candidate
    cy.get('#project-tar-file-input').attachFile('openrefine-demo-sandbox-csv.openrefine.tar.gz.zip')
    cy.get('#import-project-button').click();
    cy.getCell(0, 'Item').should('to.contain', 'Choose new match');
    cy.columnActionClick('Item', [
    'Reconcile',
    'Actions',
    'Add column with URLs of matched entities',
    ]);
    cy.assertNotificationContainingText(
        'Create new column Url of Matched entities based on column Item by filling 4 rows with if(cell.recon.match!=null,"https://www.wikidata.org/wiki/{{id}}".replace("{{id}}",escape(cell.recon.match.id,"url")),"")',
    );
    cy.getCell(0, 'Url of Matched entities').should('to.contain', 'https://www.wikidata.org/wiki/Q3938');
    cy.getCell(1, 'Url of Matched entities').should('to.contain', '');
    cy.getCell(2, 'Url of Matched entities').should('to.contain', '');
    cy.getCell(3, 'Url of Matched entities').should('to.contain', '');
    });
    });
    