
describe(__filename, function () {
  it('Ensure cells cells contains error message when error occurs', function () {
    const fixture = [
      ['a'],
      ['0a'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('a', ['Edit cells', 'Transform']);

    cy.typeExpression('value.wholeText()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 1 cell');

    cy.assertCellEquals(0, 'a', 'wholeText() cannot work with this \'string\' and failed as the first parameter is not an XML or HTML Element.  Please first use parseXml() or parseHtml() and select(query) prior to using this function');
  });
});
