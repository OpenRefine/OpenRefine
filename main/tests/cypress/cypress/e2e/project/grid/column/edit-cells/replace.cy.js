describe(__filename, function () {
  it('Ensure cells are replaced', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'Change', '1c'],
      ['2a', 'Change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type("change");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("a");
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 1 cell');

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'Change');
    cy.assertCellEquals(2, 'b', 'Change');
  });
  it('Ensure cells are replaced case insensitively', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'Change', '1c'],
      ['2a', 'Change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type("change");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("a");
    cy.get('label[bind="or_views_find_case_insensitive"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'a');
    cy.assertCellEquals(2, 'b', 'a');
  });
  it('Ensure cells are replaced with whole word', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change1', '1c'],
      ['2a', 'change2', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type("change");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("a");
    cy.get('label[bind="or_views_find_whole_word"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 1 cells');

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'change1');
    cy.assertCellEquals(2, 'b', 'change2');
  });
  it('Ensure cells are replaced with regular expression', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'hat', '0c'],
      ['1a', 'cat', '1c'],
      ['2a', 'bat', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type("[hc]at");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("a");
    cy.get('label[bind="or_views_find_regExp"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 2 cells');

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'a');
    cy.assertCellEquals(2, 'b', 'bat');
  });
  it('Ensure cells are replaced with line ', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'first_name second_name', '0c'],
      ['1a', 'first_name second_name', '1c'],
      ['2a', 'first_name', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type(" ");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("\\n");
    cy.get('label[bind="or_views_replace_dont_escape"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 2 cells');

    cy.assertCellEquals(0, 'b', 'first_name\nsecond_name');
    cy.assertCellEquals(1, 'b', 'first_name\nsecond_name');
    cy.assertCellEquals(2, 'b', 'first_name');
  });
  it('Ensure cells are replaced with \\n', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'first_name second_name', '0c'],
      ['1a', 'first_name second_name', '1c'],
      ['2a', 'first_name', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Replace']);

    cy.get('.dialog-container input[bind="text_to_findInput"]').type(" ");
    cy.get('.dialog-container input[bind="replacement_textInput"]').type("\\\\n");
    cy.get('label[bind="or_views_replace_dont_escape"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 2 cells');

    cy.assertCellEquals(0, 'b', 'first_name\\nsecond_name');
    cy.assertCellEquals(1, 'b', 'first_name\\nsecond_name');
    cy.assertCellEquals(2, 'b', 'first_name');

  });
});
