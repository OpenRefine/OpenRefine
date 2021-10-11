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
    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'a');
    cy.assertCellEquals(2, 'b', 'a');
  });
});