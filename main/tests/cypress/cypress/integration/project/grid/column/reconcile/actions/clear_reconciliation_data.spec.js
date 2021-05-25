describe('Clear reconciliation data', () => {
  it('Test clearing reconciliation for a reconciled dataset', () => {
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
      'Clear reconciliation data',
    ]);

    cy.get('table.data-table').should('not.to.contain', 'Choose new match');
    // the green bar for matched item should be invisible
    cy.get(
      'table.data-table thead div.column-header-recon-stats-matched'
    ).should('not.be.visible');
  });
});
