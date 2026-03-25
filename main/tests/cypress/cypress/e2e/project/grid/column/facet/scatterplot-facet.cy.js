/**
 * Test the scatterplot facet.
 */

/**
 * Helper that posts to the get-rows API with a scatterplot facet config and returns the filtered count.
 * @param {string} projectId - The OpenRefine project ID
 * @param {string} openRefineUrl - Base URL of the OpenRefine instance
 * @param {string} csrfToken - CSRF token for the request
 * @param {object} facetConfig - Partial scatterplot facet configuration (merged with base config)
 */
function getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, facetConfig) {
  const engine = {
    facets: [
      {
        type: 'scatterplot',
        name: 'Water (x) vs. Energ_Kcal (y)',
        cx: 'Water',
        ex: 'value',
        cy: 'Energ_Kcal',
        ey: 'value',
        l: 150,
        dot: 1,
        ...facetConfig,
      },
    ],
    mode: 'row-based',
  };
  return cy
    .request({
      method: 'POST',
      url: `${openRefineUrl}/command/core/get-rows`,
      form: true,
      body: {
        project: projectId,
        engine: JSON.stringify(engine),
        start: 0,
        limit: 0,
        csrf_token: csrfToken,
      },
    })
    .then((resp) => resp.body.filtered);
}

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
    cy.get('label').contains('lin').should('to.exist');
    cy.get('label').contains('log').should('to.exist');
    cy.get('label[title="Rotated 45° counter-clockwise"]').should('to.exist');
    cy.get('label[title="No rotation"]').should('to.exist');
    cy.get('label[title="Rotated 45° clockwise"]').should('to.exist');
    cy.get('label[title="Small dot size"]').should('to.exist');
    cy.get('label[title="Regular dot size"]').should('to.exist');
    cy.get('label[title="Big dot size"]').should('to.exist');
    cy.get('.scatterplot-export-plot > a').contains('Export plot').should('to.exist');
    cy.get('#summary-bar').should('to.contain', '199 rows');
  });

  it('Rotation and log scale filters different rows for the same selection', function () {
    cy.loadAndVisitProject('food.small', 'food-small');
    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');

    // TODO: We'd really like to do this using mouse selection rather than
    // protocol requests, but we've been unable to get it working with the
    // jQuery imgareaselect widget
    cy.url().then((url) => {
      const projectId = url.split('=').slice(-1)[0];
      const openRefineUrl = Cypress.env('OPENREFINE_URL');

      cy.request(openRefineUrl + '/command/core/get-csrf-token').then(
        (tokenResp) => {
          const csrfToken = tokenResp.body.token;

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            from_x: 0.0,
            to_x: 0.5,
            from_y: 0.0,
            to_y: 1.0,
            dim_x: 'lin',
            dim_y: 'lin',
          }).as('noRotationCount');

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            from_x: 0.0,
            to_x: 0.5,
            from_y: 0.0,
            to_y: 1.0,
            dim_x: 'log',
            dim_y: 'log',
          }).as('logCount');

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            from_x: 0.0,
            to_x: 0.5,
            from_y: 0.0,
            to_y: 1.0,
            dim_x: 'lin',
            dim_y: 'lin',
            r: 'cw',
          }).as('cwCount');
        }
      );
    });

    cy.then(function () {
      // With CW rotation or log scale, the same rectangular pixel selection maps
      // to a different region of the data space, so the filtered count differs
      expect(this.cwCount).to.equal(198);
      expect(this.noRotationCount).to.equal(76);
      expect(this.logCount).to.equal(29);
    });
  });

});
