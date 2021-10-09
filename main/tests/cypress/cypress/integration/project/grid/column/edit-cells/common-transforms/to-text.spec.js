describe(__filename, function () {
  it('Ensure number is converted to text', function () {
    cy.loadAndVisitProject('food.mini');

    cy.castColumnTo('NDB_No', 'number');

    cy.columnActionClick('NDB_No', [
      'Edit cells',
      'Common transforms',
      'To text',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 2 cells in column NDB_No: value.toString()'
    );

    cy.assertCellEquals(0, 'NDB_No', '1001');
    cy.assertCellEquals(1, 'NDB_No', '1002');
  });
});
