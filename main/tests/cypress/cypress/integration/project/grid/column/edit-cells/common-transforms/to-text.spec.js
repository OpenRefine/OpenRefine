describe(__filename, function () {
  it('Ensure text is converted to Uppercase', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'To text',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 0 cells in column Shrt_Desc: value.toString()'
    );
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(1, 'Shrt_Desc', 'BUTTER,WHIPPED,WITH SALT');
  });
});
