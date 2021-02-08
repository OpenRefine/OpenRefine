describe(__filename, function () {
  it('Ensure flag is visible and toggle on/off', function () {
    cy.loadAndVisitProject('food.mini');
    cy.get('.odd > :nth-child(1) > .data-table-star-off')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Star row 1'
    );
    cy.get('.even > :nth-child(1) > .data-table-star-off')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Star row 2'
    );

    cy.get('.odd > :nth-child(1) > .data-table-star-on')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Unstar row 1'
    );
    cy.get('.even > :nth-child(1) > .data-table-star-on')
      .should('exist')
      .click();
    cy.get('#notification > div:nth-child(2) > span:nth-child(1)').should(
      'have.text',
      'Unstar row 2'
    );
  });
});
