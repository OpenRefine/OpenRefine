/**
 * Those tests are generic test to ensure the general behavior of the various facets components
 * It's using "text facet" as it is the most simple facet
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
        from_x: 0.3,
        to_x: 0.7,
        from_y: 0.3,
        to_y: 0.7,
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
  });

  it('Log scale filters different rows than linear scale for the same selection', function () {
    cy.loadAndVisitProject('food.small', 'food-small');
    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');

    cy.url().then((url) => {
      const projectId = url.split('=').slice(-1)[0];
      const openRefineUrl = Cypress.env('OPENREFINE_URL');

      cy.request(openRefineUrl + '/command/core/get-csrf-token').then(
        (tokenResp) => {
          const csrfToken = tokenResp.body.token;

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            dim_x: 'lin',
            dim_y: 'lin',
          }).as('linearCount');

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            dim_x: 'log',
            dim_y: 'log',
          }).as('logCount');
        }
      );
    });

    cy.then(function () {
      // With log scale the coordinate transform maps [0.3,0.7] to a
      // different data range than with linear scale, so filtered counts must differ
      expect(this.logCount).to.not.equal(this.linearCount);
      // Both should be partial selections (not all rows, not zero)
      expect(this.linearCount).to.be.greaterThan(0);
      expect(this.logCount).to.be.greaterThan(0);
    });
  });

  it('CW rotation filters different rows than no rotation for the same selection', function () {
    cy.loadAndVisitProject('food.small', 'food-small');
    cy.castColumnTo('Water', 'number');
    cy.castColumnTo('Energ_Kcal', 'number');

    cy.url().then((url) => {
      const projectId = url.split('=').slice(-1)[0];
      const openRefineUrl = Cypress.env('OPENREFINE_URL');

      cy.request(openRefineUrl + '/command/core/get-csrf-token').then(
        (tokenResp) => {
          const csrfToken = tokenResp.body.token;

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            dim_x: 'lin',
            dim_y: 'lin',
          }).as('noRotationCount');

          getScatterplotFilteredCount(projectId, openRefineUrl, csrfToken, {
            dim_x: 'lin',
            dim_y: 'lin',
            r: 'cw',
          }).as('cwCount');
        }
      );
    });

    cy.then(function () {
      // With CW rotation the same rectangular pixel selection maps to a
      // rotated region of the data space, so filtered counts must differ
      expect(this.cwCount).to.not.equal(this.noRotationCount);
      // Both should be partial selections (not all rows, not zero)
      expect(this.noRotationCount).to.be.greaterThan(0);
      expect(this.cwCount).to.be.greaterThan(0);
    });
  });
});
