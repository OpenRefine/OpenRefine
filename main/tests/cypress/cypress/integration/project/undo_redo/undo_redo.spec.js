describe(__filename, function () {
  it('Ensure the Undo button is visible after deleting a column', function () {
    cy.loadAndVisitProject('food.mini');
    cy.deleteColumn('NDB_No');

    cy.get('#notification-container')
      .should('be.visible')
      .should('to.contain', 'Remove column NDB_No');
    cy.get('#notification-container .notification-action')
      .should('be.visible')
      .should('to.contain', 'Undo');
  });

  it('Ensure the Undo button is effectively working', function () {
    cy.loadAndVisitProject('food.mini');

    cy.deleteColumn('NDB_No');
    // ensure that the column is back in the grid
    cy.get('#notification-container .notification-action')
      .should('be.visible')
      .should('to.contain', 'Undo');
    cy.get('#notification-container a[bind="undoLink"]').click();
    cy.get('.data-table th[title="NDB_No"]').should('exist');
  });

  it('Delete 3 columns, then successively undo and redo the modifications using the Undo/Redo panel', function () {
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

    // successively undo all modifications
    cy.get(
      '.history-panel-body .history-past a.history-entry:last-of-type'
    ).click();
    cy.get('.history-panel-body .history-past').should(
      'to.contain',
      'Remove column NDB_No'
    );
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Remove column Water'
    );
    cy.get('.history-panel-body .history-future').should(
      'to.contain',
      'Remove column Shrt_Desc'
    );

    cy.get(
      '.history-panel-body .history-past a.history-entry:last-of-type'
    ).click();
    cy.get('.history-panel-body .history-now').should(
      'to.contain',
      'Remove column NDB_No'
    );
    cy.get('.history-panel-body .history-future').should(
      'to.contain',
      'Remove column Water'
    );
    cy.get('.history-panel-body .history-future').should(
      'to.contain',
      'Remove column Shrt_Desc'
    );
  });

  // Very long test to run
  // it('Ensure the Undo button dissapear after timeout after deleting a column', function () {
  // 	const ORNotificationTimeout = 15000;
  // cy.loadAndVisitProject('food.mini');
  // 	cy.columnActionClick('NDB_No', ['Edit column', 'Remove this column']);
  // 	cy.get('#notification-container', { timeout: ORNotificationTimeout }).should('not.be.visible');
  // });
});
