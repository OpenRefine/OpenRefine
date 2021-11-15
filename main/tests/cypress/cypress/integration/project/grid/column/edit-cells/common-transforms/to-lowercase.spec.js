describe(__filename, function () {
  it('Ensure values in cells are converted to lowercase', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'To lowercase',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on cells in column Shrt_Desc using expression value.toLowercase()'
    );
    cy.assertCellEquals(0, 'Shrt_Desc', 'butter,with salt');
    cy.assertCellEquals(1, 'Shrt_Desc', 'butter,whipped,with salt');
  });
});
