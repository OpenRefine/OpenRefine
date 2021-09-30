describe(__filename, function () {
  it('it collapses all columns to right', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('NDB_No', ['View', 'Collapse all columns to right']);

    cy.get('.odd td:nth-child(5)').should('to.contain', '');
    cy.get('.odd td:nth-child(6)').should('to.contain', '');
    cy.get('.odd td:nth-child(7)').should('to.contain', '');

    cy.get('.even td:nth-child(5)').should('to.contain', '');
    cy.get('.even td:nth-child(6)').should('to.contain', '');
    cy.get('.even td:nth-child(7)').should('to.contain', '');

    cy.get('tr').children().first().next().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().next().next().should('to.contain', '');

  });
});
