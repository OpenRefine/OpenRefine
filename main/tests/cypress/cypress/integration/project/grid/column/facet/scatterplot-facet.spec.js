// Common configuration for scatterplot snapshots
// 5% is required, there are some differences between a local run and the images on ubuntu (github actions)
const matchImageSnapshotOptions = {
  failureThreshold: 0.05,
  failureThresholdType: 'percent',
};

/**
 * Those tests are generic test to ensure the general behavior of the various facets components
 * It's using "text facet" as it is the most simple facet
 */
describe(__filename, function () {
  it('Test and verify the matrix against a snapshot (3 integer columns)', function () {
    cy.loadAndVisitProject('food.small', 'food-small');
    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');
    cy.castColumnTo('Protein', 'number');
    cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);
    cy.get('.scatterplot-matrix-table').matchImageSnapshot(
      'scatterplot-default',
      matchImageSnapshotOptions
    );
  });

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

  it('Test the rendering options (lin, log, dot sizes) (visual testing)', function () {
    cy.loadAndVisitProject('food.small', 'food-small');
    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');
    cy.castColumnTo('Protein', 'number');
    cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);

    // test linear rendering
    cy.get('.scatterplot-selectors label').contains('lin').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot(
      'scatterplot-matrix-lin',
      matchImageSnapshotOptions
    );
    // test log rendering
    cy.get('.scatterplot-selectors label').contains('log').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot(
      'scatterplot-matrix-log',
      matchImageSnapshotOptions
    );

    // test the small dots rendering
    cy.get('label[title="Small Dot Size"]').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot(
      'scatterplot-matrix-small-dot',
      matchImageSnapshotOptions
    );

    // test the regular dots rendering
    cy.get('label[title="Regular Dot Size"]').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot(
      'scatterplot-matrix-regulat-dot',
      matchImageSnapshotOptions
    );

    // test the big dots rendering
    cy.get('label[title="Big Dot Size"]').click();
    cy.get('.scatterplot-matrix-table').matchImageSnapshot(
      'scatterplot-matrix-big-dot',
      matchImageSnapshotOptions
    );
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
      cy.get('label[title="Rotated 45° Counter-Clockwise"]').should('to.exist');
      cy.get('label[title="No rotation"]').should('to.exist');
      cy.get('label[title="Rotated 45° Clockwise"]').should('to.exist');
      cy.get('label[title="Small Dot Size"]').should('to.exist');
      cy.get('label[title="Regular Dot Size"]').should('to.exist');
      cy.get('label[title="Big Dot Size"]').should('to.exist');
      cy.get('a').contains('export plot').should('to.exist');
    });
  });

  // it('Test image rendering inside the facet (visual testing)', function () {
  //   cy.loadAndVisitProject('food.small', 'food-small');
  //   cy.castColumnTo('Water', 'number');
  //   cy.castColumnTo('Energ_Kcal', 'number');
  //   cy.castColumnTo('Protein', 'number');
  //   cy.columnActionClick('Protein', ['Facet', 'Scatterplot facet']);
  //   cy.get(
  //     '.scatterplot-matrix-table a[title="Water (x) vs. Protein (y)"]'
  //   ).click();

  //   // // verify that all the buttons to change the scatterplot look&feel are there + export button
  //   // // cy.getFacetContainer('Water (x) vs. Protein (y)').within(() => {
  //   // // test linear rendering
  //   // cy.get('#left-panel .scatterplot-selectors label').contains('lin').click();
  //   // cy.get('#left-panel .facet-scatterplot-plot-container').matchImageSnapshot(
  //   //   'scatterplot-facet-lin',
  //   //   matchImageSnapshotOptions
  //   // );
  //   // test log rendering
  //   cy.get('#left-panel .scatterplot-selectors label').contains('log').click();
  //   cy.get('#left-panel .facet-scatterplot-plot-container').matchImageSnapshot(
  //     'scatterplot-facet-log',
  //     matchImageSnapshotOptions
  //   );

  //   // test the small dots rendering
  //   cy.get('#left-panel label[title="Small Dot Size"]').click();
  //   cy.get('#left-panel .facet-scatterplot-plot-container').matchImageSnapshot(
  //     'scatterplot-facet-small-dot',
  //     matchImageSnapshotOptions
  //   );

  //   // test the regular dots rendering
  //   cy.get('#left-panel label[title="Regular Dot Size"]').click();
  //   cy.get('#left-panel .facet-scatterplot-plot-container').matchImageSnapshot(
  //     'scatterplot-facet-regulat-dot',
  //     matchImageSnapshotOptions
  //   );

  //   // test the big dots rendering
  //   cy.get('#left-panel label[title="Big Dot Size"]').click();
  //   cy.get('#left-panel .facet-scatterplot-plot-container').matchImageSnapshot(
  //     'scatterplot-facet-big-dot',
  //     matchImageSnapshotOptions
  //   );
  // });
});
