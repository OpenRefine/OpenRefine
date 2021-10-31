describe(__filename, function () {
  it('Ensure cells cells contains error message when error occurs', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('value.ParseHtml()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'parseHtml() cannot work with this \'string\' and expects a single String as an argument');
    cy.assertCellEquals(1, 'b', 'parseHtml() cannot work with this \'string\' and expects a single String as an argument');
    cy.assertCellEquals(2, 'b', 'parseHtml() cannot work with this \'string\' and expects a single String as an argument');
  });
});
describe(__filename, function () {
  it('Ensure cells cells contains error message when error occurs', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('value.ParseXml()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'parseXml() cannot work with this \'string\' and expects a single String as an argument');
    cy.assertCellEquals(1, 'b', 'parseXml() cannot work with this \'string\' and expects a single String as an argument');
    cy.assertCellEquals(2, 'b', 'parseXml() cannot work with this \'string\' and expects a single String as an argument');
  });
});

describe(__filename, function () {
  it('Ensure cells cells contains error message when error occurs', function () {
    const fixture = [
      ['a'],
      ['0a'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('a', ['Edit cells', 'Transform']);

    cy.typeExpression('value.WholeText()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 1 cell');

    cy.assertCellEquals(0, 'a', 'wholeText() cannot work with this \'string\' and failed as the first parameter is not an XML or HTML Element.  Please first use parseXml() or parseHtml() and select(query) prior to using this function');
  });
});
