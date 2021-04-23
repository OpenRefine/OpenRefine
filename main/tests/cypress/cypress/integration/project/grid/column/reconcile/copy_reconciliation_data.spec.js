describe('Copy reconciliation data', () => {
  it('Copy reconciliation data from species to species_copy', () => {
    const fixture = [
      ['species_original', 'species_copy'],
      ['Hypsibius dujardini', 'Hypsibius dujardini'],
      ['Protula bispiralis', 'Protula bispiralis'],
      [null, 'Hypsibius dujardini'], // new line to ensure copy is done one multiple rows
    ];

    cy.loadAndVisitProject(fixture);
    cy.reconcileColumn('species_original');
    cy.assertColumnIsReconciled('species_original');

    cy.columnActionClick('species_original', [
      'Reconcile',
      'Copy reconciliation data',
    ]);

    // check the dialog, enter a new column name "id_column"
    cy.get('.dialog-container .dialog-header').should(
      'to.contain',
      'Copy recon judgments from column species_original'
    );
    cy.get('.dialog-container select[bind="toColumnSelect"]').select(
      'species_copy'
    );

    //
    // cy.assertColumnIsReconciled('species_copy');

    cy.get('.dialog-container .dialog-footer button').contains('Copy').click();

    cy.assertNotificationContainingText(
      'Copy 3 recon judgments from column species_original to species_copy'
    );

    // ensure 5 rows are matched based on the identifier
    // 2 on the original column, 3 on the copy
    cy.get(
      'table.data-table td .data-table-cell-content:contains("Choose new match")'
    ).should('have.length', 5);
  });
});
