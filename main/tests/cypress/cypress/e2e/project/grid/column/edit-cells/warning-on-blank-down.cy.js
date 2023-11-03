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

    //Create Pending Sort
    cy.columnActionClick('b', ['Sort']);
    cy.waitForDialogPanel();
    cy.confirmDialogPanel();

    //Verify Sort occurred
    cy.assertCellEquals(0, 'b', '2b');
    cy.assertCellEquals(1, 'b', 'also identical');
    cy.assertCellEquals(2, 'b', 'also identical');
    cy.assertCellEquals(3, 'b', 'also identical');
    cy.assertCellEquals(4, 'b', 'identical');
    cy.assertCellEquals(5, 'b', 'identical');

    // click, then cancel dialog window
    cy.columnActionClick('b', ['Edit cells', 'Blank down']);
    cy.waitForDialogPanel();
    cy.cancelDialogPanel();

    //Verify blank down did not occur
    cy.assertCellEquals(0, 'b', '2b');
    cy.assertCellEquals(1, 'b', 'also identical');
    cy.assertCellEquals(2, 'b', 'also identical');
    cy.assertCellEquals(3, 'b', 'also identical');
    cy.assertCellEquals(4, 'b', 'identical');
    cy.assertCellEquals(5, 'b', 'identical');

    // click, then confirm dialog window
    cy.columnActionClick('b', ['Edit cells', 'Blank down']);
    cy.waitForDialogPanel();
    cy.confirmDialogPanel();

    // ensure notification and cell content
    cy.assertNotificationContainingText('Blank down 3 cells');
    cy.assertCellEquals(0, 'b', '2b');
    cy.assertCellEquals(1, 'b', 'also identical');
    cy.assertCellEquals(2, 'b', 'identical');
    cy.assertCellEquals(3, 'b', null);
    cy.assertCellEquals(4, 'b', null);
    cy.assertCellEquals(5, 'b', null);
  });
});
