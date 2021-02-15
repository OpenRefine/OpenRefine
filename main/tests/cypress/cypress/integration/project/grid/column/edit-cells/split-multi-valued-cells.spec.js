describe(__filename, function () {
  it('Test a split by separator', function () {
    const fixture = [
      ['a', 'b'],

      ['0a', 'b_0_0***b_0_1***b_0_2'],
      ['1a', 'b_1_0***b_1_1***b_1_2'],
    ];

    cy.loadAndVisitProject(fixture);

    // click
    cy.columnActionClick('b', ['Edit cells', 'Split multi-valued cells...']);
    cy.get('.dialog-container label').contains('by separator').click();
    cy.get('.dialog-container input[bind="separatorInput"]').type('***');
    cy.get('.dialog-container button[bind="okButton"]').click();

    cy.assertCellEquals(0, 'b', 'b_0_0');
    cy.assertCellEquals(1, 'b', 'b_0_1');
    cy.assertCellEquals(2, 'b', 'b_0_2');

    cy.assertCellEquals(3, 'b', 'b_1_0');
    cy.assertCellEquals(4, 'b', 'b_1_1');
    cy.assertCellEquals(5, 'b', 'b_1_2');
  });

  // it('Test a split by regex', function () {});

  // it('Test a split by field length', function () {});

  // it('Test a split by transition from lowercase to uppercase', function () {});

  // it('Test a split by transition from numbers to letters', function () {});

  // it('Test a split by transition from numbers to letters (adding "Reverse splitting order" option)', function () {});
});
