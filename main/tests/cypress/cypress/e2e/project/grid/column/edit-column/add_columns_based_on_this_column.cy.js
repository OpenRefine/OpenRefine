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
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
    cy.typeExpression('return value.lower()');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
    cy.confirmDialogPanel();

    // make sure the computation has completed (because this is treated as a long-running operation)
    cy.get('.column-header-name').contains('Test_Python_toLower').should('exist');
    cy.get('.data-table td .waiting', { timeout: 10000 }).should('not.exist');

    cy.assertCellEquals(0, 'Test_Python_toLower', 'butter,with salt');
    cy.assertCellEquals(1, 'Test_Python_toLower', 'butter,whipped,with salt');
  });
  it('Cancel a long computation of Python expressions', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Add column based on this column…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="columnNameInput"]').type('long_computation');
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
    cy.get('textarea.expression-preview-code').type('from time import sleep\nsleep(10)\nreturn value');
    cy.confirmDialogPanel();

    // check that the computation is in progress in the grid
    cy.get('.column-header-name').contains('long_computation').should('exist');
    cy.get('.data-table td .waiting').should('exist');
    
    // cancel the process
    cy.get('.process-buttons-container button').contains('Cancel').click();

    // there are no pending cells anymore
    cy.get('.column-header-name').contains('long_computation').should('not.exist');
    cy.get('.data-table td .waiting').should('not.exist');
  });

  it('Ensure new column is added based on previous column in Clojure', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Add column based on this column…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="columnNameInput"]').type('Test_Clojure_toLower');
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
    cy.typeExpression('(.. value (toLowerCase) )');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
    cy.confirmDialogPanel();

    // make sure the computation has completed (because this is treated as a long-running operation)
    cy.get('.column-header-name').contains('Test_Clojure_toLower').should('exist');
    cy.get('.data-table td .waiting', { timeout: 10000 }).should('not.exist');

    cy.assertCellEquals(0, 'Test_Clojure_toLower', 'butter,with salt');
    cy.assertCellEquals(1, 'Test_Clojure_toLower', 'butter,whipped,with salt');
  });
});
