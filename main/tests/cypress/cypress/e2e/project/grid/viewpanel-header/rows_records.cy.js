describe(__filename, function () {
  afterEach(() => {
    cy.addProjectForDeletion()
  })

  it('ensures rows and records display same in csv file', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('span[bind="modeSelectors"]').contains('records').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);

    cy.get('body[ajax_in_progress="false"]');

    cy.get('span[bind="modeSelectors"]').contains('rows').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);
  });
  it('ensures rows and records are different for 3-level json file', function () {
    const projectName = Date.now();
    cy.fixture('donut-records').then((jsonValue) => {
      cy.loadAndVisitSampleJSONProject(projectName, jsonValue);
    });
    cy.get('span[bind="modeSelectors"]').contains('records').click();
    cy.get('span:contains("records")').should('length',3);
    cy.get('tr td:nth-child(3)').then((recordNumber) => {
      for (let i = 1; i <= 3; i++) {
      expect(recordNumber.text()).to.contain(i);
      }
    });

    cy.get('span[bind="modeSelectors"]').contains('row').click();
    cy.get('span:contains("rows")').should('length',3);
    cy.get('tr td:nth-child(3)').then((rowNumber) => {
      for (let i = 1; i <= 10; i++) {
        expect(rowNumber.text()).to.contain(i);
      }
    });
  });
});
