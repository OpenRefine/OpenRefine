describe(__filename, function () {
  it('Ensure text conversion to Uppercase', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'To uppercase',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 0 cells in column Shrt_Desc: value.toUppercase()'
    );
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(1, 'Shrt_Desc', 'BUTTER,WHIPPED,WITH SALT');
  });
});
