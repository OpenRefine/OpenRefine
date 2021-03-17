/**
 * Those tests are generic test to ensure the general behavior of the various facets components
 * It's using "text facet" as it is the most simple facet
 */
describe(__filename, function () {
  // it('Test and verify the matrix against a snapshot (3 integer columns)', function () {
  // 	cy.loadAndVisitProject('food.small', 'food-small');

  // 	cy.castColumnTo('Water', 'number');
  // 	cy.castColumnTo('Energ_Kcal', 'number');
  // 	cy.castColumnTo('Protein', 'number');

  // 	cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);
  // 	cy.get('.scatterplot-matrix-table').matchImageSnapshot(
  // 		'scatterplot-default'
  // 	);
  // });

  it('Verify the scatterplot matrix order of elements', function () {
    cy.loadAndVisitProject('food.small', 'food-small');

    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');
    cy.castColumnTo('Protein', 'number');

    cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);

    cy.get('.scatterplot-matrix-table').within(() => {
      // Check row headers
      cy.get('tr:first-child .column_header').should('to.contain', 'Water');
      cy.get('tr:nth-child(2) .column_header').should(
        'to.contain',
        'Energ_Kcal'
      );
      cy.get('tr:nth-child(3) .column_header').should('to.contain', 'Protein');

      // check first row
      cy.get('tr:first-child td:nth-child(2) a')
        .invoke('attr', 'title')
        .should('eq', 'Water (x) vs. Energ_Kcal (y)');
      cy.get('tr:first-child td:nth-child(3) a')
        .invoke('attr', 'title')
        .should('eq', 'Water (x) vs. Protein (y)');

      // check second row
      cy.get('tr:nth-child(2) td:nth-child(2) a')
        .invoke('attr', 'title')
        .should('eq', 'Energ_Kcal (x) vs. Protein (y)');
    });
  });

  it('Visual testing for the lin/log options', function () {
    cy.loadAndVisitProject('food.small', 'food-small');

    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');
    cy.castColumnTo('Protein', 'number');

    cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);

    // test against the linear screenshot
    cy.get('.scatterplot-selectors label').contains('lin').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot('scatterplot-lin', {
      failureThreshold: 5,
    });

    // test against the linear screenshot
    cy.get('.scatterplot-selectors label').contains('log').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot('scatterplot-log', {
      failureThreshold: 5,
    });
  });
});
