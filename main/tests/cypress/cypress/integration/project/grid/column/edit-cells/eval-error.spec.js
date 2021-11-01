describe(__filename, function () {
  it('Ensure cells contain error message when error occurs for InnerHtml', function () {
    const fixture = [
      ['a'],
      ['0a'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('a', ['Edit cells', 'Transform']);

    cy.typeExpression('value.innerHtml()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 1 cell');

    cy.assertCellEquals(0, 'a', 'innerHtml() cannot work with this \'string\'. The first parameter is not an HTML Element.  Please first use parseHtml(string) and select(query) prior to using this function');
  });
});

describe(__filename, function () {
  it('Ensure cells contain error message when error occurs for XmlText', function () {
    const fixture = [
      ['a'],
      ['0a'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('a', ['Edit cells', 'Transform']);

    cy.typeExpression('value.xmlText()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 1 cell');

    cy.assertCellEquals(0, 'a', 'xmlText() cannot work with this \'string\' and failed as the first parameter is not an XML or HTML Element.  Please first use parseXml() or parseHtml() and select(query) prior to using this function');
  });
});

describe(__filename, function () {
  it('Ensure cells contain error message when error occurs for WholeText', function () {
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
