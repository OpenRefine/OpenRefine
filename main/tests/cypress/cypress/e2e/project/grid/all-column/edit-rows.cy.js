describe(__filename, function () {
  it('Ensure all rows are starred', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Edit rows', 'Star rows']);
    cy.assertNotificationContainingText('Star 2 rows');

    cy.get('.data-table tr:nth-child(1) td:nth-child(1) a').should(
      'have.class',
      'data-table-star-on'
    );
    cy.get('.data-table tr:nth-child(2) td:nth-child(1) a').should(
      'have.class',
      'data-table-star-on'
    );
  });
  it('Ensure all rows are unstarred', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Edit rows', 'Star rows']);
    cy.assertNotificationContainingText('Star 2 rows');

    cy.get('.data-table tr:nth-child(1) td:nth-child(1) a').should(
      'have.class',
      'data-table-star-on'
    );
    cy.get('.data-table tr:nth-child(2) td:nth-child(1) a').should(
      'have.class',
      'data-table-star-on'
    );
    cy.columnActionClick('All', ['Edit rows', 'Unstar rows']);
    cy.assertNotificationContainingText('Unstar 2 rows');

    cy.get('.data-table tr:nth-child(1) td:nth-child(1) a').should(
      'have.class',
      'data-table-star-off'
    );
    cy.get('.data-table tr:nth-child(2) td:nth-child(1) a').should(
      'have.class',
      'data-table-star-off'
    );
  });
  it('Ensure all rows are flagged', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Edit rows', 'Flag rows']);
    cy.assertNotificationContainingText('Flag 2 rows');

    cy.get('.data-table tr:nth-child(1) td:nth-child(2) a').should(
      'have.class',
      'data-table-flag-on'
    );
    cy.get('.data-table tr:nth-child(2) td:nth-child(2) a').should(
      'have.class',
      'data-table-flag-on'
    );
  });
  it('Ensure all rows are unflagged', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Edit rows', 'Flag rows']);
    cy.assertNotificationContainingText('Flag 2 rows');

    cy.get('.data-table tr:nth-child(1) td:nth-child(2) a').should(
      'have.class',
      'data-table-flag-on'
    );
    cy.get('.data-table tr:nth-child(2) td:nth-child(2) a').should(
      'have.class',
      'data-table-flag-on'
    );
    cy.columnActionClick('All', ['Edit rows', 'Unflag rows']);
    cy.assertNotificationContainingText('Unflag 2 rows');

    cy.get('.data-table tr:nth-child(1) td:nth-child(2) a').should(
      'have.class',
      'data-table-flag-off'
    );
    cy.get('.data-table tr:nth-child(2) td:nth-child(2) a').should(
      'have.class',
      'data-table-flag-off'
    );
  });
  it('Ensure it removes matching rows', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Edit rows', 'Remove matching rows']);
    cy.assertNotificationContainingText('Remove 2 rows');

    cy.assertGridEquals([['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal']]);
  });
});
