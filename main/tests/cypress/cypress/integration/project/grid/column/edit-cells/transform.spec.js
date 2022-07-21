describe(__filename, function () {
  it('Ensure cells are transformed', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('replace(value,"change","a")');
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'a');
    cy.assertCellEquals(2, 'b', 'a');
  });
  it('Ensure cells are set to blank when error occurs', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('value.replace()');;
    cy.get('label[bind="or_views_setBlank"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', '');
    cy.assertCellEquals(1, 'b', '');
    cy.assertCellEquals(2, 'b', '');
  });
  it('Ensure cells cells contains error message when error occurs', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('value.replace()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'replace expects three strings, or one string, one regex, and one string');
    cy.assertCellEquals(1, 'b', 'replace expects three strings, or one string, one regex, and one string');
    cy.assertCellEquals(2, 'b', 'replace expects three strings, or one string, one regex, and one string');
  });
});
