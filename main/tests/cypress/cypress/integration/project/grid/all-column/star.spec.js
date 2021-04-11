describe(__filename, function () {
  it('Ensure star is visible and toggle on/off', function () {
    cy.loadAndVisitProject('food.mini');
    cy.get('.data-table tr:nth-child(1) td:nth-child(1) a')
      .should('have.class', 'data-table-star-off')
      .click();
    cy.assertNotificationContainingText('Star row 1');

    cy.get('.data-table tr:nth-child(2) td:nth-child(1) a')
      .should('have.class', 'data-table-star-off')
      .click();
    cy.assertNotificationContainingText('Star row 2');

    cy.get('.data-table tr:nth-child(1) td:nth-child(1) a')
      .should('have.class', 'data-table-star-on')
      .click();
    cy.assertNotificationContainingText('Unstar row 1');

    cy.get('.data-table tr:nth-child(2) td:nth-child(1) a')
      .should('have.class', 'data-table-star-on')
      .click();
    cy.assertNotificationContainingText('Unstar row 2');
  });
});
