describe(__filename, function () {
  it('A numeric facet must be casted first', function () {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Water', ['Facet', 'Numeric facet']);
    cy.get('#refine-tabs-facets .facets-container li:first-child').contains(
      'No numeric value present.'
    );
  });

  it('Changing the type of the column for numeric', function () {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Water', ['Facet', 'Numeric facet']);
    cy.get('#refine-tabs-facets .facets-container li:first-child').contains(
      'No numeric value present.'
    );

    cy.get(
      '#refine-tabs-facets .facets-container li:first-child a[bind="changeButton"]'
    ).click();
    cy.typeExpression('value.toNumber()');
    cy.get('.dialog-footer button').contains('OK').click();
    cy.get('.facet-container .facet-range-body').should('exist');
  });

  it('Test for change button and the sliding feature in numeric facet', function () {
    cy.loadAndVisitProject('food.small');

    cy.columnActionClick('Water', ['Facet', 'Numeric facet']);
    cy.get('#refine-tabs-facets .facets-container li:first-child a[bind="changeButton"]').click();
    cy.typeExpression('value.toNumber()');
    cy.get('.dialog-footer button').contains('OK').click();
    cy.get('#refine-tabs-facets').should('exist');

    // Wait for numeric facet to be populated before trying to manipulate it
    cy.get('.facet-range-status').contains('0 — 94')

    //sliding the right slider
    cy.get('.slider-widget-bracket').eq(1)
    .trigger('mousedown',{ force: true })
    .trigger('mousemove',-130,0,{ force: true })
    .trigger('mouseup',{ force: true });
    cy.get('.facet-range-status').contains('0 — 45');
    cy.get('#summary-bar').contains('72 matching rows');

    // wait for the numeric facet to be refreshed before next mouse events
    cy.get('.browsing-panel-indicator').should("not.be.visible");

    // sliding entire selection by the body (middle)
    cy.get('.slider-widget-draggable').eq(0)
    .trigger('mousedown',{ force: true })
    .trigger('mousemove',130,0,{ force: true })
    .trigger('mouseup',{ force: true });
    cy.get('.facet-range-status').contains('23 — 68');
    cy.get('#summary-bar').contains('74 matching rows');

    // wait for the numeric facet to be refreshed before next mouse events
    cy.get('.browsing-panel-indicator').should("not.be.visible");

    //sliding the left slider
    cy.get('.slider-widget-bracket').eq(0)
    .trigger('mousedown',{ force: true })
    .trigger('mousemove',130,0,{ force: true })
    .trigger('mouseup',{ force: true });
    cy.get('.facet-range-status').contains('66 — 68');
    cy.get('#summary-bar').contains('3 matching rows');
  });

  it('Test for checkboxes and reset button', function () {
    cy.loadAndVisitProject('food.small');

    cy.columnActionClick('Magnesium', ['Facet', 'Numeric facet']);
    cy.get('#refine-tabs-facets .facets-container li:first-child a[bind="changeButton"]').click();
    cy.typeExpression('value.toNumber()');
    cy.get('.dialog-footer button').contains('OK').click();

    cy.get('.browsing-panel-indicator').should("not.be.visible")

    cy.getNumericFacetContainer('Magnesium').find('#facet-0-numeric').click();
    cy.get('#summary-bar').contains('11 matching rows');
    cy.get('.browsing-panel-indicator').should("not.be.visible");

    cy.getNumericFacetContainer('Magnesium').find('#facet-0-error').click();
    cy.get('#summary-bar').contains('0 matching rows');
    cy.get('.browsing-panel-indicator').should("not.be.visible");

    cy.getNumericFacetContainer('Magnesium').contains('reset').click();
    cy.get('#summary-bar').contains('199 rows');
  });
});
