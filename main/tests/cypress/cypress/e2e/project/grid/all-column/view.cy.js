describe(__filename, function () {
  it('it collapses all columns', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Hide / Show', 'Hide all columns']);

    cy.get('.odd td:nth-child(4)').should('to.contain', '');
    cy.get('.odd td:nth-child(5)').should('to.contain', '');
    cy.get('.odd td:nth-child(6)').should('to.contain', '');
    cy.get('.odd td:nth-child(7)').should('to.contain', '');

    cy.get('.even td:nth-child(4)').should('to.contain', '');
    cy.get('.even td:nth-child(5)').should('to.contain', '');
    cy.get('.even td:nth-child(6)').should('to.contain', '');
    cy.get('.even td:nth-child(7)').should('to.contain', '');
  });
  it('it expands all columns', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Hide / Show', 'Hide all columns']);

    cy.get('.odd td:nth-child(4)').should('to.contain', '');
    cy.get('.even td:nth-child(7)').should('to.contain', '');

    cy.columnActionClick('All', ['Hide / Show', 'Show all columns']);

    cy.assertGridEquals([
      ['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal'],
      ['01001', 'BUTTER,WITH SALT', '15.87', '717'],
      ['01002', 'BUTTER,WHIPPED,WITH SALT', '15.87', '717'],
    ]);
  });
  it('it shows or hides null values', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('Energ_Kcal', ['Edit cells', 'Common transforms', 'To null']);
    cy.columnActionClick('All', ['Hide / Show', 'Show / Hide null values in cells']);

    cy.assertGridEquals([
      ['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal'],
      ['01001', 'BUTTER,WITH SALT', '15.87', null],
      ['01002', 'BUTTER,WHIPPED,WITH SALT', '15.87', null],
    ]);
    cy.columnActionClick('All', ['Hide / Show', 'Show / Hide null values in cells']);

    cy.assertGridEquals([
      ['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal'],
      ['01001', 'BUTTER,WITH SALT', '15.87', null],
      ['01002', 'BUTTER,WHIPPED,WITH SALT', '15.87', null],
    ]);
  });
});
