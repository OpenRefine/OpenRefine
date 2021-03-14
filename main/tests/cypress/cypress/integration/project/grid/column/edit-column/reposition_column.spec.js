describe(__filename, function () {
  it('Ensures a column is moved to beginning', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Move column to beginning',
    ]);

    cy.assertNotificationContainingText('Move column Shrt_Desc to position 0');

    cy.get('.data-table-header th:nth-child(2)').should(
      'to.contain',
      'Shrt_Desc'
    );
  });
  it('Ensures a column is moved to end', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Move column to end']);

    cy.assertNotificationContainingText('Move column Shrt_Desc to position 3');

    cy.get('.data-table-header th:nth-child(5)').should(
      'to.contain',
      'Shrt_Desc'
    );
  });
  it('Ensures a column is moved left', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Move column left']);

    cy.assertNotificationContainingText('Move column Shrt_Desc to position 0');

    cy.get('.data-table-header th:nth-child(2)').should(
      'to.contain',
      'Shrt_Desc'
    );
  });
  it('Ensures a column is moved right', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Move column right']);

    cy.assertNotificationContainingText('Move column Shrt_Desc to position 2');

    cy.get('.data-table-header th:nth-child(4)').should(
      'to.contain',
      'Shrt_Desc'
    );
  });
});
