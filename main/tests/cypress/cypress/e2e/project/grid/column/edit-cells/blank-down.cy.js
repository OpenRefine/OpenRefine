describe(__filename, function () {
  it('Ensure cells are blanked down', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'identical', '0c'],
      ['1a', 'identical', '1c'],
      ['2a', '2b', '2c'],
      ['3a', 'also identical', '3c'],
      ['4a', 'also identical', '4c'],
      ['5a', 'also identical', '5c'],
    ];

    cy.loadAndVisitProject(fixture);

    // click
    cy.columnActionClick('b', ['Edit cells', 'Blank down']);

    // ensure notification and cell content
    cy.assertNotificationContainingText('Blank down 3 cells');
    cy.assertCellEquals(0, 'b', 'identical'); // untouched
    cy.assertCellEquals(1, 'b', null); // blanked
    cy.assertCellEquals(2, 'b', '2b'); // untouched
    cy.assertCellEquals(3, 'b', 'also identical'); // untouched
    cy.assertCellEquals(4, 'b', null); // blanked
    cy.assertCellEquals(5, 'b', null); // blanked
  });
});
