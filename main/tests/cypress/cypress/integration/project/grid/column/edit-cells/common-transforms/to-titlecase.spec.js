describe(__filename, function () {
  it('Ensure text conversion to titlecase', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'To titlecase',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText(
      'Text transform on 2 cells in column Shrt_Desc: value.toTitlecase() '
    );
    cy.assertCellEquals(0, 'Shrt_Desc', 'Butter,with Salt');
    cy.assertCellEquals(1, 'Shrt_Desc', 'Butter,whipped,with Salt');
  });
});
