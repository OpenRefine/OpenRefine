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

    cy.typeExpression('replace(value,"change","a")');
    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'a');
    cy.assertCellEquals(2, 'b', 'a');
  });
});
