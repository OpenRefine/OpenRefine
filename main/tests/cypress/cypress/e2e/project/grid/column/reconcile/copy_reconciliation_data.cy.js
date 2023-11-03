describe('Copy reconciliation data', () => {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Copy reconciliation data from species to species_copy', () => {
    cy.visitOpenRefine();
    cy.navigateTo('Import project');
    cy.get('#or-import-locate').should('to.contain', 'Locate an existing Refine project file');

    //we're using here the "automatched" project, so we can test that the facet contains choice for matched and non-matched judgments
    cy.get('#project-tar-file-input').attachFile('reconciled-project-automatch.zip')
    cy.get('#import-project-button').click();


    // Step 1, Duplicate the "species" column
    cy.columnActionClick('species', [
      'Edit column',
      'Add column based on this column…',
    ]);
    cy.waitForDialogPanel();
    cy.get('input[bind="columnNameInput"]').type('duplicated_column');
    cy.confirmDialogPanel();

    cy.columnActionClick('species', [
      'Reconcile',
      'Copy reconciliation data',
    ]);

    // check the dialog, enter a new column name "id_column"
    cy.get('.dialog-container .dialog-header').should(
      'to.contain',
      'Copy recon judgments from column species'
    );
    cy.get('.dialog-container select[bind="toColumnSelect"]').select(
      'duplicated_column'
    );

    cy.get('.dialog-container .dialog-footer button').contains('Copy').click();

    cy.assertNotificationContainingText(
      'Copy 4 recon judgments from column species to duplicated_column'
    );

    // ensure 4 are matched on the duplicate column
    cy.get(
      'table.data-table td .data-table-cell-content:contains("Choose new match")'
    ).should('have.length', 4);
  });
});
