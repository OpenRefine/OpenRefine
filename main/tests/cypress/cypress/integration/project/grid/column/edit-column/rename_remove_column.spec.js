describe(__filename, function () {
  it('Ensures a column is removed from the data-table', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Remove this column']);

    cy.assertNotificationContainingText('Remove column Shrt_Desc');

    cy.columnActionClick('Water', ['Edit column', 'Remove this column']);

    cy.assertNotificationContainingText('Remove column Water');

    cy.get('.data-table-header').find('th').should('have.length', 3);

    cy.assertGridEquals([
      ['NDB_No', 'Energ_Kcal'],
      ['01001', '717'],
      ['01002', '717'],
    ]);
  });
  it('Ensures a column is renamed in the data-table', function () {
    cy.loadProject('food.mini').then((projectId) => {
      cy.visit(
        Cypress.env('OPENREFINE_URL') + '/project?project=' + projectId,
        {
          onBeforeLoad(win) {
            cy.stub(win, 'prompt').returns('test_rename_butter');
          },
        }
      );
    });

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Rename this column']);

    cy.assertNotificationContainingText('Rename column Shrt_Desc');

    cy.assertCellEquals(0, 'test_rename_butter', 'BUTTER,WITH SALT');
    cy.assertCellEquals(1, 'test_rename_butter', 'BUTTER,WHIPPED,WITH SALT');
  });
});
