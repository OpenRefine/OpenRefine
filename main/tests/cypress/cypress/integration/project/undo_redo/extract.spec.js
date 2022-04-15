const defaultInducedHistoryJson = [
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

describe(__filename, function () {
  it('Test select/deselect all', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.deleteColumn('Shrt_Desc');

    cy.get('#or-proj-undoRedo').click();
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Extract')
      .click();

    // deselect all
    cy.get('.dialog-container button[bind="deselectAllButton"]').click();
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
      defaultInducedHistoryJson
    );
  });

  it('Test select/deselect individual entries', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.deleteColumn('Shrt_Desc');
    cy.get('#or-proj-undoRedo').click();
    cy.get('#refine-tabs-history .history-panel-controls')
      .contains('Extract')
      .click();

    // deselect "Remove Water"
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

    // deselect "Remove Shrt_Desc"
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
      defaultInducedHistoryJson
    );
  });

  it('Test download of JSON file', { retries: { runMode: 0 } }, function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');
    cy.deleteColumn('Shrt_Desc');

    cy.get('#or-proj-undoRedo').click();
    cy.get('#refine-tabs-history .history-panel-controls')
    .contains('Extract')
    .click();
    // (the extractable commands are selected by default)

    cy.get('.dialog-container button[bind="saveJsonAsFileButton"]')
    .contains('Export')
    .click();

    const downloadsFolder = Cypress.config('downloadsFolder');
    const downloadedFilename = downloadsFolder + '/history.json';

    cy.readFile(downloadedFilename, 'utf8')
      .should('deep.equal', defaultInducedHistoryJson)
  })

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
      defaultInducedHistoryJson
    );
  });
});
