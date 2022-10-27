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

    cy.waitForOrOperation();

    //sliding the right slider
    cy.get('.slider-widget-bracket').eq(1)
    .trigger('mousedown',{ force: true })
    .trigger('mousemove',-130,0,{ force: true })
    .trigger('mouseup',{ force: true })
    cy.get('#summary-bar').contains('72 matching rows');

    //sliding the middle portion in numeric facet
    cy.get('.slider-widget-draggable').eq(0)
    .trigger('mousedown',{ force: true }).waitForOrOperation()
    .trigger('mousemove',130,0,{ force: true })
    .trigger('mouseup',{ force: true }).waitForOrOperation();
    cy.get('#summary-bar').contains('70 matching rows');

    //sliding the left slider
    cy.get('.slider-widget-bracket').eq(0)
    .trigger('mousedown',{ force: true })
    .trigger('mousemove',130,0,{ force: true })
    .trigger('mouseup',{ force: true })
    cy.get('#summary-bar').contains('4 matching rows');
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
