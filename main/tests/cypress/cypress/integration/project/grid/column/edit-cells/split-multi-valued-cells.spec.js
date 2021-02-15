describe(__filename, function () {
  it('Test a split by separator', function () {
    const fixture = [
      ['A column'],
      ['b_0_0***b_0_1***b_0_2'],
      ['b_1_0***b_1_1***b_1_2'],
    ];
    cy.loadAndVisitProject(fixture);
    // click
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);
    cy.get('.dialog-container label').contains('by separator').click();
    cy.get('.dialog-container input[bind="separatorInput"]').type('***');
    cy.get('.dialog-container button[bind="okButton"]').click();
    cy.assertCellEquals(0, 'A column', 'b_0_0');
    cy.assertCellEquals(1, 'A column', 'b_0_1');
    cy.assertCellEquals(2, 'A column', 'b_0_2');
    cy.assertCellEquals(3, 'A column', 'b_1_0');
    cy.assertCellEquals(4, 'A column', 'b_1_1');
    cy.assertCellEquals(5, 'A column', 'b_1_2');
  });

  it('Test a split by regex', function () {
    // each cell contains a number between letters, we will split by \d which is the regex for any int
    const fixture = [['A column'], ['a1b2c'], ['d3e4f']];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);
    cy.get('.dialog-container label').contains('by separator').click();
    cy.get('.dialog-container input[bind="separatorInput"]').type('\\d');
    cy.get('.dialog-container label').contains('regular expression').click();
    cy.get('.dialog-container button[bind="okButton"]').click();
    cy.assertCellEquals(0, 'A column', 'a');
    cy.assertCellEquals(1, 'A column', 'b');
    cy.assertCellEquals(2, 'A column', 'c');
    cy.assertCellEquals(3, 'A column', 'd');
    cy.assertCellEquals(4, 'A column', 'e');
    cy.assertCellEquals(5, 'A column', 'f');
  });

  it('Test a split by field length', function () {
    const fixture = [['A column'], ['BUTTER,WITH SALT']];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);
    cy.get('.dialog-container').should('to.contain', 'Split multi-valued');
    cy.get('.dialog-container textarea[bind="lengthsTextarea"]').type('2,4,8');
    cy.get('.dialog-container label').contains('by field lengths').click();
    cy.get('.dialog-container button[bind="okButton"]').click();
    cy.assertCellEquals(0, 'A column', 'BU');
    cy.assertCellEquals(1, 'A column', 'TTER');
    cy.assertCellEquals(2, 'A column', ',WITH SA');
  });
  it('Test a split by field length (single number)', function () {
    // each cell contains a number between letters, we will split by \d which is the regex for any int
    const fixture = [['A column'], ['BUTTER,WITH SALT']];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);
    cy.get('.dialog-container').should('to.contain', 'Split multi-valued');
    cy.get('.dialog-container textarea[bind="lengthsTextarea"]').type('2');
    cy.get('.dialog-container label').contains('by field lengths').click();
    cy.get('.dialog-container button[bind="okButton"]').click();
    cy.assertCellEquals(0, 'A column', 'BU');
    cy.assertCellEquals(1, 'A column', 'TT');
    cy.assertCellEquals(2, 'A column', 'ER');
  });

  it('Test a split by transition from lowercase to uppercase', function () {
    const fixture = [['A column'], ['BuTtEr']];

    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);

    cy.get('.dialog-container input[value="cases"]').check();
    cy.get('.dialog-container button[bind="okButton"]').click();
    cy.assertCellEquals(0, 'A column', 'B');
    cy.assertCellEquals(1, 'A column', 'u');
    cy.assertCellEquals(2, 'A column', 'T');
    cy.assertCellEquals(3, 'A column', 't');
    cy.assertCellEquals(4, 'A column', 'E');
    cy.assertCellEquals(5, 'A column', 'r');
  });

  it('Test a split by transition from lowercase to uppercase (with the example from the UX)', function () {
    const fixture = [['A column'], ['11Abc', 'Def22']];

    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);

    // what's the expected grid here ?
  });

  it('Test a split by transition from numbers to letters', function () {
    const fixture = [['A column'], ['11', 'AbcDef22']];

    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells...',
    ]);

    // what's the expected grid here ?
  });
  it('Test a split by transition from numbers to letters (adding "Reverse splitting order" option)', function () {});
});
