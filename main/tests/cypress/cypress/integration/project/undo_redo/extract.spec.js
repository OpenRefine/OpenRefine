describe(__filename, function () {
  it('Test select/unselect all', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.deleteColumn('Shrt_Desc');

    cy.get('#or-proj-undoRedo').click();
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Extract')
      .click();

    // unselect all
    cy.get('.dialog-container button[bind="unselectAllButton"]').click();
    cy.wait(500); // eslint-disable-line
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(1) td:first-child input'
    ).should('not.to.be.checked');
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(2) td:first-child input'
    ).should('not.to.be.checked');
    cy.get('.dialog-container textarea.history-operation-json').should(
      'have.value',
      '[]'
    );

    // // reselect all
    cy.get('.dialog-container button[bind="selectAllButton"]').click();
    cy.wait(500); // eslint-disable-line
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(1) td:first-child input'
    ).should('be.checked');
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(2) td:first-child input'
    ).should('be.checked');
    cy.assertTextareaHaveJsonValue(
      '.dialog-container textarea.history-operation-json',
      [
        {
          op: 'core/column-removal',
          columnName: 'NDB_No',
          description: 'Remove column NDB_No',
        },
        {
          op: 'core/column-removal',
          columnName: 'Shrt_Desc',
          description: 'Remove column Shrt_Desc',
        },
      ]
    );
  });

  it('Test select/unselect individual entries', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.deleteColumn('Shrt_Desc');
    cy.get('#or-proj-undoRedo').click();
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Extract')
      .click();

    // unselect "Remove Water"
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(1) td:first-child'
    ).click();
    cy.assertTextareaHaveJsonValue(
      '.dialog-container textarea.history-operation-json',
      [
        {
          op: 'core/column-removal',
          columnName: 'Shrt_Desc',
          description: 'Remove column Shrt_Desc',
        },
      ]
    );

    // unselect "Remove Shrt_Desc"
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(2) td:first-child'
    ).click();
    cy.assertTextareaHaveJsonValue(
      '.dialog-container textarea.history-operation-json',
      []
    );

    // reselect "Remove Shrt_Desc"
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(2) td:first-child'
    ).click();
    cy.assertTextareaHaveJsonValue(
      '.dialog-container textarea.history-operation-json',
      [
        {
          op: 'core/column-removal',
          columnName: 'Shrt_Desc',
          description: 'Remove column Shrt_Desc',
        },
      ]
    );

    // reselect "Remove Water"
    cy.get(
      '.history-extract-dialog-entries tr:nth-child(1) td:first-child'
    ).click();
    cy.assertTextareaHaveJsonValue(
      '.dialog-container textarea.history-operation-json',
      [
        {
          op: 'core/column-removal',
          columnName: 'NDB_No',
          description: 'Remove column NDB_No',
        },
        {
          op: 'core/column-removal',
          columnName: 'Shrt_Desc',
          description: 'Remove column Shrt_Desc',
        },
      ]
    );
  });

  it('Test the close button', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.get('#or-proj-undoRedo').click();
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Extract')
      .click();

    cy.get('.dialog-container').should('exist').should('be.visible');
    cy.get('.dialog-container button[bind="closeButton"]').click();
    cy.get('.dialog-container').should('not.exist');
  });

  it('Ensure action are recorded in the extract panel', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.deleteColumn('Shrt_Desc');

    cy.get('#or-proj-undoRedo').click();

    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Extract')
      .click();

    // test the entries
    cy.get('.history-extract-dialog-entries').contains('Remove column NDB_No');
    cy.get('.history-extract-dialog-entries').contains(
      'Remove column Shrt_Desc'
    );

    // test the json
    cy.assertTextareaHaveJsonValue(
      '.dialog-container textarea.history-operation-json',
      [
        {
          op: 'core/column-removal',
          columnName: 'NDB_No',
          description: 'Remove column NDB_No',
        },
        {
          op: 'core/column-removal',
          columnName: 'Shrt_Desc',
          description: 'Remove column Shrt_Desc',
        },
      ]
    );
  });
});
