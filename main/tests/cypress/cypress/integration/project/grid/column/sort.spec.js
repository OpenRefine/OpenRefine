describe(__filename, function () {
  it('Perform a text sort + Reverse + Remove', function () {
    cy.loadAndVisitProject('food.sort');

    // sort and confirm
    cy.columnActionClick('Shrt_Desc', ['Sort']);
    cy.waitForDialogPanel();
    cy.confirmDialogPanel();

    // ensure sorting is active
    cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
    cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT');

    cy.columnActionClick('Shrt_Desc', ['Sort', 'Reverse']);

    cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT');
    cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WHIPPED,WITH SALT');

    cy.columnActionClick('Shrt_Desc', ['Sort', 'Remove sort']);

    cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT');
    cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
  });
  it('Perform a number sort + Reverse + Remove', function () {
    cy.loadAndVisitProject('food.sort');

    cy.castColumnTo('NDB_No', 'number');
    cy.columnActionClick('NDB_No', ['Sort']);

    cy.waitForDialogPanel();
    cy.get('[type="radio"]').check('number');
    cy.get('[type="radio"]').check('reverse');
    cy.confirmDialogPanel();

    // ensure sorting is active
    cy.getCell(0, 'NDB_No').should('to.contain', 1002);
    cy.getCell(1, 'NDB_No').should('to.contain', 1001);
    cy.columnActionClick('NDB_No', ['Sort', 'Reverse']);
    cy.getCell(0, 'NDB_No').should('to.contain', 1001);
    cy.getCell(1, 'NDB_No').should('to.contain', 1002);
    cy.columnActionClick('NDB_No', ['Sort', 'Remove sort']);
    cy.getCell(0, 'NDB_No').should('to.contain', 1001);
    cy.getCell(1, 'NDB_No').should('to.contain', 1002);
  });
  it('Perform a date sort + Reverse + Remove', function () {
    cy.loadAndVisitProject('food.sort');

    cy.castColumnTo('Date', 'date');
    cy.columnActionClick('Date', ['Sort']);

    cy.waitForDialogPanel();
    cy.get('[type="radio"]').check('date');
    cy.get('[type="radio"]').check('reverse');
    cy.confirmDialogPanel();

    // ensure sorting is active
    cy.getCell(0, 'Date').should('to.contain', '2020-12-17T00:00:00Z');
    cy.getCell(1, 'Date').should('to.contain', '2020-08-17T00:00:00Z');

    cy.columnActionClick('Date', ['Sort', 'Reverse']);

    cy.getCell(0, 'Date').should('to.contain', '2020-08-17T00:00:00Z');
    cy.getCell(1, 'Date').should('to.contain', '2020-12-17T00:00:00Z');

    cy.columnActionClick('Date', ['Sort', 'Remove sort']);
    cy.getCell(0, 'Date').should('to.contain', '2020-12-17T00:00:00Z');
    cy.getCell(1, 'Date').should('to.contain', '2020-08-17T00:00:00Z');
  });
  it('Perform a bool sort + Reverse + Remove', function () {
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

    cy.columnActionClick('Fat', ['Sort', 'Reverse']);
    cy.getCell(0, 'Fat').should('to.contain', 'true');
    cy.getCell(1, 'Fat').should('to.contain', 'false');

    cy.columnActionClick('Fat', ['Sort', 'Remove sort']);
    cy.getCell(0, 'Fat').should('to.contain', 'false');
    cy.getCell(1, 'Fat').should('to.contain', 'true');
  });
});
