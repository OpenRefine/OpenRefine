describe(__filename, function () {
  it('Ensure cells are replaced', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type("change");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("a");
    cy.get('.dialog-container button[bind="okButton"]').click();

    cy.get('.odd td:nth-child(5)').should('to.contain', 'a');

    cy.get('.even td:nth-child(5)').should('to.contain', 'a');


  });
});