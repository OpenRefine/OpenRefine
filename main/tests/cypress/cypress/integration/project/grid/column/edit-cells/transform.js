describe(__filename, function () {
  it('Ensure cells are transformed', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.get('.dialog-container textarea[bind="expressionPreviewTextarea"]').type('replace(value,"change","a")');
    cy.wait(400);
    cy.confirmDialogPanel();

    cy.get('.odd td:nth-child(5)').should('to.contain', 'a');
    cy.get('.even td:nth-child(5)').should('to.contain', 'a');

    cy.wait(400);
  });
});
