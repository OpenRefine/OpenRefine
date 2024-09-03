describe(__filename, function () {
  it('Ensure new column is added based on previous column in GREL', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Add column based on this column…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="columnNameInput"]').type('Test_GREL_toLower');
    cy.typeExpression('value.toLowercase()');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Test_GREL_toLower', 'butter,with salt');
    cy.assertCellEquals(1, 'Test_GREL_toLower', 'butter,whipped,with salt');
  });
  it('Ensure new column is added based on previous column in Python', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Add column based on this column…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="columnNameInput"]').type('Test_Python_toLower');

    cy.get('textarea.expression-preview-code').clear()
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
    // Wait for Jython interpreter to load (as indicated by changed error message)
    cy.get('.expression-preview-parsing-status').contains('Internal error');

    cy.typeExpression('return value.lower()');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child',{timeout: 10000}
    ).should('to.contain', 'butter,with salt');
    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Test_Python_toLower', 'butter,with salt');
    cy.assertCellEquals(1, 'Test_Python_toLower', 'butter,whipped,with salt');
  });
  it('Ensure new column is added based on previous column in Clojure', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Add column based on this column…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="columnNameInput"]').type('Test_Clojure_toLower');

    cy.get('textarea.expression-preview-code').clear().type('(');
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
    // Wait for Clojure interpreter to load (as indicated by changed error message)
    cy.get('.expression-preview-parsing-status').contains('Syntax error reading source');

    cy.typeExpression('(.. value (toLowerCase) )');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Test_Clojure_toLower', 'butter,with salt');
    cy.assertCellEquals(1, 'Test_Clojure_toLower', 'butter,whipped,with salt');
  });
});
