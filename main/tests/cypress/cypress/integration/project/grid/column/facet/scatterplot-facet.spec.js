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

  it('Visual testing for the lin/log options', function () {
    cy.loadAndVisitProject('food.small', 'food-small');

    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');
    cy.castColumnTo('Protein', 'number');

    cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);

    // test against the linear screenshot
    cy.get('.scatterplot-selectors label').contains('lin').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot('scatterplot-lin');

    // test against the linear screenshot
    cy.get('.scatterplot-selectors label').contains('log').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot('scatterplot-log');
  });
});
