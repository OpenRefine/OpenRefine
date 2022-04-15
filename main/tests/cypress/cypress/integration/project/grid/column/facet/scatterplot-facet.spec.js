/**
 * Those tests are generic test to ensure the general behavior of the various facets components
 * It's using "text facet" as it is the most simple facet
 */
describe(__filename, function () {
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

  it('Test facet created from the scatterplot matrix', function () {
    cy.loadAndVisitProject('food.small', 'food-small');
    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');
    cy.castColumnTo('Protein', 'number');
    cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);
    // test against the linear screenshot
    cy.get(
      '.scatterplot-matrix-table a[title="Water (x) vs. Protein (y)"]'
    ).click();
    // test the facet
    cy.getFacetContainer('Water (x) vs. Protein (y)').should('to.exist');
    // verify that all the buttons to change the scatterplot look&feel are there + export button
    cy.getFacetContainer('Water (x) vs. Protein (y)').within(() => {
      cy.get('label').contains('lin').should('to.exist');
      cy.get('label').contains('log').should('to.exist');
      cy.get('label[title="Rotated 45° counter-clockwise"]').should('to.exist');
      cy.get('label[title="No rotation"]').should('to.exist');
      cy.get('label[title="Rotated 45° clockwise"]').should('to.exist');
      cy.get('label[title="Small dot size"]').should('to.exist');
      cy.get('label[title="Regular dot size"]').should('to.exist');
      cy.get('label[title="Big dot size"]').should('to.exist');
      cy.get('a').contains('Export plot').should('to.exist');
    });
  });
});
