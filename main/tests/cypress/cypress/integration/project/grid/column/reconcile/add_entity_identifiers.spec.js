describe('Add entity identifiers', () => {
  it('Add a new column that contains the reconciliation id', () => {
    const fixture = [
      ['record_id', 'date', 'location', 'species'],
      ['1', '2017-06-23', 'Maryland', 'Hypsibius dujardini'],
      ['2', '2018-06-09', 'South Carolina', 'Protula bispiralis'],
      ['3', '2018-06-09', 'West Virginia', 'Monstera deliciosa'],
      ['15', '2018-09-06', 'West Virginia', 'Bos taurus'],
      ['16', '2017-10-05', 'Maryland', 'Amandinea devilliersiana'],
      ['24', '2015-05-01', 'West Virginia', 'Faciolela oxyrynca'],
    ];

    cy.loadAndVisitProject(fixture);
    cy.reconcileColumn('species');
    cy.assertColumnIsReconciled('species');

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
