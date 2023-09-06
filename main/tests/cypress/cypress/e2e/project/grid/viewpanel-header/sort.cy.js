describe(__filename, function () {
  it('Ensure it reverses and reorders text sort', function () {
    cy.loadAndVisitProject('food.sort');

    cy.columnActionClick('Shrt_Desc', ['Sort']);
    cy.waitForDialogPanel();
    cy.confirmDialogPanel();

    cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
    cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT');
    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('By Shrt_Desc').trigger('mouseover');
    cy.get('.menu-item').contains('Reverse').click();
    cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT');
    cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('Reorder rows permanently').click();
    cy.assertNotificationContainingText('Reorder rows');
    
    // check that it triggered an operation
    cy.get('#or-proj-undoRedo').click();
    cy.get('.history-entry span').should('to.contain', 'Reorder rows');
    
    // after reloading, the grid should still be in the new order
    cy.reload();
    cy.waitForProjectTable();
    cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT');
    cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
  });
  it('Ensure it reverses and reorders number sort', function () {
    cy.loadAndVisitProject('food.sort');

    cy.castColumnTo('NDB_No', 'number');
    cy.columnActionClick('NDB_No', ['Sort']);

    cy.waitForDialogPanel();
    cy.get('[type="radio"]').check('number');
    cy.get('[type="radio"]').check('reverse');
    cy.confirmDialogPanel();

    cy.getCell(0, 'NDB_No').should('to.contain', 1002);
    cy.getCell(1, 'NDB_No').should('to.contain', 1001);

    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('By NDB_No').trigger('mouseover');
    cy.get('.menu-item').contains('Reverse').click();
    cy.getCell(0, 'NDB_No').should('to.contain', 1001);
    cy.getCell(1, 'NDB_No').should('to.contain', 1002);
    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('Reorder rows permanently').click();
    cy.assertNotificationContainingText('Reorder rows');

    // check that it triggered an operation
    cy.get('#or-proj-undoRedo').click();
    cy.get('.history-entry span').should('to.contain', 'Reorder rows');
    
    // after reloading, the grid should still be in the new order
    cy.reload();
    cy.waitForProjectTable();
    cy.getCell(0, 'NDB_No').should('to.contain', 1001);
    cy.getCell(1, 'NDB_No').should('to.contain', 1002);
  });
  it('Ensure it reverses and reorders date sort', function () {
    cy.loadAndVisitProject('food.sort');

    cy.castColumnTo('Date', 'date');
    cy.columnActionClick('Date', ['Sort']);

    cy.waitForDialogPanel();
    cy.get('[type="radio"]').check('date');
    cy.get('[type="radio"]').check('reverse');
    cy.confirmDialogPanel();

    cy.getCell(0, 'Date').should('to.contain', '2020-12-17T00:00:00Z');
    cy.getCell(1, 'Date').should('to.contain', '2020-08-17T00:00:00Z');

    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('By Date').trigger('mouseover');
    cy.get('.menu-item').contains('Reverse').click();
    cy.getCell(0, 'Date').should('to.contain', '2020-08-17T00:00:00Z');
    cy.getCell(1, 'Date').should('to.contain', '2020-12-17T00:00:00Z');

    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('Reorder rows permanently').click();
    cy.assertNotificationContainingText('Reorder rows');

    // check that it triggered an operation
    cy.get('#or-proj-undoRedo').click();
    cy.get('.history-entry span').should('to.contain', 'Reorder rows');
    
    // after reloading, the grid should still be in the new order
    cy.reload();
    cy.waitForProjectTable();
    cy.getCell(0, 'Date').should('to.contain', '2020-08-17T00:00:00Z');
    cy.getCell(1, 'Date').should('to.contain', '2020-12-17T00:00:00Z');
  });
  it('Ensure it reverses and reorders bool sort', function () {
    cy.loadAndVisitProject('food.sort');

    cy.getCell(0, 'Fat')
      .trigger('mouseover')
      .within(() => {
        cy.get('a.data-table-cell-edit').click();
      });
    cy.get('select').select('boolean');
    cy.get('button').contains(new RegExp('Apply', 'g')).click();

    cy.getCell(1, 'Fat')
      .trigger('mouseover')
      .within(() => {
        cy.get('a.data-table-cell-edit').click();
      });
    cy.get('select').select('boolean');
    cy.get('button').contains(new RegExp('Apply', 'g')).click();

    cy.columnActionClick('Fat', ['Sort']);

    cy.waitForDialogPanel();
    cy.get('[type="radio"]').check('boolean');
    cy.confirmDialogPanel();

    cy.getCell(0, 'Fat').should('to.contain', 'false');
    cy.getCell(1, 'Fat').should('to.contain', 'true');
    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('By Fat').trigger('mouseover');
    cy.get('.menu-item').contains('Reverse').click();
    cy.getCell(0, 'Fat').should('to.contain', 'true');
    cy.getCell(1, 'Fat').should('to.contain', 'false');
    cy.get('.viewpanel-sorting a').contains('Sort').click();
    cy.get('.menu-container').contains('Reorder rows permanently').click();
    cy.assertNotificationContainingText('Reorder rows');

    // check that it triggered an operation
    cy.get('#or-proj-undoRedo').click();
    cy.get('.history-entry span').should('to.contain', 'Reorder rows');
    
    // after reloading, the grid should still be in the new order
    cy.reload();
    cy.waitForProjectTable();
    cy.getCell(0, 'Fat').should('to.contain', 'true');
    cy.getCell(1, 'Fat').should('to.contain', 'false');
  });
});
