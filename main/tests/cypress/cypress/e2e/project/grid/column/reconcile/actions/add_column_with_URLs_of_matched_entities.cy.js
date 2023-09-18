describe('Add column with URLs of matched entities', () => {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  it('Add column with URLs of matched entities', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');
    
    // we're using here the reconciled and matched project, so rows are reconciled and matched to their best candidate
    cy.get('#project-tar-file-input').attachFile('openrefine-demo-sandbox-csv.openrefine.tar.gz.zip')
    cy.get('#import-project-button').click();
    cy.getCell(0, 'Item').should('to.contain', 'Choose new match');
    cy.columnActionClick('Item', [
      'Reconcile',
      'Add column with URLs of matched entities',
    ]);

    // check the dialog, enter a new column name "id_column"
    cy.get('.dialog-container .dialog-header').should(
      'to.contain',
      'Add a name for the column containing URL of matched entities'
    );
    cy.get('.dialog-container .dialog-body input').type('Entity URL');
    cy.get('.dialog-container .dialog-footer button').contains('OK').click();
    cy.assertNotificationContainingText(
      'Create new column Entity URL based on column Item by filling 1 rows with if(cell.recon.match!=null,"https://www.wikidata.org/wiki/{{id}}".replace("{{id}}",escape(cell.recon.match.id,"url")),null)',
    );

    cy.getCell(0, 'Entity URL').should('to.contain','https://www.wikidata.org/wiki/Q3938' );
    cy.assertCellEquals(1,'Entity URL','null');
    cy.assertCellEquals(2,'Entity URL','null');
    cy.assertCellEquals(3,'Entity URL','null');

  });
});
    
