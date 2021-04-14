describe('Discard reconciliation judgments', () => {
  it('Test discard existing reconciliation judgments', () => {
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
      'Actions',
      'Discard reconciliation judgments',
    ]);

    cy.assertNotificationContainingText('Discard recon judgments for 6 cells');

    // Check that all matches are gone (they contains Choose new match )
    cy.get('table.data-table td .data-table-cell-content').should(
      'not.to.contain',
      'Choose new match'
    );

    // ensure none of the cells contains 'Search for match'
    // which means they are not matched and reconciliation judgments are gone
    cy.getCell(0, 'species').should('to.contain', 'Search for match');
    cy.getCell(1, 'species').should('to.contain', 'Search for match');
    cy.getCell(2, 'species').should('to.contain', 'Search for match');
    cy.getCell(3, 'species').should('to.contain', 'Search for match');
    cy.getCell(4, 'species').should('to.contain', 'Search for match');
    cy.getCell(5, 'species').should('to.contain', 'Search for match');
  });
});
