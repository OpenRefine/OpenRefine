describe(__filename, function () {
  it('ensures rows and records display same in csv file', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('span[bind="modeSelectors"]').contains('records').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);

    cy.get('span[bind="modeSelectors"]').contains('rows').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);
  });
  it('ensures rows and records are different for 3-level json file', function () {
    const projectName = Date.now();
    cy.loadAndVisitSampleJSONProject(projectName, 'sweets.json');
    cy.get('span[bind="modeSelectors"]').contains('records').click();
    for (let i = 1; i <= 3; i++) {
      cy.get('tr td:nth-child(3)').should('to.contain', i);
    }
    cy.get('span[bind="modeSelectors"]').contains('row').click();
    for (let i = 1; i <= 10; i++) {
      cy.get('tr td:nth-child(3)').should('to.contain', i);
    }
  });
});
