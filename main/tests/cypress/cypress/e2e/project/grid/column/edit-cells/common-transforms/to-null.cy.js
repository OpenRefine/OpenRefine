describe(__filename, function () {
  it('Ensure values in cells are converted to null ', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'To null',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 2 cells in column Shrt_Desc: null'
    );
    cy.assertCellEquals(0, 'Shrt_Desc', null);
    cy.assertCellEquals(1, 'Shrt_Desc', null);
  });
});
