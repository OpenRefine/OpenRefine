describe(__filename, function () {
	it('A numeric facet must be casted first', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('Water', ['Facet', 'Numeric facet']);
		cy.get('#refine-tabs-facets .facets-container li:first-child').contains('No numeric value present.');
	});

	it('Changing the type of the column for numeric', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('Water', ['Facet', 'Numeric facet']);
		cy.get('#refine-tabs-facets .facets-container li:first-child').contains('No numeric value present.');

		cy.get('#refine-tabs-facets .facets-container li:first-child a[bind="changeButton"]').click();
		cy.get('.expression-preview-code').type('value.toNumber()');
		cy.get('.dialog-footer button').contains('OK').click();
		cy.get('.facet-container .facet-range-body').should('exist');
	});
});
