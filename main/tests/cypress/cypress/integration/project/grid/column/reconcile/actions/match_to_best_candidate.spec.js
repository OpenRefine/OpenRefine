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
    cy.getCell(0, 'species').should('to.contain', 'Search for match');
    cy.getCell(1, 'species').should('to.contain', 'Search for match');
    cy.getCell(2, 'species').should('to.contain', 'Search for match');
    cy.getCell(3, 'species').should('to.contain', 'Search for match');
    cy.getCell(4, 'species').should('to.contain', 'Search for match');
    cy.getCell(5, 'species').should('to.contain', 'Search for match');

    cy.columnActionClick('species', [
      'Reconcile',
      'Actions',
      'Match each cell to its best candidate',
    ]);

    cy.assertNotificationContainingText(
      'Match each of 6 cells to its best candidate'
    );

    // ensure all cells contains 'Choose new match'
    // which means they are matched
    cy.getCell(0, 'species').should('to.contain', 'Choose new match');
    cy.getCell(1, 'species').should('to.contain', 'Choose new match');
    cy.getCell(2, 'species').should('to.contain', 'Choose new match');
    cy.getCell(3, 'species').should('to.contain', 'Choose new match');
    cy.getCell(4, 'species').should('to.contain', 'Choose new match');
    cy.getCell(5, 'species').should('to.contain', 'Choose new match');
  });
});
