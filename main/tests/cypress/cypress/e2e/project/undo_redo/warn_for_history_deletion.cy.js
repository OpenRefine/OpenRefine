describe(__filename, function () {
  it('Apply an operation after changes have been undone', function () {
    cy.loadAndVisitProject('food.mini');

    // delete NDB_No
    cy.deleteColumn('NDB_No');
    cy.get('#or-proj-undoRedo').should('to.contain', '1 / 1');
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Remove column NDB_No'
    );

    // delete Water
    cy.deleteColumn('Water');
    cy.get('#or-proj-undoRedo').should('to.contain', '2 / 2');
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Remove column Water'
    );

    // Delete Shrt_Desc
    cy.deleteColumn('Shrt_Desc');
    cy.get('#or-proj-undoRedo').should('to.contain', '3 / 3');
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Remove column Shrt_Desc'
    );

    // Open the Undo/Redo panel
    cy.get('#or-proj-undoRedo').click();

    // ensure all previous actions have been recorded
    cy.get('.history-panel-body .history-past').should(
      'to.contain',
      'Remove column NDB_No'
    );
    cy.get('.history-panel-body .history-past').should(
      'to.contain',
      'Remove column Water'
    );
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Remove column Shrt_Desc'
    );

    // undo all changes at once
    cy.get(
      '.history-panel-body .history-past a.history-entry:first-of-type'
    ).click();
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Create project'
    );
    cy.get('.history-panel-body .history-future').should(
      'to.contain',
      'Remove column NDB_No'
    );
    cy.get('.history-panel-body .history-future').should(
      'to.contain',
      'Remove column Shrt_Desc'
    );

    // edit a cell in the grid
    cy.getCell(1, 'Shrt_Desc')
      .trigger('mouseover')
      .find('button.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(
      'OpenRefine Testing'
    );
    cy.get('.menu-container button[bind="okButton"]').click();

    // ensure we get a dialog warning us for deletion of changes
    cy.get('.dialog-header').should('to.contain', 'Confirm deletion of project history');
    // confirm the dialog   
    cy.get('.dialog-footer .button-primary').click();

    // ensure value has been changed in the grid
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.assertCellEquals(1, 'Shrt_Desc', 'OpenRefine Testing');

    // There are no future changes anymore, since they have been deleted
    cy.get('.history-panel-body .history-future a').should('not.exist');
  });
});
