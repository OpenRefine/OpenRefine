describe(__filename, function () {
  it('it collapses all columns', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['View', 'Collapse all columns']);

    cy.get('.odd td:nth-child(4)').should('to.contain', '');
    cy.get('.odd td:nth-child(5)').should('to.contain', '');
    cy.get('.odd td:nth-child(6)').should('to.contain', '');
    cy.get('.odd td:nth-child(7)').should('to.contain', '');

    cy.get('.even td:nth-child(4)').should('to.contain', '');
    cy.get('.even td:nth-child(5)').should('to.contain', '');
    cy.get('.even td:nth-child(6)').should('to.contain', '');
    cy.get('.even td:nth-child(7)').should('to.contain', '');

    cy.get('tr').children().first().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().next().should('to.contain', '');
    cy.get('tr').children().first().next().next().next().next().should('to.contain', '');
  });
});
