describe(__filename, function () {
  it('Ensures value in cells changes to empty string', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'To empty string',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 2 cells in column Shrt_Desc: ""'
    );
    cy.assertCellEquals(0, 'Shrt_Desc', '');
    cy.assertCellEquals(1, 'Shrt_Desc', '');
  });
});
