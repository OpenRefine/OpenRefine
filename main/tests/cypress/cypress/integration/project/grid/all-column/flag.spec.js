describe(__filename, function () {
  it('Ensure flag is visible and toggle on/off', function () {
    cy.loadAndVisitProject('food.mini');
    cy.get('.odd > :nth-child(2) > .data-table-flag-off')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Flag row 1'
    );
    cy.get('.even > :nth-child(2) > .data-table-flag-off')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Flag row 2'
    );

    cy.get('.odd > :nth-child(2) > .data-table-flag-on')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Unflag row 1'
    );
    cy.get('.even > :nth-child(2) > .data-table-flag-on')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Unflag row 2'
    );
  });
});
