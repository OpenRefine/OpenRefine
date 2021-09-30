describe(__filename, function () {
  it('it collapses all columns', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('NDB_No', ['View', 'Collapse this column']);

    cy.get('.odd td:nth-child(4)').should('to.contain', '');

    cy.get('.even td:nth-child(4)').should('to.contain', '');

    cy.get('tr').children().first().next().should('to.contain', '');
  });
});
