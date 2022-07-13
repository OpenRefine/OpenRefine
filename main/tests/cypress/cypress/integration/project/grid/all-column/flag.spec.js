describe(__filename, function () {
  it('Ensure flag is visible and toggle on/off', function () {
    cy.loadAndVisitProject('food.mini');
    cy.get('.data-table tr:nth-child(1) td:nth-child(2) a')
      .should('have.class', 'data-table-flag-off')
      .click();
    cy.assertNotificationContainingText('Flag row 1');

    cy.get('.data-table tr:nth-child(2) td:nth-child(2) a')
      .should('have.class', 'data-table-flag-off')
      .click();
    cy.assertNotificationContainingText('Flag row 2');

    cy.get('.data-table tr:nth-child(1) td:nth-child(2) a')
      .should('have.class', 'data-table-flag-on')
      .click();
    cy.assertNotificationContainingText('Unflag row 1');

    cy.get('.data-table tr:nth-child(2) td:nth-child(2) a')
      .should('have.class', 'data-table-flag-on')
      .click();
    cy.assertNotificationContainingText('Unflag row 2');
  });
});
