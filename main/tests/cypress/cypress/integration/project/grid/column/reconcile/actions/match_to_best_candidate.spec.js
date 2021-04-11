describe('Match each cell to its best candidate', () => {
  it('Match each cell to its best candidate', () => {
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
    // here reconcileColumn is called with automatch = false
    cy.reconcileColumn('species', false);
    cy.assertColumnIsReconciled('species');

    // before matching, ensure we have no matches
    cy.get(
      'table.data-table td .data-table-cell-content:contains("Search for match")'
    ).should('have.length', 6);

    cy.columnActionClick('species', [
      'Reconcile',
      'Actions',
      'Match each cell to its best candidate',
    ]);

    cy.get(
      'table.data-table td .data-table-cell-content:contains("Choose new match")'
    ).should('have.length', 6);
  });
});
