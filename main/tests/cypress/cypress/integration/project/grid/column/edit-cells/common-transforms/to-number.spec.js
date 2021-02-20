describe(__filename, function () {
  it('Ensure some cells are converted to numbers, and some remains untouched (int)', function () {
    const fixture = [
      ['NDB_No', 'A Number'],
      ['01001', 'This is not a number'],
      ['01002', '42'],
      ['01003', '43'],
    ];
    cy.loadAndVisitProject(fixture);

    // click
    cy.columnActionClick('A Number', [
      'Edit cells',
      'Common transforms',
      'To number',
    ]);

    // Ensure notification and cell content
    cy.assertNotificationContainingText('Text transform on 2 cells');
    cy.assertCellEquals(0, 'A Number', 'This is not a number');
    cy.assertCellEquals(1, 'A Number', '42');
    cy.assertCellEquals(2, 'A Number', '43');

    // Ensure a numeric type is applied to the cell
    cy.assertCellNotString(1, 'A Number');
    cy.assertCellNotString(2, 'A Number');
  });

  it('Ensure toNumber works with floats', function () {
    const fixture = [
      ['NDB_No', 'A Number'],
      ['01001', '42.2'],
      ['01002', '43.5'],
      ['01002', '43.50000'],
      ['01002', '43.500001'],
    ];
    cy.loadAndVisitProject(fixture);

    // click
    cy.columnActionClick('A Number', [
      'Edit cells',
      'Common transforms',
      'To number',
    ]);

    // Ensure cell content
    cy.assertCellEquals(0, 'A Number', '42.2');
    cy.assertCellEquals(1, 'A Number', '43.5');
    cy.assertCellEquals(2, 'A Number', '43.5');
    cy.assertCellEquals(3, 'A Number', '43.500001');
  });
});
