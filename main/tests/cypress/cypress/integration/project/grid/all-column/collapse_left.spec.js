describe(__filename, function () {
  it('it collapses all columns to left', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('Energ_Kcal', ['View', 'Collapse all columns to left']);

    cy.get('.odd td:nth-child(4)').should('to.contain', '');
    cy.get('.odd td:nth-child(5)').should('to.contain', '');
    cy.get('.odd td:nth-child(6)').should('to.contain', '');

    cy.get('.even td:nth-child(4)').should('to.contain', '');
    cy.get('.even td:nth-child(5)').should('to.contain', '');
    cy.get('.even td:nth-child(6)').should('to.contain', '');

    cy.get('tr').children().first().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().next().should('to.contain', '');

  });
});
