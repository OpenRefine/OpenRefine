describe(__filename, function () {
  it('Apply a recipe', function () {
    cy.loadAndVisitProject('food.mini');

    // Check some columns before the test
    cy.get('table.data-table thead th[title="Shrt_Desc"]').should('exist');
    cy.get('table.data-table thead th[title="Water"]').should('exist');


    // find the "apply" button
    cy.get('#or-proj-undoRedo').click();
    cy.wait(500); // eslint-disable-line
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Apply')
      .click();
      
    // JSON for operations that will be applied
    const recipeFile = { filePath: 'recipe.json', mimeType: 'application/json' };
    cy.get('#file-input[type="file"]').attachFile(recipeFile);
    
    cy.get('.dialog-container button[bind="applyButton"]').click();

    // Column mapping dialog
    cy.get('.dialog-header').contains('Map recipe columns to project columns');
    cy.get('select[name="column_0"]').contains('Energ_Kcal');
    cy.get('select[name="column_2"]').select('Shrt_Desc');

    cy.get('input[type="submit"]').contains('Run operations').click();

    cy.get('table.data-table thead th[title="Energ_Kcal"]').should(
      'not.to.exist'
    );

    cy.assertNotificationContainingText('3 operations applied');
    cy.get('a#or-proj-undoRedo > span.count')
      .invoke('text')
      .should('equal', '3 / 3');
    cy.get('#summary-bar > span')
      .invoke('text')
      .should('equal', '5 rows');

    cy.get('.notification-action a').should('to.contain', 'Undo').click();

    cy.get('a#or-proj-undoRedo > span.count')
      .invoke('text')
      .should('equal', '0 / 3');
  });

  it('Apply a recipe that does not reference any column', function () {
    cy.loadAndVisitProject('food.mini');

    // flag a row before the test
    cy.get('.data-table tr:nth-child(1) td:nth-child(2) a')
      .should('have.class', 'data-table-flag-off')
      .click();
    cy.assertNotificationContainingText('Flag row 1');

    // find the "apply" button
    cy.get('#or-proj-undoRedo').click();
    cy.wait(500); // eslint-disable-line
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Apply')
      .click();
      
    // JSON for operations that will be applied
    const recipeFile = { filePath: 'recipe_without_column_reference.json', mimeType: 'application/json' };
    cy.get('#file-input[type="file"]').attachFile(recipeFile);
    
    cy.get('.dialog-container button[bind="applyButton"]').click();

    cy.assertNotificationContainingText('Remove 1 rows');
  });

  it('Use an invalid JSON payload', function () {
    cy.loadAndVisitProject('food.mini');

    // find the "apply" button
    cy.get('#or-proj-undoRedo').click();
    cy.wait(500); // eslint-disable-line
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Apply')
      .click();
      
    cy.get('.dialog-container .history-operation-json').type(
      "[{foo",
      {
        parseSpecialCharSequences: false,
        delay: 0,
        waitForAnimations: false,
      }
    );
    cy.get('.dialog-container button[bind="applyButton"]').click();

    cy.get('.dialog-container .history-operation-json-error').contains('Invalid JSON format');
  });

});
