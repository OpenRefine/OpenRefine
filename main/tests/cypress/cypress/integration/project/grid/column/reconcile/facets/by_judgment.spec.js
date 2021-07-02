describe('Facet by judgment', () => {
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

    // cleanup automatic facets before doing any testing
    cy.get('#or-proj-facFil').click();
    cy.get('div.browsing-panel-controls').should('be.visible');
    cy.get('#refine-tabs-facets a')
      .contains('Remove All')
      .should('be.visible')
      .click();

    cy.columnActionClick('species', ['Reconcile', 'Facets', 'By judgment']);

    // ensure a new facet has been added
    cy.getFacetContainer('species: judgment').should('exist');
  });
});
