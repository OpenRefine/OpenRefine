describe('Add entity identifiers', () => {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Add a new column that contains the reconciliation id', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('#or-import-locate').should('to.contain', 'Locate an existing Refine project file');

    //we're using here the "automatched" project, so we can test that the facet contains choice for matched and non-matched judgments
    cy.get('#project-tar-file-input').attachFile('reconciled-project-automatch.zip')
    cy.get('#import-project-button').click();

    cy.columnActionClick('species', [
      'Reconcile',
      'Add entity identifiers column',
    ]);

    // check the dialog, enter a new column name "id_column"
    cy.get('.dialog-container .dialog-header').should(
      'to.contain',
      'Add column containing entity identifiers on species'
    );
    cy.get('.dialog-container .dialog-body input').type('id_column');
    cy.get('.dialog-container .dialog-footer input').contains('OK').click();

    // Check the cells content for the new column
    cy.assertCellEquals(0, 'id_column', '2253634'); // untouched
    cy.assertCellEquals(1, 'id_column', '2328088'); // blanked
    cy.assertCellEquals(2, 'id_column', '2868241'); // untouched
    cy.assertCellEquals(3, 'id_column', null); // untouched
    cy.assertCellEquals(4, 'id_column', '8211794'); // blanked
    cy.assertCellEquals(5, 'id_column', null); // blanked
  });
});
