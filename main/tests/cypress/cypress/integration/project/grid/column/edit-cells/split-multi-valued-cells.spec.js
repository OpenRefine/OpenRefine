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
      'Split multi-valued cells…',
    ]);
    cy.get('.dialog-container label').contains('by separator').click();
    cy.get('.dialog-container input[bind="separatorInput"]').type('***');
    cy.confirmDialogPanel();

    cy.assertGridEquals([
      ['A column'],
      ['b_0_0'],
      ['b_0_1'],
      ['b_0_2'],
      ['b_1_0'],
      ['b_1_1'],
      ['b_1_2'],
    ]);
  });

  it('Test a split by regex', function () {
    // each cell contains a number between letters, we will split by \d which is the regex for any int
    const fixture = [['A column'], ['a1b2c'], ['d3e4f']];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells…',
    ]);
    cy.get('.dialog-container label').contains('by separator').click();
    cy.get('.dialog-container input[bind="separatorInput"]').type('\\d');
    cy.get('.dialog-container label').contains('regular expression').click();
    cy.confirmDialogPanel();

    cy.assertGridEquals([
      ['A column'],
      ['a'],
      ['b'],
      ['c'],
      ['d'],
      ['e'],
      ['f'],
    ]);
  });

  it('Test a split by field length', function () {
    const fixture = [['A column'], ['BUTTER,WITH SALT']];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells…',
    ]);
    cy.get('.dialog-container').should('to.contain', 'Split multi-valued');
    cy.get('.dialog-container textarea[bind="lengthsTextarea"]').type('2,4,8');
    cy.get('.dialog-container label').contains('by field lengths').click();
    cy.confirmDialogPanel();

    cy.assertGridEquals([['A column'], ['BU'], ['TTER'], [',WITH SA']]);
  });

  it('Test a split by transition from lowercase to uppercase (with the example from the UX)', function () {
    const fixture = [['A column'], ['11AbcDef22']];

    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells…',
    ]);
    cy.get('.dialog-container input[value="cases"]').check();
    cy.confirmDialogPanel();

    cy.assertGridEquals([['A column'], ['11Abc'], ['Def22']]);
  });

  it('Test a split by transition from numbers to letters', function () {
    const fixture = [['A column'], ['Abcdef1Abcdef2Abcdef3']];

    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('A column', [
      'Edit cells',
      'Split multi-valued cells…',
    ]);

    cy.get('.dialog-container input[value="number"]').check();
    cy.confirmDialogPanel();

    cy.assertGridEquals([['A column'], ['Abcdef1'], ['Abcdef2'], ['Abcdef3']]);
  });
});
