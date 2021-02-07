describe(__filename, function () {
  it('Ensure cells are filled down', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', '0b', '0c'],
      ['1a', null, '1c'],
      ['2a', '2b', '2c'],
      ['3a', null, '3c'],
      ['4a', null, '4c'],
      ['5a', '5b', '5c'],
    ];

    cy.loadAndVisitProject(fixture);
    // click
    cy.columnActionClick('b', ['Edit cells', 'Fill down']);

    // ensure notification and cell content
    cy.assertNotificationContainingText('Fill down 3 cells in column b');
    cy.assertCellEquals(0, 'b', '0b'); // untouched
    cy.assertCellEquals(1, 'b', '0b'); // filled
    cy.assertCellEquals(2, 'b', '2b'); // untouched
    cy.assertCellEquals(3, 'b', '2b'); // filled
    cy.assertCellEquals(4, 'b', '2b'); // filled
    cy.assertCellEquals(5, 'b', '5b'); // untouched
  });
});
